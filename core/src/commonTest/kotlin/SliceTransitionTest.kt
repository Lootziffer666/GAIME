import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import rpg.world.Direction
import rpg.world.GameMaps
import rpg.world.GridEntity
import rpg.world.GridEntityType
import rpg.world.GridWorld

class SliceTransitionTest {

    // --- Map parsing ---

    @Test
    fun sewerMapParsesCorrectly() {
        val map = GameMaps.sewer()
        assertEquals(15, map.width)
        assertEquals(22, map.height)
        assertEquals(7, map.spawnX)
        assertEquals(2, map.spawnY)
        // Rubble row at y=8: every cell is blocked (it's a wall-like row)
        assertTrue(map.isBlocked(7, 8), "rubble tile is blocked")
        // Rubble tile has the rubble trigger
        assertEquals(GameMaps.TRIGGER_RUBBLE, map.triggerAt(7, 8))
        // Vellum puzzle trigger at (7,12)
        assertEquals(GameMaps.TRIGGER_VELLUM_PUZZLE, map.triggerAt(7, 12))
        // Sewer exit at (7,20)
        assertEquals(GameMaps.TRIGGER_SEWER_EXIT, map.triggerAt(7, 20))
    }

    @Test
    fun bossRoomMapParsesCorrectly() {
        val map = GameMaps.bossRoom()
        assertEquals(13, map.width)
        assertEquals(14, map.height)
        assertEquals(6, map.spawnX)
        assertEquals(2, map.spawnY)
        // Chokepoint wall at row 5 – all blocked except the gap at x=6
        assertTrue(map.isBlocked(0, 5))
        assertTrue(map.isBlocked(5, 5))
        assertFalse(map.isBlocked(6, 5), "chokepoint gap is passable")
        assertTrue(map.isBlocked(7, 5))
        // Page pickup at (6,10)
        assertEquals(GameMaps.TRIGGER_PAGE_PICKUP, map.triggerAt(6, 10))
        // Boss exit at (6,12)
        assertEquals(GameMaps.TRIGGER_BOSS_EXIT, map.triggerAt(6, 12))
    }

    // --- Obstacle clearing ---

    @Test
    fun clearObstacleMakesRubblePassable() {
        val world = GridWorld(GameMaps.sewer())
        assertTrue(world.map.isBlocked(7, 8), "rubble is initially blocked")
        // Player starts at (7,2); can only go down to y=7 before rubble blocks at y=8
        world.player.let { p ->
            // Move to (7,7): 5 steps down
            repeat(5) {
                assertTrue(world.requestStep(Direction.DOWN))
                world.update(0.16f)
            }
            assertEquals(7, p.tileX); assertEquals(7, p.tileY)
        }
        assertFalse(world.requestStep(Direction.DOWN), "blocked by rubble before clearing")

        world.clearObstacle(GameMaps.TRIGGER_RUBBLE)
        assertTrue(world.requestStep(Direction.DOWN), "passable after clearing")
    }

    // --- Entity blocking and interactions ---

    @Test
    fun enemyEntityBlocksMovement() {
        val world = GridWorld(GameMaps.sewer())
        world.entities.add(GridEntity("rat", 7, 3, GridEntityType.ENEMY, "enemy_rat"))
        // Player at (7,2); enemy at (7,3) – should block downward step
        assertFalse(world.requestStep(Direction.DOWN), "enemy blocks movement")
        assertEquals(2, world.player.tileY, "player did not move")
    }

    @Test
    fun approachingEnemyQueuesInteraction() {
        val world = GridWorld(GameMaps.sewer())
        val rat = GridEntity("rat", 7, 3, GridEntityType.ENEMY, "enemy_rat")
        world.entities.add(rat)
        world.requestStep(Direction.DOWN)
        val interactions = world.consumeEntityInteractions()
        assertEquals(1, interactions.size)
        assertEquals("rat", interactions.first().id)
        assertTrue(world.consumeEntityInteractions().isEmpty(), "interactions consumed")
    }

    @Test
    fun removeEntityAllowsPassage() {
        val world = GridWorld(GameMaps.sewer())
        world.entities.add(GridEntity("rat", 7, 3, GridEntityType.ENEMY, "enemy_rat"))
        assertFalse(world.requestStep(Direction.DOWN))
        world.removeEntity("rat")
        assertTrue(world.entities.isEmpty())
        assertTrue(world.requestStep(Direction.DOWN), "passable after removal")
    }

    @Test
    fun npcEntityDoesNotBlockMovement() {
        val world = GridWorld(GameMaps.tavern())
        world.entities.add(GridEntity("barkeep", 5, 4, GridEntityType.NPC, "hero_brugg"))
        // Player at (5,3); NPC at (5,4) — NPCs do not block movement
        assertTrue(world.requestStep(Direction.DOWN), "NPC does not block movement")
    }
}
