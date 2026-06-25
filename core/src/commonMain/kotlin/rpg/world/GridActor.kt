package rpg.world

/**
 * An actor that occupies one tile and slides smoothly to the next when it
 * moves (classic grid-step RPG movement). [tileX]/[tileY] is the logical
 * (target) cell -- it updates the instant a step starts, so collision checks
 * are simple; the visual position interpolates from [fromX]/[fromY] using
 * [progress] (0..1) for rendering only.
 */
class GridActor(
    var tileX: Int,
    var tileY: Int,
    var facing: Direction = Direction.DOWN
) {
    var fromX: Int = tileX
        private set
    var fromY: Int = tileY
        private set
    var progress: Float = 1f
        private set
    var moving: Boolean = false
        private set

    fun startMove(toX: Int, toY: Int) {
        fromX = tileX
        fromY = tileY
        tileX = toX
        tileY = toY
        progress = 0f
        moving = true
    }

    /** Advances the slide; returns true on the frame the move completes. */
    fun advance(fraction: Float): Boolean {
        if (!moving) return false
        progress += fraction
        if (progress >= 1f) {
            progress = 1f
            moving = false
            return true
        }
        return false
    }

    /** Visual X in tile units (interpolated while moving). */
    fun visualX(): Float = fromX + (tileX - fromX) * progress

    /** Visual Y in tile units (interpolated while moving). */
    fun visualY(): Float = fromY + (tileY - fromY) * progress
}
