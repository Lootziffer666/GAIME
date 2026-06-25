package rpg.combat

/** Enemy templates used in the vertical slice (docs/VERTICAL_SLICE.md). */
enum class EnemyArchetype(
    val displayName: String,
    val maxHp: Int,
    val attackPower: Int,
    val attackStyle: AttackStyle = AttackStyle.MELEE
) {
    SEWER_RAT("Sewer Rat", maxHp = 12, attackPower = 3, attackStyle = AttackStyle.MELEE),
    SLUDGE_BLOB("Sludge Blob", maxHp = 20, attackPower = 4, attackStyle = AttackStyle.RANGED_SLOW),
    RAT_ACCOUNTANT("The Rat Accountant", maxHp = 60, attackPower = 6),

    // ═══ Chapter 2 ═══════════════════════════════════════════════════════
    FOREST_WOLF("Forest Wolf", maxHp = 18, attackPower = 5, attackStyle = AttackStyle.MELEE),
    TAX_COLLECTOR_BADGER("The Tax Collector Badger", maxHp = 80, attackPower = 7);

    /** Builds a fresh [Combatant] from this template. */
    fun spawn(id: String, isPaperAdd: Boolean = false): Combatant =
        Combatant(
            id = id,
            name = displayName,
            maxHp = maxHp,
            side = Side.ENEMY,
            attackPower = attackPower,
            isPaperAdd = isPaperAdd,
            attackStyle = attackStyle
        )
}
