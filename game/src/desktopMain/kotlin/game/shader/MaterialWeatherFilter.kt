package game.shader

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.image.bitmap.Bitmap32
import korlibs.korge.render.*
import korlibs.korge.view.filter.ShaderFilter

/**
 * Material-aware weather — synced with Shader Editor v2.
 *
 * Material detection: color-distance matching against palette colors (not R-channel ID).
 * This matches the hand-painted material maps directly by their RGB values:
 *   Roof:    RGB(0.976, 0.447, 0.161) — orange
 *   Path:    RGB(0.863, 0.149, 0.149) — red
 *   Wood:    RGB(0.522, 0.302, 0.055) — brown
 *   Window:  RGB(0.059, 0.463, 0.431) — teal
 *   Puddle:  RGB(0.024, 0.427, 0.831) — cyan/blue
 *   Rock:    RGB(0.278, 0.333, 0.412) — grey-blue
 *   Tree:    RGB(0.670, 0.055, 0.720) — purple
 *   (Grass = everything else)
 *
 * Color grading per material: target color × relative brightness, with
 * contrast/brightness/opacity controls. Preset "Sturm → Bild 3 (Dorf)".
 *
 * Post-processing: desaturation + exposure reduction + lift (from editor).
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
            val r = createTemp(Float1); SET(r, original.x)
            val g = createTemp(Float1); SET(g, original.y)
            val b = createTemp(Float1); SET(b, original.z)

            val uvX = createTemp(Float1); SET(uvX, coords.x / texSize.x)
            val uvY = createTemp(Float1); SET(uvY, coords.y / texSize.y)

            // === MATERIAL MAP — color distance matching ===
            val matSample = createTemp(Float4)
            SET(matSample, texture2D(u_MaterialMap, vec2(uvX, uvY)))
            val mmR = createTemp(Float1); SET(mmR, matSample.x)
            val mmG = createTemp(Float1); SET(mmG, matSample.y)
            val mmB = createTemp(Float1); SET(mmB, matSample.z)

            // Distance to each material palette color
            val dRoof = createTemp(Float1)
            SET(dRoof, sqrt((mmR - 0.976f.lit) * (mmR - 0.976f.lit) + (mmG - 0.447f.lit) * (mmG - 0.447f.lit) + (mmB - 0.161f.lit) * (mmB - 0.161f.lit)))
            val dPath = createTemp(Float1)
            SET(dPath, sqrt((mmR - 0.863f.lit) * (mmR - 0.863f.lit) + (mmG - 0.149f.lit) * (mmG - 0.149f.lit) + (mmB - 0.149f.lit) * (mmB - 0.149f.lit)))
            val dWood = createTemp(Float1)
            SET(dWood, sqrt((mmR - 0.522f.lit) * (mmR - 0.522f.lit) + (mmG - 0.302f.lit) * (mmG - 0.302f.lit) + (mmB - 0.055f.lit) * (mmB - 0.055f.lit)))
            val dWin = createTemp(Float1)
            SET(dWin, sqrt((mmR - 0.059f.lit) * (mmR - 0.059f.lit) + (mmG - 0.463f.lit) * (mmG - 0.463f.lit) + (mmB - 0.431f.lit) * (mmB - 0.431f.lit)))
            val dPud = createTemp(Float1)
            SET(dPud, sqrt((mmR - 0.024f.lit) * (mmR - 0.024f.lit) + (mmG - 0.427f.lit) * (mmG - 0.427f.lit) + (mmB - 0.831f.lit) * (mmB - 0.831f.lit)))
            val dRock = createTemp(Float1)
            SET(dRock, sqrt((mmR - 0.278f.lit) * (mmR - 0.278f.lit) + (mmG - 0.333f.lit) * (mmG - 0.333f.lit) + (mmB - 0.412f.lit) * (mmB - 0.412f.lit)))
            val dTree = createTemp(Float1)
            SET(dTree, sqrt((mmR - 0.670f.lit) * (mmR - 0.670f.lit) + (mmG - 0.055f.lit) * (mmG - 0.055f.lit) + (mmB - 0.720f.lit) * (mmB - 0.720f.lit)))

            // Find minimum distance
            val best = createTemp(Float1)
            SET(best, min(min(min(dRoof, dPath), min(dWood, dWin)), min(min(dPud, dRock), dTree)))

            // Material masks
            val mRo = createTemp(Float1); SET(mRo, step(dRoof, best + 0.001f.lit) * hasMat)
            val mSt = createTemp(Float1); SET(mSt, max(step(dPath, best + 0.001f.lit), step(dRock, best + 0.001f.lit)) * hasMat)
            val mWo = createTemp(Float1); SET(mWo, step(dWood, best + 0.001f.lit) * hasMat)
            val mFo = createTemp(Float1); SET(mFo, step(dTree, best + 0.001f.lit) * hasMat)
            val isW2 = createTemp(Float1); SET(isW2, step(dWin, best + 0.001f.lit) * hasMat)
            val mP0 = createTemp(Float1); SET(mP0, step(dPud, best + 0.001f.lit) * hasMat)
            val mGr = createTemp(Float1); SET(mGr, clamp(hasMat - mRo - mSt - mWo - mFo - isW2 - mP0, 0f.lit, 1f.lit))

            // Flow angle + drip from material map
            val fa = createTemp(Float1); SET(fa, (mmG - 0.5f.lit) * 6.2832f.lit)
            val drip = createTemp(Float1)
            SET(drip, max(mP0, max(mSt * 0.45f.lit, mGr * 0.35f.lit)))

            val noMat = createTemp(Float1); SET(noMat, 1f.lit - hasMat)

            // === COLOR GRADING (from editor preset "Sturm → Bild 3") ===
            // grade(color, target, contrast, brightness, opacity, mask*weather)
            val lum = createTemp(Float1)
            SET(lum, r * 0.299f.lit + g * 0.587f.lit + b * 0.114f.lit)
            val relBr = createTemp(Float1)
            SET(relBr, clamp(lum * 2.5f.lit, 0.3f.lit, 1.5f.lit))

            // Shadow lift (preset shad=24 → 0.24)
            val lift = createTemp(Float1)
            SET(lift, (1f.lit - lum) * weather * 0.24f.lit)

            val finalR = createTemp(Float1); SET(finalR, r + lift)
            val finalG = createTemp(Float1); SET(finalG, g + lift)
            val finalB = createTemp(Float1); SET(finalB, b + lift)

            // Preset values: "Sturm → Bild 3 (Dorf)"
            // Roof: RGB(138,82,45)/255, con=70, bri=48, opa=78
            // Stone: RGB(172,166,148)/255, con=82, bri=72, opa=86
            // Grass: RGB(62,112,45)/255, con=66, bri=56, opa=78
            // Wood: RGB(80,54,34)/255, con=72, bri=42, opa=82
            // Foliage: RGB(42,90,34)/255, con=72, bri=48, opa=82

            // Inline grade function per material:
            // wet = target * relBr; graded = mix(pixel, wet, opacity)
            // graded = mix(grey, graded, 1+contrast); graded += brightness
            // result = mix(pixel, graded, mask * weather)

            // --- ROOF ---
            val roofWetR = createTemp(Float1); SET(roofWetR, 0.541f.lit * relBr)
            val roofWetG = createTemp(Float1); SET(roofWetG, 0.322f.lit * relBr)
            val roofWetB = createTemp(Float1); SET(roofWetB, 0.176f.lit * relBr)
            val roofGR = createTemp(Float1); SET(roofGR, mix(finalR, roofWetR, 0.78f.lit))
            val roofGG = createTemp(Float1); SET(roofGG, mix(finalG, roofWetG, 0.78f.lit))
            val roofGB = createTemp(Float1); SET(roofGB, mix(finalB, roofWetB, 0.78f.lit))
            val roofLum = createTemp(Float1); SET(roofLum, roofGR * 0.299f.lit + roofGG * 0.587f.lit + roofGB * 0.114f.lit)
            SET(roofGR, mix(roofLum, roofGR, 1.4f.lit) - 0.01f.lit)
            SET(roofGG, mix(roofLum, roofGG, 1.4f.lit) - 0.01f.lit)
            SET(roofGB, mix(roofLum, roofGB, 1.4f.lit) - 0.01f.lit)
            SET(finalR, mix(finalR, roofGR, mRo * weather))
            SET(finalG, mix(finalG, roofGG, mRo * weather))
            SET(finalB, mix(finalB, roofGB, mRo * weather))

            // --- STONE ---
            val stoneWR = createTemp(Float1); SET(stoneWR, 0.675f.lit * relBr)
            val stoneWG = createTemp(Float1); SET(stoneWG, 0.651f.lit * relBr)
            val stoneWB = createTemp(Float1); SET(stoneWB, 0.580f.lit * relBr)
            val stoneGR = createTemp(Float1); SET(stoneGR, mix(finalR, stoneWR, 0.86f.lit))
            val stoneGG = createTemp(Float1); SET(stoneGG, mix(finalG, stoneWG, 0.86f.lit))
            val stoneGB = createTemp(Float1); SET(stoneGB, mix(finalB, stoneWB, 0.86f.lit))
            val stoneLum = createTemp(Float1); SET(stoneLum, stoneGR * 0.299f.lit + stoneGG * 0.587f.lit + stoneGB * 0.114f.lit)
            SET(stoneGR, mix(stoneLum, stoneGR, 1.64f.lit) + 0.11f.lit)
            SET(stoneGG, mix(stoneLum, stoneGG, 1.64f.lit) + 0.11f.lit)
            SET(stoneGB, mix(stoneLum, stoneGB, 1.64f.lit) + 0.11f.lit)
            SET(finalR, mix(finalR, stoneGR, mSt * weather))
            SET(finalG, mix(finalG, stoneGG, mSt * weather))
            SET(finalB, mix(finalB, stoneGB, mSt * weather))

            // --- GRASS ---
            val grassWR = createTemp(Float1); SET(grassWR, 0.243f.lit * relBr)
            val grassWG = createTemp(Float1); SET(grassWG, 0.439f.lit * relBr)
            val grassWB = createTemp(Float1); SET(grassWB, 0.176f.lit * relBr)
            val grassGR = createTemp(Float1); SET(grassGR, mix(finalR, grassWR, 0.78f.lit))
            val grassGG = createTemp(Float1); SET(grassGG, mix(finalG, grassWG, 0.78f.lit))
            val grassGB = createTemp(Float1); SET(grassGB, mix(finalB, grassWB, 0.78f.lit))
            val grassLum = createTemp(Float1); SET(grassLum, grassGR * 0.299f.lit + grassGG * 0.587f.lit + grassGB * 0.114f.lit)
            SET(grassGR, mix(grassLum, grassGR, 1.32f.lit) + 0.03f.lit)
            SET(grassGG, mix(grassLum, grassGG, 1.32f.lit) + 0.03f.lit)
            SET(grassGB, mix(grassLum, grassGB, 1.32f.lit) + 0.03f.lit)
            SET(finalR, mix(finalR, grassGR, mGr * weather))
            SET(finalG, mix(finalG, grassGG, mGr * weather))
            SET(finalB, mix(finalB, grassGB, mGr * weather))

            // --- WOOD ---
            val woodWR = createTemp(Float1); SET(woodWR, 0.314f.lit * relBr)
            val woodWG = createTemp(Float1); SET(woodWG, 0.212f.lit * relBr)
            val woodWB = createTemp(Float1); SET(woodWB, 0.133f.lit * relBr)
            val woodGR = createTemp(Float1); SET(woodGR, mix(finalR, woodWR, 0.82f.lit))
            val woodGG = createTemp(Float1); SET(woodGG, mix(finalG, woodWG, 0.82f.lit))
            val woodGB = createTemp(Float1); SET(woodGB, mix(finalB, woodWB, 0.82f.lit))
            val woodLum = createTemp(Float1); SET(woodLum, woodGR * 0.299f.lit + woodGG * 0.587f.lit + woodGB * 0.114f.lit)
            SET(woodGR, mix(woodLum, woodGR, 1.44f.lit) - 0.04f.lit)
            SET(woodGG, mix(woodLum, woodGG, 1.44f.lit) - 0.04f.lit)
            SET(woodGB, mix(woodLum, woodGB, 1.44f.lit) - 0.04f.lit)
            SET(finalR, mix(finalR, woodGR, mWo * weather))
            SET(finalG, mix(finalG, woodGG, mWo * weather))
            SET(finalB, mix(finalB, woodGB, mWo * weather))

            // --- FOLIAGE ---
            val folWR = createTemp(Float1); SET(folWR, 0.165f.lit * relBr)
            val folWG = createTemp(Float1); SET(folWG, 0.353f.lit * relBr)
            val folWB = createTemp(Float1); SET(folWB, 0.133f.lit * relBr)
            val folGR = createTemp(Float1); SET(folGR, mix(finalR, folWR, 0.82f.lit))
            val folGG = createTemp(Float1); SET(folGG, mix(finalG, folWG, 0.82f.lit))
            val folGB = createTemp(Float1); SET(folGB, mix(finalB, folWB, 0.82f.lit))
            val folLum = createTemp(Float1); SET(folLum, folGR * 0.299f.lit + folGG * 0.587f.lit + folGB * 0.114f.lit)
            SET(folGR, mix(folLum, folGR, 1.44f.lit) - 0.01f.lit)
            SET(folGG, mix(folLum, folGG, 1.44f.lit) - 0.01f.lit)
            SET(folGB, mix(folLum, folGB, 1.44f.lit) - 0.01f.lit)
            SET(finalR, mix(finalR, folGR, mFo * weather))
            SET(finalG, mix(finalG, folGG, mFo * weather))
            SET(finalB, mix(finalB, folGB, mFo * weather))

            // No-map fallback
            SET(finalR, mix(finalR, r * (1f.lit - weather * 0.4f.lit), noMat * weather))
            SET(finalG, mix(finalG, g * (1f.lit - weather * 0.4f.lit), noMat * weather))
            SET(finalB, mix(finalB, b * (1f.lit - weather * 0.35f.lit), noMat * weather))

            // === PUDDLES ===
            val pn1 = createTemp(Float1)
            SET(pn1, sin(uvX * 19f.lit + uvY * 13f.lit + 1.7f.lit) * sin(uvX * 29f.lit - uvY * 7f.lit))
            val pn2 = createTemp(Float1)
            SET(pn2, sin(uvX * 5f.lit + uvY * 37f.lit + 3.1f.lit) * 0.7f.lit)
            val pField = createTemp(Float1)
            SET(pField, clamp((pn1 + pn2) * 0.4f.lit + 0.4f.lit, 0f.lit, 1f.lit))
            val pTh = createTemp(Float1)
            SET(pTh, mix(weather * 0.84f.lit, weather * 1.3f.lit, clamp(drip, 0f.lit, 1f.lit)))
            val hp = createTemp(Float1)
            SET(hp, clamp(mSt * step(pField, pTh) + mGr * drip * step(pField, weather * 0.8f.lit), 0f.lit, 1f.lit))
            val ripple = createTemp(Float1)
            SET(ripple, sin(uvX * 90f.lit + time * 2.5f.lit) * sin(uvY * 70f.lit + time * 1.8f.lit))
            SET(ripple, clamp(ripple * ripple, 0f.lit, 1f.lit) * 0.1f.lit)
            // Puddle color from preset: RGB(92,126,138)/255
            val pudW = createTemp(Float1); SET(pudW, max(hp, mP0) * weather)
            SET(finalR, mix(finalR, 0.361f.lit + ripple, pudW))
            SET(finalG, mix(finalG, 0.494f.lit + ripple, pudW))
            SET(finalB, mix(finalB, 0.541f.lit + ripple, pudW))

            // === RIVULETS ===
            val fdx = createTemp(Float1); SET(fdx, cos(fa))
            val fdy = createTemp(Float1); SET(fdy, sin(fa))
            val fp = createTemp(Float1); SET(fp, uvX * fdx + uvY * fdy)
            val fpp = createTemp(Float1); SET(fpp, uvX * (-fdy) + uvY * fdx)
            val rivX = createTemp(Float1)
            SET(rivX, fract(fpp * 35f.lit + sin(fp * 8f.lit) * 0.08f.lit))
            val rv = createTemp(Float1)
            SET(rv, step(0.44f.lit, rivX) * step(rivX, 0.56f.lit))
            SET(rv, rv * step(0.2f.lit, fract(fp * 4f.lit + time * 3.5f.lit)) * step(fract(fp * 4f.lit + time * 3.5f.lit), 0.7f.lit))
            SET(rv, rv * mRo * weather * 0.46f.lit)
            SET(finalR, finalR + rv * 0.3f.lit)
            SET(finalG, finalG + rv * 0.4f.lit)
            SET(finalB, finalB + rv * 0.6f.lit)

            // === WINDOW GLOW (preset: RGB(235,132,78)/255) ===
            SET(finalR, mix(finalR, 0.922f.lit, isW2 * weather * 0.7f.lit))
            SET(finalG, mix(finalG, 0.518f.lit, isW2 * weather * 0.7f.lit))
            SET(finalB, mix(finalB, 0.306f.lit, isW2 * weather * 0.7f.lit))

            // === FOG (ground level, preset fog=24 → 0.24) ===
            val fogZ = createTemp(Float1); SET(fogZ, clamp((uvY - 0.55f.lit) * 3f.lit, 0f.lit, 1f.lit))
            val fogD = createTemp(Float1); SET(fogD, weather * weather * fogZ * 0.24f.lit)
            // Fog color: RGB(150,158,142)/255
            SET(finalR, mix(finalR, 0.588f.lit, fogD))
            SET(finalG, mix(finalG, 0.620f.lit, fogD))
            SET(finalB, mix(finalB, 0.557f.lit, fogD))

            // === RAIN (preset rain=8 → subtle) ===
            val rainD = createTemp(Float1); SET(rainD, weather * weather * 0.08f.lit)
            val rn1 = createTemp(Float1)
            SET(rn1, step(0.993f.lit, fract(sin(uvX * 280f.lit + uvY * 550f.lit + time * 18f.lit + wind * uvX * 140f.lit) * 43758.5f.lit)))
            val rn2 = createTemp(Float1)
            SET(rn2, step(0.994f.lit, fract(sin(uvX * 180f.lit + uvY * 400f.lit + time * 14f.lit + wind * uvX * 100f.lit + 5.7f.lit) * 43758.5f.lit)))
            val rnT = createTemp(Float1); SET(rnT, clamp(rn1 + rn2, 0f.lit, 1f.lit) * rainD)
            SET(finalR, finalR + rnT * 0.5f.lit)
            SET(finalG, finalG + rnT * 0.6f.lit)
            SET(finalB, finalB + rnT * 0.8f.lit)

            // === POST: desaturation + exposure (from editor) ===
            val postLum = createTemp(Float1)
            SET(postLum, finalR * 0.299f.lit + finalG * 0.587f.lit + finalB * 0.114f.lit)
            // Desaturation: mix toward grey by weather*0.08
            SET(finalR, mix(finalR, postLum, weather * 0.08f.lit))
            SET(finalG, mix(finalG, postLum, weather * 0.08f.lit))
            SET(finalB, mix(finalB, postLum, weather * 0.08f.lit))
            // Exposure reduction: multiply by mix(1.0, 0.88, weather)
            val expo = createTemp(Float1); SET(expo, mix(1f.lit, 0.88f.lit, weather))
            SET(finalR, finalR * expo)
            SET(finalG, finalG * expo)
            SET(finalB, finalB * expo)
            // Lift: add warm offset
            SET(finalR, finalR + 0.055f.lit * weather)
            SET(finalG, finalG + 0.060f.lit * weather)
            SET(finalB, finalB + 0.050f.lit * weather)

            // === DECAY ===
            val mn = createTemp(Float1)
            SET(mn, fract(sin(uvX * 127.1f.lit + uvY * 311.7f.lit) * 43758.5f.lit) * 0.5f.lit + sin(uvX * 23f.lit + uvY * 17f.lit) * 0.3f.lit + 0.2f.lit)
            // Moss: preset age=10, moss=72 → 0.1 * 0.72
            val mossAmt = createTemp(Float1); SET(mossAmt, 0.072f.lit)
            SET(finalR, mix(finalR, 0.12f.lit, mRo * mossAmt * step(0.4f.lit, mn) * 0.7f.lit))
            SET(finalG, mix(finalG, 0.28f.lit, mRo * mossAmt * step(0.4f.lit, mn) * 0.7f.lit))
            SET(finalB, mix(finalB, 0.08f.lit, mRo * mossAmt * step(0.4f.lit, mn) * 0.7f.lit))
            // Grass invasion: preset inv=60 → 0.1 * 0.6
            val invN = createTemp(Float1)
            SET(invN, fract(sin(uvX * 97f.lit + uvY * 53f.lit) * 43758.5f.lit) * 0.5f.lit + sin(uvX * 11f.lit - uvY * 7f.lit) * 0.3f.lit + 0.2f.lit)
            SET(finalR, mix(finalR, 0.08f.lit, mSt * 0.06f.lit * step(0.35f.lit, invN) * 0.6f.lit))
            SET(finalG, mix(finalG, 0.22f.lit, mSt * 0.06f.lit * step(0.35f.lit, invN) * 0.6f.lit))
            SET(finalB, mix(finalB, 0.05f.lit, mSt * 0.06f.lit * step(0.35f.lit, invN) * 0.6f.lit))
            // Wood aging
            SET(finalR, mix(finalR, finalR * 0.5f.lit, mWo * 0.05f.lit))
            SET(finalG, mix(finalG, finalG * 0.6f.lit + 0.02f.lit, mWo * 0.05f.lit))
            SET(finalB, mix(finalB, finalB * 0.4f.lit, mWo * 0.05f.lit))

            SET(out, vec4(clamp(finalR, 0f.lit, 1f.lit), clamp(finalG, 0f.lit, 1f.lit), clamp(finalB, 0f.lit, 1f.lit), original.w))
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
