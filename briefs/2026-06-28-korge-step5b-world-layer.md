# Brief: KorGE Migration Step 5b — World Layer: Smooth Movement, NPCs, Dialog, HUD, Map Transitions

**Datum:** 2026-06-28
**Branch:** `kiro/korge-step5b-world-layer`
**BASE_SHA:** `7a570b1b`

---

## Aufgabe

Step 5a lieferte ein spielbares Grundgerüst: TiledMapScene zeigt Interior1.tmx, der Swordsman
bewegt sich kachelweise, SPACE startet BattleScene. Drei Systeme fehlen, die den Loop erst zu
einem echten Spiel machen:

1. **Smooth Movement** — Sprite teleportiert derzeit tile-for-tile. Visuell: der Charakter soll
   zwischen Kacheln gleiten (160 ms pro Schritt, wie in klassischen JRPG).
2. **NPC-System + Dialog** — NPCs stehen auf der Karte, E-Taste öffnet einen Dialogkasten mit
   Speaker-Name und Text. Welt pausiert während Dialog aktiv.
3. **Kartenübergang** — Interior1.tmx und Exterior.tmx sind zwei ladbare Locations. Einen
   konfigurierbaren Exit-Trigger implementieren, der beim Betreten einer bestimmten Kachel
   nahtlos die andere Karte lädt.
4. **HUD** — feste Overlay-Schicht mit Hero-HP-Anzeige und Gold-Counter.
5. **`WorldScene.kt`** — neue Hauptszene, die alles vereint und `TiledMapScene` als
   Einstiegspunkt ersetzt.

`TiledMapScene.kt` bleibt als historische Referenz erhalten (DO_NOT_TOUCH).

Akzeptanzkriterium bleibt **Kompilierung**. Visuell nur lokaler Lauf.

---

## SCOPE

```
modify:
  - game/src/desktopMain/kotlin/game/CharacterSprite.kt   # smooth movement + loadFromSheet
  - game/src/desktopMain/kotlin/game/BattleScene.kt       # Q → WorldScene statt TiledMapScene
  - game/src/desktopMain/kotlin/game/Main.kt              # Boot WorldScene

create:
  - game/src/desktopMain/kotlin/game/DialogLine.kt
  - game/src/desktopMain/kotlin/game/NpcDefinition.kt
  - game/src/desktopMain/kotlin/game/MapConfig.kt
  - game/src/desktopMain/kotlin/game/DialogOverlay.kt
  - game/src/desktopMain/kotlin/game/HudOverlay.kt
  - game/src/desktopMain/kotlin/game/WorldScene.kt
```

---

## DO_NOT_TOUCH

```
- core/                                          # fertig; keine Änderungen
- composeApp/                                    # Interim, throwaway
- settings.gradle.kts
- game/src/desktopMain/kotlin/game/TiledMapScene.kt   # historische Referenz
- game/src/desktopMain/kotlin/game/TiledMapView.kt
- game/src/desktopMain/kotlin/game/TilesetAtlas.kt
- game/src/desktopMain/kotlin/game/SpriteLoader.kt
- game/src/desktopMain/kotlin/game/AudioManager.kt
- game/src/desktopMain/kotlin/game/PlayerSprite.kt
- game/src/desktopMain/kotlin/game/Hd2dStage.kt
- assets/                                        # read-only
```

---

## Schritt 1 — `DialogLine.kt`

Minimales Datenmodell für eine Dialogzeile. Kein Audio in diesem Schritt — reines Text-Dialog.

```kotlin
package game

/**
 * One line of in-world dialogue.
 *
 * [speaker] — displayed bold above the text; empty string = narrator line.
 * [text]    — the dialogue text (word-wrapped by DialogOverlay).
 */
data class DialogLine(
    val speaker: String,
    val text: String,
)
```

---

## Schritt 2 — `NpcDefinition.kt`

Beschreibt einen NPC auf der Karte: Tile-Position, Sprite-Sheet-Pfad und Dialogzeilen.

```kotlin
package game

/**
 * Defines a non-player character placed at a fixed tile position on the map.
 *
 * [tileX], [tileY]    — logical grid position in the TMX map.
 * [idleSheetPath]     — asset path for the idle sprite sheet (loaded via SpriteLoader).
 *                       May be null to use a procedural fallback (purple rect).
 * [facing]            — initial facing direction (LEFT/RIGHT/UP/DOWN).
 * [dialog]            — lines shown sequentially when player interacts (E key).
 *                       Empty list = NPC is silent (no interaction prompt).
 */
data class NpcDefinition(
    val tileX: Int,
    val tileY: Int,
    val idleSheetPath: String?,
    val facing: Facing = Facing.DOWN,
    val dialog: List<DialogLine>,
)
```

---

## Schritt 3 — `MapConfig.kt`

Aggregiert alles was `WorldScene` für eine Location braucht: TMX-Pfade, Spawn, NPCs, Musik,
und Exits (Übergänge zu anderen Locations).

