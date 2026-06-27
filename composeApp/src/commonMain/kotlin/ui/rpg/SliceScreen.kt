package ui.rpg

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gaime.resources.Res
import gaime.resources.boss_rat_accountant
import gaime.resources.enemy_blob
import gaime.resources.enemy_rat
import gaime.resources.enemy_wolf
import gaime.resources.hero_brugg
import gaime.resources.hero_nib
import gaime.resources.hero_vellum
import gaime.resources.npc_world_barkeep
import gaime.resources.npc_world_citizen1
import gaime.resources.npc_world_citizen2
import gaime.resources.npc_world_guard
import gaime.resources.npc_world_merchant
import gaime.resources.npc_world_patron
import gaime.resources.tileset_dungeon
import gaime.resources.title_screen
import gaime.resources.title_screen_portrait
import gaime.resources.questbook_open
import gaime.resources.questbook_closed
import gaime.resources.world_boss
import gaime.resources.world_bridge
import gaime.resources.world_chapel_ext
import gaime.resources.world_forest
import gaime.resources.world_glassblowers_ext
import gaime.resources.world_guildhall_ext
import gaime.resources.world_heroes_home_ext
import gaime.resources.world_market
import gaime.resources.world_sewer
import gaime.resources.world_tavern
import gaime.resources.world_temple_ext
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import rpg.BarkOutcome
import rpg.SliceDirector
import rpg.SlicePhase
import rpg.bark.BarkEvent
import rpg.bark.AmbientBarks
import rpg.bark.audio.BarkAudioPlayer
import rpg.bark.audio.createPlatformAudioPlayer
import rpg.gamepad.createControllerPoller
import rpg.i18n.GameLocale
import rpg.i18n.Locale
import rpg.i18n.localized
import rpg.combat.BossController
import rpg.combat.BossControllerInterface
import rpg.combat.CombatAction
import rpg.combat.CombatEngine
import rpg.combat.CombatEvent
import rpg.combat.CombatResult
import rpg.combat.Combatant
import rpg.combat.EnemyArchetype
import rpg.combat.Side
import rpg.combat.TaxCollectorController
import rpg.items.Inventory
import rpg.items.ItemCatalog
import rpg.items.ItemType
import rpg.questbook.QuestPressure
import rpg.questbook.QuestbookEffect
import rpg.questbook.RoomContext
import rpg.world.Direction
import rpg.world.BakedMaps
import rpg.world.GameMaps
import rpg.world.GridEntity
import rpg.world.GridEntityType
import rpg.world.GridWorld
import ui.GameCanvas
import kotlin.random.Random
import kotlin.time.TimeSource
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.abs
import kotlin.math.roundToInt

// --- Scripted dialogue lines ---

private val INTRO_LINES = listOf(
    DialogueLine("Barkeep", "Greetings, stranger.", "bark/barkeep/greetings_stranger.wav"),
    DialogueLine("Barkeep", "You've been officially registered as a Hero Party. Don't ask how."),
    DialogueLine("Nib", "...by who?"),
    DialogueLine("Barkeep", "The Questbook. It fell on the desk, opened to the right page, and made a noise like a judge swallowing a bell."),
    DialogueLine("Vellum", "Hmm, I wonder what this could be.", "bark/vellum/hmm_i_wonder_what_this_could_be.wav"),
    DialogueLine("Brugg", "A mistake."),
    DialogueLine("Barkeep", "Around here we call that paperwork."),
    DialogueLine("Nib", "So that's how it is then?", "bark/nib/so_thats_how_it_is_then.wav"),
    DialogueLine("Barkeep", "Exactly. Congratulations. You're important now, which is generally how things get worse.")
)
private val FALLING_LINES = listOf(
    DialogueLine("", "The cellar floor gives way."),
    DialogueLine("", "The party falls into the sewers below."),
    DialogueLine("Brugg", "...")
)
private val POST_BOSS_LINES = listOf(
    DialogueLine("", "The Rat Accountant has been defeated."),
    DialogueLine("", "A glowing page flutters from the remains of the filing cabinet."),
    DialogueLine("Vellum", "This... changes everything.")
)
private val RETURN_LINES = listOf(
    DialogueLine("", "The party climbs back to The Limping Cockatrice."),
    DialogueLine("Barkeep", "Back already? Smells like sewer.", "bark/barkeep/been_playing_in_the_sewers_have_we.wav"),
    DialogueLine("", "Quest Pressure: Reset. New quests pending. The Questbook is always listening.")
)

// --- Chapter 2 dialogue lines ---

private val CHAPTER2_MARKET_INTRO_LINES = listOf(
    DialogueLine("", "The party exits into the market square of Stokeport."),
    DialogueLine("Nib", "Fresh air! And fresh pockets to pick."),
    DialogueLine("Brugg", "No."),
    DialogueLine("Nib", "To observe academically."),
    DialogueLine("Vellum", "The Questbook is restless. It demands a new page."),
    DialogueLine("", "The public quest board rattles. Several notices correct themselves badly."),
    DialogueLine("Questbook", "QUEST UPDATED: LOCATE RESPONSIBLE AUTHORITY."),
    DialogueLine("Nib", "That sounds expensive."),
    DialogueLine("Brugg", "That sounds like the guard."),
    DialogueLine("Vellum", "That sounds like the beginning of a lawsuit.")
)

private val CHAPTER2_MERCHANT_LINES = listOf(
    DialogueLine("Merchant", "Greetings, friend.", "bark/merchant/greetings_friend.wav"),
    DialogueLine("Merchant", "See if any of this strikes your fancy.", "bark/merchant/see_if_any_of_this_strikes_your_fancy.wav"),
    DialogueLine("Nib", "That depends. Is any of it valuable, cursed, or recently unattended?"),
    DialogueLine("Merchant", "Make me an offer.", "bark/merchant/make_me_an_offer.wav"),
    DialogueLine("Nib", "How much do you want for this?"),
    DialogueLine("Merchant", "Name your price.", "bark/merchant/name_your_price.wav"),
    DialogueLine("Nib", "One coin."),
    DialogueLine("Merchant", "Insulting."),
    DialogueLine("Nib", "Two coins, but I say it with concern."),
    DialogueLine("Merchant", "Might I interest you in this bauble?", "bark/merchant/might_i_interest_you_in_this_bauble.wav"),
    DialogueLine("Vellum", "It glows faintly."),
    DialogueLine("Merchant", "Exactly. Premium faintness."),
    DialogueLine("Brugg", "What does it do?"),
    DialogueLine("Merchant", "It suggests importance. That is the backbone of commerce."),
    DialogueLine("Nib", "I hate how much I respect that.")
)

private val CHAPTER2_GUARD_LINES = listOf(
    DialogueLine("Guard", "Who goes there?", "bark/guard/who_goes_there.wav"),
    DialogueLine("Brugg", "Former guard. Current inconvenience."),
    DialogueLine("Guard", "Drop your weapons and surrender.", "bark/guard/drop_your_weapons_and_surrender.wav"),
    DialogueLine("Nib", "Can we surrender emotionally? My arms are tired."),
    DialogueLine("Guard", "Nothing to see here.", "bark/guard/nothing_to_see_here.wav"),
    DialogueLine("Vellum", "That phrase has never once meant that."),
    DialogueLine("Guard", "The forest trail east of here has been overrun by wolves."),
    DialogueLine("Guard", "If you're looking for trouble, you'll find it there."),
    DialogueLine("Brugg", "Just keep to the trail.", "bark/brugg/just_keep_to_the_trail.wav"),
    DialogueLine("Guard", "Good advice. Last man left the trail and came back with a mushroom calling him father."),
    DialogueLine("Nib", "Was the mushroom rich?"),
    DialogueLine("Guard", "Emotionally."),
    DialogueLine("Nib", "Useless.")
)

private val CHAPTER2_POST_BOSS_LINES = listOf(
    DialogueLine("", "The Tax Collector Badger has been defeated."),
    DialogueLine("", "A second glowing page flutters from its stamp collection."),
    DialogueLine("Vellum", "So that's how it is then.", "bark/vellum/so_thats_how_it_is_then.wav")
)

private val CHAPTER2_RETURN_LINES = listOf(
    DialogueLine("", "The party returns to Stokeport Market."),
    DialogueLine("Guard", "Back already? Been playing in the sewers, have we?", "bark/guard/been_playing_in_the_sewers_have_we.wav"),
    DialogueLine("", "Quest Pressure: Reset. Page 2 secured. The Questbook grows heavier.")
)

// --- NPC dialogue lines ---

private val BARKEEP_PRE_SEWER_LINES = listOf(
    DialogueLine("Barkeep", "Spend some coin or get out.", "bark/barkeep/spend_some_coin_or_get_out.wav"),
    DialogueLine("Nib", "I was considering a third option: standing here suspiciously."),
    DialogueLine("Barkeep", "That option costs two copper."),
    DialogueLine("Brugg", "Barkeep! A flagon of ale!"),
    DialogueLine("Barkeep", "That's nothing a flagon of ale won't fix.", "bark/barkeep/thats_nothing_a_flagon_of_ale_wont_fix.wav"),
    DialogueLine("Vellum", "There are very few civic disasters that sentence has not worsened."),
    DialogueLine("Barkeep", "You've gotta try this roast cockatrice.", "bark/barkeep/youve_gotta_try_this_roast_cockatrice.wav"),
    DialogueLine("Nib", "Is it legally food?"),
    DialogueLine("Barkeep", "Legally adjacent.")
)

private val BARKEEP_POST_SEWER_LINES = listOf(
    DialogueLine("Barkeep", "Been playing in the sewers, have we?", "bark/barkeep/been_playing_in_the_sewers_have_we.wav"),
    DialogueLine("Nib", "Playing implies consent."),
    DialogueLine("Brugg", "The floor gave way."),
    DialogueLine("Barkeep", "Happens when the city outsources maintenance to destiny."),
    DialogueLine("Vellum", "We found a page from the Questbook."),
    DialogueLine("Barkeep", "Ah. Wet, cursed, or stamped?"),
    DialogueLine("Nib", "Yes."),
    DialogueLine("Barkeep", "Then it's official.")
)

private val PATRON_LINES = listOf(
    DialogueLine("Patron", "He sure is slow for a four-armed bartender."),
    DialogueLine("Nib", "Four arms, one work ethic. Tragic ratio."),
    DialogueLine("Patron", "I heard the Questbook once registered a sneeze as a holy expedition."),
    DialogueLine("Vellum", "A common error. Many prophecies begin as respiratory events."),
    DialogueLine("Patron", "I hear the king likes to wear evening gowns."),
    DialogueLine("Brugg", "Irrelevant."),
    DialogueLine("Nib", "Not if the gowns have pockets.")
)

// --- World connector dialogue lines ---

private val HEROES_HOME_EXT_LINES = listOf(
    DialogueLine("", "The party steps outside into the morning air."),
    DialogueLine("Nib", "Fresh air. Suspicious. Usually costs extra."),
    DialogueLine("Brugg", "The guild hall is down the road."),
    DialogueLine("Vellum", "And the Questbook is still restless."),
    DialogueLine("Citizen", "Greetings, stranger.", "bark/citizen/greetings_stranger.wav"),
    DialogueLine("Citizen", "Are you the official heroes?"),
    DialogueLine("Nib", "Officially? Yes. Competently? Define your terms."),
    DialogueLine("Citizen", "The last official hero tried to rescue a bucket because someone said it had fallen."),
    DialogueLine("Brugg", "Did it need rescuing?"),
    DialogueLine("Citizen", "It was a bucket."),
    DialogueLine("Vellum", "A classic ambiguity. Container, victim, or metaphor."),
    DialogueLine("Nib", "If the bucket had treasure, I support its recovery.")
)

private val GUILDMASTER_LINES = listOf(
    DialogueLine("Guildmaster", "Greetings, friends.", "bark/guildmaster/greetings_friends.wav"),
    DialogueLine("Guildmaster", "Registered heroes may pick up contracts at the board inside."),
    DialogueLine("Guildmaster", "Non-registered adventurers are asked to leave or be fined."),
    DialogueLine("Nib", "We're registered. The Questbook said so."),
    DialogueLine("Guildmaster", "The Questbook also once registered a sandwich as missing royalty."),
    DialogueLine("Brugg", "Was it?"),
    DialogueLine("Guildmaster", "No. But it had a crown-shaped bite mark, and the law is weaker than presentation."),
    DialogueLine("Vellum", "Knowledge is the answer.", "bark/guildmaster/knowledge_is_the_answer.wav"),
    DialogueLine("Guildmaster", "Experience is how we grow.", "bark/guildmaster/experience_is_how_we_grow.wav"),
    DialogueLine("Nib", "Pain is how we invoice."),
    DialogueLine("Guildmaster", "Grab your torch. There's work to be done.", "bark/guildmaster/grab_your_torch_theres_work_to_be_done.wav")
)

