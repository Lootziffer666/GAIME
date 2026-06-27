package ui.rpg

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import engine.Scene
import rpg.render.RenderMetrics
import rpg.world.Camera
import rpg.world.GridEntity
import rpg.world.GridEntityType
import rpg.world.GridWorld
import kotlin.math.sin

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
    private val srcTile: Int = RenderMetrics.LEGACY_TILE,
    private val tilePx: Float = RenderMetrics.SCREEN_TILE,
    /**
     * Optional pre-rendered HD scene (baked from a Tiled map). When set, the
     * whole map is drawn by scaling this image to the world size instead of
     * blitting the [tileset] atlas per cell. Its native size must be
     * map.width*srcTile x map.height*srcTile.
     */
    private val background: ImageBitmap? = null,
    /**
     * On-screen scale for character/entity sprites relative to one tile. Sprites
     * are drawn larger than a tile and anchored bottom-centre so the party and
     * NPCs read clearly instead of looking tiny. 1f = exactly one tile.
     */
    private val characterScale: Float = 1.5f
) : Scene {

    override val name: String = "World"

    var onTrigger: ((String) -> Unit)? = null
    var onEntityInteraction: ((GridEntity) -> Unit)? = null
    var spriteMap: Map<String, ImageBitmap> = emptyMap()

    /** Cinematic atmosphere preset (lighting, motes, grade, fog) — set per map by the host. */
    var atmosphere: SceneAtmosphere = SceneAtmosphere.TAVERN

    private val camera = Camera()
    private val columns = (tileset.width / srcTile).coerceAtLeast(1)
    private var time = 0f

    override fun update(deltaTime: Float) {
        time += deltaTime.coerceAtMost(0.05f)
        world.update(deltaTime)
        val triggered = world.consumeTriggers()
        if (triggered.isNotEmpty()) {
            val cb = onTrigger
            if (cb != null) triggered.forEach(cb)
        }
        val interactions = world.consumeEntityInteractions()
        if (interactions.isNotEmpty()) {
            val cb = onEntityInteraction
            if (cb != null) interactions.forEach(cb)
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

        val bg = background
        if (bg != null) {
            // Baked HD scene: scale the whole image to world size (nearest-neighbour
            // keeps the pixel art crisp), offset by the camera.
            drawImageScaled(drawScope, bg, -camX, -camY, worldW, worldH)
        } else {
            for (ty in y0..y1) {
                for (tx in x0..x1) {
                    blit(drawScope, map.tileAt(tx, ty), tx * tilePx - camX, ty * tilePx - camY)
                }
            }
        }

        // Entities (NPCs, enemies).
        for (entity in world.entities) {
            val sprite = spriteMap[entity.sprite] ?: continue
            val ex = entity.tileX * tilePx - camX
            val ey = entity.tileY * tilePx - camY
            drawSprite(drawScope, sprite, ex, ey)
        }

        // Player (with a small bob while sliding between tiles).
        val bob = if (world.player.moving) -3f else 0f
        drawSprite(drawScope, playerSprite, pCenterX - tilePx / 2f - camX, pCenterY - tilePx / 2f - camY + bob)

        drawScope.drawAtmosphere(atmosphere, time, vw, vh)
    }

    private fun drawImageScaled(ds: DrawScope, img: ImageBitmap, dstX: Float, dstY: Float, dstW: Float, dstH: Float) {
        ds.drawImage(
            image = img,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(img.width, img.height),
            dstOffset = IntOffset(dstX.toInt(), dstY.toInt()),
            dstSize = IntSize(dstW.toInt(), dstH.toInt()),
            filterQuality = FilterQuality.None
        )
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

    private fun drawSprite(ds: DrawScope, img: ImageBitmap, dstX: Float, dstY: Float, scale: Float = characterScale) {
        // (dstX, dstY) is the top-left of the 1-tile cell box. Draw the sprite at
        // `scale` times tile size, centred horizontally and anchored to the cell's
        // bottom edge so larger characters keep their feet on the tile.
        val size = tilePx * scale
        val dx = dstX + (tilePx - size) / 2f
        val dy = dstY + (tilePx - size)
        ds.drawImage(
            image = img,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(img.width, img.height),
            dstOffset = IntOffset(dx.toInt(), dy.toInt()),
            dstSize = IntSize(size.toInt(), size.toInt()),
            filterQuality = FilterQuality.None
        )
    }

    override fun onPointerMove(x: Float, y: Float) { /* movement is via D-pad/keys */ }
    override fun onPointerExit() { /* no-op */ }
}
