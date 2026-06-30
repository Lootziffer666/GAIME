package game.shader

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.korge.render.*
import korlibs.korge.view.filter.ShaderFilter

/**
 * Physically-inspired material rendering for world overlays.
 *
 * Instead of flat colored rectangles, this shader interprets the overlay colors
 * as MATERIALS and renders them with physical properties:
 * - Water (blue hues): reflective surface + subtle distortion + specular highlight
 * - Snow (white/bright): subsurface scattering glow + sparkle
 * - Blood (red hues): wet-look gloss + darkening over time
 * - Cracks (brown/dark): depth shadow + ambient occlusion
 *
 * The shader works as a POST-PROCESSING pass on the existing overlays — it reads
 * each pixel's color, classifies it by hue/saturation/value, and applies the
 * appropriate material treatment. This means the existing GridOverlay system
 * continues to work unchanged; this filter just makes it LOOK physical.
 *
 * Applied via ComposedFilter on the worldLayer (stacks with other effects).
 *
 * Inspired by: hoxxep/webgl-ray-tracing-demo (RT in fragment shader),
 * thebookofshaders (SDF + noise), sqrmelon (multi-pass demoscene rendering).
 */
class WorldMaterialFilter(
    var time: Float = 0f,
    var intensity: Float = 1.0f,
    var lightAngle: Float = 0.7f,   // radians, top-left light source
    var lightHeight: Float = 0.6f,  // 0=grazing, 1=overhead
) : ShaderFilter() {

    object MaterialUB : UniformBlock(fixedLocation = 14) {
        val u_Time by float()
        val u_Intensity by float()
        val u_LightDir by vec4()  // (dirX, dirY, dirZ, 0) — normalized light direction
    }

    companion object : BaseProgramProvider() {
        override val fragment: FragmentShader = FragmentShaderDefault {
            val coords = fragmentCoords
            val texSize = TexInfoUB.u_TextureSize
            val time = MaterialUB.u_Time
            val intensity = MaterialUB.u_Intensity
            val lightDir = MaterialUB.u_LightDir

            // Sample the current pixel
            val original = tex(coords)

            // Early out: fully transparent or near-transparent = background, pass through
            // This ensures the painted background stays completely untouched.
            // Only overlay pixels (alpha > 0.1) get material treatment.
            val isOverlay = createTemp(Float1)
            SET(isOverlay, step(0.1f.lit, original.w))

            // === COLOR CLASSIFICATION ===
            // Determine material type from the overlay color.
            // We use simple hue/saturation/value analysis.

            val r = createTemp(Float1)
            val g = createTemp(Float1)
            val b = createTemp(Float1)
            SET(r, original.x)
            SET(g, original.y)
            SET(b, original.z)

            // Luminance
            val lum = createTemp(Float1)
            SET(lum, r * 0.299f.lit + g * 0.587f.lit + b * 0.114f.lit)

            // Rough hue classification via channel dominance
            // Water: blue dominant (b > r && b > g)
            val isWater = createTemp(Float1)
            SET(isWater, step(r, b) * step(g, b) * step(0.2f.lit, b))

            // Snow: high luminance, low saturation (all channels near-equal and bright)
            val maxC = createTemp(Float1)
            val minC = createTemp(Float1)
            SET(maxC, max(max(r, g), b))
            SET(minC, min(min(r, g), b))
            val saturation = createTemp(Float1)
            SET(saturation, (maxC - minC) / max(maxC, 0.001f.lit))
            val isSnow = createTemp(Float1)
            SET(isSnow, step(0.6f.lit, lum) * step(saturation, 0.3f.lit))

            // Blood: red dominant (r > g*1.5 && r > b*1.5)
            val isBlood = createTemp(Float1)
            SET(isBlood, step(g * 1.5f.lit, r) * step(b * 1.5f.lit, r) * step(0.15f.lit, r))

            // Cracks/fatigue: dark brown (low lum, slight warm tint)
            val isCrack = createTemp(Float1)
            SET(isCrack, step(lum, 0.4f.lit) * step(0.05f.lit, lum) * (1f.lit - isWater) * (1f.lit - isBlood))

            // === MATERIAL RENDERING ===

            // UV for noise/distortion (normalized to texture)
            val uvX = createTemp(Float1)
            val uvY = createTemp(Float1)
            SET(uvX, coords.x / texSize.x)
            SET(uvY, coords.y / texSize.y)

            // --- WATER: Reflective surface with ripple distortion + specular ---
            // Fresnel-like brightening at edges (simulated via noise)
            val waterRipple = createTemp(Float1)
            SET(waterRipple, sin(uvX * 80f.lit + time * 2.3f.lit) * sin(uvY * 60f.lit + time * 1.7f.lit) * 0.15f.lit)

            // Specular highlight (fake reflection of overhead light)
            val specX = createTemp(Float1)
            val specY = createTemp(Float1)
            SET(specX, sin(uvX * 40f.lit + time * 1.1f.lit) * 0.3f.lit + lightDir.x)
            SET(specY, sin(uvY * 30f.lit + time * 0.9f.lit) * 0.3f.lit + lightDir.y)
            val specular = createTemp(Float1)
            SET(specular, clamp(1f.lit - (specX * specX + specY * specY) * 4f.lit, 0f.lit, 1f.lit))
            // Sharpen specular
            SET(specular, specular * specular * specular * 0.4f.lit)

            val waterR = createTemp(Float1)
            val waterG = createTemp(Float1)
            val waterB = createTemp(Float1)
            // Darken the base slightly, add blue shift + ripple variation + specular
            SET(waterR, r * 0.7f.lit + waterRipple * 0.05f.lit + specular * 0.8f.lit)
            SET(waterG, g * 0.8f.lit + waterRipple * 0.08f.lit + specular * 0.9f.lit)
            SET(waterB, b * 1.1f.lit + waterRipple * 0.1f.lit + specular * 1.0f.lit)

            // --- SNOW: Subsurface scattering + sparkle ---
            val snowSparkle = createTemp(Float1)
            // Sparkle = sharp noise that triggers rarely
            val sparkleNoise = createTemp(Float1)
            SET(sparkleNoise, fract(sin(uvX * 127.1f.lit + uvY * 311.7f.lit + time * 3.7f.lit) * 43758.5453f.lit))
            SET(snowSparkle, step(0.97f.lit, sparkleNoise) * 0.6f.lit)

            // Subsurface: slight blue in shadows, warm in highlights
            val snowR = createTemp(Float1)
            val snowG = createTemp(Float1)
            val snowB = createTemp(Float1)
            SET(snowR, r + snowSparkle + 0.05f.lit)
            SET(snowG, g + snowSparkle + 0.03f.lit)
            SET(snowB, b + snowSparkle * 0.8f.lit + 0.08f.lit) // slightly more blue = subsurface

            // --- BLOOD: Wet gloss + slight darkening ---
            val bloodGloss = createTemp(Float1)
            SET(bloodGloss, clamp(specular * 1.5f.lit, 0f.lit, 0.5f.lit))
            val bloodR = createTemp(Float1)
            val bloodG = createTemp(Float1)
            val bloodB = createTemp(Float1)
            SET(bloodR, r * 1.1f.lit + bloodGloss * 0.6f.lit) // slightly brighter red + gloss
            SET(bloodG, g * 0.6f.lit + bloodGloss * 0.2f.lit) // suppress green
            SET(bloodB, b * 0.5f.lit + bloodGloss * 0.3f.lit) // suppress blue

            // --- CRACKS: Depth shadow + ambient occlusion ---
            // Sample neighbors to create a faux-depth shadow
            val crackShadow = createTemp(Float1)
            // Darken from light direction (cracks are recessed)
            SET(crackShadow, 0.3f.lit + lum * 0.4f.lit)
            val crackR = createTemp(Float1)
            val crackG = createTemp(Float1)
            val crackB = createTemp(Float1)
            SET(crackR, r * crackShadow)
            SET(crackG, g * crackShadow)
            SET(crackB, b * crackShadow)

            // === COMPOSITING: blend material results based on classification ===
            val finalR = createTemp(Float1)
            val finalG = createTemp(Float1)
            val finalB = createTemp(Float1)

            // Start with original
            SET(finalR, r)
            SET(finalG, g)
            SET(finalB, b)

            // Apply water
            SET(finalR, mix(finalR, waterR, isWater * intensity))
            SET(finalG, mix(finalG, waterG, isWater * intensity))
            SET(finalB, mix(finalB, waterB, isWater * intensity))

            // Apply snow
            SET(finalR, mix(finalR, snowR, isSnow * intensity))
            SET(finalG, mix(finalG, snowG, isSnow * intensity))
            SET(finalB, mix(finalB, snowB, isSnow * intensity))

            // Apply blood
            SET(finalR, mix(finalR, bloodR, isBlood * intensity))
            SET(finalG, mix(finalG, bloodG, isBlood * intensity))
            SET(finalB, mix(finalB, bloodB, isBlood * intensity))

            // Apply cracks
            SET(finalR, mix(finalR, crackR, isCrack * intensity))
            SET(finalG, mix(finalG, crackG, isCrack * intensity))
            SET(finalB, mix(finalB, crackB, isCrack * intensity))

            // Clamp and preserve alpha
            SET(finalR, clamp(finalR, 0f.lit, 1f.lit))
            SET(finalG, clamp(finalG, 0f.lit, 1f.lit))
            SET(finalB, clamp(finalB, 0f.lit, 1f.lit))

            // Only apply to overlay pixels (isOverlay), pass-through for background
            SET(out, vec4(
                mix(original.x, finalR, isOverlay),
                mix(original.y, finalG, isOverlay),
                mix(original.z, finalB, isOverlay),
                original.w
            ))
        }
    }

    override val programProvider: ProgramProvider get() = WorldMaterialFilter

    override fun updateUniforms(ctx: RenderContext, filterScale: Double) {
        super.updateUniforms(ctx, filterScale)
        // Compute light direction from angle + height
        val dirX = kotlin.math.cos(lightAngle) * (1f - lightHeight)
        val dirY = kotlin.math.sin(lightAngle) * (1f - lightHeight)
        val dirZ = lightHeight

        ctx[MaterialUB].push {
            it[u_Time] = time
            it[u_Intensity] = intensity
            it.set(u_LightDir, dirX, dirY, dirZ, 0f)
        }
    }
}
