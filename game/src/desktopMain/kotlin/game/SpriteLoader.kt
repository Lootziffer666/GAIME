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
 * frames. The sheet has N rows (4 rows = facing directions); each row's columns
 * are the animation frames for that direction.
 *
 * Row layout (CraftPix HD characters — verified via B005 screenshots):
 *   Row 0 = DOWN (front-facing)
 *   Row 1 = UP (back-facing)
 *   Row 2 = RIGHT
 *   Row 3 = LEFT (or: same as RIGHT, mirrored — some sheets only have 3 rows)
 *
 * See KNOWN_BUGS B005.
 */
object SpriteLoader {

    /** Frame edge length for the CraftPix HD character sheets used in this game. */
    const val DEFAULT_FRAME_SIZE = 64

    /** Row indices for directional sheets. */
    const val ROW_DOWN = 0
    const val ROW_UP = 1
    const val ROW_RIGHT = 2
    const val ROW_LEFT = 3

    /**
     * Load a sprite sheet from [assetPath] via resourcesVfs and slice ROW 0.
     * Falls back to a single procedural placeholder frame if loading fails.
     */
    suspend fun load(assetPath: String, frameSize: Int = DEFAULT_FRAME_SIZE): List<BmpSlice> {
        return try {
            val bitmap = resourcesVfs[assetPath].readBitmap()
            sliceFrames(bitmap, frameSize)
        } catch (_: Exception) {
            listOf(buildFallbackBitmap().slice())
        }
    }

    /**
     * Load a sprite sheet and return ALL rows as a Map<rowIndex, List<BmpSlice>>.
     * Used for directional sprites. Falls back to a single-row map on error.
     */
    suspend fun loadAllRows(assetPath: String, frameSize: Int = DEFAULT_FRAME_SIZE): Map<Int, List<BmpSlice>> {
        return try {
            val bitmap = resourcesVfs[assetPath].readBitmap()
            sliceAllRows(bitmap, frameSize)
        } catch (_: Exception) {
            val fallback = listOf(buildFallbackBitmap().slice())
            mapOf(0 to fallback, 1 to fallback, 2 to fallback, 3 to fallback)
        }
    }

    /**
     * Slice ROW 0 of a grid sprite sheet into square [frameSize] frames.
     * If the bitmap is smaller than [frameSize], the whole bitmap height is used.
     */
    fun sliceFrames(bitmap: Bitmap, frameSize: Int = DEFAULT_FRAME_SIZE): List<BmpSlice> {
        if (bitmap.height <= 0 || bitmap.width <= 0) return listOf(bitmap.slice())
        val fs = if (frameSize <= 0 || frameSize > bitmap.height) bitmap.height else frameSize
        val frameCount = (bitmap.width / fs).coerceAtLeast(1)
        return List(frameCount) { i ->
            bitmap.slice(RectangleInt(i * fs, 0, fs, fs))
        }
    }

    /**
     * Slice ALL rows of a grid sprite sheet. Returns Map<rowIndex, List<BmpSlice>>.
     * Rows that don't fit in the bitmap height are skipped.
     */
    fun sliceAllRows(bitmap: Bitmap, frameSize: Int = DEFAULT_FRAME_SIZE): Map<Int, List<BmpSlice>> {
        if (bitmap.height <= 0 || bitmap.width <= 0) return mapOf(0 to listOf(bitmap.slice()))
        val fs = if (frameSize <= 0 || frameSize > bitmap.height) bitmap.height else frameSize
        val rowCount = bitmap.height / fs
        val colCount = (bitmap.width / fs).coerceAtLeast(1)
        val result = mutableMapOf<Int, List<BmpSlice>>()
        for (row in 0 until rowCount) {
            result[row] = List(colCount) { col ->
                bitmap.slice(RectangleInt(col * fs, row * fs, fs, fs))
            }
        }
        return result
    }

    internal fun buildFallbackBitmap(): Bitmap32 {
        val s = 32
        val bmp = Bitmap32(s, s, premultiplied = true)
        val color = RGBA(0x7e, 0x25, 0x53, 0xff)
        for (y in 4 until s - 4) for (x in 8 until s - 8) bmp[x, y] = color
        return bmp
    }
}
