package game

import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.SolidRect
import korlibs.korge.view.solidRect
import rpg.weather.SeasonalGrid
import rpg.weather.WindState

/**
 * Renders summer grass as green tufts at tile bottoms with wind sway.
 * This overlay retains manual rect management because grass position depends
 * on wind + time (dynamic offset per frame), which GridOverlay's static
 * placement doesn't support. Still reduced from 82 to ~50 lines.
 */
class SummerOverlay(
    private val parent: Container,
    private val tileWidth: Int,
    private val tileHeight: Int,
) {
    private val rects = mutableListOf<SolidRect>()

    fun update(grid: SeasonalGrid, windState: WindState, elapsedTime: Float) {
        var rectIndex = 0
        val windSway = grid.windSwayGrass(windState.dx)
        val swayOffset = kotlin.math.sin(elapsedTime * 2.0f) * windSway * tileWidth

        for (ly in 0 until grid.height) {
            for (lx in 0 until grid.width) {
                val wx = lx + grid.offsetX
                val wy = ly + grid.offsetY
                val hash = (lx * 7 + ly * 13) % 7
                if (hash > 4) continue

                val bend = grid.grassBendAt(wx, wy)
                val alpha = if (bend > 0.5f) 140 else 200
                val green = if (hash < 2) 0xaa else 0xcc
                val color = RGBA(0x22, green, 0x33, alpha)

                val rect = getOrCreateRect(rectIndex)
                val grassW = tileWidth * 0.5
                val grassH = tileHeight * 0.35
                val baseX = wx.toDouble() * tileWidth + (tileWidth - grassW) / 2.0
                val baseY = wy.toDouble() * tileHeight + tileHeight * 0.65
                val bendOffset = bend * windState.dx * tileWidth * 0.4

                rect.x = baseX + bendOffset + swayOffset
                rect.y = baseY
                rect.scaledWidth = grassW
                rect.scaledHeight = grassH
                rect.color = color
                rect.visible = true
                rectIndex++
            }
        }
        for (i in rectIndex until rects.size) rects[i].visible = false
    }

    private fun getOrCreateRect(index: Int): SolidRect {
        if (index < rects.size) return rects[index]
        val rect = parent.solidRect(1.0, 1.0, RGBA(0x22, 0xaa, 0x33, 0x80)).apply { visible = false }
        rects.add(rect)
        return rect
    }
}
