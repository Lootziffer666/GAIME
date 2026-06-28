package game

import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.SolidRect
import korlibs.korge.view.solidRect
import rpg.weather.FootprintGrid

/**
 * Renders boot imprints from a [FootprintGrid] as small dark brownish rectangles.
 * Each footprint is slightly smaller than a full tile (60% centered) to represent
 * a boot mark rather than a full tile fill.
 *
 * Follows the WaterOverlay pattern: pool SolidRects, show/hide per frame.
 * Must be added to `mapView` so it scales/scrolls with the camera.
 */
class FootprintOverlay(
    private val parent: Container,
    private val tileWidth: Int,
    private val tileHeight: Int,
) {
    private val rects = mutableListOf<SolidRect>()
    private val sizeRatio = 0.6

    /**
     * Updates visible footprint rectangles from the footprint grid state.
     */
    fun update(footprintGrid: FootprintGrid) {
        var rectIndex = 0
        for (y in 0 until footprintGrid.height) {
            for (x in 0 until footprintGrid.width) {
                val worldX = x + footprintGrid.offsetX
                val worldY = y + footprintGrid.offsetY
                val intensity = footprintGrid[worldX, worldY]
                if (intensity > 0.05f) {
                    val rect = getOrCreateRect(rectIndex)
                    val w = tileWidth * sizeRatio
                    val h = tileHeight * sizeRatio
                    val offsetX = (tileWidth - w) / 2.0
                    val offsetY = (tileHeight - h) / 2.0
                    rect.x = worldX.toDouble() * tileWidth + offsetX
                    rect.y = worldY.toDouble() * tileHeight + offsetY
                    rect.width = w
                    rect.height = h

                    val alpha = (intensity * 160).toInt().coerceIn(0, 255)
                    rect.color = RGBA(0x44, 0x33, 0x22, alpha)
                    rect.visible = true
                    rectIndex++
                }
            }
        }
        // Hide unused rects
        for (i in rectIndex until rects.size) {
            rects[i].visible = false
        }
    }

    private fun getOrCreateRect(index: Int): SolidRect {
        if (index < rects.size) return rects[index]
        val w = tileWidth * sizeRatio
        val h = tileHeight * sizeRatio
        val rect = parent.solidRect(w, h, RGBA(0x44, 0x33, 0x22, 0x80))
            .apply { visible = false }
        rects.add(rect)
        return rect
    }
}
