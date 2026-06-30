package game

import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.image.format.PNG
import korlibs.image.format.writeTo
import korlibs.io.file.std.localCurrentDirVfs
import korlibs.io.file.std.resourcesVfs
import korlibs.korge.testing.OffscreenStage
import korlibs.korge.testing.korgeScreenshotTest
import korlibs.korge.view.filter.filter
import korlibs.korge.view.renderToBitmap
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import korlibs.math.geom.Size
import rpg.combat.Combatant
import rpg.combat.Side
import rpg.items.Inventory
import rpg.tiled.TmxLoader
import rpg.BarkOutcome
import rpg.SliceDirector
import rpg.bark.BarkEvent
import rpg.questbook.QuestPressure
import rpg.questbook.QuestbookReaction
import rpg.questbook.RoomContext
import rpg.weather.BloodGrid
import rpg.weather.FootprintGrid
import rpg.weather.SeasonalGrid
import rpg.weather.SnowGrid
import rpg.weather.WindState
import korlibs.image.format.readBitmap
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.filter.filter
import rpg.world.Camera
import game.world.ImageWorldDef
import game.world.ImageMapId
import game.systems.SystemRegistry
import rpg.systems.WorldContext
import rpg.systems.WaterSystem
import rpg.systems.BloodSystem
import rpg.systems.SnowSystem
import rpg.systems.FootprintSystem
import rpg.systems.SeasonSystem
import rpg.systems.MaterialFatigueSystem
import rpg.weather.MaterialFatigue
import rpg.weather.Season
import rpg.weather.WaterGrid

/**
 * Offscreen GL screenshot harness — renders the real game scenes (interior,
 * exterior, battle) to PNGs under build/screenshots/ using KorGE's headless
 * offscreen renderer. No window required.
 *
 * Run via:  ./gradlew :game:screenshot
 * (the task sets EGL_PLATFORM=surfaceless, LIBGL_ALWAYS_SOFTWARE=1, and puts the
 *  repo root on the classpath so resourcesVfs resolves "assets/...").
 */
private const val VW = 640.0
private const val VH = 360.0
private const val SCALE = 3.0
// Resolve against the process working dir (the task sets it to the repo root).
// NOTE: korlibs localVfs("relative") resolves against "/", not the CWD — use
// localCurrentDirVfs for a CWD-relative path.
private val OUT = localCurrentDirVfs["build/screenshots"]

fun main() {
    captureWorld(MapConfig.interior(), "interior", withDialog = true)
    captureWorld(MapConfig.exterior(), "exterior", withDialog = false)
    captureBattle()
    captureShaderBeerGoggle()
    captureShaderPoison()
    captureShaderLighting()
    captureShaderRain()
    captureShaderHeatShimmer()
    // 6d: Scripted playthrough captures
    captureInteriorDialog()
    captureExteriorDialog()
    captureBattleMidway()
    captureBattleVictory()
    captureQuestbookReaction()
    captureWorldPressureHigh()
    captureBattleBossPhase2()
    captureWorldRainPuddles()
    captureWorldPuddleDrain()
    captureWorldLantern()
    captureWorldDrunk()
    captureQuestbookOpen()
    captureFrozenApproach()
    captureFrozenFootprints()
    captureFrozenBlood()
    captureQuestbookGlory()
    captureSpringApproach()
    captureSummerApproach()
    captureAutumnApproach()
    captureWinterApproach()
    captureComposeLightingFog()
    captureMaterialFatigue()
    captureChapel()
    captureGuildHall()
    captureGlassblowers()
    captureRuinedTemple()
    captureBridge()
    captureDoodle1440p()
    captureGridOverlayDebug()
    captureDoodleUpscaleCompare()
    captureDoodleWorld()
    // Step 14: Unified World Runtime captures
    captureUnifiedTavern()
    captureUnifiedWildwood()
    captureUnifiedDialog()
    // Step 15: Systems architecture proof
    captureUnifiedSystems()
    // Step 16: Pfeiler 2 — unified scene specs (all grid physics visible)
    UNIFIED_SPECS.forEach { renderUnifiedScene(it) }
    // Step 17: Figures via normalized sheets
    captureFiguresTavern()
    captureFiguresMarkerCheck()
    // RT Material Rendering proof
    captureRTMaterial()
    // Shader palette expansion
    captureShaderCaustic()
    captureShaderWetSurface()
    captureShaderDecay()
    // Weather composition triptych (same image, 3 weather states)
    captureWeatherTriptych()
    // Material-aware weather (one shader, three states, material-differentiated)
    captureMaterialWeather()
    // Village material-weather pipeline (Dorf-Bild with baked material segmentation)
    captureVillageMaterialWeather()
    // Guild house material-weather
    captureGuildMaterialWeather()
}

private fun captureWorld(config: MapConfig, name: String, withDialog: Boolean) {
    korgeScreenshotTest(Size(VW, VH)) {
        val tiledMap = TmxLoader.parse(resourcesVfs[config.tmxPath].readString())
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }
        val mapView = TiledMapView(tiledMap, atlases)
        mapView.scale = SCALE
        addChild(mapView)

        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman()
        player.gridX = config.spawnX
        player.gridY = config.spawnY
        player.play(SpriteAnimation.IDLE)

        for (npc in config.npcs) {
            val s = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
            s.loadFromSheet(npc.idleSheetPath)
            s.gridX = npc.tileX; s.gridY = npc.tileY; s.facing = npc.facing
            s.play(SpriteAnimation.IDLE)
        }

        // Camera centre on player (mirrors WorldScene)
        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE

        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), config.displayName).update(config.displayName)
        if (withDialog) DialogOverlay(this, VW, VH).show(config.npcs.first().dialog)

        save(name)
    }
}

private fun captureBattle() {
    korgeScreenshotTest(Size(VW, VH)) {
        solidRect(VW, VH, RGBA(0x0a, 0x0a, 0x14, 0xff))

        val hero = CharacterSprite(this, 48, 48)
        hero.loadSwordsman(); hero.gridX = 2; hero.gridY = 3; hero.facing = Facing.RIGHT
        hero.play(SpriteAnimation.IDLE)
        val vampire = CharacterSprite(this, 48, 48)
        vampire.loadVampire(); vampire.gridX = 9; vampire.gridY = 3; vampire.facing = Facing.LEFT
        vampire.play(SpriteAnimation.IDLE)

        val barW = 120.0
        solidRect(barW, 12.0, Colors["#333333"]).apply { x = 40.0; y = 20.0 }
        solidRect(barW, 12.0, Colors["#22cc22"]).apply { x = 40.0; y = 20.0 }
        solidRect(barW, 12.0, Colors["#333333"]).apply { x = VW - 160.0; y = 20.0 }
        solidRect(barW, 12.0, Colors["#cc2222"]).apply { x = VW - 160.0; y = 20.0 }
        text("Nib: 80/80", textSize = 14.0, color = Colors.WHITE).apply { x = 40.0; y = 36.0 }
        text("Vampire: 60/60", textSize = 14.0, color = Colors.WHITE).apply { x = VW - 160.0; y = 36.0 }
        text("ENTER=Attack  E=Heal  Q=Back", textSize = 12.0, color = Colors["#aaaaaa"])
            .apply { x = VW / 2.0 - 100.0; y = VH - 30.0 }

        save("battle")
    }
}

private suspend fun OffscreenStage.save(name: String) {
    val bmp = renderToBitmap(views)
    OUT.mkdirs()
    bmp.writeTo(OUT["$name.png"], PNG)
    println("SCREENSHOT_OK: ${bmp.width}x${bmp.height} -> build/screenshots/$name.png")
}

// =============================================================================
// SHADER EFFECT DEMOS
// =============================================================================

/**
 * Beer goggles: Interior scene through 3 flagons of ale.
 * The world is warm, soft, and gently swaying.
 */
private fun captureShaderBeerGoggle() {
    val config = MapConfig.interior()
    korgeScreenshotTest(Size(VW, VH)) {
        val tiledMap = TmxLoader.parse(resourcesVfs[config.tmxPath].readString())
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }
        val mapView = TiledMapView(tiledMap, atlases)
        mapView.scale = SCALE
        addChild(mapView)

        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman()
        player.gridX = config.spawnX; player.gridY = config.spawnY
        player.play(SpriteAnimation.IDLE)

        for (npc in config.npcs) {
            val s = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
            s.loadFromSheet(npc.idleSheetPath)
            s.gridX = npc.tileX; s.gridY = npc.tileY; s.facing = npc.facing
            s.play(SpriteAnimation.IDLE)
        }

        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE

        // Apply beer goggle shader (3 ales deep)
        val beerFilter = game.shader.BeerGoggleFilter(drunkLevel = 0.6f, time = 2.5f)
        mapView.filter = beerFilter

        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 12), "Heroes' Home (3 ales)")

        save("shader_beer_goggle")
    }
}

/**
 * Poison: Exterior scene with growing disorientation.
 * Chromatic aberration + tunnel vision.
 */
private fun captureShaderPoison() {
    val config = MapConfig.exterior()
    korgeScreenshotTest(Size(VW, VH)) {
        val tiledMap = TmxLoader.parse(resourcesVfs[config.tmxPath].readString())
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }
        val mapView = TiledMapView(tiledMap, atlases)
        mapView.scale = SCALE
        addChild(mapView)

        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman()
        player.gridX = config.spawnX; player.gridY = config.spawnY
        player.facing = Facing.DOWN
        player.play(SpriteAnimation.WALK)

        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE

        // Apply poison shader (severity 0.7)
        val poisonFilter = game.shader.PoisonFilter(intensity = 0.7f, time = 4.2f)
        mapView.filter = poisonFilter

        val hero = Combatant(id = "nib", name = "Nib", maxHp = 30, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), "Village (POISONED)")

        save("shader_poison")
    }
}

/**
 * 2D Lighting: Interior tavern scene with 3 candle light sources.
 * Ambient darkness = 0.12 (very dark), lights illuminate warm pools.
 */
private fun captureShaderLighting() {
    val config = MapConfig.interior()
    korgeScreenshotTest(Size(VW, VH)) {
        val tiledMap = TmxLoader.parse(resourcesVfs[config.tmxPath].readString())
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }
        val mapView = TiledMapView(tiledMap, atlases)
        mapView.scale = SCALE
        addChild(mapView)

        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman()
        player.gridX = config.spawnX; player.gridY = config.spawnY
        player.play(SpriteAnimation.IDLE)

        for (npc in config.npcs) {
            val s = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
            s.loadFromSheet(npc.idleSheetPath)
            s.gridX = npc.tileX; s.gridY = npc.tileY; s.facing = npc.facing
            s.play(SpriteAnimation.IDLE)
        }

        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE

        // Apply 2D lighting — 3 candles in the tavern
        val tilePixelSize = (tiledMap.tileWidth * SCALE).toFloat()
        val lightingFilter = game.shader.LightingFilter(
            ambientDarkness = 0.12f,
            time = 1.7f,  // frozen moment for deterministic screenshot
        )
        lightingFilter.tilePixelSize = tilePixelSize
        lightingFilter.lights = listOf(
            // Bar area candle (warm, medium)
            game.shader.LightSource(tileX = 5, tileY = 7, radius = 6f, r = 1.0f, g = 0.8f, b = 0.4f, intensity = 0.9f, flickerSpeed = 2.5f, flickerAmount = 0.15f),
            // Center room candle (warm, large)
            game.shader.LightSource(tileX = 8, tileY = 12, radius = 7f, r = 1.0f, g = 0.85f, b = 0.5f, intensity = 1.0f, flickerSpeed = 3.0f, flickerAmount = 0.12f),
            // Corner candle (dimmer, smaller)
            game.shader.LightSource(tileX = 13, tileY = 15, radius = 4f, r = 0.9f, g = 0.7f, b = 0.3f, intensity = 0.7f, flickerSpeed = 4.0f, flickerAmount = 0.2f),
        )
        mapView.filter = lightingFilter

        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), "Heroes' Home (night)")

        save("shader_lighting")
    }
}

/**
 * Rain: Exterior scene with downpour (diagonal rain streaks).
 */
private fun captureShaderRain() {
    val config = MapConfig.exterior()
    korgeScreenshotTest(Size(VW, VH)) {
        val tiledMap = TmxLoader.parse(resourcesVfs[config.tmxPath].readString())
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }
        val mapView = TiledMapView(tiledMap, atlases)
        mapView.scale = SCALE
        addChild(mapView)

        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman()
        player.gridX = config.spawnX; player.gridY = config.spawnY
        player.facing = Facing.DOWN
        player.play(SpriteAnimation.WALK)

        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE

        // Rain shader (heavy downpour with slight wind)
        val rainFilter = game.shader.RainFilter(intensity = 0.8f, windAngle = 0.2f, time = 3.7f)
        mapView.filter = rainFilter

        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), "Village (RAINING)")

        save("shader_rain")
    }
}

/**
 * Heat shimmer: Interior near forge/oven — rising hot air distortion.
 */
private fun captureShaderHeatShimmer() {
    val config = MapConfig.interior()
    korgeScreenshotTest(Size(VW, VH)) {
        val tiledMap = TmxLoader.parse(resourcesVfs[config.tmxPath].readString())
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }
        val mapView = TiledMapView(tiledMap, atlases)
        mapView.scale = SCALE
        addChild(mapView)

        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman()
        player.gridX = config.spawnX; player.gridY = config.spawnY
        player.play(SpriteAnimation.IDLE)

        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE

        // Heat shimmer (strong, near forge)
        val heatFilter = game.shader.HeatShimmerFilter(intensity = 0.8f, time = 2.1f, frequency = 35f)
        mapView.filter = heatFilter

        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), "Heroes' Home (HOT)")

        save("shader_heat_shimmer")
    }
}

// =============================================================================
// 6d: SCRIPTED PLAYTHROUGH CAPTURES
// =============================================================================

private fun captureInteriorDialog() {
    val config = MapConfig.interior()
    korgeScreenshotTest(Size(VW, VH)) {
        val tiledMap = TmxLoader.parse(resourcesVfs[config.tmxPath].readString())
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }
        val mapView = TiledMapView(tiledMap, atlases)
        mapView.scale = SCALE
        addChild(mapView)
        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman(); player.gridX = config.spawnX; player.gridY = config.spawnY
        player.play(SpriteAnimation.IDLE)
        for (npc in config.npcs) {
            val s = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
            s.loadFromSheet(npc.idleSheetPath); s.gridX = npc.tileX; s.gridY = npc.tileY; s.facing = npc.facing
            s.play(SpriteAnimation.IDLE)
        }
        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE
        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), config.displayName)
        DialogOverlay(this, VW, VH).show(config.npcs.first().dialog)
        save("interior_dialog")
    }
}

private fun captureExteriorDialog() {
    val config = MapConfig.exterior()
    korgeScreenshotTest(Size(VW, VH)) {
        val tiledMap = TmxLoader.parse(resourcesVfs[config.tmxPath].readString())
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }
        val mapView = TiledMapView(tiledMap, atlases)
        mapView.scale = SCALE
        addChild(mapView)
        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman(); player.gridX = config.spawnX; player.gridY = config.spawnY
        player.play(SpriteAnimation.IDLE)
        for (npc in config.npcs) {
            val s = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
            s.loadFromSheet(npc.idleSheetPath); s.gridX = npc.tileX; s.gridY = npc.tileY; s.facing = npc.facing
            s.play(SpriteAnimation.IDLE)
        }
        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE
        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), config.displayName)
        DialogOverlay(this, VW, VH).show(config.npcs.first().dialog)
        save("exterior_dialog")
    }
}