```kotlin
package game

/**
 * Identifies one of the two available TMX locations.
 */
enum class MapId { INTERIOR, EXTERIOR }

/**
 * A configured exit tile: stepping on (tileX, tileY) triggers a transition to
 * [destination], spawning the player at (spawnX, spawnY) on the new map.
 */
data class MapExit(
    val tileX: Int,
    val tileY: Int,
    val destination: MapId,
    val spawnX: Int,
    val spawnY: Int,
)

/**
 * Complete configuration for one map location. WorldScene reads this from
 * WorldScene.pendingConfig and never stores renderer state here.
 *
 * [id]          — which map this is (used as display name + for transitions).
 * [tmxDir]      — directory containing the TMX file and its referenced tilesets.
 * [tmxFile]     — file name of the TMX map (relative to tmxDir).
 * [spawnX/Y]    — tile coordinates where the player appears on entry.
 * [bgmPath]     — asset path for background music (passed to AudioManager.playMusic).
 * [npcs]        — NPC entities placed on this map.
 * [exits]       — exit tiles that trigger a transition to another map.
 * [displayName] — shown in the HUD location label.
 */
data class MapConfig(
    val id: MapId,
    val tmxDir: String,
    val tmxFile: String,
    val spawnX: Int,
    val spawnY: Int,
    val bgmPath: String,
    val npcs: List<NpcDefinition>,
    val exits: List<MapExit>,
    val displayName: String,
) {
    val tmxPath: String get() = "$tmxDir/$tmxFile"

    companion object {
        /**
         * Heroes' Home interior — Interior1.tmx.
         * NPCs:
         *   - Barkeep at (4, 8): Swordsman lvl2 sheet (placeholder), facing RIGHT
         *   - Citizen at (12, 16): Vampire sheet (placeholder), facing LEFT
         * Exit:
         *   - Tile (8, 1) → EXTERIOR, spawn (8, 20)
         *     (approximate north-door position; adjust if Kiro finds a better coordinate
         *      by inspecting Interior1.tmx visually)
         */
        fun interior(): MapConfig = MapConfig(
            id = MapId.INTERIOR,
            tmxDir = "assets/HD/locations/heroes-home/Tiled_files",
            tmxFile = "Interior1.tmx",
            spawnX = 8,
            spawnY = 12,
            bgmPath = "assets/audio/music/Quest_Accepted_Unfortunately_.mp3",
            displayName = "Heroes' Home",
            npcs = listOf(
                NpcDefinition(
                    tileX = 4,
                    tileY = 8,
                    idleSheetPath = "assets/HD/characters/swordsman/PNG/Swordsman_lvl2/Without_shadow/Swordsman_lvl2_Idle_without_shadow.png",
                    facing = Facing.RIGHT,
                    dialog = listOf(
                        DialogLine("Barkeep", "Spend some coin or get out."),
                        DialogLine("Barkeep", "You've been officially registered as a Hero Party. Don't ask how."),
                        DialogLine("Nib", "...by who?"),
                        DialogLine("Barkeep", "The Questbook. It fell on the desk and opened to the right page. Fate, probably."),
                    )
                ),
                NpcDefinition(
                    tileX = 12,
                    tileY = 16,
                    idleSheetPath = "assets/HD/characters/vampire/PNG/Vampires1/Without_shadow/Vampires1_Idle_without_shadow.png",
                    facing = Facing.LEFT,
                    dialog = listOf(
                        DialogLine("Patron", "He sure is slow for a four-armed bartender."),
                        DialogLine("Patron", "I hear the king likes to wear evening gowns."),
                    )
                ),
            ),
            exits = listOf(
                MapExit(tileX = 8, tileY = 1, destination = MapId.EXTERIOR, spawnX = 8, spawnY = 20),
            ),
        )

        /**
         * Heroes' Home exterior — Exterior.tmx.
         * NPCs:
         *   - Guard at (5, 10): Swordsman lvl3 sheet, facing RIGHT
         *   - Traveler at (12, 8): Vampire sheet (placeholder), facing DOWN
         * Exit:
         *   - Tile (8, 22) → INTERIOR, spawn (8, 3)
         *     (south edge of exterior → back through the house door)
         */
        fun exterior(): MapConfig = MapConfig(
            id = MapId.EXTERIOR,
            tmxDir = "assets/HD/locations/heroes-home/Tiled_files",
            tmxFile = "Exterior.tmx",
            spawnX = 8,
            spawnY = 20,
            bgmPath = "assets/audio/music/Sovereign_Heights.mp3",
            displayName = "Village Exterior",
            npcs = listOf(
                NpcDefinition(
                    tileX = 5,
                    tileY = 10,
                    idleSheetPath = "assets/HD/characters/swordsman/PNG/Swordsman_lvl3/Without_shadow/Swordsman_lvl3_Idle_without_shadow.png",
                    facing = Facing.RIGHT,
                    dialog = listOf(
                        DialogLine("Guard", "Who goes there?"),
                        DialogLine("Guard", "The forest trail east of here has been overrun by wolves."),
                        DialogLine("Guard", "If you're looking for trouble, you'll find it there."),
                        DialogLine("Nib", "Just keep to the trail."),
                    )
                ),
                NpcDefinition(
                    tileX = 12,
                    tileY = 8,
                    idleSheetPath = null, // procedural fallback
                    facing = Facing.DOWN,
                    dialog = listOf(
                        DialogLine("Traveler", "Where's the nearest inn?"),
                        DialogLine("Traveler", "I've been walking since sunrise."),
                    )
                ),
            ),
            exits = listOf(
                MapExit(tileX = 8, tileY = 22, destination = MapId.INTERIOR, spawnX = 8, spawnY = 11),
            ),
        )

        /** Returns the MapConfig for [id] using the default spawn coordinates. */
        fun forId(id: MapId): MapConfig = when (id) {
            MapId.INTERIOR -> interior()
            MapId.EXTERIOR -> exterior()
        }

        /**
         * Returns a MapConfig for [id] with overridden spawn position.
         * Used by map exits to land the player at the correct entry tile.
         */
        fun forId(id: MapId, spawnX: Int, spawnY: Int): MapConfig =
            forId(id).copy(spawnX = spawnX, spawnY = spawnY)
    }
}
```

