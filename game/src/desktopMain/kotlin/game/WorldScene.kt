package game

import korlibs.event.Key
import korlibs.io.file.std.resourcesVfs
import korlibs.korge.scene.Scene
import korlibs.korge.scene.sceneContainer
import korlibs.korge.view.SContainer
import korlibs.korge.view.addUpdater
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

/**
 * Main world scene — replaces TiledMapScene as boot target.
 * Loads a TMX map, renders it, places the player + NPCs, handles smooth movement,
 * NPC dialog, HUD, map transitions, and battle triggers.
 */
class WorldScene : Scene() {

    companion object {
        /** Set before calling changeTo<WorldScene>() to configure the next map load. */
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

        // SliceDirector for Bark pipeline
        val director = SliceDirector { System.currentTimeMillis() }
        director.barkAudioPlayer = BarkAudioPlayer(GameAudioPlayer(this@WorldScene))
        val roomId = if (config.id == MapId.INTERIOR) RoomContext.ROOM_TAVERN else "exterior"
        director.enterRoom(RoomContext(mapId = config.id.name.lowercase(), roomId = roomId))

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

        // 6. HUD (in scene root — screen-fixed, not inside mapView)
        val hud = HudOverlay(this, hero, inventory, config.displayName)

        // 7. Dialog overlay (in scene root)
        val dialog = DialogOverlay(this, width, height)

        // 7b. Questbook overlay (in scene root)
        val questbook = QuestbookOverlay(this, width, height)
        questbook.refresh(director.pressure, director.questMarkers + director.falseMarkers)

        // 8. BGM
        audioManager.playMusic(config.bgmPath)

        // 9. Input loop
        addUpdater { dt ->
            questbook.update(dt.seconds.toFloat())
            val keys = views.input.keys

            // --- Dialog has priority ---
            if (dialog.isActive) {
                if (keys.justPressed(Key.RETURN) || keys.justPressed(Key.SPACE)) {
                    dialog.advance()
                }
                return@addUpdater
            }

            // --- SPACE → BattleScene ---
            if (keys.justPressed(Key.SPACE)) {
                audioManager.stopMusic()
                launch { sceneContainer.changeTo<BattleScene>() }
                return@addUpdater
            }

            // --- E → NPC interaction ---
            if (keys.justPressed(Key.E)) {
                val faceX = player.gridX + player.facing.dx
                val faceY = player.gridY + player.facing.dy
                val npc = npcSprites.firstOrNull { (def, _) ->
                    def.tileX == faceX && def.tileY == faceY && def.dialog.isNotEmpty()
                }
                if (npc != null) {
                    dialog.show(npc.first.dialog)
                    npc.first.barkEvent?.let { event ->
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

            // --- Movement (only when not mid-step) ---
            var dx = 0; var dy = 0
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
                    // NPC blocking: don't walk onto an NPC tile
                    val npcBlocking = npcSprites.any { (def, _) ->
                        def.tileX == nx && def.tileY == ny
                    }
                    if (!npcBlocking && player.startMove(nx, ny)) {
                        player.play(SpriteAnimation.WALK)

                        // Exit check after successful move
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

            // --- HUD update ---
            hud.update(config.displayName)
        }

        // 10. Camera follow (uses visualGridX/Y for smooth tracking)
        addUpdater {
            val px = player.visualGridX * tiledMap.tileWidth * mapScale
            val py = player.visualGridY * tiledMap.tileHeight * mapScale
            mapView.x = views.virtualWidth / 2.0 - px
            mapView.y = views.virtualHeight / 2.0 - py
        }
    }

    override suspend fun sceneAfterDestroy() {
        audioManager.stopMusic()
    }
}
