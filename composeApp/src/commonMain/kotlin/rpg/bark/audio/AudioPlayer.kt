package rpg.bark.audio

/**
 * Platform-agnostic audio playback contract for short WAV clips.
 *
 * Implementations load WAV data from Compose Resources (the `files/` directory)
 * and play it using native audio APIs. Desktop uses `javax.sound.sampled`, while
 * Android uses `MediaPlayer`.
 *
 * All operations are non-blocking and safe to call from coroutines.
 */
interface AudioPlayer {

    /**
     * Play the WAV file at the given [resourcePath].
     * The path is relative to the Compose Resources `files/` root,
     * e.g. `"bark/nib/i_smell_treasure.wav"`.
     *
     * If audio is already playing, it is stopped first (interruption).
     */
    fun play(resourcePath: String)

    /** Stop any currently playing audio. */
    fun stop()

    /** Whether audio is currently playing. */
    fun isPlaying(): Boolean

    /** Release underlying audio resources. Call when the player is no longer needed. */
    fun release()
}

/** A no-op [AudioPlayer] for testing or headless operation. */
object NoOpAudioPlayer : AudioPlayer {
    override fun play(resourcePath: String) {}
    override fun stop() {}
    override fun isPlaying(): Boolean = false
    override fun release() {}
}