private val CHAPEL_DEVOTEE_LINES = listOf(
    DialogueLine("Citizen", "Our prayers will be answered.", "bark/citizen/our_prayers_will_be_answered.wav"),
    DialogueLine("Nib", "That's either comforting or a threat."),
    DialogueLine("Citizen", "The chapel has been... quiet lately. Too quiet."),
    DialogueLine("Citizen", "This is unusual.", "bark/citizen/this_is_unusual.wav"),
    DialogueLine("Vellum", "Quiet chapels are rarely quiet for affordable reasons."),
    DialogueLine("Citizen", "Something moved the pews. Something large."),
    DialogueLine("Brugg", "Animal?"),
    DialogueLine("Citizen", "No. It organized them alphabetically by guilt."),
    DialogueLine("Nib", "I don't like furniture with judgment."),
    DialogueLine("Citizen", "I need to speak to the town guard.", "bark/citizen/i_need_to_speak_to_the_town_guard.wav"),
    DialogueLine("Brugg", "The town guard is currently arresting itself."),
    DialogueLine("Citizen", "Again?"),
    DialogueLine("Vellum", "Perfect. Let's go in.")
)

private val TEMPLE_EXT_INTRO_LINES = listOf(
    DialogueLine("", "The ruined temple exterior. Overgrown. Unsettled."),
    DialogueLine("Brugg", "Wolves."),
    DialogueLine("Nib", "Lots of wolves."),
    DialogueLine("Vellum", "And markings on the stones."),
    DialogueLine("Nib", "Do the markings say treasure?"),
    DialogueLine("Vellum", "They say warning."),
    DialogueLine("Nib", "Ancient people were terrible at marketing."),
    DialogueLine("Guard", "There are all manner of creatures within these woods.", "bark/guard/there_are_all_manner_of_creatures_within_these_woods.wav"),
    DialogueLine("Brugg", "Look sharp."),
    DialogueLine("Nib", "I prefer looking profitable."),
    DialogueLine("Vellum", "The deeper we go, the darker it gets."),
    DialogueLine("Nib", "That is how depth works."),
    DialogueLine("Vellum", "Not always spiritually."),
    DialogueLine("Nib", "Especially spiritually.")
)

// --- New scene dialogue lines ---

private val CHAPEL_INTERIOR_LINES = listOf(
    DialogueLine("Priest", "Blessings upon you, travelers.", "bark/priest/blessings_upon_you.wav"),
    DialogueLine("Nib", "Are they refundable?"),
    DialogueLine("Priest", "The chapel does not sell blessings."),
    DialogueLine("Nib", "That sounds exactly like something said before a donation box."),
    DialogueLine("Priest", "We must live by the teachings of holy wisdom.", "bark/priest/we_must_live_by_the_teachings_of_holy_wisdom.wav"),
    DialogueLine("Vellum", "Holy wisdom is rarely the issue. The issue is usually the people who laminate it."),
    DialogueLine("Priest", "The Questbook passed through here once."),
    DialogueLine("Brugg", "When?"),
    DialogueLine("Priest", "Before the pews began confessing."),
    DialogueLine("Nib", "Furniture shouldn't have interiority."),
    DialogueLine("Priest", "It heard the words 'save all souls' and began sorting parishioners by theological compliance."),
    DialogueLine("Vellum", "A machine cannot understand mercy."),
    DialogueLine("Priest", "No. But it understands categories."),
    DialogueLine("Questbook", "QUEST NOTED: REVIEW SOUL CLASSIFICATION."),
    DialogueLine("Brugg", "No."),
    DialogueLine("Nib", "Good. The book is getting religious. That always ends in architecture.")
)

private val CHAPTER2_GUILDHALL_INTERIOR_LINES = listOf(
    DialogueLine("Mage", "Of all the arcane lore...", "bark/mage/of_all_the_arcane_lore.wav"),
    DialogueLine("Nib", "That is a dangerous way to start a sentence."),
    DialogueLine("Mage", "The Questbook is not cursed."),
    DialogueLine("Brugg", "It drops stamps from the ceiling."),
    DialogueLine("Mage", "That is civic enchantment. Different smell."),
    DialogueLine("Vellum", "Knowledge is the answer.", "bark/vellum/knowledge_is_the_answer.wav"),
    DialogueLine("Mage", "Sometimes. Sometimes knowledge is the door, the trap, and the idiot holding the torch."),
    DialogueLine("Nib", "Which one am I?"),
    DialogueLine("Mage", "You are the reason we label torches."),
    DialogueLine("Mage", "Now what was that incantation?", "bark/mage/now_what_was_that_incantation.wav"),
    DialogueLine("Mage", "Ah. The Questbook reacts to declared meaning, not truth."),
    DialogueLine("Brugg", "Plain words."),
    DialogueLine("Mage", "If someone says a thing like it matters, the book tries to make it matter."),
    DialogueLine("Nib", "So confidence is dangerous."),
    DialogueLine("Mage", "Historically, yes."),
    DialogueLine("Questbook", "QUEST UPDATED: SEEK NEXT PAGE."),
    DialogueLine("Mage", "There. It did it again. Self-important little library.")
)

private val CHAPTER3_BOSS_MEDUSA_INTRO_LINES = listOf(
    DialogueLine("", "The forest opens into a stone clearing. Statues stand in neat rows, each frozen mid-regret."),
    DialogueLine("Nib", "Those are decorative, right?"),
    DialogueLine("Brugg", "No."),
    DialogueLine("Vellum", "The balance of life and death sits on a knife's edge."),
    DialogueLine("Nib", "I preferred decorative."),
    DialogueLine("Medusa", "Another party. Another prophecy with boots."),
    DialogueLine("Brugg", "Stand aside."),
    DialogueLine("Medusa", "I stood aside once. A hero called it destiny and built a statue park out of my afternoon."),
    DialogueLine("Vellum", "We do not seek violence."),
    DialogueLine("Medusa", "No one ever does. They seek glory, answers, treasure, closure, content. Violence is the delivery method."),
    DialogueLine("Nib", "I only seek treasure."),
    DialogueLine("Medusa", "Honest. Repulsive, but honest."),
    DialogueLine("Questbook", "QUEST ACCEPTED: DEFEAT THE MONSTER."),
    DialogueLine("Medusa", "There it is. The little book sees a woman with snakes and files paperwork for murder."),
    DialogueLine("Brugg", "Then we break the paperwork."),
    DialogueLine("Vellum", "Or at least misfile it.")
)

private val CHAPTER3_BOSS_MEDUSA_POST_LINES = listOf(
    DialogueLine("", "The stone light fades. The statues remain statues, but somehow look less blamed."),
    DialogueLine("Medusa", "You did not kill me."),
    DialogueLine("Brugg", "Wasn't ordered."),
    DialogueLine("Nib", "Also, unclear loot table."),
    DialogueLine("Vellum", "The Questbook named you monster because the story needed one."),
    DialogueLine("Medusa", "Stories always need monsters. It saves them from needing context."),
    DialogueLine("Questbook", "QUEST COMPLETE: MONSTER ENCOUNTER RESOLVED."),
    DialogueLine("Nib", "Resolved is doing a lot of work there."),
    DialogueLine("Medusa", "Take your page. And if the book asks who the villain was, tell it to look in a mirror."),
    DialogueLine("Vellum", "Hard-won knowledge.", "bark/vellum/hard-won_knowledge.wav"),
    DialogueLine("", "The party receives a torn Questbook page, warm as if recently embarrassed.")
)

private val VILLAGE_ELDER_LINES = listOf(
    DialogueLine("Elder", "Well met.", "bark/guildmaster/well_met.wav"),
    DialogueLine("Nib", "That depends on whether this becomes unpaid advice."),
    DialogueLine("Elder", "The Questbook was built after the old wars, when heroes became too expensive and prophecies too vague."),
    DialogueLine("Brugg", "So the city automated courage."),
    DialogueLine("Elder", "The city automated liability."),
    DialogueLine("Vellum", "Knowledge is the answer.", "bark/vellum/knowledge_is_the_answer.wav"),
    DialogueLine("Elder", "Knowledge was the first draft. Then came committees."),
    DialogueLine("Nib", "And nobody stopped them?"),
    DialogueLine("Elder", "Everyone thought it would help the little man."),
    DialogueLine("Brugg", "Did it?"),
    DialogueLine("Elder", "It helped the little man fill out forms explaining why help was unavailable."),
    DialogueLine("Nib", "That sounds like government."),
    DialogueLine("Elder", "That sounds like everything, eventually."),
    DialogueLine("Elder", "Remember this: the Questbook does not hate people."),
    DialogueLine("Elder", "It merely turns their needs into obligations."),
    DialogueLine("Vellum", "That may be worse.")
)

private val CHAPTER2_BRIDGE_LINES = listOf(
    DialogueLine("", "A narrow bridge crosses the muddy river east of Stokeport."),
    DialogueLine("Citizen", "Greetings, stranger.", "bark/citizen/greetings_stranger.wav"),
    DialogueLine("Citizen", "Careful crossing. The bridge has been asking travelers for purpose."),
    DialogueLine("Nib", "Bridges should ask for tolls. Like honest criminals."),
    DialogueLine("Citizen", "A man said he was 'just passing through.' The Questbook marked him as temporary infrastructure."),
    DialogueLine("Brugg", "Where is he?"),
    DialogueLine("Citizen", "Second plank from the left."),
    DialogueLine("Vellum", "This is unusual.", "bark/citizen/this_is_unusual.wav"),
    DialogueLine("Nib", "No, this is Stokeport."),
    DialogueLine("Citizen", "If you hear singing under the bridge, do not answer."),
    DialogueLine("Nib", "Is it a troll?"),
    DialogueLine("Citizen", "Worse. A bard with unresolved metaphor."),
    DialogueLine("Questbook", "QUEST NOTED: INSPECT BRIDGE PURPOSE."),
    DialogueLine("Brugg", "Ignore it."),
    DialogueLine("Nib", "I love how that has become our strategy.")
)

private val CHAPTER1_OUTRO_LINES = listOf(
    DialogueLine("", "The first page settles into the Questbook with a damp administrative sigh."),
    DialogueLine("Brugg", "Objective complete.", "bark/brugg/grab_your_torch_theres_work_to_be_done.wav"),
    DialogueLine("Nib", "Do we get paid?"),
    DialogueLine("Vellum", "We learned something."),
    DialogueLine("Nib", "That is not currency."),
    DialogueLine("Questbook", "QUEST UPDATED: REPORT TO CIVIC AUTHORITY."),
    DialogueLine("Brugg", "Town guard."),
    DialogueLine("Nib", "Nothing good starts with those two words.")
)

private val CHAPTER2_OUTRO_LINES = listOf(
    DialogueLine("", "The guardhouse falls quiet. Several laws stop contradicting themselves out loud."),
    DialogueLine("Brugg", "Order restored."),
    DialogueLine("Nib", "Order lightly embarrassed."),
    DialogueLine("Vellum", "The Questbook is learning the shape of authority."),
    DialogueLine("Nib", "Does it have to?"),
    DialogueLine("Questbook", "QUEST UPDATED: FOLLOW THE FOREST TRAIL."),
    DialogueLine("Brugg", "Just keep to the trail.", "bark/brugg/just_keep_to_the_trail.wav"),
    DialogueLine("Nib", "The trail has never met us. It should be afraid.")
)

private val CHAPTER3_OUTRO_LINES = listOf(
    DialogueLine("", "The forest stops rearranging itself, which somehow feels judgmental."),
    DialogueLine("Vellum", "The page is real."),
    DialogueLine("Nib", "The tree was too helpful. I feel violated by advice."),
    DialogueLine("Brugg", "We ignored the correct path."),
    DialogueLine("Vellum", "And therefore found the right one."),
    DialogueLine("Nib", "I hate when wisdom sounds like budget design."),
    DialogueLine("Questbook", "QUEST COMPLETE: DIRECTIONS ACQUIRED."),
    DialogueLine("Nib", "Good. Can we acquire lunch?")
)

// --- Room contexts ---

