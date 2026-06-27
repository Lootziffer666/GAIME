package ui.rpg

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gaime.resources.Res
import gaime.resources.boss_rat_accountant
import gaime.resources.enemy_rat
import gaime.resources.hero_brugg
import gaime.resources.hero_nib
import gaime.resources.hero_vellum
import gaime.resources.marker_quest
import gaime.resources.questbook_open
import gaime.resources.tile_floor
import gaime.resources.tile_wall
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import rpg.BarkOutcome
import rpg.SliceDirector
import rpg.bark.BarkEvent
import rpg.bark.BarkRegistry
import rpg.combat.BossController
import rpg.combat.CombatAction
import rpg.combat.CombatEngine
import rpg.combat.CombatEvent
import rpg.combat.CombatResult
import rpg.combat.Combatant
import rpg.combat.EnemyArchetype
import rpg.combat.Side
import rpg.questbook.QuestPressure
import rpg.questbook.RoomContext
import kotlin.time.TimeSource

/**
 * Interactive demo proving the bark -> Questbook -> effect pipeline plus the
 * deterministic combat core, rendered with real (Kenney tiny-dungeon) sprites.
 * This is the M1 "vertical slice in a box": no tilemap/movement layer yet.
 */
@Composable
fun RpgDemoScreen() {
    val timeStart = remember { TimeSource.Monotonic.markNow() }
    val director = remember { SliceDirector { timeStart.elapsedNow().inWholeMilliseconds } }

    // Recomposition trigger: bumped after every mutation of the director.
    var version by remember { mutableStateOf(0) }
    fun refresh() { version++ }

    var flashText by remember { mutableStateOf<String?>(null) }
    var dialogue by remember { mutableStateOf("The Questbook sits on the table, glowing faintly.") }

    // Auto-dismiss the Questbook flash overlay after a moment.
    LaunchedEffect(flashText) {
        if (flashText != null) {
            delay(2600)
            flashText = null
        }
    }

    fun fire(bark: BarkEvent) {
        when (val outcome = director.fireBark(bark)) {
            is BarkOutcome.Fired -> {
                flashText = outcome.reaction.questbookText
                dialogue = "${BarkRegistry[bark].character}: \"${BarkRegistry[bark].audioText}\""
            }
            is BarkOutcome.Suppressed -> {
                dialogue = "(${bark} is on cooldown -- ${outcome.remainingMillis / 1000}s left)"
            }
        }
        refresh()
    }

    fun startRoom(ctx: RoomContext, intro: String) {
        director.enterRoom(ctx)
        dialogue = intro
        refresh()
    }

    fun startRatFight() {
        val party = freshParty()
        val rats = listOf(
            EnemyArchetype.SEWER_RAT.spawn("rat_1"),
            EnemyArchetype.SEWER_RAT.spawn("rat_2")
        )
        director.startCombat(CombatEngine(party, rats))
        dialogue = "Two Sewer Rats block the path!"
        refresh()
    }

    fun startBossFight() {
        val party = freshParty()
        val boss = EnemyArchetype.RAT_ACCOUNTANT.spawn("rat_accountant")
        director.startCombat(CombatEngine(party, emptyList(), boss, BossController()))
        dialogue = "The Rat Accountant looks up from its desk of garbage."
        refresh()
    }

    fun combat(action: CombatAction) {
        val turn = director.combatAction(action)
        val lastMsg = turn.events.filterIsInstance<CombatEvent.Message>().lastOrNull()?.text
        if (lastMsg != null) dialogue = lastMsg
        turn.events.filterIsInstance<CombatEvent.BarkTriggered>().lastOrNull()?.let {
            flashText = director.questbook.log.lastOrNull()?.questbookText
        }
        refresh()
    }

    // version is read here so the whole screen recomposes on refresh().
    @Suppress("UNUSED_EXPRESSION") version

    val scroll = rememberScrollState()
    Box(modifier = Modifier.fillMaxSize().background(
        Brush.verticalGradient(listOf(Color(0xFF100A1E), Color(0xFF1A1726)))
    )) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(scroll),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "GAIME -- Quest Accepted: Unfortunately",
                color = Color(0xFFE8C170),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(Modifier.height(8.dp))

            PressureIndicator(director.pressure)
            Spacer(Modifier.height(10.dp))

            RoomScene(director)
            Spacer(Modifier.height(10.dp))

            RoomButtons(::startRoom)
            Spacer(Modifier.height(8.dp))

            BarkButtons(::fire)
            Spacer(Modifier.height(10.dp))

            CombatPanel(
                engine = director.currentCombat,
                onStartRats = ::startRatFight,
                onStartBoss = ::startBossFight,
                onAction = ::combat
            )
            Spacer(Modifier.height(10.dp))

            DialogueBox(dialogue)
            Spacer(Modifier.height(8.dp))

            director.partyName?.let {
                Text(
                    "Official Party Name: \"$it\"",
                    color = Color(0xFF7FD17F),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        flashText?.let { QuestbookFlash(it) }
    }
}

private fun freshParty(): List<Combatant> = listOf(
    Combatant("nib", "Nib", maxHp = 20, side = Side.PLAYER, attackPower = 4),
    Combatant("brugg", "Brugg", maxHp = 30, side = Side.PLAYER, attackPower = 5),
    Combatant("vellum", "Vellum", maxHp = 18, side = Side.PLAYER, attackPower = 4)
)

@Composable
private fun PressureIndicator(pressure: QuestPressure) {
    val color = when (pressure) {
        QuestPressure.LOW -> Color(0xFF4CAF50)
        QuestPressure.MEDIUM -> Color(0xFFFFB300)
        QuestPressure.HIGH -> Color(0xFFE53935)
    }
    Surface(shape = RoundedCornerShape(8.dp), color = color) {
        Text(
            "QUEST PRESSURE: $pressure",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun RoomScene(director: SliceDirector) {
    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF2B2640)) {
        Column(Modifier.padding(10.dp)) {
            Text("Room: ${director.context.roomId}", color = Color(0xFFB0A8D0), fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            // 5×4 tile grid: wall border, floor interior, heroes in row 2
            val tileSize = 36
            val hasMarker = director.questMarkers.isNotEmpty()
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                for (row in 0..3) {
                    Row {
                        for (col in 0..6) {
                            val isWall = row == 0 || row == 3 || col == 0 || col == 6
                            val isHero = row == 2 && col in 2..4
                            val isMarker = hasMarker && row == 1 && col == 5
                            when {
                                isMarker -> Sprite(Res.drawable.marker_quest, tileSize)
                                isHero   -> when (col) {
                                    2 -> Sprite(Res.drawable.hero_nib,    tileSize)
                                    3 -> Sprite(Res.drawable.hero_brugg,  tileSize)
                                    else -> Sprite(Res.drawable.hero_vellum, tileSize)
                                }
                                isWall   -> Sprite(Res.drawable.tile_wall,  tileSize)
                                else     -> Sprite(Res.drawable.tile_floor, tileSize)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Markers: ${director.questMarkers.size}  •  " +
                    "False markers: ${director.falseMarkers.size}  •  " +
                    "Cleared: ${director.clearedObstacles.size}",
                color = Color(0xFFB0A8D0),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun RoomButtons(onRoom: (RoomContext, String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Travel", color = Color(0xFF8C84B0), fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SmallButton("Tavern") {
                onRoom(
                    RoomContext("tavern", RoomContext.ROOM_TAVERN, hasInteractableTarget = true),
                    "The Limping Cockatrice. The Questbook glows on the table."
                )
            }
            SmallButton("Corridor") {
                onRoom(
                    RoomContext("sewer", RoomContext.ROOM_SEWER_CORRIDOR, hasEnemies = true),
                    "Sewers of Bad Decisions. A narrow corridor."
                )
            }
            SmallButton("Mini-Dungeon") {
                onRoom(
                    RoomContext(
                        "sewer", RoomContext.ROOM_MINI_DUNGEON,
                        hasPuzzleElement = true, hasBreakableObstacle = true
                    ),
                    "A larger room. Rubble blocks one exit."
                )
            }
            SmallButton("Boss") {
                onRoom(
                    RoomContext("sewer", RoomContext.ROOM_BOSS, hasFlammableTarget = true),
                    "The Rat Accountant's office."
                )
            }
        }
    }
}

@Composable
private fun BarkButtons(onFire: (BarkEvent) -> Unit) {
    val sliceBarks = BarkRegistry.all().filter { it.usedInSlice }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Barks (slice)", color = Color(0xFF8C84B0), fontSize = 12.sp)
        FlowBarkRow(sliceBarks.map { it.key to it.key.name }, onFire)
    }
}

@Composable
private fun FlowBarkRow(items: List<Pair<BarkEvent, String>>, onFire: (BarkEvent) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { (bark, label) ->
                    SmallButton(label.replace("_", " "), color = Color(0xFF4A3F73)) { onFire(bark) }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun CombatPanel(
    engine: CombatEngine?,
    onStartRats: () -> Unit,
    onStartBoss: () -> Unit,
    onAction: (CombatAction) -> Unit
) {
    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF2B2640)) {
        Column(Modifier.padding(10.dp).fillMaxWidth()) {
            Text("Combat", color = Color(0xFFE8C170), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallButton("Start Rat Fight", onClick = onStartRats)
                SmallButton("Start Boss Fight", onClick = onStartBoss)
            }
            if (engine != null) {
                Spacer(Modifier.height(8.dp))
                engine.bossPhase?.let {
                    Text("Boss phase: $it", color = Color(0xFFE53935), fontSize = 12.sp)
                }
                Text("Enemies", color = Color(0xFFB0A8D0), fontSize = 12.sp)
                engine.enemies.forEach { e ->
                    val res = if (e.name.contains("Accountant")) Res.drawable.boss_rat_accountant else Res.drawable.enemy_rat
                    HpRow(res, e.name, e.hp, e.maxHp, Color(0xFFE53935))
                }
                Spacer(Modifier.height(4.dp))
                Text("Party", color = Color(0xFFB0A8D0), fontSize = 12.sp)
                engine.party.forEach { p ->
                    val res = when (p.id) {
                        "brugg" -> Res.drawable.hero_brugg
                        "vellum" -> Res.drawable.hero_vellum
                        else -> Res.drawable.hero_nib
                    }
                    HpRow(res, p.name, p.hp, p.maxHp, Color(0xFF4CAF50))
                }

                Spacer(Modifier.height(8.dp))
                when (engine.result) {
                    CombatResult.ONGOING -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val target = engine.livingEnemies().firstOrNull()?.id
                        SmallButton("Attack") { target?.let { onAction(CombatAction.Attack(it)) } }
                        SmallButton("Dodge") { onAction(CombatAction.Dodge) }
                        SmallButton("Flame", color = Color(0xFF4A3F73)) {
                            onAction(CombatAction.UtilityBark(BarkEvent.VELLUM_CALLS_FOR_FLAME))
                        }
                    }
                    CombatResult.VICTORY -> Text("VICTORY", color = Color(0xFF7FD17F), fontWeight = FontWeight.Bold)
                    CombatResult.DEFEAT -> Text(
                        "DEFEAT -- Quest Status: Unresolved.",
                        color = Color(0xFFE53935),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun HpRow(res: DrawableResource, name: String, hp: Int, maxHp: Int, barColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
        Sprite(res, 48)
        Spacer(Modifier.width(8.dp))
        Text(name, color = Color.White, fontSize = 12.sp, modifier = Modifier.width(90.dp))
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

@Composable
private fun DialogueBox(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF000000),
        modifier = Modifier.fillMaxWidth()
            .border(2.dp, Color(0xFF5A5090), RoundedCornerShape(8.dp))
    ) {
        Text(
            text,
            color = Color(0xFFE0E0E0),
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun QuestbookFlash(text: String) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.TopCenter) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xF2241E12),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(Res.drawable.questbook_open),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("QUESTBOOK", color = Color(0xFFE8C170), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(text, color = Color(0xFFF5E9C8), fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun Sprite(res: DrawableResource, size: Int) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(size.dp)
    )
}

@Composable
private fun SmallButton(label: String, color: Color = Color(0xFF3A4A6B), onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 12.sp, color = Color.White)
    }
}
