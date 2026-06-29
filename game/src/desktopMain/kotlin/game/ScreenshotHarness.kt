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
