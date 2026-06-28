package game

import korlibs.event.Key
import korlibs.io.file.std.resourcesVfs
import korlibs.korge.scene.Scene
import korlibs.korge.view.SContainer
import korlibs.korge.view.addUpdater
import rpg.tiled.CollisionGrid
import rpg.tiled.TileType
import rpg.tiled.TmxLoader

/**
 * Main gameplay scene: loads a Tiled TMX map, renders it with [TiledMapView],
 * places the [PlayerSprite], and handles grid-based movement with collision.
 *
 * Camera follows the player by offsetting the map container.
 */
class TiledMapScene : Scene() {

    override suspend fun SContainer.sceneInit() {
        val tmxDir = "assets/HD/locations/heroes-home/Tiled_files"
        val tmxPath = "$tmxDir/Interior1.tmx"

        // 1. Load and parse the TMX map
        val tmxContent = resourcesVfs[tmxPath].readString()
        val tiledMap = TmxLoader.parse(tmxContent)
        val collision = CollisionGrid.from(tiledMap)

        // 2. Load tileset atlases
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, tmxDir) }

        // 3. Render the map (base scale 1:1, then upscale for desktop)
        val mapView = TiledMapView(tiledMap, atlases)
        val mapScale = 3.0 // 16px tiles * 3 = 48px on screen
        mapView.scale = mapScale
        this.addChild(mapView)

        // 4. Player sprite inside mapView so it scales and scrolls with the map
        val player = PlayerSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.gridX = 8
        player.gridY = 12

        // 5. Keyboard input + collision checking
        addUpdater {
            val keys = views.input.keys
            var dx = 0
            var dy = 0
            if (keys.pressing(Key.LEFT) || keys.pressing(Key.A)) { dx = -1; player.facing = Facing.LEFT }
            if (keys.pressing(Key.RIGHT) || keys.pressing(Key.D)) { dx = 1; player.facing = Facing.RIGHT }
            if (keys.pressing(Key.UP) || keys.pressing(Key.W)) { dy = -1; player.facing = Facing.UP }
            if (keys.pressing(Key.DOWN) || keys.pressing(Key.S)) { dy = 1; player.facing = Facing.DOWN }

            if (dx != 0 || dy != 0) {
                val nx = player.gridX + dx
                val ny = player.gridY + dy
                val cx = nx - collision.offsetX
                val cy = ny - collision.offsetY
                val cellType = collision[cx, cy]
                if (cellType == TileType.WALKABLE || cellType == TileType.TRIGGER) {
                    player.gridX = nx
                    player.gridY = ny
                    player.setWalking()
                }
            } else {
                player.setIdle()
            }
        }

        // 6. Camera follow: offset mapView so the player stays centered
        addUpdater {
            val px = player.gridX * tiledMap.tileWidth * mapScale
            val py = player.gridY * tiledMap.tileHeight * mapScale
            mapView.x = views.virtualWidth / 2.0 - px
            mapView.y = views.virtualHeight / 2.0 - py
        }
    }
}
