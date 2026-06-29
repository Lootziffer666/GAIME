package game

import korlibs.event.Key
import korlibs.image.color.Colors
import korlibs.image.format.readBitmap
import korlibs.io.file.std.resourcesVfs
import korlibs.korge.scene.Scene
import korlibs.korge.view.SContainer
import korlibs.korge.view.addUpdater
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.solidRect
import korlibs.time.seconds
import rpg.tiled.CollisionGrid
import rpg.tiled.TileType
import rpg.tiled.TmxLoader
import korlibs.korge.view.filter.filter
import game.shader.DoodleLineFilter

/**
 * Step 13: Playable Doodle World @ 1440p.
 *
 * Architecture:
 * - Background: hi-res painted image (tavern_interior.png), crisp, NO shader
 * - Logic: invisible CollisionGrid from tavern_interior.tmx (78x78)
 * - Character: DoodleLineFilter on own container (cartoon outline + boil)
 * - Movement: WASD/Arrows, tile-based, collision-checked against grid
 *
 * This is the "this is what the game looks like" moment.
 * No NPCs, no dialog, no combat — just the core rendering loop.
 */
class DoodleWorldScene : Scene() {

    override suspend fun SContainer.sceneMain() {
        val outputW = views.virtualWidth   // 2560
        val outputH = views.virtualHeight  // 1440

        // =====================================================================
        // 1. BACKGROUND: painted image, aspect-preserving fit to height
        // =====================================================================
        val bgBitmap = resourcesVfs["assets/HD/backgrounds/tavern_interior.png"].readBitmap()
        val imgW = bgBitmap.width.toDouble()
        val imgH = bgBitmap.height.toDouble()

        // Fit to output height, preserve aspect (image is ~1254x1254 square)
        val bgScale = outputH / imgH
        val bgDisplayW = imgW * bgScale
        val bgDisplayH = outputH.toDouble()
        val bgOffsetX = (outputW - bgDisplayW) / 2.0  // center horizontally (letterbox)

        // Dark letterbox bars
        if (bgOffsetX > 0) {
            solidRect(bgOffsetX, outputH.toDouble(), Colors.BLACK).apply { x = 0.0; y = 0.0 }
            solidRect(bgOffsetX, outputH.toDouble(), Colors.BLACK).apply { x = outputW - bgOffsetX; y = 0.0 }
        }

        val bg = image(bgBitmap)
        bg.smoothing = true
        bg.scaleX = bgScale
        bg.scaleY = bgScale
        bg.x = bgOffsetX
        bg.y = 0.0

        // =====================================================================
        // 2. LOGIC GRID: invisible collision layer
        // =====================================================================
        val tmxContent = resourcesVfs["assets/HD/backgrounds/tavern_interior.tmx"].readString()
        val tiledMap = TmxLoader.parse(tmxContent)
        val grid = CollisionGrid.from(tiledMap)

        val gridRows = grid.rows  // 78 for tavern_interior
        val gridCols = grid.cols  // 78
        val screenTile = bgDisplayH / gridRows  // pixels per tile on screen

        // =====================================================================
        // 3. CHARACTER LAYER: own container with DoodleLineFilter
        // =====================================================================
        val charLayer = container {}
        addChild(charLayer)

        // Grid-derived character scale (NEVER hardcoded)
        val tilesTall = 5  // character height in tiles (matches painted NPC scale)
        val charScale = (tilesTall * screenTile) / 64.0  // CraftPix sprite = 64px

        // Find a walkable spawn cell (B004: derive from CollisionGrid)
        var spawnX = gridCols / 2
        var spawnY = gridRows / 2
        // Spiral outward from center to find WALKABLE
        outer@ for (radius in 0 until kotlin.math.max(gridCols, gridRows)) {
            for (dy in -radius..radius) {
                for (dx in -radius..radius) {
                    if (kotlin.math.abs(dx) + kotlin.math.abs(dy) != radius) continue
                    val cx = gridCols / 2 + dx
                    val cy = gridRows / 2 + dy
                    if (cx < 0 || cx >= grid.cols || cy < 0 || cy >= grid.rows) continue
                    if (grid[cx, cy] == TileType.WALKABLE || grid[cx, cy] == TileType.TRIGGER) {
                        spawnX = cx + grid.offsetX
                        spawnY = cy + grid.offsetY
                        break@outer
                    }
                }
            }
        }

        // Create player character
        val player = CharacterSprite(charLayer, screenTile.toInt().coerceAtLeast(1), screenTile.toInt().coerceAtLeast(1))
        player.loadSwordsman()
        player.gridX = spawnX
        player.gridY = spawnY
        player.facing = Facing.DOWN
        player.play(SpriteAnimation.IDLE)

        // Scale the character layer for correct size
        charLayer.scaleX = charScale
        charLayer.scaleY = charScale
        // Offset charLayer to align with the background image position
        charLayer.x = bgOffsetX
        charLayer.y = 0.0

        // Apply DoodleLineFilter to character layer
        val doodleFilter = DoodleLineFilter(
            lineStrength = 0.8f,
            jitter = 0.4f,
        )
        charLayer.filter = doodleFilter

        // =====================================================================
        // 4. INPUT + MOVEMENT (tile-based, collision-checked)
        // =====================================================================
        addUpdater { dt ->
            val dtSec = dt.seconds.toFloat()

            // Tick doodle boil animation
            doodleFilter.time += dtSec

            val keys = views.input.keys
            var dx = 0; var dy = 0
            if (keys.pressing(Key.LEFT) || keys.pressing(Key.A)) { dx = -1; player.facing = Facing.LEFT }
            if (keys.pressing(Key.RIGHT) || keys.pressing(Key.D)) { dx = 1; player.facing = Facing.RIGHT }
            if (keys.pressing(Key.UP) || keys.pressing(Key.W)) { dy = -1; player.facing = Facing.UP }
            if (keys.pressing(Key.DOWN) || keys.pressing(Key.S)) { dy = 1; player.facing = Facing.DOWN }

            if ((dx != 0 || dy != 0) && !player.isMoving) {
                val nx = player.gridX + dx
                val ny = player.gridY + dy
                // Check collision (grid coords are world coords, need to subtract offset for grid lookup)
                val cx = nx - grid.offsetX
                val cy = ny - grid.offsetY
                val cellType = grid[cx, cy]
                if (cellType == TileType.WALKABLE || cellType == TileType.TRIGGER) {
                    player.startMove(nx, ny)
                    player.play(SpriteAnimation.WALK)
                }
            } else if (!player.isMoving) {
                player.play(SpriteAnimation.IDLE)
            }
        }
    }
}
