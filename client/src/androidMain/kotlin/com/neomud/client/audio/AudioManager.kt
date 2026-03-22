package com.neomud.client.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.neomud.client.platform.PlatformAudioManager
import com.neomud.client.platform.PlatformLogger
import kotlinx.coroutines.*
import java.io.File
import java.net.URL

class AudioManager(context: Context) : PlatformAudioManager {
    private val tag = "AudioManager"
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var mediaPlayer: MediaPlayer? = null
    private var currentBgmTrack: String? = null

    // soundId string -> SoundPool loaded ID
    private val sfxCache = mutableMapOf<String, Int>()
    // soundId strings currently being loaded (avoid duplicate downloads)
    private val sfxLoading = mutableSetOf<String>()
    // SoundPool IDs pending first play after load completes
    private val pendingFirstPlay = mutableSetOf<Int>()

    override var masterVolume: Float = 1f
        private set
    override var sfxVolume: Float = 1f
        private set
    override var bgmVolume: Float = 0.5f
        private set

    init {
        val prefs = appContext.getSharedPreferences("neomud_audio", Context.MODE_PRIVATE)
        masterVolume = prefs.getFloat("volume_master", 1f)
        sfxVolume = prefs.getFloat("volume_sfx", 1f)
        bgmVolume = prefs.getFloat("volume_bgm", 0.5f)

        // Single listener handles all first-play-after-load events
        soundPool.setOnLoadCompleteListener { pool, sampleId, status ->
            if (status == 0 && pendingFirstPlay.remove(sampleId)) {
                val vol = masterVolume * sfxVolume
                pool.play(sampleId, vol, vol, 1, 0, 1f)
            }
        }
    }

    override fun playSfx(serverBaseUrl: String, soundId: String, category: String) {
        if (soundId.isBlank() || masterVolume == 0f || sfxVolume == 0f) return

        val cacheKey = "$category/$soundId"
        val cached = sfxCache[cacheKey]
        if (cached != null) {
            val vol = masterVolume * sfxVolume
            soundPool.play(cached, vol, vol, 1, 0, 1f)
            return
        }

        // Don't double-download
        if (cacheKey in sfxLoading) return
        sfxLoading.add(cacheKey)

        scope.launch {
            try {
                val url = "$serverBaseUrl/assets/audio/$category/$soundId.mp3"
                val tempFile = File(appContext.cacheDir, "sfx_${category}_$soundId.mp3")
                if (!tempFile.exists()) {
                    URL(url).openStream().use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                val loadedId = soundPool.load(tempFile.absolutePath, 1)
                sfxCache[cacheKey] = loadedId
                pendingFirstPlay.add(loadedId)
            } catch (e: Exception) {
                PlatformLogger.w(tag, "Failed to load SFX '$category/$soundId': ${e.message}")
            } finally {
                sfxLoading.remove(cacheKey)
            }
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

        scope.launch {
            try {
                withContext(Dispatchers.Main) {
                    val mp = MediaPlayer()
                    mp.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )

                    val assetPrefix = "file:///android_asset/"
                    if (uri.startsWith(assetPrefix)) {
                        // Compose Multiplatform resource URI — strip the virtual prefix
                        // and use AssetManager to get a file descriptor that MediaPlayer
                        // can play directly from the APK.
                        val assetPath = uri.removePrefix(assetPrefix)
                        val afd = appContext.assets.openFd(assetPath)
                        mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        afd.close()
                    } else {
                        mp.setDataSource(uri)
                    }

                    mp.isLooping = true
                    val vol = masterVolume * bgmVolume
                    mp.setVolume(vol, vol)
                    mp.setOnPreparedListener { it.start() }
                    mp.setOnErrorListener { _, what, extra ->
                        PlatformLogger.w(tag, "BGM error for '$trackId': what=$what extra=$extra")
                        true
                    }
                    mp.prepareAsync()
                    mediaPlayer = mp
                }
            } catch (e: Exception) {
                PlatformLogger.w(tag, "Failed to load BGM '$trackId': ${e.message}")
            }
        }
    }

    override fun stopBgm() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                reset()
                release()
            }
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
            val vol = masterVolume * bgmVolume
            mediaPlayer?.setVolume(vol, vol)
        } catch (_: Exception) {}

        // Persist
        appContext.getSharedPreferences("neomud_audio", Context.MODE_PRIVATE)
            .edit()
            .putFloat("volume_master", masterVolume)
            .putFloat("volume_sfx", sfxVolume)
            .putFloat("volume_bgm", bgmVolume)
            .apply()
    }

    override fun release() {
        scope.cancel()
        stopBgm()
        soundPool.release()
    }
}
