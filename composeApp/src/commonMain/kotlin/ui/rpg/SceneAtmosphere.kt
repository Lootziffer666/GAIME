package ui.rpg

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The cinematic post-processing layer that lifts the flat pixel art toward an
 * "Odd Tales / The Last Night" look — i.e. the magic isn't the sprites, it's the
 * real-time layer on top: colour grade, volumetric light shafts, flickering
 * bloom lights, drifting atmosphere particles (dust / embers), rolling fog and
 * film grain. All drawn in Compose [DrawScope] (no shaders), time-driven so it
 * breathes. Configured per scene via [SceneAtmosphere] presets.
 */

/** A soft, optionally-flickering point light that blooms additively. */
data class AccentLight(
    val xFrac: Float,
    val yFrac: Float,
    val color: Color,
    val radiusFrac: Float,
    val intensity: Float = 0.5f,
    val flicker: Float = 0f // 0 = steady, 1 = candle-like
)

/** A volumetric god-ray beam. */
data class LightShaft(
    val xFrac: Float,
    val angleDeg: Float,
    val color: Color,
    val widthFrac: Float,
    val intensity: Float
)

enum class MoteStyle { DUST, EMBER, FOG_FLECK }

data class SceneAtmosphere(
    val grade: Color,
    val gradeStrength: Float,
    val keyLight: Color,
    val keyIntensity: Float,
    val accents: List<AccentLight> = emptyList(),
    val shaft: LightShaft? = null,
    val moteColor: Color = Color.White,
    val moteCount: Int = 40,
    val moteStyle: MoteStyle = MoteStyle.DUST,
    val fog: Color? = null,
    val fogStrength: Float = 0f,
    val grain: Float = 0.05f,
    val vignette: Float = 0.7f
) {
    companion object {
        /** Warm hearth, lazy dust motes, a shaft of daylight from a window. */
        val TAVERN = SceneAtmosphere(
            grade = Color(0xFFFFB347), gradeStrength = 0.16f,
            keyLight = Color(0xFFFFD98A), keyIntensity = 0.32f,
            accents = listOf(
                AccentLight(0.18f, 0.30f, Color(0xFFFF8A3C), 0.22f, 0.55f, flicker = 0.9f),
                AccentLight(0.84f, 0.28f, Color(0xFFFFC062), 0.18f, 0.40f, flicker = 0.7f)
            ),
            shaft = LightShaft(0.70f, 22f, Color(0xFFFFE9B0), 0.12f, 0.22f),
            moteColor = Color(0xFFFFE6B0), moteCount = 46, moteStyle = MoteStyle.DUST,
            grain = 0.045f, vignette = 0.66f
        )

        /** Cold drain light, blue fog rolling low, eerie flicker, drifting flecks. */
        val SEWER = SceneAtmosphere(
            grade = Color(0xFF2A4A6B), gradeStrength = 0.30f,
            keyLight = Color(0xFF3A8FE0), keyIntensity = 0.26f,
            accents = listOf(
                AccentLight(0.50f, 0.18f, Color(0xFF6FE0FF), 0.26f, 0.45f, flicker = 0.5f),
                AccentLight(0.20f, 0.65f, Color(0xFF2E7FBF), 0.20f, 0.30f, flicker = 0.3f)
            ),
            shaft = LightShaft(0.48f, 8f, Color(0xFFAEE6FF), 0.09f, 0.18f),
            moteColor = Color(0xFFBFE9FF), moteCount = 30, moteStyle = MoteStyle.FOG_FLECK,
            fog = Color(0xFF14283C), fogStrength = 0.40f,
            grain = 0.07f, vignette = 0.80f
        )

        /** Danger red, rising embers, hot pulsing core. */
        val BOSS = SceneAtmosphere(
            grade = Color(0xFFE53030), gradeStrength = 0.26f,
            keyLight = Color(0xFFFF5A3C), keyIntensity = 0.40f,
            accents = listOf(
                AccentLight(0.50f, 0.42f, Color(0xFFFF3030), 0.34f, 0.60f, flicker = 0.4f)
            ),
            moteColor = Color(0xFFFFB060), moteCount = 54, moteStyle = MoteStyle.EMBER,
            grain = 0.06f, vignette = 0.84f
        )

        /** Warm daylight market. */
        val MARKET = SceneAtmosphere(
            grade = Color(0xFFFFD27A), gradeStrength = 0.12f,
            keyLight = Color(0xFFFFE6A8), keyIntensity = 0.26f,
            shaft = LightShaft(0.40f, 18f, Color(0xFFFFF0C8), 0.14f, 0.20f),
            moteColor = Color(0xFFFFF0C8), moteCount = 38, moteStyle = MoteStyle.DUST,
            grain = 0.04f, vignette = 0.58f
        )

        /** Muted green canopy, light shafts through leaves, floating spores. */
        val FOREST = SceneAtmosphere(
            grade = Color(0xFF3FAE6B), gradeStrength = 0.20f,
            keyLight = Color(0xFF9BE0A0), keyIntensity = 0.24f,
            accents = listOf(
                AccentLight(0.30f, 0.22f, Color(0xFFCFFFB0), 0.20f, 0.35f, flicker = 0.2f)
            ),
            shaft = LightShaft(0.62f, 26f, Color(0xFFE6FFC0), 0.10f, 0.24f),
            moteColor = Color(0xFFD8FFC0), moteCount = 50, moteStyle = MoteStyle.DUST,
            fog = Color(0xFF14321E), fogStrength = 0.22f,
            grain = 0.05f, vignette = 0.7f
        )
    }
}

