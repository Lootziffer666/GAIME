package game.shader

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.korge.render.*
import korlibs.korge.view.filter.ShaderFilter
import korlibs.math.*

/**
 * 2D lighting system as a multiply-blend shader filter.
 *
 * Supports up to 3 point light sources. Each light contributes a radial gradient
 * with flicker. Areas outside light reach are darkened to [ambientDarkness].
 *
 * Apply to the map container: `mapView.filter = lightingFilter`
 * Light positions are converted from tile coords internally.
 */
class LightingFilter(
    var ambientDarkness: Float = 0.15f,
    var time: Float = 0f,
) : ShaderFilter() {

    object LightingUB : UniformBlock(fixedLocation = 8) {
        val u_Ambient by float()
        val u_Time by float()
        val u_Light0 by vec4()     // (px, py, radiusPx, intensity)
        val u_Light1 by vec4()
        val u_Light2 by vec4()
        val u_Flicker by vec4()    // (speed0, speed1, speed2, 0)
    }

    companion object : BaseProgramProvider() {
        override val fragment: FragmentShader = FragmentShaderDefault {
            val coords = fragmentCoords
            val original = tex(coords)
            val time = LightingUB.u_Time
            val ambient = LightingUB.u_Ambient

            // Accumulate light level in a temp
            val lightLevel = createTemp(Float1)
            SET(lightLevel, ambient)

            // Light 0
            val l0 = LightingUB.u_Light0
            val dx0 = createTemp(Float1)
            val dy0 = createTemp(Float1)
            val dist0 = createTemp(Float1)
            val falloff0 = createTemp(Float1)
            val contrib0 = createTemp(Float1)
            SET(dx0, coords.x - l0.x)
            SET(dy0, coords.y - l0.y)
            SET(dist0, sqrt(dx0 * dx0 + dy0 * dy0))
            SET(falloff0, clamp(1f.lit - dist0 / max(l0.z, 1f.lit), 0f.lit, 1f.lit))
            SET(contrib0, falloff0 * falloff0 * l0.w * (1f.lit + sin(time * LightingUB.u_Flicker.x * 6.28f.lit + l0.x * 0.07f.lit) * 0.15f.lit))
            SET(lightLevel, lightLevel + contrib0)

            // Light 1
            val l1 = LightingUB.u_Light1
            val dx1 = createTemp(Float1)
            val dy1 = createTemp(Float1)
            val dist1 = createTemp(Float1)
            val falloff1 = createTemp(Float1)
            val contrib1 = createTemp(Float1)
            SET(dx1, coords.x - l1.x)
            SET(dy1, coords.y - l1.y)
            SET(dist1, sqrt(dx1 * dx1 + dy1 * dy1))
            SET(falloff1, clamp(1f.lit - dist1 / max(l1.z, 1f.lit), 0f.lit, 1f.lit))
            SET(contrib1, falloff1 * falloff1 * l1.w * (1f.lit + sin(time * LightingUB.u_Flicker.y * 6.28f.lit + l1.x * 0.07f.lit) * 0.15f.lit))
            SET(lightLevel, lightLevel + contrib1)

            // Light 2
            val l2 = LightingUB.u_Light2
            val dx2 = createTemp(Float1)
            val dy2 = createTemp(Float1)
            val dist2 = createTemp(Float1)
            val falloff2 = createTemp(Float1)
            val contrib2 = createTemp(Float1)
            SET(dx2, coords.x - l2.x)
            SET(dy2, coords.y - l2.y)
            SET(dist2, sqrt(dx2 * dx2 + dy2 * dy2))
            SET(falloff2, clamp(1f.lit - dist2 / max(l2.z, 1f.lit), 0f.lit, 1f.lit))
            SET(contrib2, falloff2 * falloff2 * l2.w * (1f.lit + sin(time * LightingUB.u_Flicker.z * 6.28f.lit + l2.x * 0.07f.lit) * 0.15f.lit))
            SET(lightLevel, lightLevel + contrib2)

            // Clamp and multiply
            SET(lightLevel, clamp(lightLevel, 0f.lit, 1.3f.lit))
            SET(out, vec4(original.x * lightLevel, original.y * lightLevel, original.z * lightLevel, original.w))
        }
    }

    /** Active light sources (max 3 used). */
    var lights: List<LightSource> = emptyList()

    /** Tile size × scale — for converting tile coords to pixel coords. */
    var tilePixelSize: Float = 48f

    override val programProvider: ProgramProvider get() = LightingFilter

    override fun updateUniforms(ctx: RenderContext, filterScale: Double) {
        super.updateUniforms(ctx, filterScale)
        ctx[LightingUB].push {
            it[u_Ambient] = ambientDarkness
            it[u_Time] = time

            fun packLight(light: LightSource?): FloatArray {
                if (light == null) return floatArrayOf(0f, 0f, 0f, 0f)
                val px = light.tileX * tilePixelSize + tilePixelSize / 2f
                val py = light.tileY * tilePixelSize + tilePixelSize / 2f
                val radiusPx = light.radius * tilePixelSize
                return floatArrayOf(px, py, radiusPx, light.intensity)
            }

            val l0 = packLight(lights.getOrNull(0))
            val l1 = packLight(lights.getOrNull(1))
            val l2 = packLight(lights.getOrNull(2))

            it.set(u_Light0, l0[0], l0[1], l0[2], l0[3])
            it.set(u_Light1, l1[0], l1[1], l1[2], l1[3])
            it.set(u_Light2, l2[0], l2[1], l2[2], l2[3])
            it.set(u_Flicker,
                lights.getOrNull(0)?.flickerSpeed ?: 0f,
                lights.getOrNull(1)?.flickerSpeed ?: 0f,
                lights.getOrNull(2)?.flickerSpeed ?: 0f,
                0f
            )
        }
    }
}
