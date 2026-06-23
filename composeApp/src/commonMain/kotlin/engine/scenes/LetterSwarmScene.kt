package engine.scenes

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import engine.Particle
import engine.ParticleEngine
import engine.Personality
import engine.Scene
import kotlin.random.Random

class LetterSwarmScene(
    private val textMeasurer: TextMeasurer
) : Scene {

    override val name: String = "LetterSwarm"

    private val text = "Thinking is happening. Please play responsibly."
    private val particleEngine = ParticleEngine()
    private var initialized = false

    private fun initParticles(canvasWidth: Float, canvasHeight: Float) {
        if (initialized) return
        initialized = true

        val chars = text.toCharArray()
        val startX = 40f
        val startY = canvasHeight / 2f
        val spacing = 12f

        chars.forEachIndexed { index, char ->
            if (char == ' ') return@forEachIndexed // skip spaces for particles

            val originX = startX + (index * spacing) % (canvasWidth - 80f)
            val row = (index * spacing) / (canvasWidth - 80f)
            val originY = startY + row.toInt() * 30f

            val personality = when (Random.nextInt(3)) {
                0 -> Personality.Nervous
                1 -> Personality.Curious
                else -> Personality.Sleepy
            }

            particleEngine.particles.add(
                Particle(
                    char = char,
                    x = originX + (Random.nextFloat() - 0.5f) * 20f,
                    y = originY + (Random.nextFloat() - 0.5f) * 20f,
                    originX = originX,
                    originY = originY,
                    personality = personality
                )
            )
        }
    }

    override fun update(deltaTime: Float) {
        particleEngine.update(deltaTime)
    }

    override fun draw(drawScope: DrawScope) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        initParticles(width, height)

        for (particle in particleEngine.particles) {
            val color = when (particle.personality) {
                Personality.Nervous -> Color(0xFF_FF_6B_6B)
                Personality.Curious -> Color(0xFF_4E_C9_B0)
                Personality.Sleepy -> Color(0xFF_7B_A2_D8)
            }

            val style = TextStyle(
                color = color.copy(alpha = particle.alpha),
                fontSize = particle.size.sp
            )

            drawScope.drawText(
                textMeasurer = textMeasurer,
                text = particle.char.toString(),
                topLeft = Offset(particle.x, particle.y),
                style = style
            )
        }
    }

    override fun onPointerMove(x: Float, y: Float) {
        particleEngine.setPointer(x, y)
    }

    override fun onPointerExit() {
        particleEngine.clearPointer()
    }
}
