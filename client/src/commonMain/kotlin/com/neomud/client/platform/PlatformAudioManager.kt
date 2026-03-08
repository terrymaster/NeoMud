package com.neomud.client.platform

interface PlatformAudioManager {
    val masterVolume: Float
    val sfxVolume: Float
    val bgmVolume: Float

    fun playSfx(serverBaseUrl: String, soundId: String, category: String)
    fun playBgm(serverBaseUrl: String, trackId: String)
    fun stopBgm()
    fun setVolumes(master: Float, sfx: Float, bgm: Float)
    fun release()
}
