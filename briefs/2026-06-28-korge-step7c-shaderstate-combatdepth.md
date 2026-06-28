# Brief: Step 7c + Combat-Tiefe — „Die Shader SIND der State" + Boss-Encounter

**Datum:** 2026-06-28
**Branch:** `kiro/korge-step7c-shaderstate-combatdepth`
**BASE_SHA:** `633495eb`

---

## Vorbemerkung: Großer Auftrag, in einem Durchgang

Dieses Brief bündelt **zwei** zusammenhängende Features, weil sie dieselbe Pipeline nutzen
(`SliceDirector`-State → sichtbare Ausgabe). Bitte BEIDE Teile (A und B) vollständig liefern,
mit jeweils eigenem Screenshot-Beweis, bevor du den PR öffnest. Kein Teil-PR.

- **Teil A — Step 7c: Shader = State.** Die Quest-Pressure (und Combat-Zustand) treibt
  Bildschirm-Shader. Umsetzung der in `docs/SHADER_GAME_CONCEPT.md` / `docs/ONTOLOGIE_DES_SICHTBAREN.md`
  formulierten Vision: „Es gibt keinen Game-State den der Spieler nicht sieht. Die Shader SIND der State."
- **Teil B — Combat-Tiefe.** `BattleScene` bekommt einen echten Boss-Encounter
  (Rat Accountant + Phasen + Adds), wertet `CombatTurn.events` aus und macht sie sichtbar —
  über das in 7b gebaute `QuestbookOverlay` UND über die Shader aus Teil A.

Die Verbindung: Combat erzeugt Events (`BossPhaseChanged`, `AddsSummoned` …) → diese Events
treiben Shader + UI. Teil B ist der erste echte Konsument von Teil A.

**Wichtig vor dem Start:**
```bash
git fetch origin main
git checkout -b kiro/korge-step7c-shaderstate-combatdepth origin/main
git log --oneline -3   # soll 633495eb ganz oben zeigen
```

---

## Verfügbare API (nur lesen, NICHT ändern — alles existiert schon)

### Shader (`game.shader`)
```kotlin
class ShaderEffects {
    val poisonFilter: PoisonFilter        // var intensity: Float (0..1), var time: Float
    val beerGoggleFilter: BeerGoggleFilter
    val lightingFilter: LightingFilter
    val heatShimmerFilter: HeatShimmerFilter   // var intensity, var time, var frequency
    val rainFilter: RainFilter
    fun startTimeUpdater(root: Container)  // tickt time für alle Filter
    fun attachPoison(target: Container)    // target.filter = poisonFilter
    fun attachHeatShimmer(target: Container)
    fun detach(target: Container)          // target.filter = null
}
```
`PoisonFilter.intensity`: 0.0 = klar, 1.0 = maximale Desorientierung (chromatische Aberration +
Vignette-Tunnel). `HeatShimmerFilter.intensity`: sinusförmige UV-Verzerrung.

### Combat (`rpg.combat`)
```kotlin
class CombatEngine(
    party: List<Combatant>,
    enemies: List<Combatant>,
    boss: Combatant? = null,                    // boss + bossController zusammen oder beide null
    bossController: BossControllerInterface? = null,
)
class TaxCollectorController : BossControllerInterface   // parameterloser Boss-Controller (Rat Accountant)
enum class EnemyArchetype { SEWER_RAT, RAT_ACCOUNTANT, ... }   // .spawn() → Combatant (Signatur in :core prüfen)
enum class BossPhase { PHASE_1, PHASE_2 }
sealed class CombatEvent {
    data class Message(val text: String)
    data class BossPhaseChanged(val phase: BossPhase)
    data class AddsSummoned(val count: Int)
    data class BarkTriggered(val bark: BarkEvent)
}
```

### SliceDirector (`rpg`)
```kotlin
director.combatAction(action): CombatTurn     // CombatTurn(events: List<CombatEvent>, result: CombatResult)
director.pressure: QuestPressure              // LOW / MEDIUM / HIGH (steigt bei BossPhase PHASE_2 auf HIGH)
director.questMarkers / .falseMarkers: List<String>
director.startCombat(engine) / .fireBark(event): BarkOutcome
```
**Schon verdrahtet in `:core`:** `SliceDirector.handleCombatEvents()` reagiert auf
`BossPhaseChanged(PHASE_2)` mit `pressure → HIGH` + falschem Marker. D.h. nach einem
`combatAction()`-Tick spiegelt `director.pressure` die Boss-Phase wider — das ist der Hebel
für die Shader.

