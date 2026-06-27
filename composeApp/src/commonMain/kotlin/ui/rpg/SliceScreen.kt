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
import androidx.compose.foundation.clickable
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
import rpg.settings.GameSettings
import rpg.bark.BarkEvent
import rpg.bark.AmbientBarks
import rpg.bark.audio.BarkAudioPlayer
import rpg.bark.audio.createPlatformAudioPlayer
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

// --- Scripted dialogue lines ---

private val INTRO_LINES = listOf(
    DialogueLine("Barkeep", "You've been officially registered as a Hero Party. Don't ask how.", "bark/barkeep/greetings_stranger.wav"),
    DialogueLine("Nib", "...by who?", "bark/nib/where_am_i.wav"),
    DialogueLine("Barkeep", "The Questbook. It fell on the desk and opened to the right page. Fate, probably.", "bark/barkeep/hmm_i_wonder_what_this_could_be.wav")
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
    DialogueLine("Vellum", "The Questbook is restless. It demands a new page.")
)

private val CHAPTER2_MERCHANT_LINES = listOf(
    DialogueLine("Merchant", "See if any of this strikes your fancy.", "bark/merchant/see_if_any_of_this_strikes_your_fancy.wav"),
    DialogueLine("Merchant", "Make me an offer. I won't bite.", "bark/merchant/make_me_an_offer.wav"),
    DialogueLine("Nib", "How much do you want for this?", "bark/nib/how_much_do_you_want_for_this.wav")
)

private val CHAPTER2_GUARD_LINES = listOf(
    DialogueLine("Guard", "The forest trail east of here has been overrun by wolves.", "bark/guard/there_are_all_manner_of_creatures_within_these_woods.wav"),
    DialogueLine("Guard", "If you're looking for trouble, you'll find it there.", "bark/guard/nothing_to_see_here.wav"),
    DialogueLine("Brugg", "Just keep to the trail.", "bark/brugg/just_keep_to_the_trail.wav")
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
    DialogueLine("Brugg", "Barkeep! A flagon of ale!", "bark/brugg/barkeep_a_flagon_of_ale.wav")
)

private val BARKEEP_POST_SEWER_LINES = listOf(
    DialogueLine("Barkeep", "Been playing in the sewers, have we?", "bark/barkeep/been_playing_in_the_sewers_have_we.wav")
)

private val PATRON_LINES = listOf(
    DialogueLine("Patron", "He sure is slow for a four-armed bartender.", "bark/vellum/he_sure_is_slow_for_a_four_armed_bartender.wav")
)

// --- World connector dialogue lines ---

private val HEROES_HOME_EXT_LINES = listOf(
    DialogueLine("", "The party steps outside into the morning air."),
    DialogueLine("Nib", "The guild hall is just down the road. And the tavern's right behind us."),
    DialogueLine("Brugg", "Let's not dawdle.", "bark/brugg/just_keep_to_the_trail.wav")
)

private val GUILDMASTER_LINES = listOf(
    DialogueLine("Guildmaster", "Registered heroes may pick up contracts at the board inside.", "bark/guildmaster/greetings_friends.wav"),
    DialogueLine("Guildmaster", "Non-registered adventurers are asked to leave or be fined.", "bark/guildmaster/grab_your_torch_theres_work_to_be_done.wav"),
    DialogueLine("Nib", "We're registered. The Questbook said so.", "bark/nib/hard-won_knowledge.wav")
)

private val CHAPEL_DEVOTEE_LINES = listOf(
    DialogueLine("Citizen", "The chapel has been... quiet lately. Too quiet.", "bark/citizen/hmm_i_wonder_what_this_could_be.wav"),
    DialogueLine("Citizen", "Something moved the pews. Something large.", "bark/citizen/what_dark_dealings_await_here.wav"),
    DialogueLine("Vellum", "Perfect. Let's go in.", "bark/vellum/so_thats_how_it_is_then.wav")
)

