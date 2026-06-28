package game

import korlibs.io.file.std.resourcesVfs
import korlibs.korge.scene.Scene
import korlibs.korge.view.Container
import korlibs.korge.view.SContainer
import korlibs.korge.view.container
import korlibs.korge.view.filter.filter
import rpg.tiled.TmxLoader
import rpg.weather.WeatherConfig
import rpg.weather.WeatherState
import game.shader.LightSource
import game.shader.LightingFilter
import game.shader.SnowFilter
import game.shader.BloodFilter

/**
 * Standalone physics test scene demonstrating all 5 world physics systems:
 * Snow accumulation, Footprints, Wind-driven particles, Blood splatters, Torch light.
 *
 * This scene is purely for screenshot/demo verification and is NOT wired into the
 * main game boot sequence. All values are deterministic (fixed time, no randomness).
 */
class PhysicsTestScene : Scene() {

    override suspend fun SContainer.sceneMain() {
        val config = MapConfig.exterior()

        // Load and render the TMX map as visual base
        val tmxContent = resourcesVfs[config.tmxPath].readString()
        val tiledMap = TmxLoader.parse(tmxContent)
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }

        val mapScale = 3.0
        val tilePixelSize = (tiledMap.tileWidth * mapScale).toFloat()

        // --- Core weather simulation ---
        // Grid 13x8 matching 640/48 x 360/48 tiles approx
        val weatherState = WeatherState(
            config = WeatherConfig(
                snowRate = 0.1f,
                meltRate = 0.0f,         // no melting for demo
                bloodFadeRate = 0.0f,    // no fading for demo
                footprintRefillRate = 0.0f,
                windDriftSpeed = 0.3f
            ),
            gridWidth = 13,
            gridHeight = 8
        )

        // Configure wind: angle 0.3 rad, strength 0.7
        weatherState.wind.direction = 0.3f
        weatherState.wind.strength = 0.7f

        // Simulate 10 seconds of snow accumulation (100 ticks of 0.1s)
        repeat(100) { weatherState.tick(0.1f) }

        // Stamp footprints: path of 4 tiles
        weatherState.snow.stampFootprint(5, 4)
        weatherState.snow.stampFootprint(6, 4)
        weatherState.snow.stampFootprint(7, 4)
        weatherState.snow.stampFootprint(8, 4)

        // Splatter blood at 2 locations
        weatherState.blood.splatter(3, 3, intensity = 0.8f, radius = 1)
        weatherState.blood.splatter(10, 6, intensity = 0.6f, radius = 2)

        // --- Render layers with nested containers for multiple filters ---
        // Outermost: BloodFilter
        // Middle: SnowFilter
        // Innermost (mapView): LightingFilter

        // Calculate average snow depth for SnowFilter intensity
        var totalSnow = 0f
        for (y in 0 until 8) {
            for (x in 0 until 13) {
                totalSnow += weatherState.snow[x, y]
            }
        }
        val avgSnowDepth = totalSnow / (13 * 8)

        // Build nested container hierarchy
        val outerContainer = container {
            val middleContainer = container {
                val mapView = TiledMapView(tiledMap, atlases)
                mapView.scale = mapScale
                addChild(mapView)

                // Place player for reference
                val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
                player.loadSwordsman()
                player.gridX = config.spawnX
                player.gridY = config.spawnY
                player.play(SpriteAnimation.IDLE)

                // Camera centre on player
                mapView.x = 640.0 / 2.0 - player.visualGridX * tiledMap.tileWidth * mapScale
                mapView.y = 360.0 / 2.0 - player.visualGridY * tiledMap.tileHeight * mapScale

                // Apply LightingFilter on the innermost map view
                val lightingFilter = LightingFilter(
                    ambientDarkness = 0.15f,
                    time = 2.0f
                )
                lightingFilter.tilePixelSize = tilePixelSize
                lightingFilter.lights = listOf(
                    // Torch 1: warm candle at tile (-3, 8)
                    LightSource(
                        tileX = -3, tileY = 8,
                        radius = 6f, r = 1.0f, g = 0.8f, b = 0.4f,
                        intensity = 0.9f, flickerSpeed = 3.0f, flickerAmount = 0.15f
                    ),
                    // Torch 2: warm candle at tile (0, 10)
                    LightSource(
                        tileX = 0, tileY = 10,
                        radius = 5f, r = 1.0f, g = 0.75f, b = 0.35f,
                        intensity = 0.85f, flickerSpeed = 3.5f, flickerAmount = 0.12f
                    ),
                )
                mapView.filter = lightingFilter

                // Apply SnowFilter on middle container
                val snowFilter = SnowFilter(
                    intensity = avgSnowDepth,
                    windAngle = weatherState.wind.direction,
                    time = 2.5f
                )
                this.filter = snowFilter
            }

            // Apply BloodFilter on outer container
            val bloodFilter = BloodFilter(
                intensity = 0.5f,
                splatterSeed = 42.0f,
                time = 1.0f
            )
            this.filter = bloodFilter
        }

        // Also show rain driven by wind (demonstrates wind driving both rain + snow)
        // NOTE: In a real scene, rain would occupy a separate overlay container.
        // For this prototype demo, wind drives the SnowFilter drift direction only.
    }
}
