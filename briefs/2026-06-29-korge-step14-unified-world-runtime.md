# Brief: Step 14 — Unified World Runtime (Spine, Pfeiler 1)

**MODELL: Opus-only** (vor Start Modell prüfen — `.kiro/steering/handoff-protocol.md`).
**Pflichtlektüre VOR der Arbeit:**
- `docs/ENGINE_VISION.md` — die 5 Pfeiler, diese Aufgabe ist **Pfeiler 1 (die Spine)**.
- `.claude/skills/gaime-shaders/SKILL.md` — `tex()` braucht Pixel-Koordinaten; **Layer-Scale
  multipliziert auch die Position** (Step-13-Trap: `layerTile = round(64/tilesTall)`,
  `charScale = screenTile/layerTile`); „render ≠ logic — PNG ansehen"; B004; B007.
- `docs/MAP_ART_DIRECTION.md` — Bild = Haut, Grid = Logik.
- `docs/KNOWN_BUGS.md` — vor Arbeit lesen, neue Funde ergänzen.

**Datum:** 2026-06-29
**Branch:** `kiro/korge-step14-unified-world-runtime`
**BASE_SHA:** `16cff450`

---

## Ziel

Die drei halben Welt-Renderpfade zu **einem** Runtime zusammenführen. Heute kann nur
`WorldScene` (sichtbare Tilemap) Gameplay — NPCs, Dialog, Bark, HUD, Questbook, Kampf,
Karten-Übergänge — aber im **alten Tile-Look**. Die schöne Bild+Grid-Doodle-Welt
(`DoodleWorldScene`, Step 13) kann nur „Figur bewegt sich über gemaltem Hintergrund".

**Diese Aufgabe holt das gesamte Gameplay in die Bild+Grid-Welt** und macht sie zum
**einzigen** Gameplay-Runtime. Danach steckt alles Weitere (Pfeiler 2–5) hier ein.

**Bewusst NICHT in diesem Brief** (kein Scope-Mix — siehe CLAUDE.md):
- Wetter/Physik-Overlays (Wasser, Schnee, Jahreszeit, Atem, Drunk, Day/Night, Lighting)
  → das ist **Pfeiler 2** (`BaseOverlay` + Render-Parität), eigener Brief.
- Löschen von `WorldScene.kt` / des `:composeApp`-Moduls → mechanischer Folge-Brief.
  Hier wird `WorldScene` nur **nicht mehr gebootet** (faktisch stillgelegt), nicht gelöscht.
- mapbuilder-Härtung (Pfeiler 3).

---

## Kern-Designentscheidung: NPCs sind unsichtbare Interaktions-Hotspots

`tavern_interior.png` **enthält bereits gemalte Gäste** (Wirt, Patrons an den Tischen —
siehe `docs/screenshots/step13-doodle-world-1440p.png`). Doodle-Sprites darüber zu legen
wäre falsch/doppelt.

→ In der Bild+Grid-Welt sind NPCs **unsichtbare Hotspots auf Grid-Zellen** (Dialog +
optional BarkEvent), platziert dort, wo eine gemalte Figur sitzt. Der **Spieler ist die
einzige gerenderte Figur** (Doodle-Filter). „Bild = Haut" liefert die NPC-Optik, „Grid =
Logik" liefert die Interaktion.

Pixelgenaue Ausrichtung der Hotspots auf die gemalten Figuren ist **Art-Direction-Politur
(Pfeiler 5)** — hier reichen wenige, **verifiziert begehbare** Zellen nahe der gemalten
Gäste, damit der Loop (hingehen → E → Dialog → Bark) beweisbar funktioniert.

---

## Teil A — Eigene Daten für die Bild-Welt (`game/world/ImageWorldDef.kt`)

`MapConfig` bleibt unangetastet (alte Tile-Koordinaten, von `WorldScene` genutzt). Die
Bild-Welt bekommt **eigene** Daten in HD-Grid-Koordinaten (78×78 für die Taverne, 0-basiert):