private val TEMPLE_EXT_INTRO_LINES = listOf(
    DialogueLine("", "The ruined temple exterior. Overgrown. Unsettled."),
    DialogueLine("Brugg", "Wolves.", "bark/brugg/just_keep_to_the_trail.wav"),
    DialogueLine("Nib", "Lots of wolves.")
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

    // Player-adjustable settings (sound/music/voice + locale).
    var settings by remember { mutableStateOf(GameSettings()) }

    // Engine + persistent party (HP carries across encounters)
    val director = remember { SliceDirector(clock) }
    val party = remember { freshSliceParty() }

    // Wire up bark audio playback
    val barkAudioPlayer = remember { BarkAudioPlayer(createPlatformAudioPlayer()) }
    DisposableEffect(Unit) {
        director.barkAudioPlayer = barkAudioPlayer
        onDispose {
            barkAudioPlayer.release()
            director.barkAudioPlayer = null
        }
    }
    // Keep the audio player's mute state in sync with the sound setting.
    // Runs on initial composition and again whenever soundEnabled changes.
    LaunchedEffect(settings.soundEnabled) {
        barkAudioPlayer.enabled = settings.soundEnabled
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
                GridEntity("rat_corridor_1", 6, 5, GridEntityType.ENEMY, "enemy_rat"),
                GridEntity("rat_corridor_2", 8, 5, GridEntityType.ENEMY, "enemy_rat"),
                GridEntity("rat_mini_1", 5, 13, GridEntityType.ENEMY, "enemy_rat"),
                GridEntity("rat_mini_2", 7, 13, GridEntityType.ENEMY, "enemy_rat"),
                GridEntity("rat_mini_3", 9, 13, GridEntityType.ENEMY, "enemy_rat"),
                GridEntity("blob_mini", 7, 15, GridEntityType.ENEMY, "enemy_blob")
            ))
        }
    }
    val bossWorld = remember {
        GridWorld(GameMaps.bossRoom()).also { w ->
            w.entities.add(GridEntity("rat_accountant", 6, 7, GridEntityType.ENEMY, "boss_rat_accountant"))
        }
    }

    // Chapter 2 worlds
    val marketWorld = remember {
        GridWorld(GameMaps.stokeportMarket()).also { w ->
            w.entities.add(GridEntity("merchant",  12, 10, GridEntityType.NPC, "npc_merchant"))
            w.entities.add(GridEntity("guard",      6, 15, GridEntityType.NPC, "npc_guard"))
            w.entities.add(GridEntity("citizen1",  10,  7, GridEntityType.NPC, "npc_citizen1"))
        }
    }
    val forestWorld = remember {
        GridWorld(GameMaps.forestTrail()).also { w ->
            w.entities.addAll(listOf(
                GridEntity("wolf_1", 16, 8, GridEntityType.ENEMY, "enemy_wolf"),
                GridEntity("wolf_2", 18, 11, GridEntityType.ENEMY, "enemy_wolf"),
                GridEntity("wolf_3", 20, 9, GridEntityType.ENEMY, "enemy_wolf"),
                GridEntity("tax_badger", 24, 18, GridEntityType.ENEMY, "boss_rat_accountant")
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
            w.entities.add(GridEntity("wolf_a", 8, 6, GridEntityType.ENEMY, "enemy_wolf"))
            w.entities.add(GridEntity("wolf_b", 13, 4, GridEntityType.ENEMY, "enemy_wolf"))
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
                SlicePhase.SEWER_COMBAT -> {
                    sewerWorld.removeEntity("rat_corridor_1"); sewerWorld.removeEntity("rat_corridor_2")
                    director.clearCombat()
                    fireAndFlash(BarkEvent.NIB_IT_WASNT_ME)
                    phase = SlicePhase.SEWER
                }
                SlicePhase.MINI_DUNGEON_COMBAT -> {
                    listOf("rat_mini_1","rat_mini_2","rat_mini_3","blob_mini").forEach(sewerWorld::removeEntity)
                    director.clearCombat()
                    phase = SlicePhase.SEWER
                }
                SlicePhase.BOSS_COMBAT -> {
                    bossWorld.removeEntity("rat_accountant")
                    director.clearCombat()
                    fireAndFlash(BarkEvent.VELLUM_THIS_CHANGES_EVERYTHING)
                    dialogueLines = POST_BOSS_LINES
                    dialogueIndex = 0
                    phase = SlicePhase.POST_BOSS
                }
                SlicePhase.CHAPTER2_FOREST_COMBAT -> {
                    listOf("wolf_1", "wolf_2", "wolf_3").forEach(forestWorld::removeEntity)
                    director.clearCombat()
                    phase = SlicePhase.CHAPTER2_FOREST
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
    sewerScene.onEntityInteraction = { entity ->
        if (phase == SlicePhase.SEWER) {
            lastActivityTime = clock()
            when {
                entity.id.startsWith("rat_corridor") -> {
                    // Exploration bark: encountering enemies
                    AmbientBarks.pick(AmbientBarks.ENEMY_WARNING, barkRandom)?.let { fireAndFlash(it) }
                    director.startCombat(CombatEngine(party, listOf(
                        EnemyArchetype.SEWER_RAT.spawn("rat_corridor_1"),
                        EnemyArchetype.SEWER_RAT.spawn("rat_corridor_2")
                    )))
                    combatMessage = "Two Sewer Rats block the path!"
                    phase = SlicePhase.SEWER_COMBAT
                }
                entity.id.startsWith("rat_mini") || entity.id == "blob_mini" -> {
                    // Exploration bark: encountering enemies
                    AmbientBarks.pick(AmbientBarks.ENEMY_WARNING, barkRandom)?.let { fireAndFlash(it) }
                    director.startCombat(CombatEngine(party, listOf(
                        EnemyArchetype.SEWER_RAT.spawn("rat_mini_1"),
                        EnemyArchetype.SEWER_RAT.spawn("rat_mini_2"),
                        EnemyArchetype.SEWER_RAT.spawn("rat_mini_3"),
                        EnemyArchetype.SLUDGE_BLOB.spawn("blob_mini")
                    )))
                    combatMessage = "Three Sewer Rats and a Sludge Blob appear!"
                    phase = SlicePhase.MINI_DUNGEON_COMBAT
                }
            }
        }
    }

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
                    dialogueLines = CHAPTER2_MERCHANT_LINES
                    dialogueIndex = 0
                    phase = SlicePhase.CHAPTER2_MARKET_NPC
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
    forestScene.onEntityInteraction = { entity ->
        if (phase == SlicePhase.CHAPTER2_FOREST) {
            lastActivityTime = clock()
            when {
                entity.id.startsWith("wolf") -> {
                    val warningBarks = AmbientBarks.FOREST_WARNING
                    AmbientBarks.pick(warningBarks, barkRandom)?.let { fireAndFlash(it) }
                    director.startCombat(CombatEngine(party, listOf(
                        EnemyArchetype.FOREST_WOLF.spawn("wolf_1"),
                        EnemyArchetype.FOREST_WOLF.spawn("wolf_2"),
                        EnemyArchetype.FOREST_WOLF.spawn("wolf_3")
                    )))
                    combatMessage = "Three Forest Wolves emerge from the undergrowth!"
                    phase = SlicePhase.CHAPTER2_FOREST_COMBAT
                }
                entity.id == "tax_badger" -> {
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
    templeExtScene.onEntityInteraction = { entity ->
        if (phase == SlicePhase.CHAPTER2_TEMPLE_EXT && entity.id.startsWith("wolf")) {
            lastActivityTime = clock()
            AmbientBarks.pick(AmbientBarks.FOREST_WARNING, barkRandom)?.let { fireAndFlash(it) }
            director.startCombat(CombatEngine(party, listOf(
                EnemyArchetype.FOREST_WOLF.spawn("wolf_a"),
                EnemyArchetype.FOREST_WOLF.spawn("wolf_b")
            )))
            combatMessage = "Two wolves guard the temple approach!"
            phase = SlicePhase.CHAPTER2_FOREST_COMBAT
        }
    }

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
    bridgeScene.onEntityInteraction = { _ -> }

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
                                else -> {}
                            }
                        }
                        return@onKeyEvent npc != null
                    }
                    else -> return@onKeyEvent false
                }
                true
            }
    ) {
        when (phase) {
            // --- Title screen ---
            SlicePhase.TITLE_SCREEN ->
                TitleView(
                    soundEnabled = settings.soundEnabled,
                    onSoundEnabledChange = { settings = settings.copy(soundEnabled = it) },
                    onStart = { phase = SlicePhase.INTRO_CUTSCENE }
                )

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
                    onBark   = ::fireAndFlash
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
                    }
                )

            SlicePhase.BOSS_ROOM ->
                ExploreView(
                    title    = "The Rat Accountant's Office",
                    scene    = bossScene,
                    pressure = director.pressure,
                    falseMarkers = director.falseMarkers,
                    onStep   = bossWorld::requestStep,
                    barkButtons = listOf(BarkEvent.VELLUM_CALLS_FOR_FLAME to "Vellum: Flame"),
                    onBark   = ::fireAndFlash
                )

            // --- Combat ---
            SlicePhase.SEWER_COMBAT,
            SlicePhase.MINI_DUNGEON_COMBAT,
            SlicePhase.BOSS_COMBAT ->
                CombatView(director = director, message = combatMessage, onAction = ::handleCombatAction)

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
                    onBark   = ::fireAndFlash
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
                    onBark   = ::fireAndFlash
                )

            SlicePhase.CHAPTER2_FOREST_COMBAT ->
                CombatView(director = director, message = combatMessage, onAction = ::handleCombatAction)

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
                    }
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
                    onStep   = heroesHomeExtWorld::requestStep,
                    barkButtons = emptyList(),
                    onBark   = ::fireAndFlash
                )

            SlicePhase.CHAPTER2_GUILDHALL ->
                ExploreView(
                    title    = "Adventurers' Guild",
                    scene    = guildHallExtScene,
                    pressure = director.pressure,
                    falseMarkers = director.falseMarkers,
                    onStep   = guildHallExtWorld::requestStep,
                    barkButtons = listOf(BarkEvent.NIB_SMELL_GOLD to "Nib: Contracts"),
                    onBark   = ::fireAndFlash
                )

            SlicePhase.CHAPTER2_CHAPEL_EXT ->
                ExploreView(
                    title    = "Chapel of the Unresolved",
                    scene    = chapelExtScene,
                    pressure = director.pressure,
                    falseMarkers = director.falseMarkers,
                    onStep   = chapelExtWorld::requestStep,
                    barkButtons = listOf(BarkEvent.VELLUM_CALLS_FOR_FLAME to "Vellum: Sense"),
                    onBark   = ::fireAndFlash
                )

            SlicePhase.CHAPTER2_TEMPLE_EXT ->
                ExploreView(
                    title    = "Ruined Temple Approach",
                    scene    = templeExtScene,
                    pressure = director.pressure,
                    falseMarkers = director.falseMarkers,
                    onStep   = templeExtWorld::requestStep,
                    barkButtons = listOf(BarkEvent.BRUGG_ATTACK to "Brugg: Attack!"),
                    onBark   = ::fireAndFlash
                )

            SlicePhase.CHAPTER2_GLASSBLOWERS ->
                ExploreView(
                    title    = "Glassblowers' District",
                    scene    = glassblowersExtScene,
                    pressure = director.pressure,
                    falseMarkers = director.falseMarkers,
                    onStep   = glassblowersExtWorld::requestStep,
                    barkButtons = listOf(BarkEvent.NIB_SMELL_GOLD to "Nib: Browse"),
                    onBark   = ::fireAndFlash
                )

            SlicePhase.CHAPTER2_BRIDGE ->
                ExploreView(
                    title    = "The Ironway Bridge",
                    scene    = bridgeScene,
                    pressure = director.pressure,
                    falseMarkers = director.falseMarkers,
                    onStep   = bridgeWorld::requestStep,
                    barkButtons = emptyList(),
                    onBark   = ::fireAndFlash
                )
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
    onBark: (BarkEvent) -> Unit
) {
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
private fun TitleView(
    soundEnabled: Boolean,
    onSoundEnabledChange: (Boolean) -> Unit,
    onStart: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val promptAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        )
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown) { onStart(); true } else false
            }
            .focusable()
            .clickable { onStart() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(Res.drawable.title_screen),
            contentDescription = "Quest Accepted: Unfortunately",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                // Sound toggle (sits below where a language picker would live).
                Button(
                    onClick = { onSoundEnabledChange(!soundEnabled) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xCC241E12))
                ) {
                    Text(
                        if (soundEnabled) "Sound: On" else "Sound: Off",
                        color = Color(0xFFE8C170),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xCC241E12)),
                    modifier = Modifier.alpha(promptAlpha)
                ) {
                    Text(
                        "Tap to Start",
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
