package engine.scenes

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import engine.Scene
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * VARIANT A - HD-2D look faked inside Compose Canvas.
 *
 * This is a pure-procedural showcase (no sprite assets required to compile) that
 * demonstrates how far the existing Compose Canvas engine can be pushed toward an
 * Octopath-Traveler "HD-2D" feeling using only 2D draw primitives:
 *
 *   1. Parallax depth      - 4 layers scroll at different speeds for pseudo-3D depth.
 *   2. Diegetic lighting    - warm radial "lamp" + pulsing Questbook glow (screen blend).
 *   3. Atmosphere particles - drifting dust motes / fireflies catch the light.
 *   4. Tilt-shift fake DoF   - top & bottom gradient bands simulate depth-of-field blur.
 *   5. Vignette + scanlines  - cinematic framing and retro CRT texture.
 *
 * Limitations vs. true HD-2D: there is no real depth buffer, no bloom on emissive
 * pixels, no volumetric light. It is a convincing *stylisation*, not 3D rendering.
 */
class Hd2dDemoScene : Scene {

    override val name: String = "HD2D-A"

    private var time = 0f
    private var pointerX = 0.5f   // normalised 0..1, drives parallax camera sway
    private var pointerActive = false
    private var lastWidth = 1000f // captured during draw for accurate pointer normalisation
    private var lastHeight = 720f

    // Dust motes / fireflies: depth in 0..1 (0 = far, 1 = near)
    private data class Mote(
        var x: Float,
        var y: Float,
        val depth: Float,
        val speed: Float,
        val phase: Float,
        val radius: Float
    )

    private val motes = mutableListOf<Mote>()
    private var initialized = false

    private fun init(w: Float, h: Float) {
        if (initialized) return
        initialized = true
        repeat(60) {
            val depth = Random.nextFloat()
            motes.add(
                Mote(
                    x = Random.nextFloat() * w,
                    y = Random.nextFloat() * h,
                    depth = depth,
                    speed = 6f + depth * 22f,
                    phase = Random.nextFloat() * (2f * PI.toFloat()),
                    radius = 1.2f + depth * 2.8f
                )
            )
        }
    }

    override fun update(deltaTime: Float) {
        val dt = deltaTime.coerceAtMost(0.05f)
        time += dt
        for (m in motes) {
            // gentle upward drift with horizontal sine wander
            m.y -= m.speed * dt
            m.x += sin(time * 0.6f + m.phase) * 8f * dt
            if (m.y < -8f) m.y = lastHeight + Random.nextFloat() * 40f
        }
    }

    override fun draw(drawScope: DrawScope) {
        val w = drawScope.size.width
        val h = drawScope.size.height
        init(w, h)
        lastWidth = w
        lastHeight = h

        // Camera sway: parallax offset driven by pointer (or a slow auto-pan idle).
        val camX = if (pointerActive) (pointerX - 0.5f) else sin(time * 0.25f) * 0.35f

        drawSky(drawScope, w, h)
        drawParallaxLayer(drawScope, w, h, depth = 0.15f, camX, baseY = h * 0.55f,
            color = Color(0xFF_2B_3A_67), hills = 5, amp = h * 0.10f)
        drawParallaxLayer(drawScope, w, h, depth = 0.35f, camX, baseY = h * 0.65f,
            color = Color(0xFF_34_4E_7A), hills = 4, amp = h * 0.14f)
        drawBuildings(drawScope, w, h, depth = 0.55f, camX)
        drawGround(drawScope, w, h)

        drawQuestbookGlow(drawScope, w, h)
        drawLampLight(drawScope, w, h)
        drawMotes(drawScope)

        drawTiltShift(drawScope, w, h)
        drawVignette(drawScope, w, h)
        drawScanlines(drawScope, w, h)
    }

    // --- Background layers -------------------------------------------------

