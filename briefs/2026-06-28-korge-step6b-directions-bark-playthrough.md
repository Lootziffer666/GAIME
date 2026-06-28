# Brief: Step 6b/c/d — Richtungs-Sprites, Bark-Pipeline, Scripted Playthrough

**Datum:** 2026-06-28
**Branch:** `kiro/korge-step6b-directions-bark-playthrough`
**BASE_SHA:** `57e8da37`

---

## Hintergrund

Step 6a (Audio-Migration) ist in main. Dieser Brief liefert die drei Deliverables,
die Kiro in PR#38 behauptet aber NICHT implementiert hat:

- **6b** — Richtungs-Sprite-Reihen (SpriteLoader + CharacterSprite)
- **6c** — Bark-Pipeline mit Audio in `:game` (SliceDirector + GameAudioPlayer)
- **6d** — Scripted Playthrough im ScreenshotHarness (5 neue Captures)

**Wichtig vor dem Start:**
```bash
git fetch origin main
git checkout -b kiro/korge-step6b-directions-bark-playthrough origin/main
git log --oneline -3   # soll 57e8da37 ganz oben zeigen
```

---

## Aufgabe

### 6b — Richtungs-Sprite-Reihen

CraftPix-Sheets sind 4-Zeilen-Grids (B005). Bisher schneiden wir immer Reihe 0.
Jetzt soll `CharacterSprite` die korrekte Reihe je nach Facing nutzen.

**Reihen-Konvention für CraftPix-Swordsman/Vampire (verifiziert durch Screenshot):**
```
Reihe 0 → DOWN  (Front)
Reihe 1 → LEFT  (Seite links)
Reihe 2 → UP    (Rücken)
Reihe 3 → RIGHT (Seite rechts — oft scaleX-Spiegelung von Reihe 1)
```
**Achtung:** Falls Reihe 3 nicht existiert (Sheet hat nur 3 Reihen), LEFT-Reihe +
`scaleX = -1.0` als Fallback. Falls Sheet hat nur 1 Reihe → scaleX-Flip wie bisher.
Kiro muss die tatsächliche Reihenfolge per Screenshot verifizieren und dokumentieren.

#### `SpriteLoader.kt` — Änderungen:

```kotlin
/**
 * Slices ALL rows of a CraftPix grid sheet.
 * Returns List<row> where each row = List<BmpSlice> (left-to-right frames).
 * Row count = bitmap.height / frameSize (coerced to ≥ 1).
 */
fun sliceAllRows(bitmap: Bitmap, frameSize: Int = DEFAULT_FRAME_SIZE): List<List<BmpSlice>> {
    if (bitmap.height <= 0 || bitmap.width <= 0) return listOf(listOf(bitmap.slice()))
    val fs = if (frameSize <= 0 || frameSize > bitmap.height) bitmap.height else frameSize
    val rowCount = (bitmap.height / fs).coerceAtLeast(1)
    val colCount = (bitmap.width / fs).coerceAtLeast(1)
    return List(rowCount) { row ->
        List(colCount) { col ->
            bitmap.slice(RectangleInt(col * fs, row * fs, fs, fs))
        }
    }
}

/**
 * Convenience: load a sheet and return all rows.
 * Fallback: single row with the procedural fallback frame.
 */
suspend fun loadAllRows(assetPath: String, frameSize: Int = DEFAULT_FRAME_SIZE): List<List<BmpSlice>> {
    return try {
        val bitmap = resourcesVfs[assetPath].readBitmap()
        sliceAllRows(bitmap, frameSize)
    } catch (_: Exception) {
        listOf(listOf(buildFallbackBitmap().slice()))
    }
}
```

`sliceFrames()` bleibt unverändert (Rückwärtskompatibilität für Harness + Tests).

#### `CharacterSprite.kt` — Änderungen:

Animations-Map ändert sich von `Map<SpriteAnimation, List<BmpSlice>>` zu
`Map<SpriteAnimation, List<List<BmpSlice>>>` wobei die innere Liste = Reihen (pro Facing).

