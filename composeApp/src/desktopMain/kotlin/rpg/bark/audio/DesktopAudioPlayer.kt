package rpg.bark.audio

import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineEvent

/**
 * Desktop (JVM) audio player using `javax.sound.sampled`.
 *
 * WAV files are loaded from the classpath (Compose Resources places
 * `composeResources/files/` content on the classpath at runtime).
 * A single [Clip] is reused for sequential playback; starting a new clip
 * closes the previous one.
 *
 * Thread safety: [currentClip] access is guarded by [lock] to prevent
 * a race between the LineListener callback (audio thread) and [stop]
 * (calling thread).
 */
class DesktopAudioPlayer : AudioPlayer {

    private val lock = Any()
    private var currentClip: Clip? = null
    private var onCompleteCallback: (() -> Unit)? = null

    override fun play(resourcePath: String, onComplete: (() -> Unit)?) {
        stop()

        try {
            // Compose Resources bundles files/ content on the classpath
            val bytes = this::class.java.classLoader
                ?.getResourceAsStream("files/$resourcePath")
                ?.readBytes()
                ?: return // Resource not found; silently skip

            val audioStream = AudioSystem.getAudioInputStream(ByteArrayInputStream(bytes))
            val clip = AudioSystem.getClip()
            clip.open(audioStream)
            clip.addLineListener { event ->
                if (event.type == LineEvent.Type.STOP) {
                    synchronized(lock) {
                        // Only close if this clip is still the active one and is open.
                        // Prevents double-close when stop() races with natural completion.
                        if (currentClip === clip && clip.isOpen) {
                            clip.close()
                            currentClip = null
                            onCompleteCallback?.invoke()
                            onCompleteCallback = null
                        }
                    }
                }
            }
            synchronized(lock) {
                currentClip = clip
                onCompleteCallback = onComplete
            }
            clip.start()
        } catch (_: Exception) {
            // Audio playback is best-effort; never crash the game loop
        }
    }

    override fun stop() {
        synchronized(lock) {
            currentClip?.let { clip ->
                // Discard the completion callback on explicit stop (interruption)
                onCompleteCallback = null
                if (clip.isRunning) {
                    clip.stop()
                }
                if (clip.isOpen) {
                    clip.close()
                }
            }
            currentClip = null
        }
    }

    override fun isPlaying(): Boolean = synchronized(lock) {
        currentClip?.isRunning == true
    }

    override fun release() {
        stop()
    }
}
