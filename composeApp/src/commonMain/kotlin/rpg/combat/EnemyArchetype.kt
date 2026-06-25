package rpg.combat

/** Enemy templates used across the campaign (docs/CAMPAIGN.md). */
enum class EnemyArchetype(
    val displayName: String,
    val maxHp: Int,
    val attackPower: Int
) {
    // ─── Chapter 1: Sewers ───────────────────────────────────────────────
    SEWER_RAT("Sewer Rat", maxHp = 12, attackPower = 3),
    SLUDGE_BLOB("Sludge Blob", maxHp = 20, attackPower = 4),
    TAX_SLIME("Tax Slime", maxHp = 18, attackPower = 4),
    RAT_ACCOUNTANT("The Rat Accountant", maxHp = 60, attackPower = 6),

    // ─── Chapter 3: Woods ─────────────────────────────────────────────────
    KOBOLD_SCOUT("Kobold Scout", maxHp = 10, attackPower = 4),
    QUEST_WISP("Quest Wisp", maxHp = 8, attackPower = 5),
    HELPFUL_TREE("The Helpful Tree", maxHp = 70, attackPower = 5),

    // ─── Chapter 4: Ship ──────────────────────────────────────────────────
    PIRATE_CLERK("Pirate Clerk", maxHp = 14, attackPower = 5),
    BARREL_MIMIC("Barrel Mimic", maxHp = 22, attackPower = 6),
    CAPTAIN_FORMBEARD("Captain Formbeard", maxHp = 80, attackPower = 7),

    // ─── Chapter 5: Island Cave ──────────────────────────────────────────
    ADMINISTRAGON("The Administragon", maxHp = 120, attackPower = 8);

    /** Builds a fresh [Combatant] from this template. */
    fun spawn(id: String, isPaperAdd: Boolean = false): Combatant =
        Combatant(
            id = id,
            name = displayName,
            maxHp = maxHp,
            side = Side.ENEMY,
            attackPower = attackPower,
            isPaperAdd = isPaperAdd
        )
}
