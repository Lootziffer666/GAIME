package game

import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.image.format.PNG
import korlibs.image.format.writeTo
import korlibs.io.file.std.localVfs
import korlibs.io.file.std.resourcesVfs
import korlibs.korge.testing.OffscreenStage
import korlibs.korge.testing.korgeScreenshotTest
import korlibs.korge.view.renderToBitmap
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import korlibs.math.geom.Size
import rpg.combat.CombatAction
import rpg.combat.CombatEngine
import rpg.combat.CombatResult
import rpg.combat.Combatant
import rpg.combat.Side
import rpg.items.Inventory
import rpg.tiled.CollisionGrid
import rpg.tiled.TileType
import rpg.tiled.TmxLoader

/**
 * Offscreen GL screenshot harness — renders scripted playthrough scenarios to PNGs
 * under build/screenshots/ using KorGE's headless offscreen renderer. These serve
 * as visual regression proof and as input for CUE-AGENT (QA + Video).
 *
 * Run via:  ./gradlew :game:screenshot
 *
 * Scripted playthrough (simulates a real game session):
 * 1. Interior spawn + walk south + dialog with Barkeep → "dialog"
 * 2. Walk to exit tile → exterior loaded → "exterior_entry"
 * 3. Exterior: walk to Guard NPC → dialog → "exterior_dialog"
 * 4. Battle: 2× attack → "battle_midway"
 * 5. Battle: fight to conclusion → "battle_victory"
 *
 * Also retains the original static captures (interior, exterior, battle) for
 * backward compatibility.
 */
private const val VW = 640.0
private const val VH = 360.0
private const val SCALE = 3.0
private val OUT = localVfs("build/screenshots")

fun main() {
    // --- Original static captures (backward compat) ---
    captureWorld(MapConfig.interior(), "interior", withDialog = true)
    captureWorld(MapConfig.exterior(), "exterior", withDialog = false)
    captureBattle("battle")

    // --- Scripted playthrough ---
    captureScriptedInterior()
    captureScriptedExterior()
    captureScriptedBattleMidway()
    captureScriptedBattleVictory()
}

// =============================================================================
// SCRIPTED PLAYTHROUGH
// =============================================================================

/**
 * Interior: spawn at (8,12), walk 3 steps south, face Barkeep, open dialog.
 */
private fun captureScriptedInterior() {
    val config = MapConfig.interior()
    korgeScreenshotTest(Size(VW, VH)) {
        val tiledMap = TmxLoader.parse(resourcesVfs[config.tmxPath].readString())
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }
        val mapView = TiledMapView(tiledMap, atlases)
        mapView.scale = SCALE
        addChild(mapView)

        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman()
        // Spawn + simulate walk south 3 tiles
        player.gridX = config.spawnX
        player.gridY = config.spawnY + 3
        player.facing = Facing.DOWN
        player.play(SpriteAnimation.IDLE)

        // Place NPCs
        for (npc in config.npcs) {
            val s = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
            s.loadFromSheet(npc.idleSheetPath)
            s.gridX = npc.tileX; s.gridY = npc.tileY; s.facing = npc.facing
            s.play(SpriteAnimation.IDLE)
        }

        // Camera on player
        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE

        // HUD + Dialog (simulating E-key interaction)
        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), config.displayName)
        DialogOverlay(this, VW, VH).show(config.npcs.first().dialog)

        save("dialog")
    }
}

/**
 * Exterior: player has transitioned from interior, spawns at (8,20).
 */
private fun captureScriptedExterior() {
    val config = MapConfig.exterior()
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
        player.facing = Facing.UP
        player.play(SpriteAnimation.WALK)

        for (npc in config.npcs) {
            val s = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
            s.loadFromSheet(npc.idleSheetPath)
            s.gridX = npc.tileX; s.gridY = npc.tileY; s.facing = npc.facing
            s.play(SpriteAnimation.IDLE)
        }

        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE

        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), config.displayName)

        save("exterior_entry")
    }
}

/**
 * Exterior with Guard dialog open.
 */
private fun captureScriptedExteriorDialog() {
    val config = MapConfig.exterior()
    val guard = config.npcs.first()
    korgeScreenshotTest(Size(VW, VH)) {
        val tiledMap = TmxLoader.parse(resourcesVfs[config.tmxPath].readString())
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, config.tmxDir) }
        val mapView = TiledMapView(tiledMap, atlases)
        mapView.scale = SCALE
        addChild(mapView)

        // Player adjacent to guard, facing the guard
        val player = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
        player.loadSwordsman()
        player.gridX = guard.tileX - 1
        player.gridY = guard.tileY
        player.facing = Facing.RIGHT
        player.play(SpriteAnimation.IDLE)

        for (npc in config.npcs) {
            val s = CharacterSprite(mapView, tiledMap.tileWidth, tiledMap.tileHeight)
            s.loadFromSheet(npc.idleSheetPath)
            s.gridX = npc.tileX; s.gridY = npc.tileY; s.facing = npc.facing
            s.play(SpriteAnimation.IDLE)
        }

        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE

        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), config.displayName)
        DialogOverlay(this, VW, VH).show(guard.dialog)

        save("exterior_dialog")
    }
}

/**
 * Battle midway: 2 attacks dealt, both combatants damaged.
 */
