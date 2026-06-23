package rpg.combat

import rpg.bark.BarkEvent

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
    private val bossController: BossController? = null
) {
    private val _enemies: MutableList<Combatant> = enemies.toMutableList()
    val enemies: List<Combatant> get() = _enemies.toList()

    var result: CombatResult = CombatResult.ONGOING
        private set

    val bossPhase: BossPhase? get() = bossController?.currentPhase

    private val pending = mutableListOf<CombatEvent>()

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
        }

        if (livingEnemies().isEmpty()) {
            result = CombatResult.VICTORY
            events += CombatEvent.Message("Encounter won.")
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

    private fun enemyTurn(dodging: Boolean, events: MutableList<CombatEvent>) {
        // Boss acts first (phase update + scaled attack), then remaining enemies.
        if (boss != null && boss.isAlive && bossController != null) {
            val dmg = bossController.takeTurn(boss, events)
            strikeParty(boss, dmg, dodging, events)
        }
        for (enemy in _enemies.filter { it.isAlive && it !== boss }) {
            strikeParty(enemy, enemy.attackPower, dodging, events)
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
    }
}
