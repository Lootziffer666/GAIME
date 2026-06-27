package rpg.items

import rpg.combat.Combatant
import rpg.combat.Side
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InventoryTest {

    private fun hero(hp: Int = 30, atk: Int = 5) =
        Combatant("hero", "Hero", maxHp = hp, side = Side.PLAYER, attackPower = atk)

    private val minorPotion get() = ItemCatalog.get("minor_potion")!!
    private val potion      get() = ItemCatalog.get("potion")!!
    private val shortSword  get() = ItemCatalog.get("short_sword")!!

    // ─── Gold / potions ──────────────────────────────────────────────────

    @Test
    fun inventoryStartsWithFiftyGold() {
        assertEquals(50, Inventory().gold)
    }

    @Test
    fun buyingAMinorPotionSucceedsAndDeductsGold() {
        val inv = Inventory()
        assertEquals(BuyResult.BOUGHT, inv.buy(minorPotion, emptyList()))
        assertEquals(20, inv.gold)
        assertEquals(1, inv.count("minor_potion"))
    }

    @Test
    fun buyingAMinorPotionWithTooLittleGoldFails() {
        val inv = Inventory(initialGold = 10)
        assertEquals(BuyResult.CANNOT_AFFORD, inv.buy(minorPotion, emptyList()))
        assertEquals(10, inv.gold)
        assertFalse(inv.hasPotions())
    }

    // ─── Weapons ──────────────────────────────────────────────────────────

    @Test
    fun buyingAShortSwordEquipsItAndRaisesAttackPower() {
        val fighter = hero(atk = 5)
        val inv = Inventory(initialGold = 200)
        assertEquals(BuyResult.EQUIPPED, inv.buy(shortSword, listOf(fighter)))
        assertEquals(7, fighter.attackPower) // 5 + 2
        assertTrue(inv.isEquipped("short_sword"))
    }

    @Test
    fun buyingTheSameWeaponTwiceReportsAlreadyEquipped() {
        val fighter = hero(atk = 5)
        val inv = Inventory(initialGold = 300)
        assertEquals(BuyResult.EQUIPPED, inv.buy(shortSword, listOf(fighter)))
        val goldAfterFirst = inv.gold
        assertEquals(BuyResult.ALREADY_EQUIPPED, inv.buy(shortSword, listOf(fighter)))
        assertEquals(7, fighter.attackPower) // no second bonus
        assertEquals(goldAfterFirst, inv.gold)
    }

    @Test
    fun weaponCannotBeAffordedWhenGoldInsufficient() {
        val inv = Inventory() // 50g, short_sword costs 120
        assertEquals(BuyResult.CANNOT_AFFORD, inv.buy(shortSword, listOf(hero())))
        assertEquals(50, inv.gold)
    }

    // ─── Potion usage ───────────────────────────────────────────────────

    @Test
    fun useCheapestPotionWithNoPotionsReturnsZero() {
        assertEquals(0, Inventory().useCheapestPotion(hero()))
    }

    @Test
    fun useCheapestPotionHealsAndReturnsEffectValue() {
        val wounded = hero(hp = 30)
        wounded.takeDamage(20) // now at 10 HP
        val inv = Inventory()
        inv.buy(minorPotion, emptyList())
        assertEquals(5, inv.useCheapestPotion(wounded))
        assertEquals(15, wounded.hp)
        assertFalse(inv.hasPotions())
    }

    @Test
    fun cheapestPotionIsChosenWhenSeveralAreOwned() {
        val wounded = hero(hp = 30)
        wounded.takeDamage(25) // 5 HP remaining
        val inv = Inventory(initialGold = 500)
        inv.buy(potion, emptyList())      // effectValue 15
        inv.buy(minorPotion, emptyList()) // effectValue 5 (first in catalog order)
        assertEquals(5, inv.useCheapestPotion(wounded)) // minor used first
        assertEquals(0, inv.count("minor_potion"))
        assertEquals(1, inv.count("potion"))
    }

    @Test
    fun hasPotionsReflectsStashState() {
        val inv = Inventory()
        assertFalse(inv.hasPotions())
        inv.buy(minorPotion, emptyList())
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
        assertEquals("Minor Potion", item.name)
        assertEquals(30, item.price)
        assertEquals(5, item.effectValue)
    }

    @Test
    fun getReturnsNullForUnknownId() {
        assertNull(ItemCatalog.get("nonexistent"))
    }
}
