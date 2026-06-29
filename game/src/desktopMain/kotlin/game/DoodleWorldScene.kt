package game

import korlibs.event.Key
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.image.format.readBitmap
import korlibs.io.file.std.resourcesVfs
import korlibs.korge.scene.Scene
import korlibs.korge.scene.sceneContainer
import korlibs.korge.view.SContainer
import korlibs.korge.view.addUpdater
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.solidRect
import korlibs.time.seconds
import kotlinx.coroutines.launch
import rpg.BarkOutcome
import rpg.SliceDirector
import rpg.bark.audio.BarkAudioPlayer
import rpg.combat.Combatant
import rpg.combat.Side
import rpg.items.Inventory
import rpg.questbook.RoomContext
import rpg.tiled.CollisionGrid
import rpg.tiled.TileType
import rpg.tiled.TmxLoader
import rpg.world.Camera
import korlibs.korge.view.filter.filter
import game.shader.DoodleLineFilter
import game.world.ImageMapId
import game.world.ImageWorldDef

/**
 * Step 14: Unified World Runtime (Pfeiler 1 — the Spine).
 *
 * This is THE single gameplay runtime for the Bild+Grid world. It replaces the old
 * tile-based WorldScene as the boot target and unifies:
 * - Painted HD background (crisp, no shader)
 * - Invisible CollisionGrid from TMX
 * - Doodle-filtered character (cartoon outline + boil)
 * - NPC interaction hotspots (invisible — painted background provides visuals)
 * - Dialog, Bark pipeline, HUD, Questbook, Battle, Map transitions
 * - Camera (from :core) for maps larger than viewport
 *
 * Architecture: "Bild = Haut, Grid = Logik"
 * The player is the ONLY rendered figure. NPCs are invisible hotspots on grid cells
 * placed where the painted background shows characters.
 */
class DoodleWorldScene : Scene() {

    companion object {
        /** Set before changeTo<DoodleWorldScene>() to configure which map loads next. */
        var pendingMap: ImageMapId = ImageMapId.TAVERN_INTERIOR

        /** Override spawn position (set by exit transitions). Null = use def default. */
        var pendingSpawn: Pair<Int, Int>? = null

        /** Debug flag: render hotspot markers on NPC cells. */
        const val DEBUG_HOTSPOTS = true
    }