private val TAVERN_CTX = RoomContext("tavern", RoomContext.ROOM_TAVERN, hasInteractableTarget = true)
private val MARKET_CTX = RoomContext("stokeport_market", RoomContext.ROOM_MARKET, hasInteractableTarget = true)
private val FOREST_CTX = RoomContext("forest_trail", RoomContext.ROOM_FOREST, hasEnemies = true, hasPuzzleElement = true)

// --- Idle bark selection ---
// (Ambient/exploration/idle bark pools now live in rpg.bark.AmbientBarks so the
//  selection logic is testable with a seeded Random — see review issue #3.)

private val SEWER_CTX = RoomContext(
    "sewer", RoomContext.ROOM_MINI_DUNGEON,
    hasEnemies = true, hasPuzzleElement = true, hasBreakableObstacle = true
)
private val BOSS_CTX = RoomContext("sewer", RoomContext.ROOM_BOSS, hasFlammableTarget = true)

// --- Party template ---

private fun freshSliceParty(): List<Combatant> = listOf(
    Combatant("nib", "Nib", maxHp = 20, side = Side.PLAYER, attackPower = 4),
    Combatant("brugg", "Brugg", maxHp = 30, side = Side.PLAYER, attackPower = 5),
    Combatant("vellum", "Vellum", maxHp = 18, side = Side.PLAYER, attackPower = 4)
)

// ---

@Composable
fun SliceScreen() {
    var resetKey by remember { mutableStateOf(0) }
    val timeStart = remember { TimeSource.Monotonic.markNow() }
    key(resetKey) {
        SliceContent(
            clock = { timeStart.elapsedNow().inWholeMilliseconds },
            onReset = { resetKey++ }
        )
    }
}

