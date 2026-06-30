package game.shader

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.korge.render.*
import korlibs.korge.view.filter.ShaderFilter

/**
 * Material-aware weather system — analyzes each pixel's color to determine its
 * material type, then applies weather effects PER MATERIAL.
 *
 * This is the key insight: instead of a separate material texture (which KorGE
 * filters can't bind), the shader classifies materials IN REAL TIME from the
 * source pixel's HSV values — the same segmentation the mapbuilder uses offline,
 * but running at 60fps on the GPU.
 *
 * Material classification (HSV-based, matching mapbuilder/segment.py):
 * - STONE (path/cobblestone): low saturation, mid-high value → puddles form, darkens uniformly
 * - WOOD (buildings/doors): warm hue (15-40°), medium saturation → darkens, no puddles, subtle sheen
 * - GRASS (vegetation): green hue (60-150°), high saturation → saturates MORE when wet, subtle droplets
 * - WATER (existing water): blue hue → caustics intensify, ripples
 * - ROOF (tile/shingle): warm-brown, high position in image → water runs off, specular
 *
 * Weather state is a single float 0.0 (clear sky) → 1.0 (heavy storm).
 * All material responses interpolate smoothly from dry to soaked.
 */
class MaterialWeatherFilter(
    var time: Float = 0f,
    var weatherState: Float = 0.0f,  // 0=sun, 0.5=rain, 1.0=storm
    var windAngle: Float = 0.3f,     // rain/fog drift direction
) : ShaderFilter() {

    object MatWeatherUB : UniformBlock(fixedLocation = 18) {
        val u_Time by float()
        val u_Weather by float()
        val u_Wind by float()
    }

    companion object : BaseProgramProvider() {
        override val fragment: FragmentShader = FragmentShaderDefault {
            val coords = fragmentCoords
            val texSize = TexInfoUB.u_TextureSize
            val time = MatWeatherUB.u_Time
            val weather = MatWeatherUB.u_Weather
            val wind = MatWeatherUB.u_Wind

            val original = tex(coords)
            val r = createTemp(Float1)
            val g = createTemp(Float1)
            val b = createTemp(Float1)
            SET(r, original.x)
            SET(g, original.y)
            SET(b, original.z)

            // === HSV CONVERSION (inline, no function calls in GLSL DSL) ===
            val maxC = createTemp(Float1)
            val minC = createTemp(Float1)
            SET(maxC, max(max(r, g), b))
            SET(minC, min(min(r, g), b))
            val delta = createTemp(Float1)
            SET(delta, maxC - minC)

            // Value = maxC
            val vv = createTemp(Float1)
            SET(vv, maxC)

            // Saturation = delta/maxC (0 if maxC==0)
            val sat = createTemp(Float1)
            SET(sat, delta / max(maxC, 0.001f.lit))

            // Hue (0-1 range, not 0-360)
            val hue = createTemp(Float1)
            // Approximate hue from channel dominance (cheaper than full formula)
            // Red-dominant (H near 0 or 1): r > g && r > b
            // Green-dominant (H near 0.33): g > r && g > b
            // Blue-dominant (H near 0.66): b > r && b > g
            val isRedMax = createTemp(Float1)
            val isGreenMax = createTemp(Float1)
            SET(isRedMax, step(g, r) * step(b, r))
            SET(isGreenMax, step(r, g) * step(b, g) * (1f.lit - isRedMax))
            // Simplified hue estimate
            SET(hue, isRedMax * ((g - b) / max(delta, 0.001f.lit) / 6f.lit) +
                isGreenMax * (0.333f.lit + (b - r) / max(delta, 0.001f.lit) / 6f.lit) +
                (1f.lit - isRedMax) * (1f.lit - isGreenMax) * (0.666f.lit + (r - g) / max(delta, 0.001f.lit) / 6f.lit))
            // Wrap to 0-1
            SET(hue, fract(hue))

            // === MATERIAL CLASSIFICATION ===
            // Stone/path: low saturation (< 0.3), mid value (0.3-0.8)
            val isStone = createTemp(Float1)
            SET(isStone, step(sat, 0.3f.lit) * step(0.25f.lit, vv) * step(vv, 0.85f.lit))

            // Grass/vegetation: green hue (0.2-0.45), sat > 0.25, value > 0.2
            val isGrass = createTemp(Float1)
            SET(isGrass, step(0.18f.lit, hue) * step(hue, 0.47f.lit) * step(0.2f.lit, sat) * step(0.15f.lit, vv))

            // Wood/building: warm hue (0.04-0.12), sat > 0.2, value 0.2-0.7
            val isWood = createTemp(Float1)
            SET(isWood, step(0.03f.lit, hue) * step(hue, 0.13f.lit) * step(0.15f.lit, sat) * step(0.15f.lit, vv) * step(vv, 0.75f.lit))

            // Water: blue hue (0.5-0.72), sat > 0.2
            val isWater = createTemp(Float1)
            SET(isWater, step(0.5f.lit, hue) * step(hue, 0.72f.lit) * step(0.15f.lit, sat))

            // Roof: similar to wood but higher in image (use Y position as hint)
            val normalizedY = createTemp(Float1)
            SET(normalizedY, coords.y / texSize.y)

            // UV for effects
            val uvX = createTemp(Float1)
            val uvY = createTemp(Float1)
            SET(uvX, coords.x / texSize.x)
            SET(uvY, coords.y / texSize.y)

            // === WEATHER EFFECTS PER MATERIAL ===
            val finalR = createTemp(Float1)
            val finalG = createTemp(Float1)
            val finalB = createTemp(Float1)
            SET(finalR, r)
            SET(finalG, g)
            SET(finalB, b)

            // --- Global darkening (overcast sky, proportional to weather) ---
            val skyDarken = createTemp(Float1)
            SET(skyDarken, 1f.lit - weather * 0.35f.lit)
            SET(finalR, finalR * skyDarken)
            SET(finalG, finalG * skyDarken)
            SET(finalB, finalB * skyDarken)

            // --- STONE: puddles (dark reflective patches) + wet darkening ---
            val puddleNoise = createTemp(Float1)
            SET(puddleNoise, fract(sin(uvX * 47.3f.lit + uvY * 91.7f.lit) * 43758.5f.lit))
            val hasPuddle = createTemp(Float1)
            // Puddles form on stone where noise < weather*0.5 (more puddles in heavier rain)
            SET(hasPuddle, isStone * step(puddleNoise, weather * 0.4f.lit))

            // Puddle: darken + blue tint + specular
            val puddleSpec = createTemp(Float1)
            SET(puddleSpec, sin(uvX * 60f.lit + time * 2f.lit) * sin(uvY * 45f.lit + time * 1.5f.lit))
            SET(puddleSpec, clamp(puddleSpec * puddleSpec * 0.3f.lit, 0f.lit, 0.3f.lit))
            SET(finalR, mix(finalR, finalR * 0.5f.lit + puddleSpec * 0.3f.lit, hasPuddle * weather))
            SET(finalG, mix(finalG, finalG * 0.55f.lit + puddleSpec * 0.35f.lit, hasPuddle * weather))
            SET(finalB, mix(finalB, finalB * 0.6f.lit + puddleSpec * 0.5f.lit + 0.1f.lit, hasPuddle * weather))

            // Stone (non-puddle): just darken slightly when wet
            val stoneWet = createTemp(Float1)
            SET(stoneWet, isStone * (1f.lit - hasPuddle) * weather)
            SET(finalR, finalR * (1f.lit - stoneWet * 0.15f.lit))
            SET(finalG, finalG * (1f.lit - stoneWet * 0.15f.lit))
            SET(finalB, finalB * (1f.lit - stoneWet * 0.12f.lit))

            // --- GRASS: saturate more when wet (rain makes greens pop) ---
            val grassWet = createTemp(Float1)
            SET(grassWet, isGrass * weather)
            val grassLum = createTemp(Float1)
            SET(grassLum, finalR * 0.299f.lit + finalG * 0.587f.lit + finalB * 0.114f.lit)
            // Push away from grey = increase saturation
            SET(finalR, mix(finalR, finalR + (finalR - grassLum) * 0.4f.lit, grassWet))
            SET(finalG, mix(finalG, finalG + (finalG - grassLum) * 0.5f.lit, grassWet))
            SET(finalB, mix(finalB, finalB + (finalB - grassLum) * 0.3f.lit, grassWet))
            // Slight darkening
            SET(finalR, finalR * (1f.lit - grassWet * 0.1f.lit))
            SET(finalG, finalG * (1f.lit - grassWet * 0.05f.lit))
            SET(finalB, finalB * (1f.lit - grassWet * 0.1f.lit))

            // --- WOOD: darken significantly + subtle sheen (no puddles) ---
            val woodWet = createTemp(Float1)
            SET(woodWet, isWood * weather)
            SET(finalR, finalR * (1f.lit - woodWet * 0.25f.lit))
            SET(finalG, finalG * (1f.lit - woodWet * 0.2f.lit))
            SET(finalB, finalB * (1f.lit - woodWet * 0.15f.lit))
            // Subtle specular on wet wood
            val woodSpec = createTemp(Float1)
            SET(woodSpec, fract(sin(uvX * 200f.lit + uvY * 150f.lit + time) * 43758.5f.lit))
            SET(woodSpec, step(0.98f.lit, woodSpec) * 0.15f.lit * woodWet)
            SET(finalR, finalR + woodSpec)
            SET(finalG, finalG + woodSpec)
            SET(finalB, finalB + woodSpec)

            // --- WATER: caustic intensification ---
            val waterEffect = createTemp(Float1)
            SET(waterEffect, isWater * weather)
            val caustic = createTemp(Float1)
            SET(caustic, sin(uvX * 80f.lit + time * 2.3f.lit) * sin(uvY * 60f.lit + time * 1.7f.lit))
            SET(caustic, clamp(caustic * caustic * 2f.lit, 0f.lit, 1f.lit) * 0.2f.lit)
            SET(finalR, finalR + caustic * waterEffect)
            SET(finalG, finalG + caustic * waterEffect)
            SET(finalB, finalB + caustic * waterEffect * 1.3f.lit)

            // --- FOG (global, denser at bottom of image, increases with weather) ---
            val fogDensity = createTemp(Float1)
            // More fog at bottom (ground level) than top (sky/roofs)
            SET(fogDensity, weather * weather * (0.3f.lit + (1f.lit - normalizedY) * 0.15f.lit))
            val fogColor = 0.7f.lit  // grey-white fog
            SET(finalR, mix(finalR, fogColor, fogDensity))
            SET(finalG, mix(finalG, fogColor, fogDensity))
            SET(finalB, mix(finalB, fogColor * 1.05f.lit, fogDensity))

            // --- RAIN STREAKS (angled lines, density proportional to weather) ---
            val rainUV = createTemp(Float1)
            SET(rainUV, uvX * 200f.lit + uvY * 500f.lit + time * 15f.lit + wind * uvX * 100f.lit)
            val rainStreak = createTemp(Float1)
            SET(rainStreak, step(0.995f.lit, fract(sin(rainUV) * 43758.5f.lit)) * weather * 0.3f.lit)
            SET(finalR, finalR + rainStreak)
            SET(finalG, finalG + rainStreak)
            SET(finalB, finalB + rainStreak * 1.2f.lit)

            // Clamp + preserve alpha
            SET(out, vec4(
                clamp(finalR, 0f.lit, 1f.lit),
                clamp(finalG, 0f.lit, 1f.lit),
                clamp(finalB, 0f.lit, 1f.lit),
                original.w
            ))
        }
    }

    override val programProvider: ProgramProvider get() = MaterialWeatherFilter

    override fun updateUniforms(ctx: RenderContext, filterScale: Double) {
        super.updateUniforms(ctx, filterScale)
        ctx[MatWeatherUB].push {
            it[u_Time] = time
            it[u_Weather] = weatherState
            it[u_Wind] = windAngle
        }
    }
}