**Hinweis Exit-Koordinaten:**
Die Koordinaten `(8, 1)` für Interior und `(8, 22)` für Exterior sind Schätzwerte basierend auf
den bekannten Map-Dimensionen. Kiro soll die Exit-Kacheln anhand der CollisionGrid-Typen oder
durch Inspektion der TMX-Dateien korrigieren, falls die Spawn-Koordinaten visuell falsch wirken.
Die Architektur (MapExit + MapConfig.exits) erlaubt triviale Anpassung ohne Strukturänderung.

---

## Schritt 4 — `CharacterSprite.kt` erweitern

Drei Erweiterungen gegenüber dem Step-5a-Stand:

### 4a — Smooth Movement

Neue interne State-Variablen und neue public API für interpolierte Bewegung:

```kotlin
// Intern (neue Felder):
private var fromGridX: Int = 0
private var fromGridY: Int = 0
private var moveProgress: Float = 1f        // 1f = idle/arrived, 0f..1f = mid-step
private val stepDurationMs: Float = 160f    // ms für einen vollständigen Schritt

// Public:
val isMoving: Boolean get() = moveProgress < 1f

/** Logical position in tile units (interpolated). Used for camera. */
val visualGridX: Double
    get() = fromGridX + (gridX - fromGridX) * moveProgress.toDouble()
val visualGridY: Double
    get() = fromGridY + (gridY - fromGridY) * moveProgress.toDouble()

/**
 * Requests a move to (toGridX, toGridY). Returns false if the sprite is
 * still mid-step (caller should not update gridX/Y in that case).
 * On success: records from-position, sets gridX/Y, resets moveProgress to 0.
 */
fun startMove(toGridX: Int, toGridY: Int): Boolean {
    if (isMoving) return false
    fromGridX = gridX
    fromGridY = gridY
    gridX = toGridX
    gridY = toGridY
    moveProgress = 0f
    return true
}
```

In `advanceAnimation(dtMs: Float)`: vor der Frame-Logik `moveProgress` vorwärts ticken:
```kotlin
if (isMoving) {
    moveProgress = (moveProgress + dtMs / stepDurationMs).coerceAtMost(1f)
    updatePosition()
}
```

`updatePosition()` nutzt `visualGridX/Y` statt `gridX/Y` für die visuelle Position:
```kotlin
private fun updatePosition() {
    img.x = visualGridX * tileWidth + pixelOffsetX
    img.y = visualGridY * tileHeight + pixelOffsetY
    // Flip für LEFT-Facing: Pivot-Korrektur anpassen wenn scaleX = -1
    if (img.scaleX < 0) img.x += img.width  // korrigiert Flip-Origin
}
```

**Wichtig:** `gridX`/`gridY` Property-Setter (`set(value) { field = value; updatePosition() }`)
bleiben erhalten, aber `updatePosition()` liest jetzt `visualGridX/Y`. Da `startMove()` erst
`fromGridX/Y` setzt und dann `gridX/Y`, und `moveProgress = 0f` heißt "noch am Ausgangspunkt",
ergibt `visualGridX = fromGridX + (gridX - fromGridX) * 0 = fromGridX` — korrekt.

**Init-Anpassung:** `fromGridX = gridX; fromGridY = gridY; moveProgress = 1f` im `init`-Block
sicherstellen.

### 4b — Generic Sheet Loader

Neue suspend-Funktion neben `loadSwordsman()` / `loadVampire()`:

```kotlin
/**
 * Loads a single idle sheet from [idleSheetPath] (required) and optionally
 * a walk sheet from [walkSheetPath]. Falls back to procedural bitmaps on error.
 * Used for NPC sprites where the exact animation set doesn't matter.
 */
suspend fun loadFromSheet(
    idleSheetPath: String?,
    walkSheetPath: String? = null,
) {
    val idleFrames = if (idleSheetPath != null) SpriteLoader.load(idleSheetPath) else SpriteLoader.sliceFrames(SpriteLoader.buildFallbackBitmap())
    animations[SpriteAnimation.IDLE] = idleFrames
    animations[SpriteAnimation.WALK] = if (walkSheetPath != null) SpriteLoader.load(walkSheetPath) else idleFrames
    // ATTACK/HURT/DEATH: copy IDLE as fallback so play() never crashes
    animations[SpriteAnimation.ATTACK] = idleFrames
    animations[SpriteAnimation.HURT] = idleFrames
    animations[SpriteAnimation.DEATH] = idleFrames
    applyFirstFrame()
}
```

