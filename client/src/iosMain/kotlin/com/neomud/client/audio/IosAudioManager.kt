package com.neomud.client.audio

import com.neomud.client.platform.PlatformAudioManager
import com.neomud.client.platform.PlatformLogger
import kotlinx.coroutines.*
import platform.AVFoundation.*
import platform.Foundation.*

class IosAudioManager : PlatformAudioManager {
    private val tag = "IosAudioManager"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var bgmPlayer: AVAudioPlayer? = null
    private var currentBgmTrack: String? = null

    // Cache downloaded audio data by key
    private val sfxCache = mutableMapOf<String, NSData>()
    private val sfxLoading = mutableSetOf<String>()

    override var masterVolume: Float = 1f
        private set
    override var sfxVolume: Float = 1f
        private set
    override var bgmVolume: Float = 0.5f
        private set

    init {
        val defaults = NSUserDefaults.standardUserDefaults
        // NSUserDefaults returns 0 for unset keys — use object check to detect first launch
        if (defaults.objectForKey("volume_master") != null) {
            masterVolume = defaults.floatForKey("volume_master")
            sfxVolume = defaults.floatForKey("volume_sfx")
            bgmVolume = defaults.floatForKey("volume_bgm")
        }

        // Configure audio session for playback
        try {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, error = null)
            session.setActive(true, error = null)
        } catch (e: Exception) {
            PlatformLogger.w(tag, "Failed to configure audio session: ${e.message}")
        }
    }

    override fun playSfx(serverBaseUrl: String, soundId: String, category: String) {
        if (soundId.isBlank() || masterVolume == 0f || sfxVolume == 0f) return

        val cacheKey = "$category/$soundId"

        // Play from cache if available
        sfxCache[cacheKey]?.let { data ->
            playSfxFromData(data, cacheKey)
            return
        }

        // Don't double-load
        if (cacheKey in sfxLoading) return
        sfxLoading.add(cacheKey)

        scope.launch(Dispatchers.Default) {
            try {
                val url = "$serverBaseUrl/assets/audio/$category/$soundId.mp3"
                val nsUrl = NSURL(string = url) ?: return@launch
                val data = NSData.dataWithContentsOfURL(nsUrl) ?: return@launch

                sfxCache[cacheKey] = data
                withContext(Dispatchers.Main) {
                    playSfxFromData(data, cacheKey)
                }
            } catch (e: Exception) {
                PlatformLogger.w(tag, "Failed to load SFX '$cacheKey': ${e.message}")
            } finally {
                sfxLoading.remove(cacheKey)
            }
        }
    }

    private fun playSfxFromData(data: NSData, key: String) {
        try {
            val player = AVAudioPlayer(data = data, error = null) ?: return
            player.volume = masterVolume * sfxVolume
            player.prepareToPlay()
            player.play()
            // AVAudioPlayer is retained by the system while playing
        } catch (e: Exception) {
            PlatformLogger.w(tag, "Failed to play SFX '$key': ${e.message}")
        }
    }

    override fun playBgm(serverBaseUrl: String, trackId: String) {
        playBgmFromUri("$serverBaseUrl/assets/audio/bgm/$trackId.mp3", trackId)
    }

    override fun playBgmFromUri(uri: String, trackId: String) {
        if (trackId == currentBgmTrack) return
        if (trackId.isBlank()) {
            stopBgm()
            return
        }

        stopBgm()
        currentBgmTrack = trackId

        scope.launch(Dispatchers.Default) {
            try {
                val nsUrl = NSURL(string = uri) ?: return@launch
                val data = NSData.dataWithContentsOfURL(nsUrl) ?: return@launch

                withContext(Dispatchers.Main) {
                    val player = AVAudioPlayer(data = data, error = null) ?: return@withContext
                    player.numberOfLoops = -1 // infinite loop
                    player.volume = masterVolume * bgmVolume
                    player.prepareToPlay()
                    player.play()
                    bgmPlayer = player
                }
            } catch (e: Exception) {
                PlatformLogger.w(tag, "Failed to play BGM '$trackId': ${e.message}")
            }
        }
    }

    override fun stopBgm() {
        bgmPlayer?.stop()
        bgmPlayer = null
        currentBgmTrack = null
    }

    override fun setVolumes(master: Float, sfx: Float, bgm: Float) {
        masterVolume = master.coerceIn(0f, 1f)
        sfxVolume = sfx.coerceIn(0f, 1f)
        bgmVolume = bgm.coerceIn(0f, 1f)

        // Update BGM volume immediately
        bgmPlayer?.volume = masterVolume * bgmVolume

        // Persist
        val defaults = NSUserDefaults.standardUserDefaults
        defaults.setFloat(masterVolume, forKey = "volume_master")
        defaults.setFloat(sfxVolume, forKey = "volume_sfx")
        defaults.setFloat(bgmVolume, forKey = "volume_bgm")
        defaults.synchronize()
    }

    override fun release() {
        scope.cancel()
        stopBgm()
        sfxCache.clear()
    }
}
