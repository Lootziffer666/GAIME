package game

import korlibs.image.bitmap.Bitmap
import korlibs.image.bitmap.Bitmap32
import korlibs.image.bitmap.BmpSlice
import korlibs.image.bitmap.slice
import korlibs.image.color.RGBA
import korlibs.image.format.readBitmap
import korlibs.io.file.std.resourcesVfs
import korlibs.math.geom.RectangleInt

/**
 * Loads CraftPix sprite sheets and slices them into individual frames.
 *
 * CraftPix convention: all frames horizontal, square (frameWidth = height),
 * frame count = bitmap.width / bitmap.height, no padding between frames.
 */
object SpriteLoader {

    /**
     * Load a sprite sheet from [assetPath] via resourcesVfs and slice into frames.
     * Falls back to a single procedural placeholder frame if loading fails.
     */
    suspend fun load(assetPath: String): List<BmpSlice> {
        return try {
            val bitmap = resourcesVfs[assetPath].readBitmap()
            sliceFrames(bitmap)
        } catch (_: Exception) {
            // Fallback: single procedural frame (compile-safe, no asset needed)
            listOf(buildFallbackBitmap().slice())
        }
    }

    /**
     * Slice a horizontally-arranged sprite sheet into square frames.
     */
    fun sliceFrames(bitmap: Bitmap): List<BmpSlice> {
        val frameSize = bitmap.height
        if (frameSize <= 0) return listOf(bitmap.slice())
        val frameCount = (bitmap.width / frameSize).coerceAtLeast(1)
        return List(frameCount) { i ->
            bitmap.slice(RectangleInt(i * frameSize, 0, frameSize, frameSize))
        }
    }

    internal fun buildFallbackBitmap(): Bitmap32 {
        val s = 32
        val bmp = Bitmap32(s, s, premultiplied = true)
        val color = RGBA(0x7e, 0x25, 0x53, 0xff)
        for (y in 4 until s - 4) for (x in 8 until s - 8) bmp[x, y] = color
        return bmp
    }
}
