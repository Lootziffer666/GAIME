package rpg.items

import rpg.combat.Combatant

/** Outcome of an [Inventory.buy] attempt. */
enum class BuyResult {
    /** A potion was purchased and added to the stash. */
    BOUGHT,

    /** A weapon was purchased and equipped on the whole party. */
    EQUIPPED,

    /** The weapon was already equipped; nothing changed and no gold was spent. */
    ALREADY_EQUIPPED,

    /** Not enough gold; nothing changed. */
    CANNOT_AFFORD
}

/**
 * The party's gold, owned potions and equipped weapons. Pure logic with no I/O.
 *
 * Constructed with no arguments ([Inventory]) and seeded with [STARTING_GOLD].
 * Weapons, once bought, apply their attack bonus to *every* party member (the
 * canonical "equip applies to all party members" rule); potions are consumed by
 * [useCheapestPotion] to heal a chosen target.
 */
class Inventory {

    /** Current gold. Starts at [STARTING_GOLD]. */
    var gold: Int = STARTING_GOLD
        private set

    private val _potionCounts: MutableMap<String, Int> = mutableMapOf()
    private val _equippedWeapons: MutableSet<String> = mutableSetOf()

    /** Owned potions keyed by item id, with quantities (read-only snapshot). */
    val potionCounts: Map<String, Int> get() = _potionCounts.toMap()

    /** Ids of weapons currently equipped (read-only snapshot). */
    val equippedWeapons: Set<String> get() = _equippedWeapons.toSet()

    /** Adds gold (e.g. loot rewards). */
    fun grantGold(amount: Int) {
        if (amount > 0) gold += amount
    }

    /**
     * Attempts to purchase [item], applying weapon equips to every member of [party].
     *
     * - Weapons: [BuyResult.ALREADY_EQUIPPED] if already owned, [BuyResult.CANNOT_AFFORD]
     *   if too expensive, otherwise spends gold, records the weapon and raises every
     *   [party] member's `attackPower` by [Item.effectValue], returning [BuyResult.EQUIPPED].
     * - Potions: [BuyResult.CANNOT_AFFORD] if too expensive, otherwise spends gold, adds
     *   one to the stash and returns [BuyResult.BOUGHT].
     *
     * [party] defaults to empty so callers that only track economy/equip state (e.g. unit
     * tests, or wiring that applies bonuses separately) can call `buy(item)`.
     */
    fun buy(item: Item, party: List<Combatant> = emptyList()): BuyResult = when (item.type) {
        ItemType.WEAPON -> when {
            item.id in _equippedWeapons -> BuyResult.ALREADY_EQUIPPED
            gold < item.price -> BuyResult.CANNOT_AFFORD
            else -> {
                gold -= item.price
                _equippedWeapons.add(item.id)
                party.forEach { it.attackPower += item.effectValue }
                BuyResult.EQUIPPED
            }
        }
        ItemType.POTION -> if (gold < item.price) {
            BuyResult.CANNOT_AFFORD
        } else {
            gold -= item.price
            _potionCounts[item.id] = (_potionCounts[item.id] ?: 0) + 1
            BuyResult.BOUGHT
        }
    }

    /** How many of the item with [id] are owned (0 for weapons / unowned). */
    fun count(id: String): Int = _potionCounts[id] ?: 0

    /** Whether the weapon with [id] is currently equipped. */
    fun isEquipped(id: String): Boolean = id in _equippedWeapons

    /** True if the stash holds at least one potion. */
    fun hasPotions(): Boolean = _potionCounts.values.any { it > 0 }

    /** Combined attack bonus from all equipped weapons (sum of effectValue). */
    fun equippedAttackBonus(): Int =
        _equippedWeapons.mapNotNull { ItemCatalog.get(it) }.sumOf { it.effectValue }

    /**
     * Consumes the cheapest owned potion and heals [target] by that potion's
     * [Item.effectValue]. Returns the HP actually healed, or 0 if no potions are
     * held (in which case nothing is consumed).
     */
    fun useCheapestPotion(target: Combatant): Int {
        val cheapest = _potionCounts.entries
            .filter { it.value > 0 }
            .mapNotNull { ItemCatalog.get(it.key) }
            .minByOrNull { it.price }
            ?: return 0

        val remaining = (_potionCounts[cheapest.id] ?: 0) - 1
        if (remaining <= 0) _potionCounts.remove(cheapest.id) else _potionCounts[cheapest.id] = remaining

        return target.heal(cheapest.effectValue)
    }

    companion object {
        /** Gold a fresh inventory starts with. */
        const val STARTING_GOLD: Int = 50
    }
}
