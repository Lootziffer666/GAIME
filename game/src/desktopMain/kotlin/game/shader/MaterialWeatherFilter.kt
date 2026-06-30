package game.shader

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.korge.render.*
import korlibs.korge.view.filter.ShaderFilter

/**
 * Material-aware weather system — analyzes each pixel's color to determine its
 * material type, then applies weather effects PER MATERIAL.
 *
 * This shader classifies materials IN REAL TIME from the source pixel's HSV values.
 * HSV ranges are calibrated against the offline segmentation produced by
 * tools/mapbuilder/material_segment.py (ground truth). The offline pipeline generates
 * material_grid.json + material_bitmap.png for visualization/QA.
 *
 * Material classification (HSV-based, calibrated from village image analysis):
 * - STONE (path/cobblestone): H=15-22, S=120-140, V>220 → puddles, darkens, reflections
 * - WOOD (buildings/doors): H=8-20, S>100, V=80-165 → darkens, subtle sheen, no puddles
 * - GRASS (vegetation): H=25-85, S>50, V>110 → saturates MORE when wet
 * - ROOF (tile/shingle): H=8-14, S>135, V=150-225 → water beads off, specular streaks
 * - FOLIAGE (tree canopy): H=25-100, S>30, V<110 → drips, color deepens
 * - WATER (existing water): H=90-130, S>50 → caustics intensify
 *
 * Weather state is a single float 0.0 (clear sky) → 1.0 (heavy storm).
 * All material responses interpolate smoothly from dry to soaked.
 *
 * Note on architecture: KorGE filters can only bind ONE texture, so a separate
 * material-map texture is impossible. The offline segmentation is used for QA
 * and calibration; the runtime shader classifies from pixel data. This works
 * well for painted backgrounds with distinct material colors.
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

            // === HSV CONVERSION ===
            val maxC = createTemp(Float1)
            val minC = createTemp(Float1)
            SET(maxC, max(max(r, g), b))
            SET(minC, min(min(r, g), b))
            val delta = createTemp(Float1)
            SET(delta, maxC - minC)

            // Value
            val vv = createTemp(Float1)
            SET(vv, maxC)

            // Saturation
            val sat = createTemp(Float1)
            SET(sat, delta / max(maxC, 0.001f.lit))

            // Hue (0-1 range)
            val hue = createTemp(Float1)
            val isRedMax = createTemp(Float1)
            val isGreenMax = createTemp(Float1)
            SET(isRedMax, step(g, r) * step(b, r))
            SET(isGreenMax, step(r, g) * step(b, g) * (1f.lit - isRedMax))
            SET(hue, isRedMax * ((g - b) / max(delta, 0.001f.lit) / 6f.lit) +
                isGreenMax * (0.333f.lit + (b - r) / max(delta, 0.001f.lit) / 6f.lit) +
                (1f.lit - isRedMax) * (1f.lit - isGreenMax) * (0.666f.lit + (r - g) / max(delta, 0.001f.lit) / 6f.lit))
            SET(hue, fract(hue))

            // === MATERIAL CLASSIFICATION ===
            // Calibrated against offline segmentation (material_segment.py results):
            // Village image HSV ranges observed:
            //   Stone path: H=0.083-0.111 (15-20°/180), S=0.47-0.55, V=0.86-0.97
            //   Roof:       H=0.044-0.078 (8-14°/180), S=0.53-0.61, V=0.59-0.88
            //   Wood:       H=0.044-0.111 (8-20°/180), S=0.39-0.60, V=0.31-0.65
            //   Grass:      H=0.139-0.472 (25-85°/180), S>0.20, V>0.43
            //   Foliage:    H=0.139-0.556 (25-100°/180), S>0.12, V<0.43
            //   Flowers:    H>0.833 or H<0.028, S>0.31, V>0.20

            // Stone path: low saturation OR (warm hue + high value + moderate sat)
            // Key insight: stone is the BRIGHTEST warm material
            val isStone = createTemp(Float1)
            // Path 1: desaturated bright (grey stones)
            val stoneGrey = step(sat, 0.22f.lit) * step(0.6f.lit, vv)
            // Path 2: warm hue (H=0.08-0.12), high value (>0.86), moderate sat
            val stoneWarm = step(0.07f.lit, hue) * step(hue, 0.13f.lit) * step(0.86f.lit, vv) * step(0.35f.lit, sat) * step(sat, 0.6f.lit)
            SET(isStone, clamp(stoneGrey + stoneWarm, 0f.lit, 1f.lit))

            // Roof: reddish-brown, darker than path, higher saturation
            // H=0.044-0.078, S>0.53, V=0.59-0.88
            val isRoof = createTemp(Float1)
            SET(isRoof, step(0.035f.lit, hue) * step(hue, 0.085f.lit) *
                step(0.50f.lit, sat) * step(0.55f.lit, vv) * step(vv, 0.90f.lit) *
                (1f.lit - isStone))  // stone takes priority if both match

            // Wood: warm hue, medium value, moderate saturation
            // H=0.044-0.111, S>0.39, V=0.31-0.65
            val isWood = createTemp(Float1)
            SET(isWood, step(0.035f.lit, hue) * step(hue, 0.12f.lit) *
                step(0.30f.lit, sat) * step(0.25f.lit, vv) * step(vv, 0.66f.lit) *
                (1f.lit - isStone) * (1f.lit - isRoof))

            // Grass: green hue, bright
            // H=0.139-0.472, S>0.20, V>0.43
            val isGrass = createTemp(Float1)
            SET(isGrass, step(0.13f.lit, hue) * step(hue, 0.48f.lit) *
                step(0.15f.lit, sat) * step(0.40f.lit, vv))

            // Foliage: green hue but darker (tree canopy, bushes)
            // H=0.139-0.556, S>0.12, V<0.43
            val isFoliage = createTemp(Float1)
            SET(isFoliage, step(0.13f.lit, hue) * step(hue, 0.56f.lit) *
                step(0.10f.lit, sat) * step(vv, 0.43f.lit))

            // Water: blue hue
            val isWater = createTemp(Float1)
            SET(isWater, step(0.5f.lit, hue) * step(hue, 0.72f.lit) * step(0.15f.lit, sat))

            // UV for effects
            val uvX = createTemp(Float1)
            val uvY = createTemp(Float1)
            SET(uvX, coords.x / texSize.x)
            SET(uvY, coords.y / texSize.y)

            val normalizedY = createTemp(Float1)
            SET(normalizedY, coords.y / texSize.y)

            // === WEATHER EFFECTS PER MATERIAL ===
            val finalR = createTemp(Float1)
            val finalG = createTemp(Float1)
            val finalB = createTemp(Float1)
            SET(finalR, r)
            SET(finalG, g)
            SET(finalB, b)

            // --- Global overcast darkening ---
            val skyDarken = createTemp(Float1)
            SET(skyDarken, 1f.lit - weather * 0.30f.lit)
            SET(finalR, finalR * skyDarken)
            SET(finalG, finalG * skyDarken)
            SET(finalB, finalB * skyDarken)

            // --- STONE: puddles + wet darkening ---
            val puddleNoise = createTemp(Float1)
            SET(puddleNoise, fract(sin(uvX * 47.3f.lit + uvY * 91.7f.lit) * 43758.5f.lit))
            val hasPuddle = createTemp(Float1)
            SET(hasPuddle, isStone * step(puddleNoise, weather * 0.4f.lit))

            // Puddle: darken + blue tint + animated specular
            val puddleSpec = createTemp(Float1)
            SET(puddleSpec, sin(uvX * 60f.lit + time * 2f.lit) * sin(uvY * 45f.lit + time * 1.5f.lit))
            SET(puddleSpec, clamp(puddleSpec * puddleSpec * 0.3f.lit, 0f.lit, 0.3f.lit))
            SET(finalR, mix(finalR, finalR * 0.5f.lit + puddleSpec * 0.3f.lit, hasPuddle * weather))
            SET(finalG, mix(finalG, finalG * 0.55f.lit + puddleSpec * 0.35f.lit, hasPuddle * weather))
            SET(finalB, mix(finalB, finalB * 0.6f.lit + puddleSpec * 0.5f.lit + 0.1f.lit, hasPuddle * weather))

            // Non-puddle stone: just darken when wet
            val stoneWet2 = createTemp(Float1)
            SET(stoneWet2, isStone * (1f.lit - hasPuddle) * weather)
            SET(finalR, finalR * (1f.lit - stoneWet2 * 0.15f.lit))
            SET(finalG, finalG * (1f.lit - stoneWet2 * 0.15f.lit))
            SET(finalB, finalB * (1f.lit - stoneWet2 * 0.12f.lit))

            // --- GRASS: saturate more (rain makes greens pop) ---
            val grassWet = createTemp(Float1)
            SET(grassWet, isGrass * weather)
            val grassLum = createTemp(Float1)
            SET(grassLum, finalR * 0.299f.lit + finalG * 0.587f.lit + finalB * 0.114f.lit)
            SET(finalR, mix(finalR, finalR + (finalR - grassLum) * 0.4f.lit, grassWet))
            SET(finalG, mix(finalG, finalG + (finalG - grassLum) * 0.5f.lit, grassWet))
            SET(finalB, mix(finalB, finalB + (finalB - grassLum) * 0.3f.lit, grassWet))
            // Slight darkening
            SET(finalR, finalR * (1f.lit - grassWet * 0.1f.lit))
            SET(finalG, finalG * (1f.lit - grassWet * 0.05f.lit))
            SET(finalB, finalB * (1f.lit - grassWet * 0.1f.lit))

            // --- WOOD: darken significantly + subtle sheen ---
            val woodWet = createTemp(Float1)
            SET(woodWet, isWood * weather)
            SET(finalR, finalR * (1f.lit - woodWet * 0.25f.lit))
            SET(finalG, finalG * (1f.lit - woodWet * 0.2f.lit))
            SET(finalB, finalB * (1f.lit - woodWet * 0.15f.lit))
            // Wet wood specular (rare sparkle)
            val woodSpec = createTemp(Float1)
            SET(woodSpec, fract(sin(uvX * 200f.lit + uvY * 150f.lit + time) * 43758.5f.lit))
            SET(woodSpec, step(0.98f.lit, woodSpec) * 0.15f.lit * woodWet)
            SET(finalR, finalR + woodSpec)
            SET(finalG, finalG + woodSpec)
            SET(finalB, finalB + woodSpec)

            // --- ROOF: water beads off (Abperlen) + specular streaks ---
            val roofWet = createTemp(Float1)
            SET(roofWet, isRoof * weather)
            // Roof darkens LESS than wood (water runs off, doesn't soak in)
            SET(finalR, finalR * (1f.lit - roofWet * 0.12f.lit))
            SET(finalG, finalG * (1f.lit - roofWet * 0.10f.lit))
            SET(finalB, finalB * (1f.lit - roofWet * 0.08f.lit))
            // Specular streaks running down the roof (vertical, wind-shifted)
            val roofStreak = createTemp(Float1)
            val streakUV = createTemp(Float1)
            // Streaks flow DOWN the roof, shifted by wind
            SET(streakUV, uvX * 150f.lit + wind * 20f.lit + uvY * 30f.lit)
            val streakPhase = createTemp(Float1)
            SET(streakPhase, fract(sin(streakUV) * 43758.5f.lit))
            // Animate: streaks move down over time
            val streakAnim = createTemp(Float1)
            SET(streakAnim, fract(uvY * 8f.lit + time * 3f.lit + streakPhase * 6.28f.lit))
            // Thin bright streaks (water beading down)
            SET(roofStreak, step(0.92f.lit, streakAnim) * step(0.85f.lit, streakPhase) * roofWet * 0.3f.lit)
            SET(finalR, finalR + roofStreak * 0.8f.lit)
            SET(finalG, finalG + roofStreak * 0.9f.lit)
            SET(finalB, finalB + roofStreak * 1.0f.lit)

            // --- FOLIAGE: color deepens + slight drip effect ---
            val foliageWet = createTemp(Float1)
            SET(foliageWet, isFoliage * weather)
            // Deepen: push green channel, suppress red
            SET(finalR, finalR * (1f.lit - foliageWet * 0.2f.lit))
            SET(finalG, finalG * (1f.lit + foliageWet * 0.08f.lit))
            SET(finalB, finalB * (1f.lit - foliageWet * 0.05f.lit))
            // Occasional drip highlight (small bright spots falling)
            val dripNoise = createTemp(Float1)
            SET(dripNoise, fract(sin(uvX * 311f.lit + uvY * 127f.lit + time * 4f.lit) * 43758.5f.lit))
            val dripActive = createTemp(Float1)
            SET(dripActive, step(0.995f.lit, dripNoise) * foliageWet * 0.2f.lit)
            SET(finalR, finalR + dripActive)
            SET(finalG, finalG + dripActive)
            SET(finalB, finalB + dripActive * 1.3f.lit)

            // --- WATER: caustic intensification ---
            val waterEffect = createTemp(Float1)
            SET(waterEffect, isWater * weather)
            val caustic = createTemp(Float1)
            SET(caustic, sin(uvX * 80f.lit + time * 2.3f.lit) * sin(uvY * 60f.lit + time * 1.7f.lit))
            SET(caustic, clamp(caustic * caustic * 2f.lit, 0f.lit, 1f.lit) * 0.2f.lit)
            SET(finalR, finalR + caustic * waterEffect)
            SET(finalG, finalG + caustic * waterEffect)
            SET(finalB, finalB + caustic * waterEffect * 1.3f.lit)

            // --- FOG: ground-level haze, density increases with weather ---
            val fogDensity = createTemp(Float1)
            SET(fogDensity, weather * weather * (0.25f.lit + (1f.lit - normalizedY) * 0.12f.lit))
            val fogColor = 0.72f.lit
            SET(finalR, mix(finalR, fogColor, fogDensity))
            SET(finalG, mix(finalG, fogColor, fogDensity))
            SET(finalB, mix(finalB, fogColor * 1.05f.lit, fogDensity))

            // --- RAIN STREAKS: angled lines ---
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