```kotlin
// Reihen-Index nach Facing (CraftPix-Konvention — durch Screenshot verifizieren):
private fun Facing.rowIndex(): Int = when (this) {
    Facing.DOWN  -> 0
    Facing.LEFT  -> 1
    Facing.UP    -> 2
    Facing.RIGHT -> 3
}

// Frames für aktuelles Facing + Animation:
private fun framesForCurrent(): List<BmpSlice>? {
    val rows = animations[currentAnim] ?: return null
    if (rows.isEmpty()) return null
    return when {
        rows.size > facing.rowIndex() -> rows[facing.rowIndex()]
        rows.size == 1               -> rows[0]   // Single-row sheet → scaleX flip
        else                         -> rows[0]
    }
}
```

`updateFacing()` setzt bei Single-row-Sheets weiterhin `scaleX = ±1`.
Bei Multi-row-Sheets (size > 1) setzt `scaleX = 1.0` (keine Spiegelung nötig).

`loadSwordsman()` und `loadVampire()` rufen `SpriteLoader.loadAllRows(path)` statt
`SpriteLoader.load(path)` auf. `loadFromSheet()` ruft `loadAllRows(idleSheetPath)` auf.

**Keine API-Änderung nach außen** — `play()`, `startMove()`, `gridX/Y` etc. bleiben identisch.

---

### 6c — Bark-Pipeline mit Audio in `:game`

#### Neues File: `game/src/desktopMain/kotlin/game/GameAudioPlayer.kt`

Implementiert `rpg.bark.audio.AudioPlayer` (aus `:core`) für KorGE:

```kotlin
package game

import korlibs.io.file.std.resourcesVfs
import korlibs.audio.sound.readSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import rpg.bark.audio.AudioPlayer

/**
 * AudioPlayer implementation for :game using KorGE audio API.
 *
 * Bark WAVs live at assets/audio/bark/<path> since Step 6a.
 * BarkAudioRegistry.pathFor() returns paths WITHOUT the "assets/audio/" prefix
 * (e.g. "bark/barkeep/spend_some_coin_or_get_out.wav") — this class adds the prefix.
 */
class GameAudioPlayer(private val scope: CoroutineScope) : AudioPlayer {
    private var currentJob: Job? = null

    override fun play(resourcePath: String, onComplete: (() -> Unit)?) {
        stop()
        currentJob = scope.launch {
            try {
                val fullPath = "assets/audio/$resourcePath"
                resourcesVfs[fullPath].readSound().play().await()
                onComplete?.invoke()
            } catch (_: Exception) { /* no audio in CI — graceful degradation */ }
        }
    }

    override fun stop() {
        currentJob?.cancel()
        currentJob = null
    }

    override fun isPlaying(): Boolean = currentJob?.isActive == true

    override fun release() = stop()
}
```

#### `NpcDefinition.kt` — Neues Feld:

```kotlin
data class NpcDefinition(
    val tileX: Int,
    val tileY: Int,
    val idleSheetPath: String?,
    val facing: Facing = Facing.DOWN,
    val dialog: List<DialogLine>,
    val barkEvent: rpg.bark.BarkEvent? = null,   // NEU: Bark der beim Interagieren feuert
)
```

#### `MapConfig.kt` — BarkEvents verdrahten:

```kotlin
// Interior — Barkeep:
NpcDefinition(..., barkEvent = rpg.bark.BarkEvent.BARKEEP_SPEND_SOME_COIN)

// Interior — Patron:
NpcDefinition(..., barkEvent = rpg.bark.BarkEvent.PATRON_HE_SURE_IS_SLOW)

// Exterior — Guard:
NpcDefinition(..., barkEvent = rpg.bark.BarkEvent.GUARD_BACK_ALREADY)

// Exterior — Traveler (kein passender BarkEvent):
NpcDefinition(..., barkEvent = null)
```

#### `WorldScene.kt` — SliceDirector einbinden:

```kotlin
// Imports hinzufügen:
import rpg.SliceDirector
import rpg.bark.audio.BarkAudioPlayer
import rpg.questbook.RoomContext

// In sceneMain(), nach Inventory:
val director = SliceDirector { System.currentTimeMillis() }
director.barkAudioPlayer = BarkAudioPlayer(GameAudioPlayer(this))  // Scene = CoroutineScope

// enterRoom() beim Laden der Szene aufrufen:
val roomId = if (config.id == MapId.INTERIOR) RoomContext.ROOM_TAVERN else "exterior"
director.enterRoom(RoomContext(mapId = config.id.name.lowercase(), roomId = roomId))

// E-key NPC Interaction: nach dialog.show() den Bark feuern:
if (npc != null) {
    dialog.show(npc.first.dialog)
    npc.first.barkEvent?.let { event ->
        launch { director.fireBark(event) }
    }
    return@addUpdater
}
```

**Keine SliceDirector-Integration in den Bewegungs- oder SPACE-Battle-Trigger.**
Die Questbook-Reaktionen aus `director.fireBark()` werden in dieser Phase NICHT
in der UI angezeigt — das ist Step 7b/c. Hier reicht: Bark feuert + Audio spielt.

#### `BattleScene.kt` — SliceDirector für Combat-Barks:

```kotlin
// Imports:
import rpg.SliceDirector
import rpg.bark.audio.BarkAudioPlayer

// In sceneMain():
val director = SliceDirector { System.currentTimeMillis() }
director.barkAudioPlayer = BarkAudioPlayer(GameAudioPlayer(this))
director.startCombat(engine)

// Attack: ersetze engine.tick() durch director.combatAction()
val turn = director.combatAction(CombatAction.Attack(target.id))
// turn.events und turn.result auswerten statt engine.result direkt
acted = true

// Heal:
val turn = director.combatAction(CombatAction.Heal)
acted = true

// result check: benutze turn.result statt engine.result
when (turn.result) {
    CombatResult.VICTORY -> { ... }
    CombatResult.DEFEAT  -> { ... }
    CombatResult.ONGOING -> { }
}
```

Zusätzlich: Wenn der Held angreift, Bark `BarkEvent.BRUGG_ATTACK` feuern:
```kotlin
launch { director.fireBark(BarkEvent.BRUGG_ATTACK) }
```

Audio-Logik für sfxAttack/sfxHit bleibt via `audioManager.playSfx()` —
BarkAudioPlayer ist für Story-Barks, AudioManager für Kampf-SFX (Schwerter, Treffer).

---

### 6d — Scripted Playthrough (ScreenshotHarness)

**KRITISCH — B007:** Die Zeile
```kotlin
private val OUT = localCurrentDirVfs["build/screenshots"]
```
darf NICHT geändert werden. Weder Import noch Wert anrühren. Diese Zeile ist
dreimal von Kiro revertiert worden (KNOWN_BUGS B007) — ab jetzt ist sie in DO_NOT_TOUCH.

5 neue Capture-Funktionen an das Ende von `ScreenshotHarness.kt` anfügen.
In `fun main()` registrieren:
```kotlin
captureInteriorDialog()
captureExteriorDialog()
captureBattleMidway()
captureBattleVictory()
```

(Eine 5. Capture ist freigestellt — z.B. `captureExteriorEntry` = Spawn-Moment im Exterior.)

**`captureInteriorDialog()`** — Interior-Map, Barkeep-Dialog offen:
- Gleicher Setup wie `captureWorld(interior, ...)` aber mit `dialog.show(barkeepLines)` davor
- Dialog-Zeile 0 sichtbar ("Spend some coin or get out.")
- Screenshot: `"interior_dialog"`

**`captureExteriorDialog()`** — Exterior-Map, Guard-Dialog offen:
- Gleicher Setup wie `captureWorld(exterior, ...)` aber mit `dialog.show(guardLines)` davor
- Dialog-Zeile 0 sichtbar ("Who goes there?")
- Screenshot: `"exterior_dialog"`

**`captureBattleMidway()`** — Combat nach 2 Angriffen:
- Hero: 80 HP (unverletzt), Vampire: 60 − (12 × 2) = 36 HP → Vampire-HP-Bar halb leer
- Statt KI-Logik: direkt Combatant-Objekte mit diesen HP erstellen und nur Bars + Labels rendern
- Screenshot: `"battle_midway"`

**`captureBattleVictory()`** — Combat nach Sieg:
- Vampire: 0 HP, VICTORY!-Text sichtbar (gelb, mittig)
- Screenshot: `"battle_victory"`

