package game

import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.SolidRect
import korlibs.korge.view.solidRect
import rpg.weather.MaterialFatigue

/**
 * Renders material fatigue/cracks from a [MaterialFatigue] grid as visible
 * crack lines on affected tiles. Hairline cracks appear at CRACK_THRESHOLD,
 * major fractures at BROKEN_THRESHOLD.
 *
 * Cracks are rendered as thin light-colored lines (stress fractures) that
 * APPEAR before something breaks — no HP bar needed, the player SEES the damage.
 *
 * Follows the WaterOverlay/SnowOverlay pattern: pool SolidRects, show/hide per frame.
 */
class MaterialFatigueOverlay(
    private val parent: Container,
    private val tileWidth: Int,
    private val tileHeight: Int,
) {
    private val rects = mutableListOf<SolidRect>()

    // Crack colors
    private val hairlineCrack = RGBA(0xcc, 0xbb, 0x99, 0x90)  // light tan, semi-transparent
    private val majorFracture = RGBA(0x44, 0x33, 0x22, 0xdd)  // dark brown, nearly opaque

    fun update(grid: MaterialFatigue) {
        var rectIndex = 0
        for (ly in 0 until grid.height) {
            for (lx in 0 until grid.width) {
                val wx = lx + grid.offsetX
                val wy = ly + grid.offsetY
                val stress = grid.stressAt(wx, wy)

                if (stress >= MaterialFatigue.CRACK_THRESHOLD) {
                    val isBroken = stress >= MaterialFatigue.BROKEN_THRESHOLD
                    val color = if (isBroken) majorFracture else hairlineCrack

                    // Crack pattern: 2-3 thin lines per tile based on stress level
                    val crackCount = if (isBroken) 3 else if (stress > 0.5f) 2 else 1

                    for (i in 0 until crackCount) {
                        val rect = getOrCreateRect(rectIndex)
                        // Deterministic crack placement using cell hash
                        val hash = (lx * 7 + ly * 13 + i * 11) % 10
                        val isHorizontal = hash < 5

                        if (isHorizontal) {
                            // Horizontal crack line
                            val crackW = tileWidth * (0.4 + stress * 0.4)
                            val crackH = if (isBroken) 2.0 else 1.0
                            val offX = (hash % 4) / 8.0 * tileWidth
                            val offY = (0.2 + i * 0.25) * tileHeight
                            rect.x = wx.toDouble() * tileWidth + offX
                            rect.y = wy.toDouble() * tileHeight + offY
                            rect.width = crackW
                            rect.height = crackH
                        } else {
                            // Vertical crack line
                            val crackW = if (isBroken) 2.0 else 1.0
                            val crackH = tileHeight * (0.3 + stress * 0.4)
                            val offX = (0.2 + i * 0.3) * tileWidth
                            val offY = (hash % 4) / 8.0 * tileHeight
                            rect.x = wx.toDouble() * tileWidth + offX
                            rect.y = wy.toDouble() * tileHeight + offY
                            rect.width = crackW
                            rect.height = crackH
                        }

                        rect.color = color
                        rect.visible = true
                        rectIndex++
                    }
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
        val rect = parent.solidRect(1.0, 1.0, hairlineCrack)
            .apply { visible = false }
        rects.add(rect)
        return rect
    }
}
