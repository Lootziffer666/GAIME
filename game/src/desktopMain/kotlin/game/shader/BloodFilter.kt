package game.shader

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.korge.render.*
import korlibs.korge.view.filter.ShaderFilter
import korlibs.math.*

/**
 * Blood splatter overlay shader — renders dark red-black splatters over the scene.
 * Uses a procedural noise-like pattern for organic splatter edges.
 *
 * [intensity] 0.0 = no blood, 1.0 = heavily splattered.
 * [splatterSeed] determines the pattern variation (deterministic for screenshots).
 * [time] drives subtle pulsing animation (seconds elapsed).
 */
class BloodFilter(
    var intensity: Float = 0f,
    var splatterSeed: Float = 1.0f,
    var time: Float = 0f,
) : ShaderFilter() {

    object BloodUB : UniformBlock(fixedLocation = 12) {
        val u_Intensity by float()
        val u_Seed by float()
        val u_Time by float()
    }

    companion object : BaseProgramProvider() {
        override val fragment: FragmentShader = FragmentShaderDefault {
            val coords01 = fragmentCoords01
            val original = tex(fragmentCoords)
            val intensity = BloodUB.u_Intensity
            val seed = BloodUB.u_Seed
            val time = BloodUB.u_Time

            // Procedural noise using fract/sin combination for organic edges
            // Hash function: fract(sin(dot(co, vec2(12.9898, 78.233))) * 43758.5453)
            val noiseScale = 15f.lit
            val nx = coords01.x * noiseScale + seed
            val ny = coords01.y * noiseScale + seed * 0.7f.lit
            val dotProduct = nx * 12.9898f.lit + ny * 78.233f.lit
            val noise1 = fract(sin(dotProduct) * 43758.5453f.lit)

            // Second octave for finer detail
            val nx2 = coords01.x * noiseScale * 2.3f.lit + seed * 1.3f.lit
            val ny2 = coords01.y * noiseScale * 2.3f.lit + seed * 0.5f.lit
            val dotProduct2 = nx2 * 12.9898f.lit + ny2 * 78.233f.lit
            val noise2 = fract(sin(dotProduct2) * 43758.5453f.lit)

            // Combine octaves: threshold creates organic splatter shape
            val combinedNoise = noise1 * 0.6f.lit + noise2 * 0.4f.lit

            // Step threshold: only show blood where noise exceeds a threshold
            // Lower threshold = more coverage. Scale with intensity.
            val threshold = 1f.lit - intensity * 0.7f.lit
            val splatterMask = step(threshold, combinedNoise)

            // Subtle time-based pulse (blood glistens)
            val pulse = 1f.lit + sin(time * 2f.lit) * 0.05f.lit

            // Blood alpha: mask * intensity * pulse
            val bloodAlpha = splatterMask * intensity * 0.7f.lit * pulse

            // Blood color range: dark red (0.4, 0.0, 0.0) to darker (0.15, 0.0, 0.0)
            // Mix based on noise for variation
            val bloodR = (0.15f.lit + noise1 * 0.25f.lit) * bloodAlpha
            val bloodG = 0f.lit
            val bloodB = 0f.lit

            // Composite: darken + tint red (multiplicative darkening + additive red)
            val darken = 1f.lit - bloodAlpha * 0.5f.lit
            val r = original.x * darken + bloodR
            val g = original.y * darken + bloodG
            val b = original.z * darken + bloodB

            SET(out, vec4(clamp(r, 0f.lit, 1f.lit), clamp(g, 0f.lit, 1f.lit), clamp(b, 0f.lit, 1f.lit), original.w))
        }
    }

    override val programProvider: ProgramProvider get() = BloodFilter

    override fun updateUniforms(ctx: RenderContext, filterScale: Double) {
        super.updateUniforms(ctx, filterScale)
        ctx[BloodUB].push {
            it[u_Intensity] = intensity
            it[u_Seed] = splatterSeed
            it[u_Time] = time
        }
    }
}
