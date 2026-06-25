package rpg.bark.audio

import android.media.MediaPlayer
import java.io.File
import java.io.FileInputStream

/**
 * Android audio player using [MediaPlayer] for bark WAV playback.
 *
 * On Android, Compose Resources bundles `composeResources/files/` content
 * as resources accessible at runtime via the classpath. This implementation
 * loads WAV data from the classpath and writes to a temporary file since
 * MediaPlayer requires a file descriptor or URI.
 *
 * Playback is fire-and-forget; calling [play] while audio is active
 * interrupts the previous playback.
 */
class AndroidAudioPlayer : AudioPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var tempFile: File? = null

    override fun play(resourcePath: String) {
        stop()

        try {
            // Load from classpath (Compose Resources makes files available this way)
            val bytes = this::class.java.classLoader
                ?.getResourceAsStream("files/$resourcePath")
                ?.readBytes()
                ?: return // Resource not found; silently skip

            // MediaPlayer requires a file descriptor; write to a temp file
            val tmp = File.createTempFile("bark_audio_", ".wav")
            tmp.deleteOnExit()
            tmp.writeBytes(bytes)
            tempFile = tmp

            val player = MediaPlayer()
            val fis = FileInputStream(tmp)
            player.setDataSource(fis.fd)
            fis.close()
            player.setOnCompletionListener {
                it.release()
                tmp.delete()
            }
            player.prepare()
            player.start()
            mediaPlayer = player
        } catch (_: Exception) {
            // Audio playback is best-effort; never crash the game loop
        }
    }

    override fun stop() {
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            } catch (_: Exception) {
                // Already released or in error state
            }
        }
        mediaPlayer = null
        tempFile?.delete()
        tempFile = null
    }

    override fun isPlaying(): Boolean = try {
        mediaPlayer?.isPlaying == true
    } catch (_: Exception) {
        false
    }

    override fun release() {
        stop()
    }
}