### QuestbookOverlay (`game`, aus 7b)
```kotlin
class QuestbookOverlay(parent: Container, vw: Double, vh: Double) {
    fun showReaction(reaction: QuestbookReaction, pressure: QuestPressure, markers: List<String>)
    fun refresh(pressure: QuestPressure, markers: List<String>)
    fun update(dtSeconds: Float)
}
```

---

## Teil A — Step 7c: Shader = State

### A1 — Neues File: `game/src/desktopMain/kotlin/game/ShaderStateBinder.kt`

Eine schmale, **engine-nahe** Mapping-Klasse: nimmt `ShaderEffects` + ein Ziel-`Container`
und übersetzt Spielzustand → aktiver Shader + Intensität. Single Source of Truth für die
Pressure→Shader-Abbildung, damit World und Battle dieselbe Logik nutzen.

```kotlin
package game

import korlibs.korge.view.Container
import game.shader.ShaderEffects
import rpg.questbook.QuestPressure

/**
 * Binds game state to screen shaders — the "shaders ARE the state" principle
 * (docs/SHADER_GAME_CONCEPT.md). Pressure and combat distress drive the poison
 * filter's intensity; the world destabilises visibly as bureaucratic chaos peaks.
 */
class ShaderStateBinder(
    private val effects: ShaderEffects,
    private val target: Container,
) {
    private var poisonAttached = false

    /** Maps quest pressure to a poison-shader intensity and attaches/detaches as needed. */
    fun applyPressure(pressure: QuestPressure) {
        val intensity = when (pressure) {
            QuestPressure.LOW    -> 0.0f
            QuestPressure.MEDIUM -> 0.35f
            QuestPressure.HIGH   -> 0.85f
        }
        applyPoison(intensity)
    }

    /**
     * Combat distress: hero HP fraction (1.0 = full, 0.0 = dead) adds disorientation.
     * Combined with any pressure-driven intensity (takes the max).
     */
    fun applyCombatDistress(heroHpFraction: Float, pressure: QuestPressure) {
        val fromPressure = when (pressure) {
            QuestPressure.LOW -> 0.0f; QuestPressure.MEDIUM -> 0.35f; QuestPressure.HIGH -> 0.85f
        }
        val fromHp = (1.0f - heroHpFraction) * 0.7f   // je niedriger HP, desto stärker
        applyPoison(maxOf(fromPressure, fromHp))
    }

    private fun applyPoison(intensity: Float) {
        if (intensity <= 0.01f) {
            if (poisonAttached) { effects.detach(target); poisonAttached = false }
            effects.poisonFilter.intensity = 0f
            return
        }
        effects.poisonFilter.intensity = intensity.coerceIn(0f, 1f)
        if (!poisonAttached) { effects.attachPoison(target); poisonAttached = true }
    }
}
```

### A2 — `WorldScene.kt` einbinden

1. Imports: `import game.shader.ShaderEffects`.
2. Nach dem `mapView` + `director` Setup:
   ```kotlin
   val effects = ShaderEffects()
   effects.startTimeUpdater(this)                     // tickt Shader-Zeit
   val shaderBinder = ShaderStateBinder(effects, mapView)
   shaderBinder.applyPressure(director.pressure)      // initial
   ```
3. Nach jedem `questbook.showReaction(...)` (E-Interaktion) auch:
   ```kotlin
   shaderBinder.applyPressure(director.pressure)
   ```
   So: feuert ein druck-erhöhender Bark → Pressure steigt → Welt wird sofort sichtbar „vergiftet".

**Achtung Filter-Performance:** `target.filter` auf den großen `mapView` ist ok (ein Filter).
Nicht mehrere Filter gleichzeitig stapeln. `ShaderStateBinder` nutzt nur den Poison-Filter.

