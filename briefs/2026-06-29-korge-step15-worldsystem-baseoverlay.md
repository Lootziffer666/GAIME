# Brief: Step 15 — WorldSystem-Register + BaseOverlay (Architektur-Spine VOR Pfeiler 2)

**MODELL: Opus-only** (vor Start Modell prüfen — `.kiro/steering/handoff-protocol.md`).
**Pflichtlektüre VOR der Arbeit:**
- `docs/ENGINE_VISION.md` — Pfeiler 1 steht; dies ist das **Fundament für Pfeiler 2**.
- `.claude/skills/gaime-shaders/SKILL.md` — Overlay-Fallen (Alpha-Floor `110+depth*140`,
  Off-Camera-Platzierung), „render ≠ logic — PNG ansehen", B007, B004, Step-13-Skala-Regel.
- `docs/KNOWN_BUGS.md` — vor Arbeit lesen, neue Funde ergänzen.

**Datum:** 2026-06-29
**Branch:** `kiro/korge-step15-worldsystem-baseoverlay`
**BASE_SHA:** `d8383877`

---

## Warum (aus dem Code bewiesen, vom Owner)

Zwei strukturelle Schulden, die **vor** Pfeiler 2 (sichtbare Physik) bezahlt werden müssen,
sonst skaliert das Chaos mit jedem der 40 Systeme:

1. **Overlay-Verdopplung.** 8 Overlay-Dateien (`Water/Snow/Blood/Footprint/Spring/Summer/
   Autumn/MaterialFatigue`, 59–93 Z.) sind **dasselbe Muster**: `ctor(parent, tileW, tileH)`,
   gepoolte `SolidRect`-Liste, `update(grid)` → über Zellen iterieren → farbige Rects mit
   Alpha-Floor. Bei 40 Systemen → 40 handgeschriebene Dateien. Keine portierbar.
2. **Inline-Spaghetti + Modulbruch.** `WorldScene` (595 Z.) mischt Physik-Ticks, Gameplay-
   Logik und Render in **einer** Funktion. Schlimmer: **Gameplay-Logik steckt in `:game`**
   (Drunk→HP-Schaden, Gold-Diebstahl, Stumble) statt im testbaren `:core`. Würde DoodleWorldScene
   diese Inline-Wachstumskurve erben, ist es in 3 Wochen das neue WorldScene.

**Dieser Brief baut die Spine, an der Pfeiler 2 nur noch andockt:** ein System = `tick()` in
`:core` (rein, getestet) + `render()` via **`BaseOverlay`** in `:game`, von einer **Registry**
iteriert. Danach ist „11 weitere Systeme sichtbar machen" reine Wiederholung, kein Neubau.