### 4c — Facing dx/dy

Hinzufügen zu `Facing` (in `CharacterSprite.kt`, da dort definiert):

```kotlin
enum class Facing { UP, DOWN, LEFT, RIGHT }

val Facing.dx: Int get() = when (this) { Facing.LEFT -> -1; Facing.RIGHT -> 1; else -> 0 }
val Facing.dy: Int get() = when (this) { Facing.UP -> -1; Facing.DOWN -> 1; else -> 0 }
```

Diese Extension Properties ermöglichen `player.facing.dx` / `player.facing.dy` in WorldScene
ohne Import von `rpg.world.Direction`.

---

## Schritt 5 — `DialogOverlay.kt`

Eine KorGE-`Container`-Subklasse (oder Hilfsfunktion, die einen Container zurückgibt), die
einen Dialogkasten am unteren Bildschirmrand rendert. Während Dialog aktiv: Weltinput pausiert
(geprüft von WorldScene via `dialog.isActive`).

```kotlin
package game

import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.korge.view.*

/**
 * Full-width dialog box anchored to the bottom of the screen.
 *
 * Usage:
 *   val dialog = DialogOverlay(sceneContainer, virtualWidth, virtualHeight)
 *   dialog.show(listOf(DialogLine("Barkeep", "Spend some coin or get out.")))
 *   // In addUpdater: if (keys.justPressed(Key.RETURN)) dialog.advance()
 *   // WorldScene pauses movement input while dialog.isActive == true
 */
class DialogOverlay(
    parent: Container,
    private val vw: Double,
    private val vh: Double,
) {
    private val panelHeight = vh * 0.28       // 28 % der Bildschirmhöhe
    private val panelY = vh - panelHeight - 8.0

    private val panel: SolidRect
    private val speakerLabel: Text
    private val bodyLabel: Text
    private val promptLabel: Text

    private var lines: List<DialogLine> = emptyList()
    private var lineIndex: Int = 0

    val isActive: Boolean get() = panel.visible

    init {
        // Halbtransparentes dunkles Panel
        panel = parent.solidRect(vw - 16.0, panelHeight, RGBA(0x0a, 0x0a, 0x14, 0xdd))
            .apply { x = 8.0; y = panelY; visible = false }

        // Rahmen (einfacher solidRect-Streifen oben)
        parent.solidRect(vw - 16.0, 2.0, Colors["#886644"])
            .apply { x = 8.0; y = panelY; visible = false }
            .also { panel.addChild(it) /* bleibt unsichtbar wenn panel versteckt */ }

        speakerLabel = parent.text("", textSize = 14.0, color = Colors["#ffdd88"])
            .apply { x = 20.0; y = panelY + 10.0; visible = false }

        bodyLabel = parent.text("", textSize = 13.0, color = Colors.WHITE)
            .apply { x = 20.0; y = panelY + 30.0; visible = false }

        promptLabel = parent.text("▼ ENTER", textSize = 11.0, color = Colors["#888888"])
            .apply { x = vw - 80.0; y = panelY + panelHeight - 20.0; visible = false }
    }

    /**
     * Starts showing [newLines] from the beginning.
     * If [newLines] is empty, does nothing.
     */
    fun show(newLines: List<DialogLine>) {
        if (newLines.isEmpty()) return
        lines = newLines
        lineIndex = 0
        display(lines[0])
    }

    /**
     * Advances to the next line. Hides the overlay after the last line.
     */
    fun advance() {
        lineIndex++
        if (lineIndex >= lines.size) {
            hide()
        } else {
            display(lines[lineIndex])
        }
    }

    private fun display(line: DialogLine) {
        val hasPanel = true  // always show panel
        panel.visible = hasPanel
        speakerLabel.visible = hasPanel
        bodyLabel.visible = hasPanel
        promptLabel.visible = hasPanel

        speakerLabel.text = if (line.speaker.isNotEmpty()) line.speaker else ""
        bodyLabel.text = line.text
        // Simple word-wrap: KorGE Text wraps automatically if width is set
        // For now: plain multiline is sufficient (text may overflow on very long lines)
    }

    private fun hide() {
        panel.visible = false
        speakerLabel.visible = false
        bodyLabel.visible = false
        promptLabel.visible = false
        lines = emptyList()
        lineIndex = 0
    }
}
```

**Hinweise:**
- Das Panel ist ein Kind von `parent` (dem SContainer der Scene), nicht von `mapView` — so
  bleibt es screen-fixed und wird nicht von der Kamera verschoben.
- KorGE's `text()` rendert Zeilenumbrüche mit `\n`. Kiro kann lange Zeilen in `DialogLine`
  manuell mit `\n` umbrechen, oder die `bodyLabel.width` setzen damit KorGE selbst umbricht.
- `isActive` wird in WorldScene's `addUpdater` geprüft: wenn `true`, werden alle
  Bewegungs-/Kampf-Inputs übersprungen.

---

## Schritt 6 — `HudOverlay.kt`

Permanentes HUD in der oberen linken Ecke. Zeigt Hero-HP, Gold und Location-Namen.