private fun captureScriptedBattleMidway() {
    korgeScreenshotTest(Size(VW, VH)) {
        solidRect(VW, VH, RGBA(0x0a, 0x0a, 0x14, 0xff))

        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        val vampire = Combatant(id = "vampire_1", name = "Vampire", maxHp = 60, side = Side.ENEMY, attackPower = 8)
        val engine = CombatEngine(party = listOf(hero), enemies = listOf(vampire))

        // Simulate 2 attack rounds
        engine.tick(CombatAction.Attack(vampire.id))
        engine.tick(CombatAction.Attack(vampire.id))

        val heroSprite = CharacterSprite(this, 48, 48)
        heroSprite.loadSwordsman(); heroSprite.gridX = 2; heroSprite.gridY = 3
        heroSprite.facing = Facing.RIGHT; heroSprite.play(SpriteAnimation.IDLE)

        val vampSprite = CharacterSprite(this, 48, 48)
        vampSprite.loadVampire(); vampSprite.gridX = 9; vampSprite.gridY = 3
        vampSprite.facing = Facing.LEFT; vampSprite.play(SpriteAnimation.HURT)

        // HP bars reflecting damage
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

/**
 * Battle victory: vampire defeated.
 */
private fun captureScriptedBattleVictory() {
    korgeScreenshotTest(Size(VW, VH)) {
        solidRect(VW, VH, RGBA(0x0a, 0x0a, 0x14, 0xff))

        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        val vampire = Combatant(id = "vampire_1", name = "Vampire", maxHp = 60, side = Side.ENEMY, attackPower = 8)
        val engine = CombatEngine(party = listOf(hero), enemies = listOf(vampire))

        // Fight until victory
        while (engine.result == CombatResult.ONGOING) {
            engine.tick(CombatAction.Attack(vampire.id))
        }

        val heroSprite = CharacterSprite(this, 48, 48)
        heroSprite.loadSwordsman(); heroSprite.gridX = 2; heroSprite.gridY = 3
        heroSprite.facing = Facing.RIGHT; heroSprite.play(SpriteAnimation.IDLE)

        val vampSprite = CharacterSprite(this, 48, 48)
        vampSprite.loadVampire(); vampSprite.gridX = 9; vampSprite.gridY = 3
        vampSprite.facing = Facing.LEFT; vampSprite.play(SpriteAnimation.DEATH)

        val barW = 120.0
        solidRect(barW, 12.0, Colors["#333333"]).apply { x = 40.0; y = 20.0 }
        solidRect(barW * hero.hpFraction, 12.0, Colors["#22cc22"]).apply { x = 40.0; y = 20.0 }
        solidRect(barW, 12.0, Colors["#333333"]).apply { x = VW - 160.0; y = 20.0 }
        solidRect(0.0, 12.0, Colors["#cc2222"]).apply { x = VW - 160.0; y = 20.0 }
        text("Nib: ${hero.hp}/${hero.maxHp}", textSize = 14.0, color = Colors.WHITE).apply { x = 40.0; y = 36.0 }
        text("Vampire: 0/${vampire.maxHp}", textSize = 14.0, color = Colors.WHITE).apply { x = VW - 160.0; y = 36.0 }
        text("VICTORY!", textSize = 24.0, color = Colors["#ffcc00"]).apply { x = VW / 2.0 - 60.0; y = VH / 2.0 - 12.0 }

        save("battle_victory")
    }
}

// =============================================================================
// ORIGINAL STATIC CAPTURES (backward compat)
// =============================================================================

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

        mapView.x = VW / 2.0 - player.visualGridX * tiledMap.tileWidth * SCALE
        mapView.y = VH / 2.0 - player.visualGridY * tiledMap.tileHeight * SCALE

        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), config.displayName).update(config.displayName)
        if (withDialog) DialogOverlay(this, VW, VH).show(config.npcs.first().dialog)

        save(name)
    }
}

private fun captureBattle(name: String) {
    korgeScreenshotTest(Size(VW, VH)) {
        solidRect(VW, VH, RGBA(0x0a, 0x0a, 0x14, 0xff))

        val heroSprite = CharacterSprite(this, 48, 48)
        heroSprite.loadSwordsman(); heroSprite.gridX = 2; heroSprite.gridY = 3; heroSprite.facing = Facing.RIGHT
        heroSprite.play(SpriteAnimation.IDLE)
        val vampSprite = CharacterSprite(this, 48, 48)
        vampSprite.loadVampire(); vampSprite.gridX = 9; vampSprite.gridY = 3; vampSprite.facing = Facing.LEFT
        vampSprite.play(SpriteAnimation.IDLE)

        val barW = 120.0
        solidRect(barW, 12.0, Colors["#333333"]).apply { x = 40.0; y = 20.0 }
        solidRect(barW, 12.0, Colors["#22cc22"]).apply { x = 40.0; y = 20.0 }
        solidRect(barW, 12.0, Colors["#333333"]).apply { x = VW - 160.0; y = 20.0 }
        solidRect(barW, 12.0, Colors["#cc2222"]).apply { x = VW - 160.0; y = 20.0 }
        text("Nib: 80/80", textSize = 14.0, color = Colors.WHITE).apply { x = 40.0; y = 36.0 }
        text("Vampire: 60/60", textSize = 14.0, color = Colors.WHITE).apply { x = VW - 160.0; y = 36.0 }
        text("ENTER=Attack  E=Heal  Q=Back", textSize = 12.0, color = Colors["#aaaaaa"])
            .apply { x = VW / 2.0 - 100.0; y = VH - 30.0 }

        save(name)
    }
}

private suspend fun OffscreenStage.save(name: String) {
    val bmp = renderToBitmap(views)
    OUT.mkdirs()
    bmp.writeTo(OUT["$name.png"], PNG)
    println("SCREENSHOT_OK: ${bmp.width}x${bmp.height} -> build/screenshots/$name.png")
}
