package ui.rpg

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gaime.resources.Res
import gaime.resources.item_broadsword
import gaime.resources.item_grand_elixir
import gaime.resources.item_greater_potion
import gaime.resources.item_minor_potion
import gaime.resources.item_rusty_dagger
import gaime.resources.item_short_sword
import gaime.resources.item_standard_potion
import gaime.resources.item_warhammer
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import rpg.combat.Combatant
import rpg.items.BuyResult
import rpg.items.Inventory
import rpg.items.Item
import rpg.items.ItemCatalog
import rpg.items.ItemType

/** Maps a catalogue item id to its HD shop icon. */
private fun itemIcon(id: String): DrawableResource = when (id) {
    "minor_potion"    -> Res.drawable.item_minor_potion
    "standard_potion" -> Res.drawable.item_standard_potion
    "greater_potion"  -> Res.drawable.item_greater_potion
    "grand_elixir"    -> Res.drawable.item_grand_elixir
    "rusty_dagger"    -> Res.drawable.item_rusty_dagger
    "short_sword"     -> Res.drawable.item_short_sword
    "broadsword"      -> Res.drawable.item_broadsword
    "warhammer"       -> Res.drawable.item_warhammer
    else              -> Res.drawable.item_minor_potion
}

/**
 * The merchant's shop. A full-screen overlay backed by the canonical [Inventory]
 * and [ItemCatalog]: buy potions (added to the stash) and weapons (equipped on
 * the whole [party]). Styled to match the rest of the slice UI.
 *
 * [Inventory] is a plain (non-observable) model, so a local [version] counter is
 * bumped after each purchase to drive recomposition.
 */
@Composable
fun ShopView(
    inventory: Inventory,
    party: List<Combatant>,
    onClose: () -> Unit
) {
    var version by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable(enabled = false) {}, // swallow taps so they don't fall through
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xF0241E12),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.9f)
                .border(2.dp, Color(0xFF5A4020), RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header: title + gold (keyed on version so gold updates after buys).
                key(version) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Merchant",
                            color = Color(0xFFE8C170),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "Gold: ${inventory.gold}",
                            color = Color(0xFFFFD86B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))

                // Item list.
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    key(version) {
                        ItemCatalog.ALL.forEach { item ->
                            ShopRow(
                                item = item,
                                owned = inventory.count(item.id),
                                equipped = inventory.isEquipped(item.id),
                                affordable = inventory.gold >= item.price,
                                onBuy = {
                                    message = describe(inventory.buy(item, party), item)
                                    version++
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                message?.let {
                    Text(it, color = Color(0xFFB7E3A0), fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                }
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A4A6B)),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Leave Shop", color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }
}

private fun describe(result: BuyResult, item: Item): String = when (result) {
    BuyResult.BOUGHT           -> "Bought ${item.name}."
    BuyResult.EQUIPPED         -> "Equipped ${item.name} (+${item.effectValue} ATK to the party)."
    BuyResult.ALREADY_EQUIPPED -> "${item.name} is already equipped."
    BuyResult.CANNOT_AFFORD    -> "Not enough gold for ${item.name}."
}

@Composable
private fun ShopRow(
    item: Item,
    owned: Int,
    equipped: Boolean,
    affordable: Boolean,
    onBuy: () -> Unit
) {
    val isWeapon = item.type == ItemType.WEAPON
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x33000000), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF5A4020), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(itemIcon(item.id)),
            contentDescription = item.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0x22FFFFFF))
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, color = Color(0xFFF5E9C8), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(item.description, color = Color(0xFFB0A8D0), fontSize = 11.sp)
            val status = when {
                isWeapon && equipped -> "Equipped  (+${item.effectValue} ATK)"
                isWeapon             -> "Weapon  (+${item.effectValue} ATK to party)"
                owned > 0            -> "Owned: $owned  (heals ${item.effectValue})"
                else                 -> "Heals ${item.effectValue} HP"
            }
            Text(status, color = Color(0xFF8FB98F), fontSize = 10.sp)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text("${item.price}g", color = Color(0xFFFFD86B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            val canBuy = !(isWeapon && equipped) && affordable
            Button(
                onClick = onBuy,
                enabled = canBuy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4A6B3A),
                    disabledContainerColor = Color(0xFF3A352C)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    when {
                        isWeapon && equipped -> "Equipped"
                        isWeapon             -> "Equip"
                        else                 -> "Buy"
                    },
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }
}
