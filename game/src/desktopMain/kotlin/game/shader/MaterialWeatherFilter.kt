package game.shader

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.image.bitmap.Bitmap32
import korlibs.korge.render.*
import korlibs.korge.view.filter.ShaderFilter

/**
 * Material-aware weather system — Multi-Texture, pixel-accurate.
 *
 * Material map is RGB:
 *   R = Material ID (0-255, decode: floor(R/255*7+0.5) → 0=Grass..7=Unknown)
 *   G = Flow angle on roof pixels (0-255 → 0°-360° direction of steepest descent)
 *   B = Drip zone intensity (0=none, 255=directly below roof edge, puddles form first here)
 *
 * Key behaviors:
 * - Roof rivulets flow in the direction computed from the roof slope (G channel)
 * - Puddles form FIRST in drip zones (below roof edges, no gutters)
 * - Water on stone collects in joints first (darker pixels = cracks)
 * - Rain streaks are global (visible everywhere)
 * - Grass effect: darken + saturate existing color, never adds green
 */
class MaterialWeatherFilter(
    var time: Float = 0f,
    var weatherState: Float = 0.0f,
    var windAngle: Float = 0.3f,
) : ShaderFilter() {

    private var materialTexture: AGTexture? = null

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
        val u_HasMat by float()
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

            val uvX = createTemp(Float1)
            val uvY = createTemp(Float1)
            SET(uvX, coords.x / texSize.x)
            SET(uvY, coords.y / texSize.y)

            // === MATERIAL MAP LOOKUP (RGB) ===
            val matSample = createTemp(Float4)
            SET(matSample, texture2D(u_MaterialMap, vec2(uvX, uvY)))
            // R channel = material ID
            val matId = createTemp(Float1)
            SET(matId, floor(matSample.x * 7f.lit + 0.5f.lit))
            // G channel = flow angle (0-1 → 0-2π)
            val flowAngle = createTemp(Float1)
            SET(flowAngle, matSample.y * 6.2832f.lit)  // 0-1 → 0-2π
            // B channel = drip zone intensity (0-1, higher = more drip influence)
            val dripIntensity = createTemp(Float1)
            SET(dripIntensity, matSample.z)

            // Material masks
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

            // === HSV FALLBACK (no material map) ===
            val maxC = createTemp(Float1)
            val minC = createTemp(Float1)
            SET(maxC, max(max(r, g), b))
            SET(minC, min(min(r, g), b))
            val delta = createTemp(Float1)
            SET(delta, maxC - minC)
            val vv = createTemp(Float1); SET(vv, maxC)
            val sat = createTemp(Float1); SET(sat, delta / max(maxC, 0.001f.lit))
            val hue = createTemp(Float1)
            val isRM = createTemp(Float1); val isGM = createTemp(Float1)
            SET(isRM, step(g, r) * step(b, r))
            SET(isGM, step(r, g) * step(b, g) * (1f.lit - isRM))
            SET(hue, fract(
                isRM * ((g - b) / max(delta, 0.001f.lit) / 6f.lit) +
                isGM * (0.333f.lit + (b - r) / max(delta, 0.001f.lit) / 6f.lit) +
                (1f.lit - isRM) * (1f.lit - isGM) * (0.666f.lit + (r - g) / max(delta, 0.001f.lit) / 6f.lit)
            ))
            val noMat = createTemp(Float1); SET(noMat, 1f.lit - hasMat)
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

            val mG = createTemp(Float1); SET(mG, isGrass + fbGrass)
            val mS = createTemp(Float1); SET(mS, isStone + fbStone)
            val mW = createTemp(Float1); SET(mW, isWood + fbWood)
            val mR = createTemp(Float1); SET(mR, isRoof + fbRoof)
            val mF = createTemp(Float1); SET(mF, isFoliage + fbFoliage)
            val mWa = createTemp(Float1); SET(mWa, isWater)

            // === WEATHER EFFECTS ===
            val finalR = createTemp(Float1)
            val finalG = createTemp(Float1)
            val finalB = createTemp(Float1)
            SET(finalR, r)
            SET(finalG, g)
            SET(finalB, b)

            // --- GLOBAL: overcast + desaturation ---
            SET(finalR, finalR * (1f.lit - weather * 0.45f.lit))
            SET(finalG, finalG * (1f.lit - weather * 0.45f.lit))
            SET(finalB, finalB * (1f.lit - weather * 0.45f.lit))
            val lum = createTemp(Float1)
            SET(lum, finalR * 0.299f.lit + finalG * 0.587f.lit + finalB * 0.114f.lit)
            SET(finalR, mix(finalR, lum, weather * 0.2f.lit))
            SET(finalG, mix(finalG, lum, weather * 0.2f.lit))
            SET(finalB, mix(finalB, lum, weather * 0.2f.lit))

            // =================================================================
            // STONE: Water in JOINTS first, then puddles
            // =================================================================
            val stoneWet = createTemp(Float1)
            SET(stoneWet, mS * weather)
            // Pixel brightness determines joint vs face
            val pixBright = createTemp(Float1)
            SET(pixBright, (r + g + b) / 3f.lit)
            // Darker pixels (joints) wet first, threshold rises with weather
            val jointThresh = createTemp(Float1)
            SET(jointThresh, 0.4f.lit + weather * 0.5f.lit)
            val jointWet = createTemp(Float1)
            SET(jointWet, stoneWet * step(pixBright, jointThresh))
            SET(finalR, finalR * (1f.lit - jointWet * 0.35f.lit))
            SET(finalG, finalG * (1f.lit - jointWet * 0.35f.lit))
            SET(finalB, finalB * (1f.lit - jointWet * 0.3f.lit))

            // Puddle field (organic shapes)
            val pn1 = createTemp(Float1)
            val pn2 = createTemp(Float1)
            val pn3 = createTemp(Float1)
            SET(pn1, sin(uvX * 19f.lit + uvY * 13f.lit + 1.7f.lit) * sin(uvX * 29f.lit - uvY * 7f.lit))
            SET(pn2, sin(uvX * 5f.lit + uvY * 37f.lit + 3.1f.lit) * 0.7f.lit)
            SET(pn3, sin(uvX * 43f.lit + uvY * 23f.lit) * 0.3f.lit)
            val puddleField = createTemp(Float1)
            SET(puddleField, clamp((pn1 + pn2 + pn3) * 0.35f.lit + 0.4f.lit, 0f.lit, 1f.lit))

            // =================================================================
            // DRIP ZONE: puddles form FIRST below roof edges
            // =================================================================
            // dripIntensity from B channel: 1.0 directly below traufe, fades out
            // Puddle threshold is LOWER in drip zones (puddles appear earlier)
            val puddleThresh = createTemp(Float1)
            // Normal: needs weather*0.6 to form. Drip zone: needs much less
            SET(puddleThresh, mix(weather * 0.6f.lit, weather * 1.2f.lit, dripIntensity * hasMat))

            // Puddles on stone (including drip-zone boost)
            val puddleOnStone = createTemp(Float1)
            SET(puddleOnStone, mS * step(puddleField, puddleThresh))

            // Puddles bleed onto grass near stone
            val texelOff = createTemp(Float1)
            SET(texelOff, 10f.lit / texSize.x)
            val nS1 = createTemp(Float4); SET(nS1, texture2D(u_MaterialMap, vec2(uvX + texelOff, uvY)))
            val nS2 = createTemp(Float4); SET(nS2, texture2D(u_MaterialMap, vec2(uvX - texelOff, uvY)))
            val nS3 = createTemp(Float4); SET(nS3, texture2D(u_MaterialMap, vec2(uvX, uvY + texelOff)))
            val nS4 = createTemp(Float4); SET(nS4, texture2D(u_MaterialMap, vec2(uvX, uvY - texelOff)))
            val nId1 = createTemp(Float1); SET(nId1, floor(nS1.x * 7f.lit + 0.5f.lit))
            val nId2 = createTemp(Float1); SET(nId2, floor(nS2.x * 7f.lit + 0.5f.lit))
            val nId3 = createTemp(Float1); SET(nId3, floor(nS3.x * 7f.lit + 0.5f.lit))
            val nId4 = createTemp(Float1); SET(nId4, floor(nS4.x * 7f.lit + 0.5f.lit))
            val nearStone = createTemp(Float1)
            SET(nearStone, clamp(
                step(0.5f.lit, nId1) * step(nId1, 1.5f.lit) +
                step(0.5f.lit, nId2) * step(nId2, 1.5f.lit) +
                step(0.5f.lit, nId3) * step(nId3, 1.5f.lit) +
                step(0.5f.lit, nId4) * step(nId4, 1.5f.lit), 0f.lit, 1f.lit))
            val puddleBleed = createTemp(Float1)
            SET(puddleBleed, mG * nearStone * hasMat * step(puddleField, puddleThresh * 0.8f.lit))

            // Drip zone puddles on GRASS (even without nearby stone)
            val dripPuddle = createTemp(Float1)
            SET(dripPuddle, mG * dripIntensity * hasMat * step(puddleField, weather * 0.9f.lit))

            val hasPuddle = createTemp(Float1)
            SET(hasPuddle, clamp(puddleOnStone + puddleBleed + dripPuddle, 0f.lit, 1f.lit))

            // Puddle rendering
            val ripple = createTemp(Float1)
            SET(ripple, sin(uvX * 100f.lit + time * 2.8f.lit) * sin(uvY * 75f.lit + time * 2f.lit))
            SET(ripple, clamp(ripple * ripple, 0f.lit, 1f.lit) * 0.12f.lit)
            val impSeed = createTemp(Float1)
            SET(impSeed, fract(sin(uvX * 97f.lit + uvY * 53f.lit) * 43758.5f.lit))
            val impPhase = createTemp(Float1)
            SET(impPhase, fract(time * 1.5f.lit + impSeed * 6.28f.lit))
            val impRing = createTemp(Float1)
            SET(impRing, step(0.92f.lit, impSeed) * sin(impPhase * 12f.lit) * (1f.lit - impPhase))
            SET(impRing, clamp(impRing * 0.12f.lit, 0f.lit, 0.12f.lit))

            val pudW = createTemp(Float1)
            SET(pudW, hasPuddle * weather)
            SET(finalR, mix(finalR, 0.10f.lit + ripple * 0.3f.lit + impRing, pudW))
            SET(finalG, mix(finalG, 0.13f.lit + ripple * 0.4f.lit + impRing, pudW))
            SET(finalB, mix(finalB, 0.20f.lit + ripple * 0.6f.lit + impRing * 1.3f.lit, pudW))

            // --- GRASS: darken + contrast, no green tint ---
            val grassWet = createTemp(Float1)
            SET(grassWet, mG * weather * (1f.lit - clamp(puddleBleed + dripPuddle, 0f.lit, 1f.lit)))
            SET(finalR, finalR * (1f.lit - grassWet * 0.3f.lit))
            SET(finalG, finalG * (1f.lit - grassWet * 0.15f.lit))
            SET(finalB, finalB * (1f.lit - grassWet * 0.25f.lit))
            val gLum = createTemp(Float1)
            SET(gLum, finalR * 0.299f.lit + finalG * 0.587f.lit + finalB * 0.114f.lit)
            SET(finalR, mix(finalR, finalR + (finalR - gLum) * 0.3f.lit, grassWet))
            SET(finalG, mix(finalG, finalG + (finalG - gLum) * 0.3f.lit, grassWet))
            SET(finalB, mix(finalB, finalB + (finalB - gLum) * 0.3f.lit, grassWet))

            // --- WOOD: soaked dark + sheen ---
            val woodWet = createTemp(Float1)
            SET(woodWet, mW * weather)
            SET(finalR, finalR * (1f.lit - woodWet * 0.45f.lit))
            SET(finalG, finalG * (1f.lit - woodWet * 0.4f.lit))
            SET(finalB, finalB * (1f.lit - woodWet * 0.3f.lit))
            val wSheen = createTemp(Float1)
            SET(wSheen, sin(uvX * 60f.lit + uvY * 30f.lit + time * 0.4f.lit))
            SET(wSheen, clamp(wSheen * 0.4f.lit + 0.3f.lit, 0f.lit, 0.5f.lit) * woodWet * 0.08f.lit)
            SET(finalR, finalR + wSheen)
            SET(finalG, finalG + wSheen)
            SET(finalB, finalB + wSheen * 1.2f.lit)

            // =================================================================
            // ROOF: uniformly wet + flow-directed rivulets
            // =================================================================
            val roofWet = createTemp(Float1)
            SET(roofWet, mR * weather)
            // Uniform wet base (entire roof)
            SET(finalR, finalR * (1f.lit - roofWet * 0.35f.lit))
            SET(finalG, finalG * (1f.lit - roofWet * 0.32f.lit))
            SET(finalB, finalB * (1f.lit - roofWet * 0.22f.lit))
            // Uniform gloss
            val rGloss = createTemp(Float1)
            SET(rGloss, clamp(sin(uvX * 20f.lit + time * 0.3f.lit) * sin(uvY * 15f.lit + time * 0.2f.lit) * 0.3f.lit + 0.15f.lit, 0f.lit, 0.3f.lit))
            SET(finalR, finalR + rGloss * roofWet * 0.08f.lit)
            SET(finalG, finalG + rGloss * roofWet * 0.09f.lit)
            SET(finalB, finalB + rGloss * roofWet * 0.12f.lit)

            // Rivulets: flow in the direction of the computed slope (flowAngle from G channel)
            // Project UV along flow direction to create streaks aligned with roof slope
            val flowDirX = createTemp(Float1)
            val flowDirY = createTemp(Float1)
            SET(flowDirX, cos(flowAngle))
            SET(flowDirY, sin(flowAngle))
            // Project fragment position onto flow direction
            val flowProj = createTemp(Float1)
            SET(flowProj, uvX * flowDirX + uvY * flowDirY)
            // Perpendicular = across the rivulet (thin)
            val flowPerp = createTemp(Float1)
            SET(flowPerp, uvX * (-flowDirY) + uvY * flowDirX)

            // Rivulet pattern: thin streaks along flow, spaced by perpendicular
            val rivuletX = createTemp(Float1)
            SET(rivuletX, fract(flowPerp * 40f.lit + sin(flowProj * 10f.lit) * 0.1f.lit))
            val isRivulet = createTemp(Float1)
            SET(isRivulet, step(0.45f.lit, rivuletX) * step(rivuletX, 0.55f.lit))
            // Animate along flow direction
            val rivFlow = createTemp(Float1)
            SET(rivFlow, fract(flowProj * 5f.lit + time * 4f.lit))
            val rivVis = createTemp(Float1)
            SET(rivVis, step(0.2f.lit, rivFlow) * step(rivFlow, 0.7f.lit))
            val rivulet = createTemp(Float1)
            SET(rivulet, isRivulet * rivVis * roofWet * 0.18f.lit)
            SET(finalR, finalR + rivulet * 0.4f.lit)
            SET(finalG, finalG + rivulet * 0.5f.lit)
            SET(finalB, finalB + rivulet * 0.8f.lit)

            // --- FOLIAGE: dark, heavy ---
            val folWet = createTemp(Float1)
            SET(folWet, mF * weather)
            SET(finalR, finalR * (1f.lit - folWet * 0.4f.lit))
            SET(finalG, finalG * (1f.lit - folWet * 0.25f.lit))
            SET(finalB, finalB * (1f.lit - folWet * 0.35f.lit))

            // --- DROPLETS: clustered, random ---
            val clN = createTemp(Float1)
            SET(clN, sin(uvX * 11f.lit + uvY * 7f.lit + time * 0.2f.lit) * sin(uvX * 5f.lit - uvY * 13f.lit + 2.1f.lit))
            SET(clN, clamp(clN + 0.15f.lit, 0f.lit, 1f.lit))
            val dSeed = createTemp(Float1)
            SET(dSeed, fract(sin(uvX * 347f.lit + uvY * 193f.lit + floor(time * 3f.lit) * 7.3f.lit) * 43758.5f.lit))
            val dTh = createTemp(Float1)
            SET(dTh, 1f.lit - weather * 0.012f.lit * clN)
            val dAct = createTemp(Float1)
            SET(dAct, step(dTh, dSeed) * (mG + mF + mR + mW) * weather)
            val dBr = createTemp(Float1)
            SET(dBr, fract(dSeed * 7.77f.lit) * 0.25f.lit + 0.1f.lit)
            SET(finalR, finalR + dAct * dBr * 0.5f.lit)
            SET(finalG, finalG + dAct * dBr * 0.6f.lit)
            SET(finalB, finalB + dAct * dBr * 0.9f.lit)

            // --- WATER: ripples ---
            val wE = createTemp(Float1)
            SET(wE, mWa * weather)
            val wR2 = createTemp(Float1)
            SET(wR2, sin(uvX * 100f.lit + time * 3f.lit) * sin(uvY * 80f.lit + time * 2.5f.lit))
            SET(wR2, clamp(wR2 * 3f.lit, -1f.lit, 1f.lit) * 0.1f.lit * wE)
            SET(finalR, finalR + wR2)
            SET(finalG, finalG + wR2)
            SET(finalB, finalB + wR2 * 1.5f.lit)

            // --- FOG ---
            val fogD = createTemp(Float1)
            SET(fogD, weather * weather * (0.3f.lit + (1f.lit - uvY) * 0.18f.lit))
            SET(finalR, mix(finalR, 0.50f.lit, fogD))
            SET(finalG, mix(finalG, 0.53f.lit, fogD))
            SET(finalB, mix(finalB, 0.60f.lit, fogD))

            // --- RAIN: global diagonal streaks ---
            val rainD = createTemp(Float1)
            SET(rainD, weather * weather)
            val rn1 = createTemp(Float1)
            SET(rn1, step(0.993f.lit, fract(sin(uvX * 280f.lit + uvY * 550f.lit + time * 18f.lit + wind * uvX * 140f.lit) * 43758.5f.lit)))
            val rn2 = createTemp(Float1)
            SET(rn2, step(0.994f.lit, fract(sin(uvX * 180f.lit + uvY * 400f.lit + time * 14f.lit + wind * uvX * 100f.lit + 5.7f.lit) * 43758.5f.lit)))
            val rnTot = createTemp(Float1)
            SET(rnTot, clamp(rn1 + rn2, 0f.lit, 1f.lit) * rainD * 0.35f.lit)
            SET(finalR, finalR + rnTot * 0.65f.lit)
            SET(finalG, finalG + rnTot * 0.75f.lit)
            SET(finalB, finalB + rnTot * 1.0f.lit)

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
