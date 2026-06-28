package game

import korlibs.audio.sound.SoundChannel
import korlibs.audio.sound.readMusic
import korlibs.audio.sound.readSound
import korlibs.io.file.std.resourcesVfs

/**
 * Thin wrapper around KorGE Audio API. Handles background music (streaming)
 * and short SFX (in-memory). No own threading — KorGE audio is inherently
 * suspend-friendly.
 */
class AudioManager {
    private var musicChannel: SoundChannel? = null

    suspend fun playMusic(assetPath: String, loop: Boolean = true) {
        stopMusic()
        try {
            val sound = resourcesVfs[assetPath].readMusic()
            musicChannel = if (loop) sound.playForever() else sound.play()
        } catch (_: Exception) {
            // No audio in sandbox / headless CI — graceful degradation
        }
    }

    fun stopMusic() {
        musicChannel?.stop()
        musicChannel = null
    }

    suspend fun playSfx(assetPath: String) {
        try {
            resourcesVfs[assetPath].readSound().play()
        } catch (_: Exception) {
            // Graceful degradation — no SFX if file not found
        }
    }
}