### A3 — Screenshot-Beweis für Teil A
Neue Capture `captureWorldPressureHigh()` (siehe Abschnitt „Screenshots" unten): Interior-Map,
Pressure künstlich auf HIGH, Poison-Shader sichtbar (chromatische Aberration + Vignette).

---

## Teil B — Combat-Tiefe (Boss, Phasen, Adds, sichtbare Events)

### B1 — `BattleScene.kt`: Boss-Encounter-Modus

Aktuell: fixer Vampir-Kampf, `turn.events` wird ignoriert (nur `turn.result` genutzt).

Neu:
1. **Encounter-Auswahl** über `companion object`:
   ```kotlin
   companion object {
       /** true → Boss-Encounter (Rat Accountant), false → Standard-Vampir-Kampf. */
       var bossEncounter: Boolean = false
   }
   ```
   `WorldScene` setzt `BattleScene.bossEncounter = false` vor dem Standard-`changeTo` (Default bleibt
   der Vampir-Kampf, damit der bestehende SPACE-Trigger unverändert funktioniert). Der Boss-Modus wird
   in dieser Phase NUR vom Screenshot-Harness gesetzt — kein neuer In-Game-Trigger nötig.

2. **Engine-Aufbau je nach Modus:**
   ```kotlin
   val engine = if (bossEncounter) {
       val boss = EnemyArchetype.RAT_ACCOUNTANT.spawn()        // Signatur in :core prüfen
       CombatEngine(party = listOf(hero), enemies = emptyList(),
                    boss = boss, bossController = TaxCollectorController())
   } else {
       CombatEngine(party = listOf(hero), enemies = listOf(vampire))
   }
   ```
   Der Gegner-Sprite/Label im Boss-Modus heißt „The Rat Accountant" statt „Vampire" (nutze den
   Boss-Combatant für HP-Bar + Label; Sprite darf der Vampir bleiben — eigenes Boss-Sheet ist
   späteres Feature, dokumentiere das im Result).

3. **`CombatTurn.events` auswerten** (statt zu verwerfen). Nach `val turn = director.combatAction(...)`:
   ```kotlin
   for (event in turn.events) {
       when (event) {
           is CombatEvent.BossPhaseChanged -> {
               questbook.showReaction(/* synthetische Reaktion ODER */ ...,
                   director.pressure, director.questMarkers + director.falseMarkers)
               // ODER einfacher: combatToast("The Accountant files objections!")
               shaderBinder.applyCombatDistress(hero.hpFraction, director.pressure)
           }
           is CombatEvent.AddsSummoned -> combatToast("Adds summoned: ${event.count}")
           is CombatEvent.Message      -> combatToast(event.text)
           is CombatEvent.BarkTriggered -> { /* Audio läuft schon über director; optional Toast */ }
       }
   }
   ```
   `combatToast(text)` = kleine Helper-Anzeige. **Empfehlung:** Reuse `QuestbookOverlay` auch in
   `BattleScene` (es ist screen-fixed und kann an den Scene-Root). Für Messages ohne echte
   `QuestbookReaction` ergänze dem Overlay eine schlanke Methode `showMessage(text: String,
   pressure: QuestPressure)` (in `QuestbookOverlay.kt`, nutzt denselben Toast wie `showReaction`,
   nur ohne Marker-Update). Das ist die EINZIGE erlaubte Änderung an `QuestbookOverlay.kt`.

4. **Shader im Battle:** `ShaderEffects` + `ShaderStateBinder` auf den Scene-Root (oder ein Battle-
   Container). Nach jedem `acted`-Tick:
   ```kotlin
   shaderBinder.applyCombatDistress(hero.hpFraction, director.pressure)
   ```
   Effekt: Held verliert HP ODER Boss erreicht PHASE_2 (→ pressure HIGH) → Bildschirm „vergiftet" sich.

   **Filter-Ziel:** Lege die Kampf-Inhalte (Sprites, Bars, Text) in EINEN Container und filtere den.
   Wenn das zu invasiv ist, filtere den Scene-Root `this`. HP-Bars/Text dürfen mit-verzerrt werden —
   das ist gewollt (der ganze Bildschirm ist der State). `startTimeUpdater(this)` nicht vergessen.

### B2 — Screenshot-Beweis für Teil B
Neue Capture `captureBattleBossPhase2()`: Boss-Encounter, künstlich in PHASE_2 versetzt
(Boss-HP unter 50 % via `takeDamage`, einen Tick laufen lassen ODER Pressure direkt auf HIGH +
distress anwenden), Poison-Shader sichtbar + Boss-Toast/Marker. Vampir-Standardkampf-Captures
(`battle_midway`, `battle_victory`) bleiben unverändert.

---

## Screenshots (ScreenshotHarness.kt)

**KRITISCH — B007:** `private val OUT = localCurrentDirVfs["build/screenshots"]` und der Import
`import korlibs.io.file.std.localCurrentDirVfs` NICHT ändern. (Viertes Mal als DO_NOT_TOUCH gelistet.)

Zwei neue Captures ans Ende anfügen + in `fun main()` registrieren:
```kotlin
captureWorldPressureHigh()
captureBattleBossPhase2()
```

- **`captureWorldPressureHigh()`** — Interior-Aufbau wie `captureQuestbookReaction()`. Einen
  `ShaderEffects` + `ShaderStateBinder` auf `mapView` legen, `applyPressure(QuestPressure.HIGH)`,
  einen Frame rendern. (Da der Harness statisch rendert: `poisonFilter.time = 1.5f` setzen, damit
  die Animation einen sichtbaren Zustand hat.) Save `"world_pressure_high"`.
- **`captureBattleBossPhase2()`** — Boss-Encounter-Aufbau, `applyCombatDistress` mit z.B.
  `heroHpFraction = 0.4f, pressure = HIGH`, `poisonFilter.time = 1.5f`, Boss-Toast sichtbar.
  Save `"battle_boss_phase2"`.

Beide in `korgeScreenshotTest(Size(VW, VH))`, gleiche Patterns wie bestehende Captures.

---

## SCOPE

```
modify:
  - game/src/desktopMain/kotlin/game/WorldScene.kt
  - game/src/desktopMain/kotlin/game/BattleScene.kt
  - game/src/desktopMain/kotlin/game/QuestbookOverlay.kt   (NUR: showMessage(text, pressure) ergänzen)
  - game/src/desktopMain/kotlin/game/ScreenshotHarness.kt  (NUR: 2 Captures anfügen + registrieren)

create:
  - game/src/desktopMain/kotlin/game/ShaderStateBinder.kt
  - briefs/2026-06-28-korge-step7c-shaderstate-combatdepth-result.md
```

## DO_NOT_TOUCH

```
- game/src/desktopMain/kotlin/game/ScreenshotHarness.kt  → localCurrentDirVfs-Zeile + Import (B007)
- game/src/desktopMain/kotlin/game/shader/               Shader-GLSL fertig — NUR konsumieren, nicht ändern
- core/                          Combat-/Pressure-/Boss-Logik ist komplett — NUR konsumieren
- composeApp/                    Throwaway
- game/src/desktopMain/kotlin/game/HudOverlay.kt
- game/src/desktopMain/kotlin/game/SpriteLoader.kt / CharacterSprite.kt / MapConfig.kt / NpcDefinition.kt
- settings.gradle.kts
- docs/KNOWN_BUGS.md             nur lesen
```

---

## ACCEPTANCE

```bash
./gradlew :core:desktopTest                  → BUILD SUCCESSFUL
./gradlew :game:compileKotlinDesktop         → BUILD SUCCESSFUL
./gradlew :composeApp:compileKotlinDesktop   → BUILD SUCCESSFUL
```

**Visueller Beweis (PFLICHT):**
```bash
bash scripts/setup-gl.sh   # falls nötig
./gradlew :game:screenshot
```
Erwartet: die bestehenden 13 PNGs + **2 neue** = **15** in `build/screenshots/`:
- `world_pressure_high.png` — Welt mit Poison-Shader (chromatische Aberration + Vignette sichtbar)
- `battle_boss_phase2.png` — Boss-Kampf mit Shader-Distress + Boss-Toast/Marker

Beide müssen den Shader-Effekt DEUTLICH zeigen (nicht nur leichte Tönung) und dürfen kein
schwarzes Rechteck sein. Falls `:game:screenshot` einmalig mit GL-„Too many callbacks" abbricht:
einmal wiederholen (bekanntes Headless-GL-Timing, kein Code-Fehler).

---

## Kontext / Querverweise

- **Vision-Docs:** `docs/SHADER_GAME_CONCEPT.md`, `docs/ONTOLOGIE_DES_SICHTBAREN.md`,
  `docs/SHADER_ACTORS_AND_AUDIOMANCER.md` — die „Shader = State"-Philosophie. Dieses Brief setzt
  die erste konkrete Stufe um: Pressure/Combat-Distress → Poison-Shader.
- **KNOWN_BUGS B007:** `localCurrentDirVfs` korrekt lassen.
- **KNOWN_BUGS Step 5b:** `SolidRect` ist Leaf-View; Text kein Auto-Wrap.
- **KNOWN_BUGS Step 3:** `addUpdater { dt -> }` Param ist `kotlin.time.Duration`; `dt.seconds`.
- **`BarkOutcome`** in Package `rpg`. **`CombatEvent`/`BossPhase`/`EnemyArchetype`** in `rpg.combat`.
- **Boss-Pipeline ist in `:core` fertig:** `CombatEngine(boss=, bossController=)` +
  `TaxCollectorController` emittieren `BossPhaseChanged`/`AddsSummoned`; `SliceDirector` hebt bei
  PHASE_2 die Pressure auf HIGH. Du musst in `:game` NUR konsumieren + sichtbar machen.
- **`EnemyArchetype.spawn()`** — exakte Signatur in `core/.../rpg/combat/EnemyArchetype.kt` prüfen
  (Spawn-per-Encounter, siehe Combatant-Doku).
- **Determinismus:** gleiche Bark/gleicher Context → gleiche Reaktion. Für reproduzierbare
  Screenshots im Harness `SliceDirector { 0L }` (feste Uhr) nutzen.
- **Nächster Schritt (NICHT hier):** eigenes Boss-Sprite-Sheet für den Rat Accountant; weitere
  Shader-Zustände (BeerGoggle bei Tavernen-Szenen, Lighting in Dungeons).
