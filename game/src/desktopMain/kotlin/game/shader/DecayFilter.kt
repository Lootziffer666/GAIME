package game.shader

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.korge.render.*
import korlibs.korge.view.filter.ShaderFilter

/**
 * Procedural decay/growth overlay — renders rust, moss, lichen, and organic
 * creep using fractal Brownian motion (FBM) noise.
 *
 * The noise is seeded by screen position (deterministic per tile) and driven
 * by a decay parameter that controls coverage. Low decay = pristine surface;
 * high decay = heavily overgrown/rusted.
 *
 * Supports two modes via decayType:
 * - RUST (0): warm orange-brown tones, metallic patina
 * - MOSS (1): cool green-brown tones, organic growth
 *
 * Technique: thebookofshaders Ch.13 (Fractal Brownian Motion).
 */
class DecayFilter(
    var time: Float = 0f,
    var decayLevel: Float = 0.0f,   // 0 = pristine, 1 = fully decayed
    var decayType: Int = 0,          // 0 = rust, 1 = moss
) : ShaderFilter() {

    object DecayUB : UniformBlock(fixedLocation = 17) {
        val u_Time by float()
        val u_DecayLevel by float()
        val u_DecayType by float()
    }

    companion object : BaseProgramProvider() {
        override val fragment: FragmentShader = FragmentShaderDefault {
            val coords = fragmentCoords
            val texSize = TexInfoUB.u_TextureSize
            val time = DecayUB.u_Time
            val decayLevel = DecayUB.u_DecayLevel
            val decayType = DecayUB.u_DecayType

            val original = tex(coords)

            // Normalized coordinates for noise
            val uvX = createTemp(Float1)
            val uvY = createTemp(Float1)
            SET(uvX, coords.x / texSize.x * 12f.lit)
            SET(uvY, coords.y / texSize.y * 12f.lit)

            // Simple pseudo-random hash (cheaper than true Perlin, good enough for decay)
            // noise(x,y) = fract(sin(dot(xy, vec2(12.9898, 78.233))) * 43758.5453)
            val n1 = createTemp(Float1)
            SET(n1, fract(sin(uvX * 12.9898f.lit + uvY * 78.233f.lit) * 43758.5453f.lit))
            val n2 = createTemp(Float1)
            SET(n2, fract(sin(uvX * 39.346f.lit + uvY * 11.135f.lit + 1.3f.lit) * 43758.5453f.lit))
            val n3 = createTemp(Float1)
            SET(n3, fract(sin(uvX * 73.156f.lit + uvY * 52.235f.lit + 2.7f.lit) * 43758.5453f.lit))

            // FBM approximation: layer multiple octaves
            val fbm = createTemp(Float1)
            SET(fbm, n1 * 0.5f.lit + n2 * 0.3f.lit + n3 * 0.2f.lit)

            // Threshold by decayLevel: only show decay where noise > (1-decayLevel)
            val threshold = createTemp(Float1)
            SET(threshold, 1f.lit - decayLevel)
            val decayMask = createTemp(Float1)
            SET(decayMask, clamp((fbm - threshold) / 0.15f.lit, 0f.lit, 1f.lit))

            // Decay colors
            // Rust: warm orange-brown, slight variation
            val rustR = createTemp(Float1)
            val rustG = createTemp(Float1)
            val rustB = createTemp(Float1)
            SET(rustR, 0.55f.lit + n1 * 0.15f.lit)
            SET(rustG, 0.25f.lit + n2 * 0.1f.lit)
            SET(rustB, 0.08f.lit + n3 * 0.05f.lit)

            // Moss: green-brown, organic
            val mossR = createTemp(Float1)
            val mossG = createTemp(Float1)
            val mossB = createTemp(Float1)
            SET(mossR, 0.15f.lit + n1 * 0.1f.lit)
            SET(mossG, 0.35f.lit + n2 * 0.15f.lit)
            SET(mossB, 0.1f.lit + n3 * 0.08f.lit)

            // Select decay color by type (0=rust, 1=moss)
            val dR = createTemp(Float1)
            val dG = createTemp(Float1)
            val dB = createTemp(Float1)
            SET(dR, mix(rustR, mossR, decayType))
            SET(dG, mix(rustG, mossG, decayType))
            SET(dB, mix(rustB, mossB, decayType))

            // Blend: original → decay color, masked by FBM pattern
            val finalR = createTemp(Float1)
            val finalG = createTemp(Float1)
            val finalB = createTemp(Float1)
            SET(finalR, mix(original.x, dR, decayMask * 0.7f.lit))
            SET(finalG, mix(original.y, dG, decayMask * 0.7f.lit))
            SET(finalB, mix(original.z, dB, decayMask * 0.7f.lit))

            // Slight darkening in decayed areas (aged surfaces absorb light)
            val ageDarken = createTemp(Float1)
            SET(ageDarken, 1f.lit - decayMask * 0.15f.lit)
            SET(finalR, finalR * ageDarken)
            SET(finalG, finalG * ageDarken)
            SET(finalB, finalB * ageDarken)

            SET(out, vec4(
                clamp(finalR, 0f.lit, 1f.lit),
                clamp(finalG, 0f.lit, 1f.lit),
                clamp(finalB, 0f.lit, 1f.lit),
                original.w
            ))
        }
    }

    override val programProvider: ProgramProvider get() = DecayFilter

    override fun updateUniforms(ctx: RenderContext, filterScale: Double) {
        super.updateUniforms(ctx, filterScale)
        ctx[DecayUB].push {
            it[u_Time] = time
            it[u_DecayLevel] = decayLevel
            it[u_DecayType] = decayType.toFloat()
        }
    }
}