    override suspend fun SContainer.sceneMain() {
        val outputW = views.virtualWidth.toFloat()   // 2560
        val outputH = views.virtualHeight.toFloat()  // 1440

        // =====================================================================
        // 0. LOAD MAP DEFINITION
        // =====================================================================
        val def = ImageWorldDef.forId(pendingMap)

        // =====================================================================
        // 1. LOAD MAP: painted image + collision grid
        // =====================================================================
        val bgBitmap = resourcesVfs[def.imagePath].readBitmap()
        val imgW = bgBitmap.width.toFloat()
        val imgH = bgBitmap.height.toFloat()

        val tmxContent = resourcesVfs[def.gridTmxPath].readString()
        val tiledMap = TmxLoader.parse(tmxContent)
        val grid = CollisionGrid.from(tiledMap)

        val gridRows = grid.rows
        val gridCols = grid.cols

        // =====================================================================
        // 2. GRID-AS-UNIT, scroll-capable world layer
        // =====================================================================
        // screenTile: how many screen pixels one grid cell occupies (fit to height)
        val screenTile = outputH / gridRows.toFloat()
        // bgScale: scale factor to fit the painted image to fill the viewport height
        val bgScale = outputH / imgH
        // World dimensions in screen pixels
        val worldW = imgW * bgScale
        val worldH = outputH

        // worldLayer: contains bg image + entityLayer + hotspot debug markers.
        // Camera moves this container.
        val worldLayer = container {}
        addChild(worldLayer)

        // Background image inside worldLayer
        val bg = worldLayer.image(bgBitmap)
        bg.smoothing = true
        bg.scaleX = bgScale.toDouble()
        bg.scaleY = bgScale.toDouble()

        // =====================================================================
        // 3. CAMERA — rpg.world.Camera (core, tested, clamped)
        // =====================================================================
        val camera = Camera()

        // =====================================================================
        // 4. PLAYER — CharacterSprite with DoodleLineFilter
        // =====================================================================
        val entityLayer = worldLayer.container {}

        // Grid-derived character sizing (NEVER hardcoded) — Step 13 math:
        // tilesTall=5, layerTile = round(64/tilesTall), charScale = screenTile/layerTile
        // charScale multiplies BOTH the sprite size AND in-layer position.
        val tilesTall = 5
        val layerTile = (64.0 / tilesTall).toInt().coerceAtLeast(1) // ~13 px in layer space
        val charScale = screenTile / layerTile.toFloat()

        // Determine spawn
        val (rawSpawnX, rawSpawnY) = pendingSpawn ?: def.spawn ?: (gridCols / 2 to gridRows / 2)
        pendingSpawn = null // consume

        // Verify spawn is walkable (B004: derive from CollisionGrid)
        var spawnX = rawSpawnX
        var spawnY = rawSpawnY
        val spawnCx = spawnX - grid.offsetX
        val spawnCy = spawnY - grid.offsetY
        if (grid[spawnCx, spawnCy] != TileType.WALKABLE && grid[spawnCx, spawnCy] != TileType.TRIGGER) {
            // Spiral outward to find walkable
            outer@ for (radius in 1 until kotlin.math.max(gridCols, gridRows)) {
                for (dy in -radius..radius) {
                    for (dx in -radius..radius) {
                        if (kotlin.math.abs(dx) + kotlin.math.abs(dy) != radius) continue
                        val cx = spawnCx + dx
                        val cy = spawnCy + dy
                        if (cx < 0 || cx >= grid.cols || cy < 0 || cy >= grid.rows) continue
                        if (grid[cx, cy] == TileType.WALKABLE || grid[cx, cy] == TileType.TRIGGER) {
                            spawnX = cx + grid.offsetX
                            spawnY = cy + grid.offsetY
                            break@outer
                        }
                    }
                }
            }
        }

        // Create player
        val player = CharacterSprite(entityLayer, layerTile, layerTile)
        player.loadSwordsman()
        player.gridX = spawnX
        player.gridY = spawnY
        player.facing = Facing.DOWN
        player.play(SpriteAnimation.IDLE)

        // Scale entity layer
        entityLayer.scaleX = charScale.toDouble()
        entityLayer.scaleY = charScale.toDouble()

        // Apply DoodleLineFilter ONLY to entity layer (background stays crisp)
        val doodleFilter = DoodleLineFilter(
            lineStrength = 0.8f,
            jitter = 0.4f,
        )
        entityLayer.filter = doodleFilter

        // =====================================================================
        // 5. NPC HOTSPOTS — invisible, optional debug markers
        // =====================================================================
        if (DEBUG_HOTSPOTS) {
            for (npc in def.npcs) {
                val markerX = npc.cellX * screenTile
                val markerY = npc.cellY * screenTile
                val marker = worldLayer.solidRect(
                    screenTile.toDouble() * 0.6,
                    screenTile.toDouble() * 0.6,
                    RGBA(0xff, 0xaa, 0x00, 0x66) // semi-transparent orange
                )
                marker.x = (markerX + screenTile * 0.2).toDouble()
                marker.y = (markerY + screenTile * 0.2).toDouble()
            }
        }

        // =====================================================================
        // 6. HUD — screen-fixed (scene root, NOT worldLayer)
        // =====================================================================
        val hero = Combatant(
            id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12
        )
        val inventory = Inventory(initialGold = 50)
        val hud = HudOverlay(this, hero, inventory, def.displayName)

        // =====================================================================
        // 7. DIALOG — screen-fixed
        // =====================================================================
        val dialog = DialogOverlay(this, outputW.toDouble(), outputH.toDouble())

        // =====================================================================
        // 8. BARK PIPELINE
        // =====================================================================
        val director = SliceDirector { System.currentTimeMillis() }
        director.barkAudioPlayer = BarkAudioPlayer(GameAudioPlayer(this@DoodleWorldScene))
        val roomId = if (def.id == ImageMapId.TAVERN_INTERIOR) RoomContext.ROOM_TAVERN else "wildwood"
        director.enterRoom(RoomContext(mapId = def.id.name.lowercase(), roomId = roomId))

        // =====================================================================
        // 9. QUESTBOOK — screen-fixed
        // =====================================================================
        val questbook = QuestbookOverlay(this, outputW.toDouble(), outputH.toDouble())
        questbook.refresh(director.pressure, director.questMarkers + director.falseMarkers)

        // =====================================================================
        // 10. INPUT LOOP
        // =====================================================================
        addUpdater { dt ->
            val dtSec = dt.seconds.toFloat()

            // Tick doodle boil animation
            doodleFilter.time += dtSec

            // Tick questbook toast timer
            questbook.update(dtSec)

            // ─── Camera follow ───────────────────────────────────────────────
            val playerScreenX = player.visualGridX.toFloat() * screenTile
            val playerScreenY = player.visualGridY.toFloat() * screenTile
            camera.follow(playerScreenX, playerScreenY, outputW, outputH, worldW, worldH)
            worldLayer.x = -camera.x.toDouble()
            worldLayer.y = -camera.y.toDouble()

            // ─── Input priority (mirrors WorldScene pattern) ─────────────────
            val keys = views.input.keys

            // Dialog has priority
            if (dialog.isActive) {
                if (keys.justPressed(Key.RETURN) || keys.justPressed(Key.SPACE)) {
                    dialog.advance()
                }
                return@addUpdater
            }

            // SPACE → BattleScene
            if (keys.justPressed(Key.SPACE)) {
                BattleScene.bossEncounter = false
                launch { sceneContainer.changeTo<BattleScene>() }
                return@addUpdater
            }

            // J → QuestbookScreen
            if (keys.justPressed(Key.J)) {
                QuestbookScreen.entries = director.questbook.log
                QuestbookScreen.markers = director.questMarkers + director.falseMarkers
                QuestbookScreen.partyName = director.partyName
                QuestbookScreen.pressure = director.pressure
                launch { sceneContainer.changeTo<QuestbookScreen>() }
                return@addUpdater
            }

            // E → NPC hotspot interaction (facing cell)
            if (keys.justPressed(Key.E)) {
                val faceX = player.gridX + player.facing.dx
                val faceY = player.gridY + player.facing.dy
                val hotspot = def.npcs.firstOrNull { it.cellX == faceX && it.cellY == faceY }
                if (hotspot != null && hotspot.dialog.isNotEmpty()) {
                    dialog.show(hotspot.dialog)
                    hotspot.barkEvent?.let { event ->
                        launch {
                            val outcome = director.fireBark(event)
                            if (outcome is BarkOutcome.Fired) {
                                questbook.showReaction(
                                    outcome.reaction,
                                    director.pressure,
                                    director.questMarkers + director.falseMarkers,
                                )
                            }
                        }
                    }
                    return@addUpdater
                }
            }

            // ─── Movement (WASD/Arrows, tile-based, collision-checked) ───────
            var dx = 0; var dy = 0
            if (keys.pressing(Key.LEFT) || keys.pressing(Key.A)) { dx = -1; player.facing = Facing.LEFT }
            if (keys.pressing(Key.RIGHT) || keys.pressing(Key.D)) { dx = 1; player.facing = Facing.RIGHT }
            if (keys.pressing(Key.UP) || keys.pressing(Key.W)) { dy = -1; player.facing = Facing.UP }
            if (keys.pressing(Key.DOWN) || keys.pressing(Key.S)) { dy = 1; player.facing = Facing.DOWN }

            if ((dx != 0 || dy != 0) && !player.isMoving) {
                val nx = player.gridX + dx
                val ny = player.gridY + dy
                val cx = nx - grid.offsetX
                val cy = ny - grid.offsetY
                val cellType = grid[cx, cy]
                if (cellType == TileType.WALKABLE || cellType == TileType.TRIGGER) {
                    player.startMove(nx, ny)
                    player.play(SpriteAnimation.WALK)

                    // Exit check after successful move
                    val exit = def.exits.firstOrNull { it.cellX == nx && it.cellY == ny }
                    if (exit != null) {
                        pendingMap = exit.destination
                        pendingSpawn = exit.destSpawnX to exit.destSpawnY
                        launch { sceneContainer.changeTo<DoodleWorldScene>() }
                    }
                }
            } else if (!player.isMoving) {
                player.play(SpriteAnimation.IDLE)
            }

            // ─── HUD update ──────────────────────────────────────────────────
            hud.update(def.displayName)
        }
    }
}
