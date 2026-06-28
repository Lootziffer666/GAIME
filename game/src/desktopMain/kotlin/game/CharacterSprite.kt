package game

import korlibs.image.bitmap.BmpSlice
import korlibs.korge.view.Container
import korlibs.korge.view.Image
import korlibs.korge.view.addUpdater
import korlibs.korge.view.image
import korlibs.time.milliseconds

/**
 * A character sprite with real CraftPix sheets (Idle/Walk/Attack/Hurt/Death),
 * grid-based positioning with smooth interpolated movement, and directional
 * sprite rows (4 rows per sheet). Falls back to procedural bitmaps if loading fails.
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
    private var moveProgress: Float = 1f
    private val stepDurationMs: Float = 160f

    val isMoving: Boolean get() = moveProgress < 1f

    val visualGridX: Double
        get() = fromGridX + (gridX - fromGridX) * moveProgress.toDouble()
    val visualGridY: Double
        get() = fromGridY + (gridY - fromGridY) * moveProgress.toDouble()

    fun startMove(toGridX: Int, toGridY: Int): Boolean {
        if (isMoving) return false
        fromGridX = gridX
        fromGridY = gridY
        moveProgress = 0f
        gridX = toGridX
        gridY = toGridY
        return true
    }

    // --- Directional animations: Map<Animation, List<Rows>> where each Row = List<BmpSlice> ---
    private val animations = mutableMapOf<SpriteAnimation, List<List<BmpSlice>>>()
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

    // --- Facing → row index (CraftPix convention, verified per B005) ---
    private fun Facing.rowIndex(): Int = when (this) {
        Facing.DOWN -> 0
        Facing.LEFT -> 1
        Facing.UP -> 2
        Facing.RIGHT -> 3
    }

    /** Get frames for current facing + animation, with row fallback. */
    private fun framesForCurrent(): List<BmpSlice>? {
        val rows = animations[currentAnim] ?: return null
        if (rows.isEmpty()) return null
        return when {
            rows.size > facing.rowIndex() -> rows[facing.rowIndex()]
            rows.size == 1 -> rows[0]  // Single-row sheet → scaleX flip handles facing
            else -> rows[0]
        }
    }

    // --- Asset loading (suspend) ---

    suspend fun loadSwordsman() {
        val base = "assets/HD/characters/swordsman/PNG/Swordsman_lvl1/Without_shadow"
        animations[SpriteAnimation.IDLE] = SpriteLoader.loadAllRows("$base/Swordsman_lvl1_Idle_without_shadow.png")
        animations[SpriteAnimation.WALK] = SpriteLoader.loadAllRows("$base/Swordsman_lvl1_Walk_without_shadow.png")
        animations[SpriteAnimation.ATTACK] = SpriteLoader.loadAllRows("$base/Swordsman_lvl1_attack_without_shadow.png")
        animations[SpriteAnimation.HURT] = SpriteLoader.loadAllRows("$base/Swordsman_lvl1_Hurt_without_shadow.png")
        animations[SpriteAnimation.DEATH] = SpriteLoader.loadAllRows("$base/Swordsman_lvl1_Death_without_shadow.png")
        applyFirstFrame()
    }

    suspend fun loadVampire() {
        val base = "assets/HD/characters/vampire/PNG/Vampires1/Without_shadow"
        animations[SpriteAnimation.IDLE] = SpriteLoader.loadAllRows("$base/Vampires1_Idle_without_shadow.png")
        animations[SpriteAnimation.WALK] = SpriteLoader.loadAllRows("$base/Vampires1_Walk_without_shadow.png")
        animations[SpriteAnimation.ATTACK] = SpriteLoader.loadAllRows("$base/Vampires1_Attack_without_shadow.png")
        animations[SpriteAnimation.HURT] = SpriteLoader.loadAllRows("$base/Vampires1_Hurt_without_shadow.png")
        animations[SpriteAnimation.DEATH] = SpriteLoader.loadAllRows("$base/Vampires1_Death_without_shadow.png")
        applyFirstFrame()
    }

    /**
     * Loads a single idle sheet (and optionally walk). Used for NPC sprites.
     * Falls back to procedural bitmaps on error.
     */
    suspend fun loadFromSheet(idleSheetPath: String?, walkSheetPath: String? = null) {
        val idleRows = if (idleSheetPath != null) {
            SpriteLoader.loadAllRows(idleSheetPath)
        } else {
            listOf(SpriteLoader.sliceFrames(SpriteLoader.buildFallbackBitmap()))
        }
        animations[SpriteAnimation.IDLE] = idleRows
        animations[SpriteAnimation.WALK] = if (walkSheetPath != null) SpriteLoader.loadAllRows(walkSheetPath) else idleRows
        animations[SpriteAnimation.ATTACK] = idleRows
        animations[SpriteAnimation.HURT] = idleRows
        animations[SpriteAnimation.DEATH] = idleRows
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

        val frames = framesForCurrent() ?: return
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
        val frames = framesForCurrent() ?: return
        if (frames.isEmpty()) return
        val idx = frameIndex.coerceIn(0, frames.size - 1)
        img.bitmap = frames[idx]
    }

    private fun applyFirstFrame() {
        val frames = framesForCurrent()
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
        // Multi-row sheets: no scaleX flip needed (row handles facing).
        // Single-row sheets: LEFT = scaleX -1, others = 1.
        val rows = animations[currentAnim]
        val isMultiRow = rows != null && rows.size > 1
        img.scaleX = if (!isMultiRow && facing == Facing.LEFT) -1.0 else 1.0
        // When facing changes, reset frame to show correct row
        applyCurrentFrame()
        updatePosition()
    }
}
