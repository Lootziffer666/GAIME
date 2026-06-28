package game

import korlibs.image.bitmap.BmpSlice
import korlibs.korge.view.Container
import korlibs.korge.view.Image
import korlibs.korge.view.addUpdater
import korlibs.korge.view.image
import korlibs.time.milliseconds

/**
 * A character sprite with real CraftPix sheets (Idle/Walk/Attack/Hurt/Death),
 * grid-based positioning with smooth interpolated movement, and facing flip.
 * Falls back to procedural bitmaps if asset loading fails.
 */
enum class SpriteAnimation { IDLE, WALK, ATTACK, HURT, DEATH }

// Facing enum lives in PlayerSprite.kt (DO_NOT_TOUCH) — reused here.
// Extension properties for direction deltas:
val Facing.dx: Int get() = when (this) { Facing.LEFT -> -1; Facing.RIGHT -> 1; else -> 0 }
val Facing.dy: Int get() = when (this) { Facing.UP -> -1; Facing.DOWN -> 1; else -> 0 }

class CharacterSprite(
    private val parent: Container,
    private val tileWidth: Int,
    private val tileHeight: Int,
) {
    var gridX: Int = 0
        set(value) { field = value; updatePosition() }
    var gridY: Int = 0
        set(value) { field = value; updatePosition() }
    var facing: Facing = Facing.DOWN
        set(value) { field = value; updateFacing() }
    var pixelOffsetX: Double = 0.0
    var pixelOffsetY: Double = 0.0

    // --- Smooth movement ---
    private var fromGridX: Int = 0
    private var fromGridY: Int = 0
    private var moveProgress: Float = 1f  // 1f = idle/arrived
    private val stepDurationMs: Float = 160f

    /** True while the sprite is mid-step between tiles. */
    val isMoving: Boolean get() = moveProgress < 1f

    /** Interpolated X position in tile units (for camera follow). */
    val visualGridX: Double
        get() = fromGridX + (gridX - fromGridX) * moveProgress.toDouble()

    /** Interpolated Y position in tile units (for camera follow). */
    val visualGridY: Double
        get() = fromGridY + (gridY - fromGridY) * moveProgress.toDouble()

    /**
     * Requests a move to (toGridX, toGridY). Returns false if mid-step.
     * On success: records from-position, sets gridX/Y, resets moveProgress to 0.
     */
    fun startMove(toGridX: Int, toGridY: Int): Boolean {
        if (isMoving) return false
        fromGridX = gridX
        fromGridY = gridY
        moveProgress = 0f  // Set BEFORE gridX/Y so updatePosition reads visualGrid = fromGrid
        gridX = toGridX
        gridY = toGridY
        return true
    }

    private val animations = mutableMapOf<SpriteAnimation, List<BmpSlice>>()
    private var currentAnim: SpriteAnimation = SpriteAnimation.IDLE
    private var frameIndex = 0
    private var elapsedMs = 0f
    private var looping = true
    private var onDoneCallback: (() -> Unit)? = null

    private val img: Image

    init {
        fromGridX = 0
        fromGridY = 0
        img = parent.image(SpriteLoader.sliceFrames(SpriteLoader.buildFallbackBitmap())[0]) {
            smoothing = false
        }
        parent.addUpdater { dt ->
            advanceAnimation(dt.milliseconds.toFloat())
        }
    }

    // --- Asset loading (suspend) ---

    suspend fun loadSwordsman() {
        val base = "assets/HD/characters/swordsman/PNG/Swordsman_lvl1/Without_shadow"
        animations[SpriteAnimation.IDLE] = SpriteLoader.load("$base/Swordsman_lvl1_Idle_without_shadow.png")
        animations[SpriteAnimation.WALK] = SpriteLoader.load("$base/Swordsman_lvl1_Walk_without_shadow.png")
        animations[SpriteAnimation.ATTACK] = SpriteLoader.load("$base/Swordsman_lvl1_attack_without_shadow.png")
        animations[SpriteAnimation.HURT] = SpriteLoader.load("$base/Swordsman_lvl1_Hurt_without_shadow.png")
        animations[SpriteAnimation.DEATH] = SpriteLoader.load("$base/Swordsman_lvl1_Death_without_shadow.png")
        applyFirstFrame()
    }

    suspend fun loadVampire() {
        val base = "assets/HD/characters/vampire/PNG/Vampires1/Without_shadow"
        animations[SpriteAnimation.IDLE] = SpriteLoader.load("$base/Vampires1_Idle_without_shadow.png")
        animations[SpriteAnimation.WALK] = SpriteLoader.load("$base/Vampires1_Walk_without_shadow.png")
        animations[SpriteAnimation.ATTACK] = SpriteLoader.load("$base/Vampires1_Attack_without_shadow.png")
        animations[SpriteAnimation.HURT] = SpriteLoader.load("$base/Vampires1_Hurt_without_shadow.png")
        animations[SpriteAnimation.DEATH] = SpriteLoader.load("$base/Vampires1_Death_without_shadow.png")
        applyFirstFrame()
    }

    /**
     * Loads a single idle sheet (and optionally walk). Used for NPC sprites.
     * Falls back to procedural bitmaps on error.
     */
    suspend fun loadFromSheet(idleSheetPath: String?, walkSheetPath: String? = null) {
        val idleFrames = if (idleSheetPath != null) {
            SpriteLoader.load(idleSheetPath)
        } else {
            SpriteLoader.sliceFrames(SpriteLoader.buildFallbackBitmap())
        }
        animations[SpriteAnimation.IDLE] = idleFrames
        animations[SpriteAnimation.WALK] = if (walkSheetPath != null) SpriteLoader.load(walkSheetPath) else idleFrames
        // Fallback for other anims so play() never crashes
        animations[SpriteAnimation.ATTACK] = idleFrames
        animations[SpriteAnimation.HURT] = idleFrames
        animations[SpriteAnimation.DEATH] = idleFrames
        applyFirstFrame()
    }

    // --- Animation control ---

    fun play(animation: SpriteAnimation, loop: Boolean = true, onDone: (() -> Unit)? = null) {
        if (animation == currentAnim && looping == loop) return
        currentAnim = animation
        frameIndex = 0
        elapsedMs = 0f
        looping = loop
        onDoneCallback = onDone
        applyCurrentFrame()
    }

    // --- Internals ---

    private fun advanceAnimation(dtMs: Float) {
        // Smooth movement tick FIRST — independent of animation frames
        if (isMoving) {
            moveProgress = (moveProgress + dtMs / stepDurationMs).coerceAtMost(1f)
            updatePosition()
        }

        val frames = animations[currentAnim] ?: return
        if (frames.isEmpty()) return

        val frameDuration = when (currentAnim) {
            SpriteAnimation.ATTACK, SpriteAnimation.HURT -> 80f
            else -> 120f
        }
        elapsedMs += dtMs
        if (elapsedMs >= frameDuration) {
            elapsedMs -= frameDuration
            frameIndex++
            if (frameIndex >= frames.size) {
                if (looping) {
                    frameIndex = 0
                } else {
                    frameIndex = frames.size - 1
                    onDoneCallback?.invoke()
                    onDoneCallback = null
                    return
                }
            }
            applyCurrentFrame()
        }
    }

    private fun applyCurrentFrame() {
        val frames = animations[currentAnim] ?: return
        if (frames.isEmpty()) return
        val idx = frameIndex.coerceIn(0, frames.size - 1)
        img.bitmap = frames[idx]
    }

    private fun applyFirstFrame() {
        val frames = animations[SpriteAnimation.IDLE]
        if (!frames.isNullOrEmpty()) {
            img.bitmap = frames[0]
        }
        val frameSize = img.bitmap.width
        pixelOffsetX = -(frameSize - tileWidth) / 2.0
        pixelOffsetY = -(frameSize - tileHeight) / 2.0
        fromGridX = gridX
        fromGridY = gridY
        moveProgress = 1f
        updatePosition()
    }

    private fun updatePosition() {
        img.x = visualGridX * tileWidth + pixelOffsetX
        img.y = visualGridY * tileHeight + pixelOffsetY
    }

    private fun updateFacing() {
        img.scaleX = if (facing == Facing.LEFT) -1.0 else 1.0
        updatePosition()
    }
}
