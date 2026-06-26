package rpg.world

/**
 * The walkable world: a [TileMap] plus a player that steps tile-by-tile, plus
 * any [GridEntity] objects placed by the host screen. Movement and trigger logic
 * are pure and deterministic (driven by an explicit delta time), so they can be
 * unit-tested without any rendering.
 */
class GridWorld(
    val map: TileMap,
    val player: GridActor = GridActor(map.spawnX, map.spawnY),
    private val stepDuration: Float = 0.16f
) {
    private val firedTriggers = mutableSetOf<String>()
    private val pendingTriggers = ArrayDeque<String>()

    /** All NPC and enemy entities currently on this map. */
    val entities: MutableList<GridEntity> = mutableListOf()

    /**
     * Tile cell indices (y*width+x) that were statically blocked but have been
     * cleared at runtime (e.g. rubble removed by a utility bark).
     */
    private val unblockedOverrides = mutableSetOf<Int>()

    private val pendingEntityInteractions = ArrayDeque<GridEntity>()

    /**
     * Requests a one-tile step in [dir]. Always faces that direction; only moves
     * if not already mid-step and the target tile is unblocked. ENEMY entities
     * also block movement: approaching one queues an interaction instead of moving.
     */
    fun requestStep(dir: Direction): Boolean {
        player.facing = dir
        if (player.moving) return false
        val nx = player.tileX + dir.dx
        val ny = player.tileY + dir.dy
        val entityAtTarget = entities.firstOrNull { it.tileX == nx && it.tileY == ny }
        if (entityAtTarget != null && entityAtTarget.type == GridEntityType.ENEMY) {
            pendingEntityInteractions.addLast(entityAtTarget)
            return false
        }
        val cellIdx = ny * map.width + nx
        if (map.isBlocked(nx, ny) && cellIdx !in unblockedOverrides) return false
        player.startMove(nx, ny)
        return true
    }

    /** Advances the current step. Fires each trigger tile once on arrival. */
    fun update(deltaTime: Float) {
        if (!player.moving) return
        val arrived = player.advance(deltaTime / stepDuration)
        if (arrived) {
            map.triggerAt(player.tileX, player.tileY)?.let { id ->
                if (firedTriggers.add(id)) pendingTriggers.addLast(id)
            }
        }
    }

    /** Returns and clears triggers that fired since the last call. */
    fun consumeTriggers(): List<String> {
        if (pendingTriggers.isEmpty()) return emptyList()
        val out = pendingTriggers.toList()
        pendingTriggers.clear()
        return out
    }

    /**
     * Removes the obstacle identified by [triggerId] from the collision layer,
     * making that tile passable. Uses the trigger map to locate the cell index.
     */
    fun clearObstacle(triggerId: String) {
        map.triggers.entries.firstOrNull { it.value == triggerId }?.let { (idx, _) ->
            unblockedOverrides.add(idx)
        }
    }

    /** Returns and clears pending entity interactions (enemy approach events). */
    fun consumeEntityInteractions(): List<GridEntity> {
        if (pendingEntityInteractions.isEmpty()) return emptyList()
        val out = pendingEntityInteractions.toList()
        pendingEntityInteractions.clear()
        return out
    }

    /** Removes an entity by id (call after combat victory to allow passage). */
    fun removeEntity(id: String) {
        entities.removeAll { it.id == id }
    }

    /**
     * Returns the NPC entity adjacent to the player in their facing direction,
     * or null if none. Used for interact-key NPC dialogue.
     */
    fun requestInteraction(): GridEntity? {
        val nx = player.tileX + player.facing.dx
        val ny = player.tileY + player.facing.dy
        return entities.firstOrNull {
            it.tileX == nx && it.tileY == ny && it.type == GridEntityType.NPC
        }
    }
}
