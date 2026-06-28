# Brief: KorGE Migration Step 5a — Real Sprites + BattleScene + Audio

**Datum:** 2026-06-28
**Branch:** `kiro/korge-step5a-sprites-battle-audio`
**BASE_SHA:** `c0e8308ed5d2885d85a80b026959a0c0162c430a`

---

## Aufgabe

Step 4b lieferte prozedurale Platzhalter-Sprites und kein Audio. Step 5a ersetzt
die Platzhalter durch **echte HD-Assets** (CraftPix-Sprite-Sheets), fügt eine
**BattleScene** (verdrahtet mit `:core`'s `CombatEngine`) ein, und startet
**Hintergrundmusik + Kampf-SFX** via KorGE-Audio-API.

Das ist die erste Schicht des spielbaren Loops: Welt erkunden → Kampf auslösen →
Kampf durchführen → Ergebnis → zurück zur Karte.

Akzeptanzkriterium bleibt **Kompilierung**. Visuelles/Audio-Ergebnis nur lokal.

---

## SCOPE

```
modify:
  - game/src/desktopMain/kotlin/game/TiledMapScene.kt  # CharacterSprite statt PlayerSprite, Battle-Trigger
  - game/src/desktopMain/kotlin/game/Main.kt            # Musik starten
  - game/build.gradle.kts                               # falls KorGE-Audio-Artifact fehlt

create:
  - game/src/desktopMain/kotlin/game/SpriteLoader.kt
  - game/src/desktopMain/kotlin/game/CharacterSprite.kt
  - game/src/desktopMain/kotlin/game/BattleScene.kt
  - game/src/desktopMain/kotlin/game/AudioManager.kt
```

---

## DO_NOT_TOUCH

```
- core/                                           # fertig; keine Änderungen
- composeApp/                                     # noch nicht entfernt
- settings.gradle.kts
- game/src/desktopMain/kotlin/game/PlayerSprite.kt       # bleibt als historische Referenz
- game/src/desktopMain/kotlin/game/TiledMapView.kt       # unverändert
- game/src/desktopMain/kotlin/game/TilesetAtlas.kt       # unverändert
- game/src/desktopMain/kotlin/game/Hd2dStage.kt          # bleibt als historische Referenz
- assets/                                         # read-only
```

---

## Schritt 1 — `SpriteLoader.kt`

Lädt einen CraftPix-Sprite-Sheet und liefert die einzelnen Frame-Slices.

**CraftPix-Konvention für diese Sheets:**
- Alle Frames horizontal nebeneinander
- Alle Frames quadratisch: Framebreite = Framehöhe = Sheet-Höhe
- Frameanzahl = `bitmap.width / bitmap.height`
- Kein Alpha-Rand zwischen Frames (nahtlos)

```kotlin
package game

import korlibs.image.bitmap.Bitmap
import korlibs.image.bitmap.BmpSlice
import korlibs.image.bitmap.slice
import korlibs.image.format.readBitmap
import korlibs.io.file.std.resourcesVfs
import korlibs.math.geom.RectangleInt

object SpriteLoader {
    suspend fun load(assetPath: String): List<BmpSlice> {
        val bitmap = resourcesVfs[assetPath].readBitmap()
        return sliceFrames(bitmap)
    }

    fun sliceFrames(bitmap: Bitmap): List<BmpSlice> {
        val frameSize = bitmap.height
        val frameCount = bitmap.width / frameSize
        return List(frameCount) { i ->
            bitmap.slice(RectangleInt(i * frameSize, 0, frameSize, frameSize))
        }
    }
}
```

---

## Schritt 2 — `CharacterSprite.kt`

Ersetzt `PlayerSprite`. Lädt echte Sprite-Sheets von `resourcesVfs`; fällt bei
Ladefehlern auf prozedurale Bitmaps zurück (so bleibt der Compile-Check sicher).

**Asset-Pfade:**

```
Swordsman:
  assets/HD/characters/swordsman/PNG/Swordsman_lvl1/Without_shadow/
    Swordsman_lvl1_Idle_without_shadow.png
    Swordsman_lvl1_Walk_without_shadow.png
    Swordsman_lvl1_attack_without_shadow.png
    Swordsman_lvl1_Hurt_without_shadow.png
    Swordsman_lvl1_Death_without_shadow.png

Vampire:
  assets/HD/characters/vampire/PNG/Vampires1/Without_shadow/
    Vampires1_Idle_without_shadow.png
    Vampires1_Walk_without_shadow.png
    Vampires1_Attack_without_shadow.png
    Vampires1_Hurt_without_shadow.png
    Vampires1_Death_without_shadow.png
```

**Interface:**

```kotlin
package game

import korlibs.image.bitmap.*
import korlibs.korge.view.*
import korlibs.time.*

enum class SpriteAnimation { IDLE, WALK, ATTACK, HURT, DEATH }
enum class Facing { UP, DOWN, LEFT, RIGHT }

class CharacterSprite(
    parent: Container,
    private val tileWidth: Int,
    private val tileHeight: Int,
) {
    var gridX: Int = 0
    var gridY: Int = 0
    var facing: Facing = Facing.DOWN
    var pixelOffsetX: Double = 0.0   // für Anpassungen (Sprite-Größe > Kachel)
    var pixelOffsetY: Double = 0.0

    // Laden (suspend)
    suspend fun loadSwordsman()
    suspend fun loadVampire()

    // Animation steuern
    fun play(animation: SpriteAnimation, loop: Boolean = true, onDone: (() -> Unit)? = null)

    // Position: direkt aus gridX/Y * tileSize + pixelOffset
    // Facing: LEFT → scaleX = -1; RIGHT → scaleX = 1; UP/DOWN → DOWN (TODO)
}
```

**Frame-Dauer:** 120 ms pro Frame für Idle/Walk, 80 ms für Attack/Hurt.

**Death-Animation:** `loop = false` + `onDone` Callback (für BattleScene).

**Fallback:** Falls `resourcesVfs[path].readBitmap()` wirft → `SpriteLoader.sliceFrames`
auf ein prozedurales 16×16-Bitmap (wie in `PlayerSprite`) anwenden. Keine
Exception nach oben werfen.

**Wichtig (Koordinatensystem-Korrektur aus Step 4b):** `CharacterSprite` muss in
den **gleichen Container** wie `TiledMapView` eingefügt werden (d.h. `mapView`
aus `TiledMapScene`), **nicht** in den Scene-Root — so dass der Sprite mit der
Map skaliert und kamerakorrigiert wird.

---

## Schritt 3 — `AudioManager.kt`

Dünne Wrapper-Klasse um die KorGE-Audio-API. Kein eigenes Threading — KorGE
Audio ist von Haus aus suspend-freundlich.

```kotlin
package game

import korlibs.audio.sound.*
import korlibs.io.file.std.resourcesVfs

class AudioManager {
    private var musicChannel: SoundChannel? = null

    suspend fun playMusic(assetPath: String, loop: Boolean = true) {
        stopMusic()
        val sound = resourcesVfs[assetPath].readMusic()
        musicChannel = if (loop) sound.playForever() else sound.play()
    }

    fun stopMusic() {
        musicChannel?.stop()
        musicChannel = null
    }

    suspend fun playSfx(assetPath: String) {
        resourcesVfs[assetPath].readSound().play()
    }
}
```

**KorGE-Audio-API-Hinweise (6.0.0):**
- `readMusic()` für lange MP3-Tracks (Streaming), `readSound()` für kurze WAV-SFX (in Memory).
- `SoundChannel.stop()` stoppt sofort.
- `sound.play()` und `sound.playForever()` sind suspend-kompatibel (können aus
  `sceneInit` oder `addUpdater` aufgerufen werden — wobei `addUpdater` selbst
  nicht suspend ist; Audio-Calls in `sceneInit` platzieren oder via
  `launchImmediately` / `launch` im Scope der Scene).

---

## Schritt 4 — `BattleScene.kt`

**Konzept:** Side-by-side Kampfansicht — Swordsman links, Vampire rechts.
Rundenbasiert, getrieben von `:core`'s `CombatEngine`. Kein Tilemap-Hintergrund
(schwarzer Hintergrund ausreichend für diese Stufe).

**Combatants:**

```kotlin
val hero   = Combatant(name = "Nib",  hp = 80, maxHp = 80, attackPower = 12, isPartyMember = true)
val vampire = Combatant(name = "Vampire", hp = 60, maxHp = 60, attackPower = 8, isPartyMember = false)
val engine  = CombatEngine(party = listOf(hero), enemies = listOf(vampire))
```

**Layout (virtual 640×360):**

```
[Swordsman sprite, 3×]  ——  [HP bars]  ——  [Vampire sprite, 3× + flipH]
                          [Taste: ENTER=Attack, E=Heal, Q=Zurück zur Karte]
```

**Schritte pro Runde:**
1. Spieler drückt ENTER (oder SPACE) → `engine.tick(CombatAction.ATTACK)`
2. Events auswerten: für `CombatEvent.HERO_HURT` → Swordsman `play(HURT)`,
   für `CombatEvent.ENEMY_HURT` → Vampire `play(HURT)`, SFX abspielen
3. `engine.result` prüfen: `VICTORY` → Vampire `play(DEATH)`, dann Text "VICTORY",
   `DEFEAT` → Swordsman `play(DEATH)`, Text "DEFEAT"
4. Q-Taste → `sceneContainer().changeTo<TiledMapScene>()`

**HP-Bars:** Einfache `solidRect`-Elemente (grün für Hero, rot für Vampire),
Breite proportional zu `hp / maxHp`.

**Animations-Synchronisation:** `play(ATTACK, loop=false, onDone = { play(IDLE) })` nach
dem Angriff.

**Audio:**
- Kampfbeginn: `audioManager.playMusic("assets/audio/music/Defiance_at_Dawn.mp3")`
- Treffer-SFX: `audioManager.playSfx("assets/audio/sfx/Minifantasy_Dungeon_SFX/...")` —
  Kiro: einen passenden WAV-Dateinamen aus dem Verzeichnis auswählen
  (`ls assets/audio/sfx/Minifantasy_Dungeon_SFX/` → am ehesten ein Attack/Hit-Sound)
- VICTORY-SFX: `audioManager.playSfx("assets/audio/sfx/Level up Pickup (Rpg).wav")`

**Wichtig:** `AudioManager` als Scene-Member halten, nicht als Singleton — so dass
`stopMusic()` beim Scene-Wechsel aufgerufen werden kann (in `sceneDestroy()`).

---

## Schritt 5 — `TiledMapScene.kt` anpassen

Nur zwei Änderungen:

1. **`PlayerSprite` → `CharacterSprite`:**
   ```kotlin
   // alt:
   val player = PlayerSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
   // neu:
   val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
   player.loadSwordsman()
   player.play(SpriteAnimation.IDLE)
   ```

2. **Kampf-Trigger:** SPACE-Taste startet `BattleScene`:
   ```kotlin
   if (keys.justPressed(Key.SPACE)) {
       audioManager.stopMusic()
       sceneContainer().changeTo<BattleScene>()
   }
   ```
   `keys.justPressed(Key.X)` ist die KorGE-6.0-API für "genau einmal im Frame
   gedrückt" (nicht kontinuierlich wie `keys.pressing`).

3. **Musik:** `audioManager.playMusic("assets/audio/music/Quest_Accepted_Unfortunately_.mp3")`
   nach dem Laden der Map.

---

## Schritt 6 — `Main.kt` (minimal)

Kein direkter Music-Start in `Main.kt` — Music liegt jetzt in den Scenes.
`Main.kt` bleibt fast unverändert; nur Kommentar aktualisieren:

```kotlin
suspend fun main() = Korge {
    sceneContainer().changeTo<TiledMapScene>()
}
```

---

## Schritt 7 — `game/build.gradle.kts` (nur falls nötig)

KorGE 6.0.0 enthält `korlibs-audio`. Falls `readMusic()`/`readSound()` nicht
gefunden wird → minimale Dependency-Ergänzung und im Result dokumentieren.

---

## ACCEPTANCE

```
./gradlew :game:compileKotlinDesktop        → BUILD SUCCESSFUL (0 errors)
./gradlew :core:desktopTest                 → BUILD SUCCESSFUL (206 Tests, unverändert)
./gradlew :composeApp:compileKotlinDesktop  → BUILD SUCCESSFUL (unverändert)
```

Kein GL-Fenster, kein Runtime-Test — reine Kompilierung genügt.

**Erwartetes visuelles/Audio-Ergebnis (nur manuell/lokal, nicht CI):**
- `TiledMapScene` zeigt Interior1.tmx, Swordsman-Sprite mit Idle-Animation läuft
- WASD/Pfeiltasten: Swordsman bewegt sich, CollisionGrid blockiert Wände
- Hintergrundmusik „Quest Accepted: Unfortunately!" spielt
- SPACE → Übergang zu `BattleScene`
- `BattleScene`: Swordsman links, Vampire rechts, HP-Bars
- ENTER: Angriff-Animation, Treffer-SFX, HP-Bars aktualisieren
- Tod-Animation bei HP=0, dann VICTORY/DEFEAT-Text
- Q → zurück zu `TiledMapScene`

---

## Doku-Pflicht nach Abschluss

- `docs/KORGE_MIGRATION_PLAN.md` → Step 5a als ✅ markieren (Zwischenstufe vor Step 5)
- `docs/KNOWN_BUGS.md` → KorGE-Audio-Pitfalls + alle anderen Findings
- `briefs/2026-06-28-korge-step5a-real-sprites-battle-audio-result.md` → Result-Report

---

## Kontext

**Koordinatensystem-Korrektur (Step 4b Fix):**
`CharacterSprite` muss in `mapView` eingefügt werden, nicht in den Scene-Root.
In `TiledMapScene.kt` ist `mapView` eine lokale Variable; übergib sie als
Container-Argument: `CharacterSprite(mapView, ...)`.
Beim Wechsel zu `BattleScene` ist `mapView` nicht mehr relevant.

**`:core`-APIs die direkt verwendet werden:**
- `rpg.combat.Combatant(name, hp, maxHp, attackPower, isPartyMember: Boolean)`
- `rpg.combat.CombatEngine(party, enemies)`
- `rpg.combat.CombatEngine.tick(action: CombatAction): List<CombatEvent>`
- `rpg.combat.CombatAction.ATTACK`, `.DODGE`, `.HEAL`
- `rpg.combat.CombatResult.ONGOING`, `.VICTORY`, `.DEFEAT`
- `rpg.combat.CombatEvent` — sealed class, relevante Subtypen für Hurt/Death
- `rpg.music.MusicTrack` — Enum mit `resourceBase` (z.B. `MusicTrack.TITLE_QUEST_ACCEPTED.resourceBase`)

**KorGE-6.0-APIs (bereits in Schritten 3–5 genutzt):**
- `korlibs.korge.scene.Scene`, `SContainer`, `sceneContainer()`, `changeTo<>()`
- `korlibs.korge.view.*` — `Container`, `image()`, `solidRect()`, `text()`, `addUpdater`
- `korlibs.event.Key` — `keys.pressing()`, `keys.justPressed()`
- `korlibs.image.bitmap.*`, `korlibs.image.format.readBitmap`
- `korlibs.io.file.std.resourcesVfs`
- `korlibs.audio.sound.*` — `readMusic()`, `readSound()`, `SoundChannel`

**Donor-Policy:** KorGE ist die einzige Code-Dependency; keine anderen Bibliotheken.
`resourcesVfs` löst auf dem JVM-Desktop-Target Pfade relativ zum Arbeitsverzeichnis auf.

**Über die CombatEngine:** `tick()` ist synchron und deterministisch. Es gibt keinen
Schaden-Typen, den man separat tracken muss — der Schaden steht in den `CombatEvent`s.
`livingEnemies()` und `livingParty()` prüfen ob die Runde entschieden ist (parallel
zu `engine.result`).

**Sprite-Größe vs. Kachelgröße:** Die CraftPix-Sheets haben größere Frames als 16 px.
`pixelOffsetX/Y` in `CharacterSprite` kompensiert das, sodass der Sprite visuell
auf der richtigen Kachel steht. Empfehlung: `pixelOffsetX = -(frameSize - tileWidth) / 2.0`.

**Kiro muss die exakte Frameanzahl der Sprite-Sheets nicht wissen** —
`SpriteLoader.sliceFrames()` leitet sie automatisch aus `bitmap.width / bitmap.height` ab.
