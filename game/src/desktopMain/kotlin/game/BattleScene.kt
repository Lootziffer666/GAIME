package game

import korlibs.event.Key
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.korge.scene.Scene
import korlibs.korge.scene.sceneContainer
import korlibs.korge.view.SContainer
import korlibs.korge.view.addUpdater
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import kotlinx.coroutines.launch
import rpg.combat.CombatAction
import rpg.combat.CombatEngine
import rpg.combat.CombatResult
import rpg.combat.Combatant
import rpg.combat.Side

/**
 * Side-by-side battle scene: Swordsman (left) vs Vampire (right).
 * Turn-based, driven by :core's [CombatEngine].
 *
 * Controls:
 * - ENTER/SPACE: Attack
 * - E: Heal
 * - Q: Return to map (flee / after victory/defeat)
 *
 * Audio:
 * - BGM: Defiance_at_Dawn.mp3 (battle theme)
 * - Attack SFX: 07_human_atk_sword_1.wav
 * - Hit SFX: 26_sword_hit_1.wav
 * - Victory SFX: Level up Pickup (Rpg).wav
 */
class BattleScene : Scene() {

    private val audioManager = AudioManager()

    override suspend fun SContainer.sceneMain() {
        val vw = width
        val vh = height

        // Black backdrop
        solidRect(vw, vh, RGBA(0x0a, 0x0a, 0x14, 0xff))

        // --- Combatants ---
        val hero = Combatant(
            id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12
        )
        val vampire = Combatant(
            id = "vampire_1", name = "Vampire", maxHp = 60, side = Side.ENEMY, attackPower = 8
        )
        val engine = CombatEngine(party = listOf(hero), enemies = listOf(vampire))

        // --- Sprites ---
        val heroSprite = CharacterSprite(this, 48, 48)
        heroSprite.loadSwordsman()
        heroSprite.gridX = 2; heroSprite.gridY = 3
        heroSprite.facing = Facing.RIGHT
        heroSprite.play(SpriteAnimation.IDLE)

        val vampireSprite = CharacterSprite(this, 48, 48)
        vampireSprite.loadVampire()
        vampireSprite.gridX = 9; vampireSprite.gridY = 3
        vampireSprite.facing = Facing.LEFT
        vampireSprite.play(SpriteAnimation.IDLE)

        // --- HP Bars ---
        val barWidth = 120.0
        val barHeight = 12.0
        solidRect(barWidth, barHeight, Colors["#333333"]).apply { x = 40.0; y = 20.0 }
        val heroBarFg = solidRect(barWidth, barHeight, Colors["#22cc22"]).apply { x = 40.0; y = 20.0 }
        solidRect(barWidth, barHeight, Colors["#333333"]).apply { x = vw - 160.0; y = 20.0 }
        val vampBarFg = solidRect(barWidth, barHeight, Colors["#cc2222"]).apply { x = vw - 160.0; y = 20.0 }

        val heroLabel = text("Nib: ${hero.hp}/${hero.maxHp}", textSize = 14.0, color = Colors.WHITE)
            .apply { x = 40.0; y = 36.0 }
        val vampLabel = text("Vampire: ${vampire.hp}/${vampire.maxHp}", textSize = 14.0, color = Colors.WHITE)
            .apply { x = vw - 160.0; y = 36.0 }

        text("ENTER=Attack  E=Heal  Q=Back", textSize = 12.0, color = Colors["#aaaaaa"])
            .apply { x = vw / 2.0 - 100.0; y = vh - 30.0 }

        val resultText = text("", textSize = 24.0, color = Colors["#ffcc00"])
            .apply { x = vw / 2.0 - 60.0; y = vh / 2.0 - 12.0 }

        // --- Audio: battle BGM ---
        audioManager.playMusic("assets/audio/music/Defiance_at_Dawn.mp3")

        // --- SFX paths ---
        val sfxAttack = "assets/audio/sfx/Minifantasy_Dungeon_SFX/07_human_atk_sword_1.wav"
        val sfxHit = "assets/audio/sfx/Minifantasy_Dungeon_SFX/26_sword_hit_1.wav"
        val sfxVictory = "assets/audio/sfx/Level up Pickup (Rpg).wav"

        // --- Input + Combat Logic ---
        var battleOver = false
        var heroHpBefore = hero.hp

        addUpdater {
            val keys = views.input.keys

            // Q → flee / return to map
            if (keys.justPressed(Key.Q)) {
                audioManager.stopMusic()
                launch { sceneContainer.changeTo<TiledMapScene>() }
                return@addUpdater
            }

            if (battleOver) return@addUpdater

            var acted = false

            if (keys.justPressed(Key.RETURN) || keys.justPressed(Key.SPACE)) {
                // Attack the first living enemy
                val target = engine.livingEnemies().firstOrNull()
                if (target != null) {
                    heroHpBefore = hero.hp

                    // Hero attack animation
                    heroSprite.play(SpriteAnimation.ATTACK, loop = false) {
                        heroSprite.play(SpriteAnimation.IDLE)
                    }
                    // Attack SFX
                    launch { audioManager.playSfx(sfxAttack) }

                    // Tick the engine (hero attacks, then enemy retaliates)
                    engine.tick(CombatAction.Attack(target.id))
                    acted = true

                    // Hit SFX for the enemy taking damage
                    launch { audioManager.playSfx(sfxHit) }
                }
            } else if (keys.justPressed(Key.E)) {
                heroHpBefore = hero.hp
                engine.tick(CombatAction.Heal)
                acted = true
            }

            if (acted) {
                // Update HP bars
                heroBarFg.width = barWidth * hero.hpFraction
                vampBarFg.width = barWidth * vampire.hpFraction
                heroLabel.text = "Nib: ${hero.hp}/${hero.maxHp}"
                vampLabel.text = "Vampire: ${vampire.hp}/${vampire.maxHp}"

                // --- Vampire animations ---
                if (!vampire.isAlive) {
                    vampireSprite.play(SpriteAnimation.DEATH, loop = false)
                } else {
                    // Vampire was hit → hurt animation, then back to idle
                    vampireSprite.play(SpriteAnimation.HURT, loop = false) {
                        vampireSprite.play(SpriteAnimation.IDLE)
                    }
                }

                // --- Hero animations after enemy retaliation ---
                if (!hero.isAlive) {
                    heroSprite.play(SpriteAnimation.DEATH, loop = false)
                } else if (hero.hp < heroHpBefore) {
                    // Hero took damage from enemy's counter-attack → hurt anim
                    heroSprite.play(SpriteAnimation.HURT, loop = false) {
                        heroSprite.play(SpriteAnimation.IDLE)
                    }
                    launch { audioManager.playSfx(sfxHit) }
                }

                // --- Check result ---
                when (engine.result) {
                    CombatResult.VICTORY -> {
                        resultText.text = "VICTORY!"
                        battleOver = true
                        launch { audioManager.playSfx(sfxVictory) }
                    }
                    CombatResult.DEFEAT -> {
                        resultText.text = "DEFEAT"
                        battleOver = true
                    }
                    CombatResult.ONGOING -> { /* continue */ }
                }
            }
        }
    }

    override suspend fun sceneAfterDestroy() {
        audioManager.stopMusic()
    }
}
