package rpg.items

/** The two kinds of shop item the party can buy. */
enum class ItemType {
    /** Consumable that restores HP when used. */
    POTION,

    /** Equippable that permanently raises every party member's attack power. */
    WEAPON
}

/**
 * A single purchasable item. Pure data: the canonical catalogue lives in
 * [ItemCatalog] and the player's owned items live in [Inventory].
 *
 * @param id stable lookup key (e.g. "minor_potion"), used by [ItemCatalog.get] and save data.
 * @param name human-readable name shown in the shop.
 * @param description short flavour/utility line shown in the shop.
 * @param type whether this is a [ItemType.POTION] or [ItemType.WEAPON].
 * @param price cost in gold.
 * @param effectValue for potions, the HP restored when used; for weapons, the attack power
 *   added to every party member when equipped.
 */
data class Item(
    val id: String,
    val name: String,
    val description: String,
    val type: ItemType,
    val price: Int,
    val effectValue: Int
)
