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
 * Verifies the canonical shop economy: no-arg [Inventory], [BuyResult] outcomes,
 * party-wide weapon equips, potion ownership via [Inventory.count], equip state
 * via [Inventory.isEquipped], and [Inventory.useCheapestPotion].
 */
class InventoryTest {

    private fun member(id: String, hp: Int = 30, atk: Int = 5) =
        Combatant(id, id, maxHp = hp, side = Side.PLAYER, attackPower = atk)

    private fun party() = listOf(member("nib"), member("brugg"), member("vellum"))

    // ─── Construction ─────────────────────────────────────────────────────

    @Test
    fun inventoryIsCreatedWithNoArgsAndStartingGold() {
        val inv = Inventory()
        assertEquals(Inventory.STARTING_GOLD, inv.gold)
        assertEquals(50, inv.gold)
        assertFalse(inv.hasPotions())
    }

    // ─── Potions ──────────────────────────────────────────────────────────

    @Test
    fun buyingAnAffordablePotionReturnsBought() {
        val inv = Inventory()
        val result = inv.buy(ItemCatalog.MINOR_POTION)
        assertEquals(BuyResult.BOUGHT, result)
        assertEquals(30, inv.gold) // 50 - 20
        assertEquals(1, inv.count("minor_potion"))
        assertTrue(inv.hasPotions())
    }

    @Test
    fun buyingAnUnaffordableItemReturnsCannotAfford() {
        val inv = Inventory()
        val result = inv.buy(ItemCatalog.GRAND_ELIXIR) // price 160 > 50
        assertEquals(BuyResult.CANNOT_AFFORD, result)
        assertEquals(50, inv.gold) // unchanged
        assertEquals(0, inv.count("grand_elixir"))
    }

    @Test
    fun buyingTheSamePotionTwiceIncrementsCount() {
        val inv = Inventory()
        inv.grantGold(100)
        assertEquals(BuyResult.BOUGHT, inv.buy(ItemCatalog.MINOR_POTION))
        assertEquals(BuyResult.BOUGHT, inv.buy(ItemCatalog.MINOR_POTION))
        assertEquals(2, inv.count("minor_potion"))
    }

    // ─── Weapons ────────────────────────────────────────────────────────

    @Test
    fun buyingAWeaponReturnsEquippedAndMarksItEquipped() {
        val inv = Inventory()
        assertFalse(inv.isEquipped("short_sword"))
        val result = inv.buy(ItemCatalog.SHORT_SWORD) // price 40
        assertEquals(BuyResult.EQUIPPED, result)
        assertTrue(inv.isEquipped("short_sword"))
        assertEquals(10, inv.gold) // 50 - 40
    }

    @Test
    fun buyingTheSameWeaponTwiceReturnsAlreadyEquipped() {
        val inv = Inventory()
        inv.grantGold(100)
        assertEquals(BuyResult.EQUIPPED, inv.buy(ItemCatalog.SHORT_SWORD))
        val goldAfterFirst = inv.gold
        assertEquals(BuyResult.ALREADY_EQUIPPED, inv.buy(ItemCatalog.SHORT_SWORD))
        assertEquals(goldAfterFirst, inv.gold) // no extra charge
    }

    @Test
    fun weaponEquipAppliesToAllPartyMembers() {
        val inv = Inventory()
        val party = party() // each starts at attackPower 5
        assertEquals(BuyResult.EQUIPPED, inv.buy(ItemCatalog.SHORT_SWORD, party)) // +2
        party.forEach { assertEquals(7, it.attackPower) }
        assertEquals(ItemCatalog.SHORT_SWORD.effectValue, inv.equippedAttackBonus())
    }

    @Test
    fun cannotAffordWeaponLeavesPartyUnchanged() {
        val inv = Inventory()
        val party = party()
        assertEquals(BuyResult.CANNOT_AFFORD, inv.buy(ItemCatalog.WARHAMMER, party)) // price 140
        party.forEach { assertEquals(5, it.attackPower) }
        assertFalse(inv.isEquipped("warhammer"))
    }

    // ─── Potion usage ───────────────────────────────────────────────────

    @Test
    fun useCheapestPotionWithNoPotionsReturnsZeroAndHealsNothing() {
        val inv = Inventory()
        val target = member("nib", hp = 30, atk = 5)
        target.takeDamage(10) // 20 hp
        assertEquals(0, inv.useCheapestPotion(target))
        assertEquals(20, target.hp)
    }

    @Test
    fun useCheapestPotionConsumesCheapestAndHealsTarget() {
        val inv = Inventory()
        inv.grantGold(200)
        inv.buy(ItemCatalog.STANDARD_POTION) // price 45, heals 12
        inv.buy(ItemCatalog.MINOR_POTION)    // price 20, heals 5  <- cheapest

        val target = member("brugg", hp = 30, atk = 5)
        target.takeDamage(20) // 10 hp

        val healed = inv.useCheapestPotion(target)
        assertEquals(5, healed)                 // minor potion's effectValue
        assertEquals(15, target.hp)             // 10 + 5
        assertEquals(0, inv.count("minor_potion"))   // cheapest consumed
        assertEquals(1, inv.count("standard_potion")) // pricier one remains
    }

    @Test
    fun useCheapestPotionCapsHealingAtMaxHp() {
        val inv = Inventory()
        inv.buy(ItemCatalog.MINOR_POTION) // heals 5
        val target = member("vellum", hp = 18, atk = 4)
        target.takeDamage(2) // 16 hp, only 2 missing
        assertEquals(2, inv.useCheapestPotion(target)) // capped to actual missing HP
        assertEquals(18, target.hp)
    }

    // ─── Catalogue ────────────────────────────────────────────────────────

    @Test
    fun catalogueHasExactlyEightItems() {
        assertEquals(8, ItemCatalog.ALL.size)
        assertEquals(4, ItemCatalog.potions().size)
        assertEquals(4, ItemCatalog.weapons().size)
    }

    @Test
    fun getReturnsTheMatchingItem() {
        val item = ItemCatalog.get("minor_potion")
        assertNotNull(item)
        assertEquals(ItemCatalog.MINOR_POTION, item)
        assertEquals("Minor Potion", item.name)
        assertEquals(20, item.price)
    }

    @Test
    fun getReturnsNullForUnknownId() {
        assertNull(ItemCatalog.get("nonexistent"))
    }
}