    private fun drawSky(ds: DrawScope, w: Float, h: Float) {
        ds.drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF_10_0A_1E),   // deep indigo top
                0.6f to Color(0xFF_2A_1B_3D), // dusk purple
                1f to Color(0xFF_5A_2E_3E)    // warm horizon glow
            ),
            size = Size(w, h)
        )
    }

    private fun drawParallaxLayer(
        ds: DrawScope, w: Float, h: Float,
        depth: Float, camX: Float, baseY: Float,
        color: Color, hills: Int, amp: Float
    ) {
        val shift = -camX * w * depth
        val step = w / hills
        for (i in -1..hills) {
            val cx = shift + i * step + step / 2f
            val peakY = baseY - amp * (0.6f + 0.4f * sin(i * 1.7f))
            // crude hill = triangle-ish via overlapping circles
            ds.drawCircle(
                color = color,
                radius = step * 0.85f,
                center = Offset(cx, peakY + amp)
            )
        }
        ds.drawRect(
            color = color,
            topLeft = Offset(0f, baseY + amp),
            size = Size(w, h - baseY)
        )
    }

    private fun drawBuildings(ds: DrawScope, w: Float, h: Float, depth: Float, camX: Float) {
        val shift = -camX * w * depth
        val color = Color(0xFF_1C_12_24)
        val winColor = Color(0xFF_FF_C2_6B)
        val baseY = h * 0.70f
        val bw = w / 6f
        for (i in -1..6) {
            val bx = shift + i * bw
            val bh = (h * 0.18f) + ((i * 53) % 7) * (h * 0.02f)
            ds.drawRect(color, topLeft = Offset(bx, baseY - bh), size = Size(bw * 0.82f, bh))
            // lit windows
            for (wy in 0 until 3) {
                for (wx in 0 until 2) {
                    val lit = ((i + wx * 3 + wy * 7) % 4) == 0
                    if (lit) {
                        ds.drawRect(
                            color = winColor.copy(alpha = 0.85f),
                            topLeft = Offset(bx + 10f + wx * (bw * 0.32f), baseY - bh + 14f + wy * 22f),
                            size = Size(bw * 0.18f, 12f)
                        )
                    }
                }
            }
        }
    }

    private fun drawGround(ds: DrawScope, w: Float, h: Float) {
        val groundTop = h * 0.70f
        ds.drawRect(
            brush = Brush.verticalGradient(
                groundTop to Color(0xFF_15_0E_12),
                h to Color(0xFF_0A_07_0A)
            ),
            topLeft = Offset(0f, groundTop),
            size = Size(w, h - groundTop)
        )
        // cobblestone hint: faint horizontal perspective lines
        var y = groundTop + 12f
        var gap = 10f
        while (y < h) {
            ds.drawLine(
                color = Color(0xFF_2A_1E_22).copy(alpha = 0.5f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.5f
            )
            gap *= 1.18f
            y += gap
        }
    }

    // --- Lighting (screen / additive blend) --------------------------------

    private fun drawQuestbookGlow(ds: DrawScope, w: Float, h: Float) {
        // The glowing Questbook sits center-stage on the "table".
        val pulse = 0.6f + 0.4f * sin(time * 2.2f)
        val center = Offset(w * 0.5f, h * 0.74f)
        ds.drawCircle(
            brush = Brush.radialGradient(
                0f to Color(0xFF_7B_E0_FF).copy(alpha = 0.55f * pulse),
                0.5f to Color(0xFF_3A_8F_E0).copy(alpha = 0.25f * pulse),
                1f to Color.Transparent,
                center = center,
                radius = w * 0.22f
            ),
            radius = w * 0.22f,
            center = center,
            blendMode = BlendMode.Screen
        )
    }

    private fun drawLampLight(ds: DrawScope, w: Float, h: Float) {
        // Warm tavern lamp, upper-left, with a soft flicker.
        val flicker = 0.85f + 0.15f * sin(time * 9f) * sin(time * 3.3f)
        val center = Offset(w * 0.22f, h * 0.30f)
        ds.drawCircle(
            brush = Brush.radialGradient(
                0f to Color(0xFF_FF_D9_8A).copy(alpha = 0.45f * flicker),
                1f to Color.Transparent,
                center = center,
                radius = w * 0.45f
            ),
            radius = w * 0.45f,
            center = center,
            blendMode = BlendMode.Screen
        )
    }

    private fun drawMotes(ds: DrawScope) {
        for (m in motes) {
            val twinkle = 0.4f + 0.6f * sin(time * 3f + m.phase)
            ds.drawCircle(
                color = Color(0xFF_FF_E9_B0).copy(alpha = (0.15f + m.depth * 0.55f) * twinkle),
                radius = m.radius,
                center = Offset(m.x, m.y),
                blendMode = BlendMode.Screen
            )
        }
    }

    // --- Post-processing overlays ------------------------------------------

    private fun drawTiltShift(ds: DrawScope, w: Float, h: Float) {
        // Fake depth-of-field: darken+desaturate top and bottom bands.
        ds.drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF_0A_06_12).copy(alpha = 0.55f),
                0.18f to Color.Transparent
            ),
            size = Size(w, h)
        )
        ds.drawRect(
            brush = Brush.verticalGradient(
                0.82f to Color.Transparent,
                1f to Color(0xFF_0A_06_12).copy(alpha = 0.6f)
            ),
            size = Size(w, h)
        )
    }

    private fun drawVignette(ds: DrawScope, w: Float, h: Float) {
        val r = maxOf(w, h) * 0.75f
        ds.drawRect(
            brush = Brush.radialGradient(
                0.55f to Color.Transparent,
                1f to Color(0xFF_00_00_00).copy(alpha = 0.7f),
                center = Offset(w / 2f, h / 2f),
                radius = r
            ),
            size = Size(w, h)
        )
    }

    private fun drawScanlines(ds: DrawScope, w: Float, h: Float) {
        var y = 0f
        while (y < h) {
            ds.drawLine(
                color = Color(0xFF_00_00_00).copy(alpha = 0.10f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
            y += 3f
        }
    }

    override fun onPointerMove(x: Float, y: Float) {
        pointerActive = true
        pointerX = (x / lastWidth).coerceIn(0f, 1f)
    }

    override fun onPointerExit() {
        pointerActive = false
    }
}
