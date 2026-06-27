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
 * ## Temp-file caching (current)
 *
 * The classpath-based resource loading requires writing bytes to a temp file
 * because MediaPlayer needs a seekable file descriptor. To avoid re-writing the
 * same WAV on every trigger (review issue #4), decoded temp files are cached by
 * resource path: the first playback of a bark writes its temp file, and every
 * subsequent playback of the same bark reuses it. This bounds disk I/O to one
 * write per distinct bark for the lifetime of the player.
 *
 * ## Streaming alternative
 *
 * For even lower latency and memory usage, consider streaming directly via
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

    /** Cache of resource path -> on-disk temp WAV, written once and reused. */
    private val tempFileCache = mutableMapOf<String, File>()

    override fun play(resourcePath: String, onComplete: (() -> Unit)?) {
        stop()

        try {
            val tmp = tempFileForResource(resourcePath) ?: return

            val player = MediaPlayer()
            val fis = FileInputStream(tmp)
            player.setDataSource(fis.fd)
            fis.close()
            player.setOnCompletionListener {
                onComplete?.invoke()
                it.release()
                if (mediaPlayer === it) mediaPlayer = null
            }
            player.prepare()
            player.start()
            mediaPlayer = player
        } catch (_: Exception) {
            // Audio playback is best-effort; never crash the game loop
        }
    }

    /**
     * Returns the cached temp file for [resourcePath], writing it on first use.
     * Returns null if the resource cannot be found on the classpath.
     */
    private fun tempFileForResource(resourcePath: String): File? {
        tempFileCache[resourcePath]?.let { cached ->
            if (cached.exists()) return cached
            // Cache entry was evicted from disk; fall through and rewrite.
            tempFileCache.remove(resourcePath)
        }

        // Load from classpath (Compose Resources makes files available this way)
        val bytes = this::class.java.classLoader
            ?.getResourceAsStream("files/$resourcePath")
            ?.readBytes()
            ?: return null // Resource not found; silently skip

        val tmp = File.createTempFile("bark_audio_", ".wav")
        tmp.deleteOnExit()
        tmp.writeBytes(bytes)
        tempFileCache[resourcePath] = tmp
        return tmp
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
    }

    override fun isPlaying(): Boolean = try {
        mediaPlayer?.isPlaying == true
    } catch (_: Exception) {
        false
    }

    override fun release() {
        stop()
        // Drop cached temp files on full release.
        tempFileCache.values.forEach { runCatching { it.delete() } }
        tempFileCache.clear()
    }
}