```kotlin
package game

import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.korge.view.*
import rpg.combat.Combatant
import rpg.items.Inventory

/**
 * Screen-fixed HUD overlay showing hero HP, gold, and current location.
 * Must be added to the scene root (not mapView) so it stays fixed on screen.
 * Call [update] every frame to refresh the dynamic values.
 */
class HudOverlay(
    parent: Container,
    private val hero: Combatant,
    private val inventory: Inventory,
    locationName: String,
) {
    private val barMaxWidth = 80.0
    private val barHeight = 8.0

    private val hpBarBg: SolidRect
    private val hpBarFg: SolidRect
    private val hpLabel: Text
    private val goldLabel: Text
    private val locationLabel: Text

    init {
        // Semi-transparent background panel
        parent.solidRect(150.0, 48.0, RGBA(0x00, 0x00, 0x00, 0xaa))
            .apply { x = 4.0; y = 4.0 }

        // HP bar background
        hpBarBg = parent.solidRect(barMaxWidth, barHeight, Colors["#444444"])
            .apply { x = 8.0; y = 10.0 }

        // HP bar foreground
        hpBarFg = parent.solidRect(barMaxWidth, barHeight, Colors["#22cc22"])
            .apply { x = 8.0; y = 10.0 }

        // HP label: "HP 80/80"
        hpLabel = parent.text("HP ${hero.hp}/${hero.maxHp}", textSize = 10.0, color = Colors.WHITE)
            .apply { x = 8.0; y = 20.0 }

        // Gold label: "Gold: 50"
        goldLabel = parent.text("Gold: ${inventory.gold}", textSize = 10.0, color = Colors["#ffdd44"])
            .apply { x = 8.0; y = 34.0 }

        // Location label top-right
        locationLabel = parent.text(locationName, textSize = 10.0, color = Colors["#aaaaaa"])
            .apply { x = 8.0; y = 46.0 }
    }

    /**
     * Refreshes all dynamic values. Call once per frame from WorldScene's addUpdater.
     */
    fun update(locationName: String) {
        hpBarFg.width = barMaxWidth * hero.hpFraction
        hpLabel.text = "HP ${hero.hp}/${hero.maxHp}"
        goldLabel.text = "Gold: ${inventory.gold}"
        locationLabel.text = locationName
    }
}
```

---

## Schritt 7 — `WorldScene.kt`

Die neue Hauptszene. Ersetzt `TiledMapScene` als Boot-Target. `TiledMapScene` bleibt unverändert.

**Architektur:**
- `companion object { var pendingConfig: MapConfig }` — Konfiguration für den nächsten Ladevorgang.
  Beim Kartenübergang: `WorldScene.pendingConfig = nextConfig` → `changeTo<WorldScene>()`.
  Die Szene liest `pendingConfig` in `sceneMain()` und arbeitet damit.
- `hero` und `inventory` sind Scene-Member (nicht in `companion`) — werden bei jedem Szenenwechsel
  zurückgesetzt. Persistenter Spielstand ist ein späteres Feature (`:core` hat `GameSaveState` für
  dann). Für 5b ist Spielstand-Reset beim Kartenübergang akzeptabel.
- `audioManager` ist Scene-Member, `stopMusic()` in `sceneAfterDestroy()`.

**Vollständige Implementierungs-Spezifikation:**

