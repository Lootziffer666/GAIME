package game.shader

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.korge.render.*
import korlibs.korge.view.filter.ShaderFilter

/**
 * Doodle/Cartoon line shader — transforms character sprites into a hand-drawn look.
 *
 * Based on the classical Anime4K principle (reimplemented from concept, no foreign code):
 * detect local luminance gradients (neighbor pixel sampling) → darken and thin edges
 * to produce fine cartoon outlines. A time-driven jitter ("boil") makes lines subtly
 * shift each frame, creating the lively doodle/animation-cel feel.
 *
 * Apply ONLY to the character container (not the background) for the "cartoon figure
 * in front of painted scenery" look.
 *
 * Uniforms:
 * - u_Time: animation time (drives boil/jitter)
 * - u_LineStrength: 0..1, how prominent the dark outlines are
 * - u_Jitter: 0..~1, boil amplitude (0 = static lines, 1 = very wobbly)
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
            val coords = fragmentCoords
            val coords01 = fragmentCoords01
            val texSize = TexInfoUB.u_TextureSize
            val time = DoodleUB.u_Time
            val strength = DoodleUB.u_LineStrength
            val jitterAmp = DoodleUB.u_Jitter

            // Texel step (1 pixel in UV space)
            val dx = createTemp(Float1)
            val dy = createTemp(Float1)
            SET(dx, 1f.lit / texSize.x)
            SET(dy, 1f.lit / texSize.y)

            // Per-frame boil offset: tiny sin-based displacement on sample coords
            // This makes lines subtly shift each frame (doodle feel)
            val boilX = createTemp(Float1)
            val boilY = createTemp(Float1)
            SET(boilX, sin(time * 7.3f.lit + coords01.y * 23f.lit) * jitterAmp * dx * 0.8f.lit)
            SET(boilY, sin(time * 5.7f.lit + coords01.x * 19f.lit) * jitterAmp * dy * 0.8f.lit)

            // Sample center + 4 neighbors (with boil offset applied)
            val center = tex(coords + vec2(boilX, boilY))
            val left = tex(coords + vec2(-dx + boilX, boilY))
            val right = tex(coords + vec2(dx + boilX, boilY))
            val up = tex(coords + vec2(boilX, -dy + boilY))
            val down = tex(coords + vec2(boilX, dy + boilY))

            // Luminance of each sample (standard rec.709)
            val lumC = createTemp(Float1)
            val lumL = createTemp(Float1)
            val lumR = createTemp(Float1)
            val lumU = createTemp(Float1)
            val lumD = createTemp(Float1)
            SET(lumC, center.x * 0.2126f.lit + center.y * 0.7152f.lit + center.z * 0.0722f.lit)
            SET(lumL, left.x * 0.2126f.lit + left.y * 0.7152f.lit + left.z * 0.0722f.lit)
            SET(lumR, right.x * 0.2126f.lit + right.y * 0.7152f.lit + right.z * 0.0722f.lit)
            SET(lumU, up.x * 0.2126f.lit + up.y * 0.7152f.lit + up.z * 0.0722f.lit)
            SET(lumD, down.x * 0.2126f.lit + down.y * 0.7152f.lit + down.z * 0.0722f.lit)

            // Gradient magnitude (Sobel-like: max of horizontal and vertical differences)
            val gradH = createTemp(Float1)
            val gradV = createTemp(Float1)
            val grad = createTemp(Float1)
            SET(gradH, abs(lumR - lumL))
            SET(gradV, abs(lumD - lumU))
            SET(grad, max(gradH, gradV))

            // Edge darkening: where gradient is high, darken the pixel
            // This creates the "drawn line" effect on edges
            val edgeFactor = createTemp(Float1)
            SET(edgeFactor, clamp(grad * strength * 4f.lit, 0f.lit, 1f.lit))

            // Line color: dark (near black) edges, mixed with original
            // At full edge: pixel becomes very dark. At no edge: original color.
            val lineR = createTemp(Float1)
            val lineG = createTemp(Float1)
            val lineB = createTemp(Float1)
            SET(lineR, center.x * (1f.lit - edgeFactor * 0.85f.lit))
            SET(lineG, center.y * (1f.lit - edgeFactor * 0.85f.lit))
            SET(lineB, center.z * (1f.lit - edgeFactor * 0.85f.lit))

            // Preserve alpha from center sample (character sprites have transparency)
            SET(out, vec4(lineR, lineG, lineB, center.w))
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
