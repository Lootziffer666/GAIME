package ui.rpg

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import gaime.resources.enemy_rat
import gaime.resources.hero_brugg
import gaime.resources.hero_nib
import gaime.resources.hero_vellum
import gaime.resources.tileset_dungeon
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import rpg.BarkOutcome
import rpg.SliceDirector
import rpg.SlicePhase
import rpg.bark.BarkEvent
import rpg.bark.audio.BarkAudioPlayer
import rpg.bark.audio.createPlatformAudioPlayer
import rpg.combat.BossController
import rpg.combat.CombatAction
import rpg.combat.CombatEngine
import rpg.combat.CombatEvent
import rpg.combat.CombatResult
import rpg.combat.Combatant
import rpg.combat.EnemyArchetype
import rpg.combat.Side
import rpg.questbook.QuestPressure
import rpg.questbook.QuestbookEffect
import rpg.questbook.RoomContext
import rpg.world.Direction
import rpg.world.GameMaps
import rpg.world.GridEntity
import rpg.world.GridEntityType
import rpg.world.GridWorld
import ui.GameCanvas
import kotlin.random.Random
import kotlin.time.TimeSource

// --- Scripted dialogue lines ---

private val INTRO_LINES = listOf(
    DialogueLine("Barkeep", "You've been officially registered as a Hero Party. Don't ask how."),
    DialogueLine("Nib", "...by who?"),
    DialogueLine("Barkeep", "The Questbook. It fell on the desk and opened to the right page. Fate, probably.")
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
    DialogueLine("Barkeep", "Back already? Smells like sewer.", "bark/brugg/been_playing_in_the_sewers_have_we.wav"),
    DialogueLine("", "Quest Pressure: Reset. New quests pending. The Questbook is always listening.")
)

// --- NPC dialogue lines ---

private val BARKEEP_PRE_SEWER_LINES = listOf(
    DialogueLine("Barkeep", "Spend some coin or get out.", "bark/brugg/spend_some_coin_or_get_out.wav"),
    DialogueLine("Brugg", "Barkeep! A flagon of ale!", "bark/brugg/barkeep_a_flagon_of_ale.wav")
)

private val BARKEEP_POST_SEWER_LINES = listOf(
    DialogueLine("Barkeep", "Been playing in the sewers, have we?", "bark/brugg/been_playing_in_the_sewers_have_we.wav")
)

private val PATRON_LINES = listOf(
    DialogueLine("Patron", "He sure is slow for a four-armed bartender.", "bark/vellum/he_sure_is_slow_for_a_four_armed_bartender.wav")
)

// --- Room contexts ---

private val TAVERN_CTX = RoomContext("tavern", RoomContext.ROOM_TAVERN, hasInteractableTarget = true)

// --- Idle bark selection ---

