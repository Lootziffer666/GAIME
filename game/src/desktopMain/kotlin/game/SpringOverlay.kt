package game

import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.SolidRect
import korlibs.korge.view.solidRect
import rpg.weather.SeasonalGrid

/**
 * Renders spring flowers from a [SeasonalGrid] as small pink/yellow rectangles.
 * Higher flowerIntensity = brighter alpha. Trampled flowers fade out.
 * Cells with very high intensity (>0.7) get extra "blossom" rects (tree canopy).
 *
 * Follows the WaterOverlay/SnowOverlay pattern: pool SolidRects, show/hide per frame.
 */
class SpringOverlay(
    private val parent: Container,
    private val tileWidth: Int,
    private val tileHeight: Int,
) {
    private val rects = mutableListOf<SolidRect>()

    fun update(grid: SeasonalGrid) {
        var rectIndex = 0
        for (ly in 0 until grid.height) {
            for (lx in 0 until grid.width) {
                val wx = lx + grid.offsetX
                val wy = ly + grid.offsetY
                val intensity = grid.flowerAt(wx, wy)
                if (intensity > 0.1f) {
                    // Kräftiges Alpha (Lehre 7d/8): mindestens 110, skaliert mit Intensität
                    val alpha = (110 + intensity * 140).toInt().coerceIn(0, 255)

                    // Determine color: alternating pink/yellow based on cell hash
                    val hash = (lx * 7 + ly * 13) % 5
                    val color = if (hash < 3)
                        RGBA(0xff, 0x88, 0xcc, alpha) // pink
                    else
                        RGBA(0xff, 0xdd, 0x44, alpha) // yellow

                    // Flower rect: ~50% of tile, centered (mapView coordinates)
                    val rect = getOrCreateRect(rectIndex)
                    val flowerSize = tileWidth * 0.5
                    val offsetInTile = (tileWidth - flowerSize) / 2.0
                    rect.x = wx.toDouble() * tileWidth + offsetInTile
                    rect.y = wy.toDouble() * tileHeight + offsetInTile
                    rect.scaledWidth = flowerSize
                    rect.scaledHeight = flowerSize
                    rect.color = color
                    rect.visible = true
                    rectIndex++

                    // Tree blossom effect: extra small white/pink rects for high intensity
                    if (intensity > 0.7f) {
                        val blosRect = getOrCreateRect(rectIndex)
                        val blosSize = tileWidth * 0.3
                        blosRect.x = wx.toDouble() * tileWidth + tileWidth * 0.1
                        blosRect.y = wy.toDouble() * tileHeight - tileHeight * 0.15
                        blosRect.scaledWidth = blosSize
                        blosRect.scaledHeight = blosSize
                        blosRect.color = RGBA(0xff, 0xee, 0xf0, (alpha * 0.85).toInt().coerceIn(0, 255))
                        blosRect.visible = true
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
        val rect = parent.solidRect(1.0, 1.0, RGBA(0xff, 0x88, 0xcc, 0x80))
            .apply { visible = false }
        rects.add(rect)
        return rect
    }
}