```kotlin
data class ImageNpcHotspot(val cellX: Int, val cellY: Int, val facingHint: Facing,
                           val dialog: List<DialogLine>, val barkEvent: BarkEvent?)
data class ImageMapExit(val cellX: Int, val cellY: Int, val destination: ImageMapId,
                        val destSpawnX: Int, val destSpawnY: Int)
enum class ImageMapId { TAVERN_INTERIOR, SYLVANORIA_WILDWOOD }
data class ImageWorldDef(
    val id: ImageMapId,
    val imagePath: String,      // assets/HD/backgrounds/*.png
    val gridTmxPath: String,    // assets/HD/backgrounds/*.tmx
    val displayName: String,
    val spawn: Pair<Int,Int>?,  // null → aus CollisionGrid ableiten (B004)
    val npcs: List<ImageNpcHotspot>,
    val exits: List<ImageMapExit>,
)
```

Zwei Defs anlegen (Dialog/BarkEvent-Inhalte aus `MapConfig.interior()`/`exterior()`
**übernehmen** — gleiche `DialogLine`/`BarkEvent`-Werte, nur neue HD-Grid-Zellen):
- **TAVERN_INTERIOR:** `tavern_interior.png` + `tavern_interior.tmx`. 2 Hotspots
  (Barkeep `BARKEEP_SPEND_SOME_COIN`, Patron `PATRON_HE_SURE_IS_SLOW`) auf begehbaren
  Zellen nahe der gemalten Theke/Tische. 1 Exit → SYLVANORIA_WILDWOOD.
- **SYLVANORIA_WILDWOOD:** `sylvanoria_wildwood.png` + `sylvanoria_wildwood.tmx` (86×48).
  1–2 Hotspots (Guard `GUARD_BACK_ALREADY`). 1 Exit → TAVERN_INTERIOR.

Alle Hotspot-/Exit-/Spawn-Zellen **müssen gegen die `CollisionGrid` verifiziert** WALKABLE
sein (B004). Wenn eine geratene Zelle blockiert ist → nächste begehbare Nachbarzelle nehmen.

---

## Teil B — Das Runtime (`DoodleWorldScene.kt` wird zur Spine)

`DoodleWorldScene` von „nur Hintergrund + Figur" zum vollen Runtime ausbauen. Hilfsklassen
nach `game/world/*` extrahieren wo sinnvoll (Kamera-Glue, Interaktion, Hotspot-Rendering).
Companion-Var `var pendingMap: ImageMapId = TAVERN_INTERIOR` (wie `WorldScene.pendingConfig`),
damit Übergänge die Szene mit Ziel neu laden können.

Aufbau in `sceneMain()`:

1. **Map laden** aus der `ImageWorldDef` für `pendingMap`: Bild (`readBitmap`, `smoothing=true`),
   `gridTmxPath` → `TmxLoader.parse` → `CollisionGrid.from(...)`.

2. **Grid-as-unit, scroll-fähig** (Step-13-Mathe verallgemeinert):
   - `screenTile = OUTPUT_H / gridRows` (Bild füllt die Höhe).
   - `bgScale = OUTPUT_H / imageHeightPx`; `worldW = imageWidthPx * bgScale`,
     `worldH = OUTPUT_H`.
   - **`worldLayer` Container** enthält **bg-Bild + entityLayer + hotspot-Debug** und wird
     von der Kamera bewegt. Quadrat-Taverne (`worldW < 2560`) → zentriert; breite Wildwood
     (`worldW > 2560`) → horizontal gescrollt.

3. **Kamera** — `rpg.world.Camera` (core, getestet, clamped) **wiederverwenden**, NICHT neu
   bauen: jeder Frame `camera.follow(playerScreenX, playerScreenY, 2560f, 1440f, worldW, worldH)`,
   dann `worldLayer.x = -camera.x; worldLayer.y = -camera.y`. Das ist der einzige Weg, Karten
   größer als der Schirm spielbar zu machen.

4. **Spieler** (`CharacterSprite` in `entityLayer`, Doodle-Filter) — **exakt die Step-13-
   Skala-Regel** (Skill!): `tilesTall=5`, `layerTile = (64/tilesTall).toInt()`,
   `charScale = screenTile/layerTile`, `CharacterSprite(entityLayer, layerTile, layerTile)`,
   `entityLayer.scale = charScale`. Doodle-Filter NUR auf `entityLayer` (Hintergrund bleibt
   scharf). Boil pro Frame ticken. Spawn aus Def oder `CollisionGrid` (B004).

5. **NPC-Hotspots** — **kein Sprite** gerendert. Liste aus der Def. Optional ein dezenter
   Debug-Marker (kleiner halbtransparenter Rect auf der Zelle), der per `const val DEBUG_HOTSPOTS`
   abschaltbar ist (Default: an, damit der Screenshot beweist, dass Hotspots sitzen).

