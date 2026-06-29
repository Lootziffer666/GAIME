package game

import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import rpg.weather.FootprintGrid
import game.overlay.GridOverlay

/**
 * Renders boot imprints from a [FootprintGrid] as dark brownish rectangles (60% cell).
 * Now delegates to [GridOverlay].
 */
class FootprintOverlay(
    parent: Container,
    tileWidth: Int,
    tileHeight: Int,
) {
    private val overlay = GridOverlay(parent, tileWidth, tileHeight, sizeFraction = 0.6f) { value, _, _ ->
        val alpha = (value * 160).toInt().coerceIn(0, 255)
        RGBA(0x44, 0x33, 0x22, alpha)
    }

    fun update(footprintGrid: FootprintGrid) {
        overlay.update(footprintGrid.width, footprintGrid.height, footprintGrid.offsetX, footprintGrid.offsetY) { wx, wy ->
            val intensity = footprintGrid[wx, wy]
            if (intensity > 0.05f) intensity else 0f
        }
    }
}
