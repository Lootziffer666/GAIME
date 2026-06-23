package ui.rpg

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gaime.resources.Res
import gaime.resources.hero_nib
import gaime.resources.tileset_dungeon
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.imageResource
import rpg.BarkOutcome
import rpg.SliceDirector
import rpg.bark.BarkEvent
import rpg.world.Direction
import rpg.world.GameMaps
import rpg.world.GridWorld
import rpg.questbook.QuestPressure
import rpg.questbook.RoomContext
import ui.GameCanvas
import kotlin.time.TimeSource

/**
 * M2 walkable map demo: move the party around the tavern with the D-pad (or
 * WASD/arrows on desktop). Reaching the cellar door fires a BarkEvent through
 * the M1 pipeline, so the Questbook reacts live -- proving the map layer is
 * wired to the engine.
 */
@Composable
fun RpgWorldScreen() {
    val tileset = imageResource(Res.drawable.tileset_dungeon)
    val playerSprite = imageResource(Res.drawable.hero_nib)

    val timeStart = remember { TimeSource.Monotonic.markNow() }
    val director = remember {
        SliceDirector { timeStart.elapsedNow().inWholeMilliseconds }.also {
            it.enterRoom(RoomContext("tavern", RoomContext.ROOM_TAVERN, hasInteractableTarget = true))
        }
    }

    val flash = remember { mutableStateOf<String?>(null) }
    val pressure = remember { mutableStateOf(director.pressure) }
    val hint = remember { mutableStateOf("Walk to the cellar door (the red door below).") }

    val scene = remember(tileset, playerSprite) {
        WorldScene(GridWorld(GameMaps.tavern()), tileset, playerSprite).also { s ->
            s.onTrigger = { id ->
                if (id == GameMaps.TRIGGER_CELLAR_DOOR) {
                    when (val outcome = director.fireBark(BarkEvent.NIB_SMELL_TREASURE)) {
                        is BarkOutcome.Fired -> {
                            flash.value = outcome.reaction.questbookText
                            hint.value = "The Questbook reacted! A quest marker appears on the cellar door."
                        }
                        is BarkOutcome.Suppressed -> { /* on cooldown */ }
                    }
                    pressure.value = director.pressure
                }
            }
        }
    }

    LaunchedEffect(flash.value) {
        if (flash.value != null) {
            delay(3000)
            flash.value = null
        }
    }

    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF15131F))) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Explore -- The Limping Cockatrice",
                color = Color(0xFFE8C170),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(6.dp))
            PressureChip(pressure.value)
            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier.fillMaxWidth().weight(1f)
                    .focusRequester(focus)
                    .focusable()
                    .onKeyEvent { e ->
                        if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                        val dir = when (e.key) {
                            Key.W, Key.DirectionUp -> Direction.UP
                            Key.S, Key.DirectionDown -> Direction.DOWN
                            Key.A, Key.DirectionLeft -> Direction.LEFT
                            Key.D, Key.DirectionRight -> Direction.RIGHT
                            else -> null
                        }
                        if (dir != null) { scene.world.requestStep(dir); true } else false
                    }
            ) {
                GameCanvas(scene = scene, isActive = true, modifier = Modifier.fillMaxSize())
            }

            Spacer(Modifier.height(8.dp))
            DPad { scene.world.requestStep(it) }
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF000000), modifier = Modifier.fillMaxWidth()) {
                Text(hint.value, color = Color(0xFFE0E0E0), fontSize = 12.sp, modifier = Modifier.padding(10.dp))
            }
        }

        flash.value?.let { text ->
            Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.TopCenter) {
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
}

@Composable
private fun PressureChip(pressure: QuestPressure) {
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
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun DPad(onStep: (Direction) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PadButton("▲") { onStep(Direction.UP) }
        Row {
            PadButton("◀") { onStep(Direction.LEFT) }
            Spacer(Modifier.width(48.dp))
            PadButton("▶") { onStep(Direction.RIGHT) }
        }
        PadButton("▼") { onStep(Direction.DOWN) }
    }
}

@Composable
private fun PadButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A4A6B)),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.padding(2.dp).size(48.dp)
    ) {
        Text(label, color = Color.White, fontSize = 18.sp)
    }
}
