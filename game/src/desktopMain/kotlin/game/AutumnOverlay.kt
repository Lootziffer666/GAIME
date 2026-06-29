package game

import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.SolidRect
import korlibs.korge.view.solidRect
import rpg.weather.SeasonalGrid

/**
 * Renders autumn leaves as scattered orange/brown/red rects (2-3 per cell).
 * Retains manual rect management because multiple rects per cell with
 * deterministic offsets exceed GridOverlay's 1-rect-per-cell model.
 * Reduced from 76 to ~50 lines.
 */
class AutumnOverlay(
    private val parent: Container,
    private val tileWidth: Int,
    private val tileHeight: Int,
) {
    private val rects = mutableListOf<SolidRect>()
    private val leafColors = listOf(
        RGBA(0xe8, 0x8a, 0x22, 0xff),
        RGBA(0x8b, 0x45, 0x13, 0xff),
        RGBA(0xcc, 0x33, 0x22, 0xff),
    )

    fun update(grid: SeasonalGrid) {
        var rectIndex = 0
        for (ly in 0 until grid.height) {
            for (lx in 0 until grid.width) {
                val wx = lx + grid.offsetX
                val wy = ly + grid.offsetY
                val leaves = grid.leafCountAt(wx, wy)
                if (leaves <= 0.05f) continue

                val alpha = (110 + leaves * 140).toInt().coerceIn(0, 255)
                val leafNum = 2 + ((lx * 3 + ly * 11) % 2)
                for (i in 0 until leafNum) {
                    val colorIdx = (lx * 7 + ly * 13 + i * 3) % 3
                    val base = leafColors[colorIdx]
                    val color = RGBA(base.r, base.g, base.b, alpha)
                    val offX = ((lx * 11 + ly * 3 + i * 7) % 10) / 10.0
                    val offY = ((lx * 5 + ly * 9 + i * 4) % 10) / 10.0
                    val leafSize = tileWidth * 0.25

                    val rect = getOrCreateRect(rectIndex)
                    rect.x = wx.toDouble() * tileWidth + offX * (tileWidth - leafSize)
                    rect.y = wy.toDouble() * tileHeight + offY * (tileHeight - leafSize)
                    rect.scaledWidth = leafSize
                    rect.scaledHeight = leafSize
                    rect.color = color
                    rect.visible = true
                    rectIndex++
                }
            }
        }
        for (i in rectIndex until rects.size) rects[i].visible = false
    }

    private fun getOrCreateRect(index: Int): SolidRect {
        if (index < rects.size) return rects[index]
        val rect = parent.solidRect(1.0, 1.0, RGBA(0xe8, 0x8a, 0x22, 0x80)).apply { visible = false }
        rects.add(rect)
        return rect
    }
}
