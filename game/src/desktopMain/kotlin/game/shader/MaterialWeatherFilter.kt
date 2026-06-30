package game.shader

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.image.bitmap.Bitmap32
import korlibs.korge.render.*
import korlibs.korge.view.filter.ShaderFilter

/**
 * Material-aware weather v5 — Multi-Texture, pixel-accurate, physically motivated.
 *
 * Material map RGB channels:
 *   R = Material ID (decode: floor(R/255*7+0.5))
 *   G = Flow angle on roof (per-face, from ridge detection: left=108°, right=72°)
 *   B = Drip zone (0-180=intensity below traufe) OR Window marker (220=window)
 *
 * v5 improvements:
 * - Roof rivulets follow geometrically computed slope (first→traufe)
 * - Puddles form first in drip zones (no gutter → water drips down)
 * - Stone: joints wet first (darker pixels = cracks fill first)
 * - Grass: darken+contrast only, never green tint
 * - Fog restricted to ground level only
 * - Cloud reflections on roofs and puddles
 * - Interior lights glow through windows
 * - Shadows softened (diffuse overcast = no hard shadows)
 * - Contrast boost (wet scene is more contrasty)
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

            // === MATERIAL MAP ===
            val matSample = createTemp(Float4)
            SET(matSample, texture2D(u_MaterialMap, vec2(uvX, uvY)))
            val matId = createTemp(Float1)
            SET(matId, floor(matSample.x * 7f.lit + 0.5f.lit))
            // G = flow angle (0-1 → 0-2π)
            val flowAngle = createTemp(Float1)
            SET(flowAngle, matSample.y * 6.2832f.lit)
            // B = drip/window info
            val bChannel = createTemp(Float1)
            SET(bChannel, matSample.z)
            // Drip zone: B < 0.75 (0-180/255)
            val dripIntensity = createTemp(Float1)
            SET(dripIntensity, clamp(bChannel / 0.71f.lit, 0f.lit, 1f.lit) * step(bChannel, 0.75f.lit))
            // Window: B ≈ 0.86 (220/255)
            val isWindow = createTemp(Float1)
            SET(isWindow, step(0.8f.lit, bChannel) * step(bChannel, 0.92f.lit) * hasMat)

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

            // HSV fallback (no map)
            val maxC = createTemp(Float1); val minC = createTemp(Float1)
            SET(maxC, max(max(r, g), b)); SET(minC, min(min(r, g), b))
            val delta = createTemp(Float1); SET(delta, maxC - minC)
            val vv = createTemp(Float1); SET(vv, maxC)
            val sat = createTemp(Float1); SET(sat, delta / max(maxC, 0.001f.lit))
            val hue = createTemp(Float1)
            val isRM = createTemp(Float1); val isGM = createTemp(Float1)
            SET(isRM, step(g, r) * step(b, r))
            SET(isGM, step(r, g) * step(b, g) * (1f.lit - isRM))
            SET(hue, fract(isRM * ((g - b) / max(delta, 0.001f.lit) / 6f.lit) +
                isGM * (0.333f.lit + (b - r) / max(delta, 0.001f.lit) / 6f.lit) +
                (1f.lit - isRM) * (1f.lit - isGM) * (0.666f.lit + (r - g) / max(delta, 0.001f.lit) / 6f.lit)))
            val noMat = createTemp(Float1); SET(noMat, 1f.lit - hasMat)
            val mG = createTemp(Float1); SET(mG, isGrass + step(0.13f.lit, hue) * step(hue, 0.48f.lit) * step(0.15f.lit, sat) * step(0.40f.lit, vv) * noMat)
            val mS = createTemp(Float1); SET(mS, isStone + clamp(step(sat, 0.22f.lit) * step(0.6f.lit, vv) * noMat, 0f.lit, 1f.lit))
            val mW = createTemp(Float1); SET(mW, isWood + step(0.035f.lit, hue) * step(hue, 0.12f.lit) * step(0.25f.lit, vv) * step(vv, 0.66f.lit) * step(0.30f.lit, sat) * noMat)
            val mR = createTemp(Float1); SET(mR, isRoof + step(0.035f.lit, hue) * step(hue, 0.085f.lit) * step(0.50f.lit, sat) * step(0.55f.lit, vv) * step(vv, 0.90f.lit) * noMat)
            val mF = createTemp(Float1); SET(mF, isFoliage + step(0.13f.lit, hue) * step(hue, 0.56f.lit) * step(0.10f.lit, sat) * step(vv, 0.43f.lit) * noMat)

            // === EFFECTS ===
            val finalR = createTemp(Float1)
            val finalG = createTemp(Float1)
            val finalB = createTemp(Float1)
            SET(finalR, r); SET(finalG, g); SET(finalB, b)

            // --- SHADOW SOFTENING: lighten dark areas (diffuse overcast) ---
            val pixLum = createTemp(Float1)
            SET(pixLum, finalR * 0.299f.lit + finalG * 0.587f.lit + finalB * 0.114f.lit)
            val shadowLift = createTemp(Float1)
            // Darker pixels get lifted more (shadows become less harsh)
            SET(shadowLift, (1f.lit - pixLum) * weather * 0.15f.lit)
            SET(finalR, finalR + shadowLift)
            SET(finalG, finalG + shadowLift)
            SET(finalB, finalB + shadowLift)

            // --- OVERCAST: darken midtones/highlights (but shadows already lifted) ---
            SET(finalR, finalR * (1f.lit - weather * 0.38f.lit))
            SET(finalG, finalG * (1f.lit - weather * 0.38f.lit))
            SET(finalB, finalB * (1f.lit - weather * 0.38f.lit))

            // --- DESATURATION ---
            val lum = createTemp(Float1)
            SET(lum, finalR * 0.299f.lit + finalG * 0.587f.lit + finalB * 0.114f.lit)
            SET(finalR, mix(finalR, lum, weather * 0.18f.lit))
            SET(finalG, mix(finalG, lum, weather * 0.18f.lit))
            SET(finalB, mix(finalB, lum, weather * 0.18f.lit))

            // --- CONTRAST BOOST (wet scenes are more contrasty) ---
            val contrast = createTemp(Float1)
            SET(contrast, 1f.lit + weather * 0.15f.lit)
            SET(finalR, (finalR - 0.5f.lit) * contrast + 0.5f.lit)
            SET(finalG, (finalG - 0.5f.lit) * contrast + 0.5f.lit)
            SET(finalB, (finalB - 0.5f.lit) * contrast + 0.5f.lit)

            // =================================================================
            // STONE: joints wet first → puddles
            // =================================================================
            val stoneWet = createTemp(Float1); SET(stoneWet, mS * weather)
            val pixBr = createTemp(Float1); SET(pixBr, (r + g + b) / 3f.lit)
            val jointTh = createTemp(Float1); SET(jointTh, 0.4f.lit + weather * 0.5f.lit)
            val jointW = createTemp(Float1); SET(jointW, stoneWet * step(pixBr, jointTh))
            SET(finalR, finalR * (1f.lit - jointW * 0.3f.lit))
            SET(finalG, finalG * (1f.lit - jointW * 0.3f.lit))
            SET(finalB, finalB * (1f.lit - jointW * 0.25f.lit))

            // Puddle field
            val pn1 = createTemp(Float1); val pn2 = createTemp(Float1)
            SET(pn1, sin(uvX * 19f.lit + uvY * 13f.lit + 1.7f.lit) * sin(uvX * 29f.lit - uvY * 7f.lit))
            SET(pn2, sin(uvX * 5f.lit + uvY * 37f.lit + 3.1f.lit) * 0.7f.lit)
            val pField = createTemp(Float1)
            SET(pField, clamp((pn1 + pn2) * 0.4f.lit + 0.4f.lit, 0f.lit, 1f.lit))

            // Drip zone puddles form earlier
            val pThresh = createTemp(Float1)
            SET(pThresh, mix(weather * 0.55f.lit, weather * 1.3f.lit, dripIntensity * hasMat))
            val puddleStone = createTemp(Float1)
            SET(puddleStone, mS * step(pField, pThresh))

            // Bleed onto grass
            val tOff = createTemp(Float1); SET(tOff, 12f.lit / texSize.x)
            val ns1 = createTemp(Float4); SET(ns1, texture2D(u_MaterialMap, vec2(uvX + tOff, uvY)))
            val ns2 = createTemp(Float4); SET(ns2, texture2D(u_MaterialMap, vec2(uvX - tOff, uvY)))
            val ns3 = createTemp(Float4); SET(ns3, texture2D(u_MaterialMap, vec2(uvX, uvY + tOff)))
            val ns4 = createTemp(Float4); SET(ns4, texture2D(u_MaterialMap, vec2(uvX, uvY - tOff)))
            val nearS = createTemp(Float1)
            SET(nearS, clamp(
                step(0.5f.lit, floor(ns1.x*7f.lit+0.5f.lit)) * step(floor(ns1.x*7f.lit+0.5f.lit), 1.5f.lit) +
                step(0.5f.lit, floor(ns2.x*7f.lit+0.5f.lit)) * step(floor(ns2.x*7f.lit+0.5f.lit), 1.5f.lit) +
                step(0.5f.lit, floor(ns3.x*7f.lit+0.5f.lit)) * step(floor(ns3.x*7f.lit+0.5f.lit), 1.5f.lit) +
                step(0.5f.lit, floor(ns4.x*7f.lit+0.5f.lit)) * step(floor(ns4.x*7f.lit+0.5f.lit), 1.5f.lit), 0f.lit, 1f.lit))
            val pBleed = createTemp(Float1)
            SET(pBleed, mG * nearS * hasMat * step(pField, pThresh * 0.8f.lit))
            val dripP = createTemp(Float1)
            SET(dripP, mG * dripIntensity * hasMat * step(pField, weather * 0.8f.lit))

            val hasPud = createTemp(Float1)
            SET(hasPud, clamp(puddleStone + pBleed + dripP, 0f.lit, 1f.lit))

            // Puddle rendering + CLOUD REFLECTIONS
            val ripple = createTemp(Float1)
            SET(ripple, sin(uvX * 90f.lit + time * 2.5f.lit) * sin(uvY * 70f.lit + time * 1.8f.lit))
            SET(ripple, clamp(ripple * ripple, 0f.lit, 1f.lit) * 0.1f.lit)
            // Cloud reflection: slow-moving large-scale bright patches
            val cloudRef = createTemp(Float1)
            SET(cloudRef, sin((uvX + time * 0.02f.lit) * 3f.lit) * sin((uvY + time * 0.015f.lit) * 2f.lit))
            SET(cloudRef, clamp(cloudRef * 0.5f.lit + 0.3f.lit, 0f.lit, 0.5f.lit) * 0.12f.lit)

            val pudW = createTemp(Float1); SET(pudW, hasPud * weather)
            SET(finalR, mix(finalR, 0.08f.lit + ripple * 0.2f.lit + cloudRef * 0.8f.lit, pudW))
            SET(finalG, mix(finalG, 0.11f.lit + ripple * 0.3f.lit + cloudRef * 0.9f.lit, pudW))
            SET(finalB, mix(finalB, 0.18f.lit + ripple * 0.5f.lit + cloudRef * 1.0f.lit, pudW))

            // --- GRASS: darken + contrast only ---
            val gW = createTemp(Float1); SET(gW, mG * weather * (1f.lit - clamp(pBleed + dripP, 0f.lit, 1f.lit)))
            SET(finalR, finalR * (1f.lit - gW * 0.28f.lit))
            SET(finalG, finalG * (1f.lit - gW * 0.12f.lit))
            SET(finalB, finalB * (1f.lit - gW * 0.22f.lit))

            // --- WOOD: soaked dark ---
            val wW = createTemp(Float1); SET(wW, mW * weather * (1f.lit - isWindow))
            SET(finalR, finalR * (1f.lit - wW * 0.4f.lit))
            SET(finalG, finalG * (1f.lit - wW * 0.35f.lit))
            SET(finalB, finalB * (1f.lit - wW * 0.25f.lit))

            // =================================================================
            // WINDOWS: warm interior light glows through
            // =================================================================
            val winGlow = createTemp(Float1)
            SET(winGlow, isWindow * weather)
            // Warm orange glow
            SET(finalR, mix(finalR, 0.9f.lit, winGlow * 0.7f.lit))
            SET(finalG, mix(finalG, 0.6f.lit, winGlow * 0.6f.lit))
            SET(finalB, mix(finalB, 0.2f.lit, winGlow * 0.5f.lit))

            // =================================================================
            // ROOF: uniformly wet + cloud reflections + flow-directed rivulets
            // =================================================================
            val rW = createTemp(Float1); SET(rW, mR * weather)
            SET(finalR, finalR * (1f.lit - rW * 0.3f.lit))
            SET(finalG, finalG * (1f.lit - rW * 0.28f.lit))
            SET(finalB, finalB * (1f.lit - rW * 0.2f.lit))
            // Cloud reflection on wet roof
            val roofCloud = createTemp(Float1)
            SET(roofCloud, sin((uvX + time * 0.025f.lit) * 4f.lit) * sin((uvY + time * 0.02f.lit) * 3f.lit))
            SET(roofCloud, clamp(roofCloud * 0.4f.lit + 0.2f.lit, 0f.lit, 0.4f.lit) * rW * 0.1f.lit)
            SET(finalR, finalR + roofCloud * 0.7f.lit)
            SET(finalG, finalG + roofCloud * 0.8f.lit)
            SET(finalB, finalB + roofCloud * 1.0f.lit)

            // Rivulets: flow along computed direction (from G channel)
            val fDirX = createTemp(Float1); SET(fDirX, cos(flowAngle))
            val fDirY = createTemp(Float1); SET(fDirY, sin(flowAngle))
            val fProj = createTemp(Float1); SET(fProj, uvX * fDirX + uvY * fDirY)
            val fPerp = createTemp(Float1); SET(fPerp, uvX * (-fDirY) + uvY * fDirX)
            val rivX = createTemp(Float1)
            SET(rivX, fract(fPerp * 35f.lit + sin(fProj * 8f.lit) * 0.08f.lit))
            val isRiv = createTemp(Float1)
            SET(isRiv, step(0.44f.lit, rivX) * step(rivX, 0.56f.lit))
            val rivF = createTemp(Float1); SET(rivF, fract(fProj * 4f.lit + time * 3.5f.lit))
            val rivV = createTemp(Float1); SET(rivV, step(0.2f.lit, rivF) * step(rivF, 0.7f.lit))
            val riv = createTemp(Float1); SET(riv, isRiv * rivV * rW * 0.15f.lit)
            SET(finalR, finalR + riv * 0.4f.lit)
            SET(finalG, finalG + riv * 0.5f.lit)
            SET(finalB, finalB + riv * 0.7f.lit)

            // --- FOLIAGE ---
            val fW = createTemp(Float1); SET(fW, mF * weather)
            SET(finalR, finalR * (1f.lit - fW * 0.35f.lit))
            SET(finalG, finalG * (1f.lit - fW * 0.2f.lit))
            SET(finalB, finalB * (1f.lit - fW * 0.3f.lit))

            // --- DROPLETS: clustered random ---
            val clN = createTemp(Float1)
            SET(clN, sin(uvX * 11f.lit + uvY * 7f.lit + time * 0.2f.lit) * sin(uvX * 5f.lit - uvY * 13f.lit + 2.1f.lit))
            SET(clN, clamp(clN + 0.15f.lit, 0f.lit, 1f.lit))
            val dSd = createTemp(Float1)
            SET(dSd, fract(sin(uvX * 347f.lit + uvY * 193f.lit + floor(time * 3f.lit) * 7.3f.lit) * 43758.5f.lit))
            val dAct = createTemp(Float1)
            SET(dAct, step(1f.lit - weather * 0.012f.lit * clN, dSd) * (mG + mF + mR + mW) * weather)
            val dBr = createTemp(Float1); SET(dBr, fract(dSd * 7.77f.lit) * 0.2f.lit + 0.08f.lit)
            SET(finalR, finalR + dAct * dBr * 0.4f.lit)
            SET(finalG, finalG + dAct * dBr * 0.5f.lit)
            SET(finalB, finalB + dAct * dBr * 0.8f.lit)

            // --- FOG: GROUND LEVEL ONLY (bottom 40% of image) ---
            val fogZone = createTemp(Float1)
            // Only below 60% of image height (uvY > 0.6), fade in from 0.5
            SET(fogZone, clamp((uvY - 0.5f.lit) * 2.5f.lit, 0f.lit, 1f.lit))
            val fogD = createTemp(Float1)
            SET(fogD, weather * weather * fogZone * 0.35f.lit)
            SET(finalR, mix(finalR, 0.52f.lit, fogD))
            SET(finalG, mix(finalG, 0.55f.lit, fogD))
            SET(finalB, mix(finalB, 0.62f.lit, fogD))

            // --- RAIN: global ---
            val rainD = createTemp(Float1); SET(rainD, weather * weather)
            val rn1 = createTemp(Float1)
            SET(rn1, step(0.993f.lit, fract(sin(uvX * 280f.lit + uvY * 550f.lit + time * 18f.lit + wind * uvX * 140f.lit) * 43758.5f.lit)))
            val rn2 = createTemp(Float1)
            SET(rn2, step(0.994f.lit, fract(sin(uvX * 180f.lit + uvY * 400f.lit + time * 14f.lit + wind * uvX * 100f.lit + 5.7f.lit) * 43758.5f.lit)))
            val rnT = createTemp(Float1); SET(rnT, clamp(rn1 + rn2, 0f.lit, 1f.lit) * rainD * 0.3f.lit)
            SET(finalR, finalR + rnT * 0.6f.lit)
            SET(finalG, finalG + rnT * 0.7f.lit)
            SET(finalB, finalB + rnT * 0.9f.lit)

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
        if (matTex != null) { setTex(ctx, u_MaterialMap, matTex) }
        ctx[MatWeatherUB].push {
            it[u_Time] = time
            it[u_Weather] = weatherState
            it[u_Wind] = windAngle
            it[u_HasMat] = if (matTex != null) 1f else 0f
        }
    }
}
