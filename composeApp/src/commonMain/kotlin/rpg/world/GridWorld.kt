package rpg.world

/**
 * The walkable world: a [TileMap] plus a player that steps tile-by-tile.
 * Movement and trigger logic are pure and deterministic (driven by an explicit
 * delta time), so they can be unit-tested without any rendering.
 */
class GridWorld(
    val map: TileMap,
    val player: GridActor = GridActor(map.spawnX, map.spawnY),
    /** Seconds it takes to slide one tile. */
    private val stepDuration: Float = 0.16f
) {
    private val firedTriggers = mutableSetOf<String>()
    private val pendingTriggers = ArrayDeque<String>()

    /**
     * Requests a one-tile step in [dir]. Always faces that way; only moves if
     * not already mid-step and the target tile is not blocked. Returns whether
     * a move actually started.
     */
    fun requestStep(dir: Direction): Boolean {
        player.facing = dir
        if (player.moving) return false
        val nx = player.tileX + dir.dx
        val ny = player.tileY + dir.dy
        if (map.isBlocked(nx, ny)) return false
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
}
