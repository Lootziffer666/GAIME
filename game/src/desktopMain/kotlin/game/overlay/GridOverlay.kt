package game.overlay

import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.SolidRect
import korlibs.korge.view.solidRect

/**
 * Generic grid-based overlay renderer. Replaces 8 near-identical Overlay classes
 * with a single parameterized implementation.
 *
 * Architecture:
 * - Rect-pooling (create on demand, reuse, hide unused)
 * - Placement in parent coordinates (parent = worldLayer/mapView → auto-scrolls with camera)
 * - sizeFraction: 1.0 = full tile, 0.5 = half tile centered, etc.
 * - colorOf: maps (value, worldX, worldY) → RGBA or null (skip cell)
 *
 * Alpha-floor convention (Skill-Lehre 7d/8): callers should use
 * `(110 + value * 140).toInt()` as alpha minimum so overlays read against busy ground.
 */
class GridOverlay(
    private val parent: Container,
    private val tileWidth: Int,
    private val tileHeight: Int,
    private val sizeFraction: Float = 1f,
    private val colorOf: (value: Float, wx: Int, wy: Int) -> RGBA?,
) {
    private val rects = mutableListOf<SolidRect>()

    /**
     * Update visible rects from a grid.
     *
     * @param width Grid width (columns)
     * @param height Grid height (rows)
     * @param offsetX World-coordinate offset of column 0
     * @param offsetY World-coordinate offset of row 0
     * @param valueAt Returns the intensity/depth at world coordinates (wx, wy).
     *                Return 0 or negative to skip the cell.
     */
    fun update(
        width: Int,
        height: Int,
        offsetX: Int,
        offsetY: Int,
        valueAt: (wx: Int, wy: Int) -> Float,
    ) {
        var rectIndex = 0
        val rectW = tileWidth * sizeFraction
        val rectH = tileHeight * sizeFraction
        val insetX = (tileWidth - rectW) / 2f
        val insetY = (tileHeight - rectH) / 2f

        for (ly in 0 until height) {
            for (lx in 0 until width) {
                val wx = lx + offsetX
                val wy = ly + offsetY
                val value = valueAt(wx, wy)
                if (value <= 0f) continue

                val color = colorOf(value, wx, wy) ?: continue

                val rect = getOrCreateRect(rectIndex)
                rect.x = (wx * tileWidth + insetX).toDouble()
                rect.y = (wy * tileHeight + insetY).toDouble()
                rect.scaledWidth = rectW.toDouble()
                rect.scaledHeight = rectH.toDouble()
                rect.color = color
                rect.visible = true
                rectIndex++
            }
        }
        // Hide unused rects
        for (i in rectIndex until rects.size) {
            rects[i].visible = false
        }
    }

    private fun getOrCreateRect(index: Int): SolidRect {
        if (index < rects.size) return rects[index]
        val rect = parent.solidRect(1.0, 1.0, RGBA(0, 0, 0, 0))
            .apply { visible = false }
        rects.add(rect)
        return rect
    }
}
