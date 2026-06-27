package rpg.items

/**
 * The fixed catalogue of everything the shop sells: four healing potions and
 * four melee weapons. Prices and effect/attack values are balanced so each tier
 * is a meaningful upgrade. Lookups by [id] back save/load and shop wiring.
 */
object ItemCatalog {

    // ─── Potions (effectValue = HP restored) ─────────────────────────────
    val MINOR_POTION = Item("minor_potion", "Minor Potion", ItemType.POTION, price = 30, effectValue = 5)
    val STANDARD_POTION = Item("standard_potion", "Standard Potion", ItemType.POTION, price = 60, effectValue = 12)
    val GREATER_POTION = Item("greater_potion", "Greater Potion", ItemType.POTION, price = 120, effectValue = 25)
    val GRAND_ELIXIR = Item("grand_elixir", "Grand Elixir", ItemType.POTION, price = 200, effectValue = 50)

    // ─── Weapons (attackBonus = attack power added on equip) ──────────────
    val SHORT_SWORD = Item("short_sword", "Short Sword", ItemType.WEAPON, price = 40, attackBonus = 2)
    val BROADSWORD = Item("broadsword", "Broadsword", ItemType.WEAPON, price = 90, attackBonus = 5)
    val WARHAMMER = Item("warhammer", "Warhammer", ItemType.WEAPON, price = 150, attackBonus = 8)
    val RUNEBLADE = Item("runeblade", "Runeblade", ItemType.WEAPON, price = 300, attackBonus = 14)

    /** Every item the shop offers: 4 potions + 4 weapons. */
    val ALL: List<Item> = listOf(
        MINOR_POTION, STANDARD_POTION, GREATER_POTION, GRAND_ELIXIR,
        SHORT_SWORD, BROADSWORD, WARHAMMER, RUNEBLADE
    )

    private val byId: Map<String, Item> = ALL.associateBy { it.id }

    /** Returns the catalogue item with the given [id], or null if none exists. */
    fun get(id: String): Item? = byId[id]
}
