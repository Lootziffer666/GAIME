package game.shader

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.korge.render.*
import korlibs.korge.view.filter.ShaderFilter

/**
 * Edge-aware doodle upscale shader — EPX/Scale2x + outline + boil.
 *
 * Replaces the Step 11 bilinear-based approach which SOFTENED instead of sharpening.
 * This shader uses POINT-SAMPLING (nearest) as its base and applies the classic
 * EPX/Scale2x algorithm (Eric's Pixel Expansion / AdvMAME2x) to smooth diagonal
 * staircase edges without blur, then draws fine dark outlines + time-driven jitter.
 *
 * EPX/Scale2x algorithm source: published pixel-art scaling algorithm
 * (Wikipedia "Pixel-art scaling algorithms"). Reimplemented from the concept
 * description — NO foreign source code copied (donor policy).
 *
 * Pipeline per fragment:
 * 1. Point-sample center texel P + 4 cardinal neighbors A(up), B(right), C(left), D(down)
 * 2. Determine sub-quadrant of this fragment within the source texel (via fract)
 * 3. Apply EPX rules to pick the best color for this sub-pixel position
 * 4. Detect edges (alpha discontinuity or luminance jump) → darken for outline
 * 5. Apply time-driven boil (tiny offset on texel lookup) for living doodle feel
 *
 * Uniforms:
 * - u_Time: animation driver (boil)
 * - u_LineStrength: 0..1, outline darkness (0 = no lines, 1 = full black outlines)
 * - u_Jitter: 0..~1, boil amplitude (0 = static, higher = more wobble)
 */
