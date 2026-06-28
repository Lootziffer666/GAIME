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
 * sprite rows (4 rows per sheet = DOWN/UP/RIGHT/LEFT). Falls back to procedural
 * bitmaps if asset loading fails.
 */
enum class SpriteAnimation { IDLE, WALK, ATTACK, HURT, DEATH }

// Facing enum lives in PlayerSprite.kt (DO_NOT_TOUCH) — reused here.
// Extension properties for direction deltas:
val Facing.dx: Int get() = when (this) { Facing.LEFT -> -1; Facing.RIGHT -> 1; else -> 0 }
val Facing.dy: Int get() = when (this) { Facing.UP -> -1; Facing.DOWN -> 1; else -> 0 }

/** Maps a Facing to the sprite-sheet row index. */
val Facing.spriteRow: Int get() = when (this) {
    Facing.DOWN -> SpriteLoader.ROW_DOWN
    Facing.UP -> SpriteLoader.ROW_UP
    Facing.RIGHT -> SpriteLoader.ROW_RIGHT
    Facing.LEFT -> SpriteLoader.ROW_LEFT
}

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
        set(value) {
            if (field != value) {
                field = value
                updateFacing()
                applyDirectionRow()
            }
        }
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

    // --- Directional animation storage ---
    // Key: SpriteAnimation → Map<rowIndex, List<BmpSlice>>
    private val directionalAnims = mutableMapOf<SpriteAnimation, Map<Int, List<BmpSlice>>>()

    // Flattened current-direction frames (updated when facing changes)
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
        loadDirectional(SpriteAnimation.IDLE, "$base/Swordsman_lvl1_Idle_without_shadow.png")
        loadDirectional(SpriteAnimation.WALK, "$base/Swordsman_lvl1_Walk_without_shadow.png")
        loadDirectional(SpriteAnimation.ATTACK, "$base/Swordsman_lvl1_attack_without_shadow.png")
        loadDirectional(SpriteAnimation.HURT, "$base/Swordsman_lvl1_Hurt_without_shadow.png")
        loadDirectional(SpriteAnimation.DEATH, "$base/Swordsman_lvl1_Death_without_shadow.png")
        applyDirectionRow()
        applyFirstFrame()
    }

    suspend fun loadVampire() {
        val base = "assets/HD/characters/vampire/PNG/Vampires1/Without_shadow"
        loadDirectional(SpriteAnimation.IDLE, "$base/Vampires1_Idle_without_shadow.png")
        loadDirectional(SpriteAnimation.WALK, "$base/Vampires1_Walk_without_shadow.png")
        loadDirectional(SpriteAnimation.ATTACK, "$base/Vampires1_Attack_without_shadow.png")
        loadDirectional(SpriteAnimation.HURT, "$base/Vampires1_Hurt_without_shadow.png")
        loadDirectional(SpriteAnimation.DEATH, "$base/Vampires1_Death_without_shadow.png")
        applyDirectionRow()
        applyFirstFrame()
    }

    /**
     * Loads a single idle sheet (and optionally walk). Used for NPC sprites.
     * Falls back to procedural bitmaps on error. Only loads row 0 (front).
     */
    suspend fun loadFromSheet(idleSheetPath: String?, walkSheetPath: String? = null) {
        val idleRows = if (idleSheetPath != null) {
            SpriteLoader.loadAllRows(idleSheetPath)
        } else {
            val fb = SpriteLoader.sliceFrames(SpriteLoader.buildFallbackBitmap())
            mapOf(0 to fb, 1 to fb, 2 to fb, 3 to fb)
        }
        directionalAnims[SpriteAnimation.IDLE] = idleRows
        val walkRows = if (walkSheetPath != null) SpriteLoader.loadAllRows(walkSheetPath) else idleRows
        directionalAnims[SpriteAnimation.WALK] = walkRows
        directionalAnims[SpriteAnimation.ATTACK] = idleRows
        directionalAnims[SpriteAnimation.HURT] = idleRows
        directionalAnims[SpriteAnimation.DEATH] = idleRows
        applyDirectionRow()
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

    private suspend fun loadDirectional(anim: SpriteAnimation, path: String) {
        directionalAnims[anim] = SpriteLoader.loadAllRows(path)
    }

    /**
     * Updates the flat `animations` map from `directionalAnims` for the current [facing].
     * Called when facing changes or after loading.
     */
    private fun applyDirectionRow() {
        val row = facing.spriteRow
        for ((anim, rowMap) in directionalAnims) {
            // Use requested row, fall back to row 0, fall back to any available
            animations[anim] = rowMap[row] ?: rowMap[0] ?: rowMap.values.firstOrNull() ?: emptyList()
        }
    }

    private fun advanceAnimation(dtMs: Float) {
        // Smooth movement tick FIRST
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
        // With directional rows, we don't need scaleX flip for LEFT anymore
        // (row 3 IS the left-facing animation). Reset scaleX to 1.
        // Exception: if sheet has only 3 rows (no row 3), use scaleX=-1 on row 2.
        val rowMap = directionalAnims[currentAnim]
        if (rowMap != null && facing == Facing.LEFT && !rowMap.containsKey(SpriteLoader.ROW_LEFT)) {
            img.scaleX = -1.0
        } else {
            img.scaleX = 1.0
        }
        updatePosition()
    }
}
