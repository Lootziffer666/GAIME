package game

import korlibs.image.bitmap.Bitmap
import korlibs.image.bitmap.BmpSlice
import korlibs.image.bitmap.slice
import korlibs.image.format.readBitmap
import korlibs.io.file.std.resourcesVfs
import korlibs.math.geom.RectangleInt
import rpg.tiled.Tileset

/**
 * Wraps a [Tileset] and its loaded [Bitmap], providing slicing for individual tiles.
 *
 * Each tile is addressed by its local tile ID (0-based, relative to the tileset).
 */
class TilesetAtlas(val tileset: Tileset, val bitmap: Bitmap) {

    private val tw: Int get() = tileset.tileWidth
    private val th: Int get() = tileset.tileHeight

    /**
     * Returns the bitmap slice for the given local tile ID within this tileset.
     */
    fun sliceFor(localTileId: Int): BmpSlice {
        val col = localTileId % tileset.columns
        val row = localTileId / tileset.columns
        return bitmap.slice(RectangleInt(col * tw, row * th, tw, th))
    }

    companion object {
        /**
         * Loads the tileset image from [resourcesVfs] and wraps it with slicing logic.
         *
         * @param tileset the parsed tileset metadata from TmxLoader
         * @param dirPath the resource directory containing the tileset image
         */
        suspend fun load(tileset: Tileset, dirPath: String): TilesetAtlas {
            val pngPath = "$dirPath/${tileset.imageSource}"
            val bitmap = resourcesVfs[pngPath].readBitmap()
            return TilesetAtlas(tileset, bitmap)
        }
    }
}

/**
 * Resolves a global tile ID (GID) to the owning [TilesetAtlas] and its local tile ID.
 *
 * Tilesets must be sorted by [Tileset.firstGid] (ascending) which is the default
 * order from TmxLoader.
 *
 * @return a Pair of (atlas, localTileId) or null if the GID cannot be resolved.
 */
fun List<TilesetAtlas>.resolveGid(gid: Int): Pair<TilesetAtlas, Int>? {
    val atlas = lastOrNull { it.tileset.firstGid <= gid } ?: return null
    val localId = gid - atlas.tileset.firstGid
    return Pair(atlas, localId)
}
