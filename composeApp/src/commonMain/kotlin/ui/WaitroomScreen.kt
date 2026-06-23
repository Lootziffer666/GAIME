package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import core.GameEvent
import core.GameSessionState
import core.GameStateMachine
import engine.SceneEngine
import engine.scenes.Hd2dDemoScene
import engine.scenes.LetterSwarmScene
import engine.scenes.SpriteIdleScene

@Composable
fun WaitroomScreen(
    lifecycleActive: Boolean = true
) {
    val stateMachine = remember { GameStateMachine() }
    var currentState by remember { mutableStateOf(stateMachine.currentState) }
    var kiIsReady by remember { mutableStateOf(stateMachine.kiIsReady) }

    val textMeasurer = rememberTextMeasurer()

    val sceneEngine = remember { SceneEngine() }

    // Register scenes after textMeasurer is available
    LaunchedEffect(textMeasurer) {
        val letterSwarm = LetterSwarmScene(textMeasurer)
        val spriteIdle = SpriteIdleScene()
        val hd2dDemo = Hd2dDemoScene()
        sceneEngine.register(letterSwarm)
        sceneEngine.register(spriteIdle)
        sceneEngine.register(hd2dDemo)
        sceneEngine.switchTo("HD2D-A")
    }

    // Derive whether the scene should be running based on state and lifecycle
    val sceneIsActive = lifecycleActive && (
        currentState == GameSessionState.Thinking ||
        currentState == GameSessionState.ReadyButPlaying
    )

    fun processEvent(event: GameEvent) {
        stateMachine.transition(event)
        currentState = stateMachine.currentState
        kiIsReady = stateMachine.kiIsReady
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "State: $currentState",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "KI Ready: $kiIsReady",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Prominent "KI ist bereit!" banner - shown when KI is ready and game is still running
        if (kiIsReady && currentState == GameSessionState.ReadyButPlaying) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF4CAF50),
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "KI ist bereit! Spiel laeuft weiter...",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { processEvent(GameEvent.AiStartedThinking) }) {
                Text("Start Thinking")
            }
            Button(onClick = { processEvent(GameEvent.AiFinishedThinking) }) {
                Text("Finish Thinking")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { processEvent(GameEvent.GameOver) }) {
                Text("Game Over")
            }
            Button(onClick = { processEvent(GameEvent.PlayerClosedGame) }) {
                Text("Close Game")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { processEvent(GameEvent.Reset) }) {
            Text("Reset")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scene Picker
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val currentSceneName = sceneEngine.currentScene?.name
            Button(
                onClick = { sceneEngine.switchTo("LetterSwarm") },
                colors = if (currentSceneName == "LetterSwarm")
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                else
                    ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("LetterSwarm")
            }
            Button(
                onClick = { sceneEngine.switchTo("SpriteIdle") },
                colors = if (currentSceneName == "SpriteIdle")
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                else
                    ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("SpriteIdle")
            }
            Button(
                onClick = { sceneEngine.switchTo("HD2D-A") },
                colors = if (currentSceneName == "HD2D-A")
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                else
                    ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("HD-2D Demo")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Game Canvas - animation driven by state machine
        GameCanvas(
            scene = sceneEngine.currentScene,
            isActive = sceneIsActive,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}
