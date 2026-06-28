package rpg.tiled

import kotlin.test.Test
import kotlin.test.assertEquals

class CollisionGridTest {

    private fun mapWith(vararg layerDefs: Pair<String, List<TileCell>>): TiledMap {
        return TiledMap(
            tileWidth = 16,
            tileHeight = 16,
            tilesets = listOf(
                Tileset(1, "T", 16, 16, 10, 100, "t.png", emptyMap())
            ),
            layers = layerDefs.map { (name, cells) -> TileLayer(name, cells) },
        )
    }

    private fun cell(x: Int, y: Int, gid: Int = 1) =
        TileCell(x, y, gid, flipH = false, flipV = false, flipD = false)

    @Test
    fun `floor layer makes cells walkable`() {
        val map = mapWith(
            "Floor" to listOf(cell(0, 0), cell(1, 0), cell(0, 1), cell(1, 1))
        )
        val grid = CollisionGrid.from(map)
        assertEquals(2, grid.cols)
        assertEquals(2, grid.rows)
        assertEquals(TileType.WALKABLE, grid[0, 0])
        assertEquals(TileType.WALKABLE, grid[1, 0])
        assertEquals(TileType.WALKABLE, grid[0, 1])
        assertEquals(TileType.WALKABLE, grid[1, 1])
    }

    @Test
    fun `solid layer overrides floor with BLOCKED`() {
        val map = mapWith(
            "Floor" to listOf(cell(0, 0), cell(1, 0)),
            "Walls" to listOf(cell(1, 0))
        )
        val grid = CollisionGrid.from(map)
        assertEquals(TileType.WALKABLE, grid[0, 0])
        assertEquals(TileType.BLOCKED, grid[1, 0])
    }

    @Test
    fun `water layer sets WATER`() {
        val map = mapWith(
            "Floor" to listOf(cell(0, 0), cell(1, 0)),
            "Water" to listOf(cell(1, 0))
        )
        val grid = CollisionGrid.from(map)
        assertEquals(TileType.WALKABLE, grid[0, 0])
        assertEquals(TileType.WATER, grid[1, 0])
    }

    @Test
    fun `trigger layer sets TRIGGER`() {
        val map = mapWith(
            "Floor" to listOf(cell(0, 0), cell(1, 0)),
            "door_main" to listOf(cell(1, 0))
        )
        val grid = CollisionGrid.from(map)
        assertEquals(TileType.WALKABLE, grid[0, 0])
        assertEquals(TileType.TRIGGER, grid[1, 0])
    }

    @Test
    fun `cells without floor are BLOCKED by default`() {
        // Only Floor at (0,0), nothing at (1,0)
        val map = mapWith(
            "Floor" to listOf(cell(0, 0)),
            "Objects1" to listOf(cell(1, 0)) // decorative → no collision
        )
        val grid = CollisionGrid.from(map)
        assertEquals(TileType.WALKABLE, grid[0, 0])
        assertEquals(TileType.BLOCKED, grid[1, 0]) // no floor → blocked
    }

    @Test
    fun `solid-only map without floor - all BLOCKED`() {
        val map = mapWith(
            "Walls" to listOf(cell(0, 0), cell(1, 0))
        )
        val grid = CollisionGrid.from(map)
        assertEquals(TileType.BLOCKED, grid[0, 0])
        assertEquals(TileType.BLOCKED, grid[1, 0])
    }

    @Test
    fun `negative coordinates normalize correctly`() {
        val map = mapWith(
            "Floor" to listOf(cell(-16, -16), cell(-15, -16))
        )
        val grid = CollisionGrid.from(map)
        assertEquals(2, grid.cols)
        assertEquals(1, grid.rows)
        assertEquals(-16, grid.offsetX)
        assertEquals(-16, grid.offsetY)
        assertEquals(TileType.WALKABLE, grid[0, 0]) // normalized from (-16,-16)
        assertEquals(TileType.WALKABLE, grid[1, 0]) // normalized from (-15,-16)
    }
}
