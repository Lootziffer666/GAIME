package game.shader

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.image.bitmap.Bitmap32
import korlibs.korge.render.*
import korlibs.korge.view.filter.ShaderFilter

/**
 * Material-aware weather system — Multi-Texture, pixel-accurate.
 *
 * Uses TWO textures:
 * 1. Scene image (texture unit 0, default) — the HD painted background
 * 2. Material map (texture unit 1, u_MaterialMap) — full-resolution bitmap
 *    where R channel encodes Material ID per pixel
 *
 * The material map is generated offline by tools/mapbuilder/material_fullres.py
 * at FULL PIXEL RESOLUTION (2560x1440). No tile grid, no upscaling, no blocky
 * boundaries. Every pixel has its own material classification.
 *
 * Weather effects are AGGRESSIVE — meant to produce the dramatic difference
 * visible in the reference images (dry village → soaking wet village):
 *
 * - STONE_PATH: Large reflective puddles, dark wet surface, visible water pooling
 * - GRASS: Deep saturated green, almost muddy at edges, glistening
 * - WOOD: Dramatically darker (soaked), wet sheen across entire surface
 * - ROOF: Water streaming down in visible rivulets, specular highlights
 * - FOLIAGE: Much darker, heavy with water, occasional bright drips
 * - WATER: Intense ripples, rain impact circles
 *
 * Multi-texture pattern (via Korag AGTextureUnits):
 *   DefaultShaders.u_TexEx → Sampler at texture unit 1
 *   setTex(ctx, sampler, agTexture) in updateUniforms()
 *   texture2D(sampler, uv) in fragment shader
 */
