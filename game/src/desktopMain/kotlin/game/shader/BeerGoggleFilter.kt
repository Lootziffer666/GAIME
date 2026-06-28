package game.shader

import korlibs.graphics.shader.*
import korlibs.korge.render.RenderContext
import korlibs.korge.view.filter.ShaderFilter

/**
 * "Beer Goggles" shader — the world gets softer, warmer, and slightly swaying.
 * Makes everything (and everyone) look more appealing after a few ales.
 *
 * - Gaussian-like blur (softens harsh edges → NPCs look better)
 * - Warm amber tint (everything glows)
 * - Slight horizontal sway (world wobbles)
 *
 * [drunkLevel] 0.0 = sober, 1.0 = three flagons deep.
 * [time] drives the sway animation.
 */
class BeerGoggleFilter(
    var drunkLevel: Float = 0f,
    var time: Float = 0f,
) : ShaderFilter() {

    object BeerUB : UniformBlock(fixedLocation = 7) {
        val u_DrunkLevel by float()
        val u_Time by float()
    }

    companion object : BaseProgramProvider() {
        override val fragment = FragmentShader {
            val coords01 = fragmentCoords01
            val drunk = BeerUB.u_DrunkLevel
            val time = BeerUB.u_Time
            val texSize = TexInfoUB.u_TextureSize

            // Horizontal sway (world wobbles gently)
            val sway = sin(time * 1.5f.lit + coords01.y * 4f.lit) * drunk * 0.008f.lit
            val swayCoord = coords01 + vec2(sway, 0f.lit)

            // Fake blur: sample 5 points and average (box blur approximation)
            val blurRadius = drunk * 2f.lit / texSize.x
            val c0 = tex(swayCoord * texSize)
            val c1 = tex((swayCoord + vec2(blurRadius, 0f.lit)) * texSize)
            val c2 = tex((swayCoord + vec2(-blurRadius, 0f.lit)) * texSize)
            val c3 = tex((swayCoord + vec2(0f.lit, blurRadius)) * texSize)
            val c4 = tex((swayCoord + vec2(0f.lit, -blurRadius)) * texSize)
            val blurred = (c0 + c1 + c2 + c3 + c4) / 5f.lit

            // Warm amber tint
            val warmth = vec4(1.05f.lit, 0.95f.lit, 0.8f.lit, 1f.lit)
            val tinted = blurred * mix(vec4(1f.lit), warmth, drunk * 0.4f.lit)

            // Slight brightness boost (everything looks nicer through beer)
            val boosted = tinted * (1f.lit + drunk * 0.1f.lit)

            SET(out, vec4(boosted["rgb"], blurred["a"]))
        }
    }

    override val programProvider: ProgramProvider get() = BeerGoggleFilter

    override fun updateUniforms(ctx: RenderContext, filterScale: Double) {
        super.updateUniforms(ctx, filterScale)
        ctx[BeerUB].push {
            it[u_DrunkLevel] = drunkLevel
            it[u_Time] = time
        }
    }
}
