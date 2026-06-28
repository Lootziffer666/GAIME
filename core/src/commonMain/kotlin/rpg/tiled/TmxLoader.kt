package rpg.tiled

/**
 * Minimal state-machine TMX parser. Handles the subset used by the CraftPix
 * packs in assets/HD/locations/ — Tiled 1.10, encoding="csv", infinite or finite,
 * multi-tileset, flip bits, animated tiles. No external XML library needed.
 *
 * Usage:
 * ```kotlin
 * val map: TiledMap = TmxLoader.parse(tmxFileContentAsString)
 * ```
 */
object TmxLoader {

    // GID flip-bit constants (Tiled spec)
    private const val FLIP_H = 0x80000000.toInt()
    private const val FLIP_V = 0x40000000.toInt()
    private const val FLIP_D = 0x20000000.toInt()
    private const val GID_MASK = 0x1FFFFFFF

    fun parse(tmxContent: String): TiledMap {
        var mapTileWidth = 0
        var mapTileHeight = 0

        val tilesets = mutableListOf<MutableTileset>()
        val layers = mutableListOf<TileLayer>()

        // State
        var currentTileset: MutableTileset? = null
        var currentAnimTileId: Int? = null
        var currentAnimFrames: MutableList<AnimationFrame>? = null
        var currentLayerName: String? = null
        var currentLayerWidth: Int = 0
        var inData = false
        var dataEncoding: String? = null
        var inChunk = false
        var chunkX = 0
        var chunkY = 0
        var chunkWidth = 0
        var csvAccumulator = StringBuilder()
        val currentCells = mutableListOf<TileCell>()

        for (rawLine in tmxContent.lineSequence()) {
            val line = rawLine.trim()

            // --- <map> ---
            if (line.startsWith("<map ")) {
                mapTileWidth = line.attr("tilewidth")?.toIntOrNull() ?: 0
                mapTileHeight = line.attr("tileheight")?.toIntOrNull() ?: 0
                continue
            }

            // --- <tileset> ---
            if (line.startsWith("<tileset ") && !line.startsWith("<tileset source=")) {
                val ts = MutableTileset(
                    firstGid = line.attr("firstgid")?.toIntOrNull() ?: 1,
                    name = line.attr("name") ?: "",
                    tileWidth = line.attr("tilewidth")?.toIntOrNull() ?: mapTileWidth,
                    tileHeight = line.attr("tileheight")?.toIntOrNull() ?: mapTileHeight,
                    columns = line.attr("columns")?.toIntOrNull() ?: 1,
                    tileCount = line.attr("tilecount")?.toIntOrNull() ?: 0,
                )
                currentTileset = ts
                tilesets.add(ts)
                continue
            }

            // --- <image source="..."/> inside tileset ---
            if (line.startsWith("<image ") && currentTileset != null) {
                currentTileset!!.imageSource = line.attr("source") ?: ""
                continue
            }

            // --- <tile id="..."> inside tileset (animated tile) ---
            if (line.startsWith("<tile ") && currentTileset != null && !line.contains("gid=")) {
                currentAnimTileId = line.attr("id")?.toIntOrNull()
                continue
            }

            // --- <animation> ---
            if (line.startsWith("<animation") && currentAnimTileId != null) {
                currentAnimFrames = mutableListOf()
                continue
            }

            // --- <frame tileid="..." duration="..."/> ---
            if (line.startsWith("<frame ") && currentAnimFrames != null) {
                val tileId = line.attr("tileid")?.toIntOrNull() ?: 0
                val dur = line.attr("duration")?.toIntOrNull() ?: 0
                currentAnimFrames!!.add(AnimationFrame(tileId, dur))
                continue
            }

            // --- </animation> ---
            if (line.startsWith("</animation>") && currentAnimFrames != null && currentAnimTileId != null) {
                currentTileset?.animatedTiles?.put(currentAnimTileId!!, currentAnimFrames!!.toList())
                currentAnimFrames = null
                continue
            }

            // --- </tile> (end of animated tile block) ---
            if (line == "</tile>" && currentAnimTileId != null) {
                currentAnimTileId = null
                continue
            }

            // --- </tileset> ---
            if (line.startsWith("</tileset>")) {
                currentTileset = null
                continue
            }

            // --- <layer> ---
            if (line.startsWith("<layer ")) {
                currentLayerName = line.attr("name") ?: ""
                currentLayerWidth = line.attr("width")?.toIntOrNull() ?: 0
                currentCells.clear()
                continue
            }

            // --- <data encoding="csv"> ---
            if (line.startsWith("<data ") || line.startsWith("<data>")) {
                dataEncoding = line.attr("encoding")
                if (dataEncoding != null && dataEncoding != "csv") {
                    error("Only csv encoding is supported, got: $dataEncoding")
                }
                inData = true
                csvAccumulator.clear()
                // Handle inline CSV on same line as <data ...>CSV or <data ...>CSV</data>
                val afterTag = line.substringAfter(">", "")
                val csvPart = if (afterTag.contains("</data>")) afterTag.substringBefore("</data>") else afterTag
                if (csvPart.isNotBlank()) csvAccumulator.append(csvPart)
                if (afterTag.contains("</data>")) {
                    // Self-closing: <data ...>CSV</data> all on one line
                    parseCsvCells(csvAccumulator.toString(), 0, 0, currentLayerWidth, currentCells)
                    csvAccumulator.clear()
                    inData = false
                }
                continue
            }

            // --- <chunk x="..." y="..." width="..." height="..."> ---
            if ((line.startsWith("<chunk ") || line.startsWith("<chunk>")) && inData) {
                inChunk = true
                chunkX = line.attr("x")?.toIntOrNull() ?: 0
                chunkY = line.attr("y")?.toIntOrNull() ?: 0
                chunkWidth = line.attr("width")?.toIntOrNull() ?: 16
                csvAccumulator.clear()
                // Handle inline CSV: <chunk ...>CSV or <chunk ...>CSV</chunk>
                val afterTag = line.substringAfter(">", "")
                val csvPart = if (afterTag.contains("</chunk>")) afterTag.substringBefore("</chunk>") else afterTag
                if (csvPart.isNotBlank()) csvAccumulator.append(csvPart)
                if (afterTag.contains("</chunk>")) {
                    parseCsvCells(csvAccumulator.toString(), chunkX, chunkY, chunkWidth, currentCells)
                    csvAccumulator.clear()
                    inChunk = false
                }
                continue
            }

            // --- </chunk> ---
            if (line.contains("</chunk>") && inChunk) {
                // CSV may precede </chunk> on same line
                val csvBefore = line.substringBefore("</chunk>")
                if (csvBefore.isNotBlank()) csvAccumulator.append(csvBefore)
                parseCsvCells(csvAccumulator.toString(), chunkX, chunkY, chunkWidth, currentCells)
                csvAccumulator.clear()
                inChunk = false
                continue
            }

            // --- </data> ---
            if (line.contains("</data>")) {
                // CSV may precede </data> on same line
                val csvBefore = line.substringBefore("</data>")
                if (csvBefore.isNotBlank()) csvAccumulator.append(csvBefore)
                if (!inChunk && csvAccumulator.isNotEmpty()) {
                    parseCsvCells(csvAccumulator.toString(), 0, 0, currentLayerWidth, currentCells)
                }
                csvAccumulator.clear()
                inData = false
                inChunk = false
                continue
            }

            // --- </layer> ---
            if (line.startsWith("</layer>")) {
                if (currentLayerName != null) {
                    layers.add(TileLayer(currentLayerName!!, currentCells.toList()))
                }
                currentLayerName = null
                currentCells.clear()
                continue
            }

            // --- CSV text lines (inside <data> or <chunk>) ---
            if (inData) {
                csvAccumulator.append(line)
                continue
            }
        }

        return TiledMap(
            tileWidth = mapTileWidth,
            tileHeight = mapTileHeight,
            tilesets = tilesets.map { it.toTileset() }.sortedBy { it.firstGid },
            layers = layers,
        )
    }

