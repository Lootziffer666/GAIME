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
    private val tilePx: Float = RenderMetrics.SCREEN_TILE
) : Scene {

    override val name: String = "World"

    var onTrigger: ((String) -> Unit)? = null
    var onEntityInteraction: ((GridEntity) -> Unit)? = null
    var spriteMap: Map<String, ImageBitmap> = emptyMap()

    /** Set per-map for the right ambient glow: warm amber = tavern, cold blue = sewer, green = forest. */
    var glowColor: Color = Color(0xFFFFD98A)

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

        for (ty in y0..y1) {
            for (tx in x0..x1) {
                blit(drawScope, map.tileAt(tx, ty), tx * tilePx - camX, ty * tilePx - camY)
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

        drawHd2dOverlays(drawScope, vw, vh)
    }

    private fun drawHd2dOverlays(ds: DrawScope, w: Float, h: Float) {
        // Pulsing ambient glow — colour set by host per map.
        val pulse = 0.7f + 0.3f * sin(time * 1.8f)
        val glowCenter = Offset(w * 0.5f, h * 0.4f)
        ds.drawCircle(
            brush = Brush.radialGradient(
                0f to glowColor.copy(alpha = 0.28f * pulse),
                0.6f to glowColor.copy(alpha = 0.10f * pulse),
                1f to Color.Transparent,
                center = glowCenter,
                radius = w * 0.55f
            ),
            radius = w * 0.55f,
            center = glowCenter,
            blendMode = BlendMode.Screen
        )

        // Tilt-shift fake DoF: dark gradient bands at top and bottom.
        ds.drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF100A1E).copy(alpha = 0.60f),
                0.15f to Color.Transparent
            ),
            size = Size(w, h)
        )
        ds.drawRect(
            brush = Brush.verticalGradient(
                0.85f to Color.Transparent,
                1f to Color(0xFF100A1E).copy(alpha = 0.65f)
            ),
            size = Size(w, h)
        )

        // Vignette: darken edges.
        ds.drawRect(
            brush = Brush.radialGradient(
                0.45f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.70f),
                center = Offset(w / 2f, h / 2f),
                radius = maxOf(w, h) * 0.72f
            ),
            size = Size(w, h)
        )

        // Scanlines: horizontal lines every 3 px at low alpha.
        var y = 0f
        while (y < h) {
            ds.drawLine(
                color = Color.Black.copy(alpha = 0.09f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
            y += 3f
        }
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
