package game.shader

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.korge.render.*
import korlibs.korge.view.filter.ShaderFilter
import korlibs.math.*

/**
 * Rain particle shader -- renders falling rain streaks over the scene.
 * Wind direction, density, and speed are parametric.
 *
 * The rain is rendered as semi-transparent white/blue streaks using a
 * procedural pattern (no sprite sheet needed). Applied as a filter on the
 * map container -- rain appears above tiles but below HUD.
 *
 * @property intensity 0.0 = no rain, 1.0 = downpour.
 * @property windAngle Linear shear factor applied to rain streak UV coordinates.
 *   Despite the name, this is NOT an angle in radians -- it is used as a horizontal
 *   displacement coefficient (higher value = more diagonal rain). Named `windAngle`
 *   for consistency across weather shaders ([SnowFilter] uses the same convention).
 *   Typical range: -1.0 to 1.0.
 * @property time Animation time in seconds (drives rain fall position).
 */
class RainFilter(
    var intensity: Float = 0f,
    var windAngle: Float = 0.15f,
    var time: Float = 0f,
) : ShaderFilter() {

    object RainUB : UniformBlock(fixedLocation = 9) {
        val u_Intensity by float()
        val u_Wind by float()
        val u_Time by float()
    }

    companion object : BaseProgramProvider() {
        override val fragment: FragmentShader = FragmentShaderDefault {
            val coords01 = fragmentCoords01
            val texSize = TexInfoUB.u_TextureSize
            val intensity = RainUB.u_Intensity
            val wind = RainUB.u_Wind
            val time = RainUB.u_Time

            // Sample original scene
            val original = tex(fragmentCoords)

            // Rain pattern: tileable procedural streaks
            // Create a grid of rain drops using fract() for repetition
            val rainScale = 80f.lit  // density of rain cells
            val dropSpeed = 4f.lit   // how fast drops fall

            // Shear UV by wind angle for diagonal rain
            val sheared_x = coords01.x + coords01.y * wind
            val sheared_y = coords01.y

            // Create cell coordinates
            val cellX = fract(sheared_x * rainScale)
            val cellY = fract(sheared_y * rainScale * 0.3f.lit + time * dropSpeed)

            // Each rain drop is a thin vertical stripe in the center of its cell
            val dropWidth = 0.08f.lit
            val dropLength = 0.6f.lit
            val inDrop = step(0.5f.lit - dropWidth, cellX) *
                         step(cellX, 0.5f.lit + dropWidth) *
                         step(0.5f.lit - dropLength, cellY) *
                         step(cellY, 0.5f.lit + dropLength * 0.3f.lit)

            // Rain color: slightly blue-white, semi-transparent
            val rainAlpha = inDrop * intensity * 0.3f.lit
            val rainColor = vec4(0.7f.lit, 0.8f.lit, 1.0f.lit, rainAlpha)

            // Composite: original + rain overlay (additive-ish)
            val r = original.x + rainColor.x * rainAlpha
            val g = original.y + rainColor.y * rainAlpha
            val b = original.z + rainColor.z * rainAlpha

            SET(out, vec4(r, g, b, original.w))
        }
    }

    override val programProvider: ProgramProvider get() = RainFilter

    override fun updateUniforms(ctx: RenderContext, filterScale: Double) {
        super.updateUniforms(ctx, filterScale)
        ctx[RainUB].push {
            it[u_Intensity] = intensity
            it[u_Wind] = windAngle
            it[u_Time] = time
        }
    }
}