    private fun parseCsvCells(
        csv: String,
        originX: Int,
        originY: Int,
        width: Int,
        out: MutableList<TileCell>,
    ) {
        if (width <= 0) return
        val values = csv.split(',')
        var index = 0
        for (v in values) {
            val trimmed = v.trim()
            if (trimmed.isEmpty()) continue
            val raw = trimmed.toLongAndMask()
            if (raw != 0L) {
                val rawInt = raw.toInt()
                val flipH = (rawInt and FLIP_H) != 0
                val flipV = (rawInt and FLIP_V) != 0
                val flipD = (rawInt and FLIP_D) != 0
                val gid = (rawInt and GID_MASK)
                out.add(
                    TileCell(
                        gridX = originX + (index % width),
                        gridY = originY + (index / width),
                        gid = gid,
                        flipH = flipH,
                        flipV = flipV,
                        flipD = flipD,
                    )
                )
            }
            index++
        }
    }

    /**
     * Parse a GID value that may exceed Int.MAX_VALUE when flip bits are set.
     * Tiled encodes flip bits in the upper 3 bits of a 32-bit unsigned int.
     * Kotlin's toLong() handles the full range; we then mask back to Int bits.
     */
    private fun String.toLongAndMask(): Long {
        val l = this.trim().toLong()
        return l and 0xFFFFFFFFL
    }

    private fun String.attr(name: String): String? {
        val key = "$name=\""
        val start = this.indexOf(key)
        if (start < 0) return null
        val valStart = start + key.length
        val end = this.indexOf('"', valStart)
        if (end < 0) return null
        return this.substring(valStart, end)
    }

    private class MutableTileset(
        val firstGid: Int,
        val name: String,
        val tileWidth: Int,
        val tileHeight: Int,
        val columns: Int,
        val tileCount: Int,
        var imageSource: String = "",
        val animatedTiles: MutableMap<Int, List<AnimationFrame>> = mutableMapOf(),
    ) {
        fun toTileset() = Tileset(
            firstGid = firstGid,
            name = name,
            tileWidth = tileWidth,
            tileHeight = tileHeight,
            columns = columns,
            tileCount = tileCount,
            imageSource = imageSource,
            animatedTiles = animatedTiles.toMap(),
        )
    }
}