@Composable
private fun SliceContent(clock: () -> Long, onReset: () -> Unit) {
    // Resources
    val tileset = imageResource(Res.drawable.tileset_dungeon)
    val playerSprite = imageResource(Res.drawable.hero_nib)
    val enemyRatImg = imageResource(Res.drawable.enemy_rat)
    val enemyBlobImg = imageResource(Res.drawable.enemy_blob)
    val enemyWolfImg = imageResource(Res.drawable.enemy_wolf)
    val bossRatImg = imageResource(Res.drawable.boss_rat_accountant)
    val tavernBg        = imageResource(Res.drawable.world_tavern)
    val sewerBg         = imageResource(Res.drawable.world_sewer)
    val bossBg          = imageResource(Res.drawable.world_boss)
    val marketBg        = imageResource(Res.drawable.world_market)
    val forestBg        = imageResource(Res.drawable.world_forest)
    val heroesHomeExtBg = imageResource(Res.drawable.world_heroes_home_ext)
    val guildHallExtBg  = imageResource(Res.drawable.world_guildhall_ext)
    val chapelExtBg     = imageResource(Res.drawable.world_chapel_ext)
    val templeExtBg     = imageResource(Res.drawable.world_temple_ext)
    val glassblowersExtBg = imageResource(Res.drawable.world_glassblowers_ext)
    val bridgeBg        = imageResource(Res.drawable.world_bridge)
    val npcBarkeepImg   = imageResource(Res.drawable.npc_world_barkeep)
    val npcPatronImg    = imageResource(Res.drawable.npc_world_patron)
    val npcMerchantImg  = imageResource(Res.drawable.npc_world_merchant)
    val npcGuardImg     = imageResource(Res.drawable.npc_world_guard)
    val npcCitizen1Img  = imageResource(Res.drawable.npc_world_citizen1)
    val npcCitizen2Img  = imageResource(Res.drawable.npc_world_citizen2)
    val spriteMap = remember(enemyRatImg, enemyBlobImg, enemyWolfImg, bossRatImg,
                             npcBarkeepImg, npcPatronImg, npcMerchantImg, npcGuardImg,
                             npcCitizen1Img, npcCitizen2Img) {
        mapOf(
            "enemy_rat"          to enemyRatImg,
            "enemy_blob"         to enemyBlobImg,
            "enemy_wolf"         to enemyWolfImg,
            "boss_rat_accountant" to bossRatImg,
            "npc_barkeep"        to npcBarkeepImg,
            "npc_patron"         to npcPatronImg,
            "npc_merchant"       to npcMerchantImg,
            "npc_guard"          to npcGuardImg,
            "npc_citizen1"       to npcCitizen1Img,
            "npc_citizen2"       to npcCitizen2Img
        )
    }

    // Phase + narrative state
    var phase by remember { mutableStateOf(SlicePhase.TITLE_SCREEN) }
    var dialogueLines by remember { mutableStateOf(INTRO_LINES) }
    var dialogueIndex by remember { mutableStateOf(0) }
    var flashText by remember { mutableStateOf<String?>(null) }
    var combatMessage by remember { mutableStateOf("") }
    var version by remember { mutableStateOf(0) }
    var hasReturnedFromSewer by remember { mutableStateOf(false) }
    var chapter2Complete by remember { mutableStateOf(false) }
    var shrineActivated by remember { mutableStateOf(false) }

    // Engine + persistent party (HP carries across encounters)
    val director = remember { SliceDirector(clock) }
    val party = remember { freshSliceParty() }
    val inventory = remember { Inventory() }

    // Wire up bark audio playback
    val barkAudioPlayer = remember { BarkAudioPlayer(createPlatformAudioPlayer()) }
    DisposableEffect(Unit) {
        director.barkAudioPlayer = barkAudioPlayer
        onDispose {
            barkAudioPlayer.release()
            director.barkAudioPlayer = null
        }
    }

    // Idle bark timer state
    var lastActivityTime by remember { mutableStateOf(clock()) }
    // Long-lived RNG for ambient/exploration/idle bark selection
    val barkRandom = remember { Random.Default }

    // Worlds
    val tavernWorld = remember {
        GridWorld(BakedMaps.tavern()).also { w ->
            w.entities.add(GridEntity("barkeep",  BakedMaps.TAVERN_BARKEEP_X, BakedMaps.TAVERN_BARKEEP_Y, GridEntityType.NPC, "npc_barkeep"))
            w.entities.add(GridEntity("patron",   BakedMaps.TAVERN_PATRON_X,  BakedMaps.TAVERN_PATRON_Y,  GridEntityType.NPC, "npc_patron"))
            w.entities.add(GridEntity("citizen1",  5, 9, GridEntityType.NPC, "npc_citizen1"))
            w.entities.add(GridEntity("citizen2", 18, 11, GridEntityType.NPC, "npc_citizen2"))
        }
    }
    val sewerWorld = remember {
        GridWorld(GameMaps.sewer()).also { w ->
            w.entities.addAll(listOf(
                GridEntity("rat_corridor_1", 6, 5, GridEntityType.ENEMY, "enemy_rat",  maxHp = 1),
                GridEntity("rat_corridor_2", 8, 5, GridEntityType.ENEMY, "enemy_rat",  maxHp = 1),
                GridEntity("rat_mini_1", 5, 13, GridEntityType.ENEMY, "enemy_rat",     maxHp = 1),
                GridEntity("rat_mini_2", 7, 13, GridEntityType.ENEMY, "enemy_rat",     maxHp = 1),
                GridEntity("rat_mini_3", 9, 13, GridEntityType.ENEMY, "enemy_rat",     maxHp = 1),
                GridEntity("blob_mini", 7, 15, GridEntityType.ENEMY, "enemy_blob",     maxHp = 2),
                // Destructibles
                GridEntity("barrel_1",    3,  4, GridEntityType.DESTRUCTIBLE, "barrel",       maxHp = 1),
                GridEntity("barrel_2",   10,  3, GridEntityType.DESTRUCTIBLE, "barrel",       maxHp = 1),
                GridEntity("crate_1",     4,  8, GridEntityType.DESTRUCTIBLE, "crate",        maxHp = 1),
                GridEntity("wall_1",     11,  7, GridEntityType.DESTRUCTIBLE, "wall_cracked", maxHp = 2),
                GridEntity("wall_2",     11, 14, GridEntityType.DESTRUCTIBLE, "wall_cracked", maxHp = 2)
            ))
        }
    }
    val bossWorld = remember {
        GridWorld(GameMaps.bossRoom()).also { w ->
            w.entities.add(GridEntity("rat_accountant", 6, 7, GridEntityType.ENEMY, "boss_rat_accountant"))
            // Atmosphere props
            w.entities.add(GridEntity("boss_barrel_1", 3, 4, GridEntityType.DESTRUCTIBLE, "barrel", maxHp = 1))
            w.entities.add(GridEntity("boss_barrel_2", 9, 4, GridEntityType.DESTRUCTIBLE, "barrel", maxHp = 1))
        }
    }

    // Chapter 2 worlds
    val marketWorld = remember {
        GridWorld(GameMaps.stokeportMarket()).also { w ->
            w.entities.add(GridEntity("merchant",  12, 10, GridEntityType.NPC, "npc_merchant"))
            w.entities.add(GridEntity("guard",      6, 15, GridEntityType.NPC, "npc_guard"))
            w.entities.add(GridEntity("citizen1",  10,  7, GridEntityType.NPC, "npc_citizen1"))
            // Market clutter
            w.entities.add(GridEntity("market_crate_1",  4,  5, GridEntityType.DESTRUCTIBLE, "crate",  maxHp = 1))
            w.entities.add(GridEntity("market_crate_2", 14,  5, GridEntityType.DESTRUCTIBLE, "crate",  maxHp = 1))
            w.entities.add(GridEntity("market_barrel_1", 8,  4, GridEntityType.DESTRUCTIBLE, "barrel", maxHp = 1))
            w.entities.add(GridEntity("market_barrel_2",16, 12, GridEntityType.DESTRUCTIBLE, "barrel", maxHp = 1))
        }
    }
    val forestWorld = remember {
        GridWorld(GameMaps.forestTrail()).also { w ->
            w.entities.addAll(listOf(
                GridEntity("wolf_1", 16, 8, GridEntityType.ENEMY, "enemy_wolf",          maxHp = 2),
                GridEntity("wolf_2", 18, 11, GridEntityType.ENEMY, "enemy_wolf",         maxHp = 2),
                GridEntity("wolf_3", 20, 9, GridEntityType.ENEMY, "enemy_wolf",          maxHp = 2),
                GridEntity("tax_badger", 24, 18, GridEntityType.ENEMY, "boss_rat_accountant"), // maxHp=-1: boss → CombatEngine
                // Tall grass patches (non-solid: stepped on to destroy)
                GridEntity("grass_1", 14, 7, GridEntityType.DESTRUCTIBLE, "grass_tall", maxHp = 1, solid = false),
                GridEntity("grass_2", 15, 9, GridEntityType.DESTRUCTIBLE, "grass_tall", maxHp = 1, solid = false),
                GridEntity("grass_3", 17, 10, GridEntityType.DESTRUCTIBLE, "grass_tall", maxHp = 1, solid = false),
                GridEntity("grass_4", 19, 7, GridEntityType.DESTRUCTIBLE, "grass_tall", maxHp = 1, solid = false),
                GridEntity("grass_5", 21, 10, GridEntityType.DESTRUCTIBLE, "grass_tall", maxHp = 1, solid = false),
                // Fallen logs / crumbling ruins
                GridEntity("forest_crate_1", 13, 10, GridEntityType.DESTRUCTIBLE, "crate", maxHp = 1)
            ))
        }
    }

    // World connector worlds (open exterior locations)
    val heroesHomeExtWorld = remember {
        GridWorld(GameMaps.heroesHomeExt()).also { w ->
            w.entities.add(GridEntity("villager1", 6, 6, GridEntityType.NPC, "npc_citizen1"))
            w.entities.add(GridEntity("villager2", 14, 8, GridEntityType.NPC, "npc_citizen2"))
            w.entities.add(GridEntity("merchant",  5, 10, GridEntityType.NPC, "npc_merchant"))
        }
    }
    val guildHallExtWorld = remember {
        GridWorld(GameMaps.guildHallExt()).also { w ->
            w.entities.add(GridEntity("guildmaster",  7, 4, GridEntityType.NPC, "npc_guard"))
            w.entities.add(GridEntity("guard_post",   3, 7, GridEntityType.NPC, "npc_guard"))
            w.entities.add(GridEntity("citizen1",    12, 6, GridEntityType.NPC, "npc_citizen1"))
        }
    }
    val chapelExtWorld = remember {
        GridWorld(GameMaps.chapelExt()).also { w ->
            w.entities.add(GridEntity("chapel_guard",  10, 8, GridEntityType.NPC, "npc_guard"))
            w.entities.add(GridEntity("devotee1",       5, 10, GridEntityType.NPC, "npc_citizen1"))
            w.entities.add(GridEntity("devotee2",      16, 11, GridEntityType.NPC, "npc_citizen2"))
        }
    }
    val templeExtWorld = remember {
        GridWorld(GameMaps.templeExt()).also { w ->
            w.entities.add(GridEntity("wolf_a", 8, 6, GridEntityType.ENEMY, "enemy_wolf", maxHp = 2))
            w.entities.add(GridEntity("wolf_b", 13, 4, GridEntityType.ENEMY, "enemy_wolf", maxHp = 2))
            // Ruined stonework
            w.entities.add(GridEntity("temple_wall_1",  5, 5, GridEntityType.DESTRUCTIBLE, "wall_cracked", maxHp = 2))
            w.entities.add(GridEntity("temple_wall_2", 10, 7, GridEntityType.DESTRUCTIBLE, "wall_cracked", maxHp = 2))
            w.entities.add(GridEntity("temple_grass_1",  7, 8, GridEntityType.DESTRUCTIBLE, "grass_tall", maxHp = 1, solid = false))
            w.entities.add(GridEntity("temple_grass_2", 11, 5, GridEntityType.DESTRUCTIBLE, "grass_tall", maxHp = 1, solid = false))
        }
    }
    val glassblowersExtWorld = remember {
        GridWorld(GameMaps.glassblowersExt()).also { w ->
            w.entities.add(GridEntity("glassblower",  8, 4, GridEntityType.NPC, "npc_merchant"))
            w.entities.add(GridEntity("apprentice",  13, 6, GridEntityType.NPC, "npc_citizen1"))
        }
    }
    val bridgeWorld = remember {
        GridWorld(GameMaps.bridge()).also { w ->
            w.entities.add(GridEntity("traveller1", 18, 5, GridEntityType.NPC, "npc_citizen1"))
            w.entities.add(GridEntity("traveller2", 26, 9, GridEntityType.NPC, "npc_citizen2"))
        }
    }

    // Scenes (created once per resource load; callbacks re-assigned each recomposition)
    val tavernScene        = remember(tileset, playerSprite, tavernBg)        { WorldScene(tavernWorld,        tileset, playerSprite, background = tavernBg) }
    val sewerScene         = remember(tileset, playerSprite, sewerBg)         { WorldScene(sewerWorld,         tileset, playerSprite, background = sewerBg) }
    val bossScene          = remember(tileset, playerSprite, bossBg)          { WorldScene(bossWorld,          tileset, playerSprite, background = bossBg) }
    val marketScene        = remember(tileset, playerSprite, marketBg)        { WorldScene(marketWorld,        tileset, playerSprite, background = marketBg) }
    val forestScene        = remember(tileset, playerSprite, forestBg)        { WorldScene(forestWorld,        tileset, playerSprite, background = forestBg) }
    val heroesHomeExtScene = remember(tileset, playerSprite, heroesHomeExtBg) { WorldScene(heroesHomeExtWorld, tileset, playerSprite, background = heroesHomeExtBg) }
    val guildHallExtScene  = remember(tileset, playerSprite, guildHallExtBg)  { WorldScene(guildHallExtWorld,  tileset, playerSprite, background = guildHallExtBg) }
    val chapelExtScene     = remember(tileset, playerSprite, chapelExtBg)     { WorldScene(chapelExtWorld,     tileset, playerSprite, background = chapelExtBg) }
    val templeExtScene     = remember(tileset, playerSprite, templeExtBg)     { WorldScene(templeExtWorld,     tileset, playerSprite, background = templeExtBg) }
    val glassblowersExtScene = remember(tileset, playerSprite, glassblowersExtBg) { WorldScene(glassblowersExtWorld, tileset, playerSprite, background = glassblowersExtBg) }
    val bridgeScene        = remember(tileset, playerSprite, bridgeBg)        { WorldScene(bridgeWorld,        tileset, playerSprite, background = bridgeBg) }

    // Keep sprite maps current
    tavernScene.spriteMap        = spriteMap
    sewerScene.spriteMap         = spriteMap
    bossScene.spriteMap          = spriteMap
    marketScene.spriteMap        = spriteMap
    forestScene.spriteMap        = spriteMap
    heroesHomeExtScene.spriteMap = spriteMap
    guildHallExtScene.spriteMap  = spriteMap
    chapelExtScene.spriteMap     = spriteMap
    templeExtScene.spriteMap     = spriteMap
    glassblowersExtScene.spriteMap = spriteMap
    bridgeScene.spriteMap        = spriteMap

    // Cinematic atmosphere preset per map (lighting, motes, grade, fog) — the
    // "Odd Tales / The Last Night" real-time layer on top of the pixel art.
    tavernScene.atmosphere        = SceneAtmosphere.TAVERN
    sewerScene.atmosphere         = SceneAtmosphere.SEWER
    bossScene.atmosphere          = SceneAtmosphere.CHAPEL   // chapel interior = gothic candle atmosphere
    marketScene.atmosphere        = SceneAtmosphere.MARKET
    forestScene.atmosphere        = SceneAtmosphere.FOREST
    heroesHomeExtScene.atmosphere = SceneAtmosphere.MARKET   // bright open village
    guildHallExtScene.atmosphere  = SceneAtmosphere.GUILD_HALL
    chapelExtScene.atmosphere     = SceneAtmosphere.CHAPEL
    templeExtScene.atmosphere     = SceneAtmosphere.FOREST
    glassblowersExtScene.atmosphere = SceneAtmosphere.MARKET
    bridgeScene.atmosphere        = SceneAtmosphere.BRIDGE

    // --- helpers ---

    fun fireAndFlash(bark: BarkEvent) {
        lastActivityTime = clock()
        when (val out = director.fireBark(bark)) {
            is BarkOutcome.Fired    -> { flashText = out.reaction.questbookText; version++ }
            is BarkOutcome.Suppressed -> {}
        }
    }

    LaunchedEffect(flashText) {
        if (flashText != null) { delay(2800); flashText = null }
    }

    // Idle bark timer: fire a random ambient bark after 30s of inactivity
    LaunchedEffect(phase) {
        while (true) {
            delay(1000)
            val isExplorationPhase = phase in listOf(
                SlicePhase.TAVERN, SlicePhase.SEWER, SlicePhase.BOSS_ROOM,
                SlicePhase.CHAPTER2_MARKET, SlicePhase.CHAPTER2_FOREST, SlicePhase.CHAPTER2_SHRINE,
                SlicePhase.HEROES_HOME_EXT, SlicePhase.CHAPTER2_GUILDHALL,
                SlicePhase.CHAPTER2_CHAPEL_EXT, SlicePhase.CHAPTER2_TEMPLE_EXT,
                SlicePhase.CHAPTER2_BRIDGE, SlicePhase.CHAPTER2_GLASSBLOWERS
            )
            if (!isExplorationPhase) continue
            val elapsed = clock() - lastActivityTime
            if (elapsed >= 30_000L) {
                val idleBark = AmbientBarks.pickIdle(phase, barkRandom)
                if (idleBark != null) {
                    fireAndFlash(idleBark)
                    lastActivityTime = clock()
                }
            }
        }
    }

    // --- dialogue / phase transitions ---

    fun advanceDialogue() {
        lastActivityTime = clock()
        val isLast = dialogueIndex >= dialogueLines.size - 1
        if (!isLast) {
            dialogueIndex++
            // FALLING_CUTSCENE: Brugg's line (index 2) fires bark then player auto-transitions
            if (phase == SlicePhase.FALLING_CUTSCENE && dialogueIndex == 2) {
                fireAndFlash(BarkEvent.BRUGG_THAT_WASNT_SO_BAD)
            }
        } else {
            when (phase) {
                SlicePhase.INTRO_CUTSCENE -> {
                    director.enterRoom(TAVERN_CTX)
                    phase = SlicePhase.TAVERN
                }
                SlicePhase.FALLING_CUTSCENE -> phase = SlicePhase.SEWER
                SlicePhase.POST_BOSS        -> phase = SlicePhase.QUESTBOOK_FULL
                SlicePhase.RETURN_CUTSCENE  -> {
                    hasReturnedFromSewer = true
                    // Transition to Chapter 2 market instead of ending
                    director.enterRoom(MARKET_CTX)
                    fireAndFlash(BarkEvent.NIB_SMELL_GOLD)
                    dialogueLines = CHAPTER2_MARKET_INTRO_LINES
                    dialogueIndex = 0
                    phase = SlicePhase.CHAPTER2_MARKET_NPC
                }
                SlicePhase.NPC_DIALOGUE     -> phase = SlicePhase.TAVERN
                SlicePhase.CHAPTER2_MARKET_NPC -> phase = SlicePhase.CHAPTER2_MARKET
                SlicePhase.CHAPTER2_BOSS_INTRO -> {
                    director.startCombat(CombatEngine(
                        party, emptyList(),
                        EnemyArchetype.TAX_COLLECTOR_BADGER.spawn("tax_badger"),
                        TaxCollectorController()
                    ))
                    combatMessage = "The Tax Collector Badger demands payment!"
                    phase = SlicePhase.CHAPTER2_BOSS_COMBAT
                }
                SlicePhase.CHAPTER2_POST_BOSS -> phase = SlicePhase.CHAPTER2_QUESTBOOK_PAGE2
                SlicePhase.CHAPTER2_RETURN -> {
                    chapter2Complete = true
                    phase = SlicePhase.VICTORY
                }
                else -> {}
            }
        }
    }

    // --- controller / gamepad input ---

    val controllerPoller = remember { createControllerPoller() }
    DisposableEffect(Unit) { onDispose { controllerPoller?.release() } }

    LaunchedEffect(Unit) {
        val poller = controllerPoller ?: return@LaunchedEffect
        while (true) {
            delay(100)
            if (!poller.poll()) continue

            val explorationWorld = when (phase) {
                SlicePhase.TAVERN                -> tavernWorld
                SlicePhase.SEWER                 -> sewerWorld
                SlicePhase.BOSS_ROOM             -> bossWorld
                SlicePhase.CHAPTER2_MARKET       -> marketWorld
                SlicePhase.CHAPTER2_FOREST,
                SlicePhase.CHAPTER2_SHRINE       -> forestWorld
                SlicePhase.HEROES_HOME_EXT       -> heroesHomeExtWorld
                SlicePhase.CHAPTER2_GUILDHALL    -> guildHallExtWorld
                SlicePhase.CHAPTER2_CHAPEL_EXT   -> chapelExtWorld
                SlicePhase.CHAPTER2_TEMPLE_EXT   -> templeExtWorld
                SlicePhase.CHAPTER2_GLASSBLOWERS -> glassblowersExtWorld
                SlicePhase.CHAPTER2_BRIDGE       -> bridgeWorld
                else -> null
            }

            if (explorationWorld != null) {
                // Movement
                val dir = poller.direction()
                if (dir != null) {
                    lastActivityTime = clock()
                    explorationWorld.requestStep(dir)
                    version++
                }
                // Attack (west face button / Z key)
                if (poller.consumeAttack()) {
                    lastActivityTime = clock()
                    explorationWorld.requestAttack()
                    version++
                }
                // Interact (south face button)
                if (poller.consumeInteract()) {
                    lastActivityTime = clock()
                    val npc = explorationWorld.requestInteraction()
                    if (npc != null) {
                        when (phase) {
                            SlicePhase.TAVERN                -> tavernScene.onEntityInteraction?.invoke(npc)
                            SlicePhase.CHAPTER2_MARKET       -> marketScene.onEntityInteraction?.invoke(npc)
                            SlicePhase.CHAPTER2_FOREST       -> forestScene.onEntityInteraction?.invoke(npc)
                            SlicePhase.HEROES_HOME_EXT       -> heroesHomeExtScene.onEntityInteraction?.invoke(npc)
                            SlicePhase.CHAPTER2_GUILDHALL    -> guildHallExtScene.onEntityInteraction?.invoke(npc)
                            SlicePhase.CHAPTER2_CHAPEL_EXT   -> chapelExtScene.onEntityInteraction?.invoke(npc)
                            SlicePhase.CHAPTER2_TEMPLE_EXT   -> templeExtScene.onEntityInteraction?.invoke(npc)
                            SlicePhase.CHAPTER2_GLASSBLOWERS -> glassblowersExtScene.onEntityInteraction?.invoke(npc)
                            SlicePhase.CHAPTER2_BRIDGE       -> bridgeScene.onEntityInteraction?.invoke(npc)
                            else -> {}
                        }
                        version++
                    }
                }
            } else {
                // In cutscene / dialogue: south button advances dialogue
                val isDialogue = phase in listOf(
                    SlicePhase.INTRO_CUTSCENE, SlicePhase.FALLING_CUTSCENE,
                    SlicePhase.NPC_DIALOGUE, SlicePhase.CHAPTER2_MARKET_NPC,
                    SlicePhase.POST_BOSS, SlicePhase.RETURN_CUTSCENE,
                    SlicePhase.CHAPTER2_BOSS_INTRO, SlicePhase.CHAPTER2_POST_BOSS,
                    SlicePhase.CHAPTER2_RETURN
                )
                if (isDialogue && poller.consumeInteract()) advanceDialogue()
            }
        }
    }

    // --- combat ---

    fun handleCombatAction(action: CombatAction) {
        lastActivityTime = clock()
        val turn = director.combatAction(action)
        turn.events.filterIsInstance<CombatEvent.Message>().lastOrNull()?.let { combatMessage = it.text }
        turn.events.filterIsInstance<CombatEvent.BarkTriggered>().lastOrNull()?.let {
            director.questbook.log.lastOrNull()?.let { r -> flashText = r.questbookText }
        }
        version++

        when (turn.result) {
            CombatResult.VICTORY -> when (phase) {
                SlicePhase.BOSS_COMBAT -> {
                    bossWorld.removeEntity("rat_accountant")
                    director.clearCombat()
                    fireAndFlash(BarkEvent.VELLUM_THIS_CHANGES_EVERYTHING)
                    dialogueLines = POST_BOSS_LINES
                    dialogueIndex = 0
                    phase = SlicePhase.POST_BOSS
                }
                SlicePhase.CHAPTER2_BOSS_COMBAT -> {
                    forestWorld.removeEntity("tax_badger")
                    director.clearCombat()
                    fireAndFlash(BarkEvent.BRUGG_EXPERIENCE_IS_HOW_WE_GROW)
                    dialogueLines = CHAPTER2_POST_BOSS_LINES
                    dialogueIndex = 0
                    phase = SlicePhase.CHAPTER2_POST_BOSS
                }
                else -> {}
            }
            CombatResult.DEFEAT  -> phase = SlicePhase.GAME_OVER
            CombatResult.ONGOING -> {}
        }
    }

    // --- scene callbacks (re-assigned each recomposition for fresh phase reference) ---

    tavernScene.onTrigger = { id ->
        if (id == GameMaps.TRIGGER_CELLAR_DOOR && phase == SlicePhase.TAVERN) {
            lastActivityTime = clock()
            fireAndFlash(BarkEvent.NIB_SMELL_TREASURE)
            fireAndFlash(BarkEvent.NIB_THIS_CHEST_UNLOCKED)
            director.enterRoom(SEWER_CTX)
            // Exploration bark: entering the sewers
            AmbientBarks.pick(AmbientBarks.SEWER_ENTRY, barkRandom)?.let { fireAndFlash(it) }
            phase = SlicePhase.FALLING_CUTSCENE
            dialogueLines = FALLING_LINES
            dialogueIndex = 0
        }
    }
    tavernScene.onEntityInteraction = { entity ->
        if (phase == SlicePhase.TAVERN) {
            lastActivityTime = clock()
            when (entity.id) {
                "barkeep" -> {
                    dialogueLines = if (hasReturnedFromSewer) BARKEEP_POST_SEWER_LINES else BARKEEP_PRE_SEWER_LINES
                    dialogueIndex = 0
                    phase = SlicePhase.NPC_DIALOGUE
                }
                "patron" -> {
                    dialogueLines = PATRON_LINES
                    dialogueIndex = 0
                    phase = SlicePhase.NPC_DIALOGUE
                }
            }
        }
    }

    sewerScene.onTrigger = { id ->
        if (phase == SlicePhase.SEWER) {
            lastActivityTime = clock()
            when (id) {
                GameMaps.TRIGGER_VELLUM_PUZZLE -> fireAndFlash(BarkEvent.VELLUM_KNOWLEDGE_IS_THE_ANSWER)
                GameMaps.TRIGGER_SEWER_EXIT    -> {
                    // Exploration atmosphere bark on entering boss room
                    AmbientBarks.pick(AmbientBarks.SEWER_ATMOSPHERE, barkRandom)?.let { fireAndFlash(it) }
                    director.enterRoom(BOSS_CTX)
                    phase = SlicePhase.BOSS_ROOM
                }
            }
        }
    }
    // Sewer rats and blob are action-combat enemies (maxHp > 0): they block tiles and
    // die via requestAttack() — no CombatEngine interaction is ever queued for them.
    sewerScene.onEntityInteraction = { _ -> }

    bossScene.onTrigger = { _ -> /* page_pickup handled programmatically via post-boss flow */ }
    bossScene.onEntityInteraction = { entity ->
        if (phase == SlicePhase.BOSS_ROOM && entity.id == "rat_accountant") {
            lastActivityTime = clock()
            // Exploration bark: discovering the boss
            AmbientBarks.pick(AmbientBarks.BOSS_DISCOVERY, barkRandom)?.let { fireAndFlash(it) }
            director.startCombat(CombatEngine(party, emptyList(), EnemyArchetype.RAT_ACCOUNTANT.spawn("rat_accountant"), BossController()))
            combatMessage = "The Rat Accountant looks up from its desk of garbage."
            phase = SlicePhase.BOSS_COMBAT
        }
    }

    // Chapter 2: Market scene callbacks
    marketScene.onTrigger = { id ->
        if (phase == SlicePhase.CHAPTER2_MARKET) {
            lastActivityTime = clock()
            when (id) {
                GameMaps.TRIGGER_MARKET_EXIT -> {
                    director.enterRoom(FOREST_CTX)
                    fireAndFlash(BarkEvent.VELLUM_CREATURES_IN_WOODS)
                    phase = SlicePhase.CHAPTER2_FOREST
                }
            }
        }
    }
    marketScene.onEntityInteraction = { entity ->
        if (phase == SlicePhase.CHAPTER2_MARKET) {
            lastActivityTime = clock()
            when (entity.id) {
                "merchant" -> {
                    fireAndFlash(BarkEvent.NIB_SMELL_GOLD)
                    phase = SlicePhase.SHOP
                }
                "guard" -> {
                    fireAndFlash(BarkEvent.BRUGG_SPEAK_TO_GUARD)
                    dialogueLines = CHAPTER2_GUARD_LINES
                    dialogueIndex = 0
                    phase = SlicePhase.CHAPTER2_MARKET_NPC
                }
            }
        }
    }

    // Chapter 2: Forest scene callbacks
    forestScene.onTrigger = { id ->
        if (phase == SlicePhase.CHAPTER2_FOREST) {
            lastActivityTime = clock()
            when (id) {
                GameMaps.TRIGGER_FOREST_SHRINE -> {
                    director.enterRoom(RoomContext("forest_trail", RoomContext.ROOM_FOREST_SHRINE, hasPuzzleElement = true))
                    fireAndFlash(BarkEvent.VELLUM_ELEMENTS_MINE_TO_COMMAND)
                    phase = SlicePhase.CHAPTER2_SHRINE
                }
                GameMaps.TRIGGER_FOREST_BOSS -> {
                    director.enterRoom(RoomContext("forest_trail", RoomContext.ROOM_FOREST_BOSS))
                    fireAndFlash(BarkEvent.BRUGG_DROP_YOUR_WEAPONS)
                    dialogueLines = listOf(
                        DialogueLine("", "A massive badger in a waistcoat blocks the path."),
                        DialogueLine("Tax Collector", "You owe 47 outstanding quest fees. Pay up or face audit."),
                        DialogueLine("Brugg", "Drop your weapons!", "bark/brugg/drop_your_weapons.wav")
                    )
                    dialogueIndex = 0
                    phase = SlicePhase.CHAPTER2_BOSS_INTRO
                }
            }
        }
    }
    // Wolves are action-combat enemies; only the boss badger routes to CombatEngine.
    forestScene.onEntityInteraction = { entity ->
        if (phase == SlicePhase.CHAPTER2_FOREST && entity.id == "tax_badger") {
            lastActivityTime = clock()
            director.enterRoom(RoomContext("forest_trail", RoomContext.ROOM_FOREST_BOSS))
            fireAndFlash(BarkEvent.BRUGG_DROP_YOUR_WEAPONS)
            dialogueLines = listOf(
                DialogueLine("", "A massive badger in a waistcoat blocks the path."),
                DialogueLine("Tax Collector", "You owe 47 outstanding quest fees. Pay up or face audit."),
                DialogueLine("Brugg", "Drop your weapons!", "bark/brugg/drop_your_weapons.wav")
            )
            dialogueIndex = 0
            phase = SlicePhase.CHAPTER2_BOSS_INTRO
        }
    }

    // --- World connector scene callbacks ---

    heroesHomeExtScene.onTrigger = { id ->
        if (phase == SlicePhase.HEROES_HOME_EXT && id == GameMaps.TRIGGER_VILLAGE_ENTER) {
            lastActivityTime = clock()
            director.enterRoom(TAVERN_CTX)
            phase = SlicePhase.INTRO_CUTSCENE
            dialogueLines = INTRO_LINES
            dialogueIndex = 0
        }
    }
    heroesHomeExtScene.onEntityInteraction = { entity ->
        if (phase == SlicePhase.HEROES_HOME_EXT) {
            lastActivityTime = clock()
            dialogueLines = HEROES_HOME_EXT_LINES
            dialogueIndex = 0
            phase = SlicePhase.NPC_DIALOGUE
        }
    }

    guildHallExtScene.onTrigger = { _ -> }
    guildHallExtScene.onEntityInteraction = { entity ->
        if (phase == SlicePhase.CHAPTER2_GUILDHALL) {
            lastActivityTime = clock()
            when (entity.id) {
                "guildmaster" -> {
                    dialogueLines = GUILDMASTER_LINES
                    dialogueIndex = 0
                    phase = SlicePhase.CHAPTER2_MARKET_NPC
                }
                else -> {
                    dialogueLines = HEROES_HOME_EXT_LINES
                    dialogueIndex = 0
                    phase = SlicePhase.CHAPTER2_MARKET_NPC
                }
            }
        }
    }

    chapelExtScene.onTrigger = { id ->
        if (phase == SlicePhase.CHAPTER2_CHAPEL_EXT && id == GameMaps.TRIGGER_CHAPEL_ENTER) {
            lastActivityTime = clock()
            director.enterRoom(BOSS_CTX)
            phase = SlicePhase.BOSS_ROOM
        }
    }
    chapelExtScene.onEntityInteraction = { _ ->
        if (phase == SlicePhase.CHAPTER2_CHAPEL_EXT) {
            lastActivityTime = clock()
            dialogueLines = CHAPEL_DEVOTEE_LINES
            dialogueIndex = 0
            phase = SlicePhase.CHAPTER2_MARKET_NPC
        }
    }

    templeExtScene.onTrigger = { id ->
        if (phase == SlicePhase.CHAPTER2_TEMPLE_EXT && id == GameMaps.TRIGGER_TEMPLE_ENTER) {
            lastActivityTime = clock()
            director.enterRoom(FOREST_CTX)
            phase = SlicePhase.CHAPTER2_FOREST
        }
    }
    // Temple wolves are action-combat enemies — killed via requestAttack().
    templeExtScene.onEntityInteraction = { _ -> }

    glassblowersExtScene.onTrigger = { _ -> }
    glassblowersExtScene.onEntityInteraction = { _ ->
        if (phase == SlicePhase.CHAPTER2_GLASSBLOWERS) {
            lastActivityTime = clock()
            dialogueLines = CHAPTER2_MERCHANT_LINES
            dialogueIndex = 0
            phase = SlicePhase.CHAPTER2_MARKET_NPC
        }
    }

    bridgeScene.onTrigger = { id ->
        if (phase == SlicePhase.CHAPTER2_BRIDGE) {
            lastActivityTime = clock()
            when (id) {
                GameMaps.TRIGGER_BRIDGE_EAST -> phase = SlicePhase.CHAPTER2_MARKET
                GameMaps.TRIGGER_BRIDGE_WEST -> phase = SlicePhase.HEROES_HOME_EXT
            }
        }
    }
    bridgeScene.onEntityInteraction = { _ ->
        if (phase == SlicePhase.CHAPTER2_BRIDGE) {
            lastActivityTime = clock()
            dialogueLines = CHAPTER2_BRIDGE_LINES
            dialogueIndex = 0
            phase = SlicePhase.CHAPTER2_MARKET_NPC
        }
    }

    // --- keyboard shortcut for exploration phases ---

    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    @Suppress("UNUSED_EXPRESSION") version

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF15131F))
            .focusRequester(focus)
            .focusable()
            .onKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                lastActivityTime = clock()
                // Escape closes the shop overlay
                if (e.key == Key.Escape && phase == SlicePhase.SHOP) {
                    phase = SlicePhase.CHAPTER2_MARKET
                    return@onKeyEvent true
                }
                val world = when (phase) {
                    SlicePhase.TAVERN               -> tavernWorld
                    SlicePhase.SEWER                -> sewerWorld
                    SlicePhase.BOSS_ROOM            -> bossWorld
                    SlicePhase.CHAPTER2_MARKET      -> marketWorld
                    SlicePhase.CHAPTER2_FOREST,
                    SlicePhase.CHAPTER2_SHRINE      -> forestWorld
                    SlicePhase.HEROES_HOME_EXT      -> heroesHomeExtWorld
                    SlicePhase.CHAPTER2_GUILDHALL   -> guildHallExtWorld
                    SlicePhase.CHAPTER2_CHAPEL_EXT  -> chapelExtWorld
                    SlicePhase.CHAPTER2_TEMPLE_EXT  -> templeExtWorld
                    SlicePhase.CHAPTER2_GLASSBLOWERS -> glassblowersExtWorld
                    SlicePhase.CHAPTER2_BRIDGE      -> bridgeWorld
                    else -> null
                } ?: return@onKeyEvent false
                when (e.key) {
                    Key.W, Key.DirectionUp    -> world.requestStep(Direction.UP)
                    Key.S, Key.DirectionDown  -> world.requestStep(Direction.DOWN)
                    Key.A, Key.DirectionLeft  -> world.requestStep(Direction.LEFT)
                    Key.D, Key.DirectionRight -> world.requestStep(Direction.RIGHT)
                    Key.E -> {
                        val npc = world.requestInteraction()
                        if (npc != null) {
                            when (phase) {
                                SlicePhase.TAVERN               -> tavernScene.onEntityInteraction?.invoke(npc)
                                SlicePhase.CHAPTER2_MARKET      -> marketScene.onEntityInteraction?.invoke(npc)
                                SlicePhase.CHAPTER2_FOREST      -> forestScene.onEntityInteraction?.invoke(npc)
                                SlicePhase.HEROES_HOME_EXT      -> heroesHomeExtScene.onEntityInteraction?.invoke(npc)
                                SlicePhase.CHAPTER2_GUILDHALL   -> guildHallExtScene.onEntityInteraction?.invoke(npc)
                                SlicePhase.CHAPTER2_CHAPEL_EXT  -> chapelExtScene.onEntityInteraction?.invoke(npc)
                                SlicePhase.CHAPTER2_TEMPLE_EXT  -> templeExtScene.onEntityInteraction?.invoke(npc)
                                SlicePhase.CHAPTER2_GLASSBLOWERS -> glassblowersExtScene.onEntityInteraction?.invoke(npc)
                                SlicePhase.CHAPTER2_BRIDGE       -> bridgeScene.onEntityInteraction?.invoke(npc)
                                else -> {}
                            }
                        }
                        return@onKeyEvent npc != null
                    }
                    Key.Z -> { world.requestAttack(); version++; return@onKeyEvent true }
                    Key.I -> {
                        party.firstOrNull()?.let { nib ->
                            if (inventory.useCheapestPotion(nib) > 0) version++
                        }
                        return@onKeyEvent true
                    }
                    else -> return@onKeyEvent false
                }
                true
            }
    ) {
        when (phase) {
            // --- Title screen ---
            SlicePhase.TITLE_SCREEN ->
                TitleView { phase = SlicePhase.INTRO_CUTSCENE }

            // --- Cutscenes ---
            SlicePhase.INTRO_CUTSCENE,
            SlicePhase.FALLING_CUTSCENE,
            SlicePhase.POST_BOSS,
            SlicePhase.RETURN_CUTSCENE ->
                DialogueOverlay(
                    lines = dialogueLines,
                    currentIndex = dialogueIndex,
                    onAdvance = ::advanceDialogue,
                    barkAudioPlayer = barkAudioPlayer
                )

            // --- Exploration ---
            SlicePhase.TAVERN ->
                ExploreView(
                    title    = "The Limping Cockatrice",
                    scene    = tavernScene,
                    pressure = director.pressure,
                    falseMarkers = director.falseMarkers,
                    onStep   = tavernWorld::requestStep,
                    barkButtons = listOf(BarkEvent.NIB_SMELL_TREASURE to "Nib: Smell Treasure"),
                    onBark     = ::fireAndFlash,
                    onAttack   = { tavernWorld.requestAttack(); version++ },
                    onInteract = { tavernWorld.requestInteraction()?.let { tavernScene.onEntityInteraction?.invoke(it); version++ } },
                    onPotion   = { party.firstOrNull()?.let { if (inventory.useCheapestPotion(it) > 0) version++ } }
                )

            SlicePhase.NPC_DIALOGUE -> {
                ExploreView(
                    title    = "The Limping Cockatrice",
                    scene    = tavernScene,
                    pressure = director.pressure,
                    falseMarkers = director.falseMarkers,
                    onStep   = tavernWorld::requestStep,
                    barkButtons = emptyList(),
                    onBark   = ::fireAndFlash
                )
                DialogueOverlay(
                    lines = dialogueLines,
                    currentIndex = dialogueIndex,
                    onAdvance = ::advanceDialogue,
                    barkAudioPlayer = barkAudioPlayer
                )
            }

            SlicePhase.SEWER ->
                ExploreView(
                    title    = "Sewers of Bad Decisions",
                    scene    = sewerScene,
                    pressure = director.pressure,
                    falseMarkers = director.falseMarkers,
                    onStep   = sewerWorld::requestStep,
                    barkButtons = listOf(
                        BarkEvent.BRUGG_ATTACK        to "Brugg: Attack!",
                        BarkEvent.VELLUM_CALLS_FOR_FLAME to "Vellum: Flame"
                    ),
                    onBark = { bark ->
                        lastActivityTime = clock()
                        if (bark == BarkEvent.BRUGG_ATTACK) {
                            // Fire a loot bark before clearing the rubble
                            val lootBarks = listOf(
                                BarkEvent.NIB_OOO_ANOTHER_BARREL,
                                BarkEvent.BRUGG_OOO_ANOTHER_BARREL,
                                BarkEvent.NIB_I_WONDER_WHATS_IN_THIS_ONE,
                                BarkEvent.VELLUM_I_WONDER_WHATS_IN_THIS_ONE
                            )
                            fireAndFlash(lootBarks[Random.nextInt(lootBarks.size)])
                            when (val out = director.fireBark(bark)) {
                                is BarkOutcome.Fired -> {
                                    flashText = out.reaction.questbookText
                                    if (out.reaction.effect is QuestbookEffect.ClearObstacle) {
                                        sewerWorld.clearObstacle(GameMaps.TRIGGER_RUBBLE)
                                    }
                                    version++
                                }
                                is BarkOutcome.Suppressed -> {}
                            }
                        } else {
                            fireAndFlash(bark)
                        }
                    },
                    onAttack   = { sewerWorld.requestAttack(); version++ },
                    onInteract = { sewerWorld.requestInteraction()?.let { sewerScene.onEntityInteraction?.invoke(it); version++ } },
                    onPotion   = { party.firstOrNull()?.let { if (inventory.useCheapestPotion(it) > 0) version++ } }
                )

            SlicePhase.BOSS_ROOM ->
                ExploreView(
                    title    = "The Rat Accountant's Office",
                    scene    = bossScene,
                    pressure = director.pressure,
                    falseMarkers = director.falseMarkers,
                    onStep   = bossWorld::requestStep,
                    barkButtons = listOf(BarkEvent.VELLUM_CALLS_FOR_FLAME to "Vellum: Flame"),
                    onBark     = ::fireAndFlash,
                    onAttack   = { bossWorld.requestAttack(); version++ },
                    onInteract = { bossWorld.requestInteraction()?.let { bossScene.onEntityInteraction?.invoke(it); version++ } },
                    onPotion   = { party.firstOrNull()?.let { if (inventory.useCheapestPotion(it) > 0) version++ } }
                )

            // --- Combat (boss encounters only — regular enemies use action combat) ---
            SlicePhase.BOSS_COMBAT ->
                CombatView(director = director, message = combatMessage, onAction = ::handleCombatAction)

            // These combat phases are no longer reachable (action enemies never queue interactions)
            // but must be listed to satisfy the exhaustive when over SlicePhase.
            SlicePhase.SEWER_COMBAT,
            SlicePhase.MINI_DUNGEON_COMBAT,
            SlicePhase.CHAPTER2_FOREST_COMBAT -> {}

            // --- Questbook full overlay ---
            SlicePhase.QUESTBOOK_FULL ->
                QuestbookFullView(partyName = director.partyName ?: "Everything Changes") {
                    dialogueLines = RETURN_LINES
                    dialogueIndex = 0
                    phase = SlicePhase.RETURN_CUTSCENE
                }

            // --- End states ---
            SlicePhase.VICTORY ->
                EndView(
                    title    = if (chapter2Complete) "Quest Status: Fully Resolved." else "Quest Status: Resolved.",
                    subtitle = "Party: ${director.partyName ?: "Unknown"}",
                    color    = Color(0xFF7FD17F),
                    onRestart = onReset
                )

            // --- Chapter 2 phases ---
            SlicePhase.CHAPTER2_MARKET ->
                ExploreView(
                    title    = "Stokeport Market",
                    scene    = marketScene,
                    pressure = director.pressure,
                    falseMarkers = director.falseMarkers,
                    onStep   = marketWorld::requestStep,
                    barkButtons = listOf(BarkEvent.NIB_SMELL_GOLD to "Nib: Smell Gold"),
                    onBark     = ::fireAndFlash,
                    onAttack   = { marketWorld.requestAttack(); version++ },
                    onInteract = { marketWorld.requestInteraction()?.let { marketScene.onEntityInteraction?.invoke(it); version++ } },
                    onPotion   = { party.firstOrNull()?.let { if (inventory.useCheapestPotion(it) > 0) version++ } }
                )

            SlicePhase.CHAPTER2_MARKET_NPC -> {
                ExploreView(
                    title    = "Stokeport Market",
                    scene    = marketScene,
                    pressure = director.pressure,
                    falseMarkers = director.falseMarkers,
                    onStep   = marketWorld::requestStep,
                    barkButtons = emptyList(),
                    onBark   = ::fireAndFlash
                )
                DialogueOverlay(
                    lines = dialogueLines,
                    currentIndex = dialogueIndex,
                    onAdvance = ::advanceDialogue,
                    barkAudioPlayer = barkAudioPlayer
                )
            }

            SlicePhase.CHAPTER2_FOREST ->
                ExploreView(
                    title    = "The Forest Trail",
                    scene    = forestScene,
                    pressure = director.pressure,
                    falseMarkers = director.falseMarkers,
                    onStep   = forestWorld::requestStep,
                    barkButtons = listOf(BarkEvent.BRUGG_ATTACK to "Brugg: Attack!"),
                    onBark     = ::fireAndFlash,
                    onAttack   = { forestWorld.requestAttack(); version++ },
                    onInteract = { forestWorld.requestInteraction()?.let { forestScene.onEntityInteraction?.invoke(it); version++ } },
                    onPotion   = { party.firstOrNull()?.let { if (inventory.useCheapestPotion(it) > 0) version++ } }
                )

            SlicePhase.CHAPTER2_SHRINE ->
                ExploreView(
                    title    = "Ancient Shrine",
                    scene    = forestScene,
                    pressure = director.pressure,
                    falseMarkers = director.falseMarkers,
                    onStep   = forestWorld::requestStep,
                    barkButtons = listOf(BarkEvent.VELLUM_CALLS_FOR_LIGHTNING to "Vellum: Lightning"),
                    onBark   = { bark ->
                        lastActivityTime = clock()
                        if (bark == BarkEvent.VELLUM_CALLS_FOR_LIGHTNING && !shrineActivated) {
                            shrineActivated = true
                            fireAndFlash(bark)
                            director.enterRoom(FOREST_CTX)
                            phase = SlicePhase.CHAPTER2_FOREST
                        } else {
                            fireAndFlash(bark)
                        }
                    },
                    onAttack   = { forestWorld.requestAttack(); version++ },
                    onInteract = { forestWorld.requestInteraction()?.let { forestScene.onEntityInteraction?.invoke(it); version++ } },
                    onPotion   = { party.firstOrNull()?.let { if (inventory.useCheapestPotion(it) > 0) version++ } }
                )

            SlicePhase.CHAPTER2_BOSS_INTRO ->
                DialogueOverlay(
                    lines = dialogueLines,
                    currentIndex = dialogueIndex,
                    onAdvance = ::advanceDialogue,
                    barkAudioPlayer = barkAudioPlayer
                )

            SlicePhase.CHAPTER2_BOSS_COMBAT ->
                CombatView(director = director, message = combatMessage, onAction = ::handleCombatAction)

            SlicePhase.CHAPTER2_POST_BOSS ->
                DialogueOverlay(
                    lines = dialogueLines,
                    currentIndex = dialogueIndex,
                    onAdvance = ::advanceDialogue,
                    barkAudioPlayer = barkAudioPlayer
                )

            SlicePhase.CHAPTER2_QUESTBOOK_PAGE2 ->
                QuestbookFullView(partyName = "Outstanding Quest Balance: 47.\nPayment: Additional heroism (non-negotiable)") {
                    fireAndFlash(BarkEvent.GUARD_BACK_ALREADY)
                    dialogueLines = CHAPTER2_RETURN_LINES
                    dialogueIndex = 0
                    phase = SlicePhase.CHAPTER2_RETURN
                }

            SlicePhase.CHAPTER2_RETURN ->
                DialogueOverlay(
                    lines = dialogueLines,
                    currentIndex = dialogueIndex,
                    onAdvance = ::advanceDialogue,
                    barkAudioPlayer = barkAudioPlayer
                )

            SlicePhase.GAME_OVER ->
                EndView(
                    title    = "Quest Status: Unresolved.",
                    subtitle = "The Questbook notes your failure for administrative purposes.",
                    color    = Color(0xFFE53935),
                    onRestart = onReset
                )

            // --- World connectors ---
            SlicePhase.HEROES_HOME_EXT ->
                ExploreView(
                    title    = "Village of Hearthwick",
                    scene    = heroesHomeExtScene,
                    pressure = director.pressure,
                    falseMarkers = director.falseMarkers,
                    onStep     = heroesHomeExtWorld::requestStep,
                    barkButtons = emptyList(),
                    onBark     = ::fireAndFlash,
                    onAttack   = { heroesHomeExtWorld.requestAttack(); version++ },
                    onInteract = { heroesHomeExtWorld.requestInteraction()?.let { heroesHomeExtScene.onEntityInteraction?.invoke(it); version++ } },
                    onPotion   = { party.firstOrNull()?.let { if (inventory.useCheapestPotion(it) > 0) version++ } }
                )

            SlicePhase.CHAPTER2_GUILDHALL ->
                ExploreView(
                    title    = "Adventurers' Guild",
                    scene    = guildHallExtScene,
                    pressure = director.pressure,
                    falseMarkers = director.falseMarkers,
                    onStep     = guildHallExtWorld::requestStep,
                    barkButtons = listOf(BarkEvent.NIB_SMELL_GOLD to "Nib: Contracts"),
                    onBark     = ::fireAndFlash,
                    onAttack   = { guildHallExtWorld.requestAttack(); version++ },
                    onInteract = { guildHallExtWorld.requestInteraction()?.let { guildHallExtScene.onEntityInteraction?.invoke(it); version++ } },
                    onPotion   = { party.firstOrNull()?.let { if (inventory.useCheapestPotion(it) > 0) version++ } }
                )

            SlicePhase.CHAPTER2_CHAPEL_EXT ->
                ExploreView(
                    title    = "Chapel of the Unresolved",
                    scene    = chapelExtScene,
                    pressure = director.pressure,
                    falseMarkers = director.falseMarkers,
                    onStep     = chapelExtWorld::requestStep,
                    barkButtons = listOf(BarkEvent.VELLUM_CALLS_FOR_FLAME to "Vellum: Sense"),
                    onBark     = ::fireAndFlash,
                    onAttack   = { chapelExtWorld.requestAttack(); version++ },
                    onInteract = { chapelExtWorld.requestInteraction()?.let { chapelExtScene.onEntityInteraction?.invoke(it); version++ } },
                    onPotion   = { party.firstOrNull()?.let { if (inventory.useCheapestPotion(it) > 0) version++ } }
                )

            SlicePhase.CHAPTER2_TEMPLE_EXT ->
                ExploreView(
                    title    = "Ruined Temple Approach",
                    scene    = templeExtScene,
                    pressure = director.pressure,
                    falseMarkers = director.falseMarkers,
                    onStep     = templeExtWorld::requestStep,
                    barkButtons = listOf(BarkEvent.BRUGG_ATTACK to "Brugg: Attack!"),
                    onBark     = ::fireAndFlash,
                    onAttack   = { templeExtWorld.requestAttack(); version++ },
                    onInteract = { templeExtWorld.requestInteraction()?.let { templeExtScene.onEntityInteraction?.invoke(it); version++ } },
                    onPotion   = { party.firstOrNull()?.let { if (inventory.useCheapestPotion(it) > 0) version++ } }
                )

            SlicePhase.CHAPTER2_GLASSBLOWERS ->
                ExploreView(
                    title    = "Glassblowers' District",
                    scene    = glassblowersExtScene,
                    pressure = director.pressure,
                    falseMarkers = director.falseMarkers,
                    onStep     = glassblowersExtWorld::requestStep,
                    barkButtons = listOf(BarkEvent.NIB_SMELL_GOLD to "Nib: Browse"),
                    onBark     = ::fireAndFlash,
                    onAttack   = { glassblowersExtWorld.requestAttack(); version++ },
                    onInteract = { glassblowersExtWorld.requestInteraction()?.let { glassblowersExtScene.onEntityInteraction?.invoke(it); version++ } },
                    onPotion   = { party.firstOrNull()?.let { if (inventory.useCheapestPotion(it) > 0) version++ } }
                )

            SlicePhase.CHAPTER2_BRIDGE ->
                ExploreView(
                    title    = "The Ironway Bridge",
                    scene    = bridgeScene,
                    pressure = director.pressure,
                    falseMarkers = director.falseMarkers,
                    onStep     = bridgeWorld::requestStep,
                    barkButtons = emptyList(),
                    onBark     = ::fireAndFlash,
                    onAttack   = { bridgeWorld.requestAttack(); version++ },
                    onInteract = { bridgeWorld.requestInteraction()?.let { bridgeScene.onEntityInteraction?.invoke(it); version++ } },
                    onPotion   = { party.firstOrNull()?.let { if (inventory.useCheapestPotion(it) > 0) version++ } }
                )

            SlicePhase.SHOP -> {
                ExploreView(
                    title    = "Stokeport Market",
                    scene    = marketScene,
                    pressure = director.pressure,
                    falseMarkers = director.falseMarkers,
                    onStep   = marketWorld::requestStep,
                    barkButtons = emptyList(),
                    onBark   = ::fireAndFlash
                )
                ShopView(
                    inventory = inventory,
                    party     = party,
                    version   = version,
                    onBuy     = { item -> inventory.buy(item, party); version++ },
                    onClose   = { phase = SlicePhase.CHAPTER2_MARKET }
                )
            }
        }

        // Questbook flash (always on top) with animated slide-in/out
        SliceQuestbookFlash(visible = flashText != null, text = flashText ?: "")
    }
}

