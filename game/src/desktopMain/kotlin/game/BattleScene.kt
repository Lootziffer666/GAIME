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
import korlibs.math.geom.Size
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
        val heroBarBg = solidRect(barWidth, barHeight, Colors["#333333"]).apply { x = 40.0; y = 20.0 }
        val heroBarFg = solidRect(barWidth, barHeight, Colors["#22cc22"]).apply { x = 40.0; y = 20.0 }
        val vampBarBg = solidRect(barWidth, barHeight, Colors["#333333"]).apply { x = vw - 160.0; y = 20.0 }
        val vampBarFg = solidRect(barWidth, barHeight, Colors["#cc2222"]).apply { x = vw - 160.0; y = 20.0 }

        val heroLabel = text("Nib: 80/80", textSize = 14.0, color = Colors.WHITE).apply { x = 40.0; y = 36.0 }
        val vampLabel = text("Vampire: 60/60", textSize = 14.0, color = Colors.WHITE).apply { x = vw - 160.0; y = 36.0 }

        val statusText = text("ENTER=Attack  E=Heal  Q=Back", textSize = 12.0, color = Colors["#aaaaaa"]).apply {
            x = vw / 2.0 - 100.0; y = vh - 30.0
        }

        val resultText = text("", textSize = 24.0, color = Colors["#ffcc00"]).apply {
            x = vw / 2.0 - 60.0; y = vh / 2.0 - 12.0
        }

        // --- Audio ---
        audioManager.playMusic("assets/audio/music/Defiance_at_Dawn.mp3")

        // --- Input + Combat Logic ---
        var battleOver = false

        addUpdater {
            val keys = views.input.keys

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
                    heroSprite.play(SpriteAnimation.ATTACK, loop = false) {
                        heroSprite.play(SpriteAnimation.IDLE)
                    }
                    engine.tick(CombatAction.Attack(target.id))
                    acted = true
                }
            } else if (keys.justPressed(Key.E)) {
                engine.tick(CombatAction.Heal)
                acted = true
            }

            if (acted) {
                // Update HP bars
                heroBarFg.width = barWidth * hero.hpFraction
                vampBarFg.width = barWidth * vampire.hpFraction
                heroLabel.text = "Nib: ${hero.hp}/${hero.maxHp}"
                vampLabel.text = "Vampire: ${vampire.hp}/${vampire.maxHp}"

                // Animations for damage
                if (!vampire.isAlive) {
                    vampireSprite.play(SpriteAnimation.DEATH, loop = false)
                } else if (acted) {
                    vampireSprite.play(SpriteAnimation.HURT, loop = false) {
                        vampireSprite.play(SpriteAnimation.IDLE)
                    }
                }
                if (!hero.isAlive) {
                    heroSprite.play(SpriteAnimation.DEATH, loop = false)
                }

                // Check result
                when (engine.result) {
                    CombatResult.VICTORY -> {
                        resultText.text = "VICTORY!"
                        battleOver = true
                        // SFX
                        kotlinx.coroutines.MainScope().let { /* SFX best-effort */ }
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