private fun captureBattleMidway() {
    korgeScreenshotTest(Size(VW, VH)) {
        solidRect(VW, VH, RGBA(0x0a, 0x0a, 0x14, 0xff))
        // Hero 80 HP, Vampire 36 HP (after 2 attacks of 12 dmg each)
        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        val vampire = Combatant(id = "vampire_1", name = "Vampire", maxHp = 60, side = Side.ENEMY, attackPower = 8)
        // Simulate 2 attacks worth of damage on vampire
        vampire.takeDamage(24) // 60 - 24 = 36 HP
        val heroSprite = CharacterSprite(this, 48, 48)
        heroSprite.loadSwordsman(); heroSprite.gridX = 2; heroSprite.gridY = 3; heroSprite.facing = Facing.RIGHT
        heroSprite.play(SpriteAnimation.IDLE)
        val vampSprite = CharacterSprite(this, 48, 48)
        vampSprite.loadVampire(); vampSprite.gridX = 9; vampSprite.gridY = 3; vampSprite.facing = Facing.LEFT
        vampSprite.play(SpriteAnimation.HURT)
        val barW = 120.0
        solidRect(barW, 12.0, Colors["#333333"]).apply { x = 40.0; y = 20.0 }
        solidRect(barW * hero.hpFraction, 12.0, Colors["#22cc22"]).apply { x = 40.0; y = 20.0 }
        solidRect(barW, 12.0, Colors["#333333"]).apply { x = VW - 160.0; y = 20.0 }
        solidRect(barW * vampire.hpFraction, 12.0, Colors["#cc2222"]).apply { x = VW - 160.0; y = 20.0 }
        text("Nib: ${hero.hp}/${hero.maxHp}", textSize = 14.0, color = Colors.WHITE).apply { x = 40.0; y = 36.0 }
        text("Vampire: ${vampire.hp}/${vampire.maxHp}", textSize = 14.0, color = Colors.WHITE).apply { x = VW - 160.0; y = 36.0 }
        save("battle_midway")
    }
}

private fun captureBattleVictory() {
    korgeScreenshotTest(Size(VW, VH)) {
        solidRect(VW, VH, RGBA(0x0a, 0x0a, 0x14, 0xff))
        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        val vampire = Combatant(id = "vampire_1", name = "Vampire", maxHp = 60, side = Side.ENEMY, attackPower = 8)
        vampire.takeDamage(60) // dead
        val heroSprite = CharacterSprite(this, 48, 48)
        heroSprite.loadSwordsman(); heroSprite.gridX = 2; heroSprite.gridY = 3; heroSprite.facing = Facing.RIGHT
        heroSprite.play(SpriteAnimation.IDLE)
        val vampSprite = CharacterSprite(this, 48, 48)
        vampSprite.loadVampire(); vampSprite.gridX = 9; vampSprite.gridY = 3; vampSprite.facing = Facing.LEFT
        vampSprite.play(SpriteAnimation.DEATH)
        val barW = 120.0
        solidRect(barW, 12.0, Colors["#333333"]).apply { x = 40.0; y = 20.0 }
        solidRect(barW, 12.0, Colors["#22cc22"]).apply { x = 40.0; y = 20.0 }
        solidRect(barW, 12.0, Colors["#333333"]).apply { x = VW - 160.0; y = 20.0 }
        solidRect(0.0, 12.0, Colors["#cc2222"]).apply { x = VW - 160.0; y = 20.0 }
        text("Nib: 80/80", textSize = 14.0, color = Colors.WHITE).apply { x = 40.0; y = 36.0 }
        text("Vampire: 0/60", textSize = 14.0, color = Colors.WHITE).apply { x = VW - 160.0; y = 36.0 }
        text("VICTORY!", textSize = 24.0, color = Colors["#ffcc00"]).apply { x = VW / 2.0 - 60.0; y = VH / 2.0 - 12.0 }
        save("battle_victory")
    }
}

private fun captureQuestbookReaction() {
    val config = MapConfig.interior()
    korgeScreenshotTest(Size(VW, VH)) {
        val tiledMap = rpg.tiled.TmxLoader.parse(resourcesVfs[config.tmxPath].readString())
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }
        val mapView = TiledMapView(tiledMap, atlases)
        mapView.scale = SCALE
        addChild(mapView)
        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman(); player.gridX = config.spawnX; player.gridY = config.spawnY
        player.play(SpriteAnimation.IDLE)
        for (npc in config.npcs) {
            val s = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
            s.loadFromSheet(npc.idleSheetPath); s.gridX = npc.tileX; s.gridY = npc.tileY; s.facing = npc.facing
            s.play(SpriteAnimation.IDLE)
        }
        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE

        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), config.displayName)

        // Echte Reaktion aus der Pipeline (kein Mock):
        val director = SliceDirector { 0L }
        director.enterRoom(RoomContext(mapId = "tavern", roomId = RoomContext.ROOM_TAVERN, hasInteractableTarget = true))
        val outcome = director.fireBark(BarkEvent.NIB_SMELL_TREASURE)

        val questbook = QuestbookOverlay(this, VW, VH)
        if (outcome is BarkOutcome.Fired) {
            questbook.showReaction(outcome.reaction, director.pressure, director.questMarkers + director.falseMarkers)
        }

        save("questbook_reaction")
    }
}

private fun captureWorldPressureHigh() {
    val config = MapConfig.interior()
    korgeScreenshotTest(Size(VW, VH)) {
        val tiledMap = rpg.tiled.TmxLoader.parse(resourcesVfs[config.tmxPath].readString())
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }
        val mapView = TiledMapView(tiledMap, atlases)
        mapView.scale = SCALE
        addChild(mapView)
        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman(); player.gridX = config.spawnX; player.gridY = config.spawnY
        player.play(SpriteAnimation.IDLE)
        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE

        // Shader = State: Pressure HIGH → Poison-Shader on mapView
        val effects = game.shader.ShaderEffects()
        val shaderBinder = ShaderStateBinder(effects, mapView)
        effects.poisonFilter.time = 1.5f  // frozen animation frame for reproducibility
        shaderBinder.applyPressure(QuestPressure.HIGH)

        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), "Heroes' Home (PRESSURE HIGH)")
        QuestbookOverlay(this, VW, VH).refresh(QuestPressure.HIGH, listOf("Locate subterranean valuables", "Objection Pending (Case #0002)"))

        save("world_pressure_high")
    }
}

private fun captureBattleBossPhase2() {
    korgeScreenshotTest(Size(VW, VH)) {
        solidRect(VW, VH, RGBA(0x0a, 0x0a, 0x14, 0xff))

        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        // Simulate hero taking damage (40% HP)
        hero.takeDamage(48)

        val heroSprite = CharacterSprite(this, 48, 48)
        heroSprite.loadSwordsman(); heroSprite.gridX = 2; heroSprite.gridY = 3; heroSprite.facing = Facing.RIGHT
        heroSprite.play(SpriteAnimation.IDLE)
        val bossSprite = CharacterSprite(this, 48, 48)
        bossSprite.loadVampire(); bossSprite.gridX = 9; bossSprite.gridY = 3; bossSprite.facing = Facing.LEFT
        bossSprite.play(SpriteAnimation.ATTACK)

        val barW = 120.0
        solidRect(barW, 12.0, Colors["#333333"]).apply { x = 40.0; y = 20.0 }
        solidRect(barW * hero.hpFraction, 12.0, Colors["#22cc22"]).apply { x = 40.0; y = 20.0 }
        solidRect(barW, 12.0, Colors["#333333"]).apply { x = VW - 160.0; y = 20.0 }
        solidRect(barW * 0.7, 12.0, Colors["#cc2222"]).apply { x = VW - 160.0; y = 20.0 }
        text("Nib: ${hero.hp}/${hero.maxHp}", textSize = 14.0, color = Colors.WHITE).apply { x = 40.0; y = 36.0 }
        text("Rat Accountant: 70/100", textSize = 14.0, color = Colors.WHITE).apply { x = VW - 160.0; y = 36.0 }

        // Shader = State: combat distress (low HP + HIGH pressure)
        val effects = game.shader.ShaderEffects()
        val shaderBinder = ShaderStateBinder(effects, this)
        effects.poisonFilter.time = 1.5f
        shaderBinder.applyCombatDistress(hero.hpFraction, QuestPressure.HIGH)

        // Boss toast
        QuestbookOverlay(this, VW, VH).showMessage("The Accountant files objections!", QuestPressure.HIGH)

        save("battle_boss_phase2")
    }
}

private fun captureWorldRainPuddles() {
    val config = MapConfig.exterior()
    korgeScreenshotTest(Size(VW, VH)) {
        val tiledMap = rpg.tiled.TmxLoader.parse(resourcesVfs[config.tmxPath].readString())
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }
        val mapView = TiledMapView(tiledMap, atlases)
        mapView.scale = SCALE
        addChild(mapView)
        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman(); player.gridX = config.spawnX; player.gridY = config.spawnY
        player.play(SpriteAnimation.IDLE)
        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE

        // Rain + puddles. Grid offset -10/-5 → grid index (gx,gy) = tile (gx-10, gy-5).
        // Player spawns at tile (-5,9) = grid (5,14); fill around it so the camera
        // (centred on the player) actually frames the puddles.
        val grid = rpg.weather.WaterGrid(20, 20, offsetX = -10, offsetY = -5)
        for (x in 3..7) for (y in 12..16) grid[x, y] = 0.4f
        for (x in 8..11) for (y in 13..15) grid[x, y] = 0.3f
        grid[5, 14] = 0.85f // deep puddle at the player's feet
        val waterOverlay = WaterOverlay(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        waterOverlay.update(grid)

        // Rain shader
        val effects = game.shader.ShaderEffects()
        effects.rainFilter.intensity = 0.7f
        effects.rainFilter.time = 2.0f
        effects.attachRain(mapView)

        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), "Village (RAINING)")
        save("world_rain_puddles")
    }
}

private fun captureWorldPuddleDrain() {
    val config = MapConfig.exterior()
    korgeScreenshotTest(Size(VW, VH)) {
        val tiledMap = rpg.tiled.TmxLoader.parse(resourcesVfs[config.tmxPath].readString())
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }
        val mapView = TiledMapView(tiledMap, atlases)
        mapView.scale = SCALE
        addChild(mapView)
        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman(); player.gridX = config.spawnX; player.gridY = config.spawnY
        player.play(SpriteAnimation.IDLE)
        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE

        // Puddles with a drain tile — water flows toward drain, lower levels visible.
        // Drain + fill placed around the player tile (-5,9) = grid (5,14) so they're framed.
        val drainTiles = setOf(5 to 14) // drain at the player's feet
        val grid = rpg.weather.WaterGrid(20, 20, offsetX = -10, offsetY = -5, drainTiles = drainTiles)
        for (x in 3..7) for (y in 12..16) grid[x, y] = 0.5f
        // Simulate a few flow steps so the drain depression around (5,14) is visible
        repeat(5) { grid.flowStep() }
        val waterOverlay = WaterOverlay(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        waterOverlay.update(grid)

        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), "Village (DRAIN)")
        save("world_puddle_drain")
    }
}

private fun captureWorldLantern() {
    val config = MapConfig.interior()
    korgeScreenshotTest(Size(VW, VH)) {
        val tiledMap = rpg.tiled.TmxLoader.parse(resourcesVfs[config.tmxPath].readString())
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }
        val mapView = TiledMapView(tiledMap, atlases)
        mapView.scale = SCALE
        addChild(mapView)
        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman(); player.gridX = config.spawnX; player.gridY = config.spawnY
        player.play(SpriteAnimation.IDLE)
        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE

        // Lantern lighting on player
        val tilePx = (tiledMap.tileWidth * SCALE).toFloat()
        val effects = game.shader.ShaderEffects()
        effects.lightingFilter.ambientDarkness = 0.1f
        effects.lightingFilter.tilePixelSize = tilePx
        effects.lightingFilter.time = 1.5f
        effects.lightingFilter.lights = listOf(
            game.shader.LightSource(tileX = config.spawnX, tileY = config.spawnY,
                radius = 5f, r = 1.0f, g = 0.8f, b = 0.4f, intensity = 0.9f, flickerSpeed = 3f)
        )
        effects.attachLighting(mapView, effects.lightingFilter.lights, tilePx)

        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), "Heroes' Home (LANTERN)")
        save("world_lantern")
    }
}

private fun captureWorldDrunk() {
    val config = MapConfig.interior()
    korgeScreenshotTest(Size(VW, VH)) {
        val tiledMap = rpg.tiled.TmxLoader.parse(resourcesVfs[config.tmxPath].readString())
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }
        val mapView = TiledMapView(tiledMap, atlases)
        mapView.scale = SCALE
        addChild(mapView)
        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman(); player.gridX = config.spawnX; player.gridY = config.spawnY
        player.play(SpriteAnimation.IDLE)
        for (npc in config.npcs) {
            val s = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
            s.loadFromSheet(npc.idleSheetPath); s.gridX = npc.tileX; s.gridY = npc.tileY; s.facing = npc.facing
            s.play(SpriteAnimation.IDLE)
        }
        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE

        // Beer Goggles (3 ales deep)
        val effects = game.shader.ShaderEffects()
        effects.beerGoggleFilter.drunkLevel = 0.7f
        effects.beerGoggleFilter.time = 3.0f
        effects.attachBeerGoggle(mapView)

        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 12), "Heroes' Home (DRUNK)")
        save("world_drunk")
    }
}

private fun captureQuestbookOpen() {
    korgeScreenshotTest(Size(VW, VH)) {
        // Simulate an open questbook with real pipeline data
        val director = SliceDirector { 0L }
        director.enterRoom(rpg.questbook.RoomContext(mapId = "tavern", roomId = rpg.questbook.RoomContext.ROOM_TAVERN, hasInteractableTarget = true))
        director.fireBark(BarkEvent.NIB_SMELL_TREASURE)
        director.fireBark(BarkEvent.BRUGG_ATTACK)

        // Build the questbook screen content directly (no scene transition in harness)
        solidRect(VW, VH, RGBA(0x1a, 0x12, 0x08, 0xff))
        val bookW = VW * 0.85; val bookH = VH * 0.8
        val bookX = (VW - bookW) / 2.0; val bookY = (VH - bookH) / 2.0
        solidRect(bookW, bookH, RGBA(0xf5, 0xe6, 0xc8, 0xff)).apply { x = bookX; y = bookY }
        solidRect(4.0, bookH, Colors["#5c3a1e"]).apply { x = bookX + bookW / 2.0 - 2.0; y = bookY }

        // Left page: entries
        text("OFFICIAL QUEST LOG", textSize = 12.0, color = Colors["#5c3a1e"]).apply { x = bookX + 16.0; y = bookY + 16.0 }
        for ((i, entry) in director.questbook.log.withIndex()) {
            val t = if (entry.questbookText.length > 45) entry.questbookText.take(45) + "..." else entry.questbookText
            text("\u2022 $t", textSize = 10.0, color = Colors["#3a2a1a"]).apply { x = bookX + 16.0; y = bookY + 36.0 + i * 16.0 }
        }

        // Right page: markers + party
        val rightX = bookX + bookW / 2.0 + 16.0
        text("ACTIVE ASSIGNMENTS", textSize = 12.0, color = Colors["#5c3a1e"]).apply { x = rightX; y = bookY + 16.0 }
        val allMarkers = director.questMarkers + director.falseMarkers
        for ((i, m) in allMarkers.withIndex()) {
            text("\u25B6 $m", textSize = 10.0, color = Colors["#3a2a1a"]).apply { x = rightX; y = bookY + 36.0 + i * 16.0 }
        }
        text("PRESSURE: ${director.pressure.name}", textSize = 10.0, color = Colors["#22cc22"]).apply { x = rightX; y = bookY + bookH - 30.0 }

        save("questbook_open")
    }
}

