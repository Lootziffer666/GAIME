package rpg.tiled

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TmxLoaderTest {

    @Test
    fun `minimal finite map - one layer four cells`() {
        val tmx = """
            <?xml version="1.0"?>
            <map tilewidth="16" tileheight="16" width="2" height="2" infinite="0">
              <tileset firstgid="1" name="Base" tilewidth="16" tileheight="16" tilecount="4" columns="2">
                <image source="base.png" width="32" height="32"/>
              </tileset>
              <layer name="Floor" width="2" height="2">
                <data encoding="csv">1,2,
            3,4</data>
              </layer>
            </map>
        """.trimIndent()

        val map = TmxLoader.parse(tmx)

        assertEquals(16, map.tileWidth)
        assertEquals(16, map.tileHeight)
        assertEquals(1, map.tilesets.size)
        assertEquals(1, map.tilesets[0].firstGid)
        assertEquals("Base", map.tilesets[0].name)
        assertEquals("base.png", map.tilesets[0].imageSource)
        assertEquals(1, map.layers.size)
        assertEquals("Floor", map.layers[0].name)
        assertEquals(4, map.layers[0].cells.size)

        val c0 = map.layers[0].cells[0]
        assertEquals(0, c0.gridX)
        assertEquals(0, c0.gridY)
        assertEquals(1, c0.gid)
        assertFalse(c0.flipH)
        assertFalse(c0.flipV)
        assertFalse(c0.flipD)

        val c3 = map.layers[0].cells[3]
        assertEquals(1, c3.gridX)
        assertEquals(1, c3.gridY)
        assertEquals(4, c3.gid)
    }

    @Test
    fun `infinite map with chunks - positive and negative coordinates`() {
        val tmx = """
            <?xml version="1.0"?>
            <map tilewidth="16" tileheight="16" width="16" height="16" infinite="1">
              <tileset firstgid="1" name="T" tilewidth="16" tileheight="16" tilecount="100" columns="10">
                <image source="t.png" width="160" height="160"/>
              </tileset>
              <layer name="Ground" width="16" height="16">
                <data encoding="csv">
                  <chunk x="0" y="0" width="2" height="2">1,2,
            3,4</chunk>
                  <chunk x="-16" y="-16" width="2" height="2">5,6,
            7,8</chunk>
                </data>
              </layer>
            </map>
        """.trimIndent()

        val map = TmxLoader.parse(tmx)
        val cells = map.layers[0].cells
        assertEquals(8, cells.size)

        // Chunk at (0,0)
        val c00 = cells.first { it.gridX == 0 && it.gridY == 0 }
        assertEquals(1, c00.gid)
        val c11 = cells.first { it.gridX == 1 && it.gridY == 1 }
        assertEquals(4, c11.gid)

        // Chunk at (-16,-16)
        val cNeg = cells.first { it.gridX == -16 && it.gridY == -16 }
        assertEquals(5, cNeg.gid)
        val cNeg11 = cells.first { it.gridX == -15 && it.gridY == -15 }
        assertEquals(8, cNeg11.gid)
    }

    @Test
    fun `flip bits are extracted correctly`() {
        // GID 3 with FLIP_H set = 0x80000003 = 2147483651
        val tmx = """
            <?xml version="1.0"?>
            <map tilewidth="16" tileheight="16" width="2" height="1" infinite="0">
              <tileset firstgid="1" name="T" tilewidth="16" tileheight="16" tilecount="4" columns="2">
                <image source="t.png"/>
              </tileset>
              <layer name="Floor" width="2" height="1">
                <data encoding="csv">2147483651,3221225475</data>
              </layer>
            </map>
        """.trimIndent()
        // 2147483651 = 0x80000003 → flipH=true, gid=3
        // 3221225475 = 0xC0000003 → flipH=true, flipV=true, gid=3

        val map = TmxLoader.parse(tmx)
        val cells = map.layers[0].cells
        assertEquals(2, cells.size)

        val c0 = cells[0]
        assertEquals(3, c0.gid)
        assertTrue(c0.flipH)
        assertFalse(c0.flipV)
        assertFalse(c0.flipD)

        val c1 = cells[1]
        assertEquals(3, c1.gid)
        assertTrue(c1.flipH)
        assertTrue(c1.flipV)
        assertFalse(c1.flipD)
    }

    @Test
    fun `animated tiles parsed from tileset`() {
        val tmx = """
            <?xml version="1.0"?>
            <map tilewidth="16" tileheight="16" width="1" height="1" infinite="0">
              <tileset firstgid="1" name="Candles" tilewidth="16" tileheight="16" tilecount="6" columns="3">
                <image source="candles.png"/>
                <tile id="0">
                  <animation>
                    <frame tileid="0" duration="150"/>
                    <frame tileid="2" duration="150"/>
                  </animation>
                </tile>
              </tileset>
              <layer name="Floor" width="1" height="1">
                <data encoding="csv">1</data>
              </layer>
            </map>
        """.trimIndent()

        val map = TmxLoader.parse(tmx)
        val ts = map.tilesets[0]
        assertEquals(1, ts.animatedTiles.size)
        val frames = ts.animatedTiles[0]!!
        assertEquals(2, frames.size)
        assertEquals(0, frames[0].tileId)
        assertEquals(150, frames[0].durationMs)
        assertEquals(2, frames[1].tileId)
        assertEquals(150, frames[1].durationMs)
    }

    @Test
    fun `multiple tilesets sorted by firstGid`() {
        val tmx = """
            <?xml version="1.0"?>
            <map tilewidth="16" tileheight="16" width="1" height="1" infinite="0">
              <tileset firstgid="100" name="Second" tilewidth="16" tileheight="16" tilecount="10" columns="5">
                <image source="second.png"/>
              </tileset>
              <tileset firstgid="1" name="First" tilewidth="16" tileheight="16" tilecount="50" columns="10">
                <image source="first.png"/>
              </tileset>
              <layer name="Floor" width="1" height="1">
                <data encoding="csv">1</data>
              </layer>
            </map>
        """.trimIndent()

        val map = TmxLoader.parse(tmx)
        assertEquals(2, map.tilesets.size)
        assertEquals("First", map.tilesets[0].name)
        assertEquals(1, map.tilesets[0].firstGid)
        assertEquals("Second", map.tilesets[1].name)
        assertEquals(100, map.tilesets[1].firstGid)
    }

    @Test
    fun `empty cells (gid 0) are skipped`() {
        val tmx = """
            <?xml version="1.0"?>
            <map tilewidth="16" tileheight="16" width="2" height="2" infinite="0">
              <tileset firstgid="1" name="T" tilewidth="16" tileheight="16" tilecount="4" columns="2">
                <image source="t.png"/>
              </tileset>
              <layer name="Floor" width="2" height="2">
                <data encoding="csv">1,0,
            0,2</data>
              </layer>
            </map>
        """.trimIndent()

        val map = TmxLoader.parse(tmx)
        val cells = map.layers[0].cells
        assertEquals(2, cells.size)
        assertEquals(1, cells[0].gid)
        assertEquals(0, cells[0].gridX)
        assertEquals(0, cells[0].gridY)
        assertEquals(2, cells[1].gid)
        assertEquals(1, cells[1].gridX)
        assertEquals(1, cells[1].gridY)
    }
}
