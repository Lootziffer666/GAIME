package rpg.items

/**
 * The fixed catalogue of everything the shop sells: four healing potions and
 * four melee weapons. For potions [Item.effectValue] is HP restored; for weapons
 * it is the attack power added to every party member on equip. Lookups by [get]
 * back save/load and shop wiring.
 */
object ItemCatalog {

    // ─── Potions (effectValue = HP restored) ─────────────────────────────
    val MINOR_POTION = Item(
        "minor_potion", "Minor Potion",
        "Restores a little health. Tastes of regret.",
        ItemType.POTION, price = 20, effectValue = 5
    )
    val STANDARD_POTION = Item(
        "standard_potion", "Standard Potion",
        "A dependable healing draught.",
        ItemType.POTION, price = 45, effectValue = 12
    )
    val GREATER_POTION = Item(
        "greater_potion", "Greater Potion",
        "Restores a serious chunk of health.",
        ItemType.POTION, price = 90, effectValue = 25
    )
    val GRAND_ELIXIR = Item(
        "grand_elixir", "Grand Elixir",
        "Near-full recovery. Suspiciously fizzy.",
        ItemType.POTION, price = 160, effectValue = 50
    )

    // ─── Weapons (effectValue = attack power added to every party member) ─
    val RUSTY_DAGGER = Item(
        "rusty_dagger", "Rusty Dagger",
        "Better than fists. Marginally.",
        ItemType.WEAPON, price = 15, effectValue = 1
    )
    val SHORT_SWORD = Item(
        "short_sword", "Short Sword",
        "A reliable blade for the whole party.",
        ItemType.WEAPON, price = 40, effectValue = 2
    )
    val BROADSWORD = Item(
        "broadsword", "Broadsword",
        "Heavy, sharp, and intimidating.",
        ItemType.WEAPON, price = 80, effectValue = 5
    )
    val WARHAMMER = Item(
        "warhammer", "Warhammer",
        "Turns paperwork into confetti.",
        ItemType.WEAPON, price = 140, effectValue = 8
    )

    /** Every item the shop offers: 4 potions + 4 weapons. */
    val ALL: List<Item> = listOf(
        MINOR_POTION, STANDARD_POTION, GREATER_POTION, GRAND_ELIXIR,
        RUSTY_DAGGER, SHORT_SWORD, BROADSWORD, WARHAMMER
    )

    private val byId: Map<String, Item> = ALL.associateBy { it.id }

    /** Returns the catalogue item with the given [id], or null if none exists. */
    fun get(id: String): Item? = byId[id]

    /** All potions in the catalogue. */
    fun potions(): List<Item> = ALL.filter { it.type == ItemType.POTION }

    /** All weapons in the catalogue. */
    fun weapons(): List<Item> = ALL.filter { it.type == ItemType.WEAPON }
}
