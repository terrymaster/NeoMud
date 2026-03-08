package com.neomud.client.audio

import com.neomud.client.platform.PlatformAudioManager
import com.neomud.client.platform.PlatformLogger
import javafx.scene.media.AudioClip
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import kotlinx.coroutines.*
import java.io.File
import java.net.URI
import java.net.URL

class DesktopAudioManager : PlatformAudioManager {
    private val tag = "DesktopAudioManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var mediaPlayer: MediaPlayer? = null
    private var currentBgmTrack: String? = null

    // soundId string -> cached AudioClip
    private val sfxCache = mutableMapOf<String, AudioClip>()
    private val sfxLoading = mutableSetOf<String>()

    override var masterVolume: Float = 1f
        private set
    override var sfxVolume: Float = 1f
        private set
    override var bgmVolume: Float = 0.5f
        private set

    init {
        // Initialize JavaFX toolkit (needed for media playback outside a JavaFX Application)
        try {
            javafx.embed.swing.JFXPanel()
        } catch (_: Exception) {
            // Already initialized or unavailable
        }

        // Load persisted volume settings
        val prefs = java.util.prefs.Preferences.userNodeForPackage(DesktopAudioManager::class.java)
        masterVolume = prefs.getFloat("volume_master", 1f)
        sfxVolume = prefs.getFloat("volume_sfx", 1f)
        bgmVolume = prefs.getFloat("volume_bgm", 0.5f)
    }

    override fun playSfx(serverBaseUrl: String, soundId: String, category: String) {
        if (soundId.isBlank() || masterVolume == 0f || sfxVolume == 0f) return

        val cacheKey = "$category/$soundId"
        val cached = sfxCache[cacheKey]
        if (cached != null) {
            val vol = (masterVolume * sfxVolume).toDouble()
            cached.play(vol)
            return
        }

        if (cacheKey in sfxLoading) return
        sfxLoading.add(cacheKey)

        scope.launch {
            try {
                val url = "$serverBaseUrl/assets/audio/$category/$soundId.mp3"
                val clip = AudioClip(url)
                sfxCache[cacheKey] = clip
                val vol = (masterVolume * sfxVolume).toDouble()
                clip.play(vol)
            } catch (e: Exception) {
                PlatformLogger.w(tag, "Failed to load SFX '$category/$soundId': ${e.message}")
            } finally {
                sfxLoading.remove(cacheKey)
            }
        }
    }

    override fun playBgm(serverBaseUrl: String, trackId: String) {
        if (trackId == currentBgmTrack) return
        if (trackId.isBlank()) {
            stopBgm()
            return
        }

        stopBgm()
        currentBgmTrack = trackId

        scope.launch {
            try {
                val url = "$serverBaseUrl/assets/audio/bgm/$trackId.mp3"
                val media = Media(url)
                val mp = MediaPlayer(media)
                mp.cycleCount = MediaPlayer.INDEFINITE
                val vol = (masterVolume * bgmVolume).toDouble()
                mp.volume = vol
                mp.setOnError {
                    PlatformLogger.w(tag, "BGM error for '$trackId': ${mp.error?.message}")
                }
                withContext(Dispatchers.Main) {
                    mediaPlayer = mp
                    mp.play()
                }
            } catch (e: Exception) {
                PlatformLogger.w(tag, "Failed to load BGM '$trackId': ${e.message}")
            }
        }
    }

    override fun stopBgm() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.dispose()
        } catch (e: Exception) {
            PlatformLogger.w(tag, "Error stopping BGM: ${e.message}")
        }
        mediaPlayer = null
        currentBgmTrack = null
    }

    override fun setVolumes(master: Float, sfx: Float, bgm: Float) {
        masterVolume = master.coerceIn(0f, 1f)
        sfxVolume = sfx.coerceIn(0f, 1f)
        bgmVolume = bgm.coerceIn(0f, 1f)

        // Update BGM volume immediately
        try {
            val vol = (masterVolume * bgmVolume).toDouble()
            mediaPlayer?.volume = vol
        } catch (_: Exception) {}

        // Persist
        val prefs = java.util.prefs.Preferences.userNodeForPackage(DesktopAudioManager::class.java)
        prefs.putFloat("volume_master", masterVolume)
        prefs.putFloat("volume_sfx", sfxVolume)
        prefs.putFloat("volume_bgm", bgmVolume)
        prefs.flush()
    }

    override fun release() {
        scope.cancel()
        stopBgm()
        sfxCache.clear()
    }
}