// =============================================================================
// STEP 8: FROZEN APPROACH CAPTURES
// =============================================================================

/**
 * The Frozen Approach: Winter night scene with snow, torch lighting, and fog.
 * Uses Exterior.tmx in frozenApproach() configuration.
 */
private fun captureFrozenApproach() {
    val config = MapConfig.frozenApproach()
    korgeScreenshotTest(Size(VW, VH)) {
        val tiledMap = TmxLoader.parse(resourcesVfs[config.tmxPath].readString())
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }
        val mapView = TiledMapView(tiledMap, atlases)
        mapView.scale = SCALE
        addChild(mapView)

        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman()
        player.gridX = config.spawnX; player.gridY = config.spawnY
        player.play(SpriteAnimation.IDLE)

        // Camera centered on player
        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE

        // Snow: fill around player spawn (-5,9). Grid offsets (-10,-5) so player = index (5,14).
        val snowGrid = SnowGrid(20, 20, offsetX = -10, offsetY = -5)
        val footprintGrid = FootprintGrid(20, 20, offsetX = -10, offsetY = -5)
        for (x in -8..-2) for (y in 7..11) snowGrid[x, y] = 0.7f
        // Extra depth around player
        for (x in -6..-4) for (y in 8..10) snowGrid[x, y] = 0.8f

        val snowOverlay = SnowOverlay(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        snowOverlay.update(snowGrid, footprintGrid)

        // Lighting: ambient dark night with one warm torch at player position
        val tilePx = (tiledMap.tileWidth * SCALE).toFloat()
        val effects = game.shader.ShaderEffects()
        // Dusk, not pitch black, so the snow + warm torch cone both read.
        effects.lightingFilter.ambientDarkness = 0.4f
        effects.lightingFilter.tilePixelSize = tilePx
        effects.lightingFilter.time = 1.5f
        effects.lightingFilter.lights = listOf(
            game.shader.LightSource(
                tileX = config.spawnX, tileY = config.spawnY,
                radius = 7f, r = 1.0f, g = 0.82f, b = 0.45f,
                intensity = 1.4f, flickerSpeed = 3f, flickerAmount = 0.15f
            )
        )
        // Lighting COMPOSED WITH FOG (Step 9: ComposedFilter enables both simultaneously)
        // Night ambient + warm torch glow + misty fog — the full atmosphere.
        effects.fogFilter.density = 0.3f
        effects.fogFilter.time = 1.5f
        effects.fogFilter.driftX = 0.2f
        effects.attachLighting(mapView, effects.lightingFilter.lights, tilePx)
        effects.enable(mapView, effects.fogFilter)

        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), "The Frozen Approach")

        save("frozen_approach")
    }
}

/**
 * Frozen Approach with footprints: A trail of boot prints in the snow
 * coming from the south toward the player position.
 */
private fun captureFrozenFootprints() {
    val config = MapConfig.frozenApproach()
    korgeScreenshotTest(Size(VW, VH)) {
        val tiledMap = TmxLoader.parse(resourcesVfs[config.tmxPath].readString())
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }
        val mapView = TiledMapView(tiledMap, atlases)
        mapView.scale = SCALE
        addChild(mapView)

        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman()
        player.gridX = config.spawnX; player.gridY = config.spawnY
        player.play(SpriteAnimation.IDLE)

        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE

        // Snow grid around player
        val snowGrid = SnowGrid(20, 20, offsetX = -10, offsetY = -5)
        for (x in -8..-2) for (y in 7..11) snowGrid[x, y] = 0.7f
        for (x in -6..-4) for (y in 8..10) snowGrid[x, y] = 0.8f

        // Footprints: trail from south (y=11) toward player (y=9), at x=-5
        val footprintGrid = FootprintGrid(20, 20, offsetX = -10, offsetY = -5)
        // 7 stamps in a line approaching from the south
        footprintGrid.stamp(-5, 11)
        footprintGrid.stamp(-5, 10)
        footprintGrid.stamp(-4, 10)
        footprintGrid.stamp(-4, 9)
        footprintGrid.stamp(-5, 9)
        footprintGrid.stamp(-6, 9)
        footprintGrid.stamp(-6, 8)

        val snowOverlay = SnowOverlay(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        snowOverlay.update(snowGrid, footprintGrid)

        val footprintOverlay = FootprintOverlay(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        footprintOverlay.update(footprintGrid)

        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), "The Frozen Approach (FOOTPRINTS)")

        save("frozen_footprints")
    }
}

/**
 * Frozen Approach with blood: Fresh and old blood spills on snow.
 * Blood on white snow creates high-contrast horror atmosphere.
 */
private fun captureFrozenBlood() {
    val config = MapConfig.frozenApproach()
    korgeScreenshotTest(Size(VW, VH)) {
        val tiledMap = TmxLoader.parse(resourcesVfs[config.tmxPath].readString())
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }
        val mapView = TiledMapView(tiledMap, atlases)
        mapView.scale = SCALE
        addChild(mapView)

        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman()
        player.gridX = config.spawnX; player.gridY = config.spawnY
        player.play(SpriteAnimation.IDLE)

        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE

        // Snow grid
        val snowGrid = SnowGrid(20, 20, offsetX = -10, offsetY = -5)
        for (x in -8..-2) for (y in 7..11) snowGrid[x, y] = 0.75f

        // Blood grid: fresh spills close to player, old spills further out
        val bloodGrid = BloodGrid(20, 20, offsetX = -10, offsetY = -5)
        // Fresh blood (close to player at -5,9)
        bloodGrid.spill(-5, 9, 0.9f)   // right at player feet
        bloodGrid.spill(-4, 9, 0.7f)   // nearby
        bloodGrid.spill(-5, 8, 0.6f)   // nearby

        // Old blood (further away) -- age after spilling
        bloodGrid.spill(-7, 10, 0.8f)
        bloodGrid.spill(-3, 11, 0.5f)
        bloodGrid.spill(-6, 7, 0.6f)
        // Age only the old ones: we age all then re-spill fresh ones
        bloodGrid.age(0.8f)
        // Re-spill fresh to overwrite the aged freshness at player
        bloodGrid.spill(-5, 9, 0.9f)
        bloodGrid.spill(-4, 9, 0.7f)
        bloodGrid.spill(-5, 8, 0.6f)

        val footprintGrid = FootprintGrid(20, 20, offsetX = -10, offsetY = -5)
        val snowOverlay = SnowOverlay(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        snowOverlay.update(snowGrid, footprintGrid)

        val bloodOverlay = BloodOverlay(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        bloodOverlay.update(bloodGrid, snowGrid)

        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), "The Frozen Approach (BLOOD)")

        save("frozen_blood")
    }
}

/**
 * Questbook in its full glory: decorated parchment book at final open state.
 * Renders the same visual treatment as QuestbookScreen but frozen at scaleX=1, scaleY=1.
 */
private fun captureQuestbookGlory() {
    korgeScreenshotTest(Size(VW, VH)) {
        // Real pipeline data
        val director = SliceDirector { 0L }
        director.enterRoom(rpg.questbook.RoomContext(mapId = "tavern", roomId = rpg.questbook.RoomContext.ROOM_TAVERN, hasInteractableTarget = true))
        director.fireBark(BarkEvent.NIB_SMELL_TREASURE)
        director.fireBark(BarkEvent.BRUGG_ATTACK)
        director.fireBark(BarkEvent.GUARD_BACK_ALREADY)

        // Dark background
        solidRect(VW, VH, RGBA(0x12, 0x0c, 0x06, 0xff))

        val bookW = VW * 0.85
        val bookH = VH * 0.8
        val bookX = (VW - bookW) / 2.0
        val bookY = (VH - bookH) / 2.0

        // --- Parchment gradient (aged paper look) ---
        val parchmentColors = listOf(
            RGBA(0xf8, 0xf0, 0xd8, 0xff),
            RGBA(0xf2, 0xe4, 0xc4, 0xff),
            RGBA(0xe8, 0xd8, 0xb0, 0xff),
            RGBA(0xde, 0xcc, 0x9c, 0xff),
        )
        // Outer shadow
        solidRect(bookW + 8.0, bookH + 8.0, RGBA(0x2a, 0x1a, 0x0a, 0xcc))
            .apply { x = bookX - 4.0; y = bookY - 4.0 }

        // Gradient layers
        for ((i, color) in parchmentColors.reversed().withIndex()) {
            val inset = i * 3.0
            solidRect(bookW - inset * 2, bookH - inset * 2, color)
                .apply { x = bookX + inset; y = bookY + inset }
        }

        // Worn edge stains
        solidRect(bookW, 6.0, RGBA(0xc8, 0xb0, 0x88, 0x44))
            .apply { x = bookX; y = bookY }
        solidRect(bookW, 6.0, RGBA(0xc8, 0xb0, 0x88, 0x44))
            .apply { x = bookX; y = bookY + bookH - 6.0 }
        solidRect(6.0, bookH, RGBA(0xc8, 0xb0, 0x88, 0x44))
            .apply { x = bookX; y = bookY }
        solidRect(6.0, bookH, RGBA(0xc8, 0xb0, 0x88, 0x44))
            .apply { x = bookX + bookW - 6.0; y = bookY }

        // --- Binding / Spine ---
        val spineX = bookX + bookW / 2.0
        solidRect(2.0, bookH, RGBA(0x2a, 0x18, 0x08, 0xff)).apply { x = spineX - 5.0; y = bookY }
        solidRect(3.0, bookH, RGBA(0x3a, 0x24, 0x10, 0xff)).apply { x = spineX - 3.0; y = bookY }
        solidRect(6.0, bookH, RGBA(0x4a, 0x30, 0x18, 0xff)).apply { x = spineX - 3.0; y = bookY }
        solidRect(3.0, bookH, RGBA(0x3a, 0x24, 0x10, 0xff)).apply { x = spineX + 3.0; y = bookY }
        solidRect(2.0, bookH, RGBA(0x2a, 0x18, 0x08, 0xff)).apply { x = spineX + 5.0; y = bookY }
        solidRect(1.0, bookH, RGBA(0x6a, 0x50, 0x30, 0xff)).apply { x = spineX; y = bookY }

        // --- Framed pages ---
        val frameInset = 10.0
        val pageW = bookW / 2.0 - 20.0
        val pageH = bookH - frameInset * 2

        // Left page
        val leftPageX = bookX + frameInset
        val leftPageY = bookY + frameInset
        solidRect(pageW + 4.0, pageH + 4.0, RGBA(0x8a, 0x6a, 0x44, 0xff))
            .apply { x = leftPageX - 2.0; y = leftPageY - 2.0 }
        solidRect(pageW, pageH, RGBA(0xf5, 0xe8, 0xcc, 0xff))
            .apply { x = leftPageX; y = leftPageY }
        // Inner ornamental borders
        solidRect(pageW - 8.0, 1.0, RGBA(0xaa, 0x88, 0x55, 0x88))
            .apply { x = leftPageX + 4.0; y = leftPageY + 4.0 }
        solidRect(pageW - 8.0, 1.0, RGBA(0xaa, 0x88, 0x55, 0x88))
            .apply { x = leftPageX + 4.0; y = leftPageY + pageH - 5.0 }
        solidRect(1.0, pageH - 8.0, RGBA(0xaa, 0x88, 0x55, 0x88))
            .apply { x = leftPageX + 4.0; y = leftPageY + 4.0 }
        solidRect(1.0, pageH - 8.0, RGBA(0xaa, 0x88, 0x55, 0x88))
            .apply { x = leftPageX + pageW - 5.0; y = leftPageY + 4.0 }

        // Right page
        val rightPageX = bookX + bookW / 2.0 + 10.0
        val rightPageY = bookY + frameInset
        solidRect(pageW + 4.0, pageH + 4.0, RGBA(0x8a, 0x6a, 0x44, 0xff))
            .apply { x = rightPageX - 2.0; y = rightPageY - 2.0 }
        solidRect(pageW, pageH, RGBA(0xf5, 0xe8, 0xcc, 0xff))
            .apply { x = rightPageX; y = rightPageY }
        solidRect(pageW - 8.0, 1.0, RGBA(0xaa, 0x88, 0x55, 0x88))
            .apply { x = rightPageX + 4.0; y = rightPageY + 4.0 }
        solidRect(pageW - 8.0, 1.0, RGBA(0xaa, 0x88, 0x55, 0x88))
            .apply { x = rightPageX + 4.0; y = rightPageY + pageH - 5.0 }
        solidRect(1.0, pageH - 8.0, RGBA(0xaa, 0x88, 0x55, 0x88))
            .apply { x = rightPageX + 4.0; y = rightPageY + 4.0 }
        solidRect(1.0, pageH - 8.0, RGBA(0xaa, 0x88, 0x55, 0x88))
            .apply { x = rightPageX + pageW - 5.0; y = rightPageY + 4.0 }

        // Page shadows at spine
        solidRect(3.0, pageH, RGBA(0x44, 0x33, 0x22, 0x44))
            .apply { x = leftPageX + pageW; y = leftPageY }
        solidRect(3.0, pageH, RGBA(0x44, 0x33, 0x22, 0x44))
            .apply { x = rightPageX - 3.0; y = rightPageY }

        // Dog-ear (top-right)
        val dogEarSize = 14.0
        solidRect(dogEarSize, dogEarSize, RGBA(0xd8, 0xc4, 0xa0, 0xff))
            .apply { x = rightPageX + pageW - dogEarSize; y = rightPageY }
        solidRect(dogEarSize, 2.0, RGBA(0x66, 0x50, 0x33, 0x88))
            .apply { x = rightPageX + pageW - dogEarSize; y = rightPageY + dogEarSize }
        solidRect(2.0, dogEarSize, RGBA(0x66, 0x50, 0x33, 0x88))
            .apply { x = rightPageX + pageW - dogEarSize - 2.0; y = rightPageY }
        solidRect(dogEarSize * 0.7, dogEarSize * 0.7, RGBA(0xc0, 0xaa, 0x80, 0xff))
            .apply { x = rightPageX + pageW - dogEarSize * 0.7; y = rightPageY }

        // --- Content from SliceDirector ---
        val contentLeftX = leftPageX + 14.0
        val contentLeftY = leftPageY + 14.0
        val contentRightX = rightPageX + 14.0
        val contentRightY = rightPageY + 14.0

        // Left page: quest log entries
        text("OFFICIAL QUEST LOG", textSize = 12.0, color = RGBA(0x5c, 0x3a, 0x1e, 0xff))
            .apply { x = contentLeftX; y = contentLeftY }
        for ((i, entry) in director.questbook.log.withIndex()) {
            val t = if (entry.questbookText.length > 42)
                entry.questbookText.take(42) + "..." else entry.questbookText
            text("\u2022 $t", textSize = 10.0, color = RGBA(0x3a, 0x2a, 0x1a, 0xff))
                .apply { x = contentLeftX; y = contentLeftY + 22.0 + i * 16.0 }
        }

        // Right page: assignments + pressure
        text("ACTIVE ASSIGNMENTS", textSize = 12.0, color = RGBA(0x5c, 0x3a, 0x1e, 0xff))
            .apply { x = contentRightX; y = contentRightY }
        val allMarkers = director.questMarkers + director.falseMarkers
        for ((i, m) in allMarkers.withIndex()) {
            val markerText = if (m.length > 38) m.take(38) + "..." else m
            text("\u25B6 $markerText", textSize = 10.0, color = RGBA(0x3a, 0x2a, 0x1a, 0xff))
                .apply { x = contentRightX; y = contentRightY + 22.0 + i * 16.0 }
        }
        text("REGISTERED PARTY:", textSize = 10.0, color = RGBA(0x5c, 0x3a, 0x1e, 0xff))
            .apply { x = contentRightX; y = contentRightY + 130.0 }
        text("Nib & Company", textSize = 11.0, color = RGBA(0x3a, 0x2a, 0x1a, 0xff))
            .apply { x = contentRightX; y = contentRightY + 144.0 }
        text("BUREAUCRATIC PRESSURE: ${director.pressure.name}", textSize = 10.0, color = RGBA(0x22, 0xcc, 0x22, 0xff))
            .apply { x = contentRightX; y = contentRightY + 170.0 }

        save("questbook_glory")
    }
}