class MaterialWeatherFilter(
    var time: Float = 0f,
    var weatherState: Float = 0.0f,  // 0=sun, 0.5=rain, 1.0=storm
    var windAngle: Float = 0.3f,     // rain drift direction
) : ShaderFilter() {

    private var materialTexture: AGTexture? = null

    /**
     * Set the full-resolution material map. Call once at setup.
     * The bitmap must match the scene image dimensions exactly.
     */
    fun setMaterialMap(bitmap: Bitmap32) {
        val tex = AGTexture()
        tex.upload(bitmap, mipmaps = false)
        materialTexture = tex
    }

    fun hasMaterialMap(): Boolean = materialTexture != null

    object MatWeatherUB : UniformBlock(fixedLocation = 18) {
        val u_Time by float()
        val u_Weather by float()
        val u_Wind by float()
        val u_HasMat by float()  // 1.0 = material map bound
    }

    companion object : BaseProgramProvider() {
        private val u_MaterialMap: Sampler = DefaultShaders.u_TexEx

        override val fragment: FragmentShader = FragmentShaderDefault {
            val coords = fragmentCoords
            val texSize = TexInfoUB.u_TextureSize
            val time = MatWeatherUB.u_Time
            val weather = MatWeatherUB.u_Weather
            val wind = MatWeatherUB.u_Wind
            val hasMat = MatWeatherUB.u_HasMat

            val original = tex(coords)
            val r = createTemp(Float1)
            val g = createTemp(Float1)
            val b = createTemp(Float1)
            SET(r, original.x)
            SET(g, original.y)
            SET(b, original.z)

            // UV normalized
            val uvX = createTemp(Float1)
            val uvY = createTemp(Float1)
            SET(uvX, coords.x / texSize.x)
            SET(uvY, coords.y / texSize.y)

            // === MATERIAL LOOKUP (second texture) ===
            val matSample = createTemp(Float4)
            SET(matSample, texture2D(u_MaterialMap, vec2(uvX, uvY)))
            val matId = createTemp(Float1)
            SET(matId, floor(matSample.x * 7f.lit + 0.5f.lit))

            // Material masks (from map)
            val isGrass = createTemp(Float1)
            val isStone = createTemp(Float1)
            val isWood = createTemp(Float1)
            val isRoof = createTemp(Float1)
            val isFoliage = createTemp(Float1)
            val isWater = createTemp(Float1)
            SET(isGrass, step(matId, 0.5f.lit) * hasMat)
            SET(isStone, step(0.5f.lit, matId) * step(matId, 1.5f.lit) * hasMat)
            SET(isWood, step(1.5f.lit, matId) * step(matId, 2.5f.lit) * hasMat)
            SET(isRoof, step(2.5f.lit, matId) * step(matId, 3.5f.lit) * hasMat)
            SET(isFoliage, step(3.5f.lit, matId) * step(matId, 4.5f.lit) * hasMat)
            SET(isWater, step(4.5f.lit, matId) * step(matId, 5.5f.lit) * hasMat)

            // === HSV FALLBACK (when no material map) ===
            val maxC = createTemp(Float1)
            val minC = createTemp(Float1)
            SET(maxC, max(max(r, g), b))
            SET(minC, min(min(r, g), b))
            val delta = createTemp(Float1)
            SET(delta, maxC - minC)
            val vv = createTemp(Float1)
            SET(vv, maxC)
            val sat = createTemp(Float1)
            SET(sat, delta / max(maxC, 0.001f.lit))
            val hue = createTemp(Float1)
            val isRM = createTemp(Float1)
            val isGM = createTemp(Float1)
            SET(isRM, step(g, r) * step(b, r))
            SET(isGM, step(r, g) * step(b, g) * (1f.lit - isRM))
            SET(hue, fract(
                isRM * ((g - b) / max(delta, 0.001f.lit) / 6f.lit) +
                isGM * (0.333f.lit + (b - r) / max(delta, 0.001f.lit) / 6f.lit) +
                (1f.lit - isRM) * (1f.lit - isGM) * (0.666f.lit + (r - g) / max(delta, 0.001f.lit) / 6f.lit)
            ))

            // Fallback classifications
            val noMat = createTemp(Float1)
            SET(noMat, 1f.lit - hasMat)
            val fbGrass = createTemp(Float1)
            SET(fbGrass, step(0.13f.lit, hue) * step(hue, 0.48f.lit) * step(0.15f.lit, sat) * step(0.40f.lit, vv) * noMat)
            val fbStone = createTemp(Float1)
            SET(fbStone, clamp(step(sat, 0.22f.lit) * step(0.6f.lit, vv) * noMat, 0f.lit, 1f.lit))
            val fbWood = createTemp(Float1)
            SET(fbWood, step(0.035f.lit, hue) * step(hue, 0.12f.lit) * step(0.25f.lit, vv) * step(vv, 0.66f.lit) * step(0.30f.lit, sat) * noMat)
            val fbRoof = createTemp(Float1)
            SET(fbRoof, step(0.035f.lit, hue) * step(hue, 0.085f.lit) * step(0.50f.lit, sat) * step(0.55f.lit, vv) * step(vv, 0.90f.lit) * noMat)
            val fbFoliage = createTemp(Float1)
            SET(fbFoliage, step(0.13f.lit, hue) * step(hue, 0.56f.lit) * step(0.10f.lit, sat) * step(vv, 0.43f.lit) * noMat)

            // Combine map + fallback
            val mG = createTemp(Float1); SET(mG, isGrass + fbGrass)
            val mS = createTemp(Float1); SET(mS, isStone + fbStone)
            val mW = createTemp(Float1); SET(mW, isWood + fbWood)
            val mR = createTemp(Float1); SET(mR, isRoof + fbRoof)
            val mF = createTemp(Float1); SET(mF, isFoliage + fbFoliage)
            val mWa = createTemp(Float1); SET(mWa, isWater)

            // === WEATHER EFFECTS — AGGRESSIVE ===
            val finalR = createTemp(Float1)
            val finalG = createTemp(Float1)
            val finalB = createTemp(Float1)
            SET(finalR, r)
            SET(finalG, g)
            SET(finalB, b)

            // --- GLOBAL: overcast sky darkening (strong) ---
            val overcast = createTemp(Float1)
            SET(overcast, 1f.lit - weather * 0.45f.lit)
            SET(finalR, finalR * overcast)
            SET(finalG, finalG * overcast)
            SET(finalB, finalB * overcast)

            // --- GLOBAL: desaturation in rain (muted colors) ---
            val lum = createTemp(Float1)
            SET(lum, finalR * 0.299f.lit + finalG * 0.587f.lit + finalB * 0.114f.lit)
            val desat = createTemp(Float1)
            SET(desat, weather * 0.25f.lit)  // 25% desaturation at full storm
            SET(finalR, mix(finalR, lum, desat))
            SET(finalG, mix(finalG, lum, desat))
            SET(finalB, mix(finalB, lum, desat))

            // --- STONE: LARGE PUDDLES with real reflections ---
            val stoneWet = createTemp(Float1)
            SET(stoneWet, mS * weather)

            // Puddle noise (large-scale Voronoi-like pattern)
            val pNoise1 = createTemp(Float1)
            SET(pNoise1, sin(uvX * 23f.lit + uvY * 17f.lit) * sin(uvX * 31f.lit - uvY * 11f.lit))
            val pNoise2 = createTemp(Float1)
            SET(pNoise2, sin(uvX * 7f.lit + uvY * 41f.lit + 2.3f.lit))
            val puddleField = createTemp(Float1)
            SET(puddleField, clamp((pNoise1 + pNoise2) * 0.5f.lit + 0.3f.lit, 0f.lit, 1f.lit))

            // Puddle threshold: more puddles at higher weather
            val hasPuddle = createTemp(Float1)
            SET(hasPuddle, stoneWet * step(puddleField, weather * 0.7f.lit))

            // Puddle: dark reflective surface + animated ripples
            val ripple = createTemp(Float1)
            SET(ripple, sin(uvX * 120f.lit + time * 3f.lit) * sin(uvY * 90f.lit + time * 2.2f.lit))
            SET(ripple, clamp(ripple * ripple, 0f.lit, 1f.lit) * 0.15f.lit)

            // Reflection: mirror the sky (slight blue-grey) + specular
            SET(finalR, mix(finalR, 0.15f.lit + ripple * 0.5f.lit, hasPuddle))
            SET(finalG, mix(finalG, 0.18f.lit + ripple * 0.6f.lit, hasPuddle))
            SET(finalB, mix(finalB, 0.25f.lit + ripple * 0.8f.lit, hasPuddle))

            // Non-puddle stone: wet darkening
            val stoneNoPuddle = createTemp(Float1)
            SET(stoneNoPuddle, stoneWet * (1f.lit - hasPuddle))
            SET(finalR, finalR * (1f.lit - stoneNoPuddle * 0.3f.lit))
            SET(finalG, finalG * (1f.lit - stoneNoPuddle * 0.3f.lit))
            SET(finalB, finalB * (1f.lit - stoneNoPuddle * 0.25f.lit))

            // --- GRASS: deep saturated green, almost muddy ---
            val grassWet = createTemp(Float1)
            SET(grassWet, mG * weather)
            // Push green channel hard, suppress red
            SET(finalR, finalR * (1f.lit - grassWet * 0.35f.lit))
            SET(finalG, finalG * (1f.lit + grassWet * 0.15f.lit))
            SET(finalB, finalB * (1f.lit - grassWet * 0.15f.lit))
            // Darkening (wet grass is darker)
            SET(finalR, finalR * (1f.lit - grassWet * 0.2f.lit))
            SET(finalG, finalG * (1f.lit - grassWet * 0.12f.lit))
            SET(finalB, finalB * (1f.lit - grassWet * 0.18f.lit))
            // Subtle water droplet highlights
            val grassDrop = createTemp(Float1)
            SET(grassDrop, fract(sin(uvX * 431f.lit + uvY * 197f.lit + time * 5f.lit) * 43758.5f.lit))
            SET(grassDrop, step(0.997f.lit, grassDrop) * grassWet * 0.2f.lit)
            SET(finalG, finalG + grassDrop)
            SET(finalB, finalB + grassDrop * 0.5f.lit)

            // --- WOOD: dramatically darker + wet sheen ---
            val woodWet = createTemp(Float1)
            SET(woodWet, mW * weather)
            // Soaked wood is MUCH darker
            SET(finalR, finalR * (1f.lit - woodWet * 0.45f.lit))
            SET(finalG, finalG * (1f.lit - woodWet * 0.4f.lit))
            SET(finalB, finalB * (1f.lit - woodWet * 0.3f.lit))
            // Wet sheen (broad specular across surface)
            val woodSheen = createTemp(Float1)
            SET(woodSheen, sin(uvX * 80f.lit + uvY * 40f.lit + time * 0.5f.lit))
            SET(woodSheen, clamp(woodSheen * 0.5f.lit + 0.3f.lit, 0f.lit, 1f.lit) * woodWet * 0.12f.lit)
            SET(finalR, finalR + woodSheen)
            SET(finalG, finalG + woodSheen)
            SET(finalB, finalB + woodSheen * 1.3f.lit)

            // --- ROOF: water streaming down in rivulets ---
            val roofWet = createTemp(Float1)
            SET(roofWet, mR * weather)
            // Roof darkens moderately (water runs off, doesn't soak in)
            SET(finalR, finalR * (1f.lit - roofWet * 0.25f.lit))
            SET(finalG, finalG * (1f.lit - roofWet * 0.22f.lit))
            SET(finalB, finalB * (1f.lit - roofWet * 0.18f.lit))

            // Water rivulets streaming down (multiple vertical streams)
            val streamX = createTemp(Float1)
            SET(streamX, fract(uvX * 60f.lit + sin(uvY * 20f.lit + time) * 0.1f.lit))
            val isStream = createTemp(Float1)
            // Thin streams (streamX near 0.5 with narrow band)
            SET(isStream, step(0.45f.lit, streamX) * step(streamX, 0.55f.lit))
            // Animate downward
            val streamFlow = createTemp(Float1)
            SET(streamFlow, fract(uvY * 4f.lit + time * 6f.lit))
            // Stream is intermittent (only visible parts)
            val streamVis = createTemp(Float1)
            SET(streamVis, step(0.3f.lit, streamFlow) * step(streamFlow, 0.8f.lit))
            val roofStream = createTemp(Float1)
            SET(roofStream, isStream * streamVis * roofWet * 0.35f.lit)
            // Bright bluish streaks
            SET(finalR, finalR + roofStream * 0.4f.lit)
            SET(finalG, finalG + roofStream * 0.6f.lit)
            SET(finalB, finalB + roofStream * 1.0f.lit)

            // Broad specular on wet roof tiles
            val roofSpec = createTemp(Float1)
            SET(roofSpec, sin(uvX * 50f.lit + time * 1.5f.lit) * sin(uvY * 35f.lit + time))
            SET(roofSpec, clamp(roofSpec * roofSpec * roofSpec, 0f.lit, 1f.lit) * roofWet * 0.15f.lit)
            SET(finalR, finalR + roofSpec)
            SET(finalG, finalG + roofSpec)
            SET(finalB, finalB + roofSpec * 1.2f.lit)

            // --- FOLIAGE: much darker, heavy with water ---
            val folWet = createTemp(Float1)
            SET(folWet, mF * weather)
            // Heavy darkening (leaves are soaked)
            SET(finalR, finalR * (1f.lit - folWet * 0.4f.lit))
            SET(finalG, finalG * (1f.lit - folWet * 0.25f.lit))
            SET(finalB, finalB * (1f.lit - folWet * 0.35f.lit))
            // Occasional bright drips falling from leaves
            val drip = createTemp(Float1)
            SET(drip, fract(sin(uvX * 251f.lit + uvY * 113f.lit + time * 7f.lit) * 43758.5f.lit))
            val dripVis = createTemp(Float1)
            SET(dripVis, step(0.993f.lit, drip) * folWet * 0.4f.lit)
            SET(finalR, finalR + dripVis * 0.6f.lit)
            SET(finalG, finalG + dripVis * 0.7f.lit)
            SET(finalB, finalB + dripVis * 1.0f.lit)

            // --- WATER: intense ripples + rain impact ---
            val waterEff = createTemp(Float1)
            SET(waterEff, mWa * weather)
            val wRipple = createTemp(Float1)
            SET(wRipple, sin(uvX * 100f.lit + time * 3f.lit) * sin(uvY * 80f.lit + time * 2.5f.lit))
            SET(wRipple, clamp(wRipple * 3f.lit, -1f.lit, 1f.lit) * 0.1f.lit * waterEff)
            SET(finalR, finalR + wRipple)
            SET(finalG, finalG + wRipple)
            SET(finalB, finalB + wRipple * 1.5f.lit)

            // --- FOG / ATMOSPHERE: thick ground-level mist ---
            val fogDensity = createTemp(Float1)
            // Fog is THICK — visible haze, not a subtle tint
            // Denser at ground level (bottom of image), less at top
            SET(fogDensity, weather * weather * (0.35f.lit + (1f.lit - uvY) * 0.2f.lit))
            // Fog color: cool grey-blue
            SET(finalR, mix(finalR, 0.55f.lit, fogDensity))
            SET(finalG, mix(finalG, 0.58f.lit, fogDensity))
            SET(finalB, mix(finalB, 0.65f.lit, fogDensity))

            // --- RAIN PARTICLES: visible diagonal streaks ---
            val rainDensity = createTemp(Float1)
            SET(rainDensity, weather * weather)  // quadratic: barely visible at 0.5, dense at 1.0

            // Multiple layers of rain at different speeds/angles
            val rain1 = createTemp(Float1)
            val rainUV1 = createTemp(Float1)
            SET(rainUV1, uvX * 300f.lit + uvY * 600f.lit + time * 20f.lit + wind * uvX * 150f.lit)
            SET(rain1, step(0.994f.lit, fract(sin(rainUV1) * 43758.5f.lit)))

            val rain2 = createTemp(Float1)
            val rainUV2 = createTemp(Float1)
            SET(rainUV2, uvX * 200f.lit + uvY * 450f.lit + time * 16f.lit + wind * uvX * 120f.lit + 7.3f.lit)
            SET(rain2, step(0.995f.lit, fract(sin(rainUV2) * 43758.5f.lit)))

            val rainTotal = createTemp(Float1)
            SET(rainTotal, clamp(rain1 + rain2, 0f.lit, 1f.lit) * rainDensity * 0.4f.lit)
            // Rain is white-blue
            SET(finalR, finalR + rainTotal * 0.7f.lit)
            SET(finalG, finalG + rainTotal * 0.8f.lit)
            SET(finalB, finalB + rainTotal * 1.0f.lit)

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
        val matTex = materialTexture
        if (matTex != null) {
            setTex(ctx, u_MaterialMap, matTex)
        }
        ctx[MatWeatherUB].push {
            it[u_Time] = time
            it[u_Weather] = weatherState
            it[u_Wind] = windAngle
            it[u_HasMat] = if (matTex != null) 1f else 0f
        }
    }
}
