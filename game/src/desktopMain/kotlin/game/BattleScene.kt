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
import korlibs.time.seconds
import kotlinx.coroutines.launch
import rpg.SliceDirector
import rpg.bark.BarkEvent
import rpg.bark.audio.BarkAudioPlayer
import rpg.combat.CombatAction
import rpg.combat.CombatEngine
import rpg.combat.CombatEvent
import rpg.combat.CombatResult
import rpg.combat.Combatant
import rpg.combat.EnemyArchetype
import rpg.combat.Side
import rpg.combat.TaxCollectorController
import game.shader.ShaderEffects

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

    companion object {
        /** true → Boss-Encounter (Rat Accountant), false → Standard-Vampir-Kampf. */
        var bossEncounter: Boolean = false
    }

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
        val engine = if (bossEncounter) {
            val boss = EnemyArchetype.RAT_ACCOUNTANT.spawn("boss_rat_accountant")
            CombatEngine(party = listOf(hero), enemies = emptyList(),
                boss = boss, bossController = TaxCollectorController())
        } else {
            CombatEngine(party = listOf(hero), enemies = listOf(vampire))
        }
        val enemyDisplay = if (bossEncounter) engine.boss!! else vampire
        val enemyName = if (bossEncounter) "The Rat Accountant" else "Vampire"

        // SliceDirector for combat barks
        val director = SliceDirector { System.currentTimeMillis() }
        director.barkAudioPlayer = BarkAudioPlayer(GameAudioPlayer(this@BattleScene))
        director.startCombat(engine)

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
        val vampLabel = text("$enemyName: ${enemyDisplay.hp}/${enemyDisplay.maxHp}", textSize = 14.0, color = Colors.WHITE)
            .apply { x = vw - 160.0; y = 36.0 }

        text("ENTER=Attack  E=Heal  Q=Back", textSize = 12.0, color = Colors["#aaaaaa"])
            .apply { x = vw / 2.0 - 100.0; y = vh - 30.0 }

        val resultText = text("", textSize = 24.0, color = Colors["#ffcc00"])
            .apply { x = vw / 2.0 - 60.0; y = vh / 2.0 - 12.0 }

        // --- Audio: battle BGM ---
        audioManager.playMusic("assets/audio/music/Defiance_at_Dawn.mp3")

        // --- QuestbookOverlay + ShaderStateBinder ---
        val questbook = QuestbookOverlay(this, vw, vh)
        questbook.refresh(director.pressure, director.questMarkers + director.falseMarkers)
        val effects = ShaderEffects()
        effects.startTimeUpdater(this)
        val shaderBinder = ShaderStateBinder(effects, this)
        shaderBinder.applyCombatDistress(hero.hpFraction, director.pressure)

        // --- SFX paths ---
        val sfxAttack = "assets/audio/sfx/Minifantasy_Dungeon_SFX/07_human_atk_sword_1.wav"
        val sfxHit = "assets/audio/sfx/Minifantasy_Dungeon_SFX/26_sword_hit_1.wav"
        val sfxVictory = "assets/audio/sfx/Level up Pickup (Rpg).wav"

        // --- Input + Combat Logic ---
        var battleOver = false
        var heroHpBefore = hero.hp
        var lastTurnResult: CombatResult = CombatResult.ONGOING
        var lastEvents: List<CombatEvent> = emptyList()

        addUpdater { dt ->
            questbook.update(dt.seconds.toFloat())
            val keys = views.input.keys

            // Q → flee / return to map
            if (keys.justPressed(Key.Q)) {
                audioManager.stopMusic()
                launch { sceneContainer.changeTo<WorldScene>() }
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
                    // Attack via SliceDirector (routes combat barks through Questbook)
                    launch { director.fireBark(BarkEvent.BRUGG_ATTACK) }
                    val turn = director.combatAction(CombatAction.Attack(target.id))
                    lastEvents = turn.events
                    lastTurnResult = turn.result
                    acted = true

                    // Hit SFX for the enemy taking damage
                    launch { audioManager.playSfx(sfxHit) }
                }
            } else if (keys.justPressed(Key.E)) {
                heroHpBefore = hero.hp
                val turn = director.combatAction(CombatAction.Heal)
                lastEvents = turn.events
                lastTurnResult = turn.result
                acted = true
            }

            if (acted) {
                // Process combat events (boss phase, adds, messages)
                for (event in lastEvents) {
                    when (event) {
                        is CombatEvent.BossPhaseChanged -> questbook.showMessage("The Accountant files objections!", director.pressure)
                        is CombatEvent.AddsSummoned -> questbook.showMessage("Adds summoned: ${event.count}", director.pressure)
                        is CombatEvent.Message -> questbook.showMessage(event.text, director.pressure)
                        is CombatEvent.BarkTriggered -> { /* audio already handled by director */ }
                    }
                }
                // Shader = State: combat distress
                shaderBinder.applyCombatDistress(hero.hpFraction, director.pressure)

                // Update HP bars
                heroBarFg.width = barWidth * hero.hpFraction
                vampBarFg.width = barWidth * enemyDisplay.hpFraction
                heroLabel.text = "Nib: ${hero.hp}/${hero.maxHp}"
                vampLabel.text = "$enemyName: ${enemyDisplay.hp}/${enemyDisplay.maxHp}"

                // --- Vampire animations ---
                if (!enemyDisplay.isAlive) {
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
                when (lastTurnResult) {
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
