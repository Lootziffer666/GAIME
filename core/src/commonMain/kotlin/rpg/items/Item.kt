package rpg.items

/** The two kinds of shop item the party can buy. */
enum class ItemType {
    /** Consumable that restores HP when used. */
    POTION,

    /** Equippable that permanently raises a party member's attack power. */
    WEAPON
}

/**
 * A single purchasable item. Pure data: the canonical catalogue lives in
 * [ItemCatalog] and the player's owned items live in [Inventory].
 *
 * @param id stable lookup key (e.g. "minor_potion"), used by save data and [ItemCatalog.get].
 * @param displayName human-readable name shown in the shop UI.
 * @param type whether this is a [ItemType.POTION] or [ItemType.WEAPON].
 * @param price cost in gold.
 * @param effectValue for potions, the HP restored when used; 0 for weapons.
 * @param attackBonus for weapons, the attack power added when equipped; 0 for potions.
 */
data class Item(
    val id: String,
    val displayName: String,
    val type: ItemType,
    val price: Int,
    val effectValue: Int = 0,
    val attackBonus: Int = 0
)