Alle Captures in `korgeScreenshotTest(Size(640.0, 360.0))`. Gleiche Patterns wie bestehende
`captureWorld`/`captureBattle`. Keine neue Utility-Klasse nötig.

---

## SCOPE

```
modify:
  - game/src/desktopMain/kotlin/game/SpriteLoader.kt
  - game/src/desktopMain/kotlin/game/CharacterSprite.kt
  - game/src/desktopMain/kotlin/game/NpcDefinition.kt
  - game/src/desktopMain/kotlin/game/MapConfig.kt
  - game/src/desktopMain/kotlin/game/WorldScene.kt
  - game/src/desktopMain/kotlin/game/BattleScene.kt
  - game/src/desktopMain/kotlin/game/ScreenshotHarness.kt

create:
  - game/src/desktopMain/kotlin/game/GameAudioPlayer.kt
  - briefs/2026-06-28-korge-step6b-directions-bark-playthrough-result.md
```

## DO_NOT_TOUCH

```
- game/src/desktopMain/kotlin/game/ScreenshotHarness.kt  Zeile mit localCurrentDirVfs — NICHT ANRÜHREN (B007)
- core/                                                   KEINE Änderungen in :core
- composeApp/                                             KEINE Änderungen in :composeApp
- game/src/desktopMain/kotlin/game/shader/               Shader-Dateien unberührt lassen
- settings.gradle.kts
- docs/KNOWN_BUGS.md                                      (nur lesen, nicht ändern)
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
bash scripts/setup-gl.sh   # falls nicht schon installiert
./gradlew :game:screenshot
```
Erwartet: 8 PNGs in `build/screenshots/`:
- `interior.png`, `exterior.png`, `battle.png`  (bestehend, unverändert)
- `interior_dialog.png`, `exterior_dialog.png`, `battle_midway.png`, `battle_victory.png`
- (+ optionale 8. Capture falls 6d eine 5. hat)

Alle PNGs müssen sinnvoll aussehen (kein schwarzes Rechteck, kein Fallback-Sprite als einziger Inhalt).

---

## Kontext / Querverweise

- **KNOWN_BUGS B005:** CraftPix-Sheets sind 4-Reihen-Grids, nicht Einzel-Reihen.
- **KNOWN_BUGS B006:** `resourcesVfs` löst über Classpath auf; `assets/` muss auf Classpath liegen (Gradle-Task `desktopRuntime()` erledigt das — nicht anfassen).
- **KNOWN_BUGS B007:** `localCurrentDirVfs` in ScreenshotHarness ist der korrekte Fix — wird revertiert von jedem neuen Kiro-Branch. Dieser Brief listet die Zeile explizit in DO_NOT_TOUCH.
- **KNOWN_BUGS B008:** Kiro arbeitet gerne von falschen Base-Commits. `BASE_SHA: 57e8da37` verifizieren.
- **Audio-Pfade:** Bark-WAVs liegen seit Step 6a unter `assets/audio/bark/...`.
  `BarkAudioRegistry.pathFor(event)` gibt `"bark/<charakter>/<datei>.wav"` zurück (ohne Prefix).
  `GameAudioPlayer.play(path)` muss `"assets/audio/"` voranstellen.
- **SliceDirector-Konstruktor:** `SliceDirector { System.currentTimeMillis() }` — kein weiterer Parameter.
- **`director.barkAudioPlayer`:** wird nach Konstruktion gesetzt (var-Property, optional).
- **`RoomContext`:** existiert in `rpg.questbook.RoomContext`. Konstanten: `RoomContext.ROOM_TAVERN`.
  Für Exterior: `RoomContext(mapId = "exterior", roomId = "exterior")`.
- **Step 7a (Shader):** ist bereits in main. ScreenshotHarness hat bereits 5 Shader-Captures.
  Diese bleiben unberührt. Die neuen 4–5 Playthrough-Captures kommen dazu.
- **CUE-AGENT:** ist über `package.json devDependency` im Repo registriert. Claude führt es
  nach der Integration aus (nicht Kiro's Aufgabe).
