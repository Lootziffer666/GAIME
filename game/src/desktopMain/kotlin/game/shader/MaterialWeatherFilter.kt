package game.shader

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.image.bitmap.Bitmap32
import korlibs.korge.render.*
import korlibs.korge.view.filter.ShaderFilter

/**
 * Material-aware weather v6 — Color-palette-calibrated from reference art.
 *
 * The wet/storm colors are NOT guessed — they come directly from analyzed
 * color palettes of the user's reference concept art:
 *
 * DRY → STORM color targets (hex):
 *   Roof:     #C47352 → #542E1E  (deep kastanienbraun)
 *   Stone:    #E3C195 → #4A4540  (cold dark basalt)
 *   Grass:    #6E9E47 → #2D4524  (moosig dunkelgrün)
 *   Wood:     #8C4730 → #2E1910  (fast schwarz)
 *   Foliage:  #3F6E31 → #1A2E16  (tannengrün)
 *   Windows:  dark    → #D99441  (warm bernstein glow)
 *   Puddles:  n/a     → #4A4540 base + #8C8782 highlights
 *
 * The shader MIXes between original pixel color and the storm-target
 * multiplied by the pixel's relative luminance (preserving texture detail).
 *
 * Material map RGB:
 *   R = Material ID
 *   G = Flow angle (per roof face, from ridge detection)
 *   B = Drip zone (0-180) / Window marker (200+)
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
            val flowAngle = createTemp(Float1)
            SET(flowAngle, matSample.y * 6.2832f.lit)
            val bCh = createTemp(Float1)
            SET(bCh, matSample.z)
            val dripInt = createTemp(Float1)
            SET(dripInt, clamp(bCh / 0.71f.lit, 0f.lit, 1f.lit) * step(bCh, 0.75f.lit))
            val isWindow = createTemp(Float1)
            SET(isWindow, step(0.8f.lit, bCh) * step(bCh, 0.92f.lit) * hasMat)

            // Material masks
            val mG = createTemp(Float1); SET(mG, step(matId, 0.5f.lit) * hasMat)
            val mS = createTemp(Float1); SET(mS, step(0.5f.lit, matId) * step(matId, 1.5f.lit) * hasMat)
            val mW = createTemp(Float1); SET(mW, step(1.5f.lit, matId) * step(matId, 2.5f.lit) * hasMat)
            val mR = createTemp(Float1); SET(mR, step(2.5f.lit, matId) * step(matId, 3.5f.lit) * hasMat)
            val mF = createTemp(Float1); SET(mF, step(3.5f.lit, matId) * step(matId, 4.5f.lit) * hasMat)
            val mWa = createTemp(Float1); SET(mWa, step(4.5f.lit, matId) * step(matId, 5.5f.lit) * hasMat)

            // No-map fallback (minimal, just overall darkening)
            val noMat = createTemp(Float1); SET(noMat, 1f.lit - hasMat)

            // === PALETTE-BASED COLOR TRANSFORMATION ===
            // Per pixel luminance (preserves texture detail when mixing to target)
            val pixLum = createTemp(Float1)
            SET(pixLum, r * 0.299f.lit + g * 0.587f.lit + b * 0.114f.lit)
            // Relative brightness (how bright is this pixel vs its material's average)
            // Used to preserve texture: dark crevices stay dark, highlights stay lighter
            val relBright = createTemp(Float1)
            SET(relBright, clamp(pixLum * 2f.lit, 0f.lit, 1.5f.lit))

            val finalR = createTemp(Float1)
            val finalG = createTemp(Float1)
            val finalB = createTemp(Float1)
            SET(finalR, r); SET(finalG, g); SET(finalB, b)

            // --- SHADOW SOFTENING (overcast = diffuse light = less harsh shadows) ---
            val shadowLift = createTemp(Float1)
            SET(shadowLift, (1f.lit - pixLum) * weather * 0.12f.lit)
            SET(finalR, finalR + shadowLift)
            SET(finalG, finalG + shadowLift)
            SET(finalB, finalB + shadowLift)

            // =================================================================
            // ROOF: #C47352 → #542E1E (multiply by ~0.43/0.40/0.37)
            // =================================================================
            val roofWet = createTemp(Float1); SET(roofWet, mR * weather)
            // Target storm color scaled by relative pixel brightness
            val roofTgtR = createTemp(Float1); SET(roofTgtR, 0.33f.lit * relBright)
            val roofTgtG = createTemp(Float1); SET(roofTgtG, 0.18f.lit * relBright)
            val roofTgtB = createTemp(Float1); SET(roofTgtB, 0.12f.lit * relBright)
            SET(finalR, mix(finalR, roofTgtR, roofWet))
            SET(finalG, mix(finalG, roofTgtG, roofWet))
            SET(finalB, mix(finalB, roofTgtB, roofWet))

            // Cloud reflection on wet roof (subtle bright moving patches)
            val rCloud = createTemp(Float1)
            SET(rCloud, sin((uvX + time * 0.02f.lit) * 4f.lit) * sin((uvY + time * 0.015f.lit) * 3f.lit))
            SET(rCloud, clamp(rCloud * 0.4f.lit + 0.1f.lit, 0f.lit, 0.35f.lit) * roofWet * 0.08f.lit)
            SET(finalR, finalR + rCloud * 0.7f.lit)
            SET(finalG, finalG + rCloud * 0.8f.lit)
            SET(finalB, finalB + rCloud * 1.0f.lit)

            // Flow-directed rivulets
            val fDirX = createTemp(Float1); SET(fDirX, cos(flowAngle))
            val fDirY = createTemp(Float1); SET(fDirY, sin(flowAngle))
            val fProj = createTemp(Float1); SET(fProj, uvX * fDirX + uvY * fDirY)
            val fPerp = createTemp(Float1); SET(fPerp, uvX * (-fDirY) + uvY * fDirX)
            val rivX = createTemp(Float1)
            SET(rivX, fract(fPerp * 35f.lit + sin(fProj * 8f.lit) * 0.08f.lit))
            val isRiv = createTemp(Float1); SET(isRiv, step(0.44f.lit, rivX) * step(rivX, 0.56f.lit))
            val rivF = createTemp(Float1); SET(rivF, fract(fProj * 4f.lit + time * 3.5f.lit))
            val rivV = createTemp(Float1); SET(rivV, step(0.2f.lit, rivF) * step(rivF, 0.7f.lit))
            val riv = createTemp(Float1); SET(riv, isRiv * rivV * roofWet * 0.12f.lit)
            SET(finalR, finalR + riv * 0.3f.lit)
            SET(finalG, finalG + riv * 0.4f.lit)
            SET(finalB, finalB + riv * 0.6f.lit)

            // =================================================================
            // STONE: #E3C195 → #4A4540 (cold shift: R*0.33, G*0.36, B*0.43)
            // Joints first, then puddles
            // =================================================================
            val stoneWet = createTemp(Float1); SET(stoneWet, mS * weather)
            // Joints: darker pixels fill with water first
            val jointTh = createTemp(Float1); SET(jointTh, 0.35f.lit + weather * 0.55f.lit)
            val pixBr = createTemp(Float1); SET(pixBr, (r + g + b) / 3f.lit)
            val jointW = createTemp(Float1); SET(jointW, stoneWet * step(pixBr, jointTh))
            // Target: #4A4540 = (0.29, 0.27, 0.25) — COLD basalt
            val stoneTgtR = createTemp(Float1); SET(stoneTgtR, 0.29f.lit * relBright)
            val stoneTgtG = createTemp(Float1); SET(stoneTgtG, 0.27f.lit * relBright)
            val stoneTgtB = createTemp(Float1); SET(stoneTgtB, 0.25f.lit * relBright)
            // Apply wet stone color (joints stronger, faces lighter)
            SET(finalR, mix(finalR, stoneTgtR, jointW))
            SET(finalG, mix(finalG, stoneTgtG, jointW))
            SET(finalB, mix(finalB, stoneTgtB, jointW))
            // Remaining stone: lighter wet
            val stoneLight = createTemp(Float1); SET(stoneLight, stoneWet * (1f.lit - jointW) * 0.5f.lit)
            SET(finalR, mix(finalR, stoneTgtR * 1.3f.lit, stoneLight))
            SET(finalG, mix(finalG, stoneTgtG * 1.3f.lit, stoneLight))
            SET(finalB, mix(finalB, stoneTgtB * 1.4f.lit, stoneLight))

            // Puddles
            val pn1 = createTemp(Float1)
            SET(pn1, sin(uvX * 19f.lit + uvY * 13f.lit + 1.7f.lit) * sin(uvX * 29f.lit - uvY * 7f.lit))
            val pn2 = createTemp(Float1)
            SET(pn2, sin(uvX * 5f.lit + uvY * 37f.lit + 3.1f.lit) * 0.7f.lit)
            val pField = createTemp(Float1)
            SET(pField, clamp((pn1 + pn2) * 0.4f.lit + 0.4f.lit, 0f.lit, 1f.lit))
            // Drip zone lowers threshold
            val pTh = createTemp(Float1)
            SET(pTh, mix(weather * 0.55f.lit, weather * 1.3f.lit, dripInt * hasMat))
            val pudStone = createTemp(Float1); SET(pudStone, mS * step(pField, pTh))

            // Puddle bleed onto grass
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
            val pBleed = createTemp(Float1); SET(pBleed, mG * nearS * hasMat * step(pField, pTh * 0.8f.lit))
            val dripP = createTemp(Float1); SET(dripP, mG * dripInt * hasMat * step(pField, weather * 0.8f.lit))
            val hasPud = createTemp(Float1); SET(hasPud, clamp(pudStone + pBleed + dripP, 0f.lit, 1f.lit))

            // Puddle color: #4A4540 base + #8C8782 highlights (silbergrau)
            val ripple = createTemp(Float1)
            SET(ripple, sin(uvX * 90f.lit + time * 2.5f.lit) * sin(uvY * 70f.lit + time * 1.8f.lit))
            SET(ripple, clamp(ripple * ripple, 0f.lit, 1f.lit) * 0.08f.lit)
            val cloudR = createTemp(Float1)
            SET(cloudR, sin((uvX + time * 0.02f.lit) * 3f.lit) * sin((uvY + time * 0.015f.lit) * 2f.lit))
            SET(cloudR, clamp(cloudR * 0.5f.lit + 0.2f.lit, 0f.lit, 0.4f.lit) * 0.1f.lit)
            val pudW = createTemp(Float1); SET(pudW, hasPud * weather)
            // Puddle base: 0.29, 0.27, 0.25 + ripples + cloud reflection (→ 0.55, 0.53, 0.51)
            SET(finalR, mix(finalR, 0.29f.lit + ripple * 0.3f.lit + cloudR * 0.55f.lit, pudW))
            SET(finalG, mix(finalG, 0.27f.lit + ripple * 0.3f.lit + cloudR * 0.53f.lit, pudW))
            SET(finalB, mix(finalB, 0.25f.lit + ripple * 0.4f.lit + cloudR * 0.51f.lit, pudW))

            // =================================================================
            // GRASS: #6E9E47 → #2D4524 (multiply R*0.41, G*0.44, B*0.51)
            // =================================================================
            val grassWet = createTemp(Float1); SET(grassWet, mG * weather * (1f.lit - hasPud))
            val grassTgtR = createTemp(Float1); SET(grassTgtR, 0.18f.lit * relBright)
            val grassTgtG = createTemp(Float1); SET(grassTgtG, 0.27f.lit * relBright)
            val grassTgtB = createTemp(Float1); SET(grassTgtB, 0.14f.lit * relBright)
            SET(finalR, mix(finalR, grassTgtR, grassWet))
            SET(finalG, mix(finalG, grassTgtG, grassWet))
            SET(finalB, mix(finalB, grassTgtB, grassWet))

            // =================================================================
            // WOOD: #8C4730 → #2E1910 (multiply R*0.33, G*0.35, B*0.33)
            // =================================================================
            val woodWet = createTemp(Float1); SET(woodWet, mW * weather * (1f.lit - isWindow))
            val woodTgtR = createTemp(Float1); SET(woodTgtR, 0.18f.lit * relBright)
            val woodTgtG = createTemp(Float1); SET(woodTgtG, 0.10f.lit * relBright)
            val woodTgtB = createTemp(Float1); SET(woodTgtB, 0.06f.lit * relBright)
            SET(finalR, mix(finalR, woodTgtR, woodWet))
            SET(finalG, mix(finalG, woodTgtG, woodWet))
            SET(finalB, mix(finalB, woodTgtB, woodWet))
            // Wet sheen on wood
            val wSh = createTemp(Float1)
            SET(wSh, sin(uvX * 50f.lit + uvY * 25f.lit + time * 0.3f.lit))
            SET(wSh, clamp(wSh * 0.3f.lit + 0.15f.lit, 0f.lit, 0.3f.lit) * woodWet * 0.06f.lit)
            SET(finalR, finalR + wSh)
            SET(finalG, finalG + wSh)
            SET(finalB, finalB + wSh * 1.2f.lit)

            // =================================================================
            // FOLIAGE: #3F6E31 → #1A2E16
            // =================================================================
            val folWet = createTemp(Float1); SET(folWet, mF * weather)
            val folTgtR = createTemp(Float1); SET(folTgtR, 0.10f.lit * relBright)
            val folTgtG = createTemp(Float1); SET(folTgtG, 0.18f.lit * relBright)
            val folTgtB = createTemp(Float1); SET(folTgtB, 0.09f.lit * relBright)
            SET(finalR, mix(finalR, folTgtR, folWet))
            SET(finalG, mix(finalG, folTgtG, folWet))
            SET(finalB, mix(finalB, folTgtB, folWet))

            // =================================================================
            // WINDOWS: warm bernstein glow (#D99441)
            // =================================================================
            val winGlow = createTemp(Float1); SET(winGlow, isWindow * weather)
            SET(finalR, mix(finalR, 0.85f.lit, winGlow * 0.75f.lit))
            SET(finalG, mix(finalG, 0.58f.lit, winGlow * 0.65f.lit))
            SET(finalB, mix(finalB, 0.25f.lit, winGlow * 0.55f.lit))

            // --- DROPLETS (clustered, random) ---
            val clN = createTemp(Float1)
            SET(clN, sin(uvX * 11f.lit + uvY * 7f.lit + time * 0.2f.lit) * sin(uvX * 5f.lit - uvY * 13f.lit + 2.1f.lit))
            SET(clN, clamp(clN + 0.15f.lit, 0f.lit, 1f.lit))
            val dSd = createTemp(Float1)
            SET(dSd, fract(sin(uvX * 347f.lit + uvY * 193f.lit + floor(time * 3f.lit) * 7.3f.lit) * 43758.5f.lit))
            val dAct = createTemp(Float1)
            SET(dAct, step(1f.lit - weather * 0.012f.lit * clN, dSd) * (mG + mF + mR + mW) * weather)
            val dBr = createTemp(Float1); SET(dBr, fract(dSd * 7.77f.lit) * 0.15f.lit + 0.05f.lit)
            SET(finalR, finalR + dAct * dBr * 0.4f.lit)
            SET(finalG, finalG + dAct * dBr * 0.5f.lit)
            SET(finalB, finalB + dAct * dBr * 0.7f.lit)

            // --- FOG: ground level only (bottom 40%) ---
            val fogZone = createTemp(Float1)
            SET(fogZone, clamp((uvY - 0.55f.lit) * 3f.lit, 0f.lit, 1f.lit))
            val fogD = createTemp(Float1)
            SET(fogD, weather * weather * fogZone * 0.3f.lit)
            SET(finalR, mix(finalR, 0.45f.lit, fogD))
            SET(finalG, mix(finalG, 0.48f.lit, fogD))
            SET(finalB, mix(finalB, 0.55f.lit, fogD))

            // --- RAIN: global diagonal streaks ---
            val rainD = createTemp(Float1); SET(rainD, weather * weather)
            val rn1 = createTemp(Float1)
            SET(rn1, step(0.993f.lit, fract(sin(uvX * 280f.lit + uvY * 550f.lit + time * 18f.lit + wind * uvX * 140f.lit) * 43758.5f.lit)))
            val rn2 = createTemp(Float1)
            SET(rn2, step(0.994f.lit, fract(sin(uvX * 180f.lit + uvY * 400f.lit + time * 14f.lit + wind * uvX * 100f.lit + 5.7f.lit) * 43758.5f.lit)))
            val rnT = createTemp(Float1); SET(rnT, clamp(rn1 + rn2, 0f.lit, 1f.lit) * rainD * 0.25f.lit)
            SET(finalR, finalR + rnT * 0.5f.lit)
            SET(finalG, finalG + rnT * 0.6f.lit)
            SET(finalB, finalB + rnT * 0.8f.lit)

            // --- No-map fallback: simple overall darkening ---
            SET(finalR, mix(finalR, finalR * (1f.lit - 0.4f.lit), noMat * weather))
            SET(finalG, mix(finalG, finalG * (1f.lit - 0.4f.lit), noMat * weather))
            SET(finalB, mix(finalB, finalB * (1f.lit - 0.35f.lit), noMat * weather))

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
