package rpg.bark.audio

/**
 * Factory function to create the platform-specific [AudioPlayer] implementation.
 *
 * Desktop returns [DesktopAudioPlayer] (javax.sound.sampled),
 * Android returns [AndroidAudioPlayer] (MediaPlayer).
 */
expect fun createPlatformAudioPlayer(): AudioPlayer
