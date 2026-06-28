package rpg.tiled

/**
 * Pure data model for a parsed TMX map (Tiled 1.10, CSV encoding, infinite or finite).
 * No external dependencies — Kotlin Stdlib only.
 */

data class TiledMap(
    val tileWidth: Int,
    val tileHeight: Int,
    val tilesets: List<Tileset>,
    val layers: List<TileLayer>,
)

data class Tileset(
    val firstGid: Int,
    val name: String,
    val tileWidth: Int,
    val tileHeight: Int,
    val columns: Int,
    val tileCount: Int,
    val imageSource: String,
    val animatedTiles: Map<Int, List<AnimationFrame>>,
)

data class AnimationFrame(
    val tileId: Int,
    val durationMs: Int,
)

data class TileLayer(
    val name: String,
    val cells: List<TileCell>,
)

data class TileCell(
    val gridX: Int,
    val gridY: Int,
    val gid: Int,
    val flipH: Boolean,
    val flipV: Boolean,
    val flipD: Boolean,
)
