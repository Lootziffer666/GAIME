package rpg.items

import rpg.combat.Combatant

/** Outcome of an [Inventory.buy] attempt. */
enum class PurchaseResult {
    /** A potion was purchased and added to the stash. */
    BOUGHT,

    /** A weapon was purchased and equipped on the lead party member. */
    EQUIPPED,

    /** The weapon was already equipped; nothing changed and no gold was spent. */
    ALREADY_EQUIPPED,

    /** Not enough gold; nothing changed. */
    CANNOT_AFFORD
}

/**
 * The party's gold, owned potions and equipped weapons. Pure logic with no I/O:
 * the lead party member ([party] index 0) carries equipped weapons, and potions
 * heal the most-wounded living member.
 *
 * @param party the player's combatants; index 0 is the lead who equips weapons.
 * @param gold starting gold (default 50).
 */
class Inventory(
    val party: List<Combatant>,
    var gold: Int = 50
) {
    private val _potionCounts: MutableMap<String, Int> = mutableMapOf()
    private val _equippedWeapons: MutableSet<String> = mutableSetOf()

    /** Owned potions keyed by item id, with quantities. */
    val potionCounts: Map<String, Int> get() = _potionCounts.toMap()

    /** Ids of weapons currently equipped on the lead member. */
    val equippedWeapons: Set<String> get() = _equippedWeapons.toSet()

    /**
     * Attempts to purchase [item].
     *
     * - Weapons: returns [PurchaseResult.ALREADY_EQUIPPED] if already owned,
     *   [PurchaseResult.CANNOT_AFFORD] if too expensive, otherwise spends gold,
     *   equips it (raising [party] lead's `attackPower` by [Item.attackBonus])
     *   and returns [PurchaseResult.EQUIPPED].
     * - Potions: returns [PurchaseResult.CANNOT_AFFORD] if too expensive,
     *   otherwise spends gold, adds one to the stash and returns
     *   [PurchaseResult.BOUGHT].
     */
    fun buy(item: Item): PurchaseResult = when (item.type) {
        ItemType.WEAPON -> {
            when {
                item.id in _equippedWeapons -> PurchaseResult.ALREADY_EQUIPPED
                gold < item.price -> PurchaseResult.CANNOT_AFFORD
                else -> {
                    gold -= item.price
                    _equippedWeapons.add(item.id)
                    party.firstOrNull()?.let { it.attackPower += item.attackBonus }
                    PurchaseResult.EQUIPPED
                }
            }
        }
        ItemType.POTION -> {
            if (gold < item.price) {
                PurchaseResult.CANNOT_AFFORD
            } else {
                gold -= item.price
                _potionCounts[item.id] = (_potionCounts[item.id] ?: 0) + 1
                PurchaseResult.BOUGHT
            }
        }
    }

    /** True if the stash holds at least one potion. */
    fun hasPotions(): Boolean = _potionCounts.values.any { it > 0 }

    /**
     * Consumes the cheapest owned potion and heals the most-wounded living party
     * member by that potion's [Item.effectValue]. Returns the potion's
     * effectValue, or 0 if no potions are held.
     */
    fun useCheapestPotion(): Int {
        val cheapest = _potionCounts.entries
            .filter { it.value > 0 }
            .mapNotNull { ItemCatalog.get(it.key) }
            .minByOrNull { it.price }
            ?: return 0

        val remaining = (_potionCounts[cheapest.id] ?: 0) - 1
        if (remaining <= 0) _potionCounts.remove(cheapest.id) else _potionCounts[cheapest.id] = remaining

        party.filter { it.isAlive }.minByOrNull { it.hp }?.heal(cheapest.effectValue)
        return cheapest.effectValue
    }
}
