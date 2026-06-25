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
 */
class DesktopAudioPlayer : AudioPlayer {

    private var currentClip: Clip? = null

    override fun play(resourcePath: String) {
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
                    clip.close()
                }
            }
            currentClip = clip
            clip.start()
        } catch (_: Exception) {
            // Audio playback is best-effort; never crash the game loop
        }
    }

    override fun stop() {
        currentClip?.let { clip ->
            if (clip.isRunning) {
                clip.stop()
            }
            clip.close()
        }
        currentClip = null
    }

    override fun isPlaying(): Boolean = currentClip?.isRunning == true

    override fun release() {
        stop()
    }
}
