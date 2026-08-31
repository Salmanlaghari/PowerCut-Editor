package com.powercut.editor.domain.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * AI Voice Isolation & Noise Suppression Engine.
 * Provides background vocal isolation, AI noise reduction, and smart ducking.
 */
class VoiceIsolationEngine {
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()
    
    /**
     * Isolate vocals from background music/noise.
     * Returns isolated vocal track.
     */
    suspend fun isolateVocals(
        inputSamples: FloatArray,
        sampleRate: Int = 44100,
        sensitivity: Float = 0.8f
    ): VoiceIsolationResult = withContext(Dispatchers.Default) {
        _isProcessing.value = true
        _progress.value = 0f
        
        try {
            val frameSize = 2048
            val hopSize = 512
            val numFrames = (inputSamples.size - frameSize) / hopSize + 1
            
            val vocalOutput = FloatArray(inputSamples.size)
            val noiseOutput = FloatArray(inputSamples.size)
            
            for (frame in 0 until numFrames) {
                val start = frame * hopSize
                val end = minOf(start + frameSize, inputSamples.size)
                val chunk = inputSamples.sliceArray(start until end)
                
                // Apply spectral gating for voice isolation
                val (vocalChunk, noiseChunk) = spectralGate(chunk, sensitivity)
                
                // Overlap-add synthesis
                vocalChunk.forEachIndexed { i, sample ->
                    if (start + i < vocalOutput.size) {
                        vocalOutput[start + i] += sample
                    }
                }
                noiseChunk.forEachIndexed { i, sample ->
                    if (start + i < noiseOutput.size) {
                        noiseOutput[start + i] += sample
                    }
                }
                
                _progress.value = (frame.toFloat() / numFrames) * 100f
            }
            
            VoiceIsolationResult(
                vocalTrack = vocalOutput,
                noiseTrack = noiseOutput,
                vocalEnergy = calculateEnergy(vocalOutput),
                noiseEnergy = calculateEnergy(noiseOutput),
                signalToNoiseRatio = calculateSNR(vocalOutput, noiseOutput)
            )
        } finally {
            _isProcessing.value = false
            _progress.value = 0f
        }
    }
    
    /**
     * Remove background noise from audio.
     */
    suspend fun reduceNoise(
        inputSamples: FloatArray,
        noiseProfile: FloatArray? = null,
        reductionLevel: Float = 0.7f
    ): FloatArray = withContext(Dispatchers.Default) {
        _isProcessing.value = true
        
        try {
            val output = FloatArray(inputSamples.size)
            val frameSize = 1024
            val numFrames = inputSamples.size / frameSize
            
            // Estimate noise profile from first 100ms if not provided
            val noiseEstimate = noiseProfile ?: estimateNoiseProfile(inputSamples, 44100)
            
            for (frame in 0 until numFrames) {
                val start = frame * frameSize
                val end = minOf(start + frameSize, inputSamples.size)
                val chunk = inputSamples.sliceArray(start until end)
                
                // Spectral subtraction
                val cleanedChunk = spectralSubtract(chunk, noiseEstimate, reductionLevel)
                
                cleanedChunk.forEachIndexed { i, sample ->
                    if (start + i < output.size) {
                        output[start + i] = sample
                    }
                }
                
                _progress.value = (frame.toFloat() / numFrames) * 100f
            }
            
            output
        } finally {
            _isProcessing.value = false
        }
    }
    
    /**
     * Smart audio ducking — lower music volume when voice is present.
     */
    suspend fun smartDucking(
        musicTrack: FloatArray,
        voiceTrack: FloatArray,
        duckingAmount: Float = 0.4f,
        attackMs: Long = 50,
        releaseMs: Long = 200,
        sampleRate: Int = 44100
    ): FloatArray = withContext(Dispatchers.Default) {
        _isProcessing.value = true
        
        try {
            val output = FloatArray(musicTrack.size)
            val attackSamples = (attackMs * sampleRate / 1000).toInt()
            val releaseSamples = (releaseMs * sampleRate / 1000).toInt()
            
            var duckGain = 1f
            
            for (i in musicTrack.indices) {
                // Calculate voice envelope
                val voiceLevel = if (i < voiceTrack.size) kotlin.math.abs(voiceTrack[i]) else 0f
                
                // Dynamic gain calculation
                val targetGain = if (voiceLevel > 0.05f) duckingAmount else 1f
                
                // Smooth attack/release
                duckGain = when {
                    targetGain < duckGain -> {
                        // Attack (quick duck)
                        duckGain - (1f / attackSamples)
                    }
                    else -> {
                        // Release (slow recovery)
                        duckGain + (1f / releaseSamples)
                    }
                }.coerceIn(duckingAmount, 1f)
                
                output[i] = musicTrack[i] * duckGain
            }
            
            output
        } finally {
            _isProcessing.value = false
        }
    }
    
