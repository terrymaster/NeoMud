package com.neomud.client.audio

import com.neomud.client.platform.PlatformAudioManager

/**
 * Web audio manager — stub implementation for initial WASM bring-up.
 * TODO: Replace with real Web Audio API implementation via external JS module.
 */
class WebAudioManager : PlatformAudioManager {
    override var masterVolume: Float = 1f; private set
    override var sfxVolume: Float = 1f; private set
    override var bgmVolume: Float = 0.5f; private set

    override fun playSfx(serverBaseUrl: String, soundId: String, category: String) {
        // No-op stub — will be wired to neomud-audio.js
    }

    override fun playBgm(serverBaseUrl: String, trackId: String) {
        // No-op stub
    }

    override fun playBgmFromUri(uri: String, trackId: String) {
        // No-op stub
    }

    override fun stopBgm() {
        // No-op stub
    }

    override fun setVolumes(master: Float, sfx: Float, bgm: Float) {
        masterVolume = master
        sfxVolume = sfx
        bgmVolume = bgm
    }

    override fun release() {
        // No-op stub
    }
}
