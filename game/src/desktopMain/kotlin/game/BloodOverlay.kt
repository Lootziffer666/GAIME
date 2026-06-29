package game

import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import rpg.weather.BloodGrid
import rpg.weather.SnowGrid
import game.overlay.GridOverlay

/**
 * Renders blood spills as red rectangles. Fresh = bright, aged = dark.
 * Snow boosts contrast. Now delegates to [GridOverlay].
 */
class BloodOverlay(
    parent: Container,
    tileWidth: Int,
    tileHeight: Int,
) {
    private var bloodGridRef: BloodGrid? = null
    private var snowGridRef: SnowGrid? = null

    private val overlay = GridOverlay(parent, tileWidth, tileHeight, sizeFraction = 1f) { value, wx, wy ->
        val bg = bloodGridRef ?: return@GridOverlay null
        val fresh = bg.isFresh(wx, wy)
        var alpha = (value * 180).toInt()
        val sg = snowGridRef
        if (sg != null && sg.depthAt(wx, wy) > 0.3f) alpha += 40
        alpha = alpha.coerceIn(0, 255)
        if (fresh) RGBA(0xcc, 0x11, 0x11, alpha) else RGBA(0x55, 0x11, 0x11, alpha)
    }

    fun update(bloodGrid: BloodGrid, snowGrid: SnowGrid?) {
        bloodGridRef = bloodGrid
        snowGridRef = snowGrid
        overlay.update(bloodGrid.width, bloodGrid.height, bloodGrid.offsetX, bloodGrid.offsetY) { wx, wy ->
            val amount = bloodGrid.amountAt(wx, wy)
            if (amount > 0.01f) amount else 0f
        }
    }
}