// =============================================================================
// 4-SEASONS SHOWCASE CAPTURES
// =============================================================================

/**
 * Spring Approach: Bright morning with flowers blooming.
 * Pink and yellow flowers visible on tiles, some trampled near the player.
 */
private fun captureSpringApproach() {
    val config = MapConfig.springApproach()
    korgeScreenshotTest(Size(VW, VH)) {
        val tiledMap = TmxLoader.parse(resourcesVfs[config.tmxPath].readString())
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }
        val mapView = TiledMapView(tiledMap, atlases)
        mapView.scale = SCALE
        addChild(mapView)

        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman()
        player.gridX = config.spawnX; player.gridY = config.spawnY
        player.play(SpriteAnimation.IDLE)

        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE

        // Spring flowers: grid offset (-10,-5), player = index (5,14)
        val grid = SeasonalGrid(20, 20, offsetX = -10, offsetY = -5)
        grid.initFlowers(0.7f)

        // Trample some flowers near the player to show the effect
        grid.trampleFlower(-5, 9, 0.5f)
        grid.trampleFlower(-4, 9, 0.3f)
        grid.trampleFlower(-5, 10, 0.4f)

        val springOverlay = SpringOverlay(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        springOverlay.update(grid)

        // Bright spring lighting (morning light, mild)
        val tilePx = (tiledMap.tileWidth * SCALE).toFloat()
        val effects = game.shader.ShaderEffects()
        effects.lightingFilter.ambientDarkness = 0.15f
        effects.lightingFilter.tilePixelSize = tilePx
        effects.lightingFilter.time = 1.5f
        effects.lightingFilter.lights = listOf(
            game.shader.LightSource(
                tileX = config.spawnX, tileY = config.spawnY,
                radius = 8f, r = 1.0f, g = 0.95f, b = 0.8f,
                intensity = 0.7f, flickerSpeed = 0f, flickerAmount = 0f
            )
        )
        effects.attachLighting(mapView, effects.lightingFilter.lights, tilePx)

        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), "The Spring Approach")

        save("spring_approach")
    }
}

/**
 * Summer Approach: Long day, lush grass bending in wind.
 * Green grass tufts at tile bottoms, some bent where player walked.
 */
private fun captureSummerApproach() {
    val config = MapConfig.summerApproach()
    korgeScreenshotTest(Size(VW, VH)) {
        val tiledMap = TmxLoader.parse(resourcesVfs[config.tmxPath].readString())
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }
        val mapView = TiledMapView(tiledMap, atlases)
        mapView.scale = SCALE
        addChild(mapView)

        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman()
        player.gridX = config.spawnX; player.gridY = config.spawnY
        player.play(SpriteAnimation.IDLE)

        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE

        // Summer grass: grid offset (-10,-5), player = index (5,14)
        val grid = SeasonalGrid(20, 20, offsetX = -10, offsetY = -5)

        // Bend grass where player walked (recent trail)
        grid.bendGrass(-5, 10)
        grid.bendGrass(-5, 9)
        grid.bendGrass(-4, 9)

        val windState = WindState().apply { dx = 0.4f; strength = 0.3f }
        val summerOverlay = SummerOverlay(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        summerOverlay.update(grid, windState, elapsedTime = 2.5f)

        // Warm summer light (long day, bright)
        val tilePx = (tiledMap.tileWidth * SCALE).toFloat()
        val effects = game.shader.ShaderEffects()
        effects.lightingFilter.ambientDarkness = 0.05f
        effects.lightingFilter.tilePixelSize = tilePx
        effects.lightingFilter.time = 1.5f
        effects.lightingFilter.lights = listOf(
            game.shader.LightSource(
                tileX = config.spawnX, tileY = config.spawnY,
                radius = 10f, r = 1.0f, g = 0.98f, b = 0.9f,
                intensity = 0.5f, flickerSpeed = 0f, flickerAmount = 0f
            )
        )
        effects.attachLighting(mapView, effects.lightingFilter.lights, tilePx)

        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), "The Summer Approach")

        save("summer_approach")
    }
}

/**
 * Autumn Approach: Overcast, rain, fallen leaves on the ground.
 * Orange/brown/red leaves scattered, darker sky, rain shader.
 */
private fun captureAutumnApproach() {
    val config = MapConfig.autumnApproach()
    korgeScreenshotTest(Size(VW, VH)) {
        val tiledMap = TmxLoader.parse(resourcesVfs[config.tmxPath].readString())
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }
        val mapView = TiledMapView(tiledMap, atlases)
        mapView.scale = SCALE
        addChild(mapView)

        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman()
        player.gridX = config.spawnX; player.gridY = config.spawnY
        player.play(SpriteAnimation.IDLE)

        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE

        // Autumn leaves: grid offset (-10,-5), player = index (5,14)
        val grid = SeasonalGrid(20, 20, offsetX = -10, offsetY = -5)
        // Fill leaves around player position for visibility
        grid.dropLeaves(0.5f, timeStep = 0)
        grid.dropLeaves(0.3f, timeStep = 1)

        // Player kicked leaves at current position
        grid.kickLeaves(-5, 9)

        val autumnOverlay = AutumnOverlay(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        autumnOverlay.update(grid)

        // Dark overcast lighting + rain
        val tilePx = (tiledMap.tileWidth * SCALE).toFloat()
        val effects = game.shader.ShaderEffects()
        effects.lightingFilter.ambientDarkness = 0.35f
        effects.lightingFilter.tilePixelSize = tilePx
        effects.lightingFilter.time = 1.5f
        effects.lightingFilter.lights = listOf(
            game.shader.LightSource(
                tileX = config.spawnX, tileY = config.spawnY,
                radius = 5f, r = 0.9f, g = 0.85f, b = 0.7f,
                intensity = 0.6f, flickerSpeed = 0f, flickerAmount = 0f
            )
        )
        effects.attachLighting(mapView, effects.lightingFilter.lights, tilePx)

        // Rain shader for autumn weather
        effects.rainFilter.intensity = 0.6f
        effects.rainFilter.time = 3.0f
        effects.rainFilter.windAngle = 0.3f
        effects.attachRain(mapView)

        // Fog for atmosphere
        effects.fogFilter.density = 0.2f
        effects.fogFilter.time = 2.0f
        effects.attachFog(mapView)

        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), "The Autumn Approach")

        save("autumn_approach")
    }
}

/**
 * Winter Approach: Reuses the existing frozen_approach scene as the winter
 * entry in the 4-seasons showcase. Cold night, snow, fog, torch light.
 */
private fun captureWinterApproach() {
    val config = MapConfig.frozenApproach()
    korgeScreenshotTest(Size(VW, VH)) {
        val tiledMap = TmxLoader.parse(resourcesVfs[config.tmxPath].readString())
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }
        val mapView = TiledMapView(tiledMap, atlases)
        mapView.scale = SCALE
        addChild(mapView)

        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman()
        player.gridX = config.spawnX; player.gridY = config.spawnY
        player.play(SpriteAnimation.IDLE)

        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE

        // Snow grid around player
        val snowGrid = SnowGrid(20, 20, offsetX = -10, offsetY = -5)
        val footprintGrid = FootprintGrid(20, 20, offsetX = -10, offsetY = -5)
        for (x in -8..-2) for (y in 7..11) snowGrid[x, y] = 0.75f
        for (x in -6..-4) for (y in 8..10) snowGrid[x, y] = 0.85f

        // Footprints approaching from south
        footprintGrid.stamp(-5, 11)
        footprintGrid.stamp(-5, 10)
        footprintGrid.stamp(-5, 9)

        val snowOverlay = SnowOverlay(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        snowOverlay.update(snowGrid, footprintGrid)

        val footprintOverlay = FootprintOverlay(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        footprintOverlay.update(footprintGrid)

        // Night lighting with torch
        val tilePx = (tiledMap.tileWidth * SCALE).toFloat()
        val effects = game.shader.ShaderEffects()
        effects.lightingFilter.ambientDarkness = 0.08f
        effects.lightingFilter.tilePixelSize = tilePx
        effects.lightingFilter.time = 1.5f
        effects.lightingFilter.lights = listOf(
            game.shader.LightSource(
                tileX = config.spawnX, tileY = config.spawnY,
                radius = 6f, r = 1.0f, g = 0.8f, b = 0.4f,
                intensity = 0.9f, flickerSpeed = 3f, flickerAmount = 0.15f
            )
        )
        effects.attachLighting(mapView, effects.lightingFilter.lights, tilePx)

        // Fog
        effects.fogFilter.density = 0.35f
        effects.fogFilter.time = 2.0f
        effects.attachFog(mapView)

        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), "The Winter Approach")

        save("winter_approach")
    }
}

// =============================================================================
// STEP 9: FILTER COMPOSITION + MATERIAL FATIGUE CAPTURES
// =============================================================================

/**
 * Demonstrates filter composition: Lighting AND Fog simultaneously visible.
 * Dark night with warm torch glow PLUS fog veil — both clearly readable at once.
 */
private fun captureComposeLightingFog() {
    val config = MapConfig.interior()
    korgeScreenshotTest(Size(VW, VH)) {
        val tiledMap = TmxLoader.parse(resourcesVfs[config.tmxPath].readString())
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }
        val mapView = TiledMapView(tiledMap, atlases)
        mapView.scale = SCALE
        addChild(mapView)

        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman()
        player.gridX = config.spawnX; player.gridY = config.spawnY
        player.play(SpriteAnimation.IDLE)

        for (npc in config.npcs) {
            val s = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
            s.loadFromSheet(npc.idleSheetPath)
            s.gridX = npc.tileX; s.gridY = npc.tileY; s.facing = npc.facing
            s.play(SpriteAnimation.IDLE)
        }

        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE

        val tilePx = (tiledMap.tileWidth * SCALE).toFloat()
        val effects = game.shader.ShaderEffects()

        // Night lighting: dark ambient + warm candle
        effects.lightingFilter.ambientDarkness = 0.12f
        effects.lightingFilter.tilePixelSize = tilePx
        effects.lightingFilter.time = 1.7f
        effects.lightingFilter.lights = listOf(
            game.shader.LightSource(
                tileX = config.spawnX, tileY = config.spawnY,
                radius = 6f, r = 1.0f, g = 0.8f, b = 0.4f,
                intensity = 1.0f, flickerSpeed = 2.5f, flickerAmount = 0.15f
            )
        )
        // Fog: moderate density, visible drift
        effects.fogFilter.density = 0.35f
        effects.fogFilter.time = 2.0f
        effects.fogFilter.driftX = 0.3f

        // COMPOSED: both active simultaneously via ComposedFilter
        effects.enable(mapView, effects.lightingFilter)
        effects.enable(mapView, effects.fogFilter)

        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), "Composed: Lighting + Fog")

        save("compose_lighting_fog")
    }
}

/**
 * Material fatigue: visible cracks appearing at different stress levels.
 * Some tiles cracked (hairline), some broken (major fractures).
 */
private fun captureMaterialFatigue() {
    val config = MapConfig.interior()
    korgeScreenshotTest(Size(VW, VH)) {
        val tiledMap = TmxLoader.parse(resourcesVfs[config.tmxPath].readString())
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }
        val mapView = TiledMapView(tiledMap, atlases)
        mapView.scale = SCALE
        addChild(mapView)

        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman()
        player.gridX = config.spawnX; player.gridY = config.spawnY
        player.play(SpriteAnimation.IDLE)

        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE

        // Material fatigue grid: various stress levels around the player
        val fatigue = rpg.weather.MaterialFatigue(20, 20, offsetX = -10, offsetY = -5)

        // Light cracks near player (above threshold 0.3)
        fatigue.addStress(-5, 8, 0.4f)
        fatigue.addStress(-4, 9, 0.35f)
        fatigue.addStress(-6, 9, 0.45f)

        // Medium cracks
        fatigue.addStress(-5, 10, 0.55f)
        fatigue.addStress(-3, 9, 0.6f)

        // Broken tiles (above 0.7)
        fatigue.addStress(-4, 10, 0.8f)
        fatigue.addStressRadius(-6, 10, radius = 1, amount = 0.75f)

        val fatigueOverlay = MaterialFatigueOverlay(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        fatigueOverlay.update(fatigue)

        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), "Material Fatigue")

        save("material_fatigue")
    }
}

// =============================================================================
// STEP 10: STOKEPORT LOCATION MAP CAPTURES
// =============================================================================

private fun captureChapel() {
    val config = MapConfig.chapel()
    korgeScreenshotTest(Size(VW, VH)) {
        val tiledMap = TmxLoader.parse(resourcesVfs[config.tmxPath].readString())
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }
        val mapView = TiledMapView(tiledMap, atlases)
        mapView.scale = SCALE
        addChild(mapView)
        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman()
        player.gridX = config.spawnX; player.gridY = config.spawnY
        player.play(SpriteAnimation.IDLE)
        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE
        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), config.displayName)
        save("map_chapel")
    }
}

private fun captureGuildHall() {
    val config = MapConfig.guildHall()
    korgeScreenshotTest(Size(VW, VH)) {
        val tiledMap = TmxLoader.parse(resourcesVfs[config.tmxPath].readString())
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }
        val mapView = TiledMapView(tiledMap, atlases)
        mapView.scale = SCALE
        addChild(mapView)
        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman()
        player.gridX = config.spawnX; player.gridY = config.spawnY
        player.play(SpriteAnimation.IDLE)
        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE
        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), config.displayName)
        save("map_guildhall")
    }
}

private fun captureGlassblowers() {
    val config = MapConfig.glassblowers()
    korgeScreenshotTest(Size(VW, VH)) {
        val tiledMap = TmxLoader.parse(resourcesVfs[config.tmxPath].readString())
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }
        val mapView = TiledMapView(tiledMap, atlases)
        mapView.scale = SCALE
        addChild(mapView)
        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman()
        player.gridX = config.spawnX; player.gridY = config.spawnY
        player.play(SpriteAnimation.IDLE)
        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE
        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), config.displayName)
        save("map_glassblowers")
    }
}

