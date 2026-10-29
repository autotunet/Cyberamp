package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CyberAmpDatabase
import com.example.data.CyberAmpRepository
import com.example.data.EqPresetEntity
import com.example.model.AmplifierState
import com.example.model.FilterType
import com.example.model.ParametricBand
import com.example.model.createFlatBands
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Immutable UI state for the 10-band Parametric Equalizer and Preset Manager.
 * Enforces a strict [-12.0 dB, +12.0 dB] gain range across all 10 frequency bands.
 */
data class ParametricEqualizerUiState(
    val bands: List<ParametricBand> = createFlatBands(),
    val selectedBandIndex: Int = 0,
    val isEqualizerEnabled: Boolean = true,
    val autoGainCompensation: Boolean = true,
    val presets: List<EqPresetEntity> = emptyList(),
    val activePresetId: Int? = null,
    val activePresetName: String = "Winamp Classic V-Curve",
    val isDirtyFromPreset: Boolean = false,
    val amplifierState: AmplifierState = AmplifierState(),
    val statusMessage: String? = null
) {
    val selectedBand: ParametricBand
        get() = bands.getOrElse(selectedBandIndex.coerceIn(0, bands.lastIndex.coerceAtLeast(0))) {
            createFlatBands().first()
        }

    val peakBoostDb: Float
        get() = bands.maxOfOrNull { it.gainDb }?.coerceAtLeast(0f) ?: 0f

    val computedHeadroomOffsetDb: Float
        get() = if (autoGainCompensation && peakBoostDb > 3.0f) {
            -(peakBoostDb - 3.0f) * 0.5f
        } else {
            0f
        }

    val effectiveBandsForDsp: List<ParametricBand>
        get() = if (isEqualizerEnabled) {
            bands
        } else {
            bands.map { it.copy(gainDb = 0f) }
        }

    companion object {
        const val BAND_COUNT = 10
        const val MIN_GAIN_DB = -12.0f
        const val MAX_GAIN_DB = 12.0f
        const val MIN_Q_FACTOR = 0.4f
        const val MAX_Q_FACTOR = 4.0f

        fun clampGainDb(value: Float): Float = value.coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
        fun clampQFactor(value: Float): Float = value.coerceIn(MIN_Q_FACTOR, MAX_Q_FACTOR)
    }
}

/**
 * Dedicated ViewModel managing the 10-band parametric equalizer state,
 * [-12dB, +12dB] band gain manipulation, Q-factor/filter shaping, and preset persistence.
 */