class DoodleLineFilter(
    var time: Float = 0f,
    var lineStrength: Float = 0.8f,
    var jitter: Float = 0.4f,
) : ShaderFilter() {

    object DoodleUB : UniformBlock(fixedLocation = 13) {
        val u_Time by float()
        val u_LineStrength by float()
        val u_Jitter by float()
    }

    companion object : BaseProgramProvider() {
        override val fragment: FragmentShader = FragmentShaderDefault {
            val coords01 = fragmentCoords01
            val texSize = TexInfoUB.u_TextureSize
            val time = DoodleUB.u_Time
            val strength = DoodleUB.u_LineStrength
            val jitterAmp = DoodleUB.u_Jitter

            // Texel size in UV space (1 source pixel)
            val texelW = createTemp(Float1)
            val texelH = createTemp(Float1)
            SET(texelW, 1f.lit / texSize.x)
            SET(texelH, 1f.lit / texSize.y)

            // Boil: tiny time-driven offset on the source texel lookup
            // Makes lines subtly shift each frame (doodle/animation-cel feel)
            val boilOffX = createTemp(Float1)
            val boilOffY = createTemp(Float1)
            SET(boilOffX, sin(time * 7.3f.lit + coords01.y * 31f.lit) * jitterAmp * texelW * 0.3f.lit)
            SET(boilOffY, sin(time * 5.7f.lit + coords01.x * 23f.lit) * jitterAmp * texelH * 0.3f.lit)

            // Apply boil to coordinates
            val uv_x = createTemp(Float1)
            val uv_y = createTemp(Float1)
            SET(uv_x, coords01.x + boilOffX)
            SET(uv_y, coords01.y + boilOffY)

            // ============================================================
            // POINT-SAMPLE: find the center of the source texel (NEAREST)
            // This is the key fix from Step 11: NO bilinear sampling.
            // ============================================================
            val srcX = createTemp(Float1)
            val srcY = createTemp(Float1)
            SET(srcX, floor(uv_x / texelW) * texelW + texelW * 0.5f.lit)
            SET(srcY, floor(uv_y / texelH) * texelH + texelH * 0.5f.lit)

            // Sample center P and 4 cardinal neighbors (all point-sampled)
            val P = tex(vec2(srcX, srcY))
            val A = tex(vec2(srcX, srcY - texelH))           // up
            val B = tex(vec2(srcX + texelW, srcY))           // right
            val C = tex(vec2(srcX - texelW, srcY))           // left
            val D = tex(vec2(srcX, srcY + texelH))           // down

            // ============================================================
            // EPX/Scale2x: determine sub-quadrant and apply rules
            // ============================================================
            // Sub-position within the source texel (0..1 within that texel)
            val fracX = createTemp(Float1)
            val fracY = createTemp(Float1)
            SET(fracX, fract(uv_x / texelW))
            SET(fracY, fract(uv_y / texelH))

            // Luminance helper for color comparison (with tolerance)
            val lumP = createTemp(Float1)
            val lumA = createTemp(Float1)
            val lumB = createTemp(Float1)
            val lumC = createTemp(Float1)
            val lumD = createTemp(Float1)
            SET(lumP, P.x * 0.299f.lit + P.y * 0.587f.lit + P.z * 0.114f.lit + P.w * 0.5f.lit)
            SET(lumA, A.x * 0.299f.lit + A.y * 0.587f.lit + A.z * 0.114f.lit + A.w * 0.5f.lit)
            SET(lumB, B.x * 0.299f.lit + B.y * 0.587f.lit + B.z * 0.114f.lit + B.w * 0.5f.lit)
            SET(lumC, C.x * 0.299f.lit + C.y * 0.587f.lit + C.z * 0.114f.lit + C.w * 0.5f.lit)
            SET(lumD, D.x * 0.299f.lit + D.y * 0.587f.lit + D.z * 0.114f.lit + D.w * 0.5f.lit)

            // Tolerance-based equality: |lum1 - lum2| < eps
            val eps = 0.12f.lit
            // C==A: abs(lumC-lumA) < eps
            val ca_eq = createTemp(Float1)
            SET(ca_eq, step(abs(lumC - lumA), eps))  // 1 if equal, 0 if different
            // C!=D
            val cd_neq = createTemp(Float1)
            SET(cd_neq, step(eps, abs(lumC - lumD)))  // 1 if different
            // A!=B
            val ab_neq = createTemp(Float1)
            SET(ab_neq, step(eps, abs(lumA - lumB)))
            // A==B
            val ab_eq = createTemp(Float1)
            SET(ab_eq, step(abs(lumA - lumB), eps))
            // B!=D
            val bd_neq = createTemp(Float1)
            SET(bd_neq, step(eps, abs(lumB - lumD)))
            // D==C
            val dc_eq = createTemp(Float1)
            SET(dc_eq, step(abs(lumD - lumC), eps))
            // B==D
            val bd_eq = createTemp(Float1)
            SET(bd_eq, step(abs(lumB - lumD), eps))
            // B!=A
            val ba_neq = createTemp(Float1)
            SET(ba_neq, step(eps, abs(lumB - lumA)))
            // D!=B
            val db_neq = createTemp(Float1)
            SET(db_neq, step(eps, abs(lumD - lumB)))
            // C!=A
            val ca_neq = createTemp(Float1)
            SET(ca_neq, step(eps, abs(lumC - lumA)))

            // EPX rules: P1(top-left), P2(top-right), P3(bot-left), P4(bot-right)
            // P1 = (C==A && C!=D && A!=B) ? A : P
            val rule1 = createTemp(Float1)
            SET(rule1, ca_eq * cd_neq * ab_neq)
            // P2 = (A==B && A!=C && B!=D) ? B : P
            val rule2 = createTemp(Float1)
            SET(rule2, ab_eq * ca_neq * bd_neq)
            // P3 = (D==C && D!=B && C!=A) ? C : P
            val rule3 = createTemp(Float1)
            SET(rule3, dc_eq * db_neq * ca_neq)
            // P4 = (B==D && B!=A && D!=C) ? D : P
            val rule4 = createTemp(Float1)
            SET(rule4, bd_eq * ba_neq * cd_neq)

            // Select which quadrant we're in and apply the appropriate rule
            val isLeft = createTemp(Float1)
            val isTop = createTemp(Float1)
            SET(isLeft, step(fracX, 0.5f.lit))    // 1 if fracX <= 0.5 (left half)
            SET(isTop, step(fracY, 0.5f.lit))     // 1 if fracY <= 0.5 (top half)

            // Mix: pick the EPX result for our sub-quadrant
            // Top-left: rule1 active → use A, else P
            // Top-right: rule2 active → use B, else P
            // Bot-left: rule3 active → use C, else P
            // Bot-right: rule4 active → use D, else P
            val epxColor = createTemp(Float4)

            // Compute per-quadrant result and select
            val tl = createTemp(Float4)
            val tr = createTemp(Float4)
            val bl = createTemp(Float4)
            val br = createTemp(Float4)
            SET(tl, P + (A - P) * rule1)
            SET(tr, P + (B - P) * rule2)
            SET(bl, P + (C - P) * rule3)
            SET(br, P + (D - P) * rule4)

            // Bilinear blend between quadrants based on sub-position
            val topMix = createTemp(Float4)
            val botMix = createTemp(Float4)
            SET(topMix, mix(tr, tl, fracX))
            SET(botMix, mix(br, bl, fracX))
            SET(epxColor, mix(botMix, topMix, fracY))

            // ============================================================
            // OUTLINE: detect edges and darken
            // ============================================================
            // Edge = strong luminance difference between P and any neighbor
            val maxGrad = createTemp(Float1)
            SET(maxGrad, max(max(abs(lumP - lumA), abs(lumP - lumB)), max(abs(lumP - lumC), abs(lumP - lumD))))

            // Also detect alpha edges (silhouette boundary)
            val alphaEdge = createTemp(Float1)
            SET(alphaEdge, max(max(abs(P.w - A.w), abs(P.w - B.w)), max(abs(P.w - C.w), abs(P.w - D.w))))

            // Combined edge factor
            val edgeFactor = createTemp(Float1)
            SET(edgeFactor, clamp((maxGrad + alphaEdge * 2f.lit) * strength * 3f.lit, 0f.lit, 1f.lit))

            // Darken edges: multiply RGB by (1 - edgeFactor * 0.9)
            val darkMul = createTemp(Float1)
            SET(darkMul, 1f.lit - edgeFactor * 0.9f.lit)

            val finalR = createTemp(Float1)
            val finalG = createTemp(Float1)
            val finalB = createTemp(Float1)
            SET(finalR, epxColor.x * darkMul)
            SET(finalG, epxColor.y * darkMul)
            SET(finalB, epxColor.z * darkMul)

            // Preserve alpha from the EPX result (sprite transparency)
            SET(out, vec4(finalR, finalG, finalB, epxColor.w))
        }
    }

    override val programProvider: ProgramProvider get() = DoodleLineFilter

    override fun updateUniforms(ctx: RenderContext, filterScale: Double) {
        super.updateUniforms(ctx, filterScale)
        ctx[DoodleUB].push {
            it[u_Time] = time
            it[u_LineStrength] = lineStrength
            it[u_Jitter] = jitter
        }
    }
}
