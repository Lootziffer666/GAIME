package rpg.items

enum class ItemType { POTION, WEAPON }

/**
 * A purchasable item. Potions restore HP; weapons permanently raise the party's
 * attack power. [effectValue] is heal amount for potions, attack bonus for weapons.
 * Prices are intentionally punishing — the party starts with 50 gold.
 */
data class Item(
    val id: String,
    val name: String,
    val description: String,
    val type: ItemType,
    val price: Int,
    val effectValue: Int
)
