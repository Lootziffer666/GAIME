package ui.rpg

import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import engine.Scene
import rpg.world.Camera
import rpg.world.GridWorld

/**
 * Renders a [GridWorld] onto a Compose canvas, reusing the existing
 * [Scene]/[ui.GameCanvas] frame-loop. Tiles are blitted from a packed atlas; a
 * camera follows the player and clamps to the map. Trigger tiles reached by the
 * player are forwarded via [onTrigger] (wired to the bark pipeline by the host).
 */
class WorldScene(
    val world: GridWorld,
    private val tileset: ImageBitmap,
    private val playerSprite: ImageBitmap,
    private val srcTile: Int = 16,
    private val tilePx: Float = 48f
) : Scene {

    override val name: String = "World"

    var onTrigger: ((String) -> Unit)? = null

    private val camera = Camera()
    private val columns = (tileset.width / srcTile).coerceAtLeast(1)

    override fun update(deltaTime: Float) {
        world.update(deltaTime)
        val triggered = world.consumeTriggers()
        if (triggered.isNotEmpty()) {
            val cb = onTrigger
            if (cb != null) triggered.forEach(cb)
        }
    }

    override fun draw(drawScope: DrawScope) {
        val map = world.map
        val vw = drawScope.size.width
        val vh = drawScope.size.height
        val worldW = map.width * tilePx
        val worldH = map.height * tilePx

        val pCenterX = (world.player.visualX() + 0.5f) * tilePx
        val pCenterY = (world.player.visualY() + 0.5f) * tilePx
        camera.follow(pCenterX, pCenterY, vw, vh, worldW, worldH)
        val camX = camera.x
        val camY = camera.y

        val x0 = (camX / tilePx).toInt().coerceAtLeast(0)
        val y0 = (camY / tilePx).toInt().coerceAtLeast(0)
        val x1 = ((camX + vw) / tilePx).toInt().coerceAtMost(map.width - 1)
        val y1 = ((camY + vh) / tilePx).toInt().coerceAtMost(map.height - 1)

        for (ty in y0..y1) {
            for (tx in x0..x1) {
                blit(drawScope, map.tileAt(tx, ty), tx * tilePx - camX, ty * tilePx - camY)
            }
        }

        // Player (with a small bob while sliding between tiles).
        val bob = if (world.player.moving) -3f else 0f
        drawSprite(drawScope, playerSprite, pCenterX - tilePx / 2f - camX, pCenterY - tilePx / 2f - camY + bob)
    }

    private fun blit(ds: DrawScope, atlasIndex: Int, dstX: Float, dstY: Float) {
        val col = atlasIndex % columns
        val row = atlasIndex / columns
        ds.drawImage(
            image = tileset,
            srcOffset = IntOffset(col * srcTile, row * srcTile),
            srcSize = IntSize(srcTile, srcTile),
            dstOffset = IntOffset(dstX.toInt(), dstY.toInt()),
            dstSize = IntSize(tilePx.toInt(), tilePx.toInt()),
            filterQuality = FilterQuality.None
        )
    }

    private fun drawSprite(ds: DrawScope, img: ImageBitmap, dstX: Float, dstY: Float) {
        ds.drawImage(
            image = img,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(img.width, img.height),
            dstOffset = IntOffset(dstX.toInt(), dstY.toInt()),
            dstSize = IntSize(tilePx.toInt(), tilePx.toInt()),
            filterQuality = FilterQuality.None
        )
    }

    override fun onPointerMove(x: Float, y: Float) { /* movement is via D-pad/keys */ }
    override fun onPointerExit() { /* no-op */ }
}