// ---------------------------------------------------------------------------
// Sub-composables
// ---------------------------------------------------------------------------

@Composable
private fun ExploreView(
    title: String,
    scene: WorldScene,
    pressure: QuestPressure,
    falseMarkers: List<String> = emptyList(),
    onStep: (Direction) -> Unit,
    barkButtons: List<Pair<BarkEvent, String>>,
    onBark: (BarkEvent) -> Unit,
    onAttack: () -> Unit = {},
    onInteract: () -> Unit = {},
    onPotion: () -> Unit = {}
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isPortrait = maxHeight > maxWidth
        if (isPortrait) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, color = Color(0xFFE8C170), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    SlicePressureChip(pressure)
                }
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    GameCanvas(scene = scene, isActive = true, modifier = Modifier.fillMaxSize())
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VirtualJoystick(onStep = onStep)
                    MobileActionButtons(
                        onAttack   = onAttack,
                        onInteract = onInteract,
                        onPotion   = onPotion
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, color = Color(0xFFE8C170), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                SlicePressureChip(pressure)
                if (falseMarkers.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        falseMarkers.forEach { marker ->
                            SliceFalseMarkerChip(marker)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    GameCanvas(scene = scene, isActive = true, modifier = Modifier.fillMaxSize())
                }
                Spacer(Modifier.height(8.dp))
                SliceDPad(onStep)
                if (barkButtons.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        barkButtons.forEach { (bark, label) ->
                            SliceSmallButton(label, Color(0xFF4A3F73)) { onBark(bark) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CombatView(
    director: SliceDirector,
    message: String,
    onAction: (CombatAction) -> Unit
) {
    val engine = director.currentCombat
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Combat!", color = Color(0xFFE8C170), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        engine?.bossPhase?.let {
            Spacer(Modifier.height(4.dp))
            Text("Boss Phase: $it", color = Color(0xFFE53935), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(8.dp))

        if (engine != null) {
            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF2B2640), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    Text("Enemies", color = Color(0xFFB0A8D0), fontSize = 12.sp)
                    engine.enemies.forEach { e ->
                        val res = if (e.name.contains("Accountant")) Res.drawable.boss_rat_accountant else Res.drawable.enemy_rat
                        SliceHpRow(res, e.name, e.hp, e.maxHp, Color(0xFFE53935))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Party", color = Color(0xFFB0A8D0), fontSize = 12.sp)
                    engine.party.forEach { p ->
                        val res = when (p.id) {
                            "brugg"  -> Res.drawable.hero_brugg
                            "vellum" -> Res.drawable.hero_vellum
                            else     -> Res.drawable.hero_nib
                        }
                        SliceHpRow(res, p.name, p.hp, p.maxHp, Color(0xFF4CAF50))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF000000), modifier = Modifier.fillMaxWidth()) {
                Text(message, color = Color(0xFFE0E0E0), fontSize = 13.sp, modifier = Modifier.padding(10.dp))
            }
            Spacer(Modifier.height(8.dp))

            when (engine.result) {
                CombatResult.ONGOING -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val target = engine.livingEnemies().firstOrNull()?.id
                        SliceSmallButton("Attack") { target?.let { onAction(CombatAction.Attack(it)) } }
                        SliceSmallButton("Dodge")  { onAction(CombatAction.Dodge) }
                        SliceSmallButton("Heal", Color(0xFF2E7D32)) { onAction(CombatAction.Heal) }
                        SliceSmallButton("Flame", Color(0xFF4A3F73)) {
                            onAction(CombatAction.UtilityBark(BarkEvent.VELLUM_CALLS_FOR_FLAME))
                        }
                    }
                }
                CombatResult.VICTORY -> Text("VICTORY!", color = Color(0xFF7FD17F), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                CombatResult.DEFEAT  -> Text("DEFEAT",  color = Color(0xFFE53935), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun QuestbookFullView(partyName: String, onDismiss: () -> Unit) {
    // Animated magic book = the Questbook of the story (assets/HD/props/magic-book).
    // Sequence: dim in -> closed book settles -> cross-fades open -> page ink appears.
    var opened by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(550); opened = true }

    val bookScale by animateFloatAsState(
        targetValue = if (opened) 1f else 0.9f,
        animationSpec = tween(durationMillis = 650)
    )
    val openAlpha by animateFloatAsState(
        targetValue = if (opened) 1f else 0f,
        animationSpec = tween(durationMillis = 500)
    )
    val inkAlpha by animateFloatAsState(
        targetValue = if (opened) 1f else 0f,
        animationSpec = tween(durationMillis = 700, delayMillis = 350)
    )
    val glow = rememberInfiniteTransition()
    val glowPulse by glow.animateFloat(
        initialValue = 0.45f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse)
    )

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xF20A0712)),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .aspectRatio(1088f / 816f)
                .graphicsLayer(scaleX = bookScale, scaleY = bookScale)
        ) {
            val w = maxWidth
            val h = maxHeight

            // Warm magical glow behind the tome.
            Box(
                Modifier.fillMaxSize().alpha(glowPulse * 0.5f)
                    .background(
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            0f to Color(0xFFFFE3A0), 0.7f to Color(0x33FFB347), 1f to Color.Transparent
                        )
                    )
            )

            // Closed book underneath; the open book fades in over it.
            Image(
                painter = painterResource(Res.drawable.questbook_closed),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().alpha(1f - openAlpha),
                contentScale = ContentScale.Fit
            )
            Image(
                painter = painterResource(Res.drawable.questbook_open),
                contentDescription = "Questbook",
                modifier = Modifier.fillMaxSize().alpha(openAlpha),
                contentScale = ContentScale.Fit
            )

            // Left page: the eternal registry header. Right page: this entry.
            Column(
                modifier = Modifier
                    .padding(start = w * 0.12f, top = h * 0.24f)
                    .width(w * 0.30f)
                    .alpha(inkAlpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Official Registry\nof Heroes", color = Color(0xFF4A2F1A),
                    fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(10.dp))
                Text("Filed in perpetuity by\norder of the Questbook.",
                    color = Color(0xFF6B4A2A), fontSize = 11.sp, lineHeight = 14.sp)
                Spacer(Modifier.height(14.dp))
                Text("\u2767", color = Color(0xFF8A5A2B), fontSize = 22.sp)
            }
            Column(
                modifier = Modifier
                    .padding(start = w * 0.56f, top = h * 0.24f)
                    .width(w * 0.30f)
                    .alpha(inkAlpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Entry:", color = Color(0xFF6B4A2A), fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Text(partyName, color = Color(0xFF3A2410),
                    fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 22.sp)
                Spacer(Modifier.height(10.dp))
                Text("(This page cannot be unread.)",
                    color = Color(0xFF7A5A3A), fontSize = 10.sp)
            }
        }

        // Close affordance, anchored low.
        Box(Modifier.fillMaxSize().padding(bottom = 28.dp), contentAlignment = Alignment.BottomCenter) {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xCC2A1B0E)),
                modifier = Modifier.alpha(inkAlpha)
            ) {
                Text("Close the Questbook", color = Color(0xFFE8C170))
            }
        }
    }
}

