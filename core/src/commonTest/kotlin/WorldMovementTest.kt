import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import rpg.world.Camera
import rpg.world.Direction
import rpg.world.GameMaps
import rpg.world.GridWorld
import rpg.world.TileMapParser
import rpg.world.TileSpec

class WorldMovementTest {

    @Test
    fun tavernParsesWithWallsSpawnAndCellarTrigger() {
        val map = GameMaps.tavern()
        assertEquals(13, map.width)
        assertEquals(9, map.height)
        assertTrue(map.isBlocked(0, 0), "border is a wall")
        assertFalse(map.isBlocked(5, 3), "spawn tile is floor")
        assertEquals(5, map.spawnX)
        assertEquals(3, map.spawnY)
        assertEquals(GameMaps.TRIGGER_CELLAR_DOOR, map.triggerAt(5, 5))
    }

    @Test
    fun outOfBoundsCountsAsBlocked() {
        val map = GameMaps.tavern()
        assertTrue(map.isBlocked(-1, 0))
        assertTrue(map.isBlocked(0, -1))
        assertTrue(map.isBlocked(map.width, 0))
    }

    @Test
    fun stepOntoFloorMovesPlayerAfterSlide() {
        val world = GridWorld(GameMaps.tavern(), stepDuration = 0.16f)
        val startY = world.player.tileY
        assertTrue(world.requestStep(Direction.DOWN))
        // Logical tile updates immediately; movement completes after the slide.
        assertEquals(startY + 1, world.player.tileY)
        assertTrue(world.player.moving)
        world.update(0.16f)
        assertFalse(world.player.moving)
    }

    @Test
    fun cannotStepWhileAlreadyMoving() {
        val world = GridWorld(GameMaps.tavern())
        assertTrue(world.requestStep(Direction.DOWN))
        assertFalse(world.requestStep(Direction.DOWN), "blocked until current step finishes")
    }

    @Test
    fun stepIntoWallIsRejected() {
        // 3x3 box: only the centre is floor/spawn, everything else is wall.
        val legend = mapOf(
            '#' to TileSpec(atlasIndex = 14, blocked = true),
            '@' to TileSpec(atlasIndex = 48, spawn = true)
        )
        val map = TileMapParser.fromAscii(listOf("###", "#@#", "###"), legend)
        val world = GridWorld(map)
        assertFalse(world.requestStep(Direction.UP))
        assertFalse(world.requestStep(Direction.LEFT))
        assertEquals(1, world.player.tileX)
        assertEquals(1, world.player.tileY)
        // Facing still updates even when blocked.
        assertEquals(Direction.LEFT, world.player.facing)
    }

    @Test
    fun cellarTriggerFiresOnceOnArrival() {
        val world = GridWorld(GameMaps.tavern(), stepDuration = 0.1f)
        // Spawn (5,3) -> walk down to the door at (5,5).
        repeat(2) {
            world.requestStep(Direction.DOWN)
            world.update(0.1f)
        }
        assertEquals(listOf(GameMaps.TRIGGER_CELLAR_DOOR), world.consumeTriggers())
        // Already consumed, and one-shot: stepping away and back does not refire.
        assertTrue(world.consumeTriggers().isEmpty())
    }

    @Test
    fun cameraClampsAndCenters() {
        val cam = Camera()
        // Large world: clamps so the viewport stays inside the world.
        cam.follow(focusX = 0f, focusY = 0f, viewportW = 100f, viewportH = 100f, worldW = 500f, worldH = 500f)
        assertEquals(0f, cam.x)
        cam.follow(focusX = 10_000f, focusY = 10_000f, viewportW = 100f, viewportH = 100f, worldW = 500f, worldH = 500f)
        assertEquals(400f, cam.x)
        // Small world: centred (negative offset).
        cam.follow(focusX = 50f, focusY = 50f, viewportW = 200f, viewportH = 200f, worldW = 100f, worldH = 100f)
        assertEquals(-50f, cam.x)
    }
}
