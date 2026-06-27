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
    private val background: ImageBitmap? = null
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

        // Entities (NPCs, enemies, destructibles).
        for (entity in world.entities) {
            val ex = entity.tileX * tilePx - camX
            val ey = entity.tileY * tilePx - camY
            if (entity.type == GridEntityType.DESTRUCTIBLE) {
                drawDestructible(drawScope, entity, ex, ey)
            } else {
                val sprite = spriteMap[entity.sprite] ?: continue
                drawSprite(drawScope, sprite, ex, ey)
            }
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

    /** Renders a destructible entity using Canvas primitives (no external sprite needed). */
    private fun drawDestructible(ds: DrawScope, entity: GridEntity, dstX: Float, dstY: Float) {
        val w = tilePx
        val h = tilePx
        val damaged = entity.maxHp > 1 && entity.hp < entity.maxHp
        when (entity.sprite) {
            "grass_tall" -> {
                ds.drawRect(Color(0x884A8A30), topLeft = Offset(dstX, dstY), size = Size(w, h))
                val leans = floatArrayOf(0.08f, -0.06f, 0.07f, -0.05f)
                val bxOffsets = floatArrayOf(0.18f, 0.36f, 0.54f, 0.72f)
                for (i in 0..3) {
                    ds.drawLine(
                        color = Color(0xFF6CBF40),
                        start = Offset(dstX + w * bxOffsets[i], dstY + h * 0.90f),
                        end   = Offset(dstX + w * (bxOffsets[i] + leans[i]), dstY + h * 0.30f),
                        strokeWidth = 2.5f
                    )
                }
            }
            "crate" -> {
                ds.drawRect(Color(0xFF9B6B35), topLeft = Offset(dstX + 2f, dstY + 2f), size = Size(w - 4f, h - 4f))
                // border
                ds.drawLine(Color(0xFF6B4520), Offset(dstX + 2f, dstY + 2f), Offset(dstX + w - 2f, dstY + 2f), 2f)
                ds.drawLine(Color(0xFF6B4520), Offset(dstX + 2f, dstY + h - 2f), Offset(dstX + w - 2f, dstY + h - 2f), 2f)
                ds.drawLine(Color(0xFF6B4520), Offset(dstX + 2f, dstY + 2f), Offset(dstX + 2f, dstY + h - 2f), 2f)
                ds.drawLine(Color(0xFF6B4520), Offset(dstX + w - 2f, dstY + 2f), Offset(dstX + w - 2f, dstY + h - 2f), 2f)
                // X mark
                ds.drawLine(Color(0xFF5A3010), Offset(dstX + 7f, dstY + 7f), Offset(dstX + w - 7f, dstY + h - 7f), 2f)
                ds.drawLine(Color(0xFF5A3010), Offset(dstX + w - 7f, dstY + 7f), Offset(dstX + 7f, dstY + h - 7f), 2f)
                if (damaged) ds.drawRect(Color(0x44FF3030L), topLeft = Offset(dstX, dstY), size = Size(w, h))
            }
            "barrel" -> {
                ds.drawRect(Color(0xFF6B4222), topLeft = Offset(dstX + 3f, dstY + 2f), size = Size(w - 6f, h - 4f))
                // top cap
                ds.drawRect(Color(0xFF8B5A2B), topLeft = Offset(dstX + 2f, dstY + 2f), size = Size(w - 4f, 4f))
                // hoops
                for (i in 1..3) {
                    val hy = dstY + h * (i / 4f)
                    ds.drawLine(Color(0xFF3A2010), Offset(dstX + 3f, hy), Offset(dstX + w - 3f, hy), 2.5f)
                }
                if (damaged) ds.drawRect(Color(0x44FF3030L), topLeft = Offset(dstX, dstY), size = Size(w, h))
            }
            "wall_cracked" -> {
                val baseColor = if (damaged) Color(0xFF957070) else Color(0xFF909090)
                ds.drawRect(baseColor, topLeft = Offset(dstX, dstY), size = Size(w, h))
                // stone block seams
                ds.drawLine(Color(0xFF707070), Offset(dstX, dstY + h * 0.5f), Offset(dstX + w, dstY + h * 0.5f), 1f)
                ds.drawLine(Color(0xFF707070), Offset(dstX + w * 0.5f, dstY), Offset(dstX + w * 0.5f, dstY + h * 0.5f), 1f)
                ds.drawLine(Color(0xFF707070), Offset(dstX + w * 0.25f, dstY + h * 0.5f), Offset(dstX + w * 0.25f, dstY + h), 1f)
                ds.drawLine(Color(0xFF707070), Offset(dstX + w * 0.75f, dstY + h * 0.5f), Offset(dstX + w * 0.75f, dstY + h), 1f)
                // crack
                val crackW = if (damaged) 2.5f else 1.5f
                val crackColor = if (damaged) Color(0xFF602020) else Color(0xFF404040)
                ds.drawLine(crackColor, Offset(dstX + w * 0.55f, dstY + 2f), Offset(dstX + w * 0.43f, dstY + h * 0.38f), crackW)
                ds.drawLine(crackColor, Offset(dstX + w * 0.43f, dstY + h * 0.38f), Offset(dstX + w * 0.62f, dstY + h * 0.65f), crackW)
                ds.drawLine(crackColor, Offset(dstX + w * 0.62f, dstY + h * 0.65f), Offset(dstX + w * 0.50f, dstY + h - 2f), crackW)
                if (damaged) ds.drawRect(Color(0x22FF2020L), topLeft = Offset(dstX, dstY), size = Size(w, h))
            }
            else -> ds.drawRect(Color(0xFFFFCC00), topLeft = Offset(dstX, dstY), size = Size(w, h), alpha = 0.5f)
        }
    }

    override fun onPointerMove(x: Float, y: Float) { /* movement is via D-pad/keys */ }
    override fun onPointerExit() { /* no-op */ }
}