class ParametricEqualizerViewModel(
    private val repository: CyberAmpRepository? = null,
    private val onDspStateChanged: ((List<ParametricBand>, AmplifierState, String) -> Unit)? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ParametricEqualizerUiState(
            bands = buildDefaultWinampVCurveBands()
        )
    )
    val uiState: StateFlow<ParametricEqualizerUiState> = _uiState.asStateFlow()

    init {
        if (repository != null) {
            viewModelScope.launch {
                repository.seedInitialDataIfNeeded()
                repository.eqPresets.collect { loadedPresets ->
                    _uiState.update { current ->
                        val matchedActive = loadedPresets.find { it.name == current.activePresetName }
                        current.copy(
                            presets = loadedPresets,
                            activePresetId = matchedActive?.id ?: current.activePresetId
                        )
                    }
                }
            }
        }
        notifyDspEngine()
    }

    fun selectBand(bandIndex: Int) {
        if (bandIndex !in 0 until ParametricEqualizerUiState.BAND_COUNT) return
        _uiState.update { it.copy(selectedBandIndex = bandIndex) }
    }

    /**
     * Updates the gain of a specific band (0..9), clamping strictly within [-12.0dB, +12.0dB].
     */
    fun setBandGain(bandIndex: Int, gainDb: Float) {
        if (bandIndex !in 0 until ParametricEqualizerUiState.BAND_COUNT) return
        val clampedGain = ParametricEqualizerUiState.clampGainDb(gainDb)
        _uiState.update { state ->
            val updatedBands = state.bands.map { band ->
                if (band.index == bandIndex) band.copy(gainDb = clampedGain) else band
            }
            state.copy(
                bands = updatedBands,
                selectedBandIndex = bandIndex,
                activePresetName = "Personalizado (Custom)",
                isDirtyFromPreset = true
            )
        }
        notifyDspEngine()
    }

    /**
     * Applies a full 10-band gain curve, clamping every band to [-12.0dB, +12.0dB].
     */
    fun setAllBandGains(gainsDb: List<Float>, presetLabel: String = "Personalizado (Custom)") {
        _uiState.update { state ->
            val updatedBands = state.bands.mapIndexed { idx, band ->
                val rawGain = gainsDb.getOrElse(idx) { 0f }
                band.copy(gainDb = ParametricEqualizerUiState.clampGainDb(rawGain))
            }
            state.copy(
                bands = updatedBands,
                activePresetName = presetLabel,
                isDirtyFromPreset = presetLabel == "Personalizado (Custom)"
            )
        }
        notifyDspEngine()
    }

    /**
     * Updates the Q-factor (bandwidth sharpness) of a specific band, clamped to [0.4, 4.0].
     */
    fun setBandQFactor(bandIndex: Int, qFactor: Float) {
        if (bandIndex !in 0 until ParametricEqualizerUiState.BAND_COUNT) return
        val clampedQ = ParametricEqualizerUiState.clampQFactor(qFactor)
        _uiState.update { state ->
            val updatedBands = state.bands.map { band ->
                if (band.index == bandIndex) band.copy(qFactor = clampedQ) else band
            }
            state.copy(
                bands = updatedBands,
                selectedBandIndex = bandIndex,
                isDirtyFromPreset = true
            )
        }
        notifyDspEngine()
    }

    fun cycleBandFilterType(bandIndex: Int) {
        if (bandIndex !in 0 until ParametricEqualizerUiState.BAND_COUNT) return
        _uiState.update { state ->
            val updatedBands = state.bands.map { band ->
                if (band.index == bandIndex) {
                    val nextFilter = when (band.filterType) {
                        FilterType.PEAKING -> FilterType.LOW_SHELF
                        FilterType.LOW_SHELF -> FilterType.HIGH_SHELF
                        FilterType.HIGH_SHELF -> FilterType.PEAKING
                    }
                    band.copy(filterType = nextFilter)
                } else {
                    band
                }
            }
            state.copy(
                bands = updatedBands,
                selectedBandIndex = bandIndex,
                isDirtyFromPreset = true
            )
        }
        notifyDspEngine()
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        _uiState.update {
            it.copy(
                isEqualizerEnabled = enabled,
                statusMessage = if (enabled) "EQ Paramétrico Activado" else "EQ en Bypass (0 dB)"
            )
        }
        notifyDspEngine()
    }

    fun setAutoGainCompensation(enabled: Boolean) {
        _uiState.update { it.copy(autoGainCompensation = enabled) }
        notifyDspEngine()
    }

    fun resetToFlat() {
        _uiState.update { state ->
            val flatPreset = state.presets.find { it.name == "Flat Studio Reference" }
            state.copy(
                bands = createFlatBands(),
                activePresetId = flatPreset?.id,
                activePresetName = "Flat Studio Reference",
                isDirtyFromPreset = false,
                statusMessage = "Curva restablecida a 0.0 dB (Referencia Plana)"
            )
        }
        notifyDspEngine()
    }

    /**
     * Inverts all 10 band gains (-gainDb within [-12dB, +12dB]).
     */
    fun invertEqCurve() {
        _uiState.update { state ->
            val inverted = state.bands.map { band ->
                band.copy(gainDb = ParametricEqualizerUiState.clampGainDb(-band.gainDb))
            }
            state.copy(
                bands = inverted,
                activePresetName = "Curva Invertida",
                isDirtyFromPreset = true,
                statusMessage = "Polaridad de ganancia invertida en las 10 bandas"
            )
        }
        notifyDspEngine()
    }

    /**
     * Scales the current 10-band gain values by [scaleFactor] (e.g., 0.8f or 1.2f) within [-12dB, +12dB].
     */
    fun scaleEqCurve(scaleFactor: Float) {
        _uiState.update { state ->
            val scaled = state.bands.map { band ->
                band.copy(gainDb = ParametricEqualizerUiState.clampGainDb(band.gainDb * scaleFactor))
            }
            state.copy(
                bands = scaled,
                isDirtyFromPreset = true
            )
        }
        notifyDspEngine()
    }

    fun updateAmplifier(transform: (AmplifierState) -> AmplifierState) {
        _uiState.update { state ->
            val nextAmp = transform(state.amplifierState).let { amp ->
                amp.copy(
                    preampGainDb = amp.preampGainDb.coerceIn(-6f, 12f),
                    masterVolume = amp.masterVolume.coerceIn(0f, 1f),
                    tubeWarmth = amp.tubeWarmth.coerceIn(0f, 1f),
                    subBassBoost = amp.subBassBoost.coerceIn(0f, 1f),
                    loudnessEnhancer = amp.loudnessEnhancer.coerceIn(0f, 1f),
                    surroundRoomSize = amp.surroundRoomSize.coerceIn(0f, 1f),
                    stereoWidth = amp.stereoWidth.coerceIn(0.5f, 2.0f),
                    balancePan = amp.balancePan.coerceIn(-1f, 1f),
                    pitchTempoRatio = amp.pitchTempoRatio.coerceIn(0.75f, 1.25f),
                    spatialAzimuthDeg = amp.spatialAzimuthDeg.coerceIn(0f, 360f)
                )
            }
            state.copy(amplifierState = nextAmp)
        }
        notifyDspEngine()
    }

    fun setSpatial360Enabled(enabled: Boolean) {
        updateAmplifier {
            it.copy(
                spatial360Enabled = enabled,
                surroundEnabled = if (enabled) true else it.surroundEnabled
            )
        }
    }

    fun setSpatialAutoOrbit(autoOrbit: Boolean) {
        updateAmplifier { it.copy(spatialAutoOrbit = autoOrbit) }
    }

    fun setSpatialAzimuth(azimuthDeg: Float) {
        updateAmplifier {
            it.copy(
                spatialAzimuthDeg = azimuthDeg.coerceIn(0f, 360f),
                spatialAutoOrbit = false
            )
        }
    }

    // Preset Management (-12dB to +12dB)

    fun applyPreset(preset: EqPresetEntity) {
        val parsedGains = preset.gainsCsv
            .split(",")
            .mapNotNull { it.trim().toFloatOrNull() }
        val parsedQs = preset.qFactorsCsv
            .split(",")
            .mapNotNull { it.trim().toFloatOrNull() }

        _uiState.update { state ->
            val updatedBands = state.bands.mapIndexed { idx, band ->
                val gain = ParametricEqualizerUiState.clampGainDb(parsedGains.getOrElse(idx) { 0f })
                val q = ParametricEqualizerUiState.clampQFactor(parsedQs.getOrElse(idx) { 1.41f })
                band.copy(gainDb = gain, qFactor = q)
            }
            val updatedAmp = state.amplifierState.copy(
                preampGainDb = preset.preampDb.coerceIn(-6f, 12f),
                tubeWarmth = preset.tubeWarmth.coerceIn(0f, 1f),
                subBassBoost = preset.subBassBoost.coerceIn(0f, 1f),
                stereoWidth = preset.stereoWidth.coerceIn(0.5f, 2.0f)
            )
            state.copy(
                bands = updatedBands,
                amplifierState = updatedAmp,
                activePresetId = preset.id,
                activePresetName = preset.name,
                isDirtyFromPreset = false,
                statusMessage = "Preset cargado: ${preset.name}"
            )
        }
        notifyDspEngine()
    }

    fun saveCustomPreset(name: String) {
        val state = _uiState.value
        val cleanName = name.trim().ifEmpty {
            "Cyber Preset ${state.presets.count { !it.isBuiltIn } + 1}"
        }
        val gainsCsv = state.bands.joinToString(",") {
            String.format(Locale.US, "%.1f", ParametricEqualizerUiState.clampGainDb(it.gainDb))
        }
        val qFactorsCsv = state.bands.joinToString(",") {
            String.format(Locale.US, "%.2f", ParametricEqualizerUiState.clampQFactor(it.qFactor))
        }
        val newPreset = EqPresetEntity(
            id = if (repository == null) (state.presets.maxOfOrNull { it.id } ?: 0) + 1 else 0,
            name = cleanName,
            gainsCsv = gainsCsv,
            qFactorsCsv = qFactorsCsv,
            preampDb = state.amplifierState.preampGainDb,
            tubeWarmth = state.amplifierState.tubeWarmth,
            subBassBoost = state.amplifierState.subBassBoost,
            stereoWidth = state.amplifierState.stereoWidth,
            isBuiltIn = false
        )

        if (repository != null) {
            viewModelScope.launch {
                repository.saveCustomEqPreset(newPreset)
            }
        } else {
            _uiState.update { current ->
                current.copy(presets = current.presets + newPreset)
            }
        }

        _uiState.update {
            it.copy(
                activePresetName = cleanName,
                isDirtyFromPreset = false,
                statusMessage = "Preset '$cleanName' guardado"
            )
        }
    }

    fun deleteCustomPreset(presetId: Int) {
        val target = _uiState.value.presets.find { it.id == presetId }
        if (target != null && target.isBuiltIn) {
            _uiState.update {
                it.copy(statusMessage = "Los presets de fábrica están protegidos")
            }
            return
        }

        if (repository != null) {
            viewModelScope.launch {
                repository.deleteCustomEqPreset(presetId)
            }
        } else {
            _uiState.update { current ->
                current.copy(presets = current.presets.filterNot { it.id == presetId })
            }
        }

        _uiState.update { current ->
            val wasActive = current.activePresetId == presetId || current.activePresetName == target?.name
            if (wasActive) {
                current.copy(
                    activePresetId = null,
                    activePresetName = "Personalizado (Custom)",
                    isDirtyFromPreset = true,
                    statusMessage = "Preset eliminado"
                )
            } else {
                current.copy(statusMessage = "Preset eliminado")
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    private fun notifyDspEngine() {
        val state = _uiState.value
        val compensatedAmp = state.amplifierState.copy(
            preampGainDb = (state.amplifierState.preampGainDb + state.computedHeadroomOffsetDb)
                .coerceIn(-6f, 12f)
        )
        onDspStateChanged?.invoke(
            state.effectiveBandsForDsp,
            compensatedAmp,
            state.activePresetName
        )
    }

    companion object {
        private fun buildDefaultWinampVCurveBands(): List<ParametricBand> {
            val defaultGains = listOf(5.5f, 4.2f, 2.0f, -1.5f, -2.5f, -1.0f, 2.2f, 4.5f, 6.0f, 6.5f)
            return createFlatBands().mapIndexed { idx, band ->
                band.copy(
                    gainDb = ParametricEqualizerUiState.clampGainDb(defaultGains.getOrElse(idx) { 0f })
                )
            }
        }

        fun provideFactory(
            context: Context,
            onDspStateChanged: ((List<ParametricBand>, AmplifierState, String) -> Unit)? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appCtx = context.applicationContext
                val repo = CyberAmpRepository.getInstance(appCtx)
                return ParametricEqualizerViewModel(
                    repository = repo,
                    onDspStateChanged = onDspStateChanged
                ) as T
            }
        }
    }
}
