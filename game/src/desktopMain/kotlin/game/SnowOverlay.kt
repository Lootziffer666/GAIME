package game

import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.SolidRect
import korlibs.korge.view.solidRect
import rpg.weather.FootprintGrid
import rpg.weather.SnowGrid

/**
 * Renders snow accumulation from a [SnowGrid] as semi-transparent white rectangles.
 * Where [FootprintGrid] has footprints, alpha is reduced (dark gap reveals ground).
 *
 * Follows the WaterOverlay pattern: pool SolidRects, show/hide per frame.
 * Must be added to `mapView` so it scales/scrolls with the camera.
 */
class SnowOverlay(
    private val parent: Container,
    private val tileWidth: Int,
    private val tileHeight: Int,
) {
    private val rects = mutableListOf<SolidRect>()

    /**
     * Updates visible snow rectangles from the snow and footprint grid state.
     * Creates/reuses/hides rects as needed.
     */
    fun update(snowGrid: SnowGrid, footprintGrid: FootprintGrid) {
        var rectIndex = 0
        for (y in 0 until snowGrid.height) {
            for (x in 0 until snowGrid.width) {
                val depth = snowGrid.depthAt(x + snowGrid.offsetX, y + snowGrid.offsetY)
                if (depth > 0.05f) {
                    val rect = getOrCreateRect(rectIndex)
                    rect.x = (x + snowGrid.offsetX).toDouble() * tileWidth
                    rect.y = (y + snowGrid.offsetY).toDouble() * tileHeight
                    rect.width = tileWidth.toDouble()
                    rect.height = tileHeight.toDouble()

                    // Base alpha proportional to snow depth
                    var alpha = (80 + depth * 170).toInt()

                    // Where there are footprints, reduce alpha (ground shows through)
                    val fpIntensity = footprintGrid[x + snowGrid.offsetX, y + snowGrid.offsetY]
                    if (fpIntensity > 0.05f) {
                        alpha = (alpha * (1f - fpIntensity * 0.6f)).toInt()
                    }

                    rect.color = RGBA(0xff, 0xff, 0xff, alpha.coerceIn(0, 255))
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
        val rect = parent.solidRect(tileWidth.toDouble(), tileHeight.toDouble(), RGBA(0xff, 0xff, 0xff, 0x80))
            .apply { visible = false }
        rects.add(rect)
        return rect
    }
}
