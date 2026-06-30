package game.shader

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.korge.render.*
import korlibs.korge.view.filter.ShaderFilter

/**
 * Wet surface rendering — simulates rain/water on any surface by adding:
 * - Darkened base color (wet surfaces absorb more light)
 * - Increased specularity (wet = reflective)
 * - Subtle color saturation boost (wet colors are deeper)
 *
 * Applied globally when raining, or locally to wet zones.
 * The wetness level (0-1) controls the intensity.
 *
 * Technique: Fresnel approximation for specularity, inspired by
 * hoxxep/webgl-ray-tracing-demo material rendering.
 */
class WetSurfaceFilter(
    var time: Float = 0f,
    var wetness: Float = 0.0f,    // 0 = dry, 1 = soaked
    var specularStrength: Float = 0.4f,
) : ShaderFilter() {

    object WetUB : UniformBlock(fixedLocation = 16) {
        val u_Time by float()
        val u_Wetness by float()
        val u_Specular by float()
    }

    companion object : BaseProgramProvider() {
        override val fragment: FragmentShader = FragmentShaderDefault {
            val coords = fragmentCoords
            val texSize = TexInfoUB.u_TextureSize
            val time = WetUB.u_Time
            val wetness = WetUB.u_Wetness
            val specular = WetUB.u_Specular

            val original = tex(coords)

            // UV for specular noise
            val uvX = createTemp(Float1)
            val uvY = createTemp(Float1)
            SET(uvX, coords.x / texSize.x)
            SET(uvY, coords.y / texSize.y)

            // Wet surface: darken (absorb light) + deepen color
            val darkFactor = createTemp(Float1)
            SET(darkFactor, 1f.lit - wetness * 0.25f.lit) // up to 25% darker

            // Saturation boost: wet colors are more vivid
            val lum = createTemp(Float1)
            SET(lum, original.x * 0.299f.lit + original.y * 0.587f.lit + original.z * 0.114f.lit)
            val satBoost = createTemp(Float1)
            SET(satBoost, 1f.lit + wetness * 0.3f.lit) // 30% more saturated

            val wetR = createTemp(Float1)
            val wetG = createTemp(Float1)
            val wetB = createTemp(Float1)
            // Saturate: move away from grey (lum) toward original color
            SET(wetR, lum + (original.x - lum) * satBoost)
            SET(wetG, lum + (original.y - lum) * satBoost)
            SET(wetB, lum + (original.z - lum) * satBoost)
            // Darken
            SET(wetR, wetR * darkFactor)
            SET(wetG, wetG * darkFactor)
            SET(wetB, wetB * darkFactor)

            // Specular highlights (simulated rain droplets catching light)
            val specNoise = createTemp(Float1)
            SET(specNoise, fract(sin(uvX * 253.7f.lit + uvY * 419.3f.lit + time * 2.1f.lit) * 43758.5f.lit))
            val specHit = createTemp(Float1)
            SET(specHit, step(1f.lit - specular * wetness * 0.15f.lit, specNoise))

            // Final: mix dry→wet based on wetness, add specular
            val finalR = createTemp(Float1)
            val finalG = createTemp(Float1)
            val finalB = createTemp(Float1)
            SET(finalR, mix(original.x, wetR, wetness) + specHit * 0.3f.lit * wetness)
            SET(finalG, mix(original.y, wetG, wetness) + specHit * 0.35f.lit * wetness)
            SET(finalB, mix(original.z, wetB, wetness) + specHit * 0.4f.lit * wetness)

            SET(out, vec4(
                clamp(finalR, 0f.lit, 1f.lit),
                clamp(finalG, 0f.lit, 1f.lit),
                clamp(finalB, 0f.lit, 1f.lit),
                original.w
            ))
        }
    }

    override val programProvider: ProgramProvider get() = WetSurfaceFilter

    override fun updateUniforms(ctx: RenderContext, filterScale: Double) {
        super.updateUniforms(ctx, filterScale)
        ctx[WetUB].push {
            it[u_Time] = time
            it[u_Wetness] = wetness
            it[u_Specular] = specularStrength
        }
    }
}
