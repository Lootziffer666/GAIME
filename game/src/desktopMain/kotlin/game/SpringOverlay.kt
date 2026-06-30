package game

import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import rpg.weather.SeasonalGrid
import game.overlay.GridOverlay

/**
 * Renders spring flowers as small pink/yellow rects + blossom rects for high intensity.
 * Uses two GridOverlays: one for flowers, one for blossom effect.
 */
class SpringOverlay(
    parent: Container,
    tileWidth: Int,
    tileHeight: Int,
) {
    private val flowerOverlay = GridOverlay(parent, tileWidth, tileHeight, sizeFraction = 0.5f) { value, wx, wy ->
        val alpha = (110 + value * 140).toInt().coerceIn(0, 255)
        val hash = (wx * 7 + wy * 13) % 5
        if (hash < 3) RGBA(0xff, 0x88, 0xcc, alpha) else RGBA(0xff, 0xdd, 0x44, alpha)
    }

    private val blossomOverlay = GridOverlay(parent, tileWidth, tileHeight, sizeFraction = 0.3f) { value, _, _ ->
        if (value <= 0.7f) return@GridOverlay null
        val alpha = (110 + value * 140 * 0.85f).toInt().coerceIn(0, 255)
        RGBA(0xff, 0xee, 0xf0, alpha)
    }

    fun update(grid: SeasonalGrid) {
        flowerOverlay.update(grid.width, grid.height, grid.offsetX, grid.offsetY) { wx, wy ->
            val intensity = grid.flowerAt(wx, wy)
            if (intensity > 0.1f) intensity else 0f
        }
        blossomOverlay.update(grid.width, grid.height, grid.offsetX, grid.offsetY) { wx, wy ->
            val intensity = grid.flowerAt(wx, wy)
            if (intensity > 0.7f) intensity else 0f
        }
    }
}
