package game.shader

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.image.bitmap.Bitmap32
import korlibs.korge.render.*
import korlibs.korge.view.filter.ShaderFilter

/**
 * Material-aware weather system with MULTI-TEXTURE support.
 *
 * This filter uses TWO textures:
 * 1. The scene (default sampler, texture unit 0) — the HD painted background
 * 2. The material map (u_MaterialMap, texture unit 1) — full-resolution bitmap
 *    where each pixel's R channel encodes the Material ID (0-7)
 *
 * Pipeline (offline → runtime):
 * 1. tools/mapbuilder/material_segment.py segments the image into a tile grid
 * 2. The material bitmap is upscaled to full resolution with nearest-neighbor,
 *    using HD-image edge detection to snap boundaries to real pixel contours
 * 3. At runtime, the shader samples the material map and applies per-material
 *    weather effects without any fragile HSV heuristics
 *
 * Material IDs (encoded in the R channel, normalized 0.0-1.0):
 *   0 = GRASS       (R ≈ 0.000) → saturates in rain, slight darkening
 *   1 = STONE_PATH  (R ≈ 0.143) → puddles form, wet reflections, darkens
 *   2 = WOOD        (R ≈ 0.286) → darkens significantly, subtle wet sheen
 *   3 = ROOF        (R ≈ 0.429) → water beads off (Abperlen), specular streaks
 *   4 = FOLIAGE     (R ≈ 0.571) → color deepens, drip highlights
 *   5 = WATER       (R ≈ 0.714) → caustics intensify, ripples
 *   6 = FLOWERS     (R ≈ 0.857) → slight darkening, droplet highlights
 *   7 = UNKNOWN     (R ≈ 1.000) → minimal effect
 *
 * The material map sampler uses NEAREST filtering (no interpolation) so material
 * boundaries stay sharp even at full resolution.
 *
 * KorGE multi-texture binding pattern (same as TransitionFilter):
 *   - Declare sampler via DefaultShaders.u_TexEx (built-in extra sampler, unit 1)
 *   - Bind via setTex(ctx, u_MaterialMap, agTexture) in updateUniforms()
 *   - Sample via texture2D(u_MaterialMap, uv) in fragment shader
 */
