package game

import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.SolidRect
import korlibs.korge.view.solidRect
import rpg.weather.WaterGrid

/**
 * Renders puddles from a [WaterGrid] as semi-transparent blue rectangles on the map.
 * Alpha proportional to waterDepth. Updated per frame from the simulation grid.
 *
 * Must be added to `mapView` so it scales/scrolls with the camera.
 */
class WaterOverlay(
    private val parent: Container,
    private val tileWidth: Int,
    private val tileHeight: Int,
) {
    private val puddleRects = mutableListOf<SolidRect>()
    private val puddleColor = RGBA(0x33, 0x66, 0xcc, 0x55)

    /**
     * Updates visible puddle rectangles from the water grid state.
     * Creates/reuses/hides rects as needed.
     */
    fun update(grid: WaterGrid) {
        var rectIndex = 0
        for (y in 0 until grid.height) {
            for (x in 0 until grid.width) {
                val depth = grid[x, y]
                if (depth > WaterGrid.PUDDLE_THRESHOLD) {
                    val rect = getOrCreateRect(rectIndex)
                    rect.x = (x + grid.offsetX).toDouble() * tileWidth
                    rect.y = (y + grid.offsetY).toDouble() * tileHeight
                    rect.width = tileWidth.toDouble()
                    rect.height = tileHeight.toDouble()
                    rect.color = RGBA(0x33, 0x66, 0xcc, (depth.coerceAtMost(1f) * 120).toInt())
                    rect.visible = true
                    rectIndex++
                }
            }
        }
        // Hide unused rects
        for (i in rectIndex until puddleRects.size) {
            puddleRects[i].visible = false
        }
    }

    private fun getOrCreateRect(index: Int): SolidRect {
        if (index < puddleRects.size) return puddleRects[index]
        val rect = parent.solidRect(tileWidth.toDouble(), tileHeight.toDouble(), puddleColor)
            .apply { visible = false }
        puddleRects.add(rect)
        return rect
    }
}