private fun captureRuinedTemple() {
    val config = MapConfig.ruinedTemple()
    korgeScreenshotTest(Size(VW, VH)) {
        val tiledMap = TmxLoader.parse(resourcesVfs[config.tmxPath].readString())
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }
        val mapView = TiledMapView(tiledMap, atlases)
        mapView.scale = SCALE
        addChild(mapView)
        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman()
        player.gridX = config.spawnX; player.gridY = config.spawnY
        player.play(SpriteAnimation.IDLE)
        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE
        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), config.displayName)
        save("map_ruined_temple")
    }
}

private fun captureBridge() {
    val config = MapConfig.bridge()
    korgeScreenshotTest(Size(VW, VH)) {
        val tiledMap = TmxLoader.parse(resourcesVfs[config.tmxPath].readString())
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }
        val mapView = TiledMapView(tiledMap, atlases)
        mapView.scale = SCALE
        addChild(mapView)
        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman()
        player.gridX = config.spawnX; player.gridY = config.spawnY
        player.play(SpriteAnimation.IDLE)
        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE
        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), config.displayName)
        save("map_bridge")
    }
}

// =============================================================================
// STEP 11: DOODLE RENDER SCAFFOLD (1440p)
// =============================================================================

/**
 * Doodle 1440p: Cartoon-line characters in front of hi-res painted background.
 * Uses tavern_interior.png (gameplay scene) as background, characters get DoodleLineFilter.
 * Grid-derived sizing: gridRows=78, screenTile=1440/78, charScale from screenTile.
 */
private fun captureDoodle1440p() {
    korgeScreenshotTest(Size(2560.0, 1440.0)) {
        // 1. Hi-res painted background (full-screen, NO shader)
        val bgBitmap = resourcesVfs["assets/HD/backgrounds/tavern_interior.png"].readBitmap()
        val bg = image(bgBitmap)
        bg.smoothing = true
        bg.scaledWidth = 2560.0
        bg.scaledHeight = 1440.0

        // 2. Character layer: own container with DoodleLineFilter.
        // PROOF-SCALE close-up: at true gameplay scale on this fine 78-row grid a
        // figure is only ~90px and the fine doodle lines are sub-pixel/invisible
        // (exactly the owner's "tiny on a region map" tension). For this scaffold
        // PROOF we render the figures large so the line + boil effect is legible;
        // in-game the same charLayer would use the grid-derived scale.
        val charLayer = container {}
        addChild(charLayer)

        // At tilePx=64 × charScale, one grid cell = 64*scale px on screen. Keep grid
        // coords small so the big figures stay on the 2560×1440 frame.
        val tilePx = 64 // sprite-native tile → positions are in sprite pixels
        val hero = CharacterSprite(charLayer, tilePx, tilePx)
        hero.loadSwordsman()
        hero.gridX = 1; hero.gridY = 1
        hero.facing = Facing.RIGHT
        hero.play(SpriteAnimation.IDLE)

        val vampire = CharacterSprite(charLayer, tilePx, tilePx)
        vampire.loadVampire()
        vampire.gridX = 3; vampire.gridY = 1
        vampire.facing = Facing.LEFT
        vampire.play(SpriteAnimation.IDLE)

        // Large demo scale so a 64px sprite is ~480px tall → fine doodle lines read.
        val charScale = 7.5
        charLayer.scaleX = charScale
        charLayer.scaleY = charScale

        // 4. Apply DoodleLineFilter ONLY to character layer (not background!)
        val doodleFilter = game.shader.DoodleLineFilter(
            time = 1.5f,  // fixed for reproducible screenshot
            lineStrength = 0.8f,
            jitter = 0.4f,
        )
        charLayer.filter = doodleFilter

        save("doodle_1440p")
    }
}

/**
 * Grid overlay debug: proves the invisible logic grid matches the painted background.
 * Shows tavern_interior.png with CollisionGrid overlaid (BLOCKED=red, WALKABLE=green, WATER=blue).
 */
private fun captureGridOverlayDebug() {
    korgeScreenshotTest(Size(2560.0, 1440.0)) {
        // 1. Hi-res painted background
        val bgBitmap = resourcesVfs["assets/HD/backgrounds/tavern_interior.png"].readBitmap()
        val bg = image(bgBitmap)
        bg.smoothing = true
        bg.scaledWidth = 2560.0
        bg.scaledHeight = 1440.0

        // 2. Load the TMX and build CollisionGrid
        val tmxContent = resourcesVfs["assets/HD/backgrounds/tavern_interior.tmx"].readString()
        val tiledMap = TmxLoader.parse(tmxContent)
        val grid = rpg.tiled.CollisionGrid.from(tiledMap)

        // 3. Overlay: semi-transparent colored rects per grid cell
        val gridRows = 78
        val gridCols = 78
        val cellW = 2560.0 / gridCols  // ~32.8px
        val cellH = 1440.0 / gridRows  // ~18.5px

        for (cy in 0 until grid.rows) {
            for (cx in 0 until grid.cols) {
                val tileType = grid[cx, cy]
                val color = when (tileType) {
                    rpg.tiled.TileType.BLOCKED -> RGBA(0xff, 0x22, 0x22, 0x55)  // red tint
                    rpg.tiled.TileType.WALKABLE -> RGBA(0x22, 0xff, 0x22, 0x25) // light green
                    rpg.tiled.TileType.WATER -> RGBA(0x22, 0x66, 0xff, 0x55)    // blue
                    rpg.tiled.TileType.TRIGGER -> RGBA(0xff, 0xff, 0x22, 0x55)  // yellow
                    else -> RGBA(0x00, 0x00, 0x00, 0x00)  // transparent
                }
                if (color.a > 0) {
                    val wx = cx + grid.offsetX
                    val wy = cy + grid.offsetY
                    solidRect(cellW, cellH, color).apply {
                        x = (cx.toDouble()) * cellW
                        y = (cy.toDouble()) * cellH
                    }
                }
            }
        }

        save("grid_overlay_debug")
    }
}

/**
 * Step 12: Side-by-side upscale comparison at ~6x magnification.
 * 4 panels: bilinear | nearest | EPX+doodle | EPX only (no outline).
 * Proves the EPX shader is sharper than bilinear, smoother than nearest.
 */
private fun captureDoodleUpscaleCompare() {
    korgeScreenshotTest(Size(1680.0, 620.0)) {
        solidRect(1680.0, 620.0, RGBA(0x1a, 0x1a, 0x2e, 0xff))

        // Load a single sprite frame
        val frames = SpriteLoader.load(
            "assets/HD/characters/swordsman/PNG/Swordsman_lvl1/Without_shadow/Swordsman_lvl1_Idle_without_shadow.png"
        )
        val frame = frames[0]

        val panelW = 400.0
        val panelH = 500.0
        val gap = 20.0
        val startX = (1680.0 - 4 * panelW - 3 * gap) / 2.0
        val startY = 60.0
        val scale = 6.0  // ~6x magnification

        val labels = listOf("bilinear", "nearest", "EPX + doodle", "EPX only")

        for (i in 0 until 4) {
            val px = startX + i * (panelW + gap)

            // Label
            text(labels[i], textSize = 14.0, color = Colors.WHITE).apply {
                x = px + panelW / 2.0 - 40.0
                y = startY - 20.0
            }

            // Panel background
            solidRect(panelW, panelH, RGBA(0x10, 0x10, 0x20, 0xff)).apply {
                x = px; y = startY
            }

            // Sprite image
            val panel = container {
                val img = image(frame)
                img.smoothing = (i == 0)  // bilinear only for panel 0
                img.scaleX = scale
                img.scaleY = scale
            }
            panel.x = px + (panelW - 64.0 * scale) / 2.0
            panel.y = startY + (panelH - 64.0 * scale) / 2.0
            addChild(panel)

            // Apply filter for panels 2 and 3
            when (i) {
                2 -> {
                    // EPX + doodle (full effect)
                    val doodle = game.shader.DoodleLineFilter(
                        time = 1.5f,
                        lineStrength = 0.8f,
                        jitter = 0.3f,
                    )
                    panel.filter = doodle
                }
                3 -> {
                    // EPX only (no outline — lineStrength = 0)
                    val epxOnly = game.shader.DoodleLineFilter(
                        time = 1.5f,
                        lineStrength = 0f,
                        jitter = 0f,
                    )
                    panel.filter = epxOnly
                }
            }
        }

        save("doodle_upscale_compare")
    }
}

/**
 * Step 13: Playable doodle world — painted tavern bg + doodle character on grid.
 * Aspect-preserving fit (square image → height-fit, centered, letterboxed).
 */
private fun captureDoodleWorld() {
    korgeScreenshotTest(Size(2560.0, 1440.0)) {
        val outputW = 2560.0
        val outputH = 1440.0

        // Background: aspect-preserving fit
        val bgBitmap = resourcesVfs["assets/HD/backgrounds/tavern_interior.png"].readBitmap()
        val imgW = bgBitmap.width.toDouble()
        val imgH = bgBitmap.height.toDouble()
        val bgScale = outputH / imgH
        val bgDisplayW = imgW * bgScale
        val bgOffsetX = (outputW - bgDisplayW) / 2.0

        // Letterbox
        if (bgOffsetX > 0) {
            solidRect(bgOffsetX, outputH, Colors.BLACK).apply { x = 0.0 }
            solidRect(bgOffsetX, outputH, Colors.BLACK).apply { x = outputW - bgOffsetX }
        }

        val bg = image(bgBitmap)
        bg.smoothing = true
        bg.scaleX = bgScale
        bg.scaleY = bgScale
        bg.x = bgOffsetX

        // Grid
        val tmxContent = resourcesVfs["assets/HD/backgrounds/tavern_interior.tmx"].readString()
        val tiledMap = rpg.tiled.TmxLoader.parse(tmxContent)
        val grid = rpg.tiled.CollisionGrid.from(tiledMap)
        val gridRows = 78
        val screenTile = outputH / gridRows

        // Character layer with doodle
        val charLayer = container {}
        addChild(charLayer)

        val tilesTall = 5
        // Match DoodleWorldScene: derive scale from the int in-layer tile so the
        // figure's grid position * scale lands exactly on screenTile (no drift).
        val layerTile = (64.0 / tilesTall).toInt().coerceAtLeast(1)
        val charScl = screenTile / layerTile.toDouble()

        // Find walkable spawn
        var spawnX = grid.cols / 2 + grid.offsetX
        var spawnY = grid.rows / 2 + grid.offsetY
        for (radius in 0 until 40) {
            var found = false
            for (dy in -radius..radius) {
                for (dx in -radius..radius) {
                    if (kotlin.math.abs(dx) + kotlin.math.abs(dy) != radius) continue
                    val cx = grid.cols / 2 + dx
                    val cy = grid.rows / 2 + dy
                    if (cx < 0 || cx >= grid.cols || cy < 0 || cy >= grid.rows) continue
                    if (grid[cx, cy] == rpg.tiled.TileType.WALKABLE) {
                        spawnX = cx + grid.offsetX
                        spawnY = cy + grid.offsetY
                        found = true; break
                    }
                }
                if (found) break
            }
            if (found) break
        }

        val player = CharacterSprite(charLayer, layerTile, layerTile)
        player.loadSwordsman()
        player.gridX = spawnX
        player.gridY = spawnY
        player.facing = Facing.DOWN
        player.play(SpriteAnimation.IDLE)

        charLayer.scaleX = charScl
        charLayer.scaleY = charScl
        charLayer.x = bgOffsetX

        val doodleFilter = game.shader.DoodleLineFilter(time = 1.5f, lineStrength = 0.8f, jitter = 0.4f)
        charLayer.filter = doodleFilter

        save("doodle_world_1440p")
    }
}

// =============================================================================
// STEP 14: UNIFIED WORLD RUNTIME CAPTURES
// =============================================================================

/**
 * Unified Tavern: Full runtime — painted bg + doodle player on walkable cell +
 * HUD visible + hotspot debug markers on NPC cells. Tavern is square (78x78),
 * centered/letterboxed by camera clamp logic.
 */
private fun captureUnifiedTavern() {
    val def = ImageWorldDef.tavernInterior()
    korgeScreenshotTest(Size(2560.0, 1440.0)) {
        val outputW = 2560.0
        val outputH = 1440.0

        // 1. Load image + grid
        val bgBitmap = resourcesVfs[def.imagePath].readBitmap()
        val imgW = bgBitmap.width.toFloat()
        val imgH = bgBitmap.height.toFloat()
        val tmxContent = resourcesVfs[def.gridTmxPath].readString()
        val tiledMap = rpg.tiled.TmxLoader.parse(tmxContent)
        val grid = rpg.tiled.CollisionGrid.from(tiledMap)

        val gridRows = grid.rows
        val screenTile = (outputH / gridRows).toFloat()
        val bgScale = (outputH / imgH).toDouble()
        val worldW = imgW * bgScale.toFloat()
        val worldH = outputH.toFloat()

        // 2. World layer
        val worldLayer = container {}
        addChild(worldLayer)

        val bg = worldLayer.image(bgBitmap)
        bg.smoothing = true
        bg.scaleX = bgScale
        bg.scaleY = bgScale

        // 3. Entity layer with doodle filter
        val entityLayer = worldLayer.container {}
        val tilesTall = 5
        val layerTile = (64.0 / tilesTall).toInt().coerceAtLeast(1)
        val charScale = screenTile / layerTile.toFloat()

        // Spawn
        val (spawnX, spawnY) = def.spawn ?: (grid.cols / 2 to grid.rows / 2)
        val player = CharacterSprite(entityLayer, layerTile, layerTile)
        player.loadSwordsman()
        player.gridX = spawnX
        player.gridY = spawnY
        player.facing = Facing.DOWN
        player.play(SpriteAnimation.IDLE)

        entityLayer.scaleX = charScale.toDouble()
        entityLayer.scaleY = charScale.toDouble()

        val doodleFilter = game.shader.DoodleLineFilter(time = 1.5f, lineStrength = 0.8f, jitter = 0.4f)
        entityLayer.filter = doodleFilter

        // 4. Hotspot debug markers
        for (npc in def.npcs) {
            val markerX = npc.cellX * screenTile
            val markerY = npc.cellY * screenTile
            worldLayer.solidRect(
                screenTile.toDouble() * 0.6,
                screenTile.toDouble() * 0.6,
                RGBA(0xff, 0xaa, 0x00, 0x66)
            ).apply {
                x = (markerX + screenTile * 0.2).toDouble()
                y = (markerY + screenTile * 0.2).toDouble()
            }
        }

        // 5. Camera: center on player
        val camera = Camera()
        val playerScreenX = player.visualGridX.toFloat() * screenTile
        val playerScreenY = player.visualGridY.toFloat() * screenTile
        camera.follow(playerScreenX, playerScreenY, outputW.toFloat(), outputH.toFloat(), worldW, worldH)
        worldLayer.x = -camera.x.toDouble()
        worldLayer.y = -camera.y.toDouble()

        // 6. HUD (screen-fixed)
        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), def.displayName)

        save("unified_tavern")
    }
}

/**
 * Unified Wildwood: Wide map (86x48) with camera scrolled to player center.
 * Proves horizontal scrolling works — image extends beyond viewport edges.
 */