private fun pickIdleBark(phase: SlicePhase): BarkEvent? {
    val barks = when (phase) {
        SlicePhase.TAVERN -> listOf(
            BarkEvent.BRUGG_BARKEEP_A_FLAGON,
            BarkEvent.NIB_STEW_AGAIN,
            BarkEvent.VELLUM_NOW_WHAT_WAS_THAT_INCANTATION,
            BarkEvent.NIB_I_LOVE_GOLD,
            BarkEvent.BRUGG_WHERE_DID_I_PUT_THAT_MAP,
            BarkEvent.NIB_IS_THAT_ROAST,
            BarkEvent.BRUGG_IS_THAT_ROAST,
            BarkEvent.VELLUM_YOUVE_GOT_TO_TRY_ROAST,
            BarkEvent.BRUGG_THATS_NOTHING_ALE_WONT_FIX,
            BarkEvent.NIB_WHERES_THE_PRIVVY,
            BarkEvent.BRUGG_WHAT_NEWS,
            BarkEvent.VELLUM_WHERES_THE_NEAREST_INN
        )
        SlicePhase.SEWER, SlicePhase.BOSS_ROOM -> listOf(
            BarkEvent.BRUGG_GRAB_YOUR_TORCH,
            BarkEvent.NIB_THERES_A_HOLE_IN_MY_BOOT,
            BarkEvent.VELLUM_TIME_WAITS_FOR_NO_MAN,
            BarkEvent.VELLUM_OF_ALL_THE_ARCANE_LORE,
            BarkEvent.NIB_I_LOVE_GOLD,
            BarkEvent.NIB_THIS_THING_LOOKS_INTERESTING,
            BarkEvent.BRUGG_THIS_THING_LOOKS_INTERESTING,
            BarkEvent.VELLUM_THIS_THING_LOOKS_INTERESTING,
            BarkEvent.NIB_LETS_GET_OUT_OF_HERE,
            BarkEvent.BRUGG_LETS_GET_OUT_OF_HERE
        )
        else -> return null
    }
    return barks[Random.nextInt(barks.size)]
}
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
    val bossRatImg = imageResource(Res.drawable.boss_rat_accountant)
    val spriteMap = remember(enemyRatImg, bossRatImg) {
        mapOf("enemy_rat" to enemyRatImg, "boss_rat_accountant" to bossRatImg)
    }

    // Phase + narrative state
    var phase by remember { mutableStateOf(SlicePhase.INTRO_CUTSCENE) }
    var dialogueLines by remember { mutableStateOf(INTRO_LINES) }
    var dialogueIndex by remember { mutableStateOf(0) }
    var flashText by remember { mutableStateOf<String?>(null) }
    var combatMessage by remember { mutableStateOf("") }
    var version by remember { mutableStateOf(0) }
    var hasReturnedFromSewer by remember { mutableStateOf(false) }

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

    // Idle bark timer state
    var lastActivityTime by remember { mutableStateOf(clock()) }

    // Worlds
    val tavernWorld = remember {
        GridWorld(GameMaps.tavern()).also { w ->
            w.entities.add(GridEntity("barkeep", 5, 3, GridEntityType.NPC, "hero_brugg"))
            w.entities.add(GridEntity("patron", 3, 6, GridEntityType.NPC, "hero_vellum"))
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
                GridEntity("blob_mini", 7, 15, GridEntityType.ENEMY, "enemy_rat")
            ))
        }
    }
    val bossWorld = remember {
        GridWorld(GameMaps.bossRoom()).also { w ->
            w.entities.add(GridEntity("rat_accountant", 6, 7, GridEntityType.ENEMY, "boss_rat_accountant"))
        }
    }

    // Scenes (created once per resource load; callbacks re-assigned each recomposition)
    val tavernScene = remember(tileset, playerSprite) { WorldScene(tavernWorld, tileset, playerSprite) }
    val sewerScene  = remember(tileset, playerSprite) { WorldScene(sewerWorld,  tileset, playerSprite) }
    val bossScene   = remember(tileset, playerSprite) { WorldScene(bossWorld,   tileset, playerSprite) }

    // Keep sprite maps current
    tavernScene.spriteMap = spriteMap
    sewerScene.spriteMap  = spriteMap
    bossScene.spriteMap   = spriteMap

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
                SlicePhase.TAVERN, SlicePhase.SEWER, SlicePhase.BOSS_ROOM
            )
            if (!isExplorationPhase) continue
            val elapsed = clock() - lastActivityTime
            if (elapsed >= 30_000L) {
                val idleBark = pickIdleBark(phase)
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
                    phase = SlicePhase.VICTORY
                }
                SlicePhase.NPC_DIALOGUE     -> phase = SlicePhase.TAVERN
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
                    // Fire a treasure bark after boss victory
                    val treasureBarks = listOf(
                        BarkEvent.NIB_THIS_LOOKS_LIKE_TREASURE,
                        BarkEvent.BRUGG_THIS_LOOKS_LIKE_TREASURE,
                        BarkEvent.VELLUM_THIS_LOOKS_LIKE_TREASURE
                    )
                    fireAndFlash(treasureBarks[Random.nextInt(treasureBarks.size)])
                    dialogueLines = POST_BOSS_LINES
                    dialogueIndex = 0
                    phase = SlicePhase.POST_BOSS
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
            val sewerEntryBarks = listOf(BarkEvent.BRUGG_THE_DEEPER_WE_GO, BarkEvent.VELLUM_TREES_HAVE_EYES)
            fireAndFlash(sewerEntryBarks[Random.nextInt(sewerEntryBarks.size)])
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
                    val atmosphereBarks = listOf(
                        BarkEvent.NIB_THIS_PLACE_REEKS_OF_DEATH,
                        BarkEvent.BRUGG_THIS_PLACE_REEKS_OF_DEATH,
                        BarkEvent.VELLUM_THIS_PLACE_REEKS_OF_DEATH,
                        BarkEvent.NIB_WHAT_DARK_DEALINGS,
                        BarkEvent.BRUGG_WHAT_DARK_DEALINGS,
                        BarkEvent.VELLUM_WHAT_DARK_DEALINGS
                    )
                    fireAndFlash(atmosphereBarks[Random.nextInt(atmosphereBarks.size)])
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
                    val warningBarks = listOf(
                        BarkEvent.BRUGG_THIS_LOOKS_LIKE_TROUBLE,
                        BarkEvent.NIB_THEYRE_ONTO_US,
                        BarkEvent.NIB_LOOK_OUT,
                        BarkEvent.BRUGG_LOOK_OUT,
                        BarkEvent.VELLUM_LOOK_OUT
                    )
                    fireAndFlash(warningBarks[Random.nextInt(warningBarks.size)])
                    director.startCombat(CombatEngine(party, listOf(
                        EnemyArchetype.SEWER_RAT.spawn("rat_corridor_1"),
                        EnemyArchetype.SEWER_RAT.spawn("rat_corridor_2")
                    )))
                    combatMessage = "Two Sewer Rats block the path!"
                    phase = SlicePhase.SEWER_COMBAT
                }
                entity.id.startsWith("rat_mini") || entity.id == "blob_mini" -> {
                    // Exploration bark: encountering enemies
                    val warningBarks = listOf(
                        BarkEvent.BRUGG_THIS_LOOKS_LIKE_TROUBLE,
                        BarkEvent.NIB_THEYRE_ONTO_US,
                        BarkEvent.NIB_LOOK_OUT,
                        BarkEvent.BRUGG_LOOK_OUT,
                        BarkEvent.VELLUM_LOOK_OUT
                    )
                    fireAndFlash(warningBarks[Random.nextInt(warningBarks.size)])
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
            val discoveryBarks = listOf(BarkEvent.NIB_WHAT_DO_WE_HAVE_HERE, BarkEvent.VELLUM_HMM_WONDER_WHAT_THIS_IS)
            fireAndFlash(discoveryBarks[Random.nextInt(discoveryBarks.size)])
            director.startCombat(CombatEngine(party, emptyList(), EnemyArchetype.RAT_ACCOUNTANT.spawn("rat_accountant"), BossController()))
            combatMessage = "The Rat Accountant looks up from its desk of garbage."
            phase = SlicePhase.BOSS_COMBAT
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
                val world = when (phase) {
                    SlicePhase.TAVERN    -> tavernWorld
                    SlicePhase.SEWER     -> sewerWorld
                    SlicePhase.BOSS_ROOM -> bossWorld
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
                                SlicePhase.TAVERN -> tavernScene.onEntityInteraction?.invoke(npc)
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
                                BarkEvent.NIB_I_WONDER_WHATS_IN_THIS_ONE
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
                    title    = "Quest Status: Resolved.",
                    subtitle = "Party: ${director.partyName ?: "Unknown"}",
                    color    = Color(0xFF7FD17F),
                    onRestart = onReset
                )

            SlicePhase.GAME_OVER ->
                EndView(
                    title    = "Quest Status: Unresolved.",
                    subtitle = "The Questbook notes your failure for administrative purposes.",
                    color    = Color(0xFFE53935),
                    onRestart = onReset
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
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xF5241E12)),
        contentAlignment = Alignment.Center
    ) {
        Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Official Registry of Heroes", color = Color(0xFFE8C170), fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(16.dp))
            Text("Page 1:", color = Color(0xFFF5E9C8), fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Text(partyName, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Spacer(Modifier.height(8.dp))
            Text("(This page cannot be unread.)", color = Color(0xFFB0A8D0), fontSize = 12.sp)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A4A6B))
            ) {
                Text("Close Questbook", color = Color.White)
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
