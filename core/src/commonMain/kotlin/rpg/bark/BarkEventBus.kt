package rpg.bark

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Result of attempting to fire a bark. */
sealed class BarkFireResult {
    /** The bark was emitted to the Questbook pipeline. */
    data class Emitted(val bark: BarkEvent) : BarkFireResult()

    /** The bark was suppressed because it is still on cooldown. */
    data class OnCooldown(val bark: BarkEvent, val remainingMillis: Long) : BarkFireResult()
}

/**
 * Emits bark events to the Questbook pipeline and enforces per-key cooldowns.
 *
 * Cooldown is per bark key, not per type (docs note 3): a bark on cooldown does
 * not fire -- the audio may still play for atmosphere, but no [BarkEvent] is
 * emitted to the Questbook.
 *
 * Time is injected via [clockMillis] so cooldown behaviour is deterministic in
 * tests. The emitted flow uses replay = 1 to avoid the pre-subscription drop
 * documented in the waitroom review (signals/ManualSignalSource.kt).
 */
class BarkEventBus(
    private val clockMillis: () -> Long
) {
    private val _emitted = MutableSharedFlow<BarkEvent>(
        replay = 1,
        extraBufferCapacity = 16
    )

    /** Stream of barks that actually reached the Questbook (i.e. not on cooldown). */
    val emitted: Flow<BarkEvent> = _emitted.asSharedFlow()

    private val lastFiredAt = mutableMapOf<BarkEvent, Long>()

    /**
     * Attempt to fire [bark]. Returns whether it was emitted or suppressed by
     * its cooldown. On success the bark is published to [emitted].
     *
     * When [bypassCooldown] is true the per-key cooldown is ignored and the
     * bark always emits. This is used for combat-origin barks (taunts, death,
     * victory, damage reactions): those are driven by deterministic game state
     * rather than player spam, and the engine may legitimately reuse a key
     * within the cooldown window (e.g. a taunt key that also appears in the
     * victory set). Suppressing them would silently drop story-critical lines
     * (see review issues #1 and #2). The timestamp is still recorded so any
     * subsequent player-initiated bark of the same key respects the cooldown.
     */
    fun fire(bark: BarkEvent, bypassCooldown: Boolean = false): BarkFireResult {
        val now = clockMillis()
        val cooldownMillis = BarkRegistry[bark].cooldownSeconds * 1000L
        val last = lastFiredAt[bark]

        if (!bypassCooldown && last != null) {
            val elapsed = now - last
            if (elapsed < cooldownMillis) {
                return BarkFireResult.OnCooldown(bark, cooldownMillis - elapsed)
            }
        }

        lastFiredAt[bark] = now
        _emitted.tryEmit(bark)
        return BarkFireResult.Emitted(bark)
    }

    /** Whether [bark] can currently fire (not on cooldown). */
    fun isReady(bark: BarkEvent): Boolean {
        val last = lastFiredAt[bark] ?: return true
        val cooldownMillis = BarkRegistry[bark].cooldownSeconds * 1000L
        return clockMillis() - last >= cooldownMillis
    }

    /** Clears all cooldowns. Pressure/markers reset on map transition (docs note 5). */
    fun resetCooldowns() {
        lastFiredAt.clear()
    }
}