@Composable
private fun EndView(title: String, subtitle: String, color: Color, onRestart: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF15131F)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text(title, color = color, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(Modifier.height(12.dp))
            Text(subtitle, color = Color(0xFFF5E9C8), fontSize = 14.sp)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A4A6B))
            ) {
                Text("Play Again", color = Color.White)
            }
        }
    }
}

@Composable
private fun SliceQuestbookFlash(visible: Boolean, text: String) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(animationSpec = tween(300)) { -it } + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(animationSpec = tween(300)) { -it } + fadeOut(animationSpec = tween(300))
        ) {
            Surface(shape = RoundedCornerShape(10.dp), color = Color(0xF2241E12), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("QUESTBOOK", color = Color(0xFFE8C170), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(text, color = Color(0xFFF5E9C8), fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun SlicePressureChip(pressure: QuestPressure) {
    val targetColor = when (pressure) {
        QuestPressure.LOW    -> Color(0xFF4CAF50)
        QuestPressure.MEDIUM -> Color(0xFFFFB300)
        QuestPressure.HIGH   -> Color(0xFFE53935)
    }
    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 600)
    )
    Surface(shape = RoundedCornerShape(8.dp), color = animatedColor) {
        Text(
            "QUEST PRESSURE: $pressure",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun SliceFalseMarkerChip(label: String) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 150),
            repeatMode = RepeatMode.Reverse
        )
    )
    val offsetX by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 80),
            repeatMode = RepeatMode.Reverse
        )
    )
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFF6B2D5B),
        modifier = Modifier
            .graphicsLayer(translationX = offsetX, alpha = alpha)
    ) {
        Text(
            label,
            color = Color(0xFFFF80AB),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun SliceDPad(onStep: (Direction) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SlicePadButton("▲") { onStep(Direction.UP) }
        Row {
            SlicePadButton("◀") { onStep(Direction.LEFT) }
            Spacer(Modifier.width(48.dp))
            SlicePadButton("▶") { onStep(Direction.RIGHT) }
        }
        SlicePadButton("▼") { onStep(Direction.DOWN) }
    }
}

@Composable
private fun SlicePadButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A4A6B)),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.padding(2.dp).size(48.dp)
    ) {
        Text(label, color = Color.White, fontSize = 18.sp)
    }
}

