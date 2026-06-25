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
     */
    fun fire(bark: BarkEvent): BarkFireResult {
        val now = clockMillis()
        val cooldownMillis = BarkRegistry[bark].cooldownSeconds * 1000L
        val last = lastFiredAt[bark]

        if (last != null) {
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
