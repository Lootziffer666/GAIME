package engine

enum class Personality {
    Nervous,
    Curious,
    Sleepy
}

data class Particle(
    val char: Char,
    var x: Float,
    var y: Float,
    val originX: Float,
    val originY: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var alpha: Float = 1f,
    var size: Float = 14f,
    val personality: Personality
)
