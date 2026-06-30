package game.shader

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.korge.render.*
import korlibs.korge.view.filter.ShaderFilter

/**
 * Underwater caustic light patterns — animated refracted-light web projected
 * onto surfaces beneath water. Creates the dancing light patterns you see at
 * the bottom of a swimming pool.
 *
 * Applied to surfaces under/near water overlays to indicate depth and movement.
 * The pattern is generated procedurally from overlapping sine waves (Voronoi-like
 * approximation without actual Voronoi — much cheaper).
 *
 * Technique: thebookofshaders (Ch. 12 Cellular Noise) simplified to sine-overlap.
 */
class CausticFilter(
    var time: Float = 0f,
    var intensity: Float = 0.3f,
    var scale: Float = 40f,
    var speed: Float = 1.5f,
) : ShaderFilter() {

    object CausticUB : UniformBlock(fixedLocation = 15) {
        val u_Time by float()
        val u_Intensity by float()
        val u_Scale by float()
        val u_Speed by float()
    }

    companion object : BaseProgramProvider() {
        override val fragment: FragmentShader = FragmentShaderDefault {
            val coords = fragmentCoords
            val texSize = TexInfoUB.u_TextureSize
            val time = CausticUB.u_Time
            val intensity = CausticUB.u_Intensity
            val scale = CausticUB.u_Scale
            val speed = CausticUB.u_Speed

            val original = tex(coords)

            // Normalized UV
            val uvX = createTemp(Float1)
            val uvY = createTemp(Float1)
            SET(uvX, coords.x / texSize.x * scale)
            SET(uvY, coords.y / texSize.y * scale)

            // Caustic pattern: overlapping sine waves at different angles
            // This approximates the refracted light web cheaply
            val c1 = createTemp(Float1)
            val c2 = createTemp(Float1)
            val c3 = createTemp(Float1)
            SET(c1, sin(uvX * 1.3f.lit + time * speed + sin(uvY * 0.7f.lit + time * 0.5f.lit)))
            SET(c2, sin(uvY * 1.7f.lit - time * speed * 0.8f.lit + sin(uvX * 1.1f.lit - time * 0.3f.lit)))
            SET(c3, sin((uvX + uvY) * 0.9f.lit + time * speed * 0.6f.lit))

            // Combine: caustic = where all waves align (bright spots)
            val caustic = createTemp(Float1)
            SET(caustic, (c1 + c2 + c3) / 3f.lit)
            // Sharpen: only keep the peaks (power function)
            SET(caustic, clamp(caustic * caustic * 2f.lit, 0f.lit, 1f.lit))

            // Apply: brighten original by caustic amount (additive light)
            val finalR = createTemp(Float1)
            val finalG = createTemp(Float1)
            val finalB = createTemp(Float1)
            SET(finalR, original.x + caustic * intensity * 0.9f.lit)
            SET(finalG, original.y + caustic * intensity * 1.0f.lit)
            SET(finalB, original.z + caustic * intensity * 1.1f.lit) // slightly blue tint

            SET(out, vec4(
                clamp(finalR, 0f.lit, 1f.lit),
                clamp(finalG, 0f.lit, 1f.lit),
                clamp(finalB, 0f.lit, 1f.lit),
                original.w
            ))
        }
    }

    override val programProvider: ProgramProvider get() = CausticFilter

    override fun updateUniforms(ctx: RenderContext, filterScale: Double) {
        super.updateUniforms(ctx, filterScale)
        ctx[CausticUB].push {
            it[u_Time] = time
            it[u_Intensity] = intensity
            it[u_Scale] = scale
            it[u_Speed] = speed
        }
    }
}
