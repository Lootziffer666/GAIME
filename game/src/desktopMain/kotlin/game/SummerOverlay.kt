package game

import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.SolidRect
import korlibs.korge.view.solidRect
import rpg.weather.SeasonalGrid
import rpg.weather.WindState

/**
 * Renders summer grass from a [SeasonalGrid] as small green tufts at the
 * bottom of each tile. Grass bends (x-offset) when walked on, and sways
 * gently in the wind.
 *
 * Follows the WaterOverlay/SnowOverlay pattern: pool SolidRects, show/hide per frame.
 */
class SummerOverlay(
    private val parent: Container,
    private val tileWidth: Int,
    private val tileHeight: Int,
) {
    private val rects = mutableListOf<SolidRect>()

    /**
     * @param grid The seasonal grid with grassBend data.
     * @param windState Current wind state for sway calculation.
     * @param elapsedTime Total elapsed time in seconds for oscillation.
     */
    fun update(grid: SeasonalGrid, windState: WindState, elapsedTime: Float) {
        var rectIndex = 0
        val windSway = grid.windSwayGrass(windState.dx)
        // Global sway oscillation using sin
        val swayOffset = kotlin.math.sin(elapsedTime * 2.0f) * windSway * tileWidth

        for (ly in 0 until grid.height) {
            for (lx in 0 until grid.width) {
                val wx = lx + grid.offsetX
                val wy = ly + grid.offsetY

                // All cells get grass (summer is lush). We use a hash to vary alpha.
                val hash = (lx * 7 + ly * 13) % 7
                if (hash > 4) continue // ~28% bare patches for variety

                val bend = grid.grassBendAt(wx, wy)
                val alpha = if (bend > 0.5f) 120 else 180 // bent grass is slightly faded

                // Choose green shade based on cell hash
                val green = if (hash < 2) 0xaa else 0xcc
                val color = RGBA(0x22, green, 0x33, alpha)

                // Grass tuft: small rect at bottom of tile
                val rect = getOrCreateRect(rectIndex)
                val grassW = tileWidth * 0.5
                val grassH = tileHeight * 0.3
                val baseX = wx.toDouble() * tileWidth + (tileWidth - grassW) / 2.0
                val baseY = wy.toDouble() * tileHeight + tileHeight * 0.7

                // Apply bend offset (player walked over) and wind sway
                val bendOffset = bend * windState.dx * tileWidth * 0.4
                rect.x = baseX + bendOffset + swayOffset
                rect.y = baseY
                rect.width = grassW
                rect.height = grassH
                rect.color = color
                rect.visible = true
                rectIndex++
            }
        }
        for (i in rectIndex until rects.size) {
            rects[i].visible = false
        }
    }

    private fun getOrCreateRect(index: Int): SolidRect {
        if (index < rects.size) return rects[index]
        val rect = parent.solidRect(1.0, 1.0, RGBA(0x22, 0xaa, 0x33, 0x80))
            .apply { visible = false }
        rects.add(rect)
        return rect
    }
}
