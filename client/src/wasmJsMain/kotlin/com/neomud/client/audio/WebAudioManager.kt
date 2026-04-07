package com.neomud.client.audio

import com.neomud.client.platform.PlatformAudioManager

// JS interop — calls into neomud-audio.js loaded via index.html
private fun jsPlaySfx(url: String): Unit = js("NeoMudAudio.playSfx(url)")
private fun jsPlayNpcSfx(url: String): Unit = js("NeoMudAudio.playNpcSfx(url)")
private fun jsPlayBgm(url: String): Unit = js("NeoMudAudio.playBgm(url)")
private fun jsStopBgm(): Unit = js("NeoMudAudio.stopBgm()")
private fun jsSetVolumes(master: Float, sfx: Float, bgm: Float): Unit =
    js("NeoMudAudio.setVolumes(master, sfx, bgm)")
private fun jsGetMaster(): Float = js("NeoMudAudio.getVolumes().master")
private fun jsGetSfx(): Float = js("NeoMudAudio.getVolumes().sfx")
private fun jsGetBgm(): Float = js("NeoMudAudio.getVolumes().bgm")

class WebAudioManager : PlatformAudioManager {
    override val masterVolume: Float get() = jsGetMaster()
    override val sfxVolume: Float get() = jsGetSfx()
    override val bgmVolume: Float get() = jsGetBgm()

    override fun playSfx(serverBaseUrl: String, soundId: String, category: String) {
        if (soundId.isBlank()) return
        val url = "$serverBaseUrl/assets/audio/$category/$soundId.mp3"
        jsPlaySfx(url)
    }

    override fun playNpcSfx(serverBaseUrl: String, soundId: String, category: String) {
        if (soundId.isBlank()) return
        val url = "$serverBaseUrl/assets/audio/$category/$soundId.mp3"
        jsPlayNpcSfx(url)
    }

    override fun playBgm(serverBaseUrl: String, trackId: String) {
        if (trackId.isBlank()) { stopBgm(); return }
        val url = "$serverBaseUrl/assets/audio/bgm/$trackId.mp3"
        jsPlayBgm(url)
    }

    override fun playBgmFromUri(uri: String, trackId: String) {
        if (trackId.isBlank() || uri.isBlank()) { stopBgm(); return }
        jsPlayBgm(uri)
    }

    override fun stopBgm() {
        jsStopBgm()
    }

    override fun setVolumes(master: Float, sfx: Float, bgm: Float) {
        jsSetVolumes(master, sfx, bgm)
    }

    override fun release() {
        stopBgm()
    }
}
