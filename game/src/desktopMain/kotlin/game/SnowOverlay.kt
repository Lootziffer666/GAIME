package game

import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import rpg.weather.FootprintGrid
import rpg.weather.SnowGrid
import game.overlay.GridOverlay

/**
 * Renders snow accumulation as white rectangles. Footprints reduce alpha.
 * Now delegates to [GridOverlay].
 */
class SnowOverlay(
    parent: Container,
    tileWidth: Int,
    tileHeight: Int,
) {
    private var footprints: FootprintGrid? = null

    /** Optional walkability mask — snow only dusts the playable surface, not painted walls/objects. */
    var floorMask: ((Int, Int) -> Boolean)? = null

    private val overlay = GridOverlay(parent, tileWidth, tileHeight, sizeFraction = 1f) { value, wx, wy ->
        // Full-coverage system: NO sparse alpha floor (that white-outs the painted art).
        // Gentle proportional dusting; floor-masked so it never blankets walls/water/objects.
        var alpha = (value * 130).toInt()
        // Footprints reduce alpha
        val fp = footprints
        if (fp != null) {
            val fpIntensity = fp[wx, wy]
            if (fpIntensity > 0.05f) {
                alpha = (alpha * (1f - fpIntensity * 0.6f)).toInt()
            }
        }
        RGBA(0xff, 0xff, 0xff, alpha.coerceIn(0, 150))
    }

    fun update(snowGrid: SnowGrid, footprintGrid: FootprintGrid) {
        footprints = footprintGrid
        overlay.mask = floorMask
        overlay.update(snowGrid.width, snowGrid.height, snowGrid.offsetX, snowGrid.offsetY) { wx, wy ->
            val depth = snowGrid.depthAt(wx, wy)
            if (depth > 0.05f) depth else 0f
        }
    }
}
