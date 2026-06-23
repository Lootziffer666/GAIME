package ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import core.GameEvent
import core.GameStateMachine
import engine.SceneEngine
import engine.scenes.LetterSwarmScene
import engine.scenes.SpriteIdleScene

@Composable
fun WaitroomScreen() {
    val stateMachine = remember { GameStateMachine() }
    var currentState by remember { mutableStateOf(stateMachine.currentState) }
    var kiIsReady by remember { mutableStateOf(stateMachine.kiIsReady) }

    val textMeasurer = rememberTextMeasurer()

    val sceneEngine = remember { SceneEngine() }

    // Register scenes after textMeasurer is available
    LaunchedEffect(textMeasurer) {
        val letterSwarm = LetterSwarmScene(textMeasurer)
        val spriteIdle = SpriteIdleScene()
        sceneEngine.register(letterSwarm)
        sceneEngine.register(spriteIdle)
        sceneEngine.switchTo("LetterSwarm")
    }

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

        Spacer(modifier = Modifier.height(24.dp))

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
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Game Canvas
        GameCanvas(
            scene = sceneEngine.currentScene,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}
