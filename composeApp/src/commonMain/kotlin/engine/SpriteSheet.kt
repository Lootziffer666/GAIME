package engine

data class SpriteSheet(
    val frameWidth: Int,
    val frameHeight: Int,
    val columns: Int,
    val rows: Int,
    val frameDurations: List<Float> // duration per frame in seconds
)