class MaterialWeatherFilter(
    var time: Float = 0f,
    var weatherState: Float = 0.0f,  // 0=sun, 0.5=rain, 1.0=storm
    var windAngle: Float = 0.3f,     // rain/fog drift direction
) : ShaderFilter() {

    /**
     * The material map texture. Set this to a full-resolution Bitmap32 where
     * each pixel's R channel = Material ID (0..7 mapped to 0..255).
     *
     * Generate via: tools/mapbuilder/material_segment.py --fullres
     * The bitmap MUST match the scene image dimensions exactly.
     */
    private var materialTexture: AGTexture? = null
    private var materialBitmap: Bitmap32? = null

    /**
     * Set the material map bitmap. This uploads it to the GPU as an AGTexture.
     * Call once at scene setup (not every frame).
     */
    fun setMaterialMap(bitmap: Bitmap32) {
        materialBitmap = bitmap
        val tex = AGTexture()
        tex.upload(bitmap, mipmaps = false)
        materialTexture = tex
    }

    /** Returns true if a material map has been set. */
    fun hasMaterialMap(): Boolean = materialTexture != null

    object MatWeatherUB : UniformBlock(fixedLocation = 18) {
        val u_Time by float()
        val u_Weather by float()
        val u_Wind by float()
        val u_HasMaterialMap by float()  // 1.0 if material map bound, 0.0 for fallback
    }

    companion object : BaseProgramProvider() {

        // Second texture sampler — uses KorGE's built-in extra sampler (unit 1)
        private val u_MaterialMap: Sampler = DefaultShaders.u_TexEx

        override val fragment: FragmentShader = FragmentShaderDefault {
            val coords = fragmentCoords
            val texSize = TexInfoUB.u_TextureSize
            val time = MatWeatherUB.u_Time
            val weather = MatWeatherUB.u_Weather
            val wind = MatWeatherUB.u_Wind
            val hasMaterialMap = MatWeatherUB.u_HasMaterialMap

            val original = tex(coords)
            val r = createTemp(Float1)
            val g = createTemp(Float1)
            val b = createTemp(Float1)
            SET(r, original.x)
            SET(g, original.y)
            SET(b, original.z)

            // === MATERIAL LOOKUP ===
            // Sample the material map (second texture). R channel = material ID.
            // Normalized UV from fragment coords:
            val uvX = createTemp(Float1)
            val uvY = createTemp(Float1)
            SET(uvX, coords.x / texSize.x)
            SET(uvY, coords.y / texSize.y)

            val matSample = createTemp(Float4)
            SET(matSample, texture2D(u_MaterialMap, vec2(uvX, uvY)))

            // Material ID from R channel (0.0-1.0 → 0-7)
            // Multiply by 7 and round to nearest int
            val matId = createTemp(Float1)
            SET(matId, floor(matSample.x * 7f.lit + 0.5f.lit))

            // === MATERIAL CLASSIFICATION from map ===
            // Each material is a soft-step around its ID value
            val isGrass = createTemp(Float1)
            SET(isGrass, step(matId, 0.5f.lit))  // matId == 0

            val isStone = createTemp(Float1)
            SET(isStone, step(0.5f.lit, matId) * step(matId, 1.5f.lit))  // matId == 1

            val isWood = createTemp(Float1)
            SET(isWood, step(1.5f.lit, matId) * step(matId, 2.5f.lit))  // matId == 2

            val isRoof = createTemp(Float1)
            SET(isRoof, step(2.5f.lit, matId) * step(matId, 3.5f.lit))  // matId == 3

            val isFoliage = createTemp(Float1)
            SET(isFoliage, step(3.5f.lit, matId) * step(matId, 4.5f.lit))  // matId == 4

            val isWater = createTemp(Float1)
            SET(isWater, step(4.5f.lit, matId) * step(matId, 5.5f.lit))  // matId == 5

            val isFlowers = createTemp(Float1)
            SET(isFlowers, step(5.5f.lit, matId) * step(matId, 6.5f.lit))  // matId == 6

            // === FALLBACK: HSV classification when no material map ===
            // (Kept for backwards compat / scenes without a material map)
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
            val isRedMax = createTemp(Float1)
            val isGreenMax = createTemp(Float1)
            SET(isRedMax, step(g, r) * step(b, r))
            SET(isGreenMax, step(r, g) * step(b, g) * (1f.lit - isRedMax))
            SET(hue, isRedMax * ((g - b) / max(delta, 0.001f.lit) / 6f.lit) +
                isGreenMax * (0.333f.lit + (b - r) / max(delta, 0.001f.lit) / 6f.lit) +
                (1f.lit - isRedMax) * (1f.lit - isGreenMax) * (0.666f.lit + (r - g) / max(delta, 0.001f.lit) / 6f.lit))
            SET(hue, fract(hue))

            // HSV-based fallback classification
            val fbStone = createTemp(Float1)
            SET(fbStone, step(sat, 0.22f.lit) * step(0.6f.lit, vv) +
                step(0.07f.lit, hue) * step(hue, 0.13f.lit) * step(0.86f.lit, vv) * step(0.35f.lit, sat) * step(sat, 0.6f.lit))
            SET(fbStone, clamp(fbStone, 0f.lit, 1f.lit))

            val fbRoof = createTemp(Float1)
            SET(fbRoof, step(0.035f.lit, hue) * step(hue, 0.085f.lit) *
                step(0.50f.lit, sat) * step(0.55f.lit, vv) * step(vv, 0.90f.lit) *
                (1f.lit - fbStone))

            val fbWood = createTemp(Float1)
            SET(fbWood, step(0.035f.lit, hue) * step(hue, 0.12f.lit) *
                step(0.30f.lit, sat) * step(0.25f.lit, vv) * step(vv, 0.66f.lit) *
                (1f.lit - fbStone) * (1f.lit - fbRoof))

            val fbGrass = createTemp(Float1)
            SET(fbGrass, step(0.13f.lit, hue) * step(hue, 0.48f.lit) *
                step(0.15f.lit, sat) * step(0.40f.lit, vv))

            val fbFoliage = createTemp(Float1)
            SET(fbFoliage, step(0.13f.lit, hue) * step(hue, 0.56f.lit) *
                step(0.10f.lit, sat) * step(vv, 0.43f.lit))

            val fbWater = createTemp(Float1)
            SET(fbWater, step(0.5f.lit, hue) * step(hue, 0.72f.lit) * step(0.15f.lit, sat))

            // Mix: use material map if available, HSV fallback otherwise
            val mGrass = createTemp(Float1)
            val mStone = createTemp(Float1)
            val mWood = createTemp(Float1)
            val mRoof = createTemp(Float1)
            val mFoliage = createTemp(Float1)
            val mWater = createTemp(Float1)
            SET(mGrass, mix(fbGrass, isGrass, hasMaterialMap))
            SET(mStone, mix(fbStone, isStone, hasMaterialMap))
            SET(mWood, mix(fbWood, isWood, hasMaterialMap))
            SET(mRoof, mix(fbRoof, isRoof, hasMaterialMap))
            SET(mFoliage, mix(fbFoliage, isFoliage, hasMaterialMap))
            SET(mWater, mix(fbWater, isWater, hasMaterialMap))

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
            SET(hasPuddle, mStone * step(puddleNoise, weather * 0.4f.lit))

            val puddleSpec = createTemp(Float1)
            SET(puddleSpec, sin(uvX * 60f.lit + time * 2f.lit) * sin(uvY * 45f.lit + time * 1.5f.lit))
            SET(puddleSpec, clamp(puddleSpec * puddleSpec * 0.3f.lit, 0f.lit, 0.3f.lit))
            SET(finalR, mix(finalR, finalR * 0.5f.lit + puddleSpec * 0.3f.lit, hasPuddle * weather))
            SET(finalG, mix(finalG, finalG * 0.55f.lit + puddleSpec * 0.35f.lit, hasPuddle * weather))
            SET(finalB, mix(finalB, finalB * 0.6f.lit + puddleSpec * 0.5f.lit + 0.1f.lit, hasPuddle * weather))

            // Non-puddle stone: just darken
            val stoneWet2 = createTemp(Float1)
            SET(stoneWet2, mStone * (1f.lit - hasPuddle) * weather)
            SET(finalR, finalR * (1f.lit - stoneWet2 * 0.15f.lit))
            SET(finalG, finalG * (1f.lit - stoneWet2 * 0.15f.lit))
            SET(finalB, finalB * (1f.lit - stoneWet2 * 0.12f.lit))

            // --- GRASS: saturate more ---
            val grassWet = createTemp(Float1)
            SET(grassWet, mGrass * weather)
            val grassLum = createTemp(Float1)
            SET(grassLum, finalR * 0.299f.lit + finalG * 0.587f.lit + finalB * 0.114f.lit)
            SET(finalR, mix(finalR, finalR + (finalR - grassLum) * 0.4f.lit, grassWet))
            SET(finalG, mix(finalG, finalG + (finalG - grassLum) * 0.5f.lit, grassWet))
            SET(finalB, mix(finalB, finalB + (finalB - grassLum) * 0.3f.lit, grassWet))
            SET(finalR, finalR * (1f.lit - grassWet * 0.1f.lit))
            SET(finalG, finalG * (1f.lit - grassWet * 0.05f.lit))
            SET(finalB, finalB * (1f.lit - grassWet * 0.1f.lit))

            // --- WOOD: darken + subtle sheen ---
            val woodWet = createTemp(Float1)
            SET(woodWet, mWood * weather)
            SET(finalR, finalR * (1f.lit - woodWet * 0.25f.lit))
            SET(finalG, finalG * (1f.lit - woodWet * 0.2f.lit))
            SET(finalB, finalB * (1f.lit - woodWet * 0.15f.lit))
            val woodSpec = createTemp(Float1)
            SET(woodSpec, fract(sin(uvX * 200f.lit + uvY * 150f.lit + time) * 43758.5f.lit))
            SET(woodSpec, step(0.98f.lit, woodSpec) * 0.15f.lit * woodWet)
            SET(finalR, finalR + woodSpec)
            SET(finalG, finalG + woodSpec)
            SET(finalB, finalB + woodSpec)

            // --- ROOF: water beads off (Abperlen) + specular streaks ---
            val roofWet = createTemp(Float1)
            SET(roofWet, mRoof * weather)
            SET(finalR, finalR * (1f.lit - roofWet * 0.12f.lit))
            SET(finalG, finalG * (1f.lit - roofWet * 0.10f.lit))
            SET(finalB, finalB * (1f.lit - roofWet * 0.08f.lit))
            // Specular streaks running down
            val streakUV = createTemp(Float1)
            SET(streakUV, uvX * 150f.lit + wind * 20f.lit + uvY * 30f.lit)
            val streakPhase = createTemp(Float1)
            SET(streakPhase, fract(sin(streakUV) * 43758.5f.lit))
            val streakAnim = createTemp(Float1)
            SET(streakAnim, fract(uvY * 8f.lit + time * 3f.lit + streakPhase * 6.28f.lit))
            val roofStreak = createTemp(Float1)
            SET(roofStreak, step(0.92f.lit, streakAnim) * step(0.85f.lit, streakPhase) * roofWet * 0.3f.lit)
            SET(finalR, finalR + roofStreak * 0.8f.lit)
            SET(finalG, finalG + roofStreak * 0.9f.lit)
            SET(finalB, finalB + roofStreak * 1.0f.lit)

            // --- FOLIAGE: color deepens + drip highlights ---
            val foliageWet = createTemp(Float1)
            SET(foliageWet, mFoliage * weather)
            SET(finalR, finalR * (1f.lit - foliageWet * 0.2f.lit))
            SET(finalG, finalG * (1f.lit + foliageWet * 0.08f.lit))
            SET(finalB, finalB * (1f.lit - foliageWet * 0.05f.lit))
            val dripNoise = createTemp(Float1)
            SET(dripNoise, fract(sin(uvX * 311f.lit + uvY * 127f.lit + time * 4f.lit) * 43758.5f.lit))
            val dripActive = createTemp(Float1)
            SET(dripActive, step(0.995f.lit, dripNoise) * foliageWet * 0.2f.lit)
            SET(finalR, finalR + dripActive)
            SET(finalG, finalG + dripActive)
            SET(finalB, finalB + dripActive * 1.3f.lit)

            // --- WATER: caustic intensification ---
            val waterEffect = createTemp(Float1)
            SET(waterEffect, mWater * weather)
            val caustic = createTemp(Float1)
            SET(caustic, sin(uvX * 80f.lit + time * 2.3f.lit) * sin(uvY * 60f.lit + time * 1.7f.lit))
            SET(caustic, clamp(caustic * caustic * 2f.lit, 0f.lit, 1f.lit) * 0.2f.lit)
            SET(finalR, finalR + caustic * waterEffect)
            SET(finalG, finalG + caustic * waterEffect)
            SET(finalB, finalB + caustic * waterEffect * 1.3f.lit)

            // --- FOG: ground-level haze ---
            val normalizedY = createTemp(Float1)
            SET(normalizedY, uvY)
            val fogDensity = createTemp(Float1)
            SET(fogDensity, weather * weather * (0.25f.lit + (1f.lit - normalizedY) * 0.12f.lit))
            val fogColor = 0.72f.lit
            SET(finalR, mix(finalR, fogColor, fogDensity))
            SET(finalG, mix(finalG, fogColor, fogDensity))
            SET(finalB, mix(finalB, fogColor * 1.05f.lit, fogDensity))

            // --- RAIN STREAKS ---
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

        // Bind material map as second texture (unit 1)
        val matTex = materialTexture
        if (matTex != null) {
            setTex(ctx, u_MaterialMap, matTex)
        }

        ctx[MatWeatherUB].push {
            it[u_Time] = time
            it[u_Weather] = weatherState
            it[u_Wind] = windAngle
            it[u_HasMaterialMap] = if (matTex != null) 1f else 0f
        }
    }
}
