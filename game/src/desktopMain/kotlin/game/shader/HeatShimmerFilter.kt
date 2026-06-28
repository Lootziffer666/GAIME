package game.shader

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.korge.render.*
import korlibs.korge.view.filter.ShaderFilter
import korlibs.math.*

/**
 * Heat shimmer / mirage effect — distorts UV coordinates with a sinusoidal wave
 * that simulates rising hot air. Apply near forges, desert areas, or fire.
 *
 * [intensity] 0.0 = no distortion, 1.0 = strong shimmer.
 * [time] drives animation (seconds elapsed).
 * [frequency] wave density — higher = more wiggly lines.
 */
class HeatShimmerFilter(
    var intensity: Float = 0f,
    var time: Float = 0f,
    var frequency: Float = 30f,
) : ShaderFilter() {

    object HeatUB : UniformBlock(fixedLocation = 10) {
        val u_Intensity by float()
        val u_Time by float()
        val u_Frequency by float()
    }

    companion object : BaseProgramProvider() {
        override val fragment: FragmentShader = FragmentShaderDefault {
            val coords = fragmentCoords
            val texSize = TexInfoUB.u_TextureSize
            val intensity = HeatUB.u_Intensity
            val time = HeatUB.u_Time
            val freq = HeatUB.u_Frequency

            // Horizontal UV distortion that increases toward the top of the image
            // (hot air rises → stronger distortion at top)
            val coords01 = fragmentCoords01
            val heightFactor = 1f.lit - coords01.y  // 1 at top, 0 at bottom

            val distortion = sin(coords01.y * freq + time * 3f.lit) *
                intensity * 0.005f.lit * heightFactor

            val distortedCoords = vec2(coords.x + distortion * texSize.x, coords.y)

            SET(out, tex(distortedCoords))
        }
    }

    override val programProvider: ProgramProvider get() = HeatShimmerFilter

    override fun updateUniforms(ctx: RenderContext, filterScale: Double) {
        super.updateUniforms(ctx, filterScale)
        ctx[HeatUB].push {
            it[u_Intensity] = intensity
            it[u_Time] = time
            it[u_Frequency] = frequency
        }
    }
}
