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
 *
 * ## Temp-file approach (current)
 *
 * The classpath-based resource loading requires writing bytes to a temp file
 * because MediaPlayer needs a seekable file descriptor. This adds disk I/O
 * latency on each bark playback.
 *
 * ## Streaming alternative
 *
 * For reduced latency and memory usage, consider streaming directly via
 * AssetFileDescriptor when an Android Context is available. Compose Resources
 * maps `files/` content into Android assets, allowing:
 *
 * ```kotlin
 * // TODO: Migrate to Context-based asset streaming to avoid temp-file overhead.
 * //   val afd = context.assets.openFd("files/$resourcePath")
 * //   player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
 * //   afd.close()
 * ```
 *
 * This streams directly from the APK without intermediate copies, loading only
 * the currently needed audio data. Combined with MediaPlayer's built-in buffering,
 * this approach keeps memory at single-buffer scale regardless of total asset size.
 */
class AndroidAudioPlayer : AudioPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var tempFile: File? = null

    override fun play(resourcePath: String, onComplete: (() -> Unit)?) {
        stop()

        try {
            // Load from classpath (Compose Resources makes files available this way)
            val bytes = this::class.java.classLoader
                ?.getResourceAsStream("files/$resourcePath")
                ?.readBytes()
                ?: return // Resource not found; silently skip

            // TODO: Replace with AssetFileDescriptor approach (see class doc) to
            // eliminate temp-file I/O overhead and enable true streaming playback.
            val tmp = File.createTempFile("bark_audio_", ".wav")
            tmp.deleteOnExit()
            tmp.writeBytes(bytes)
            tempFile = tmp

            val player = MediaPlayer()
            val fis = FileInputStream(tmp)
            player.setDataSource(fis.fd)
            fis.close()
            player.setOnCompletionListener {
                onComplete?.invoke()
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
