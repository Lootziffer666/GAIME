package rpg.world

/**
 * A 2D camera that follows a focus point and clamps to the world bounds. When
 * the world is smaller than the viewport on an axis, that axis is centred.
 * Pure math (pixel units), independent of any rendering backend.
 */
class Camera {
    var x: Float = 0f
        private set
    var y: Float = 0f
        private set

    fun follow(
        focusX: Float,
        focusY: Float,
        viewportW: Float,
        viewportH: Float,
        worldW: Float,
        worldH: Float
    ) {
        x = clamp(focusX - viewportW / 2f, worldW, viewportW)
        y = clamp(focusY - viewportH / 2f, worldH, viewportH)
    }

    private fun clamp(desired: Float, world: Float, view: Float): Float =
        if (world <= view) (world - view) / 2f else desired.coerceIn(0f, world - view)
}
