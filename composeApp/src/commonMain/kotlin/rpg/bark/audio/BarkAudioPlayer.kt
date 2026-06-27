package rpg.bark.audio

import rpg.bark.BarkAudioRegistry
import rpg.bark.BarkEvent

/**
 * High-level audio player for bark voice lines.
 *
 * Integrates with [BarkAudioRegistry] to resolve WAV paths from [BarkEvent]
 * values, then delegates playback to the platform [AudioPlayer].
 *
 * Playback is fire-and-forget: calling [playBark] while another bark is still
 * playing interrupts the previous one (only one bark plays at a time).
 */
class BarkAudioPlayer(
    private val audioPlayer: AudioPlayer
) {
    /** The bark currently being played, or null if idle. */
    var currentBark: BarkEvent? = null
        private set

    /**
     * Play the voice line for [event].
     *
     * Resolves the WAV path via [BarkAudioRegistry] and starts playback.
     * Any currently playing bark is interrupted. When playback finishes
     * naturally (without interruption), [currentBark] is cleared automatically.
     */
    fun playBark(event: BarkEvent) {
        val path = BarkAudioRegistry.pathFor(event)
        audioPlayer.stop()
        currentBark = event
        audioPlayer.play(path) {
            // Clear currentBark on natural playback completion
            currentBark = null
        }
    }

    /** Stop the currently playing bark, if any. */
    fun stop() {
        audioPlayer.stop()
        currentBark = null
    }

    /**
     * Play an arbitrary WAV file by path (for NPC dialogue lines that are
     * not driven by a [BarkEvent]). Stops any currently playing audio first.
     *
     * @param path resource path relative to the Compose Resources `files/` root,
     *   e.g. `"bark/brugg/spend_some_coin_or_get_out.wav"`.
     */
    fun playRawPath(path: String) {
        audioPlayer.stop()
        currentBark = null
        audioPlayer.play(path)
    }

    /** Whether a bark is currently playing. */
    fun isPlaying(): Boolean = audioPlayer.isPlaying()

    /**
     * Resolve the WAV path for a given event without playing it.
     * Useful for testing path resolution.
     */
    fun resolvePath(event: BarkEvent): String = BarkAudioRegistry.pathFor(event)

    /** Release underlying audio resources. */
    fun release() {
        currentBark = null
        audioPlayer.release()
    }
}
