package game

import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import rpg.weather.WaterGrid
import game.overlay.GridOverlay

/**
 * Renders puddles from a [WaterGrid] as semi-transparent blue rectangles.
 * Now delegates to [GridOverlay] — same visual, 1/4 the code.
 */
class WaterOverlay(
    parent: Container,
    tileWidth: Int,
    tileHeight: Int,
) {
    private val overlay = GridOverlay(parent, tileWidth, tileHeight, sizeFraction = 1f) { value, _, _ ->
        // Alpha floor: 110 + depth*140 (Skill-Lehre 7d/8)
        RGBA(0x22, 0x55, 0x99, (110 + value.coerceAtMost(1f) * 140).toInt())
    }

    fun update(grid: WaterGrid) {
        overlay.update(grid.width, grid.height, grid.offsetX, grid.offsetY) { wx, wy ->
            val depth = grid[wx - grid.offsetX, wy - grid.offsetY]
            if (depth > WaterGrid.PUDDLE_THRESHOLD) depth else 0f
        }
    }
}