```kotlin
package game

import korlibs.event.Key
import korlibs.io.file.std.resourcesVfs
import korlibs.korge.scene.Scene
import korlibs.korge.scene.sceneContainer
import korlibs.korge.view.SContainer
import korlibs.korge.view.addUpdater
import kotlinx.coroutines.launch
import rpg.combat.Combatant
import rpg.combat.Side
import rpg.items.Inventory
import rpg.tiled.CollisionGrid
import rpg.tiled.TileType
import rpg.tiled.TmxLoader

class WorldScene : Scene() {

    companion object {
        /** Set before calling changeTo<WorldScene>() to configure the new map. */
        var pendingConfig: MapConfig = MapConfig.interior()
    }

    private val audioManager = AudioManager()

    override suspend fun SContainer.sceneMain() {
        val config = pendingConfig

        // 1. TMX laden + parsen
        val tmxContent = resourcesVfs[config.tmxPath].readString()
        val tiledMap = TmxLoader.parse(tmxContent)
        val collision = CollisionGrid.from(tiledMap)
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }

        // 2. Map rendern
        val mapView = TiledMapView(tiledMap, atlases)
        val mapScale = 3.0
        mapView.scale = mapScale
        addChild(mapView)

        // 3. Hero-Combatant und Inventory (frisch pro Szene; Persistenz → späteres Feature)
        val hero = Combatant(
            id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12
        )
        val inventory = Inventory(initialGold = 50)

        // 4. Hero-Sprite in mapView (skaliert + kamerakorrigiert)
        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman()
        player.gridX = config.spawnX
        player.gridY = config.spawnY
        player.play(SpriteAnimation.IDLE)

        // 5. NPC-Sprites in mapView
        val npcSprites: List<Pair<NpcDefinition, CharacterSprite>> = config.npcs.map { npc ->
            val sprite = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
            sprite.loadFromSheet(npc.idleSheetPath)
            sprite.gridX = npc.tileX
            sprite.gridY = npc.tileY
            sprite.facing = npc.facing
            sprite.play(SpriteAnimation.IDLE)
            npc to sprite
        }

        // 6. HUD (in Scene-Root, NICHT in mapView → bleibt screen-fixed)
        val hud = HudOverlay(this, hero, inventory, config.displayName)

        // 7. Dialog-Overlay (in Scene-Root)
        val dialog = DialogOverlay(this, width, height)

        // 8. BGM
        audioManager.playMusic(config.bgmPath)

        // 9. Input-Loop
        addUpdater { dt ->
            val keys = views.input.keys

            // --- Dialog hat Priorität ---
            if (dialog.isActive) {
                if (keys.justPressed(Key.RETURN) || keys.justPressed(Key.SPACE)) {
                    dialog.advance()
                }
                return@addUpdater
            }

            // --- Q: Abbrechen / zurück (ohne aktiven Dialog kein Sinn hier;
            //         kann als Flucht aus WorldScene genutzt werden falls gewünscht) ---

            // --- SPACE → BattleScene ---
            if (keys.justPressed(Key.SPACE)) {
                audioManager.stopMusic()
                launch { sceneContainer.changeTo<BattleScene>() }
                return@addUpdater
            }

            // --- E → NPC-Interaktion ---
            if (keys.justPressed(Key.E)) {
                val faceX = player.gridX + player.facing.dx
                val faceY = player.gridY + player.facing.dy
                val npc = npcSprites.firstOrNull { (def, _) ->
                    def.tileX == faceX && def.tileY == faceY && def.dialog.isNotEmpty()
                }
                if (npc != null) {
                    dialog.show(npc.first.dialog)
                    return@addUpdater
                }
            }

            // --- Richtungsinput (nur wenn Sprite nicht mid-step) ---
            var dx = 0; var dy = 0
            if (keys.pressing(Key.LEFT)  || keys.pressing(Key.A)) { dx = -1; player.facing = Facing.LEFT  }
            if (keys.pressing(Key.RIGHT) || keys.pressing(Key.D)) { dx =  1; player.facing = Facing.RIGHT }
            if (keys.pressing(Key.UP)    || keys.pressing(Key.W)) { dy = -1; player.facing = Facing.UP    }
            if (keys.pressing(Key.DOWN)  || keys.pressing(Key.S)) { dy =  1; player.facing = Facing.DOWN  }

            if (dx != 0 || dy != 0) {
                val nx = player.gridX + dx
                val ny = player.gridY + dy
                val cx = nx - collision.offsetX
                val cy = ny - collision.offsetY
                val cellType = collision[cx, cy]

                if (cellType == TileType.WALKABLE || cellType == TileType.TRIGGER) {
                    // NPC-Blockierung: bewege nur wenn kein NPC auf Zielkachel
                    val npcBlocking = npcSprites.any { (def, _) ->
                        def.tileX == nx && def.tileY == ny
                    }
                    if (!npcBlocking && player.startMove(nx, ny)) {
                        player.play(SpriteAnimation.WALK)

                        // Exit-Check: nach erfolgreicher Bewegung prüfen ob Exit-Tile
                        val exit = config.exits.firstOrNull { it.tileX == nx && it.tileY == ny }
                        if (exit != null) {
                            audioManager.stopMusic()
                            WorldScene.pendingConfig = MapConfig.forId(exit.destination, exit.spawnX, exit.spawnY)
                            launch { sceneContainer.changeTo<WorldScene>() }
                        }
                    }
                }
            } else {
                if (!player.isMoving) player.play(SpriteAnimation.IDLE)
            }

            // --- HUD aktualisieren ---
            hud.update(config.displayName)
        }

        // 10. Kamera (visualGridX/Y für flüssiges Camera-Follow)
        addUpdater {
            val px = player.visualGridX * tiledMap.tileWidth * mapScale
            val py = player.visualGridY * tiledMap.tileHeight * mapScale
            mapView.x = views.virtualWidth  / 2.0 - px
            mapView.y = views.virtualHeight / 2.0 - py
        }
    }

    override suspend fun sceneAfterDestroy() {
        audioManager.stopMusic()
    }
}
```

**NPC-Blockierung:** NPCs blockieren Bewegung visuell (Spieler kann nicht auf deren Kachel
laufen), aber CollisionGrid kennt die NPCs nicht — die Prüfung `npcBlocking` in WorldScene
ergänzt das.

**`player.startMove()` und das Facing-Problem:** `startMove(nx, ny)` setzt `gridX/gridY`
intern. Facing wird von WorldScene VOR `startMove()` gesetzt (durch die dx/dy-Checks oben).
Das ist korrekt — der Sprite dreht sich sofort, die visuelle Bewegung folgt.

**sceneContainer-API:** `sceneContainer` (kein Aufruf-Klammer) ist die Property der Scene,
die den aktiven SceneContainer zurückgibt. `launch { sceneContainer.changeTo<T>() }` ist das
korrekte Pattern (wie in TiledMapScene und BattleScene aus Step 5a).

---

## Schritt 8 — `Main.kt` anpassen

`WorldScene` als Einstiegspunkt statt `TiledMapScene`:

```kotlin
package game

import korlibs.korge.Korge
import korlibs.korge.scene.sceneContainer

suspend fun main() = Korge {
    sceneContainer().changeTo<WorldScene>()
}
```

