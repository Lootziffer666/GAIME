package game

import korlibs.image.bitmap.Bitmap32
import korlibs.image.bitmap.slice
import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.Image
import korlibs.korge.view.addUpdater
import korlibs.korge.view.image
import korlibs.time.milliseconds

/**
 * Player facing direction for sprite orientation.
 */
enum class Facing { UP, DOWN, LEFT, RIGHT }

/**
 * Manages the player sprite rendering with idle/walk animation states.
 *
 * Uses procedural placeholder bitmaps (colored rectangles) as a compile-time-safe
 * fallback. Real sprite sheet loading from resourcesVfs can be added later without
 * changing the public API.
 *
 * The sprite is placed into the given [container] and its position is derived from
 * [gridX]/[gridY] multiplied by tile dimensions.
 */
class PlayerSprite(container: Container, private val tileWidth: Int = 16, private val tileHeight: Int = 16) {

    var gridX: Int = 0
    var gridY: Int = 0
    var facing: Facing = Facing.DOWN

    private val spriteImage: Image

    // Procedural frame bitmaps for idle and walk states
    private val idleFrames: List<Bitmap32>
    private val walkFrames: List<Bitmap32>

    private var isWalking: Boolean = false
    private var frameIndex: Int = 0
    private var elapsedMs: Float = 0f
    private val frameDurationMs: Float = 150f

    init {
        // Build procedural placeholder frames
        idleFrames = buildIdleFrames()
        walkFrames = buildWalkFrames()

        spriteImage = container.image(idleFrames[0].slice()) {
            smoothing = false
        }

        // Animation updater
        container.addUpdater { dt ->
            val dtMs = dt.milliseconds.toFloat()
            elapsedMs += dtMs

            val frames = if (isWalking) walkFrames else idleFrames
            if (elapsedMs >= frameDurationMs) {
                elapsedMs -= frameDurationMs
                frameIndex = (frameIndex + 1) % frames.size
                spriteImage.bitmap = frames[frameIndex].slice()
            }

            // Update position from grid coordinates
            spriteImage.x = (gridX * tileWidth).toDouble()
            spriteImage.y = (gridY * tileHeight).toDouble()

            // Flip for facing direction
            when (facing) {
                Facing.LEFT -> spriteImage.scaleX = -1.0
                Facing.RIGHT -> spriteImage.scaleX = 1.0
                // TODO: UP/DOWN require separate sheets or rotation;
                // using default (facing right) for now
                Facing.UP -> spriteImage.scaleX = 1.0
                Facing.DOWN -> spriteImage.scaleX = 1.0
            }
        }
    }

    fun setIdle() {
        if (isWalking) {
            isWalking = false
            frameIndex = 0
            elapsedMs = 0f
        }
    }

    fun setWalking() {
        if (!isWalking) {
            isWalking = true
            frameIndex = 0
            elapsedMs = 0f
        }
    }

    // =========================================================================
    // Procedural placeholder bitmaps (compile without assets)
    // =========================================================================

    private fun buildIdleFrames(): List<Bitmap32> {
        // 4-frame idle "breathing" animation: slight color variation
        val baseColor = RGBA(0x7e, 0x25, 0x53, 0xff)
        val skinColor = RGBA(0xe0, 0xa8, 0x68, 0xff)
        return List(4) { frame ->
            buildHeroFrame(baseColor, skinColor, yOffset = if (frame % 2 == 0) 0 else 1)
        }
    }

    private fun buildWalkFrames(): List<Bitmap32> {
        // 6-frame walk animation: alternating leg positions via slight offsets
        val baseColor = RGBA(0x7e, 0x35, 0x63, 0xff)
        val skinColor = RGBA(0xe0, 0xa8, 0x68, 0xff)
        return List(6) { frame ->
            buildHeroFrame(baseColor, skinColor, yOffset = frame % 3)
        }
    }

    private fun buildHeroFrame(cloak: RGBA, skin: RGBA, yOffset: Int): Bitmap32 {
        val w = 16
        val h = 16
        val bmp = Bitmap32(w, h, premultiplied = true)
        val hair = RGBA(0x3a, 0x24, 0x17, 0xff)
        val boot = RGBA(0x2a, 0x1a, 0x0a, 0xff)

        // Head
        for (x in 5..10) {
            bmp[x, 2 + yOffset] = hair
            bmp[x, 3 + yOffset] = skin
            bmp[x, 4 + yOffset] = skin
        }
        // Eyes
        bmp[7, 3 + yOffset] = RGBA(0x1a, 0x1a, 0x1a, 0xff)
        bmp[9, 3 + yOffset] = RGBA(0x1a, 0x1a, 0x1a, 0xff)

        // Body (cloak)
        for (y in 5..10) for (x in 4..11) {
            bmp[x, y + yOffset] = cloak
        }

        // Legs/boots
        for (y in 11..13) {
            bmp[6, y + yOffset] = boot
            bmp[9, y + yOffset] = boot
        }

        return bmp
    }
}
