# Brief: Step 7d — Wasser, Licht, Rausch & das Questbook in seiner ganzen Pracht

**Datum:** 2026-06-28
**Branch:** `kiro/korge-step7d-water-light-drunk-questbook`
**BASE_SHA:** `91438142`

---

## Vorbemerkung: Großer Sprint, EIN PR

Dies ist ein bewusst umfangreicher Auftrag (fünf Teile A–E). Liefere **alle fünf**
vollständig mit je eigenem Beweis (Unit-Test ODER Screenshot), bevor du den PR öffnest.
Kein Teil-PR. Du hast Zeit — arbeite das am Stück durch.

Die Vision steht in deinen eigenen Docs — **nutze sie als Quelle, nicht als Dekoration**:
- `docs/SHADER_VISION.md` → Effekt-Katalog Step 7c (#7–11 Wasser, #16 Hitzeflimmern), 7b (#3 Licht)
- `docs/SHADER_ACTORS_AND_AUDIOMANCER.md` → „Nässe wird sozial gefährlich" (Holzböden saugen
  Nässe auf, Tropfspuren), Leitsatz „Jeder Shader verursacht/löst/missversteht ein Problem"
- `docs/SHADER_GAME_CONCEPT.md` / `docs/ONTOLOGIE_DES_SICHTBAREN.md` → „Die Shader SIND der State"

**Wichtig vor dem Start:**
```bash
git fetch origin main
git checkout -b kiro/korge-step7d-water-light-drunk-questbook origin/main
git log --oneline -3   # soll 91438142 ganz oben zeigen
```

---

## Architektur-Leitplanke (entscheidend!)

- **Simulations-LOGIK gehört in `:core`** (engine-agnostisch, unit-testbar). Wasserstand,
  Fließen, Abfluss, Verdunstung, Nässe-Zustand → reine Kotlin-Klassen mit Tests.
- **`:game` rendert nur** den Zustand (Shader/Sprites/Overlays) und verbindet Input.
- **Filter-Stacking:** Ein `Container` hat genau **ein** `filter`. Für gleichzeitige Effekte
  (Laterne + Regen) verifiziere die KorGE-6.0-API für Filter-Komposition
  (`ComposedFilter`/`FilterChain` — gegen `korge-6.0.0-sources.jar` prüfen) ODER nutze die in
  `SHADER_VISION.md` beschriebene **Per-Layer-Differenzierung** (Welt-Layer ≠ Wetter-Layer).
  Wenn Stacking zu teuer wird: dokumentiere die Entscheidung im Result-File.

---

## Teil A — Wasser-Simulation in `:core` (mit Unit-Tests)

Neues Package `core/src/commonMain/kotlin/rpg/weather/`:

### `WaterGrid.kt`
Ein Gitter aus `waterDepth: Float` pro Zelle (0.0 = trocken). Bounding-Box wie `CollisionGrid`
(offsetX/Y, width, height). Konstruktor nimmt Dimensionen + optionale **Drain-Tiles** (Set von
(x,y), an denen Wasser abläuft — analog zu `SHADER_VISION.md` #9 „Rinnen-Abfluss").

Methoden:
- `addRain(rate: Float)` — erhöht `waterDepth` jeder begehbaren Zelle um `rate`.
- `flowStep()` — ein Simulationsschritt: jede Zelle gibt einen Anteil ihres Überschusses an
  Nachbarn mit geringerem Stand ab (einfacher Nachbar-Transfer, 4er-Nachbarschaft). So entstehen
  und **verbinden sich Pfützen** emergent. An Drain-Tiles wird der Stand pro Step reduziert
  (`drainRate`) → Wasser läuft an der Rinne ab.
- `evaporate(rate: Float)` — senkt alle Stände (für „Regen stoppt → Pfützen trocknen", #10).
- `puddleAt(x, y): Boolean` — `waterDepth > PUDDLE_THRESHOLD`.
- `connectedPuddle(x, y): Set<Pair<Int,Int>>` — Flood-Fill der zusammenhängenden Pfütze
  (beweist „Pfützen verbinden sich"). Für Tests + späteres Splash-Rendering.

### `WetnessState.kt`
Pro Entität (Spieler/NPC) ein `wetness: Float` (0..1).
- `soak(amount)` — wird nass (in Regen / beim Betreten einer Pfütze).
- `dryNearHeat(rate)` — trocknet (am Feuer/Ofen). Clamp 0..1.
- `val isWet: Boolean get() = wetness > 0.05f`.
- `val isSlippery: Boolean` — true wenn nass genug, um auf Holzboden zu rutschen.

### Tests `core/src/commonTest/kotlin/rpg/weather/`
- `WaterGridTest`: addRain hebt Stände; flowStep verteilt von hoch zu niedrig; Drain-Tile
  reduziert; evaporate senkt; zwei benachbarte Pfützen ergeben EINE `connectedPuddle`-Menge;
  isolierte Pfützen bleiben getrennt.
- `WetnessStateTest`: soak erhöht (clamp 1.0), dryNearHeat senkt (clamp 0.0), isWet/isSlippery
  Schwellen.

**Acceptance A:** `./gradlew :core:desktopTest` grün (inkl. neue Tests).

---

## Teil B — Wasser-Rendering + rutschiger Boden + Trocknen in `:game`

### B1 — Regen + Pfützen sichtbar
- Regen über den bestehenden `RainFilter` (existiert in `game/shader/`, nur konsumieren).
- Pfützen aus `WaterGrid`: rendere pro Pfützen-Zelle ein halbtransparentes, leicht spiegelndes
  Rechteck/Overlay auf dem mapView (Alpha ∝ `waterDepth`). Eine **neue** Datei
  `game/src/desktopMain/kotlin/game/WaterOverlay.kt` (Container der die Pfützen-Rects hält,
  `update(grid: WaterGrid, tilePx, scale)`). Kein neuer GLSL-Shader nötig — Rects + Alpha + ggf.
  vertikale Spiegelung des darunterliegenden Tiles reichen für den ersten Wurf.

### B2 — Rutschiger nasser Holzboden
In `WorldScene`-Bewegung: wenn der Zielboden ein Holz-/nasser Tile ist UND `WetnessState.isSlippery`,
gleitet der Spieler **eine zusätzliche Kachel** in Bewegungsrichtung weiter (Momentum), sofern frei.
Nutze `TileType` + `WaterGrid.puddleAt(...)`. Halte es deterministisch (kein Random).

### B3 — Nass werden / am Feuer trocknen
- Spieler `soak()` wenn auf Pfützen-Tile oder (im Exterior) bei aktivem Regen.
- In der Nähe eines Feuer-Tiles / einer Feuer-`LightSource` (siehe Teil C, warmes Licht) ruft
  `dryNearHeat()`. Optional: aufsteigende Dampf-Partikel beim Trocknen (Polish, nicht Pflicht).
- Sichtbarkeit: nasser Spieler bekommt einen kühlen Blauschleier ODER einen kleinen Tropfen-
  Indikator im HUD-Bereich (kein neuer Shader Pflicht; ein getönter Rect-Tint genügt).

**Acceptance B (Screenshot):**
- `world_rain_puddles.png` — Exterior mit Regen + sichtbaren, teils verbundenen Pfützen.
- `world_puddle_drain.png` — Pfützen rund um ein Drain-Tile sichtbar abnehmend (Rinne).

---

## Teil C — Tragbare Laterne (Licht folgt dem Spieler)

- `LightingFilter` + `LightSource` existieren (`game/shader/`, nur konsumieren). `attachLighting`
  ist in `ShaderEffects`.
- Eine Laterne als `LightSource` (warm: r=1.0,g=0.8,b=0.4, radius ~5, flickerSpeed ~3), deren
  Position **jeden Frame** auf die Spieler-Kachel (`player.gridX/gridY`) gesetzt wird.
  Verifiziere wie `LightingFilter` die `lights`-Liste in die Uniforms schiebt (es gibt eine
  `render`/`updateUniforms`-Override) und aktualisiere die Laterne dort bzw. über
  `ShaderEffects.attachLighting(mapView, listOf(lantern), tilePx)` pro Frame neu.
- **Toggle mit Taste `L`** (an/aus). Bei aktiver Laterne `ambientDarkness` runter (~0.1) → die
  Laterne trägt sichtbar. Bei aus: heller Default (kein Lichtkegel).
- Die Laterne ist eine **Feuerquelle** für Teil B3 (trocknet den Spieler in ihrem Radius).

**Acceptance C (Screenshot):** `world_lantern.png` — Interior/Exterior abgedunkelt, warmer
Lichtkegel um den Spieler.

---

## Teil D — Beer Goggles: immer wenn man einen zu viel gehoben hat

- `BeerGoggleFilter` existiert (`var drunkLevel`, `var time`; Blur + Warmth + Sway → „hässliche
  Figuren werden hübsch"). Nur konsumieren.
- Neuer Zustand: `drunkLevel` steigt beim „Trinken". Konkreter Trigger: Interaktion (E) mit dem
  **Barkeep**-NPC erhöht `drunkLevel` um einen Schritt (z.B. +0.34, clamp 1.0). Über Zeit nüchtert
  man langsam aus (`drunkLevel -= soberRate * dt`, clamp 0).
- `drunkLevel` treibt `beerGoggleFilter.drunkLevel`. Erweitere dafür `ShaderStateBinder` um
  `applyDrunk(level: Float)` (attach/detach BeerGoggle analog zur Poison-Logik) — das ist die
  EINZIGE erlaubte Änderung an `ShaderStateBinder.kt`.
- **Hinweis Filter-Konflikt:** Pressure-Poison und Beer-Goggle können kollidieren (beide wollen
  `mapView.filter`). Regel: Beer-Goggle hat Vorrang solange `drunkLevel > 0` (Rausch übertönt
  Bürokratie-Übelkeit); sonst Poison nach Pressure. Dokumentiere die Wahl im Code-Kommentar.

**Acceptance D (Screenshot):** `world_drunk.png` — Interior mit deutlichem Beer-Goggle-Effekt
(weicher, warmer, leicht schräger Bild).

---

## Teil E — Das Questbook in seiner ganzen Pracht

Das zentrale Artefakt des Spiels endlich sichtbar als eigener, vollwertiger Screen.

### `QuestbookScreen.kt` (neue `Scene`)
- Vollbild-Buch-Ansicht: aufgeschlagenes Buch (zwei Seiten), pergamentfarben, mit Rahmen/Bindung
  in der Mitte. Nutze KorGE-Primitive (RoundRect/Image/Text) — kein externes Asset nötig; wenn ein
  passendes Buch-Sprite in `assets/` existiert, gern nutzen (suchen, nicht erfinden).
- **Inhalt aus echten Daten:** Die Questbook-Einträge kommen aus dem `QuestbookProcessor.log`
  (`List<QuestbookReaction>`, Feld `.questbookText`). Da der Log map-lokal ist, übergib der Scene
  die aktuellen Einträge + aktive Marker + `partyName` über ein `companion object` (wie
  `WorldScene.pendingConfig`). Linke Seite: registrierte Quests (questbookText), rechte Seite:
  aktive Marker + Party-Name + Pressure.
- **Animationen (Pflicht, soweit mit KorGE-Tweens machbar):**
  - **Aufklappen** beim Öffnen: Buch skaliert/„entfaltet" von klein→groß (scaleX 0→1 oder
    Rotation-Effekt) über ~0.4 s (`tween`).
  - **Umblättern:** Pfeiltasten ←/→ blättern Seiten; eine Seite „klappt" über (scaleX-Flip einer
    Seiten-Hälfte als simple Page-Turn-Anmutung) über ~0.3 s.
  - Sanftes Idle: leichtes Schweben/Flackern der Seitenränder (optional).
  Nutze `korlibs.korge.tween.tween` + `korlibs.time` Easing. Verifiziere die Tween-API gegen die
  KorGE-Sources.
- **Öffnen/Schließen:** In `WorldScene` Taste **`J`** (Journal) → `QuestbookScreen` (Q ist im
  Battle belegt, daher J). In `QuestbookScreen` schließt `J` oder `ESC` zurück zu `WorldScene`
  (über `WorldScene.pendingConfig` den aktuellen Map-State erhalten — KEINE Map neu-spawnen mit
  falschem Spawn; nutze die bestehende `pendingConfig`).

**Acceptance E (Screenshot):**
- `questbook_open.png` — das aufgeschlagene Buch mit echten Quest-Einträgen, Markern, Party-Name.
  (Der Screenshot zeigt den Endzustand der Aufklapp-Animation.)

---

## Screenshots (ScreenshotHarness.kt)

**KRITISCH — B007:** `private val OUT = localCurrentDirVfs["build/screenshots"]` + dessen Import
NICHT ändern (viertes Mal als DO_NOT_TOUCH).

Sechs neue Captures ans Ende anfügen + in `fun main()` registrieren:
```kotlin
captureWorldRainPuddles()
captureWorldPuddleDrain()
captureWorldLantern()
captureWorldDrunk()
captureQuestbookOpen()
// (battle/world bestehende bleiben unverändert)
```
Für reproduzierbare Frames: Shader-`time` auf einen festen Wert (z.B. 1.5f) setzen, WaterGrid mit
festen Ständen vorbefüllen (kein Random), Tween-Endzustand direkt setzen statt zu animieren.

---

## SCOPE

```
modify:
  - game/src/desktopMain/kotlin/game/WorldScene.kt
  - game/src/desktopMain/kotlin/game/ShaderStateBinder.kt   (NUR: applyDrunk(level) ergänzen)
  - game/src/desktopMain/kotlin/game/ScreenshotHarness.kt   (NUR: 6 Captures anfügen + registrieren)

create:
  - core/src/commonMain/kotlin/rpg/weather/*           (WaterGrid.kt, WetnessState.kt)
  - core/src/commonTest/kotlin/rpg/weather/*           (WaterGridTest.kt, WetnessStateTest.kt)
  - game/src/desktopMain/kotlin/game/WaterOverlay.kt
  - game/src/desktopMain/kotlin/game/QuestbookScreen.kt
  - briefs/2026-06-28-korge-step7d-water-light-drunk-questbook-result.md
```

## DO_NOT_TOUCH

```
- game/src/desktopMain/kotlin/game/ScreenshotHarness.kt → localCurrentDirVfs-Zeile + Import (B007)
- game/src/desktopMain/kotlin/game/shader/             bestehende Shader NUR konsumieren, NICHT ändern
- core/ bestehende Packages (rpg.bark, rpg.questbook, rpg.combat, rpg.tiled, …)  NUR konsumieren
        (NEU erlaubt: ausschließlich rpg.weather)
- composeApp/
- game/src/desktopMain/kotlin/game/HudOverlay.kt / QuestbookOverlay.kt / BattleScene.kt
- game/src/desktopMain/kotlin/game/CharacterSprite.kt / SpriteLoader.kt / NpcDefinition.kt
- settings.gradle.kts
- docs/KNOWN_BUGS.md   nur lesen
```

---

## ACCEPTANCE (gesamt)

```bash
./gradlew :core:desktopTest                  → BUILD SUCCESSFUL (inkl. rpg.weather-Tests)
./gradlew :game:compileKotlinDesktop         → BUILD SUCCESSFUL
./gradlew :composeApp:compileKotlinDesktop   → BUILD SUCCESSFUL
bash scripts/setup-gl.sh; ./gradlew :game:screenshot   → 15 bestehende + 6 neue = 21 PNGs
```
Neue PNGs müssen den Effekt DEUTLICH zeigen (kein schwarzes Rechteck, kein leeres Buch). Bei
GL-„Too many callbacks" einmal wiederholen (bekanntes Headless-Timing).

---

## Kontext / Querverweise

- **KNOWN_BUGS B007:** `localCurrentDirVfs` korrekt lassen.
- **KNOWN_BUGS Step 5b:** `SolidRect` ist Leaf-View (keine Kinder); Text kein Auto-Wrap.
- **KNOWN_BUGS Step 3:** `addUpdater { dt -> }` Param ist `kotlin.time.Duration`; `dt.seconds`.
- **Shader-Knöpfe:** `BeerGoggleFilter.drunkLevel` (0..1), `LightingFilter.ambientDarkness` +
  `lights`/`tilePixelSize` via `ShaderEffects.attachLighting`, `RainFilter.intensity/wind`.
  `LightSource` ist in Tile-Koordinaten.
- **Questbook-Daten:** `SliceDirector` exponiert `questbook` (`QuestbookProcessor`, Feld `.log`),
  `questMarkers`, `falseMarkers`, `partyName`, `pressure`. Alles vorhanden — nur konsumieren.
- **Determinismus:** Für Screenshots `SliceDirector { 0L }`, feste WaterGrid-Stände, feste Shader-`time`.
- **Filter-Komposition / Tween-API:** gegen `korge-6.0.0-sources.jar` verifizieren (KorGE ist die
  einzige erlaubte Code-Dependency, nur als Referenz lesen — KEIN Fremdcode ins Repo, siehe
  `docs/KORGE_MIGRATION_PLAN.md` Donor-Policy).
- **Nicht in diesem Brief (Folge-Briefs):** Schnee (#12–13), Gras/Bäume (#14–15), Nebel (#17),
  Tag/Nacht (#6), AUDIOMANCER, sowie aufwändigere Page-Turn-3D-Animation. Tropfspuren auf
  Holzböden („Nässe wird sozial gefährlich") als späterer Bark-Trigger.
```