6. **HUD** — `HudOverlay(this, hero, inventory, def.displayName)` im **Scene-Root**
   (bildschirmfest, NICHT im worldLayer). `hero: Combatant` + `inventory: Inventory` wie in
   `WorldScene` (lines ~57–60) konstruieren.

7. **Dialog** — `DialogOverlay(this, width, height)` im Scene-Root. Dialog hat Eingabe-Priorität.

8. **Bark-Pipeline** — `SliceDirector { System.currentTimeMillis() }` +
   `director.barkAudioPlayer = BarkAudioPlayer(GameAudioPlayer(this@DoodleWorldScene))`,
   `roomId` analog `WorldScene` (line ~65). Auf E-Interaktion mit Hotspot, der `barkEvent`
   hat: `director.fireBark(event)`, bei `BarkOutcome.Fired` Audio abspielen (Muster aus
   `WorldScene` ~467–490 übernehmen).

9. **Questbook** — `QuestbookOverlay(this, width, height)`; Taste **J** togglet/öffnet
   (Muster aus `WorldScene` ~457–462).

10. **Eingabe-Loop** (`addUpdater`), Prioritätsreihenfolge **wie `WorldScene`**:
    - Doodle-Boil ticken.
    - Dialog aktiv? → RETURN/SPACE = `dialog.advance()`, sonst Eingabe schlucken.
    - **SPACE** (kein Dialog) → Kampf: `sceneContainer.changeTo<BattleScene>()`.
    - **J** → `QuestbookScreen` (falls so in `WorldScene`) bzw. Overlay toggeln — Muster spiegeln.
    - **E** → Hotspot auf der **Blickrichtungs-Zelle** (`player.gridX+facing.dx`, …): Dialog
      zeigen + Bark feuern.
    - **WASD/Pfeile** → Zielzelle, nur bei WALKABLE/TRIGGER bewegen (`CharacterSprite.startMove`,
      sanfte Tile-Bewegung), Kamera folgt.
    - **Exit-Zelle betreten** → `ImageMapExit`: `pendingMap = exit.destination`,
      Spieler-Zielspawn merken, `sceneContainer.changeTo<DoodleWorldScene>()` (Szene lädt neu,
      Spawn = `destSpawnX/Y`). Verifiziert begehbar.

Camera-Center-Verhalten (quadratische Map) liefert die Step-13-Letterbox automatisch über die
clamp-Center-Logik der `Camera` — **keine** manuellen Letterbox-Rects mehr nötig (der dunkle
`Korge(backgroundColor=BLACK)` zeigt die Ränder).

---

## Teil C — Main.kt

