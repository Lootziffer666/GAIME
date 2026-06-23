package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import core.GameEvent
import core.GameStateMachine

@Composable
fun WaitroomScreen() {
    val stateMachine = remember { GameStateMachine() }
    var currentState by remember { mutableStateOf(stateMachine.currentState) }
    var kiIsReady by remember { mutableStateOf(stateMachine.kiIsReady) }

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

        Spacer(modifier = Modifier.height(24.dp))

        // Placeholder Box for future canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Canvas Placeholder",
                color = Color.White
            )
        }
    }
}
