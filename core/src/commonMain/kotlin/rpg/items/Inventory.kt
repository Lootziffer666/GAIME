package rpg.items

import rpg.combat.Combatant

enum class BuyResult { BOUGHT, EQUIPPED, CANNOT_AFFORD, ALREADY_EQUIPPED }

/**
 * Tracks the party's gold and held items. Lives as a `remember {}` in SliceContent
 * so it persists across scene transitions but resets with the game.
 *
 * Potions stack in [counts]. Weapons are one-time equips: buying raises every
 * party member's [Combatant.attackPower] directly and records the weapon as
 * equipped so it cannot be bought again.
 *
 * Starting gold (50g) is enough for exactly one Minor Potion — intentionally
 * uncomfortable, never impossible.
 */
class Inventory(initialGold: Int = 50) {
    var gold: Int = initialGold
        private set

    private val counts = mutableMapOf<String, Int>()
    private val equipped = mutableSetOf<String>()

    fun count(itemId: String): Int = counts[itemId] ?: 0
    fun isEquipped(itemId: String): Boolean = itemId in equipped

    fun buy(item: Item, party: List<Combatant>): BuyResult {
        if (item.type == ItemType.WEAPON && item.id in equipped) return BuyResult.ALREADY_EQUIPPED
        if (gold < item.price) return BuyResult.CANNOT_AFFORD
        gold -= item.price
        return if (item.type == ItemType.WEAPON) {
            equipped += item.id
            party.forEach { it.attackPower += item.effectValue }
            BuyResult.EQUIPPED
        } else {
            counts[item.id] = (counts[item.id] ?: 0) + 1
            BuyResult.BOUGHT
        }
    }

    /**
     * Uses one potion from the stack on [target], healing by the item's
     * effectValue. Returns the amount actually healed (0 if none in stock
     * or target is at full HP).
     */
    fun usePotion(itemId: String, target: Combatant): Int {
        val have = counts[itemId] ?: 0
        if (have == 0) return 0
        val item = ItemCatalog.get(itemId) ?: return 0
        val healed = target.heal(item.effectValue)
        if (healed > 0) counts[itemId] = have - 1
        return healed
    }

    /** Uses the cheapest available potion on [target]. Returns amount healed. */
    fun useCheapestPotion(target: Combatant): Int {
        for (item in ItemCatalog.ALL.filter { it.type == ItemType.POTION }) {
            val h = usePotion(item.id, target)
            if (h > 0) return h
        }
        return 0
    }

    fun hasPotions(): Boolean =
        ItemCatalog.ALL.any { it.type == ItemType.POTION && count(it.id) > 0 }

    /**
     * Spend gold. Returns true if the player had enough gold and the amount
     * was deducted; false if insufficient (gold unchanged).
     */
    fun spend(amount: Int): Boolean {
        if (gold < amount) return false
        gold -= amount
        return true
    }

    /**
     * Steal gold from this inventory. Deducts up to [amount] (capped at current gold).
     * Returns the actual amount stolen.
     */
    fun steal(amount: Int): Int {
        val stolen = minOf(amount, gold)
        gold -= stolen
        return stolen
    }
}