Bleibt 1440p, bootet weiter `DoodleWorldScene` (jetzt das volle Runtime). `pendingMap`
default `TAVERN_INTERIOR`. `WorldScene` wird **nicht mehr** referenziert/gebootet (Kommentar:
„retired as boot path — Step 14; deletion is a follow-up").

---

## Teil D — Screenshot-Beweise (`ScreenshotHarness.kt`)

**B007:** `localCurrentDirVfs`-Zeile + Import NICHT ändern.

Bestehende Captures müssen weiter laufen. Neu hinzufügen + in `main()` registrieren:
1. `captureUnifiedTavern()` @ `Size(2560,1440)`: Taverne (zentriert/letterboxed) + Doodle-Spieler
   auf begehbarer Zelle + **HUD sichtbar** + **Hotspot-Debug-Marker** an den NPC-Zellen.
   `save("unified_tavern")`.
2. `captureUnifiedWildwood()` @ `Size(2560,1440)`: Wildwood (breiter als Schirm) mit **Kamera
   auf den Spieler zentriert** (Scroll sichtbar — Bildränder zeigen Karte, nicht Letterbox).
   `save("unified_wildwood")`.
3. `captureUnifiedDialog()`: Taverne mit **aktivem DialogOverlay** (Barkeep-Zeile sichtbar),
   um den Interaktions-Loop zu beweisen. `save("unified_dialog")`.

Nach dem Rendern: **PNGs ansehen** (Skill) — HUD da? Spieler getuscht & richtig skaliert auf
begehbarem Boden? Wildwood wirklich gescrollt (kein Letterbox)? Dialog-Panel sichtbar?

---

## SCOPE

```
modify:
  - game/src/desktopMain/kotlin/game/DoodleWorldScene.kt      (→ volles Runtime)
  - game/src/desktopMain/kotlin/game/Main.kt                  (Kommentar/Boot bleibt DoodleWorldScene)
  - game/src/desktopMain/kotlin/game/ScreenshotHarness.kt     (3 Captures anfügen + registrieren)
create:
  - game/src/desktopMain/kotlin/game/world/*                  (ImageWorldDef.kt + Runtime-Helfer:
                                                               Kamera-Glue, Interaktion, Hotspots)
  - briefs/2026-06-29-korge-step14-unified-world-runtime-result.md
```

## DO_NOT_TOUCH

```
- game/src/desktopMain/kotlin/game/ScreenshotHarness.kt → localCurrentDirVfs-Zeile + Import (B007)
- game/src/desktopMain/kotlin/game/WorldScene.kt        (bleibt als Referenz, nur nicht gebootet)
- game/src/desktopMain/kotlin/game/MapConfig.kt         (alte Tile-Welt — nur LESEN für Dialog/Bark-Werte)
- game/src/desktopMain/kotlin/game/BattleScene.kt, QuestbookScreen.kt  (changeTo-Ziele — nur konsumieren)
- game/src/desktopMain/kotlin/game/shader/*             (nur DoodleLineFilter konsumieren)
- game/.../*Overlay.kt für Wetter/Physik (Water/Blood/Snow/Spring/Summer/Autumn/
  MaterialFatigue/Footprint) + ShaderStateBinder/ShaderEffects  → das ist Pfeiler 2, NICHT anfassen
- game/.../CharacterSprite.kt, HudOverlay.kt, DialogOverlay.kt, QuestbookOverlay.kt  (nur konsumieren)
- core/, composeApp/, tools/mapbuilder/, assets/ (nur lesen), settings.gradle.kts
- docs/KNOWN_BUGS.md  nur lesen (neue Bugs im Result-File melden)
```

> `CharacterSprite`/Overlay-Konstruktoren nur **aufrufen**, nicht ändern. Wenn das Runtime
> eine kleine Erweiterung an einer dieser Klassen zu brauchen scheint → im Result-File
> begründen und nachfragen, **nicht** stillschweigend die DO_NOT_TOUCH-Datei ändern.

---

## ACCEPTANCE

```bash
./gradlew :core:desktopTest                  → BUILD SUCCESSFUL
./gradlew :game:compileKotlinDesktop         → BUILD SUCCESSFUL
./gradlew :composeApp:compileKotlinDesktop   → BUILD SUCCESSFUL
bash scripts/setup-gl.sh; ./gradlew :game:screenshot
   → inkl. unified_tavern.png, unified_wildwood.png, unified_dialog.png (alle 2560×1440)
   → alle bestehenden Screenshots weiterhin erzeugt
```
`:game:run` startet im 1440p-Fenster im Unified-Runtime: WASD bewegt den Doodle-Spieler,
Kamera folgt, E öffnet NPC-Dialog, Bark feuert, J Questbook, SPACE Kampf, Exit-Zelle wechselt
die Karte (Taverne ↔ Wildwood). Bei GL „Too many callbacks" einmal wiederholen.
Sandbox-Hinweis: ggf. `LD_LIBRARY_PATH=/usr/lib64` für EGL (siehe Step-13-Result).

---

## Kontext / Querverweise

- **Step 13** hat den Bild+Grid+Doodle-Kern + die Skala-Mathe bewiesen — hier wird er zum
  Runtime erweitert, NICHT neu erfunden.
- **`rpg.world.Camera`** (core, getestet) ist der Schlüssel für Karten größer als der Schirm —
  wiederverwenden, nicht neu schreiben.
- **NPC = Hotspot, kein Sprite** — der gemalte Hintergrund liefert die NPC-Optik.
- **Dialog/Bark/HUD/Questbook/Battle-Verdrahtung**: 1:1-Muster aus `WorldScene.kt`
  (~57–65, ~424–500, ~583–588) in die Bild-Welt übertragen.
- **Nächste Stufe (NICHT hier):** Pfeiler 2 (`BaseOverlay` + sichtbare Physik), dann
  `WorldScene`/`composeApp`-Löschung (Folge-Brief), dann Vertical Slice (Pfeiler 5).
```