@Composable
private fun SliceSmallButton(label: String, color: Color = Color(0xFF3A4A6B), onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 12.sp, color = Color.White)
    }
}

@Composable
private fun TitleView(onStart: () -> Unit) {
    var selectedLocale by remember { mutableStateOf(GameLocale.current) }

    val infiniteTransition = rememberInfiniteTransition()
    val promptAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        )
    )

    val locales = listOf(
        Locale.EN to "English",
        Locale.DE to "Deutsch",
        Locale.ES to "Español",
        Locale.FR to "Français",
        Locale.IT to "Italiano",
        Locale.PT to "Português",
        Locale.RU to "Русский",
        Locale.ZH to "中文",
        Locale.JA to "日本語"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown) { onStart(); true } else false
            }
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isPortrait = maxHeight > maxWidth
            Image(
                painter = painterResource(
                    if (isPortrait) Res.drawable.title_screen_portrait else Res.drawable.title_screen
                ),
                contentDescription = "Quest Accepted: Unfortunately",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                // Language selection panel
                Surface(
                    color = Color(0xE0150F09),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            "Language / Sprache / Idioma",
                            color = Color(0xFF8A7050),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        // Two rows of language buttons
                        listOf(
                            locales.take(5),
                            locales.drop(5)
                        ).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { (locale, label) ->
                                    val isSelected = locale == selectedLocale
                                    Button(
                                        onClick = {
                                            selectedLocale = locale
                                            GameLocale.current = locale
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) Color(0xFF4A3A1A) else Color(0xFF1E1810)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .height(30.dp)
                                            .then(
                                                if (isSelected)
                                                    Modifier.border(1.dp, Color(0xFFE8C170), RoundedCornerShape(4.dp))
                                                else
                                                    Modifier.border(1.dp, Color(0xFF3A2E1A), RoundedCornerShape(4.dp))
                                            )
                                    ) {
                                        Text(
                                            label,
                                            color = if (isSelected) Color(0xFFE8C170) else Color(0xFF8A7050),
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                // Start button (localized)
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xCC241E12)),
                    modifier = Modifier.alpha(promptAlpha)
                ) {
                    Text(
                        "— PRESS ANY KEY TO BEGIN —".localized(selectedLocale),
                        color = Color(0xFFE8C170),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ShopView(
    inventory: Inventory,
    party: List<Combatant>,
    version: Int,
    onBuy: (rpg.items.Item) -> Unit,
    onClose: () -> Unit
) {
    // version param forces recomposition so gold/counts refresh after each buy
    @Suppress("UNUSED_EXPRESSION") version

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .background(Color(0xFF1A1426), RoundedCornerShape(8.dp))
                .border(2.dp, Color(0xFFE8C170), RoundedCornerShape(8.dp))
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Stokeport Market", color = Color(0xFFE8C170), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("\"Premium faintness. That is the backbone of commerce.\"",
                color = Color(0xFF888888), fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            Text("Gold: ${inventory.gold} g", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(14.dp))

            // Potions
            Text("POTIONS", color = Color(0xFF9B7FD4), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            ItemCatalog.ALL.filter { it.type == ItemType.POTION }.forEach { item ->
                ShopItemRow(
                    item       = item,
                    stackCount = inventory.count(item.id),
                    isEquipped = false,
                    canAfford  = inventory.gold >= item.price,
                    onBuy      = { onBuy(item) }
                )
                Spacer(Modifier.height(4.dp))
            }

            Spacer(Modifier.height(10.dp))

            // Weapons
            Text("WEAPONS", color = Color(0xFF9B7FD4), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            ItemCatalog.ALL.filter { it.type == ItemType.WEAPON }.forEach { item ->
                ShopItemRow(
                    item       = item,
                    stackCount = 0,
                    isEquipped = inventory.isEquipped(item.id),
                    canAfford  = inventory.gold >= item.price && !inventory.isEquipped(item.id),
                    onBuy      = { onBuy(item) }
                )
                Spacer(Modifier.height(4.dp))
            }

            Spacer(Modifier.height(10.dp))

            // Inventory hint
            val carrying = ItemCatalog.ALL
                .filter { it.type == ItemType.POTION && inventory.count(it.id) > 0 }
                .joinToString("  ") { "${it.name} ×${inventory.count(it.id)}" }
            if (carrying.isNotEmpty()) {
                Text("Carrying: $carrying", color = Color(0xFF80D080), fontSize = 12.sp)
                Text("[ I ] uses cheapest potion during exploration", color = Color(0xFF666666), fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
            }

            SliceSmallButton("Close  [ Esc ]", Color(0xFF4A3F73)) { onClose() }
        }
    }
}

@Composable
private fun ShopItemRow(
    item: rpg.items.Item,
    stackCount: Int,
    isEquipped: Boolean,
    canAfford: Boolean,
    onBuy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF251E35), RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.name, color = Color(0xFFE8C170), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                if (stackCount > 0) {
                    Spacer(Modifier.width(6.dp))
                    Text("×$stackCount", color = Color(0xFF80D080), fontSize = 12.sp)
                }
                if (isEquipped) {
                    Spacer(Modifier.width(6.dp))
                    Text("[EQUIPPED]", color = Color(0xFF80D080), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(item.description, color = Color(0xFFAAAAAA), fontSize = 11.sp)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
            Text("${item.price} g", color = Color(0xFFFFD700), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            if (!isEquipped) {
                Spacer(Modifier.height(2.dp))
                SliceSmallButton(
                    label = "Buy",
                    color = if (canAfford) Color(0xFF2E6B2E) else Color(0xFF3D3D3D)
                ) { if (canAfford) onBuy() }
            }
        }
    }
}

private fun Offset.toCardinalDirection(deadZone: Float): Direction? {
    if (getDistance() < deadZone) return null
    return if (abs(x) >= abs(y)) {
        if (x > 0) Direction.RIGHT else Direction.LEFT
    } else {
        if (y > 0) Direction.DOWN else Direction.UP
    }
}

@Composable
private fun VirtualJoystick(onStep: (Direction) -> Unit) {
    var dragDir by remember { mutableStateOf<Direction?>(null) }
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(dragDir) {
        val d = dragDir ?: return@LaunchedEffect
        while (true) {
            onStep(d)
            delay(160)
        }
    }

    Box(
        modifier = Modifier
            .size(120.dp)
            .background(Color(0x33FFFFFF), RoundedCornerShape(60.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart  = { thumbOffset = Offset.Zero },
                    onDragEnd    = { thumbOffset = Offset.Zero; dragDir = null },
                    onDragCancel = { thumbOffset = Offset.Zero; dragDir = null }
                ) { change, dragAmount ->
                    change.consume()
                    val next = thumbOffset + dragAmount
                    val maxR = size.width / 2f
                    thumbOffset = if (next.getDistance() > maxR) next * (maxR / next.getDistance()) else next
                    dragDir = thumbOffset.toCardinalDirection(deadZone = 18f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffset.x.roundToInt(), thumbOffset.y.roundToInt()) }
                .size(44.dp)
                .background(Color(0x99FFFFFF), RoundedCornerShape(22.dp))
        )
    }
}

@Composable
private fun MobileActionButtons(
    onAttack: () -> Unit,
    onInteract: () -> Unit,
    onPotion: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MobileRoundButton("[Z]", Color(0xFF4A2B2B), onAttack)
            MobileRoundButton("[E]", Color(0xFF2B3A4A), onInteract)
        }
        MobileRoundButton("[I]", Color(0xFF2B4A2B), onPotion)
    }
}

@Composable
private fun MobileRoundButton(label: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(52.dp),
        shape = RoundedCornerShape(26.dp)
    ) {
        Text(label, fontSize = 11.sp, color = Color.White)
    }
}

@Composable
private fun SliceHpRow(res: DrawableResource, name: String, hp: Int, maxHp: Int, barColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Image(
            painter = painterResource(res),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(name, color = Color.White, fontSize = 12.sp, modifier = Modifier.width(70.dp))
        Box(
            modifier = Modifier.weight(1f).height(12.dp)
                .background(Color(0xFF44405C), RoundedCornerShape(4.dp))
        ) {
            val frac = if (maxHp == 0) 0f else hp.toFloat() / maxHp
            Box(
                modifier = Modifier.fillMaxHeight()
                    .fillMaxWidth(frac.coerceIn(0f, 1f))
                    .background(barColor, RoundedCornerShape(4.dp))
            )
        }
        Spacer(Modifier.width(6.dp))
        Text("$hp/$maxHp", color = Color(0xFFB0A8D0), fontSize = 11.sp)
    }
}
