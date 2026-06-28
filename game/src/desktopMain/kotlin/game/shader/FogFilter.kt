package game.shader

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.korge.render.*
import korlibs.korge.view.filter.ShaderFilter
import korlibs.math.*

/**
 * Fog shader — renders animated, drifting semi-transparent fog over the scene.
 *
 * [density] 0.0 = clear, 1.0 = completely fogged.
 * [time] drives animation (seconds elapsed).
 * [driftX] / [driftY] control the drift direction and speed.
 *
 * The fog is rendered as a grey-white overlay with sin-based animated density
 * variation. Applied as a filter on the map container.
 */
class FogFilter(
    var density: Float = 0f,
    var time: Float = 0f,
    var driftX: Float = 0f,
    var driftY: Float = 0f,
) : ShaderFilter() {

    object FogUB : UniformBlock(fixedLocation = 10) {
        val u_Density by float()
        val u_Time by float()
        val u_DriftX by float()
        val u_DriftY by float()
    }

    companion object : BaseProgramProvider() {
        override val fragment: FragmentShader = FragmentShaderDefault {
            val coords01 = fragmentCoords01
            val original = tex(fragmentCoords)
            val density = FogUB.u_Density
            val time = FogUB.u_Time
            val driftX = FogUB.u_DriftX
            val driftY = FogUB.u_DriftY

            // Animated fog pattern using sin-based noise approximation
            // Create drifting coordinate space
            val driftedX = coords01.x + driftX * 0.1f.lit + time * 0.02f.lit
            val driftedY = coords01.y + driftY * 0.1f.lit + time * 0.015f.lit

            // Multi-octave sin-based pseudo-noise for organic fog look
            val n1 = sin(driftedX * 8f.lit + time * 0.5f.lit) * 0.3f.lit
            val n2 = sin(driftedY * 6f.lit + time * 0.3f.lit) * 0.3f.lit
            val n3 = sin((driftedX + driftedY) * 12f.lit + time * 0.7f.lit) * 0.2f.lit
            val fogPattern = (0.5f.lit + n1 + n2 + n3)

            // Fog factor: combines density with animated pattern
            val fogFactor = clamp(fogPattern * density, 0f.lit, 1f.lit)

            // Fog color: grey-white
            val fogR = 0.7f.lit
            val fogG = 0.75f.lit
            val fogB = 0.8f.lit

            // Lerp original toward fog color
            val r = original.x + (fogR - original.x) * fogFactor
            val g = original.y + (fogG - original.y) * fogFactor
            val b = original.z + (fogB - original.z) * fogFactor

            SET(out, vec4(r, g, b, original.w))
        }
    }

    override val programProvider: ProgramProvider get() = FogFilter

    override fun updateUniforms(ctx: RenderContext, filterScale: Double) {
        super.updateUniforms(ctx, filterScale)
        ctx[FogUB].push {
            it[u_Density] = density
            it[u_Time] = time
            it[u_DriftX] = driftX
            it[u_DriftY] = driftY
        }
    }
}