`pendingConfig` ist bereits mit `MapConfig.interior()` initialisiert (companion object default) —
kein explizites Setzen nötig beim ersten Start.

---

## Schritt 9 — `BattleScene.kt` anpassen

Eine einzige Änderung: Q-Taste kehrt zu `WorldScene` zurück statt zu `TiledMapScene`.

```kotlin
// Alt (Step 5a):
if (keys.justPressed(Key.Q)) {
    audioManager.stopMusic()
    launch { sceneContainer.changeTo<TiledMapScene>() }
    return@addUpdater
}

// Neu (Step 5b):
if (keys.justPressed(Key.Q)) {
    audioManager.stopMusic()
    // pendingConfig bleibt erhalten (wurde beim Übergang zu BattleScene nicht verändert)
    launch { sceneContainer.changeTo<WorldScene>() }
    return@addUpdater
}
```

`WorldScene.pendingConfig` wurde beim letzten Betreten von WorldScene gesetzt und bleibt
erhalten (companion object) — Rückkehr landet auf der gleichen Karte und am gleichen Spawn.
**Vorsicht:** Das bedeutet beim ersten Start über BattleScene (SPACE in WorldScene) kehrt Q
korrekt zu WorldScene zurück, weil pendingConfig.interior() der Default ist.

---

## ACCEPTANCE

```
./gradlew :game:compileKotlinDesktop        → BUILD SUCCESSFUL (0 errors)
./gradlew :core:desktopTest                 → BUILD SUCCESSFUL (206 Tests, unverändert)
./gradlew :composeApp:compileKotlinDesktop  → BUILD SUCCESSFUL (unverändert)
```

Kein GL-Fenster, kein Runtime-Test — reine Kompilierung genügt.

**Erwartetes visuelles Ergebnis (nur manuell/lokal):**
- `WorldScene` bootet Interior1.tmx, Swordsman mit Idle-Animation
- WASD/Pfeiltasten: Swordsman gleitet flüssig zwischen Kacheln (kein Teleport)
- Kollision blockiert Wände; NPCs blockieren ihre Kachel
- E-Taste vor NPC (facing): Dialogkasten öffnet sich unten, ENTER/SPACE schaltet weiter
- Dialog pausierts Bewegung + SPACE-Trigger
- Letzte Dialogzeile: Dialogkasten verschwindet, Bewegung wieder möglich
- HUD (oben links): HP-Leiste, Gold-Counter, Location-Name
- Exit-Kachel (am Kartenrand): Übergang zu Exterior.tmx, neue BGM spielt
- Auf Exterior: andere NPCs + Dialog, Exit führt zurück zu Interior
- SPACE → BattleScene, Q → zurück zu WorldScene (gleiche Karte)

---

## Doku-Pflicht nach Abschluss

- `docs/KORGE_MIGRATION_PLAN.md` → Step 5b als ✅ markieren
- `docs/KNOWN_BUGS.md` → alle neuen KorGE-Pitfalls (insb. Text-Wrapping, Panel-Visibility-Pattern)
- `briefs/2026-06-28-korge-step5b-world-layer-result.md` → Result-Report

---

## Kontext und Pitfall-Sammlung

### Bereits bekannte KorGE-6.0-Pitfalls (aus KNOWN_BUGS.md):

1. **`changeTo<>()` ist suspend** — aus `addUpdater {}` (non-suspend) immer via
   `launch { sceneContainer.changeTo<T>() }` aufrufen.
2. **`sceneContainer` (Property) vs. `sceneContainer()` (Factory)** — in einer laufenden Scene
   ausschließlich `sceneContainer` (kein Klammern) verwenden.
3. **KorGE Audio headless** — `AudioManager.playMusic()` und `playSfx()` sind in try/catch
   eingewickelt → kein Crash in CI. Gleiche Behandlung für alle neuen Audio-Calls.
4. **`buildFallbackBitmap()` internal** — `SpriteLoader.buildFallbackBitmap()` hat `internal`
   Sichtbarkeit; innerhalb `package game` direkt nutzbar.
5. **`justPressed` vs `pressing`** — einmalige Aktionen (Dialog, Transition, SPACE) brauchen
   `justPressed`; Bewegung nutzt `pressing` (kontinuierlich).

### Neu zu beachtende Punkte:

6. **Visibility-Kaskade in KorGE**: Ein `solidRect.visible = false` versteckt das Rect selbst,
   aber nicht seine `addChild()`-Kinder. Wenn DialogOverlay mit `addChild` für Rahmen-Elemente
   arbeitet, diese separat in der `hide()`-Funktion verstecken — ODER den Rahmen als direktes
   Kind des parent hinzufügen (nicht als Kind des Panels), und `visible` einzeln setzen.
   Empfehlung: alle DialogOverlay-Views als direkte Kinder von `parent` hinzufügen, alle
   einzeln per `.visible`-Flag steuern.

7. **Text-Breite in KorGE**: `text().apply { width = 200.0 }` begrenzt nicht den Render-Bereich
   automatisch. Lange Zeilen können überlaufen. Für 5b: DialogLine-Texte mit `\n` manuell
   umbrechen (max ~60 Zeichen pro Zeile). Word-Wrapping ist ein späteres Feature.

