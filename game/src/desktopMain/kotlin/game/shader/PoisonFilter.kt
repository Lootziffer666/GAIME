package game.shader

import korlibs.graphics.shader.*
import korlibs.korge.render.RenderContext
import korlibs.korge.view.filter.ShaderFilter

/**
 * Poison/intoxication shader — increasing disorientation through:
 * - Chromatic aberration (RGB channels shift apart)
 * - Wave distortion (world bends)
 * - Vignette tunneling (edges darken, tunnel vision)
 *
 * [intensity] 0.0 = healthy, 1.0 = maximum impairment.
 * [time] drives animation (seconds elapsed).
 */
class PoisonFilter(
    var intensity: Float = 0f,
    var time: Float = 0f,
) : ShaderFilter() {

    object PoisonUB : UniformBlock(fixedLocation = 6) {
        val u_Intensity by float()
        val u_Time by float()
    }

    companion object : BaseProgramProvider() {
        override val fragment = FragmentShader {
            val coords01 = fragmentCoords01
            val intensity = PoisonUB.u_Intensity
            val time = PoisonUB.u_Time
            val texSize = TexInfoUB.u_TextureSize

            // Chromatic aberration: shift R and B channels apart
            val aberration = intensity * 0.012f.lit
            val rOffset = aberration * sin(time * 3f.lit + coords01.y * 20f.lit)
            val bOffset = -aberration * sin(time * 2.5f.lit + coords01.y * 18f.lit)

            val rCoord = (coords01 + vec2(rOffset, 0f.lit)) * texSize
            val gCoord = coords01 * texSize
            val bCoord = (coords01 + vec2(bOffset, 0f.lit)) * texSize

            val r = tex(rCoord)["r"]
            val g = tex(gCoord)["g"]
            val b = tex(bCoord)["b"]
            val a = tex(gCoord)["a"]

            // Vignette tunneling (tunnel vision increases with intensity)
            val cx = coords01.x - 0.5f.lit
            val cy = coords01.y - 0.5f.lit
            val dist = sqrt(cx * cx + cy * cy)
            val vignetteStart = 0.5f.lit - intensity * 0.2f.lit
            val vignette = clamp((dist - vignetteStart) / (0.5f.lit - vignetteStart), 0f.lit, 1f.lit)
            val darken = 1f.lit - vignette * intensity * 0.7f.lit

            SET(out, vec4(r * darken, g * darken, b * darken, a))
        }
    }

    override val programProvider: ProgramProvider get() = PoisonFilter

    override fun updateUniforms(ctx: RenderContext, filterScale: Double) {
        super.updateUniforms(ctx, filterScale)
        ctx[PoisonUB].push {
            it[u_Intensity] = intensity
            it[u_Time] = time
        }
    }
}
