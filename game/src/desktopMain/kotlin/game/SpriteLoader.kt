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
 * Step 17: Now reads .sheet.json descriptors (from the sheet-normalizer tool)
 * when available. The descriptor provides frameW, frameH, cols, rows, and
 * foot anchor — derived from actual opaque pixel bounds, not assumptions.
 * DEFAULT_FRAME_SIZE=64 is only a FALLBACK for sheets without a descriptor.
 *
 * See KNOWN_BUGS B005.
 */
object SpriteLoader {

    /** Frame edge length fallback (only used when no .sheet.json exists). */
    const val DEFAULT_FRAME_SIZE = 64

    /**
     * Descriptor from a .sheet.json file (output of tools/sheet-normalizer).
     * Provides the actual frame grid dimensions + body measurements.
     */
    data class SheetDescriptor(
        val frameW: Int,
        val frameH: Int,
        val cols: Int,
        val rows: Int,
        val footAnchorX: Int,
        val footAnchorY: Int,
        val opaqueBodyH: Int,
        val source: String = "",
    )

    /**
     * Try to load the .sheet.json descriptor for a given sheet path.
     * Convention: for "Foo.png", look for "Foo.sheet.json" in the same directory.
     */
    suspend fun loadDescriptor(assetPath: String): SheetDescriptor? {
        val jsonPath = assetPath.removeSuffix(".png") + ".sheet.json"
        return try {
            val content = resourcesVfs[jsonPath].readString()
            parseDescriptor(content)
        } catch (_: Exception) {
            null
        }
    }

    /** Simple JSON parser for SheetDescriptor (no external serialization dependency). */
    internal fun parseDescriptor(json: String): SheetDescriptor? {
        return try {
            fun extractInt(key: String): Int {
                val pattern = Regex(""""$key"\s*:\s*(\d+)""")
                return pattern.find(json)?.groupValues?.get(1)?.toInt() ?: 0
            }
            fun extractString(key: String): String {
                val pattern = Regex(""""$key"\s*:\s*"([^"]*)"?""")
                return pattern.find(json)?.groupValues?.get(1) ?: ""
            }
            SheetDescriptor(
                frameW = extractInt("frameW"),
                frameH = extractInt("frameH"),
                cols = extractInt("cols"),
                rows = extractInt("rows"),
                footAnchorX = extractInt("footAnchorX"),
                footAnchorY = extractInt("footAnchorY"),
                opaqueBodyH = extractInt("opaqueBodyH"),
                source = extractString("source"),
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Load a sprite sheet from [assetPath] via resourcesVfs and slice into frames.
     * If a .sheet.json descriptor exists, uses its frameW/frameH instead of the default.
     * Falls back to a single procedural placeholder frame if loading fails.
     */
    suspend fun load(assetPath: String, frameSize: Int = DEFAULT_FRAME_SIZE): List<BmpSlice> {
        return try {
            val bitmap = resourcesVfs[assetPath].readBitmap()
            val descriptor = loadDescriptor(assetPath)
            val fs = descriptor?.frameW ?: frameSize
            sliceFrames(bitmap, fs)
        } catch (_: Exception) {
            // Fallback: single procedural frame (compile-safe, no asset needed)
            listOf(buildFallbackBitmap().slice())
        }
    }

    /**
     * Slice ROW 0 of a grid sprite sheet into [frameW]×[frameH] frames.
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

    /**
     * Slice ROW 0 using non-square frames (frameW × frameH from descriptor).
     */
    fun sliceFramesRect(bitmap: Bitmap, frameW: Int, frameH: Int): List<BmpSlice> {
        if (bitmap.height <= 0 || bitmap.width <= 0) return listOf(bitmap.slice())
        val fw = if (frameW <= 0 || frameW > bitmap.width) bitmap.width else frameW
        val fh = if (frameH <= 0 || frameH > bitmap.height) bitmap.height else frameH
        val frameCount = (bitmap.width / fw).coerceAtLeast(1)
        return List(frameCount) { i ->
            bitmap.slice(RectangleInt(i * fw, 0, fw, fh))
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
     * Slices ALL rows with non-square frames (frameW × frameH from descriptor).
     */
    fun sliceAllRowsRect(bitmap: Bitmap, frameW: Int, frameH: Int): List<List<BmpSlice>> {
        if (bitmap.height <= 0 || bitmap.width <= 0) return listOf(listOf(bitmap.slice()))
        val fw = if (frameW <= 0 || frameW > bitmap.width) bitmap.width else frameW
        val fh = if (frameH <= 0 || frameH > bitmap.height) bitmap.height else frameH
        val rowCount = (bitmap.height / fh).coerceAtLeast(1)
        val colCount = (bitmap.width / fw).coerceAtLeast(1)
        return List(rowCount) { row ->
            List(colCount) { col ->
                bitmap.slice(RectangleInt(col * fw, row * fh, fw, fh))
            }
        }
    }

    /**
     * Convenience: load a sheet and return all rows.
     * Uses .sheet.json descriptor if available (non-square frames).
     * Fallback: single row with the procedural fallback frame.
     */
    suspend fun loadAllRows(assetPath: String, frameSize: Int = DEFAULT_FRAME_SIZE): List<List<BmpSlice>> {
        return try {
            val descriptor = loadDescriptor(assetPath)
            if (descriptor != null) {
                // The descriptor describes the NORMALIZED sheet — slice THAT, not the original
                // (slicing the original 64px-frame sheet at the normalized frameW yields tiny slivers).
                val bitmap = resourcesVfs[normalizedPath(assetPath)].readBitmap()
                sliceAllRowsRect(bitmap, descriptor.frameW, descriptor.frameH)
            } else {
                sliceAllRows(resourcesVfs[assetPath].readBitmap(), frameSize)
            }
        } catch (_: Exception) {
            listOf(listOf(buildFallbackBitmap().slice()))
        }
    }

    /**
     * Load a sheet + its descriptor together. Returns pair of (rows, descriptor?).
     * Used by CharacterSprite to get both frames AND body metrics in one call.
     */
    suspend fun loadWithDescriptor(assetPath: String): Pair<List<List<BmpSlice>>, SheetDescriptor?> {
        return try {
            val descriptor = loadDescriptor(assetPath)
            val rows = if (descriptor != null) {
                // Descriptor describes the NORMALIZED sheet — load + slice THAT, not the original.
                val bitmap = resourcesVfs[normalizedPath(assetPath)].readBitmap()
                sliceAllRowsRect(bitmap, descriptor.frameW, descriptor.frameH)
            } else {
                sliceAllRows(resourcesVfs[assetPath].readBitmap(), DEFAULT_FRAME_SIZE)
            }
            rows to descriptor
        } catch (_: Exception) {
            listOf(listOf(buildFallbackBitmap().slice())) to null
        }
    }

    /** Path to the normalized sheet that a descriptor describes: "Foo.png" → "Foo.normalized.png". */
    private fun normalizedPath(assetPath: String): String =
        assetPath.removeSuffix(".png") + ".normalized.png"
}
