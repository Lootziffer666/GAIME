package engine.scenes

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import engine.Scene
import engine.SpriteAnimator
import engine.SpriteFrame

class SpriteIdleScene : Scene {

    override val name: String = "SpriteIdle"

    private val frameColors = listOf(
        Color(0xFF_E0_44_4E), // red
        Color(0xFF_F4_A2_61), // orange
        Color(0xFF_F7_D7_94), // yellow
        Color(0xFF_4E_C9_B0), // green
        Color(0xFF_56_9C_D6), // blue
        Color(0xFF_C5_86_C0), // purple
    )

    private val animator = SpriteAnimator(
        frames = frameColors.mapIndexed { index, _ ->
            SpriteFrame(index = index, duration = 0.15f)
        },
        looping = true
    )

    override fun update(deltaTime: Float) {
        animator.update(deltaTime)
    }

    override fun draw(drawScope: DrawScope) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        val rectWidth = width * 0.4f
        val rectHeight = height * 0.4f
        val x = (width - rectWidth) / 2f
        val y = (height - rectHeight) / 2f

        val color = frameColors[animator.currentFrameIndex % frameColors.size]

        drawScope.drawRect(
            color = color,
            topLeft = Offset(x, y),
            size = Size(rectWidth, rectHeight)
        )

        // Draw a small frame indicator
        val indicatorSize = 12f
        for (i in frameColors.indices) {
            val indicatorColor = if (i == animator.currentFrameIndex) Color.White else Color.Gray
            drawScope.drawRect(
                color = indicatorColor,
                topLeft = Offset(x + i * (indicatorSize + 4f), y + rectHeight + 16f),
                size = Size(indicatorSize, indicatorSize)
            )
        }
    }

    override fun onPointerMove(x: Float, y: Float) {
        // SpriteIdle does not react to pointer
    }

    override fun onPointerExit() {
        // SpriteIdle does not react to pointer
    }
}