8. **`CharacterSprite.startMove()` und Facing**: WorldScene setzt `player.facing` vor
   `startMove()`. Der reactive setter von `facing` ruft `updateFacing()` auf, der `img.scaleX`
   setzt. Das passiert sofort, unabhängig von `moveProgress` — korrekt: Charakter dreht sich
   sofort, bewegt sich dann.

9. **NPC-Flip für LEFT-Facing**: `CharacterSprite.updateFacing()` setzt `img.scaleX = -1` für
   LEFT. Der Pivot-Korrektur-Code in `updatePosition()` muss sicherstellen dass der Flip-Origin
   korrekt ist (existiert bereits in Step-5a-Code, prüfen ob mit visualGridX konsistent).

10. **MapConfig als companion-Default**: `WorldScene.pendingConfig` ist mit `MapConfig.interior()`
    initialisiert. Wenn `BattleScene` via Q zurück zu WorldScene geht, ohne pendingConfig zu
    ändern, landet der Spieler auf der Karte auf der er vorher war — das ist gewollt.

### `:core`-APIs die in 5b direkt verwendet werden:

```kotlin
// rpg.combat.Combatant — identisch mit BattleScene (Step 5a)
Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
hero.hp           // Int, aktuell
hero.maxHp        // Int
hero.hpFraction   // Float (0f..1f), für HP-Balken-Breite
hero.isAlive      // Boolean

// rpg.items.Inventory
val inventory = Inventory(initialGold = 50)
inventory.gold    // Int
inventory.hasPotions()  // Boolean

// rpg.tiled.* — identisch mit TiledMapScene (Step 5a)
TmxLoader.parse(tmxContent)
CollisionGrid.from(tiledMap)
collision[cx, cy] // TileType
TileType.WALKABLE, TileType.TRIGGER
collision.offsetX, collision.offsetY

// rpg.combat.Side
Side.PLAYER
```

### Asset-Pfade für NPC-Sprites:

```
Swordsman lvl2 Idle:
  assets/HD/characters/swordsman/PNG/Swordsman_lvl2/Without_shadow/Swordsman_lvl2_Idle_without_shadow.png

Swordsman lvl3 Idle:
  assets/HD/characters/swordsman/PNG/Swordsman_lvl3/Without_shadow/Swordsman_lvl3_Idle_without_shadow.png

Vampire1 Idle (für Patron):
  assets/HD/characters/vampire/PNG/Vampires1/Without_shadow/Vampires1_Idle_without_shadow.png

Prozeduraler Fallback (null idleSheetPath):
  SpriteLoader.buildFallbackBitmap() → lila 32×32 Rect (bereits in SpriteLoader.kt)
```

### TMX-Locations:

```
Interior1:
  tmxDir = "assets/HD/locations/heroes-home/Tiled_files"
  tmxFile = "Interior1.tmx"
  Bekannte spawn: (8, 12) — Mitte der 16×~24-Tile-Map
  Exit-Kachel (konfiguriert): (8, 1) — Schätzwert, obere Bildschirmkante

Exterior:
  tmxDir = "assets/HD/locations/heroes-home/Tiled_files"
  tmxFile = "Exterior.tmx"
  Exit-Kachel (konfiguriert): (8, 22) — Schätzwert, untere Bildschirmkante
  BGM: assets/audio/music/Sovereign_Heights.mp3

Beide Locations teilen das gleiche tmxDir — Tilesets sind korrekt referenziert.
```

### Smooth-Movement-Invarianten (für Kiro zur Selbstprüfung):

- `isMoving == true` genau dann wenn `moveProgress < 1f`
- `startMove()` gibt `false` zurück wenn `isMoving == true` — kein Move accepted
- `visualGridX = fromGridX` wenn `moveProgress = 0f` (Start des Steps)
- `visualGridX = gridX` wenn `moveProgress = 1f` (Step abgeschlossen)
- `gridX/Y` setzen via reaktivem Setter (bleibt wie in Step 5a) ändert sofort `visualGridX/Y`
  solange `moveProgress = 1f` (idle state)
- Kamera liest `visualGridX/Y` → flüssiges Camera-Follow

---

## Scope-Zusammenfassung für den Merge-Review

| Datei | Änderungstyp | Hauptinhalt |
|-------|-------------|-------------|
| `DialogLine.kt` | neu | data class (2 Felder) |
| `NpcDefinition.kt` | neu | data class (5 Felder) |
| `MapConfig.kt` | neu | MapId enum + MapExit data class + MapConfig data class mit interior()/exterior() factory |
| `DialogOverlay.kt` | neu | KorGE-Container-basierter Dialog-Kasten |
| `HudOverlay.kt` | neu | KorGE-Container-basiertes HUD |
| `WorldScene.kt` | neu | Hauptszene (~120 Zeilen) |
| `CharacterSprite.kt` | modify | +smooth movement (startMove, isMoving, visualGridX/Y, moveProgress), +loadFromSheet(), +Facing.dx/dy extension |
| `Main.kt` | modify | WorldScene statt TiledMapScene |
| `BattleScene.kt` | modify | 1 Zeile: TiledMapScene → WorldScene in Q-Handler |
