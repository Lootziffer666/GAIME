package engine

data class SpriteFrame(
    val index: Int,
    val duration: Float // seconds
)

class SpriteAnimator(
    private val frames: List<SpriteFrame>,
    private val looping: Boolean = true
) {
    var currentFrameIndex: Int = 0
        private set
    private var elapsedTime: Float = 0f
    var isFinished: Boolean = false
        private set

    val currentFrame: SpriteFrame
        get() = frames[currentFrameIndex]

    fun update(deltaTime: Float) {
        if (isFinished) return
        if (frames.isEmpty()) return

        elapsedTime += deltaTime
        val frameDuration = frames[currentFrameIndex].duration

        while (elapsedTime >= frameDuration) {
            elapsedTime -= frameDuration
            currentFrameIndex++

            if (currentFrameIndex >= frames.size) {
                if (looping) {
                    currentFrameIndex = 0
                } else {
                    currentFrameIndex = frames.size - 1
                    isFinished = true
                    return
                }
            }
        }
    }

    fun reset() {
        currentFrameIndex = 0
        elapsedTime = 0f
        isFinished = false
    }
}