Leitsatz (Owner-Haltung #5): **„Braucht das einen Bildschirm? Nein → `:core`. Ja → `:game`."**

---

## Teil A — `:core`: WorldSystem-Interface + Logik-Heimholung (getestet)

Neues Package `core/src/commonMain/kotlin/rpg/systems/*`.

1. **`WorldContext`** — schmale Schnittstelle, über die Systeme auf den Spielzustand wirken,
   ohne den Renderer zu kennen:
   ```kotlin
   interface WorldContext {
       val player: Combatant          // rpg.combat.Combatant (existiert)
       val inventory: Inventory       // rpg.items.Inventory (existiert)
       val playerCellX: Int; val playerCellY: Int
       val isPlayerIdle: Boolean
   }
   ```
2. **`WorldSystem`** — die Spine-Abstraktion:
   ```kotlin
   interface WorldSystem {
       val id: String
       fun tick(dtSeconds: Float, ctx: WorldContext)
   }
   ```
3. **Zwei repräsentative Systeme implementieren** (Beweis des Musters — die restlichen 11
   folgen in Pfeiler 2):
   - **`WaterSystem`** — besitzt eine `WaterGrid`; `tick` ruft die bestehende Simulation
     (`addRain`/`flowStep`/`evaporate`) je nach Wetter. Reine Umweltphysik.
   - **`DrunkSystem`** — besitzt eine `DrunkState`; **holt die heute in `WorldScene` inline
     liegende Orchestrierung nach `:core`**: `soberTick` aufrufen, daraus `delayedDamage` auf
     `ctx.player` anwenden, Gold-Diebstahl bei `isAsleep` über `ctx.inventory`, Stumble-Chance
     bereitstellen. Operiert nur auf `:core`-Typen → voll testbar.
4. **Tests** (`core/src/desktopTest/.../systems/*`):
   - `WaterSystem`: Regen füllt → Pfützen entstehen → trocknen ab.
   - `DrunkSystem`: Sober-Tick reduziert Level; verzögerter Schaden trifft `player.hp`;
     schlafend → Gold sinkt; nüchtern → kein Effekt. (Mind. 6 Tests.)

> Die alten `WorldScene`-Inline-Zeilen NICHT anfassen (WorldScene wird separat retiret) —
> die Logik wird in `:core` **neu beheimatet** (Mathe als Referenz übernehmen), nicht verschoben.

---

## Teil B — `:game`: `BaseOverlay` + die 8 Overlays auf Konfigs eindampfen

1. **`GridOverlay`** (neu, `game/src/desktopMain/kotlin/game/overlay/GridOverlay.kt`) — der
   eine generische Renderer. Verträgt das gesamte gemeinsame Muster:
   ```kotlin
   class GridOverlay(
       parent: Container, tileWidth: Int, tileHeight: Int,
       val sizeFraction: Float = 1f,                       // 1=ganze Zelle, 0.5=zentriert halb
       val colorOf: (value: Float, wx: Int, wy: Int) -> RGBA?,  // null → Zelle überspringen
   ) {
       fun update(width:Int, height:Int, offsetX:Int, offsetY:Int,
                  valueAt:(wx:Int,wy:Int)->Float)          // gepoolte Rects, show/hide
   }
   ```
   Enthält **einmal** korrekt: Rect-Pooling, `(wx*tileW, wy*tileH)`-Platzierung in
   Parent-(=mapView/worldLayer)-Koordinaten, `sizeFraction`-Zentrierung, Alpha-Floor-Konvention.
2. **Die 8 Overlay-Klassen auf dünne Konfigs reduzieren** — jede wird intern zu einem
   `GridOverlay` mit ihrem Farb-/Schwellen-Mapping. **Öffentliche API (Konstruktor +
   `update(grid)`-Signatur) UNVERÄNDERT lassen**, damit bestehende Aufrufer (`WorldScene`)
   ohne Änderung kompilieren. Ziel: jede Datei von 60–93 Z. → ~10–15 Z.
   - Sonderfälle, die mehr als 1 Rect/Zelle zeichnen (Spring-Blossom, Material-Cracks): per
     zweitem `GridOverlay` oder `sizeFraction`/zweitem `colorOf`-Pass lösen — **kein** visueller
     Regress (Vergleich gegen die bestehenden `spring_approach.png` etc.).

---

## Teil C — `:game`: SystemRegistry ins Unified Runtime + erste sichtbare Physik

1. **`SystemRegistry`** (`game/.../systems/SystemRegistry.kt`) — hält
   `List<WorldSystem>` (core) + je System eine optionale `() -> Unit`-Render-Bindung
   (ein `GridOverlay` + `valueAt`-Lambda auf das System-Grid). `tickAll(dt, ctx)` +
   `renderAll()`.
2. **In `DoodleWorldScene` verdrahten:** einen `WorldContext` aus dem vorhandenen
   `hero`/`inventory`/Spielerzelle bauen, eine `SystemRegistry` mit **WaterSystem + DrunkSystem**
   bestücken, im `addUpdater` `registry.tickAll(dtSec, ctx)` + `registry.renderAll()` aufrufen.
   Overlays in den **`worldLayer`** legen (skalieren/scrollen mit der Kamera — NICHT in den
   Scene-Root, sonst off-camera; Skill-Lehre 7d/8).
   - Das ist die **erste sichtbare Physik im Unified Runtime** — Pfützen + Drunk-Lesbarkeit.
   - Drunk für den Screenshot deterministisch antreiben (z.B. Startlevel setzen), kein `Random`.

**Keine** weiteren 11 Systeme hier — das ist Pfeiler 2. Hier nur das Muster + 2 Systeme als Beweis.

---

## Teil D — Screenshot-Beweis (`ScreenshotHarness.kt`)

**B007:** `localCurrentDirVfs`-Zeile + Import NICHT ändern. (Harness-Daten-Refactor = Owner-#3,
kommt mit Pfeiler 2 — hier nur EINE Capture anfügen, das 2012-Z.-Monster nicht weiter aufblähen.)

`captureUnifiedSystems()` @ `Size(2560,1440)`: DoodleWorldScene-Zustand (Taverne) mit
**sichtbaren Pfützen** (WaterSystem, Grid um die Spawn-Zelle vorgefüllt) und **lesbarem
Drunk-Zustand**. `save("unified_systems")`. Danach **PNG ansehen** — Pfützen unmissverständlich
sichtbar (Alpha-Floor!), im Kamerabild um den Spieler.

Bestehende Screenshots (`spring_approach`, `world_rain_puddles`, …) müssen **unverändert**
weiterlaufen — sie sind die Regressionsprobe für das Overlay-Eindampfen.

---

## SCOPE

```
modify:
  - game/src/desktopMain/kotlin/game/WaterOverlay.kt + SnowOverlay.kt + BloodOverlay.kt
    + FootprintOverlay.kt + SpringOverlay.kt + SummerOverlay.kt + AutumnOverlay.kt
    + MaterialFatigueOverlay.kt        (→ dünne GridOverlay-Konfigs, öffentliche API gleich)
  - game/src/desktopMain/kotlin/game/DoodleWorldScene.kt   (SystemRegistry + WorldContext verdrahten)
  - game/src/desktopMain/kotlin/game/ScreenshotHarness.kt  (1 Capture anfügen + registrieren)
create:
  - core/src/commonMain/kotlin/rpg/systems/*               (WorldContext, WorldSystem, WaterSystem, DrunkSystem)
  - core/src/desktopTest/kotlin/rpg/systems/*              (Tests für WaterSystem + DrunkSystem)
  - game/src/desktopMain/kotlin/game/overlay/GridOverlay.kt
  - game/src/desktopMain/kotlin/game/systems/SystemRegistry.kt
  - briefs/2026-06-29-korge-step15-worldsystem-baseoverlay-result.md
```

## DO_NOT_TOUCH

```
- game/.../ScreenshotHarness.kt → localCurrentDirVfs-Zeile + Import (B007)
- game/.../WorldScene.kt        (stirbt separat — Inline-Logik NICHT verschieben, in :core neu beheimaten)
- game/.../MapConfig.kt, ImageWorldDef.kt, BattleScene.kt, QuestbookScreen.kt (nur konsumieren)
- game/.../shader/*, ShaderEffects.kt, ShaderStateBinder.kt   (nicht Teil dieses Briefs)
- core/rpg/weather/*  (Grids NUR konsumieren — WaterGrid/DrunkState etc. nicht ändern)
- core/rpg/combat/Combatant.kt, core/rpg/items/Inventory.kt   (nur konsumieren)
- composeApp/, tools/mapbuilder/, assets/, settings.gradle.kts, docs/ (nur KNOWN_BUGS ergänzen)
```

> Wenn eine `:core/weather`-Klasse für `tick` eine Methode zu fehlen scheint → im Result
> begründen und nachfragen, NICHT die DO_NOT_TOUCH-Datei ändern.

---

## ACCEPTANCE

```bash
./gradlew :core:desktopTest                  → BUILD SUCCESSFUL (inkl. neuer systems-Tests)
./gradlew :game:compileKotlinDesktop         → BUILD SUCCESSFUL
./gradlew :composeApp:compileKotlinDesktop   → BUILD SUCCESSFUL
bash scripts/setup-gl.sh; ./gradlew :game:screenshot
   → inkl. unified_systems.png (2560×1440, Pfützen sichtbar)
   → ALLE bestehenden Screenshots weiterhin erzeugt, KEIN visueller Regress bei den
     Jahreszeiten-/Wasser-Overlays (spring/summer/autumn/winter/world_rain_puddles)
```
Bei GL „Too many callbacks" einmal wiederholen. Sandbox ggf. `LD_LIBRARY_PATH=/usr/lib64`.

---

## Kontext / Querverweise

- **Referenz-Shape:** `WaterOverlay.kt` (Pooling) + `SpringOverlay.kt` (Farb-Hash, Zweit-Rect).
  Der Alpha-Floor `110 + intensity*140` ist Pflicht (Skill-Lehre 7d/8) — in `GridOverlay` zentral.
- **`:core` hat schon:** `WaterGrid` (`addRain/flowStep/evaporate/get`), `DrunkState`
  (`soberTick(dt):Int`, `drink`, `stumbleChance`, `isAsleep`), `Combatant`, `Inventory`.
- **Erfolgskriterium der Spine:** Nach diesem Brief kostet „ein weiteres System sichtbar machen"
  = 1 `WorldSystem` (core, getestet) + 1 `GridOverlay`-Konfig + 1 Registry-Zeile. Das ist
  Pfeiler 2.
- **Bewusst NICHT hier:** restliche 11 Systeme (Pfeiler 2), Harness-Daten-Refactor (Owner-#3,
  mit Pfeiler 2), `WorldDef`-Konvergenz (#4, beim WorldScene-Retirement), WorldScene/composeApp-
  Löschung.
```
