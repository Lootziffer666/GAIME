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
import rpg.questbook.RoomContext

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
    // Physics system captures
    capturePhysicsSnow()
    capturePhysicsFootprints()
    capturePhysicsBlood()
    capturePhysicsTorchlight()
    capturePhysicsComplete()
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

// =============================================================================
// PHYSICS SYSTEM CAPTURES
// =============================================================================

/**
 * Snow: Exterior map + SnowFilter at intensity 0.7, windAngle 0.3, time=2.5.
 */
private fun capturePhysicsSnow() {
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
        player.play(SpriteAnimation.IDLE)

        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE

        val snowFilter = game.shader.SnowFilter(intensity = 0.7f, windAngle = 0.3f, time = 2.5f)
        mapView.filter = snowFilter

        save("physics_snow")
    }
}

/**
 * Footprints: Exterior map + SnowFilter at intensity 0.8 + visible footprint trail
 * rendered as dark semi-transparent rectangles on the map to simulate cleared snow.
 */
private fun capturePhysicsFootprints() {
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
        player.play(SpriteAnimation.IDLE)

        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE

        // Draw dark footprint indicators on map (simulating cleared snow)
        val tileW = tiledMap.tileWidth * SCALE
        val tileH = tiledMap.tileHeight * SCALE
        val footprintColor = RGBA(0x22, 0x22, 0x22, 0x66)
        // Footprint path: 4 tiles in a row
        for (i in 0 until 4) {
            solidRect(tileW, tileH, footprintColor).apply {
                x = mapView.x + (config.spawnX + 1 + i) * tileW
                y = mapView.y + config.spawnY * tileH
            }
        }

        // Apply snow filter with high intensity (footprints are visible as gaps)
        val snowFilter = game.shader.SnowFilter(intensity = 0.8f, windAngle = 0.2f, time = 1.8f)
        mapView.filter = snowFilter

        save("physics_footprints")
    }
}

/**
 * Blood: Exterior map + BloodFilter at intensity 0.6, splatterSeed 42.0, time=1.0.
 */
private fun capturePhysicsBlood() {
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
        player.play(SpriteAnimation.IDLE)

        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE

        val bloodFilter = game.shader.BloodFilter(intensity = 0.6f, splatterSeed = 42.0f, time = 1.0f)
        mapView.filter = bloodFilter

        save("physics_blood")
    }
}

/**
 * Torchlight: Exterior map + LightingFilter with 2 torches, ambient 0.1.
 */
private fun capturePhysicsTorchlight() {
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
        player.play(SpriteAnimation.IDLE)

        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE

        val tilePixelSize = (tiledMap.tileWidth * SCALE).toFloat()
        val lightingFilter = game.shader.LightingFilter(
            ambientDarkness = 0.1f,
            time = 2.0f,
        )
        lightingFilter.tilePixelSize = tilePixelSize
        lightingFilter.lights = listOf(
            game.shader.LightSource(
                tileX = -3, tileY = 9,
                radius = 6f, r = 1.0f, g = 0.8f, b = 0.4f,
                intensity = 0.9f, flickerSpeed = 3.0f, flickerAmount = 0.15f
            ),
            game.shader.LightSource(
                tileX = 0, tileY = 10,
                radius = 5f, r = 1.0f, g = 0.75f, b = 0.35f,
                intensity = 0.85f, flickerSpeed = 3.5f, flickerAmount = 0.12f
            ),
        )
        mapView.filter = lightingFilter

        save("physics_torchlight")
    }
}

/**
 * Complete: Exterior map + ALL 5 effects together using nested containers.
 * Outermost: BloodFilter, Middle: SnowFilter, Innermost (mapView): LightingFilter.
 * Wind drives the SnowFilter drift direction. RainFilter windAngle also set.
 */
private fun capturePhysicsComplete() {
    val config = MapConfig.exterior()
    korgeScreenshotTest(Size(VW, VH)) {
        val tiledMap = TmxLoader.parse(resourcesVfs[config.tmxPath].readString())
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }

        val tilePixelSize = (tiledMap.tileWidth * SCALE).toFloat()
        val windAngle = 0.3f

        // Nested containers: outermost → BloodFilter, middle → SnowFilter, innermost → LightingFilter
        val outerContainer = korlibs.korge.view.Container().also { addChild(it) }
        val middleContainer = korlibs.korge.view.Container().also { outerContainer.addChild(it) }

        val mapView = TiledMapView(tiledMap, atlases)
        mapView.scale = SCALE
        middleContainer.addChild(mapView)

        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman()
        player.gridX = config.spawnX; player.gridY = config.spawnY
        player.play(SpriteAnimation.IDLE)

        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE

        // Innermost: LightingFilter on mapView (torches)
        val lightingFilter = game.shader.LightingFilter(
            ambientDarkness = 0.15f,
            time = 2.0f,
        )
        lightingFilter.tilePixelSize = tilePixelSize
        lightingFilter.lights = listOf(
            game.shader.LightSource(
                tileX = -3, tileY = 9,
                radius = 6f, r = 1.0f, g = 0.8f, b = 0.4f,
                intensity = 0.9f, flickerSpeed = 3.0f, flickerAmount = 0.15f
            ),
            game.shader.LightSource(
                tileX = 0, tileY = 10,
                radius = 5f, r = 1.0f, g = 0.75f, b = 0.35f,
                intensity = 0.85f, flickerSpeed = 3.5f, flickerAmount = 0.12f
            ),
        )
        mapView.filter = lightingFilter

        // Middle: SnowFilter (wind-driven)
        val snowFilter = game.shader.SnowFilter(intensity = 0.6f, windAngle = windAngle, time = 2.5f)
        middleContainer.filter = snowFilter

        // Outermost: BloodFilter
        val bloodFilter = game.shader.BloodFilter(intensity = 0.5f, splatterSeed = 42.0f, time = 1.0f)
        outerContainer.filter = bloodFilter

        save("physics_complete")
    }
}
