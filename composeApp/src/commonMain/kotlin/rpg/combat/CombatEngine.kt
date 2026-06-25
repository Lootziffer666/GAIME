package rpg.combat

import rpg.bark.BarkEvent
import kotlin.random.Random

/**
 * Deterministic, tick-based combat core. No real-time loop and no uncontrolled
 * RNG: each call to [tick] advances exactly one round given the player's
 * [CombatAction], so encounters are fully reproducible in tests. Real-time
 * top-down wiring (movement, collision, dodge timing) is layered on later.
 *
 * Combat is the simple top-down model from docs/VERTICAL_SLICE.md: attack,
 * dodge, and the occasional utility bark. Enemy targeting is deterministic
 * (always the first living party member).
 */
class CombatEngine(
    val party: List<Combatant>,
    enemies: List<Combatant>,
    val boss: Combatant? = null,
    private val bossController: BossController? = null,
    private val random: Random = Random
) {
    private val _enemies: MutableList<Combatant> = enemies.toMutableList()
    val enemies: List<Combatant> get() = _enemies.toList()

    var result: CombatResult = CombatResult.ONGOING
        private set

    val bossPhase: BossPhase? get() = bossController?.currentPhase

    private val pending = mutableListOf<CombatEvent>()
    private var hasFiredTaunt = false

    /** Heal amount per Heal action. */
    private val healAmount = 8

    init {
        require((boss == null) == (bossController == null)) {
            "boss and bossController must be provided together"
        }
        if (boss != null && bossController != null) {
            _enemies.add(boss)
            bossController.onCombatStart(this, pending)
        }
    }

    /** Allows the boss controller to summon adds mid-fight. */
    fun addEnemy(enemy: Combatant) {
        _enemies.add(enemy)
    }

    fun livingEnemies(): List<Combatant> = _enemies.filter { it.isAlive }
    fun livingParty(): List<Combatant> = party.filter { it.isAlive }

    private fun playerAttackPower(): Int = livingParty().sumOf { it.attackPower }

    /** Advances one round. Returns the events produced this tick. */
    fun tick(action: CombatAction): List<CombatEvent> {
        if (result != CombatResult.ONGOING) return emptyList()

        val events = mutableListOf<CombatEvent>()
        if (pending.isNotEmpty()) {
            events += pending
            pending.clear()
        }

        // Fire a taunt bark on the first tick of combat
        if (!hasFiredTaunt) {
            hasFiredTaunt = true
            pickTauntBark()?.let { events += CombatEvent.BarkTriggered(it) }
        }

        var dodging = false
        when (action) {
            is CombatAction.Attack -> {
                val target = _enemies.firstOrNull { it.id == action.targetId && it.isAlive }
                if (target != null) {
                    val dealt = target.takeDamage(playerAttackPower())
                    events += CombatEvent.Message("Party hits ${target.name} for $dealt.")
                    if (!target.isAlive) events += CombatEvent.Message("${target.name} is defeated.")
                } else {
                    events += CombatEvent.Message("No such target.")
                }
            }
            CombatAction.Dodge -> {
                dodging = true
                events += CombatEvent.Message("The party braces to dodge.")
            }
            is CombatAction.UtilityBark -> applyUtilityBark(action.bark, events)
            CombatAction.Wait -> { /* pass */ }
            CombatAction.Heal -> applyHeal(events)
        }

        if (livingEnemies().isEmpty()) {
            result = CombatResult.VICTORY
            events += CombatEvent.Message("Encounter won.")
            pickVictoryBark()?.let { events += CombatEvent.BarkTriggered(it) }
            return events
        }

        enemyTurn(dodging, events)

        if (livingParty().isEmpty()) {
            result = CombatResult.DEFEAT
            events += CombatEvent.Message("The party falls. Quest Status: Unresolved.")
        }
        return events
    }

    private fun applyUtilityBark(bark: BarkEvent, events: MutableList<CombatEvent>) {
        // Surface the bark so the SliceDirector can route it through the Questbook too.
        events += CombatEvent.BarkTriggered(bark)
        when (bark) {
            BarkEvent.VELLUM_CALLS_FOR_FLAME -> {
                val burned = _enemies.filter { it.isAlive && it.isPaperAdd }
                burned.forEach { it.kill() }
                events += if (burned.isEmpty()) {
                    CombatEvent.Message("Flame finds no paper to burn.")
                } else {
                    CombatEvent.Message("Flame burns ${burned.size} paper add(s).")
                }
            }
            BarkEvent.BRUGG_ATTACK ->
                events += CombatEvent.Message("Brugg swings with extra force.")
            else ->
                events += CombatEvent.Message("Utility bark spent.")
        }
    }

    private fun applyHeal(events: MutableList<CombatEvent>) {
        val target = livingParty().minByOrNull { it.hp } ?: return
        val healed = target.heal(healAmount)
        events += CombatEvent.Message("${target.name} is healed for $healed HP.")
        val bark = healingBarkFor(target.id)
        if (bark != null) {
            events += CombatEvent.BarkTriggered(bark)
        }
    }

    private fun healingBarkFor(characterId: String): BarkEvent? = when (characterId) {
        "nib" -> BarkEvent.NIB_GOOD_AS_NEW
        "brugg" -> BarkEvent.BRUGG_I_FEEL_BETTER_THAN_EVER
        "vellum" -> BarkEvent.VELLUM_IM_BACK_ON_MY_FEET
        else -> null
    }

    private fun pickTauntBark(): BarkEvent? {
        val living = livingParty()
        if (living.isEmpty()) return null
        val speaker = living[random.nextInt(living.size)]
        val taunts = tauntsFor(speaker.id)
        return if (taunts.isNotEmpty()) taunts[random.nextInt(taunts.size)] else null
    }

    private fun tauntsFor(characterId: String): List<BarkEvent> = when (characterId) {
        "nib" -> listOf(
            BarkEvent.NIB_FROM_THE_SHADOWS,
            BarkEvent.NIB_IS_THAT_ALL_YOUVE_GOT,
            BarkEvent.NIB_YOUR_DEFENSES_ARE_WEAK
        )
        "brugg" -> listOf(
            BarkEvent.BRUGG_HAVE_AT_THEE,
            BarkEvent.BRUGG_SURRENDER_OR_DIE,
            BarkEvent.BRUGG_SHOW_YOURSELVES
        )
        "vellum" -> listOf(
            BarkEvent.VELLUM_LETS_SEE_IF_YOU_CAN_DODGE,
            BarkEvent.VELLUM_YOUR_DEFENSES_ARE_FUTILE,
            BarkEvent.VELLUM_I_SMITE_YOU
        )
        else -> emptyList()
    }

    private fun pickVictoryBark(): BarkEvent? {
        val living = livingParty()
        if (living.isEmpty()) return null
        val speaker = living[random.nextInt(living.size)]
        val victories = victoriesFor(speaker.id)
        return if (victories.isNotEmpty()) victories[random.nextInt(victories.size)] else null
    }

    private fun victoriesFor(characterId: String): List<BarkEvent> = when (characterId) {
        "nib" -> listOf(BarkEvent.NIB_IS_THAT_ALL_YOUVE_GOT)
        "brugg" -> listOf(BarkEvent.BRUGG_SURRENDER_OR_DIE)
        "vellum" -> listOf(BarkEvent.VELLUM_YOUR_DEFENSES_ARE_FUTILE)
        else -> emptyList()
    }

    private fun enemyTurn(dodging: Boolean, events: MutableList<CombatEvent>) {
        // Boss acts first (phase update + scaled attack), then remaining enemies.
        if (boss != null && boss.isAlive && bossController != null) {
            val dmg = bossController.takeTurn(boss, events)
            strikeParty(boss, dmg, dodging, events)
        }
        for (enemy in _enemies.filter { it.isAlive && it !== boss }) {
            when (enemy.attackStyle) {
                AttackStyle.MELEE -> strikeParty(enemy, enemy.attackPower, dodging, events)
                AttackStyle.RANGED_SLOW -> {
                    if (enemy.ticksSinceLastAttack % 2 == 0) {
                        // Attack tick: pick a random living party member
                        strikeRandomPartyMember(enemy, enemy.attackPower, dodging, events)
                    } else {
                        // Preparing tick: skip attack
                        events += CombatEvent.Message("${enemy.name} is preparing to spit...")
                    }
                    enemy.ticksSinceLastAttack++
                }
            }
        }
    }

    private fun strikeRandomPartyMember(
        attacker: Combatant,
        damage: Int,
        dodging: Boolean,
        events: MutableList<CombatEvent>
    ) {
        if (dodging) {
            events += CombatEvent.Message("${attacker.name}'s attack is dodged.")
            return
        }
        val living = livingParty()
        val target = living.getOrNull(random.nextInt(living.size)) ?: return
        val dealt = target.takeDamage(damage)
        events += CombatEvent.Message("${attacker.name} hits ${target.name} for $dealt.")

        // Emit damage/death barks for party members
        if (target.side == Side.PLAYER && dealt > 0) {
            if (!target.isAlive) {
                deathBarkFor(target.id)?.let { events += CombatEvent.BarkTriggered(it) }
            } else if (random.nextFloat() < 0.4f) {
                val bark = damageBarkFor(target.id, dealt, target)
                if (bark != null) {
                    events += CombatEvent.BarkTriggered(bark)
                }
            }
        }
    }

    private fun strikeParty(attacker: Combatant, damage: Int, dodging: Boolean, events: MutableList<CombatEvent>) {
        if (dodging) {
            events += CombatEvent.Message("${attacker.name}'s attack is dodged.")
            return
        }
        val target = livingParty().firstOrNull() ?: return
        val dealt = target.takeDamage(damage)
        events += CombatEvent.Message("${attacker.name} hits ${target.name} for $dealt.")

        // Emit damage/death barks for party members
        if (target.side == Side.PLAYER && dealt > 0) {
            if (!target.isAlive) {
                // Death bark (always fires on death)
                deathBarkFor(target.id)?.let { events += CombatEvent.BarkTriggered(it) }
            } else if (random.nextFloat() < 0.4f) {
                // 40% chance to fire a damage reaction bark
                val bark = damageBarkFor(target.id, dealt, target)
                if (bark != null) {
                    events += CombatEvent.BarkTriggered(bark)
                }
            }
        }
    }

    private fun deathBarkFor(characterId: String): BarkEvent? = when (characterId) {
        "nib" -> BarkEvent.NIB_AVENGE_ME
        "brugg" -> BarkEvent.BRUGG_I_DONT_HAVE_MUCH_LEFT
        "vellum" -> BarkEvent.VELLUM_I_DIDNT_THINK_IT_WOULD_END
        else -> null
    }

    private fun damageBarkFor(characterId: String, dealt: Int, target: Combatant): BarkEvent? {
        // Low HP: urgent bark
        if (target.hpFraction < 0.25f) {
            return when (characterId) {
                "nib" -> BarkEvent.NIB_THAT_STINGS
                "brugg" -> BarkEvent.BRUGG_I_DONT_HAVE_MUCH_LEFT
                "vellum" -> BarkEvent.VELLUM_I_NEED_A_HEALER
                else -> null
            }
        }
        // Medium damage (>30% of maxHp): painful bark
        if (dealt > target.maxHp * 0.3f) {
            return when (characterId) {
                "nib" -> BarkEvent.NIB_THAT_STINGS
                "brugg" -> BarkEvent.BRUGG_THATS_GOING_TO_LEAVE_A_MARK
                "vellum" -> BarkEvent.VELLUM_IM_GOING_TO_FEEL_THAT
                else -> null
            }
        }
        // Light damage: minor bark
        return when (characterId) {
            "nib" -> BarkEvent.NIB_LUCKY_HIT
            "brugg" -> BarkEvent.BRUGG_THAT_DREW_BLOOD
            "vellum" -> BarkEvent.VELLUM_IM_GOING_TO_FEEL_THAT
            else -> null
        }
    }
}