private fun captureUnifiedWildwood() {
    val def = ImageWorldDef.sylvanoriaWildwood()
    korgeScreenshotTest(Size(2560.0, 1440.0)) {
        val outputW = 2560.0
        val outputH = 1440.0

        val bgBitmap = resourcesVfs[def.imagePath].readBitmap()
        val imgW = bgBitmap.width.toFloat()
        val imgH = bgBitmap.height.toFloat()
        val tmxContent = resourcesVfs[def.gridTmxPath].readString()
        val tiledMap = rpg.tiled.TmxLoader.parse(tmxContent)
        val grid = rpg.tiled.CollisionGrid.from(tiledMap)

        val gridRows = grid.rows
        val screenTile = (outputH / gridRows).toFloat()
        val bgScale = (outputH / imgH).toDouble()
        val worldW = imgW * bgScale.toFloat()
        val worldH = outputH.toFloat()

        val worldLayer = container {}
        addChild(worldLayer)

        val bg = worldLayer.image(bgBitmap)
        bg.smoothing = true
        bg.scaleX = bgScale
        bg.scaleY = bgScale

        val entityLayer = worldLayer.container {}
        val tilesTall = 5
        val layerTile = (64.0 / tilesTall).toInt().coerceAtLeast(1)
        val charScale = screenTile / layerTile.toFloat()

        val (spawnX, spawnY) = def.spawn ?: (grid.cols / 2 to grid.rows / 2)
        val player = CharacterSprite(entityLayer, layerTile, layerTile)
        player.loadSwordsman()
        player.gridX = spawnX
        player.gridY = spawnY
        player.facing = Facing.RIGHT
        player.play(SpriteAnimation.IDLE)

        entityLayer.scaleX = charScale.toDouble()
        entityLayer.scaleY = charScale.toDouble()

        val doodleFilter = game.shader.DoodleLineFilter(time = 1.5f, lineStrength = 0.8f, jitter = 0.4f)
        entityLayer.filter = doodleFilter

        // Hotspot markers
        for (npc in def.npcs) {
            val markerX = npc.cellX * screenTile
            val markerY = npc.cellY * screenTile
            worldLayer.solidRect(
                screenTile.toDouble() * 0.6,
                screenTile.toDouble() * 0.6,
                RGBA(0xff, 0xaa, 0x00, 0x66)
            ).apply {
                x = (markerX + screenTile * 0.2).toDouble()
                y = (markerY + screenTile * 0.2).toDouble()
            }
        }

        // Camera: follow player (scrolled — worldW > outputW for wide map)
        val camera = Camera()
        val playerScreenX = player.visualGridX.toFloat() * screenTile
        val playerScreenY = player.visualGridY.toFloat() * screenTile
        camera.follow(playerScreenX, playerScreenY, outputW.toFloat(), outputH.toFloat(), worldW, worldH)
        worldLayer.x = -camera.x.toDouble()
        worldLayer.y = -camera.y.toDouble()

        // HUD
        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), def.displayName)

        save("unified_wildwood")
    }
}

/**
 * Unified Dialog: Tavern with active DialogOverlay showing Barkeep's first line.
 * Proves the interaction loop works in the unified runtime.
 */
private fun captureUnifiedDialog() {
    val def = ImageWorldDef.tavernInterior()
    korgeScreenshotTest(Size(2560.0, 1440.0)) {
        val outputW = 2560.0
        val outputH = 1440.0

        val bgBitmap = resourcesVfs[def.imagePath].readBitmap()
        val imgW = bgBitmap.width.toFloat()
        val imgH = bgBitmap.height.toFloat()
        val tmxContent = resourcesVfs[def.gridTmxPath].readString()
        val tiledMap = rpg.tiled.TmxLoader.parse(tmxContent)
        val grid = rpg.tiled.CollisionGrid.from(tiledMap)

        val gridRows = grid.rows
        val screenTile = (outputH / gridRows).toFloat()
        val bgScale = (outputH / imgH).toDouble()
        val worldW = imgW * bgScale.toFloat()
        val worldH = outputH.toFloat()

        val worldLayer = container {}
        addChild(worldLayer)

        val bg = worldLayer.image(bgBitmap)
        bg.smoothing = true
        bg.scaleX = bgScale
        bg.scaleY = bgScale

        val entityLayer = worldLayer.container {}
        val tilesTall = 5
        val layerTile = (64.0 / tilesTall).toInt().coerceAtLeast(1)
        val charScale = screenTile / layerTile.toFloat()

        val (spawnX, spawnY) = def.spawn ?: (grid.cols / 2 to grid.rows / 2)
        val player = CharacterSprite(entityLayer, layerTile, layerTile)
        player.loadSwordsman()
        player.gridX = spawnX
        player.gridY = spawnY
        player.facing = Facing.UP
        player.play(SpriteAnimation.IDLE)

        entityLayer.scaleX = charScale.toDouble()
        entityLayer.scaleY = charScale.toDouble()

        val doodleFilter = game.shader.DoodleLineFilter(time = 1.5f, lineStrength = 0.8f, jitter = 0.4f)
        entityLayer.filter = doodleFilter

        // Camera
        val camera = Camera()
        val playerScreenX = player.visualGridX.toFloat() * screenTile
        val playerScreenY = player.visualGridY.toFloat() * screenTile
        camera.follow(playerScreenX, playerScreenY, outputW.toFloat(), outputH.toFloat(), worldW, worldH)
        worldLayer.x = -camera.x.toDouble()
        worldLayer.y = -camera.y.toDouble()

        // HUD
        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), def.displayName)

        // Dialog: show Barkeep's first line
        DialogOverlay(this, outputW, outputH).show(def.npcs.first().dialog)

        save("unified_dialog")
    }
}

/**
 * Step 15: Unified Systems — Tavern with visible puddles (WaterSystem) proving
 * the SystemRegistry + GridOverlay architecture works in the Unified Runtime.
 */
private fun captureUnifiedSystems() {
    val def = ImageWorldDef.tavernInterior()
    korgeScreenshotTest(Size(2560.0, 1440.0)) {
        val outputW = 2560.0
        val outputH = 1440.0

        val bgBitmap = resourcesVfs[def.imagePath].readBitmap()
        val imgW = bgBitmap.width.toFloat()
        val imgH = bgBitmap.height.toFloat()
        val tmxContent = resourcesVfs[def.gridTmxPath].readString()
        val tiledMap = rpg.tiled.TmxLoader.parse(tmxContent)
        val grid = rpg.tiled.CollisionGrid.from(tiledMap)

        val gridRows = grid.rows
        val screenTile = (outputH / gridRows).toFloat()
        val bgScale = (outputH / imgH).toDouble()
        val worldW = imgW * bgScale.toFloat()
        val worldH = outputH.toFloat()

        val worldLayer = container {}
        addChild(worldLayer)

        val bg = worldLayer.image(bgBitmap)
        bg.smoothing = true
        bg.scaleX = bgScale
        bg.scaleY = bgScale

        // Entity layer with doodle filter
        val entityLayer = worldLayer.container {}
        val tilesTall = 5
        val layerTile = (64.0 / tilesTall).toInt().coerceAtLeast(1)
        val charScale = screenTile / layerTile.toFloat()

        val (spawnX, spawnY) = def.spawn ?: (grid.cols / 2 to grid.rows / 2)
        val player = CharacterSprite(entityLayer, layerTile, layerTile)
        player.loadSwordsman()
        player.gridX = spawnX
        player.gridY = spawnY
        player.facing = Facing.DOWN
        player.play(SpriteAnimation.IDLE)

        entityLayer.scaleX = charScale.toDouble()
        entityLayer.scaleY = charScale.toDouble()

        val doodleFilter = game.shader.DoodleLineFilter(time = 1.5f, lineStrength = 0.8f, jitter = 0.4f)
        entityLayer.filter = doodleFilter

        // WaterSystem: pre-fill puddles around the player spawn to prove overlay works
        val waterGrid = WaterGrid(
            width = grid.cols, height = grid.rows,
            offsetX = grid.offsetX, offsetY = grid.offsetY
        )
        // Fill puddles near spawn (deterministic, no rain needed)
        for (dx in -3..3) {
            for (dy in -3..3) {
                val wx = spawnX - grid.offsetX + dx
                val wy = spawnY - grid.offsetY + dy
                if (wx in 0 until grid.cols && wy in 0 until grid.rows) {
                    waterGrid[wx, wy] = 0.3f + (3 - kotlin.math.abs(dx) - kotlin.math.abs(dy)).coerceAtLeast(0) * 0.15f
                }
            }
        }

        // WaterOverlay in worldLayer (scrolls with camera)
        val overlayTileSize = screenTile.toInt().coerceAtLeast(1)
        val waterOverlay = WaterOverlay(worldLayer, overlayTileSize, overlayTileSize)
        waterOverlay.update(waterGrid)

        // Camera
        val camera = Camera()
        val playerScreenX = player.visualGridX.toFloat() * screenTile
        val playerScreenY = player.visualGridY.toFloat() * screenTile
        camera.follow(playerScreenX, playerScreenY, outputW.toFloat(), outputH.toFloat(), worldW, worldH)
        worldLayer.x = -camera.x.toDouble()
        worldLayer.y = -camera.y.toDouble()

        // HUD
        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), "Heroes' Home (PUDDLES)")

        save("unified_systems")
    }
}

// =============================================================================
// STEP 16: PFEILER 2 — UNIFIED SCENE SPEC (Harness Data-Refactor)
// =============================================================================

/**
 * A declarative specification for a unified runtime screenshot.
 * Each spec produces one 2560x1440 PNG via [renderUnifiedScene].
 */
data class UnifiedSceneSpec(
    val name: String,
    val map: ImageMapId = ImageMapId.TAVERN_INTERIOR,
    val season: Season? = null,
    val weather: Weather? = null,
    val prefill: (SystemRegistry) -> Unit = {},
    val playerCell: Pair<Int, Int>? = null,
)

/**
 * Renders a single unified scene from a [UnifiedSceneSpec].
 * Builds the full DoodleWorldScene stack (image + grid + doodle character +
 * SystemRegistry with all Pfeiler 2 systems + overlays), applies prefill,
 * and saves to PNG. Deterministic (fixed values, no Random).
 */
private fun renderUnifiedScene(spec: UnifiedSceneSpec) {
    val def = ImageWorldDef.forId(spec.map)
    korgeScreenshotTest(Size(2560.0, 1440.0)) {
        val outputW = 2560.0
        val outputH = 1440.0

        // 1. Load image + grid
        val bgBitmap = resourcesVfs[def.imagePath].readBitmap()
        val imgW = bgBitmap.width.toFloat()
        val imgH = bgBitmap.height.toFloat()
        val tmxContent = resourcesVfs[def.gridTmxPath].readString()
        val tiledMap = rpg.tiled.TmxLoader.parse(tmxContent)
        val grid = rpg.tiled.CollisionGrid.from(tiledMap)

        val gridRows = grid.rows
        val screenTile = (outputH / gridRows).toFloat()
        val bgScale = (outputH / imgH).toDouble()
        val worldW = imgW * bgScale.toFloat()
        val worldH = outputH.toFloat()

        // 2. World layer
        val worldLayer = container {}
        addChild(worldLayer)

        val bg = worldLayer.image(bgBitmap)
        bg.smoothing = true
        bg.scaleX = bgScale
        bg.scaleY = bgScale

        // 3. Entity layer with doodle filter
        val entityLayer = worldLayer.container {}
        val tilesTall = 5
        val layerTile = (64.0 / tilesTall).toInt().coerceAtLeast(1)
        val charScale = screenTile / layerTile.toFloat()

        val spawn = spec.playerCell ?: def.spawn ?: (grid.cols / 2 to grid.rows / 2)
        val player = CharacterSprite(entityLayer, layerTile, layerTile)
        player.loadSwordsman()
        player.gridX = spawn.first
        player.gridY = spawn.second
        player.facing = Facing.DOWN
        player.play(SpriteAnimation.IDLE)

        entityLayer.scaleX = charScale.toDouble()
        entityLayer.scaleY = charScale.toDouble()

        val doodleFilter = game.shader.DoodleLineFilter(time = 1.5f, lineStrength = 0.8f, jitter = 0.4f)
        entityLayer.filter = doodleFilter

        // 4. Create all grids and systems
        val overlayTileSize = screenTile.toInt().coerceAtLeast(1)
        val currentSeason = spec.season ?: def.season ?: Season.SUMMER
        val isSnowing = spec.weather == Weather.SNOW || currentSeason == Season.WINTER

        val waterGrid = WaterGrid(
            width = grid.cols, height = grid.rows,
            offsetX = grid.offsetX, offsetY = grid.offsetY
        )
        val bloodGrid = BloodGrid(
            width = grid.cols, height = grid.rows,
            offsetX = grid.offsetX, offsetY = grid.offsetY
        )
        val snowGrid = SnowGrid(
            width = grid.cols, height = grid.rows,
            offsetX = grid.offsetX, offsetY = grid.offsetY
        )
        val footprintGrid = FootprintGrid(
            width = grid.cols, height = grid.rows,
            offsetX = grid.offsetX, offsetY = grid.offsetY
        )
        val seasonalGrid = SeasonalGrid(
            width = grid.cols, height = grid.rows,
            offsetX = grid.offsetX, offsetY = grid.offsetY
        )
        val fatigueGrid = MaterialFatigue(
            width = grid.cols, height = grid.rows,
            offsetX = grid.offsetX, offsetY = grid.offsetY
        )

        if (currentSeason == Season.SPRING) seasonalGrid.initFlowers()

        val waterSystem = WaterSystem(waterGrid, isRaining = spec.weather == Weather.RAIN)
        val bloodSystem = BloodSystem(bloodGrid)
        val snowSystem = SnowSystem(snowGrid, isSnowing = isSnowing)
        val footprintSystem = FootprintSystem(footprintGrid, isRaining = spec.weather == Weather.RAIN)
        val seasonSystem = SeasonSystem(seasonalGrid, season = currentSeason)
        val fatigueSystem = MaterialFatigueSystem(fatigueGrid)

        // 5. Overlays in render order: Water > Blood > Snow > Footprints > Season > MaterialFatigue
        val waterOverlay = WaterOverlay(worldLayer, overlayTileSize, overlayTileSize)
        val bloodOverlay = BloodOverlay(worldLayer, overlayTileSize, overlayTileSize)
        val snowOverlay = SnowOverlay(worldLayer, overlayTileSize, overlayTileSize)
        val footprintOverlay = FootprintOverlay(worldLayer, overlayTileSize, overlayTileSize)
        val springOverlay = SpringOverlay(worldLayer, overlayTileSize, overlayTileSize)
        val fatigueOverlay = MaterialFatigueOverlay(worldLayer, overlayTileSize, overlayTileSize)

        // Floor-mask: ground effects only annotate WALKABLE cells (never overpaint the art).
        val floorMask: (Int, Int) -> Boolean = { wx, wy ->
            val c = grid[wx - grid.offsetX, wy - grid.offsetY]
            c == rpg.tiled.TileType.WALKABLE || c == rpg.tiled.TileType.TRIGGER
        }
        waterOverlay.floorMask = floorMask
        bloodOverlay.floorMask = floorMask
        snowOverlay.floorMask = floorMask
        footprintOverlay.floorMask = floorMask
        springOverlay.floorMask = floorMask

        // 6. SystemRegistry
        val registry = SystemRegistry()
        registry.register(waterSystem) { waterOverlay.update(waterGrid) }
        registry.register(bloodSystem) { bloodOverlay.update(bloodGrid, snowGrid) }
        registry.register(snowSystem) { snowOverlay.update(snowGrid, footprintGrid) }
        registry.register(footprintSystem) { footprintOverlay.update(footprintGrid) }
        registry.register(seasonSystem) { springOverlay.update(seasonalGrid) }
        registry.register(fatigueSystem) { fatigueOverlay.update(fatigueGrid) }

        // 7. Apply prefill (populate grids for the screenshot)
        spec.prefill(registry)

        // 8. Render all overlays after prefill
        registry.renderAll()

        // 9. NPC hotspot debug markers (drawn last so they aren't obscured)
        for (npc in def.npcs) {
            val markerX = npc.cellX * screenTile
            val markerY = npc.cellY * screenTile
            worldLayer.solidRect(
                screenTile.toDouble() * 0.6,
                screenTile.toDouble() * 0.6,
                RGBA(0xff, 0xaa, 0x00, 0x66)
            ).apply {
                x = (markerX + screenTile * 0.2).toDouble()
                y = (markerY + screenTile * 0.2).toDouble()
            }
        }

        // 10. Camera: center on player
        val camera = Camera()
        val playerScreenX = player.visualGridX.toFloat() * screenTile
        val playerScreenY = player.visualGridY.toFloat() * screenTile
        camera.follow(playerScreenX, playerScreenY, outputW.toFloat(), outputH.toFloat(), worldW, worldH)
        worldLayer.x = -camera.x.toDouble()
        worldLayer.y = -camera.y.toDouble()

        // 11. HUD (screen-fixed)
        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), def.displayName)

        save(spec.name)
    }
}

