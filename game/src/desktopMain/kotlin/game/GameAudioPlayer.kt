package game

import korlibs.audio.sound.readSound
import korlibs.io.file.std.resourcesVfs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import rpg.bark.audio.AudioPlayer

/**
 * AudioPlayer implementation for :game using KorGE audio API.
 *
 * Bark WAVs live at assets/audio/bark/<path> since Step 6a.
 * BarkAudioRegistry.pathFor() returns paths WITHOUT the "assets/audio/" prefix
 * (e.g. "bark/barkeep/spend_some_coin_or_get_out.wav") — this class adds the prefix.
 */
class GameAudioPlayer(private val scope: CoroutineScope) : AudioPlayer {
    private var currentJob: Job? = null

    override fun play(resourcePath: String, onComplete: (() -> Unit)?) {
        stop()
        currentJob = scope.launch {
            try {
                val fullPath = "assets/audio/$resourcePath"
                resourcesVfs[fullPath].readSound().play()
                onComplete?.invoke()
            } catch (_: Exception) { /* no audio in CI — graceful degradation */ }
        }
    }

    override fun stop() {
        currentJob?.cancel()
        currentJob = null
    }

    override fun isPlaying(): Boolean = currentJob?.isActive == true

    override fun release() = stop()
}
