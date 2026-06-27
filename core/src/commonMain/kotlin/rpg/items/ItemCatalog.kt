package rpg.items

/**
 * All items the merchant sells. Prices are calibrated against a 50-gold starting
 * budget: a Minor Potion is just barely affordable; everything else requires
 * looting and saving. Descriptions maintain the game's tone of bureaucratic dread.
 */
object ItemCatalog {
    val ALL: List<Item> = listOf(

        // Potions
        Item(
            id = "minor_potion",
            name = "Minor Potion",
            description = "Restores 5 HP. Tastes like optimism. Legal status: disputed.",
            type = ItemType.POTION,
            price = 30,
            effectValue = 5
        ),
        Item(
            id = "potion",
            name = "Potion",
            description = "Restores 15 HP. All herbs certified organic by someone.",
            type = ItemType.POTION,
            price = 80,
            effectValue = 15
        ),
        Item(
            id = "grand_potion",
            name = "Grand Potion",
            description = "Restores 30 HP. Named after a concept. Smells expensive.",
            type = ItemType.POTION,
            price = 200,
            effectValue = 30
        ),
        Item(
            id = "elixir",
            name = "Elixir",
            description = "Full restore. Questbook classifies this as 'suspicious wellness.'",
            type = ItemType.POTION,
            price = 500,
            effectValue = 999
        ),

        // Weapons
        Item(
            id = "short_sword",
            name = "Short Sword",
            description = "Attack +2. Nothing personal. Just blade and intent.",
            type = ItemType.WEAPON,
            price = 120,
            effectValue = 2
        ),
        Item(
            id = "staff_of_marginally_better_lighting",
            name = "Staff of Marginally Better Lighting",
            description = "Attack +3. Vellum insisted. The name is legally accurate.",
            type = ItemType.WEAPON,
            price = 200,
            effectValue = 3
        ),
        Item(
            id = "longsword",
            name = "Longsword",
            description = "Attack +4. A commitment. Brugg approves without comment.",
            type = ItemType.WEAPON,
            price = 350,
            effectValue = 4
        ),
        Item(
            id = "warhammer",
            name = "Warhammer",
            description = "Attack +5. 'Heavy and correct.' — Brugg, final review.",
            type = ItemType.WEAPON,
            price = 500,
            effectValue = 5
        )
    )

    private val byId = ALL.associateBy { it.id }

    fun get(id: String): Item? = byId[id]
}