private fun hash(i: Int, seed: Float): Float {
    val v = sin(i * 12.9898f + seed * 78.233f) * 43758.547f
    return v - kotlin.math.floor(v)
}

/**
 * Draws the full cinematic stack for [atmo] at animation [time] (seconds) over a
 * viewport [w] x [h]. Order matters: grade -> fog -> shaft -> lights/bloom ->
 * motes -> tilt-shift -> vignette -> grain.
 */
fun DrawScope.drawAtmosphere(atmo: SceneAtmosphere, time: Float, w: Float, h: Float) {
    // 1) Colour grade — push the whole frame toward the scene mood.
    drawRect(color = atmo.grade.copy(alpha = atmo.gradeStrength), size = Size(w, h), blendMode = BlendMode.Overlay)

    // 2) Rolling fog (two slow horizontal bands drifting opposite ways, low).
    atmo.fog?.let { fog ->
        val drift = (sin(time * 0.20f) * 0.5f + 0.5f)
        drawRect(
            brush = Brush.verticalGradient(
                0.55f to Color.Transparent,
                1f to fog.copy(alpha = atmo.fogStrength)
            ),
            topLeft = Offset(-w * 0.1f + drift * w * 0.2f, 0f),
            size = Size(w * 1.2f, h)
        )
    }

    // 3) Volumetric light shaft — a soft rotated beam, gently breathing.
    atmo.shaft?.let { s ->
        val breathe = 0.75f + 0.25f * sin(time * 0.7f)
        val cx = w * s.xFrac
        rotate(degrees = s.angleDeg, pivot = Offset(cx, 0f)) {
            val bw = w * s.widthFrac
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.5f to s.color.copy(alpha = s.intensity * breathe),
                    1f to Color.Transparent,
                    startX = cx - bw, endX = cx + bw
                ),
                topLeft = Offset(cx - bw, -h * 0.3f),
                size = Size(bw * 2f, h * 1.8f),
                blendMode = BlendMode.Screen
            )
        }
    }

    // 4) Key light + accent bloom lights (flickering, additive).
    fun light(cx: Float, cy: Float, color: Color, radius: Float, intensity: Float, flicker: Float) {
        val fl = if (flicker > 0f)
            1f - flicker * (0.5f + 0.5f * sin(time * 17f + cx)) * (0.4f + 0.6f * hash((cx + cy).toInt(), time * 3f))
        else 1f
        val a = (intensity * fl).coerceIn(0f, 1f)
        drawCircle(
            brush = Brush.radialGradient(
                0f to color.copy(alpha = a),
                0.5f to color.copy(alpha = a * 0.4f),
                1f to Color.Transparent,
                center = Offset(cx, cy), radius = radius
            ),
            radius = radius, center = Offset(cx, cy), blendMode = BlendMode.Screen
        )
    }
    val keyPulse = 0.8f + 0.2f * sin(time * 1.6f)
    light(w * 0.5f, h * 0.42f, atmo.keyLight, w * 0.55f, atmo.keyIntensity * keyPulse, 0f)
    for (acc in atmo.accents) {
        light(w * acc.xFrac, h * acc.yFrac, acc.color, w * acc.radiusFrac, acc.intensity, acc.flicker)
    }

    // 5) Atmosphere motes — dust drifts, embers rise, fog flecks waft.
    for (i in 0 until atmo.moteCount) {
        val bx = hash(i, 1.7f)
        val by = hash(i, 9.1f)
        val spd = 0.3f + hash(i, 3.3f) * 0.7f
        val sizePx = (0.6f + hash(i, 5.5f) * 1.8f) * (w / 480f)
        val sway = sin(time * (0.4f + hash(i, 2.2f)) + i) * 0.03f
        val pos = when (atmo.moteStyle) {
            MoteStyle.EMBER -> {
                val y = (by - (time * spd * 0.08f)) % 1f
                Offset(w * (bx + sway), h * (if (y < 0f) y + 1f else y))
            }
            MoteStyle.FOG_FLECK -> {
                val x = (bx + time * spd * 0.02f) % 1f
                Offset(w * (if (x < 0f) x + 1f else x), h * (by * 0.6f + 0.3f + sway))
            }
            MoteStyle.DUST -> {
                val y = (by + time * spd * 0.015f) % 1f
                Offset(w * (bx + sway), h * (if (y < 0f) y + 1f else y))
            }
        }
        val twinkle = 0.4f + 0.6f * (0.5f + 0.5f * sin(time * 2f + i))
        drawCircle(
            brush = Brush.radialGradient(
                0f to atmo.moteColor.copy(alpha = 0.5f * twinkle),
                1f to Color.Transparent,
                center = pos, radius = sizePx * 2.2f
            ),
            radius = sizePx * 2.2f, center = pos, blendMode = BlendMode.Screen
        )
    }

    // 6) Tilt-shift fake DoF: darken top & bottom bands so the mid-plane pops.
    drawRect(
        brush = Brush.verticalGradient(0f to Color(0xFF0A0712).copy(alpha = 0.6f), 0.16f to Color.Transparent),
        size = Size(w, h)
    )
    drawRect(
        brush = Brush.verticalGradient(0.84f to Color.Transparent, 1f to Color(0xFF0A0712).copy(alpha = 0.66f)),
        size = Size(w, h)
    )

    // 7) Vignette.
    drawRect(
        brush = Brush.radialGradient(
            0.42f to Color.Transparent,
            1f to Color.Black.copy(alpha = atmo.vignette),
            center = Offset(w / 2f, h / 2f), radius = maxOf(w, h) * 0.72f
        ),
        size = Size(w, h)
    )

    // 8) Animated film grain — sparse flickering specks (cheap, time-seeded).
    if (atmo.grain > 0f) {
        val count = ((w * h) / 9000f).toInt().coerceIn(40, 260)
        val frame = (time * 24f).toInt()
        for (i in 0 until count) {
            val gx = hash(i + frame * 31, 0.123f) * w
            val gy = hash(i + frame * 31, 0.937f) * h
            val ga = atmo.grain * (0.4f + 0.6f * hash(i + frame, 0.5f))
            drawCircle(Color.White.copy(alpha = ga), radius = 0.8f, center = Offset(gx, gy))
        }
    }
}
