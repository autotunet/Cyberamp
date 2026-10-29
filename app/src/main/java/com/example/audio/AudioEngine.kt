package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.util.Log
import com.example.model.AmplifierState
import com.example.model.ParametricBand
import com.example.model.TrackItem
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "CyberAmpAudioEngine"

data class TelemetrySnapshot(
    val spectrum64: FloatArray = FloatArray(64) { 0.05f },
    val waveformLeft: FloatArray = FloatArray(64) { 0f },
    val waveformRight: FloatArray = FloatArray(64) { 0f },
    val vuLeftDb: Float = -48f,
    val vuRightDb: Float = -48f,
    val peakClipping: Boolean = false,
    val dynamicRangeLu: Float = 14.2f,
    val spatial360AzimuthDeg: Float = 0f,
    val spatialRadarNodes: FloatArray = floatArrayOf(0.8f, 0.8f, 0.6f, 0.7f, 0.7f, 0.9f)
)

/**
 * Real-time DSP & Procedural/Local Audio Engine for CyberAmp.
 * Includes a 360-degree Binaural HRTF (ITD Haas delay + ILD head-shadow + all-pass phase) filter
 * alongside the 10-band parametric EQ and EL34 tube saturation stage.
 */
class CyberAmpAudioEngine(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _telemetry = MutableStateFlow(TelemetrySnapshot())
    val telemetry: StateFlow<TelemetrySnapshot> = _telemetry.asStateFlow()

    private val _currentPositionSec = MutableStateFlow(0f)
    val currentPositionSec: StateFlow<Float> = _currentPositionSec.asStateFlow()

    @Volatile
    private var isPlaying: Boolean = false

    @Volatile
    private var currentTrack: TrackItem? = null

    @Volatile
    private var currentBands: List<ParametricBand> = emptyList()

    @Volatile
    private var currentAmp: AmplifierState = AmplifierState()

    private var synthesisJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null

    private var hwEqualizer: Equalizer? = null
    private var hwBassBoost: BassBoost? = null
    private var hwVirtualizer: Virtualizer? = null
    private var hwLoudness: LoudnessEnhancer? = null

    fun updateDspParameters(bands: List<ParametricBand>, amp: AmplifierState) {
        currentBands = bands
        currentAmp = amp
        applyHardwareEffects(bands, amp)
        mediaPlayer?.setVolume(
            (amp.masterVolume * (1f - max(0f, amp.balancePan))).coerceIn(0f, 1f),
            (amp.masterVolume * (1f + min(0f, amp.balancePan))).coerceIn(0f, 1f)
        )
    }

    fun startTrack(track: TrackItem, startPositionSec: Float = 0f) {
        stopPlaybackInternal()
        currentTrack = track
        _currentPositionSec.value = startPositionSec
        isPlaying = true

        val uriStr = track.localUriString
        if (!uriStr.isNullOrBlank()) {
            startLocalUriPlayback(Uri.parse(uriStr), startPositionSec)
        } else {
            startProceduralDspPlayback(track)
        }
    }

    fun setPlaying(playing: Boolean) {
        if (playing == isPlaying) return
        isPlaying = playing
        if (playing) {
            val trk = currentTrack
            if (trk != null) {
                if (mediaPlayer != null) {
                    runCatching { mediaPlayer?.start() }
                    startMediaPlayerTelemetryLoop()
                } else {
                    startProceduralDspPlayback(trk)
                }
            }
        } else {
            runCatching { mediaPlayer?.pause() }
            synthesisJob?.cancel()
            _telemetry.value = TelemetrySnapshot(
                spatial360AzimuthDeg = currentAmp.spatialAzimuthDeg
            )
        }
    }

    fun seekTo(seconds: Float) {
        val trk = currentTrack ?: return
        val clamped = seconds.coerceIn(0f, trk.durationSec.toFloat())
        _currentPositionSec.value = clamped
        mediaPlayer?.let { mp ->
            runCatching { mp.seekTo((clamped * 1000).toInt()) }
        }
    }

    private fun startLocalUriPlayback(uri: Uri, startSec: Float) {
        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(context, uri)
                prepare()
                if (startSec > 0f) {
                    seekTo((startSec * 1000).toInt())
                }
                start()
            }
            mediaPlayer = mp
            attachHardwareEffects(mp.audioSessionId)
            applyHardwareEffects(currentBands, currentAmp)
            startMediaPlayerTelemetryLoop()
        } catch (e: Exception) {
            Log.w(TAG, "Fallback to DSP synth engine for URI: $uri", e)
            currentTrack?.let { startProceduralDspPlayback(it) }
        }
    }

    private fun startMediaPlayerTelemetryLoop() {
        synthesisJob?.cancel()
        synthesisJob = scope.launch(Dispatchers.Default) {
            var phase = 0.0
            while (isActive && isPlaying) {
                val mp = mediaPlayer
                if (mp != null && mp.isPlaying) {
                    _currentPositionSec.value = mp.currentPosition / 1000f
                }
                emitSynthesizedTelemetry(phase)
                phase += 0.18
                delay(45L)
            }
        }
    }

    private fun startProceduralDspPlayback(track: TrackItem) {
        synthesisJob?.cancel()
        synthesisJob = scope.launch(Dispatchers.Default) {
            val sampleRate = 24000
            val bufferSamples = 1200 // 50ms stereo frames
            val audioTrack = runCatching {
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(bufferSamples * 4)

                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            }.getOrNull()

            if (audioTrack != null && audioTrack.state == AudioTrack.STATE_INITIALIZED) {
                attachHardwareEffects(audioTrack.audioSessionId)
                applyHardwareEffects(currentBands, currentAmp)
                runCatching { audioTrack.play() }
            }

            val pcmBuffer = ShortArray(bufferSamples * 2)
            val leftWave = FloatArray(64)
            val rightWave = FloatArray(64)
            val spectrum = FloatArray(64)

            // Circular delay line for 360-degree Binaural Haas / ITD & Early Reflections (max 32ms)
            val delayLineSize = 1024
            val delayLineL = DoubleArray(delayLineSize)
            val delayLineR = DoubleArray(delayLineSize)
            var delayWriteIdx = 0

            val chordProgressions = arrayOf(
                doubleArrayOf(110.0, 130.81, 164.81, 196.0), // A minor 7 Cyber Odyssey
                doubleArrayOf(82.41, 98.0, 123.47, 146.83),  // E minor Quantum Sub-Bass
                doubleArrayOf(146.83, 174.61, 220.0, 261.63),// D minor 7 Tokyo Valve Jazz
                doubleArrayOf(130.81, 164.81, 196.0, 246.94),// C maj 7 Solar Horizon Ambient
                doubleArrayOf(98.0, 116.54, 146.83, 174.61)  // G minor Outrun Overdrive
            )
            val chords = chordProgressions[track.synthProfileId % chordProgressions.size]
            var sampleCursor = (_currentPositionSec.value * sampleRate).toLong()

            try {
                while (isActive && isPlaying) {
                    val amp = currentAmp
                    val bands = currentBands

                    val lowGainLinear = computeBandRegionLinearGain(bands, 0..2) * (1f + amp.subBassBoost * 0.85f)
                    val midGainLinear = computeBandRegionLinearGain(bands, 3..6)
                    val highGainLinear = computeBandRegionLinearGain(bands, 7..9) * (1f + amp.loudnessEnhancer * 0.5f)
                    val preampLinear = 10f.pow(amp.preampGainDb / 20f)
                    val tubeDrive = 1f + amp.tubeWarmth * 2.6f
                    val tempoRatio = amp.pitchTempoRatio.coerceIn(0.75f, 1.25f)
                    val beatsPerSec = (track.bpm / 60.0) * tempoRatio

                    // Compute 360-degree Spatial Audio Azimuth & ITD/ILD Filter Coefficients
                    val currentTimeSec = sampleCursor.toDouble() / sampleRate.toDouble()
                    val activeAzimuthDeg = if (amp.spatial360Enabled && amp.spatialAutoOrbit) {
                        ((amp.spatialAzimuthDeg + currentTimeSec * 28.0) % 360.0).toFloat()
                    } else {
                        amp.spatialAzimuthDeg
                    }
                    val azimuthRad = activeAzimuthDeg * (PI / 180.0)
                    val sinAz = sin(azimuthRad)
                    val cosAz = cos(azimuthRad)

                    // Interaural Time Difference (ITD) in samples (up to 18 samples ~ 0.75ms head radius)
                    val itdSamplesL = if (sinAz > 0) (sinAz * 18.0).toInt() else 0
                    val itdSamplesR = if (sinAz < 0) (-sinAz * 18.0).toInt() else 0
                    // Early room reflection tap (11ms to 24ms)
                    val roomTapSamples = (220 + (amp.surroundRoomSize * 340).toInt()).coerceIn(64, delayLineSize - 2)

                    var sumSqL = 0.0
                    var sumSqR = 0.0
                    var clipped = false

                    for (i in 0 until bufferSamples) {
                        val t = (sampleCursor + i).toDouble() / sampleRate.toDouble()
                        val beatPhase = t * beatsPerSec
                        val barIndex = (beatPhase / 4.0).toInt()
                        val rootFreq = chords[barIndex % chords.size] * tempoRatio

                        // Sub-bass + Kick envelope
                        val beatFrac = beatPhase - beatPhase.toInt()
                        val kickEnv = exp(-beatFrac * 7.5)
                        val bassWave = sin(2.0 * PI * (rootFreq * 0.5 + 35.0 * kickEnv) * t) *
                            (0.45 + 0.35 * kickEnv) * lowGainLinear

                        // Warm harmonic pad / arpeggio mid
                        val arpStep = ((beatPhase * 2.0).toInt()) % 4
                        val arpMultiplier = when (arpStep) {
                            0 -> 1.0
                            1 -> 1.25
                            2 -> 1.5
                            else -> 2.0
                        }
                        val midWave = (
                            sin(2.0 * PI * rootFreq * arpMultiplier * t) * 0.25 +
                                sin(2.0 * PI * rootFreq * 1.5 * t) * 0.16
                            ) * midGainLinear

                        // Shimmering high-res air harmonics
                        val highEnv = 0.5 + 0.5 * sin(2.0 * PI * (beatsPerSec * 2.0) * t)
                        val highWave = sin(2.0 * PI * rootFreq * 4.0 * t) * 0.12 * highEnv * highGainLinear

                        var rawL = (bassWave + midWave + highWave) * preampLinear
                        var rawR = (bassWave + midWave * 0.96 + highWave * 0.92) * preampLinear

                        // Write dry signal into circular binaural delay buffer
                        delayLineL[delayWriteIdx] = rawL
                        delayLineR[delayWriteIdx] = rawR

                        if (amp.spatial360Enabled || amp.surroundEnabled) {
                            // 360-degree Binaural HRTF + Haas Delay + Rear-Hemisphere Phase Cue
                            val readIdxItdL = (delayWriteIdx - itdSamplesL + delayLineSize) % delayLineSize
                            val readIdxItdR = (delayWriteIdx - itdSamplesR + delayLineSize) % delayLineSize
                            val readIdxRefl = (delayWriteIdx - roomTapSamples + delayLineSize) % delayLineSize

                            // Interaural Level Difference (ILD) head shadow
                            val ildGainL = 1.0 - 0.22 * sinAz
                            val ildGainR = 1.0 + 0.22 * sinAz
                            // Front/Back elevation pinna cue via cos(azimuth)
                            val pinnaCue = 0.88 + 0.12 * cosAz

                            val reflMix = 0.28 * amp.surroundRoomSize
                            // Cross-channel binaural early reflections
                            rawL = delayLineL[readIdxItdL] * ildGainL * pinnaCue +
                                delayLineR[readIdxRefl] * reflMix -
                                delayLineL[readIdxRefl] * (reflMix * 0.35)
                            rawR = delayLineR[readIdxItdR] * ildGainR * pinnaCue +
                                delayLineL[readIdxRefl] * reflMix -
                                delayLineR[readIdxRefl] * (reflMix * 0.35)
                        }

                        delayWriteIdx = (delayWriteIdx + 1) % delayLineSize

                        // Mid/Side Stereo Width Matrix
                        val mid = (rawL + rawR) * 0.5
                        val side = (rawL - rawR) * 0.5 * amp.stereoWidth
                        rawL = mid + side
                        rawR = mid - side

                        // EL34 Vacuum Tube soft-clipping saturation
                        if (amp.tubeWarmth > 0.02f) {
                            rawL = tanh(rawL * tubeDrive) / tanh(tubeDrive.toDouble())
                            rawR = tanh(rawR * tubeDrive) / tanh(tubeDrive.toDouble())
                        }

                        // Balance & Master volume
                        val panL = (1f - max(0f, amp.balancePan)).toDouble()
                        val panR = (1f + min(0f, amp.balancePan)).toDouble()
                        val outL = rawL * amp.masterVolume * panL
                        val outR = rawR * amp.masterVolume * panR

                        if (abs(outL) > 0.98 || abs(outR) > 0.98) {
                            clipped = true
                        }

                        val clampedL = outL.coerceIn(-0.99, 0.99)
                        val clampedR = outR.coerceIn(-0.99, 0.99)

                        sumSqL += clampedL * clampedL
                        sumSqR += clampedR * clampedR

                        if (i < 64) {
                            leftWave[i] = clampedL.toFloat()
                            rightWave[i] = clampedR.toFloat()
                        }

                        pcmBuffer[i * 2] = (clampedL * 32760).toInt().toShort()
                        pcmBuffer[i * 2 + 1] = (clampedR * 32760).toInt().toShort()
                    }

                    if (audioTrack != null && audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        audioTrack.write(pcmBuffer, 0, pcmBuffer.size)
                    } else {
                        delay(50L)
                    }

                    sampleCursor += (bufferSamples * tempoRatio).toLong()
                    val newPosSec = (sampleCursor.toFloat() / sampleRate.toFloat())
                    if (newPosSec >= track.durationSec) {
                        sampleCursor = 0L
                        _currentPositionSec.value = 0f
                    } else {
                        _currentPositionSec.value = newPosSec
                    }

                    val rmsL = sqrt(sumSqL / bufferSamples).toFloat()
                    val rmsR = sqrt(sumSqR / bufferSamples).toFloat()
                    val phase = sampleCursor.toDouble() / sampleRate.toDouble()
                    for (b in 0 until 64) {
                        val normFreq = b / 63f
                        val bandIdx = (normFreq * 9f).toInt().coerceIn(0, 9)
                        val eqBoost = if (bands.size > bandIdx) {
                            10f.pow(bands[bandIdx].gainDb / 24f)
                        } else 1f
                        val pulse = abs(sin(phase * (2.2 + b * 0.23) + b * 0.4)).toFloat()
                        val envelope = (1f - normFreq * 0.45f) * (rmsL + rmsR + 0.18f)
                        spectrum[b] = (envelope * pulse * eqBoost * (0.65f + amp.subBassBoost * 0.35f))
                            .coerceIn(0.04f, 1.0f)
                    }

                    val vuL = (20f * log10(rmsL.coerceAtLeast(0.001f))).coerceIn(-48f, 3f)
                    val vuR = (20f * log10(rmsR.coerceAtLeast(0.001f))).coerceIn(-48f, 3f)

                    // 6-channel 360 surround radar node energies (FL, FR, C, RL, RR, LFE)
                    val radarNodes = FloatArray(6) { ch ->
                        val chAngle = ch * (PI / 3.0)
                        val alignment = 0.55f + 0.45f * cos(azimuthRad - chAngle).toFloat()
                        ((rmsL + rmsR + 0.25f) * alignment * (0.7f + 0.3f * amp.surroundRoomSize))
                            .coerceIn(0.15f, 1.0f)
                    }

                    _telemetry.value = TelemetrySnapshot(
                        spectrum64 = spectrum.copyOf(),
                        waveformLeft = leftWave.copyOf(),
                        waveformRight = rightWave.copyOf(),
                        vuLeftDb = vuL,
                        vuRightDb = vuR,
                        peakClipping = clipped || amp.preampGainDb > 9.5f,
                        dynamicRangeLu = (14.5f - amp.tubeWarmth * 3.2f + amp.stereoWidth * 1.1f).coerceIn(6f, 20f),
                        spatial360AzimuthDeg = activeAzimuthDeg,
                        spatialRadarNodes = radarNodes
                    )
                }
            } finally {
                runCatching {
                    audioTrack?.stop()
                    audioTrack?.release()
                }
            }
        }
    }

    private fun emitSynthesizedTelemetry(phase: Double) {
        val bands = currentBands
        val amp = currentAmp
        val spec = FloatArray(64)
        val waveL = FloatArray(64)
        val waveR = FloatArray(64)
        for (i in 0 until 64) {
            val norm = i / 63f
            val bandIdx = (norm * 9f).toInt().coerceIn(0, 9)
            val eqGain = if (bands.size > bandIdx) 10f.pow(bands[bandIdx].gainDb / 24f) else 1f
            spec[i] = (abs(sin(phase * (1.8 + i * 0.19))) * 0.65 * eqGain * amp.masterVolume)
                .toFloat().coerceIn(0.05f, 1f)
            waveL[i] = (sin(phase * 3.0 + i * 0.25) * 0.6 * amp.masterVolume).toFloat()
            waveR[i] = (cos(phase * 3.0 + i * 0.25) * 0.6 * amp.masterVolume).toFloat()
        }
        val activeAzimuth = if (amp.spatial360Enabled && amp.spatialAutoOrbit) {
            ((amp.spatialAzimuthDeg + phase * 18.0) % 360.0).toFloat()
        } else {
            amp.spatialAzimuthDeg
        }
        _telemetry.value = TelemetrySnapshot(
            spectrum64 = spec,
            waveformLeft = waveL,
            waveformRight = waveR,
            vuLeftDb = -6f + (sin(phase * 4.0).toFloat() * 4f),
            vuRightDb = -6f + (cos(phase * 4.0).toFloat() * 4f),
            peakClipping = amp.preampGainDb > 9.5f,
            dynamicRangeLu = 13.8f,
            spatial360AzimuthDeg = activeAzimuth
        )
    }

    private fun computeBandRegionLinearGain(bands: List<ParametricBand>, range: IntRange): Float {
        if (bands.isEmpty()) return 1f
        var avgDb = 0f
        var count = 0
        for (i in range) {
            if (i in bands.indices) {
                avgDb += bands[i].gainDb
                count++
            }
        }
        if (count == 0) return 1f
        return 10f.pow((avgDb / count) / 20f)
    }

    private fun attachHardwareEffects(audioSessionId: Int) {
        releaseHardwareEffects()
        runCatching {
            hwEqualizer = Equalizer(0, audioSessionId).apply { enabled = true }
        }
        runCatching {
            hwBassBoost = BassBoost(0, audioSessionId).apply { enabled = true }
        }
        runCatching {
            hwVirtualizer = Virtualizer(0, audioSessionId).apply { enabled = true }
        }
        runCatching {
            hwLoudness = LoudnessEnhancer(audioSessionId).apply { enabled = true }
        }
    }

    private fun applyHardwareEffects(bands: List<ParametricBand>, amp: AmplifierState) {
        runCatching {
            val eq = hwEqualizer
            if (eq != null && bands.isNotEmpty()) {
                val numBands = eq.numberOfBands.toInt()
                val range = eq.bandLevelRange
                val minLevel = range[0].toInt()
                val maxLevel = range[1].toInt()
                for (b in 0 until numBands) {
                    val mappedIdx = ((b.toFloat() / max(1, numBands - 1)) * (bands.size - 1)).toInt()
                    val db = bands[mappedIdx].gainDb
                    val millibel = (db * 100f).toInt().coerceIn(minLevel, maxLevel).toShort()
                    eq.setBandLevel(b.toShort(), millibel)
                }
            }
        }
        runCatching {
            hwBassBoost?.setStrength((amp.subBassBoost * 1000).toInt().coerceIn(0, 1000).toShort())
        }
        runCatching {
            val spatialActive = amp.surroundEnabled || amp.spatial360Enabled
            hwVirtualizer?.enabled = spatialActive
            if (spatialActive) {
                val strength = if (amp.spatial360Enabled) {
                    ((0.4f + amp.surroundRoomSize * 0.6f) * 1000).toInt()
                } else {
                    (amp.surroundRoomSize * 1000).toInt()
                }
                hwVirtualizer?.setStrength(strength.coerceIn(0, 1000).toShort())
            }
        }
        runCatching {
            hwLoudness?.setTargetGain(((amp.preampGainDb + amp.loudnessEnhancer * 6f) * 100f).toInt())
        }
    }

    private fun releaseHardwareEffects() {
        runCatching { hwEqualizer?.release() }
        runCatching { hwBassBoost?.release() }
        runCatching { hwVirtualizer?.release() }
        runCatching { hwLoudness?.release() }
        hwEqualizer = null
        hwBassBoost = null
        hwVirtualizer = null
        hwLoudness = null
    }

    private fun stopPlaybackInternal() {
        synthesisJob?.cancel()
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
        releaseHardwareEffects()
    }

    fun release() {
        isPlaying = false
        stopPlaybackInternal()
    }
}
