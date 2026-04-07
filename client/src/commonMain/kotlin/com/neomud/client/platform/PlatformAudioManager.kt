package com.neomud.client.platform

interface PlatformAudioManager {
    val masterVolume: Float
    val sfxVolume: Float
    val bgmVolume: Float

    fun playSfx(serverBaseUrl: String, soundId: String, category: String)
    /** Play an NPC interaction SFX, stopping any previous NPC sound to prevent overlap. */
    fun playNpcSfx(serverBaseUrl: String, soundId: String, category: String) = playSfx(serverBaseUrl, soundId, category)
    fun playBgm(serverBaseUrl: String, trackId: String)
    fun playBgmFromUri(uri: String, trackId: String)
    fun stopBgm()
    fun setVolumes(master: Float, sfx: Float, bgm: Float)
    fun release()
}
