package game.shader

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.korge.render.*
import korlibs.korge.view.filter.ShaderFilter
import korlibs.math.*

/**
 * Snow particle shader — renders falling snow flakes over the scene.
 * White overlay proportional to [intensity] (overall snow depth), with
 * procedural falling snow particles that drift with [windAngle].
 *
 * [intensity] 0.0 = no snow, 1.0 = full blizzard.
 * [windAngle] radians offset from vertical (0 = straight down, positive = wind from left).
 * [time] drives animation (seconds elapsed).
 */
class SnowFilter(
    var intensity: Float = 0f,
    var windAngle: Float = 0f,
    var time: Float = 0f,
) : ShaderFilter() {

    object SnowUB : UniformBlock(fixedLocation = 11) {
        val u_Intensity by float()
        val u_Wind by float()
        val u_Time by float()
    }

    companion object : BaseProgramProvider() {
        override val fragment: FragmentShader = FragmentShaderDefault {
            val coords01 = fragmentCoords01
            val original = tex(fragmentCoords)
            val intensity = SnowUB.u_Intensity
            val wind = SnowUB.u_Wind
            val time = SnowUB.u_Time

            // --- Layer 1: large slow flakes ---
            val scale1 = 40f.lit
            val speed1 = 1.5f.lit
            val sx1 = coords01.x * scale1 + coords01.y * wind * 2f.lit
            val sy1 = coords01.y * scale1 * 0.5f.lit + time * speed1
            val cx1 = fract(sx1)
            val cy1 = fract(sy1)
            // Round flake: distance from cell center
            val dx1 = cx1 - 0.5f.lit
            val dy1 = cy1 - 0.5f.lit
            val dist1 = dx1 * dx1 + dy1 * dy1
            val flake1 = step(dist1, 0.02f.lit)

            // --- Layer 2: small fast flakes ---
            val scale2 = 70f.lit
            val speed2 = 2.5f.lit
            val sx2 = coords01.x * scale2 + coords01.y * wind * 3f.lit + 0.37f.lit
            val sy2 = coords01.y * scale2 * 0.4f.lit + time * speed2 + 0.61f.lit
            val cx2 = fract(sx2)
            val cy2 = fract(sy2)
            val dx2 = cx2 - 0.5f.lit
            val dy2 = cy2 - 0.5f.lit
            val dist2 = dx2 * dx2 + dy2 * dy2
            val flake2 = step(dist2, 0.01f.lit)

            // --- Layer 3: tiny drifting flakes ---
            val scale3 = 120f.lit
            val speed3 = 1.0f.lit
            val sx3 = coords01.x * scale3 + coords01.y * wind * 1.5f.lit + 0.73f.lit
            val sy3 = coords01.y * scale3 * 0.6f.lit + time * speed3 + 0.29f.lit
            val cx3 = fract(sx3)
            val cy3 = fract(sy3)
            val dx3 = cx3 - 0.5f.lit
            val dy3 = cy3 - 0.5f.lit
            val dist3 = dx3 * dx3 + dy3 * dy3
            val flake3 = step(dist3, 0.006f.lit)

            // Combined snow particle alpha
            val particleAlpha = (flake1 + flake2 * 0.7f.lit + flake3 * 0.4f.lit) * intensity * 0.6f.lit

            // White ground fog overlay proportional to intensity (subtle)
            val fogAlpha = intensity * 0.08f.lit

            // Total snow contribution
            val snowAlpha = particleAlpha + fogAlpha

            // Composite: additive white over original
            val r = original.x + snowAlpha
            val g = original.y + snowAlpha
            val b = original.z + snowAlpha

            SET(out, vec4(clamp(r, 0f.lit, 1f.lit), clamp(g, 0f.lit, 1f.lit), clamp(b, 0f.lit, 1f.lit), original.w))
        }
    }

    override val programProvider: ProgramProvider get() = SnowFilter

    override fun updateUniforms(ctx: RenderContext, filterScale: Double) {
        super.updateUniforms(ctx, filterScale)
        ctx[SnowUB].push {
            it[u_Intensity] = intensity
            it[u_Wind] = windAngle
            it[u_Time] = time
        }
    }
}
