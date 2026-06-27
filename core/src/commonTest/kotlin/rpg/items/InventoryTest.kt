package rpg.items

import rpg.combat.Combatant
import rpg.combat.Side
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Covers the shop economy: buying potions and weapons, affordability and
 * already-equipped guards, potion usage/healing, and the [ItemCatalog] lookups.
 */
class InventoryTest {

    private fun hero(hp: Int = 30, atk: Int = 5) =
        Combatant("hero", "Hero", maxHp = hp, side = Side.PLAYER, attackPower = atk)

    private fun inventory(gold: Int = 50, hp: Int = 30, atk: Int = 5) =
        Inventory(party = listOf(hero(hp, atk)), gold = gold)

    // ─── Gold / potions ──────────────────────────────────────────────────

    @Test
    fun inventoryStartsWithFiftyGold() {
        assertEquals(50, inventory().gold)
    }

    @Test
    fun buyingAMinorPotionSucceedsAndDeductsGold() {
        val inv = inventory(gold = 50)
        val result = inv.buy(ItemCatalog.MINOR_POTION)
        assertEquals(PurchaseResult.BOUGHT, result)
        assertEquals(20, inv.gold) // 50 - 30
        assertEquals(1, inv.potionCounts[ItemCatalog.MINOR_POTION.id])
    }

    @Test
    fun buyingAMinorPotionWithTooLittleGoldFails() {
        val inv = inventory(gold = 10)
        val result = inv.buy(ItemCatalog.MINOR_POTION)
        assertEquals(PurchaseResult.CANNOT_AFFORD, result)
        assertEquals(10, inv.gold) // unchanged
        assertFalse(inv.hasPotions())
    }

    // ─── Weapons ──────────────────────────────────────────────────────────

    @Test
    fun buyingAShortSwordEquipsItAndRaisesAttackPower() {
        val inv = inventory(gold = 50, atk = 5)
        val result = inv.buy(ItemCatalog.SHORT_SWORD)
        assertEquals(PurchaseResult.EQUIPPED, result)
        assertEquals(7, inv.party[0].attackPower) // 5 + 2
        assertTrue(ItemCatalog.SHORT_SWORD.id in inv.equippedWeapons)
    }

    @Test
    fun buyingTheSameWeaponTwiceReportsAlreadyEquipped() {
        val inv = inventory(gold = 100, atk = 5)
        assertEquals(PurchaseResult.EQUIPPED, inv.buy(ItemCatalog.SHORT_SWORD))
        val goldAfterFirst = inv.gold
        val second = inv.buy(ItemCatalog.SHORT_SWORD)
        assertEquals(PurchaseResult.ALREADY_EQUIPPED, second)
        assertEquals(7, inv.party[0].attackPower) // no second bonus
        assertEquals(goldAfterFirst, inv.gold)    // no extra charge
    }

    // ─── Potion usage ───────────────────────────────────────────────────

    @Test
    fun useCheapestPotionWithNoPotionsReturnsZero() {
        assertEquals(0, inventory().useCheapestPotion())
    }

    @Test
    fun useCheapestPotionHealsAndReturnsEffectValue() {
        val wounded = hero(hp = 30, atk = 5)
        wounded.takeDamage(20) // now at 10 HP
        val inv = Inventory(party = listOf(wounded), gold = 50)
        inv.buy(ItemCatalog.MINOR_POTION)

        val healedReturn = inv.useCheapestPotion()
        assertEquals(5, healedReturn)        // MINOR_POTION effectValue
        assertEquals(15, wounded.hp)         // 10 + 5
        assertFalse(inv.hasPotions())        // potion consumed
    }

    @Test
    fun cheapestPotionIsChosenWhenSeveralAreOwned() {
        val inv = inventory(gold = 500)
        inv.buy(ItemCatalog.GREATER_POTION)  // price 120
        inv.buy(ItemCatalog.MINOR_POTION)    // price 30 (cheapest)
        val effect = inv.useCheapestPotion()
        assertEquals(5, effect)              // minor used first
        assertNull(inv.potionCounts[ItemCatalog.MINOR_POTION.id])
        assertEquals(1, inv.potionCounts[ItemCatalog.GREATER_POTION.id])
    }

    @Test
    fun hasPotionsReflectsStashState() {
        val inv = inventory()
        assertFalse(inv.hasPotions())
        inv.buy(ItemCatalog.MINOR_POTION)
        assertTrue(inv.hasPotions())
    }

    // ─── Catalogue ────────────────────────────────────────────────────────

    @Test
    fun catalogueHasExactlyEightItems() {
        assertEquals(8, ItemCatalog.ALL.size)
        assertEquals(4, ItemCatalog.ALL.count { it.type == ItemType.POTION })
        assertEquals(4, ItemCatalog.ALL.count { it.type == ItemType.WEAPON })
    }

    @Test
    fun getReturnsTheMatchingItem() {
        val item = ItemCatalog.get("minor_potion")
        assertNotNull(item)
        assertEquals(ItemCatalog.MINOR_POTION, item)
        assertEquals("Minor Potion", item.displayName)
        assertEquals(30, item.price)
    }

    @Test
    fun getReturnsNullForUnknownId() {
        assertNull(ItemCatalog.get("nonexistent"))
    }
}
