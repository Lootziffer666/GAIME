package game

import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.SolidRect
import korlibs.korge.view.solidRect
import rpg.weather.BloodGrid
import rpg.weather.SnowGrid

/**
 * Renders blood spills from a [BloodGrid] as semi-transparent red rectangles.
 * Fresh blood is bright red; aged blood is dark red.
 * On snow (SnowGrid depth > 0.3 at same cell), alpha is boosted for extra contrast.
 *
 * Follows the WaterOverlay pattern: pool SolidRects, show/hide per frame.
 * Must be added to `mapView` so it scales/scrolls with the camera.
 */
class BloodOverlay(
    private val parent: Container,
    private val tileWidth: Int,
    private val tileHeight: Int,
) {
    private val rects = mutableListOf<SolidRect>()

    /**
     * Updates visible blood rectangles from the blood grid state.
     * [snowGrid] is used for snow-contrast boost (optional, pass null if no snow).
     */
    fun update(bloodGrid: BloodGrid, snowGrid: SnowGrid?) {
        var rectIndex = 0
        for (y in 0 until bloodGrid.height) {
            for (x in 0 until bloodGrid.width) {
                val worldX = x + bloodGrid.offsetX
                val worldY = y + bloodGrid.offsetY
                val amount = bloodGrid.amountAt(worldX, worldY)
                if (amount > 0.01f) {
                    val rect = getOrCreateRect(rectIndex)
                    rect.x = worldX.toDouble() * tileWidth
                    rect.y = worldY.toDouble() * tileHeight
                    rect.width = tileWidth.toDouble()
                    rect.height = tileHeight.toDouble()

                    val fresh = bloodGrid.isFresh(worldX, worldY)
                    var alpha = (amount * 180).toInt()

                    // On snow, boost alpha for extra contrast
                    if (snowGrid != null && snowGrid.depthAt(worldX, worldY) > 0.3f) {
                        alpha += 40
                    }
                    alpha = alpha.coerceIn(0, 255)

                    rect.color = if (fresh) {
                        RGBA(0xcc, 0x11, 0x11, alpha)
                    } else {
                        RGBA(0x55, 0x11, 0x11, alpha)
                    }
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
        val rect = parent.solidRect(tileWidth.toDouble(), tileHeight.toDouble(), RGBA(0xcc, 0x11, 0x11, 0x80))
            .apply { visible = false }
        rects.add(rect)
        return rect
    }
}
