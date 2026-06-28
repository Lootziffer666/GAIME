package game

import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.SolidRect
import korlibs.korge.view.solidRect
import rpg.weather.SeasonalGrid

/**
 * Renders autumn fallen leaves from a [SeasonalGrid] as small orange/brown/red
 * rectangles scattered on the ground. Each cell with leafCount > 0 gets 2-3
 * leaf rects at deterministic offsets based on cell coordinates.
 *
 * Follows the WaterOverlay/SnowOverlay pattern: pool SolidRects, show/hide per frame.
 */
class AutumnOverlay(
    private val parent: Container,
    private val tileWidth: Int,
    private val tileHeight: Int,
) {
    private val rects = mutableListOf<SolidRect>()

    // Leaf colors: orange, brown, red
    private val leafColors = listOf(
        RGBA(0xe8, 0x8a, 0x22, 0xff), // orange
        RGBA(0x8b, 0x45, 0x13, 0xff), // brown
        RGBA(0xcc, 0x33, 0x22, 0xff), // red
    )

    fun update(grid: SeasonalGrid) {
        var rectIndex = 0
        for (ly in 0 until grid.height) {
            for (lx in 0 until grid.width) {
                val wx = lx + grid.offsetX
                val wy = ly + grid.offsetY
                val leaves = grid.leafCountAt(wx, wy)
                if (leaves > 0.05f) {
                    val alpha = (leaves * 230).toInt().coerceIn(0, 255)
                    // 2-3 leaf rects per cell (deterministic count from hash)
                    val leafNum = 2 + ((lx * 3 + ly * 11) % 2)
                    for (i in 0 until leafNum) {
                        val colorIdx = (lx * 7 + ly * 13 + i * 3) % 3
                        val baseColor = leafColors[colorIdx]
                        val color = RGBA(baseColor.r, baseColor.g, baseColor.b, alpha)

                        // Deterministic offset within tile
                        val offX = ((lx * 11 + ly * 3 + i * 7) % 10) / 10.0
                        val offY = ((lx * 5 + ly * 9 + i * 4) % 10) / 10.0
                        val leafSize = tileWidth * 0.2

                        val rect = getOrCreateRect(rectIndex)
                        rect.x = wx.toDouble() * tileWidth + offX * (tileWidth - leafSize)
                        rect.y = wy.toDouble() * tileHeight + offY * (tileHeight - leafSize)
                        rect.width = leafSize
                        rect.height = leafSize
                        rect.color = color
                        rect.visible = true
                        rectIndex++
                    }
                }
            }
        }
        for (i in rectIndex until rects.size) {
            rects[i].visible = false
        }
    }

    private fun getOrCreateRect(index: Int): SolidRect {
        if (index < rects.size) return rects[index]
        val rect = parent.solidRect(1.0, 1.0, RGBA(0xe8, 0x8a, 0x22, 0x80))
            .apply { visible = false }
        rects.add(rect)
        return rect
    }
}
