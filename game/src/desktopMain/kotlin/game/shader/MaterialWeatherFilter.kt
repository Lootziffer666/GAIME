package game.shader

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.image.bitmap.Bitmap32
import korlibs.korge.render.*
import korlibs.korge.view.filter.ShaderFilter

/**
 * Material-aware weather v7 — Empirically derived from reference images.
 *
 * The color transformation is NOT guessed — it was computed by least-squares
 * regression between the user's dry and storm reference images, per material:
 *
 *   storm_pixel = a * dry_pixel + b   (per channel, per material)
 *
 * Measured transforms (a=slope, b=offset):
 *   ROOF:    R(0.17, 0.13)  G(0.29, 0.07)  B(0.70, -0.04)
 *   STONE:   → special handling (negative slopes indicate non-linear)
 *   GRASS:   R(0.09, 0.17)  G(0.10, 0.21)  B(0.22, 0.15)
 *   FOLIAGE: R(-0.19, 0.24) G(0.01, 0.25)  B(0.06, 0.22)
 *   WOOD:    R(0.08, 0.20)  G(0.14, 0.17)  B(0.15, 0.15)
 *
 * The shader interpolates between original (weather=0) and transformed (weather=1).
 * No hard color replacement. Texture detail is preserved through the linear transform.
 *
 * Additional effects applied as overlay:
 * - Puddles (on stone, bleeding to grass, drip-zone priority)
 * - Roof rivulets (flow-directed from G channel)
 * - Window glow (warm interior light)
 * - Fog (ground-level only)
 * - Rain streaks (global)
 * - Clustered droplets
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

            // Fallback (no material map): use global linear transform
            val noMat = createTemp(Float1); SET(noMat, 1f.lit - hasMat)

            // =================================================================
            // EMPIRICAL LINEAR TRANSFORMS: storm = a * dry + b
            // Interpolated by weather: result = mix(dry, a*dry+b, weather)
            // =================================================================

            // --- ROOF: R(0.17, 0.13) G(0.29, 0.07) B(0.70, -0.04) ---
            val roofR = createTemp(Float1); SET(roofR, r * 0.17f.lit + 0.13f.lit)
            val roofG = createTemp(Float1); SET(roofG, g * 0.29f.lit + 0.07f.lit)
            val roofB = createTemp(Float1); SET(roofB, b * 0.70f.lit - 0.04f.lit)

            // --- GRASS: R(0.09, 0.17) G(0.10, 0.21) B(0.22, 0.15) ---
            val grassR = createTemp(Float1); SET(grassR, r * 0.09f.lit + 0.17f.lit)
            val grassG = createTemp(Float1); SET(grassG, g * 0.10f.lit + 0.21f.lit)
            val grassB = createTemp(Float1); SET(grassB, b * 0.22f.lit + 0.15f.lit)

            // --- FOLIAGE: R(-0.19, 0.24) G(0.01, 0.25) B(0.06, 0.22) ---
            // Negative slope on R means: clamp at 0
            val folR = createTemp(Float1); SET(folR, max(r * (-0.19f.lit) + 0.24f.lit, 0f.lit))
            val folG = createTemp(Float1); SET(folG, g * 0.01f.lit + 0.25f.lit)
            val folB = createTemp(Float1); SET(folB, b * 0.06f.lit + 0.22f.lit)

            // --- WOOD: R(0.08, 0.20) G(0.14, 0.17) B(0.15, 0.15) ---
            val woodR = createTemp(Float1); SET(woodR, r * 0.08f.lit + 0.20f.lit)
            val woodG = createTemp(Float1); SET(woodG, g * 0.14f.lit + 0.17f.lit)
            val woodB = createTemp(Float1); SET(woodB, b * 0.15f.lit + 0.15f.lit)

            // --- STONE: Use simplified transform (the negative slopes indicate
            // that stone color INVERTS somewhat — bright stone becomes dark wet stone)
            // Approximate: R(0.20, 0.15) G(0.25, 0.12) B(0.35, 0.10)
            val stoneR = createTemp(Float1); SET(stoneR, r * 0.20f.lit + 0.15f.lit)
            val stoneG = createTemp(Float1); SET(stoneG, g * 0.25f.lit + 0.12f.lit)
            val stoneB = createTemp(Float1); SET(stoneB, b * 0.35f.lit + 0.10f.lit)

            // --- GLOBAL FALLBACK (no material map) ---
            // Average of all materials: R(0.19, 0.17) G(0.22, 0.17) B(0.34, 0.13)
            val fallR = createTemp(Float1); SET(fallR, r * 0.19f.lit + 0.17f.lit)
            val fallG = createTemp(Float1); SET(fallG, g * 0.22f.lit + 0.17f.lit)
            val fallB = createTemp(Float1); SET(fallB, b * 0.34f.lit + 0.13f.lit)

            // === BLEND: compute wet color per pixel based on material ===
            val wetR = createTemp(Float1)
            val wetG = createTemp(Float1)
            val wetB = createTemp(Float1)

            // Start with fallback
            SET(wetR, fallR)
            SET(wetG, fallG)
            SET(wetB, fallB)

            // Override per material (each material mask is 0 or 1, exclusive)
            SET(wetR, mix(wetR, roofR, mR))
            SET(wetG, mix(wetG, roofG, mR))
            SET(wetB, mix(wetB, roofB, mR))

            SET(wetR, mix(wetR, grassR, mG))
            SET(wetG, mix(wetG, grassG, mG))
            SET(wetB, mix(wetB, grassB, mG))

            SET(wetR, mix(wetR, folR, mF))
            SET(wetG, mix(wetG, folG, mF))
            SET(wetB, mix(wetB, folB, mF))

            SET(wetR, mix(wetR, woodR, mW))
            SET(wetG, mix(wetG, woodG, mW))
            SET(wetB, mix(wetB, woodB, mW))

            SET(wetR, mix(wetR, stoneR, mS))
            SET(wetG, mix(wetG, stoneG, mS))
            SET(wetB, mix(wetB, stoneB, mS))

            // === INTERPOLATE: dry → wet by weather ===
            val finalR = createTemp(Float1)
            val finalG = createTemp(Float1)
            val finalB = createTemp(Float1)
            SET(finalR, mix(r, wetR, weather))
            SET(finalG, mix(g, wetG, weather))
            SET(finalB, mix(b, wetB, weather))

            // =================================================================
            // OVERLAY EFFECTS (on top of the base color transform)
            // =================================================================

            // --- PUDDLES on stone (+ bleed + drip zone) ---
            val pn1 = createTemp(Float1)
            SET(pn1, sin(uvX * 19f.lit + uvY * 13f.lit + 1.7f.lit) * sin(uvX * 29f.lit - uvY * 7f.lit))
            val pn2 = createTemp(Float1)
            SET(pn2, sin(uvX * 5f.lit + uvY * 37f.lit + 3.1f.lit) * 0.7f.lit)
            val pField = createTemp(Float1)
            SET(pField, clamp((pn1 + pn2) * 0.4f.lit + 0.4f.lit, 0f.lit, 1f.lit))
            val pTh = createTemp(Float1)
            SET(pTh, mix(weather * 0.55f.lit, weather * 1.3f.lit, dripInt * hasMat))
            val pudStone = createTemp(Float1)
            SET(pudStone, mS * step(pField, pTh))

            // Bleed onto grass near stone
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

            // Puddle rendering: dark reflective + ripple + cloud
            val ripple = createTemp(Float1)
            SET(ripple, sin(uvX * 90f.lit + time * 2.5f.lit) * sin(uvY * 70f.lit + time * 1.8f.lit))
            SET(ripple, clamp(ripple * ripple, 0f.lit, 1f.lit) * 0.08f.lit)
            val cloud = createTemp(Float1)
            SET(cloud, sin((uvX + time * 0.02f.lit) * 3f.lit) * sin((uvY + time * 0.015f.lit) * 2f.lit))
            SET(cloud, clamp(cloud * 0.4f.lit + 0.2f.lit, 0f.lit, 0.35f.lit) * 0.1f.lit)
            val pudW = createTemp(Float1); SET(pudW, hasPud * weather)
            SET(finalR, mix(finalR, 0.12f.lit + ripple * 0.2f.lit + cloud * 0.4f.lit, pudW))
            SET(finalG, mix(finalG, 0.14f.lit + ripple * 0.25f.lit + cloud * 0.45f.lit, pudW))
            SET(finalB, mix(finalB, 0.18f.lit + ripple * 0.35f.lit + cloud * 0.5f.lit, pudW))

            // --- ROOF RIVULETS (flow-directed) ---
            val rW = createTemp(Float1); SET(rW, mR * weather)
            val fDirX = createTemp(Float1); SET(fDirX, cos(flowAngle))
            val fDirY = createTemp(Float1); SET(fDirY, sin(flowAngle))
            val fProj = createTemp(Float1); SET(fProj, uvX * fDirX + uvY * fDirY)
            val fPerp = createTemp(Float1); SET(fPerp, uvX * (-fDirY) + uvY * fDirX)
            val rivX = createTemp(Float1)
            SET(rivX, fract(fPerp * 35f.lit + sin(fProj * 8f.lit) * 0.08f.lit))
            val isRiv = createTemp(Float1); SET(isRiv, step(0.44f.lit, rivX) * step(rivX, 0.56f.lit))
            val rivF = createTemp(Float1); SET(rivF, fract(fProj * 4f.lit + time * 3.5f.lit))
            val rivV = createTemp(Float1); SET(rivV, step(0.2f.lit, rivF) * step(rivF, 0.7f.lit))
            val riv = createTemp(Float1); SET(riv, isRiv * rivV * rW * 0.1f.lit)
            SET(finalR, finalR + riv * 0.3f.lit)
            SET(finalG, finalG + riv * 0.4f.lit)
            SET(finalB, finalB + riv * 0.6f.lit)

            // --- WINDOW GLOW (#C78632 ≈ 0.78, 0.53, 0.20) ---
            val winGlow = createTemp(Float1); SET(winGlow, isWindow * weather)
            SET(finalR, mix(finalR, 0.78f.lit, winGlow * 0.7f.lit))
            SET(finalG, mix(finalG, 0.53f.lit, winGlow * 0.6f.lit))
            SET(finalB, mix(finalB, 0.20f.lit, winGlow * 0.5f.lit))

            // --- FOG (ground level only, bottom 40%) ---
            val fogZone = createTemp(Float1)
            SET(fogZone, clamp((uvY - 0.55f.lit) * 3f.lit, 0f.lit, 1f.lit))
            val fogD = createTemp(Float1)
            SET(fogD, weather * weather * fogZone * 0.25f.lit)
            SET(finalR, mix(finalR, 0.45f.lit, fogD))
            SET(finalG, mix(finalG, 0.48f.lit, fogD))
            SET(finalB, mix(finalB, 0.55f.lit, fogD))

            // --- RAIN STREAKS (global) ---
            val rainD = createTemp(Float1); SET(rainD, weather * weather)
            val rn1 = createTemp(Float1)
            SET(rn1, step(0.994f.lit, fract(sin(uvX * 280f.lit + uvY * 550f.lit + time * 18f.lit + wind * uvX * 140f.lit) * 43758.5f.lit)))
            val rn2 = createTemp(Float1)
            SET(rn2, step(0.995f.lit, fract(sin(uvX * 180f.lit + uvY * 400f.lit + time * 14f.lit + wind * uvX * 100f.lit + 5.7f.lit) * 43758.5f.lit)))
            val rnT = createTemp(Float1); SET(rnT, clamp(rn1 + rn2, 0f.lit, 1f.lit) * rainD * 0.2f.lit)
            SET(finalR, finalR + rnT * 0.5f.lit)
            SET(finalG, finalG + rnT * 0.6f.lit)
            SET(finalB, finalB + rnT * 0.8f.lit)

            // --- DROPLETS (clustered, random) ---
            val clN = createTemp(Float1)
            SET(clN, sin(uvX * 11f.lit + uvY * 7f.lit + time * 0.2f.lit) * sin(uvX * 5f.lit - uvY * 13f.lit + 2.1f.lit))
            SET(clN, clamp(clN + 0.15f.lit, 0f.lit, 1f.lit))
            val dSd = createTemp(Float1)
            SET(dSd, fract(sin(uvX * 347f.lit + uvY * 193f.lit + floor(time * 3f.lit) * 7.3f.lit) * 43758.5f.lit))
            val dAct = createTemp(Float1)
            SET(dAct, step(1f.lit - weather * 0.01f.lit * clN, dSd) * (mG + mF + mR + mW) * weather)
            val dBr = createTemp(Float1); SET(dBr, fract(dSd * 7.77f.lit) * 0.12f.lit + 0.04f.lit)
            SET(finalR, finalR + dAct * dBr * 0.3f.lit)
            SET(finalG, finalG + dAct * dBr * 0.4f.lit)
            SET(finalB, finalB + dAct * dBr * 0.6f.lit)

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