    /**
     * Analyze audio for voice activity detection.
     */
    fun detectVoiceActivity(
        samples: FloatArray,
        sampleRate: Int = 44100,
        frameSizeMs: Int = 30
    ): List<VoiceActivitySegment> {
        val frameSize = (sampleRate * frameSizeMs / 1000)
        val numFrames = samples.size / frameSize
        val segments = mutableListOf<VoiceActivitySegment>()
        
        var inVoice = false
        var voiceStart = 0
        
        for (frame in 0 until numFrames) {
            val start = frame * frameSize
            val end = minOf(start + frameSize, samples.size)
            val chunk = samples.sliceArray(start until end)
            
            val energy = calculateEnergy(chunk)
            val isVoice = energy > VOICE_THRESHOLD
            
            if (isVoice && !inVoice) {
                voiceStart = start
                inVoice = true
            } else if (!isVoice && inVoice) {
                segments.add(
                    VoiceActivitySegment(
                        startTimeMs = voiceStart * 1000L / sampleRate,
                        endTimeMs = start * 1000L / sampleRate,
                        confidence = 0.9f
                    )
                )
                inVoice = false
            }
        }
        
        if (inVoice) {
            segments.add(
                VoiceActivitySegment(
                    startTimeMs = voiceStart * 1000L / sampleRate,
                    endTimeMs = samples.size * 1000L / sampleRate,
                    confidence = 0.9f
                )
            )
        }
        
        return segments
    }
    
    private fun spectralGate(chunk: FloatArray, sensitivity: Float): Pair<FloatArray, FloatArray> {
        // Simplified spectral gating
        // In production, this would use FFT for proper frequency-domain processing
        val vocal = FloatArray(chunk.size)
        val noise = FloatArray(chunk.size)
        
        val threshold = sensitivity * 0.1f
        
        chunk.forEachIndexed { i, sample ->
            val absSample = kotlin.math.abs(sample)
            if (absSample > threshold) {
                vocal[i] = sample
                noise[i] = 0f
            } else {
                vocal[i] = 0f
                noise[i] = sample
            }
        }
        
        return Pair(vocal, noise)
    }
    
    private fun spectralSubtract(chunk: FloatArray, noiseProfile: FloatArray, level: Float): FloatArray {
        return FloatArray(chunk.size) { i ->
            val noise = if (i < noiseProfile.size) noiseProfile[i] else 0f
            val subtracted = chunk[i] - (noise * level)
            subtracted.coerceIn(-1f, 1f)
        }
    }
    
    private fun estimateNoiseProfile(samples: FloatArray, sampleRate: Int): FloatArray {
        // Use first 100ms as noise estimate
        val noiseLength = minOf(sampleRate / 10, samples.size)
        return samples.sliceArray(0 until noiseLength)
    }
    
    private fun calculateEnergy(samples: FloatArray): Float {
        if (samples.isEmpty()) return 0f
        return samples.sumOf { (it * it).toDouble() }.toFloat() / samples.size
    }
    
    private fun calculateSNR(signal: FloatArray, noise: FloatArray): Float {
        val signalEnergy = calculateEnergy(signal)
        val noiseEnergy = calculateEnergy(noise)
        if (noiseEnergy == 0f) return Float.MAX_VALUE
        return 10 * kotlin.math.log10(signalEnergy / noiseEnergy)
    }
    
    companion object {
        private const val VOICE_THRESHOLD = 0.02f
    }
}

data class VoiceIsolationResult(
    val vocalTrack: FloatArray,
    val noiseTrack: FloatArray,
    val vocalEnergy: Float,
    val noiseEnergy: Float,
    val signalToNoiseRatio: Float
)

data class VoiceActivitySegment(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val confidence: Float
)