// =============================================================================
// STEP 16: UNIFIED SCENE SPECS (6 new screenshots)
// =============================================================================

private val UNIFIED_SPECS = listOf(
    // 1. unified_winter: Snow accumulated + player footprints in snow
    UnifiedSceneSpec(
        name = "unified_winter",
        map = ImageMapId.SYLVANORIA_WILDWOOD,   // weather belongs outdoors, not in the tavern
        season = Season.WINTER,
        weather = Weather.SNOW,
        playerCell = 40 to 24,
        prefill = { registry ->
            val snow = registry.get<SnowSystem>("snow")!!
            // Accumulate snow across the grid (floor-masked → only on walkable ground)
            snow.grid.accumulate(0.7f)
            // Clear a footprint trail approaching the player from the south
            val px = 40; val py = 24
            snow.grid.clearAt(px, py + 3, 0.5f)
            snow.grid.clearAt(px, py + 2, 0.5f)
            snow.grid.clearAt(px, py + 1, 0.5f)
            snow.grid.clearAt(px, py, 0.4f)
            // Stamp footprints on the trail
            val fp = registry.get<FootprintSystem>("footprint")!!
            fp.grid.stamp(px, py + 3)
            fp.grid.stamp(px, py + 2)
            fp.grid.stamp(px, py + 1)
            fp.grid.stamp(px, py)
        },
    ),
    // 2. unified_blood_snow: Fresh blood on snow (the 40-systems overlay thesis)
    UnifiedSceneSpec(
        name = "unified_blood_snow",
        map = ImageMapId.SYLVANORIA_WILDWOOD,
        season = Season.WINTER,
        weather = Weather.SNOW,
        playerCell = 40 to 24,
        prefill = { registry ->
            val snow = registry.get<SnowSystem>("snow")!!
            snow.grid.accumulate(0.75f)
            val blood = registry.get<BloodSystem>("blood")!!
            // Fresh blood at and near player
            blood.spill(40, 24, 0.9f)
            blood.spill(41, 24, 0.7f)
            blood.spill(40, 23, 0.6f)
            blood.spill(39, 24, 0.5f)
            // Older blood further out (age after spilling)
            blood.spill(38, 25, 0.8f)
            blood.spill(42, 23, 0.6f)
            blood.grid.age(0.7f)
            // Re-spill fresh blood near player
            blood.spill(40, 24, 0.9f)
            blood.spill(41, 24, 0.7f)
        },
    ),
    // 3. unified_spring: Spring overlay with flowers
    UnifiedSceneSpec(
        name = "unified_spring",
        map = ImageMapId.SYLVANORIA_WILDWOOD,
        season = Season.SPRING,
        playerCell = 40 to 24,
        prefill = { registry ->
            val season = registry.get<SeasonSystem>("season")!!
            season.grid.initFlowers(0.25f)   // sparse like autumn leaves — 0.8 measled the whole map
            // Trample near player to show interaction
            season.grid.trampleFlower(40, 24, 0.5f)
            season.grid.trampleFlower(41, 24, 0.3f)
            season.grid.trampleFlower(40, 25, 0.4f)
        },
    ),
    // 4. unified_autumn: Autumn overlay with fallen leaves
    UnifiedSceneSpec(
        name = "unified_autumn",
        map = ImageMapId.SYLVANORIA_WILDWOOD,
        season = Season.AUTUMN,
        playerCell = 40 to 24,
        prefill = { registry ->
            val season = registry.get<SeasonSystem>("season")!!
            // Drop multiple rounds of leaves
            season.grid.dropLeaves(0.5f, timeStep = 0)
            season.grid.dropLeaves(0.4f, timeStep = 1)
            season.grid.dropLeaves(0.3f, timeStep = 2)
            // Player kicked leaves at current position
            season.grid.kickLeaves(40, 24)
        },
    ),
    // 5. unified_material_fatigue: Cracks visible
    UnifiedSceneSpec(
        name = "unified_material_fatigue",
        map = ImageMapId.TAVERN_INTERIOR,
        playerCell = 39 to 50,
        prefill = { registry ->
            val fatigue = registry.get<MaterialFatigueSystem>("material_fatigue")!!
            // Various stress levels around player
            fatigue.impact(39, 48, 0.4f)   // cracked
            fatigue.impact(40, 49, 0.35f)  // cracked
            fatigue.impact(38, 50, 0.55f)  // medium crack
            fatigue.impact(39, 51, 0.6f)   // medium crack
            fatigue.impact(40, 50, 0.8f)   // broken
            fatigue.impactRadius(37, 49, 2, 0.75f) // radius impact, broken center
        },
    ),
    // 6. unified_all: All systems active simultaneously (the "living world" shot, outdoors)
    UnifiedSceneSpec(
        name = "unified_all",
        map = ImageMapId.SYLVANORIA_WILDWOOD,
        season = Season.WINTER,
        weather = Weather.SNOW,
        playerCell = 40 to 24,
        prefill = { registry ->
            // Snow
            val snow = registry.get<SnowSystem>("snow")!!
            snow.grid.accumulate(0.6f)
            snow.grid.clearAt(40, 24, 0.4f)
            snow.grid.clearAt(40, 25, 0.3f)
            // Blood on snow
            val blood = registry.get<BloodSystem>("blood")!!
            blood.spill(41, 24, 0.8f)
            blood.spill(42, 25, 0.6f)
            // Footprints
            val fp = registry.get<FootprintSystem>("footprint")!!
            fp.grid.stamp(40, 26)
            fp.grid.stamp(40, 25)
            fp.grid.stamp(40, 24)
            fp.grid.stamp(39, 24)
            // Material fatigue
            val fatigue = registry.get<MaterialFatigueSystem>("material_fatigue")!!
            fatigue.impact(38, 23, 0.5f)
            fatigue.impact(39, 22, 0.75f)
            fatigue.impactRadius(42, 26, 1, 0.6f)
            // Water puddles
            val water = registry.get<WaterSystem>("water")!!
            water.grid[36, 22] = 0.5f
            water.grid[37, 22] = 0.4f
            water.grid[36, 23] = 0.3f
        },
    ),
)

// =============================================================================
// Step 17: Figures via normalized sheets
// =============================================================================

/**
 * figures_tavern: Figurefree tavern + player + NPCs as rendered Doodle figures.
 * Player = Swordsman, NPCs = Vampire (barkeep) + Swordsman lvl2 (patron).
 * All physically normalized (body ~110px screen, foot on ground).
 */
private fun captureFiguresTavern() {
    val def = ImageWorldDef.tavernInterior()
    korgeScreenshotTest(Size(2560.0, 1440.0)) {
        val outputW = 2560.0
        val outputH = 1440.0

        val bgBitmap = resourcesVfs[def.imagePath].readBitmap()
        val imgH = bgBitmap.height.toFloat()
        val tmxContent = resourcesVfs[def.gridTmxPath].readString()
        val tiledMap = rpg.tiled.TmxLoader.parse(tmxContent)
        val grid = rpg.tiled.CollisionGrid.from(tiledMap)

        val gridRows = grid.rows
        val screenTile = (outputH / gridRows).toFloat()
        val bgScale = (outputH / imgH).toDouble()

        val worldLayer = container {}
        addChild(worldLayer)

        val bg = worldLayer.image(bgBitmap)
        bg.smoothing = true
        bg.scaleX = bgScale
        bg.scaleY = bgScale

        // Physical scale from descriptor
        val targetBodyScreenPx = 96f * bgScale.toFloat()
        val idleDesc = SpriteLoader.loadDescriptor(
            "assets/HD/characters/forest_ranger/ForestRanger_Idle.png"
        )
        val playerBodyH = (idleDesc?.opaqueBodyH ?: 551).toFloat()
        val charScale = targetBodyScreenPx / playerBodyH
        val layerTile = (screenTile / charScale).toInt().coerceAtLeast(1)

        // Entity layer with doodle filter
        val entityLayer = worldLayer.container {}

        // Player
        val (spawnX, spawnY) = def.spawn ?: (grid.cols / 2 to grid.rows / 2)
        val player = CharacterSprite(entityLayer, layerTile, layerTile)
        player.loadForestRanger()
        player.gridX = spawnX
        player.gridY = spawnY
        player.facing = Facing.DOWN
        player.play(SpriteAnimation.IDLE)

        // NPCs
        for (npc in def.npcs) {
            if (npc.sheetPath != null) {
                val npcSprite = CharacterSprite(entityLayer, layerTile, layerTile)
                npcSprite.loadFromSheet(npc.sheetPath, npc.walkSheetPath)
                npcSprite.gridX = npc.cellX
                npcSprite.gridY = npc.cellY
                npcSprite.facing = npc.facingHint
                npcSprite.play(SpriteAnimation.IDLE)
            }
        }

        entityLayer.scaleX = charScale.toDouble()
        entityLayer.scaleY = charScale.toDouble()

        val doodleFilter = game.shader.DoodleLineFilter(time = 1.5f, lineStrength = 0.8f, jitter = 0.4f)
        entityLayer.filter = doodleFilter

        // Camera
        val camera = rpg.world.Camera()
        val playerScreenX = player.visualGridX.toFloat() * screenTile
        val playerScreenY = player.visualGridY.toFloat() * screenTile
        camera.follow(playerScreenX, playerScreenY, outputW.toFloat(), outputH.toFloat(),
            bgBitmap.width * bgScale.toFloat(), outputH.toFloat())
        worldLayer.x = -camera.x.toDouble()
        worldLayer.y = -camera.y.toDouble()

        // HUD
        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), "Heroes' Home (FIGURES)")

        save("figures_tavern")
    }
}

/**
 * figures_marker_check: Same player sprite rendered on the OLD baked-in tavern
 * (with painted figures) for size/style comparison against the quality markers.
 */
private fun captureFiguresMarkerCheck() {
    korgeScreenshotTest(Size(2560.0, 1440.0)) {
        val outputH = 1440.0

        // Load the OLD baked-in tavern (with painted figures as reference markers)
        val bgBitmap = resourcesVfs["assets/HD/backgrounds/tavern_interior.png"].readBitmap()
        val imgH = bgBitmap.height.toFloat()
        val tmxContent = resourcesVfs["assets/HD/backgrounds/tavern_interior.tmx"].readString()
        val tiledMap = rpg.tiled.TmxLoader.parse(tmxContent)
        val grid = rpg.tiled.CollisionGrid.from(tiledMap)

        val gridRows = grid.rows
        val screenTile = (outputH / gridRows).toFloat()
        val bgScale = (outputH / imgH).toDouble()

        val worldLayer = container {}
        addChild(worldLayer)

        val bg = worldLayer.image(bgBitmap)
        bg.smoothing = true
        bg.scaleX = bgScale
        bg.scaleY = bgScale

        // Physical scale from descriptor (same as figures_tavern)
        val targetBodyScreenPx = 96f * bgScale.toFloat()
        val idleDesc = SpriteLoader.loadDescriptor(
            "assets/HD/characters/swordsman/PNG/Swordsman_lvl1/Without_shadow/Swordsman_lvl1_Idle_without_shadow.png"
        )
        val playerBodyH = (idleDesc?.opaqueBodyH ?: 24).toFloat()
        val charScale = targetBodyScreenPx / playerBodyH
        val layerTile = (screenTile / charScale).toInt().coerceAtLeast(1)

        // Entity layer with doodle filter
        val entityLayer = worldLayer.container {}

        // Player at spawn position (next to painted figures for comparison)
        val player = CharacterSprite(entityLayer, layerTile, layerTile)
        player.loadSwordsman()
        player.gridX = 39
        player.gridY = 50
        player.facing = Facing.DOWN
        player.play(SpriteAnimation.IDLE)

        entityLayer.scaleX = charScale.toDouble()
        entityLayer.scaleY = charScale.toDouble()

        val doodleFilter = game.shader.DoodleLineFilter(time = 1.5f, lineStrength = 0.8f, jitter = 0.4f)
        entityLayer.filter = doodleFilter

        // Camera
        val camera = rpg.world.Camera()
        val playerScreenX = player.visualGridX.toFloat() * screenTile
        val playerScreenY = player.visualGridY.toFloat() * screenTile
        camera.follow(playerScreenX, playerScreenY, 2560f, outputH.toFloat(),
            bgBitmap.width * bgScale.toFloat(), outputH.toFloat())
        worldLayer.x = -camera.x.toDouble()
        worldLayer.y = -camera.y.toDouble()

        // HUD
        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), "MARKER CHECK (baked-in bg)")

        save("figures_marker_check")
    }
}

// =============================================================================
// RT Material Rendering — physical materials via fragment shader
// =============================================================================

/**
 * Proof of concept: WorldMaterialFilter transforms flat overlay colors into
 * physically rendered materials (reflective water, sparkling snow, glossy blood,
 * shadowed cracks). Same scene as unified_all but with material shader active.
 */
