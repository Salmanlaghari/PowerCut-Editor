package com.powercut.editor.core.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * App-wide studio configuration, persisted in SharedPreferences and exposed as
 * Compose state so both the Settings screen and the export pipeline read the
 * SAME values.
 *
 * Every field here has a REAL effect:
 *  - codecPreference      → encoder selection in VideoProcessor.videoEncodeArgs
 *  - bitratePreset        → CRF / VBV caps in videoEncodeArgs
 *  - hdrMode              → default project HDR flag for new projects/exports
 *  - audioSampleRateHz    → `-ar` on every export command
 *  - audioChannels        → `-ac` on every export command
 *  - magneticSnap         → timeline drag snapping threshold (0 when off)
 *  - defaultAspectPreset  → aspectPreset applied to newly imported projects
 *  - cacheLimitBytes      → enforced by the "Clear Asset Cache" utility
 */
object AppSettings {

    private lateinit var prefs: SharedPreferences

    // ── State (Compose-observable) ──
    var codecPreference by mutableStateOf("auto")   // auto | h264 | hevc | av1
        private set
    var bitratePreset by mutableStateOf("auto")     // auto | high | lossless
        private set
    var hdrMode by mutableStateOf("sdr")            // sdr | hdr10
        private set
    var audioSampleRateHz by mutableStateOf(48000)  // 44100 | 48000 | 96000
        private set
    var audioChannels by mutableStateOf(2)          // 1 = mono, 2 = stereo
        private set
    var magneticSnap by mutableStateOf(true)
        private set
    var defaultAspectPreset by mutableStateOf("16:9") // 16:9 | 9:16 | 1:1
        private set
    var cacheLimitBytes by mutableStateOf(1L * 1024 * 1024 * 1024) // -1 = unlimited
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences("powercut_settings", Context.MODE_PRIVATE)
        codecPreference = prefs.getString("codec_preference", "auto") ?: "auto"
        bitratePreset = prefs.getString("bitrate_preset", "auto") ?: "auto"
        hdrMode = prefs.getString("hdr_mode", "sdr") ?: "sdr"
        audioSampleRateHz = prefs.getInt("audio_sample_rate", 48000)
        audioChannels = prefs.getInt("audio_channels", 2)
        magneticSnap = prefs.getBoolean("magnetic_snap", true)
        defaultAspectPreset = prefs.getString("default_aspect", "16:9") ?: "16:9"
        cacheLimitBytes = prefs.getLong("cache_limit", 1L * 1024 * 1024 * 1024)
    }

    private fun edit(block: (SharedPreferences.Editor) -> Unit) {
        if (!::prefs.isInitialized) return
        val e = prefs.edit()
        block(e)
        e.apply()
    }

    fun updateCodecPreference(value: String) { codecPreference = value; edit { it.putString("codec_preference", value) } }
    fun updateBitratePreset(value: String) { bitratePreset = value; edit { it.putString("bitrate_preset", value) } }
    fun updateHdrMode(value: String) { hdrMode = value; edit { it.putString("hdr_mode", value) } }
    fun updateAudioSampleRateHz(value: Int) { audioSampleRateHz = value; edit { it.putInt("audio_sample_rate", value) } }
    fun updateAudioChannels(value: Int) { audioChannels = value; edit { it.putInt("audio_channels", value) } }
    fun updateMagneticSnap(value: Boolean) { magneticSnap = value; edit { it.putBoolean("magnetic_snap", value) } }
    fun updateDefaultAspectPreset(value: String) { defaultAspectPreset = value; edit { it.putString("default_aspect", value) } }
    fun updateCacheLimitBytes(value: Long) { cacheLimitBytes = value; edit { it.putLong("cache_limit", value) } }

    /** Reset everything back to defaults (Factory Reset utility). */
    fun resetToDefaults() {
        updateCodecPreference("auto"); updateBitratePreset("auto"); updateHdrMode("sdr")
        updateAudioSampleRateHz(48000); updateAudioChannels(2); updateMagneticSnap(true)
        updateDefaultAspectPreset("16:9"); updateCacheLimitBytes(1L * 1024 * 1024 * 1024)
    }
}
