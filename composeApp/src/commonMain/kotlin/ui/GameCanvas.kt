package ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import engine.Scene
import kotlinx.coroutines.isActive

@Composable
fun GameCanvas(
    scene: Scene?,
    modifier: Modifier = Modifier
) {
    var lastFrameTime by remember { mutableStateOf(0L) }

    // Frame loop
    LaunchedEffect(scene) {
        lastFrameTime = 0L
        while (isActive) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameTime != 0L) {
                    val deltaTime = (frameTimeNanos - lastFrameTime) / 1_000_000_000f
                    scene?.update(deltaTime)
                }
                lastFrameTime = frameTimeNanos
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(scene) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Move, PointerEventType.Enter -> {
                                val position = event.changes.firstOrNull()?.position
                                if (position != null) {
                                    scene?.onPointerMove(position.x, position.y)
                                }
                            }
                            PointerEventType.Exit -> {
                                scene?.onPointerExit()
                            }
                        }
                    }
                }
            }
    ) {
        scene?.draw(this)
    }
}
