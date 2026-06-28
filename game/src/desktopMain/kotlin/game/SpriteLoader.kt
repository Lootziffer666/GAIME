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
 * CraftPix convention (verified against the actual swordsman/vampire assets):
 * these sheets are GRIDS of square [DEFAULT_FRAME_SIZE]×[DEFAULT_FRAME_SIZE]
 * frames. The sheet height is N rows (the 4 rows are facing directions); each
 * row's columns are the animation frames for that direction. We use ROW 0 (the
 * front-facing animation): frame count = bitmap.width / frameSize.
 *
 * NOTE: the earlier assumption "frames are height-sized squares in a single row"
 * was wrong (an idle sheet is 768×256 = a 12×4 grid of 64px frames, not 3×256).
 * See KNOWN_BUGS B005.
 */
object SpriteLoader {

    /** Frame edge length for the CraftPix HD character sheets used in this game. */
    const val DEFAULT_FRAME_SIZE = 64

    /**
     * Load a sprite sheet from [assetPath] via resourcesVfs and slice into frames.
     * Falls back to a single procedural placeholder frame if loading fails.
     */
    suspend fun load(assetPath: String, frameSize: Int = DEFAULT_FRAME_SIZE): List<BmpSlice> {
        return try {
            val bitmap = resourcesVfs[assetPath].readBitmap()
            sliceFrames(bitmap, frameSize)
        } catch (_: Exception) {
            // Fallback: single procedural frame (compile-safe, no asset needed)
            listOf(buildFallbackBitmap().slice())
        }
    }

    /**
     * Slice ROW 0 of a grid sprite sheet into square [frameSize] frames.
     * If the bitmap is smaller than [frameSize] (e.g. the procedural fallback),
     * the whole bitmap height is used as the frame size so it yields one frame.
     */
    fun sliceFrames(bitmap: Bitmap, frameSize: Int = DEFAULT_FRAME_SIZE): List<BmpSlice> {
        if (bitmap.height <= 0 || bitmap.width <= 0) return listOf(bitmap.slice())
        val fs = if (frameSize <= 0 || frameSize > bitmap.height) bitmap.height else frameSize
        val frameCount = (bitmap.width / fs).coerceAtLeast(1)
        return List(frameCount) { i ->
            bitmap.slice(RectangleInt(i * fs, 0, fs, fs))
        }
    }

    internal fun buildFallbackBitmap(): Bitmap32 {
        val s = 32
        val bmp = Bitmap32(s, s, premultiplied = true)
        val color = RGBA(0x7e, 0x25, 0x53, 0xff)
        for (y in 4 until s - 4) for (x in 8 until s - 8) bmp[x, y] = color
        return bmp
    }

    /**
     * Slices ALL rows of a CraftPix grid sheet.
     * Returns List<row> where each row = List<BmpSlice> (left-to-right frames).
     * Row count = bitmap.height / frameSize (coerced to >= 1).
     */
    fun sliceAllRows(bitmap: Bitmap, frameSize: Int = DEFAULT_FRAME_SIZE): List<List<BmpSlice>> {
        if (bitmap.height <= 0 || bitmap.width <= 0) return listOf(listOf(bitmap.slice()))
        val fs = if (frameSize <= 0 || frameSize > bitmap.height) bitmap.height else frameSize
        val rowCount = (bitmap.height / fs).coerceAtLeast(1)
        val colCount = (bitmap.width / fs).coerceAtLeast(1)
        return List(rowCount) { row ->
            List(colCount) { col ->
                bitmap.slice(RectangleInt(col * fs, row * fs, fs, fs))
            }
        }
    }

    /**
     * Convenience: load a sheet and return all rows.
     * Fallback: single row with the procedural fallback frame.
     */
    suspend fun loadAllRows(assetPath: String, frameSize: Int = DEFAULT_FRAME_SIZE): List<List<BmpSlice>> {
        return try {
            val bitmap = resourcesVfs[assetPath].readBitmap()
            sliceAllRows(bitmap, frameSize)
        } catch (_: Exception) {
            listOf(listOf(buildFallbackBitmap().slice()))
        }
    }
}