private fun captureRTMaterial() {
    val def = ImageWorldDef.tavernInterior()
    korgeScreenshotTest(Size(2560.0, 1440.0)) {
        val outputW = 2560.0
        val outputH = 1440.0

        val bgBitmap = resourcesVfs[def.imagePath].readBitmap()
        val imgH = bgBitmap.height.toFloat()
        val tmxContent = resourcesVfs[def.gridTmxPath].readString()
        val tiledMap = rpg.tiled.TmxLoader.parse(tmxContent)
        val grid = rpg.tiled.CollisionGrid.from(tiledMap)

        val gridRows = grid.rows
        val screenTile = (outputH / gridRows).toFloat()
        val bgScale = (outputH / imgH).toDouble()

        val worldLayer = container {}
        addChild(worldLayer)

        val bg = worldLayer.image(bgBitmap)
        bg.smoothing = true
        bg.scaleX = bgScale
        bg.scaleY = bgScale

        // Physical scale
        val targetBodyScreenPx = 96f * bgScale.toFloat()
        val idleDesc = SpriteLoader.loadDescriptor(
            "assets/HD/characters/forest_ranger/ForestRanger_Idle.png"
        )
        val playerBodyH = (idleDesc?.opaqueBodyH ?: 551).toFloat()
        val charScale = targetBodyScreenPx / playerBodyH
        val layerTile = (screenTile / charScale).toInt().coerceAtLeast(1)

        // Entity layer
        val entityLayer = worldLayer.container {}
        val (spawnX, spawnY) = def.spawn ?: (grid.cols / 2 to grid.rows / 2)
        val player = CharacterSprite(entityLayer, layerTile, layerTile)
        player.loadForestRanger()
        player.gridX = spawnX
        player.gridY = spawnY
        player.facing = Facing.DOWN
        player.play(SpriteAnimation.IDLE)
        entityLayer.scaleX = charScale.toDouble()
        entityLayer.scaleY = charScale.toDouble()

        // Overlays — multiple systems active (like unified_all)
        val overlayTileSize = screenTile.toInt().coerceAtLeast(1)

        // Water puddles around spawn
        val waterGrid = rpg.weather.WaterGrid(
            width = grid.cols, height = grid.rows,
            offsetX = grid.offsetX, offsetY = grid.offsetY
        )
        for (dx in -4..4) {
            for (dy in -4..4) {
                val wx = spawnX - grid.offsetX + dx
                val wy = spawnY - grid.offsetY + dy
                if (wx in 0 until grid.cols && wy in 0 until grid.rows) {
                    waterGrid[wx, wy] = 0.4f + (4 - kotlin.math.abs(dx) - kotlin.math.abs(dy)).coerceAtLeast(0) * 0.12f
                }
            }
        }
        val waterOverlay = WaterOverlay(worldLayer, overlayTileSize, overlayTileSize)
        waterOverlay.update(waterGrid)

        // Snow patches (offset from water)
        val snowGrid = rpg.weather.SnowGrid(
            width = grid.cols, height = grid.rows,
            offsetX = grid.offsetX, offsetY = grid.offsetY
        )
        for (dx in 3..7) {
            for (dy in -3..1) {
                val wx = spawnX - grid.offsetX + dx
                val wy = spawnY - grid.offsetY + dy
                if (wx in 0 until grid.cols && wy in 0 until grid.rows) {
                    snowGrid[wx + grid.offsetX, wy + grid.offsetY] = 0.6f
                }
            }
        }
        val footprintGrid = rpg.weather.FootprintGrid(
            width = grid.cols, height = grid.rows,
            offsetX = grid.offsetX, offsetY = grid.offsetY
        )
        val snowOverlay = SnowOverlay(worldLayer, overlayTileSize, overlayTileSize)
        snowOverlay.update(snowGrid, footprintGrid)

        // Blood spots
        val bloodGrid = rpg.weather.BloodGrid(
            width = grid.cols, height = grid.rows,
            offsetX = grid.offsetX, offsetY = grid.offsetY
        )
        bloodGrid.spill(spawnX - 2, spawnY + 2, 0.8f)
        bloodGrid.spill(spawnX - 1, spawnY + 2, 0.5f)
        bloodGrid.spill(spawnX - 2, spawnY + 3, 0.3f)
        val bloodOverlay = BloodOverlay(worldLayer, overlayTileSize, overlayTileSize)
        bloodOverlay.update(bloodGrid, snowGrid)

        // Material fatigue (cracks)
        val fatigue = rpg.weather.MaterialFatigue(
            width = grid.cols, height = grid.rows,
            offsetX = grid.offsetX, offsetY = grid.offsetY
        )
        fatigue.addStress(spawnX + 3, spawnY + 3, 0.8f)
        fatigue.addStressRadius(spawnX + 4, spawnY + 4, 1, 0.6f)
        val fatigueOverlay = MaterialFatigueOverlay(worldLayer, overlayTileSize, overlayTileSize)
        fatigueOverlay.update(fatigue)

        // === APPLY WORLD MATERIAL FILTER ===
        // This transforms the flat overlay colors into physical materials
        val materialFilter = game.shader.WorldMaterialFilter(
            time = 2.5f,
            intensity = 1.0f,
            lightAngle = 0.7f,
            lightHeight = 0.6f,
        )
        worldLayer.filter = materialFilter

        // Camera
        val camera = rpg.world.Camera()
        val playerScreenX = player.visualGridX.toFloat() * screenTile
        val playerScreenY = player.visualGridY.toFloat() * screenTile
        camera.follow(playerScreenX, playerScreenY, outputW.toFloat(), outputH.toFloat(),
            bgBitmap.width * bgScale.toFloat(), outputH.toFloat())
        worldLayer.x = -camera.x.toDouble()
        worldLayer.y = -camera.y.toDouble()

        // HUD
        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), "RT MATERIAL RENDERING")

        save("rt_material")
    }
}
// Shader Palette Expansion — Caustic, WetSurface, Decay
// =============================================================================

private fun captureShaderCaustic() {
    korgeScreenshotTest(Size(2560.0, 1440.0)) {
        // Use a landscape image as base
        val bgBitmap = resourcesVfs["assets/HD/backgrounds/landscapes/28-19_1.png"].readBitmap()
        val bg = image(bgBitmap)
        bg.smoothing = true

        // Apply caustic filter (underwater light dance)
        val caustic = game.shader.CausticFilter(time = 3.0f, intensity = 0.4f, scale = 50f, speed = 1.5f)
        bg.filter = caustic

        save("shader_caustic")
    }
}

private fun captureShaderWetSurface() {
    korgeScreenshotTest(Size(2560.0, 1440.0)) {
        val bgBitmap = resourcesVfs["assets/HD/backgrounds/landscapes/28-21_2.png"].readBitmap()
        val bg = image(bgBitmap)
        bg.smoothing = true

        // Apply wet surface (heavy rain)
        val wet = game.shader.WetSurfaceFilter(time = 2.0f, wetness = 0.8f, specularStrength = 0.5f)
        bg.filter = wet

        save("shader_wet_surface")
    }
}

private fun captureShaderDecay() {
    korgeScreenshotTest(Size(2560.0, 1440.0)) {
        val bgBitmap = resourcesVfs["assets/HD/backgrounds/landscapes/28-22_3.png"].readBitmap()
        val bg = image(bgBitmap)
        bg.smoothing = true

        // Apply decay: moss (type=1) at 60% coverage
        val decay = game.shader.DecayFilter(time = 0f, decayLevel = 0.6f, decayType = 1)
        bg.filter = decay

        save("shader_decay_moss")
    }
}

// =============================================================================
// Weather Composition — same image, 3 states (sun / rain / storm)
// Proves: "Zeit wirkt auf Materie" via shader composition alone.
// =============================================================================

private fun captureWeatherTriptych() {
    val bgPath = "assets/HD/backgrounds/landscapes/28-19_1.png"

    // State 1: Sunny (no weather effects)
    korgeScreenshotTest(Size(2560.0, 1440.0)) {
        val bgBitmap = resourcesVfs[bgPath].readBitmap()
        val bg = image(bgBitmap)
        bg.smoothing = true
        // No filters — pristine sunny day
        save("weather_sun")
    }

    // State 2: Rain (moderate wetness + fog + slight darkening + caustics on puddles)
    korgeScreenshotTest(Size(2560.0, 1440.0)) {
        val bgBitmap = resourcesVfs[bgPath].readBitmap()
        val worldLayer = container {}
        addChild(worldLayer)
        val bg = worldLayer.image(bgBitmap)
        bg.smoothing = true

        val effects = game.shader.ShaderEffects()
        // Wet surface: 60% soaked
        effects.wetSurfaceFilter.wetness = 0.6f
        effects.wetSurfaceFilter.time = 2.0f
        effects.enable(worldLayer, effects.wetSurfaceFilter)
        // Fog: light mist
        effects.fogFilter.density = 0.2f
        effects.fogFilter.time = 1.5f
        effects.enable(worldLayer, effects.fogFilter)
        // Rain particles
        effects.rainFilter.intensity = 0.5f
        effects.rainFilter.time = 3.0f
        effects.enable(worldLayer, effects.rainFilter)
        // Slight darkening via lighting (overcast sky)
        effects.lightingFilter.ambientDarkness = 0.6f
        effects.lightingFilter.time = 2.0f
        effects.enable(worldLayer, effects.lightingFilter)
        // Caustics on wet ground
        effects.causticFilter.intensity = 0.15f
        effects.causticFilter.time = 2.5f
        effects.causticFilter.scale = 60f
        effects.enable(worldLayer, effects.causticFilter)

        save("weather_rain")
    }

    // State 3: Storm (heavy wetness + dense fog + strong darkening + caustics + rain)
    korgeScreenshotTest(Size(2560.0, 1440.0)) {
        val bgBitmap = resourcesVfs[bgPath].readBitmap()
        val worldLayer = container {}
        addChild(worldLayer)
        val bg = worldLayer.image(bgBitmap)
        bg.smoothing = true

        val effects = game.shader.ShaderEffects()
        // Heavy wetness
        effects.wetSurfaceFilter.wetness = 0.95f
        effects.wetSurfaceFilter.time = 4.0f
        effects.enable(worldLayer, effects.wetSurfaceFilter)
        // Dense fog
        effects.fogFilter.density = 0.45f
        effects.fogFilter.time = 3.0f
        effects.enable(worldLayer, effects.fogFilter)
        // Heavy rain
        effects.rainFilter.intensity = 0.9f
        effects.rainFilter.time = 5.0f
        effects.enable(worldLayer, effects.rainFilter)
        // Strong darkening (storm clouds)
        effects.lightingFilter.ambientDarkness = 0.35f
        effects.lightingFilter.time = 4.0f
        effects.enable(worldLayer, effects.lightingFilter)
        // Strong caustics (deep puddles reflecting)
        effects.causticFilter.intensity = 0.3f
        effects.causticFilter.time = 4.5f
        effects.causticFilter.scale = 45f
        effects.enable(worldLayer, effects.causticFilter)

        save("weather_storm")
    }
}

private fun captureMaterialWeather() {
    val bgPath = "assets/HD/backgrounds/landscapes/28-19_1.png"

    // Sun
    korgeScreenshotTest(Size(2560.0, 1440.0)) {
        val bgBitmap = resourcesVfs[bgPath].readBitmap()
        val bg = image(bgBitmap)
        bg.smoothing = true
        bg.filter = game.shader.MaterialWeatherFilter(time = 2.0f, weatherState = 0.0f)
        save("material_weather_sun")
    }

    // Rain
    korgeScreenshotTest(Size(2560.0, 1440.0)) {
        val bgBitmap = resourcesVfs[bgPath].readBitmap()
        val bg = image(bgBitmap)
        bg.smoothing = true
        bg.filter = game.shader.MaterialWeatherFilter(time = 3.5f, weatherState = 0.55f, windAngle = 0.3f)
        save("material_weather_rain")
    }

    // Storm
    korgeScreenshotTest(Size(2560.0, 1440.0)) {
        val bgBitmap = resourcesVfs[bgPath].readBitmap()
        val bg = image(bgBitmap)
        bg.smoothing = true
        bg.filter = game.shader.MaterialWeatherFilter(time = 5.0f, weatherState = 1.0f, windAngle = 0.5f)
        save("material_weather_storm")
    }
}

/**
 * Village Material-Weather Pipeline proof:
 * Uses the GAIME village image (ResizedImage_2026-06-30_10-29-19_2317[41].png)
 * with the calibrated MaterialWeatherFilter to demonstrate per-material weather
 * effects: stone→puddles, grass→satter, wood→dunkler, roof→Abperlen.
 *
 * Now uses MULTI-TEXTURE: the full-resolution material map
 * (assets/materials/village_material_fullres.png) is bound as a second texture.
 * The shader reads material IDs from the R channel — no HSV heuristics needed.
 */
private fun captureVillageMaterialWeather() {
    val villagePath = "ResizedImage_2026-06-30_10-29-19_2317[41].png"
    val materialMapPath = "assets/materials/village_material_fullres.png"

    // Sun (dry, clear day)
    korgeScreenshotTest(Size(2560.0, 1440.0)) {
        val bgBitmap = resourcesVfs[villagePath].readBitmap()
        val matBitmap = resourcesVfs[materialMapPath].readBitmap()
        val bg = image(bgBitmap)
        bg.smoothing = true
        val filter = game.shader.MaterialWeatherFilter(time = 1.0f, weatherState = 0.0f)
        filter.setMaterialMap(matBitmap.toBMP32())
        bg.filter = filter
        save("village_weather_v8_sun")
    }

    // Rain (moderate, wind from left)
    korgeScreenshotTest(Size(2560.0, 1440.0)) {
        val bgBitmap = resourcesVfs[villagePath].readBitmap()
        val matBitmap = resourcesVfs[materialMapPath].readBitmap()
        val bg = image(bgBitmap)
        bg.smoothing = true
        val filter = game.shader.MaterialWeatherFilter(time = 4.0f, weatherState = 0.6f, windAngle = 0.3f)
        filter.setMaterialMap(matBitmap.toBMP32())
        bg.filter = filter
        save("village_weather_v8_rain")
    }

    // Storm (heavy rain, strong wind, maximum effects)
    korgeScreenshotTest(Size(2560.0, 1440.0)) {
        val bgBitmap = resourcesVfs[villagePath].readBitmap()
        val matBitmap = resourcesVfs[materialMapPath].readBitmap()
        val bg = image(bgBitmap)
        bg.smoothing = true
        val filter = game.shader.MaterialWeatherFilter(time = 7.0f, weatherState = 1.0f, windAngle = 0.5f)
        filter.setMaterialMap(matBitmap.toBMP32())
        bg.filter = filter
        save("village_weather_v8_storm")
    }
}

// Guild house material weather test
private fun captureGuildMaterialWeather() {
    val guildPath = "ResizedImage_2026-06-30_10-36-27_1077[2].png"
    val guildMatPath = "assets/materials/guild_material_fullres.png"

    korgeScreenshotTest(Size(2560.0, 1440.0)) {
        val bgBitmap = resourcesVfs[guildPath].readBitmap()
        val matBitmap = resourcesVfs[guildMatPath].readBitmap()
        val bg = image(bgBitmap)
        bg.smoothing = true
        val filter = game.shader.MaterialWeatherFilter(time = 1.0f, weatherState = 0.0f)
        filter.setMaterialMap(matBitmap.toBMP32())
        bg.filter = filter
        save("guild_weather_v7_sun")
    }

    korgeScreenshotTest(Size(2560.0, 1440.0)) {
        val bgBitmap = resourcesVfs[guildPath].readBitmap()
        val matBitmap = resourcesVfs[guildMatPath].readBitmap()
        val bg = image(bgBitmap)
        bg.smoothing = true
        val filter = game.shader.MaterialWeatherFilter(time = 4.0f, weatherState = 0.6f, windAngle = 0.3f)
        filter.setMaterialMap(matBitmap.toBMP32())
        bg.filter = filter
        save("guild_weather_v7_rain")
    }

    korgeScreenshotTest(Size(2560.0, 1440.0)) {
        val bgBitmap = resourcesVfs[guildPath].readBitmap()
        val matBitmap = resourcesVfs[guildMatPath].readBitmap()
        val bg = image(bgBitmap)
        bg.smoothing = true
        val filter = game.shader.MaterialWeatherFilter(time = 7.0f, weatherState = 1.0f, windAngle = 0.5f)
        filter.setMaterialMap(matBitmap.toBMP32())
        bg.filter = filter
        save("guild_weather_v7_storm")
    }
}
