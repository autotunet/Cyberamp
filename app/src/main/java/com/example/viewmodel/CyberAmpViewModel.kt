package com.example.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.audio.CyberAmpAudioEngine
import com.example.audio.TelemetrySnapshot
import com.example.data.CyberAmpDatabase
import com.example.data.CyberAmpRepository
import com.example.data.EqPresetEntity
import com.example.data.PlaylistEntity
import com.example.data.UserThemePreferences
import com.example.model.AmplifierState
import com.example.model.AudioSourceType
import com.example.model.BUILTIN_TRACKS
import com.example.model.CyberSkinStyle
import com.example.model.FilterType
import com.example.model.LosslessFormat
import com.example.model.ParametricBand
import com.example.model.TrackItem
import com.example.model.VisualizerMode
import com.example.model.createFlatBands
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "CyberAmpViewModel"

data class AiMoodResult(
    val playlistTitle: String,
    val moodSummary: String,
    val selectedTracks: List<TrackItem>,
    val recommendedGainsDb: List<Float>,
    val recommendedPreampDb: Float,
    val recommendedTubeWarmth: Float,
    val recommendedSubBass: Float,
    val recommendedStereoWidth: Float,
    val acousticExplanation: String
)

sealed interface AiMoodUiState {
    data object Idle : AiMoodUiState
    data object Loading : AiMoodUiState
    data class Success(val result: AiMoodResult) : AiMoodUiState
    data class Error(val message: String) : AiMoodUiState
}

enum class MainTab(val route: String) {
    DECK("deck"),
    EQ_AMP("eq_amp"),
    LIBRARY("library"),
    AI_MOOD("ai_mood"),
    SKINS("skins")
}

class CyberAmpViewModel(
    private val appContext: Context,
    private val repository: CyberAmpRepository
) : ViewModel() {

    private val audioEngine = CyberAmpAudioEngine(appContext, viewModelScope)

    private val _selectedTab = MutableStateFlow(MainTab.DECK)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _allTracks = MutableStateFlow(BUILTIN_TRACKS)
    val allTracks: StateFlow<List<TrackItem>> = _allTracks.asStateFlow()

    private val _activeQueue = MutableStateFlow(BUILTIN_TRACKS)
    val activeQueue: StateFlow<List<TrackItem>> = _activeQueue.asStateFlow()

    private val _currentTrack = MutableStateFlow(BUILTIN_TRACKS.first())
    val currentTrack: StateFlow<TrackItem> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatEnabled = MutableStateFlow(true)
    val repeatEnabled: StateFlow<Boolean> = _repeatEnabled.asStateFlow()

    private val _parametricBands = MutableStateFlow(createFlatBands())
    val parametricBands: StateFlow<List<ParametricBand>> = _parametricBands.asStateFlow()

    private val _amplifierState = MutableStateFlow(AmplifierState())
    val amplifierState: StateFlow<AmplifierState> = _amplifierState.asStateFlow()

    private val _activePresetName = MutableStateFlow("Winamp Classic V-Curve")
    val activePresetName: StateFlow<String> = _activePresetName.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedSourceFilter = MutableStateFlow<AudioSourceType?>(null)
    val selectedSourceFilter: StateFlow<AudioSourceType?> = _selectedSourceFilter.asStateFlow()

    private val _aiMoodUiState = MutableStateFlow<AiMoodUiState>(AiMoodUiState.Idle)
    val aiMoodUiState: StateFlow<AiMoodUiState> = _aiMoodUiState.asStateFlow()

    val telemetry: StateFlow<TelemetrySnapshot> = audioEngine.telemetry
    val currentPositionSec: StateFlow<Float> = audioEngine.currentPositionSec

    val themePreferences: StateFlow<UserThemePreferences> = repository.themePreferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserThemePreferences()
    )

    val eqPresets: StateFlow<List<EqPresetEntity>> = repository.eqPresets.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val playlists: StateFlow<List<PlaylistEntity>> = repository.playlists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoriteTrackIds: StateFlow<Set<String>> = repository.favoriteTrackIds.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    // Structured JSON Schema for Firebase AI Logic mood playlist & EQ recommendation
    private val moodPlaylistSchema = Schema.obj(
        properties = mapOf(
            "playlistTitle" to Schema.string(description = "Creative cybernetic playlist title in Spanish"),
            "moodSummary" to Schema.string(description = "Empathetic description of the user's real-time mood in Spanish"),
            "selectedTrackIds" to Schema.array(
                items = Schema.string(),
                description = "Ordered list of track IDs from the catalog matching the mood"
            ),
            "recommendedGainsDb" to Schema.array(
                items = Schema.double(),
                description = "10 float values between -10.0 and +10.0 for the 10-band parametric equalizer (31Hz to 16kHz)"
            ),
            "recommendedPreampDb" to Schema.double(description = "Recommended preamp gain in dB between -3.0 and 6.0"),
            "recommendedTubeWarmth" to Schema.double(description = "Recommended EL34 tube saturation warmth between 0.0 and 1.0"),
            "recommendedSubBass" to Schema.double(description = "Recommended sub-bass booster between 0.0 and 1.0"),
            "recommendedStereoWidth" to Schema.double(description = "Recommended 3D surround stereo width between 0.8 and 1.9"),
            "acousticExplanation" to Schema.string(description = "Brief explanation in Spanish of why this parametric EQ curve and amplifier setup fits the mood")
        )
    )

    private val aiModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = moodPlaylistSchema
            }
        )
    }

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
        }
        // Apply initial Winamp Classic V-Curve
        val initialGains = listOf(5.5f, 4.2f, 2.0f, -1.5f, -2.5f, -1.0f, 2.2f, 4.5f, 6.0f, 6.5f)
        _parametricBands.update { bands ->
            bands.mapIndexed { idx, band ->
                band.copy(gainDb = initialGains.getOrElse(idx) { 0f })
            }
        }
        audioEngine.updateDspParameters(_parametricBands.value, _amplifierState.value)
    }

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    fun togglePlayPause() {
        val nextState = !_isPlaying.value
        _isPlaying.value = nextState
        if (nextState) {
            audioEngine.startTrack(_currentTrack.value, currentPositionSec.value)
        } else {
            audioEngine.setPlaying(false)
        }
    }

    fun selectAndPlayTrack(track: TrackItem) {
        _currentTrack.value = track
        _isPlaying.value = true
        audioEngine.startTrack(track, 0f)
    }

    fun skipNext() {
        val queue = _activeQueue.value
        if (queue.isEmpty()) return
        val currentIdx = queue.indexOfFirst { it.id == _currentTrack.value.id }
        val nextIdx = if (_shuffleEnabled.value && queue.size > 1) {
            queue.indices.filter { it != currentIdx }.random()
        } else {
            (currentIdx + 1) % queue.size
        }
        selectAndPlayTrack(queue[nextIdx])
    }

    fun skipPrevious() {
        if (currentPositionSec.value > 5f) {
            seekTo(0f)
            return
        }
        val queue = _activeQueue.value
        if (queue.isEmpty()) return
        val currentIdx = queue.indexOfFirst { it.id == _currentTrack.value.id }
        val prevIdx = if (currentIdx <= 0) queue.lastIndex else currentIdx - 1
        selectAndPlayTrack(queue[prevIdx])
    }

    fun seekTo(seconds: Float) {
        audioEngine.seekTo(seconds)
    }

    fun toggleShuffle() {
        _shuffleEnabled.update { !it }
    }

    fun toggleRepeat() {
        _repeatEnabled.update { !it }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSourceFilter(source: AudioSourceType?) {
        _selectedSourceFilter.value = source
    }

    fun toggleFavorite(trackId: String) {
        viewModelScope.launch {
            val isFav = favoriteTrackIds.value.contains(trackId)
            repository.toggleFavorite(trackId, isFav)
        }
    }

    fun importLocalAudioTrack(uri: Uri) {
        viewModelScope.launch {
            val fileName = queryDisplayName(uri) ?: "Audio Local Hi-Res"
            val cleanTitle = fileName.substringBeforeLast(".")
            val newTrack = TrackItem(
                id = "local_${System.currentTimeMillis()}",
                title = cleanTitle,
                artist = "Biblioteca del Dispositivo",
                album = "Importación Directa Lossless",
                genre = "Local Hi-Fi Audio",
                durationSec = 240,
                bpm = 120,
                format = LosslessFormat.FLAC_192,
                sourceType = AudioSourceType.IMPORTED_DEVICE,
                coverResId = R.drawable.img_album_quantum_bass_1793260651433,
                dominantPrimaryHex = 0xFF00FFA3,
                dominantSecondaryHex = 0xFF00E5FF,
                moodTags = listOf("local", "personalizado", "favorito"),
                synthProfileId = 1,
                localUriString = uri.toString()
            )
            _allTracks.update { listOf(newTrack) + it }
            _activeQueue.update { listOf(newTrack) + it }
            selectAndPlayTrack(newTrack)
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else null
            }
        }.getOrNull()
    }

    // Parametric EQ & Amplifier Controls
    fun syncFromEqualizerViewModel(
        bands: List<ParametricBand>,
        amp: AmplifierState,
        presetName: String
    ) {
        _parametricBands.value = bands
        _amplifierState.value = amp
        _activePresetName.value = presetName
        audioEngine.updateDspParameters(bands, amp)
    }

    fun updateBandGain(index: Int, gainDb: Float) {
        _activePresetName.value = "Personalizado (Custom)"
        _parametricBands.update { bands ->
            bands.map { if (it.index == index) it.copy(gainDb = gainDb.coerceIn(-12f, 12f)) else it }
        }
        audioEngine.updateDspParameters(_parametricBands.value, _amplifierState.value)
    }

    fun updateBandQFactor(index: Int, qFactor: Float) {
        _activePresetName.value = "Personalizado (Custom)"
        _parametricBands.update { bands ->
            bands.map { if (it.index == index) it.copy(qFactor = qFactor.coerceIn(0.4f, 4.0f)) else it }
        }
        audioEngine.updateDspParameters(_parametricBands.value, _amplifierState.value)
    }

    fun cycleBandFilterType(index: Int) {
        _parametricBands.update { bands ->
            bands.map { band ->
                if (band.index == index) {
                    val nextFilter = when (band.filterType) {
                        FilterType.PEAKING -> FilterType.LOW_SHELF
                        FilterType.LOW_SHELF -> FilterType.HIGH_SHELF
                        FilterType.HIGH_SHELF -> FilterType.PEAKING
                    }
                    band.copy(filterType = nextFilter)
                } else band
            }
        }
        audioEngine.updateDspParameters(_parametricBands.value, _amplifierState.value)
    }

    fun resetEqualizerFlat() {
        _activePresetName.value = "Flat Studio Reference"
        _parametricBands.value = createFlatBands()
        audioEngine.updateDspParameters(_parametricBands.value, _amplifierState.value)
    }

    fun updateAmplifier(transform: (AmplifierState) -> AmplifierState) {
        _amplifierState.update(transform)
        audioEngine.updateDspParameters(_parametricBands.value, _amplifierState.value)
    }

    fun applyEqPreset(preset: EqPresetEntity) {
        _activePresetName.value = preset.name
        val gains = preset.gainsCsv.split(",").mapNotNull { it.trim().toFloatOrNull() }
        val qs = preset.qFactorsCsv.split(",").mapNotNull { it.trim().toFloatOrNull() }
        _parametricBands.update { bands ->
            bands.mapIndexed { idx, b ->
                b.copy(
                    gainDb = gains.getOrElse(idx) { 0f }.coerceIn(-12f, 12f),
                    qFactor = qs.getOrElse(idx) { 1.41f }.coerceIn(0.4f, 4.0f)
                )
            }
        }
        _amplifierState.update { amp ->
            amp.copy(
                preampGainDb = preset.preampDb,
                tubeWarmth = preset.tubeWarmth,
                subBassBoost = preset.subBassBoost,
                stereoWidth = preset.stereoWidth
            )
        }
        audioEngine.updateDspParameters(_parametricBands.value, _amplifierState.value)
    }

    fun saveCurrentAsCustomPreset(name: String) {
        val cleanName = name.trim().ifEmpty { "Preset Cyber ${System.currentTimeMillis() % 1000}" }
        val bands = _parametricBands.value
        val amp = _amplifierState.value
        val gainsCsv = bands.joinToString(",") { "%.1f".format(java.util.Locale.US, it.gainDb) }
        val qsCsv = bands.joinToString(",") { "%.2f".format(java.util.Locale.US, it.qFactor) }
        viewModelScope.launch {
            repository.saveCustomEqPreset(
                EqPresetEntity(
                    name = cleanName,
                    gainsCsv = gainsCsv,
                    qFactorsCsv = qsCsv,
                    preampDb = amp.preampGainDb,
                    tubeWarmth = amp.tubeWarmth,
                    subBassBoost = amp.subBassBoost,
                    stereoWidth = amp.stereoWidth,
                    isBuiltIn = false
                )
            )
            _activePresetName.value = cleanName
        }
    }

    fun deleteCustomPreset(id: Int) {
        viewModelScope.launch {
            repository.deleteCustomEqPreset(id)
        }
    }

    fun loadPlaylist(playlist: PlaylistEntity) {
        val ids = playlist.trackIdsCsv.split(",").map { it.trim() }
        val matched = ids.mapNotNull { id -> _allTracks.value.find { it.id == id } }
        if (matched.isNotEmpty()) {
            _activeQueue.value = matched
            selectAndPlayTrack(matched.first())
            _selectedTab.value = MainTab.DECK
        }
    }

    fun deletePlaylist(id: Int) {
        viewModelScope.launch {
            repository.deletePlaylist(id)
        }
    }

    // AI Mood Playlist & Acoustic Profile Generator via Firebase AI Logic
    fun generateMoodPlaylistAndEq(userMoodPrompt: String) {
        val trimmed = userMoodPrompt.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            _aiMoodUiState.value = AiMoodUiState.Loading
            try {
                val catalogSummary = _allTracks.value.joinToString("\n") { trk ->
                    "- id: \"${trk.id}\", title: \"${trk.title}\", artist: \"${trk.artist}\", genre: \"${trk.genre}\", bpm: ${trk.bpm}, format: \"${trk.format.badge}\", tags: ${trk.moodTags.joinToString(",")}"
                }
                val prompt = """
                    Eres el motor acústico e inteligencia artificial de CyberAmp Pro.
                    El usuario describe su estado de ánimo, ambiente o actividad actual: "$trimmed"
                    
                    Catálogo de pistas disponibles (Hi-Res & Streaming):
                    $catalogSummary
                    
                    1. Selecciona entre 3 y 6 pistas del catálogo (usando exactamente sus IDs en selectedTrackIds) y ordénalas para crear el viaje musical perfecto para este estado de ánimo.
                    2. Diseña una curva de ecualización paramétrica de 10 bandas (recommendedGainsDb: exactamente 10 valores entre -10.0 y +10.0 para 31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz) y parámetros de amplificador (preamp, válvulas EL34 tubeWarmth, subBass, stereoWidth) ideales para ese estado de ánimo.
                    3. Responde en español con un título creativo, resumen del estado de ánimo y explicación técnica acústica.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    aiModel.generateContent(prompt)
                }

                val jsonString = response.text ?: throw IllegalStateException("El modelo devolvió una respuesta vacía.")
                val json = JSONObject(jsonString)

                val playlistTitle = json.optString("playlistTitle", "Mix IA: $trimmed")
                val moodSummary = json.optString("moodSummary", "Lista optimizada para tu estado de ánimo actual.")
                val acousticExplanation = json.optString("acousticExplanation", "Curva paramétrica calibrada por IA.")

                val idsArray = json.optJSONArray("selectedTrackIds")
                val selectedIds = mutableListOf<String>()
                if (idsArray != null) {
                    for (i in 0 until idsArray.length()) {
                        selectedIds.add(idsArray.getString(i))
                    }
                }
                val matchedTracks = selectedIds.mapNotNull { id ->
                    _allTracks.value.find { it.id == id }
                }.ifEmpty {
                    _allTracks.value.take(4)
                }

                val gainsArray = json.optJSONArray("recommendedGainsDb")
                val gainsList = mutableListOf<Float>()
                if (gainsArray != null) {
                    for (i in 0 until minOf(10, gainsArray.length())) {
                        gainsList.add(gainsArray.getDouble(i).toFloat().coerceIn(-12f, 12f))
                    }
                }
                while (gainsList.size < 10) {
                    gainsList.add(0f)
                }

                val preampDb = json.optDouble("recommendedPreampDb", 2.0).toFloat().coerceIn(-6f, 12f)
                val tubeWarmth = json.optDouble("recommendedTubeWarmth", 0.4).toFloat().coerceIn(0f, 1f)
                val subBass = json.optDouble("recommendedSubBass", 0.5).toFloat().coerceIn(0f, 1f)
                val stereoWidth = json.optDouble("recommendedStereoWidth", 1.35).toFloat().coerceIn(0.5f, 2.0f)

                val result = AiMoodResult(
                    playlistTitle = playlistTitle,
                    moodSummary = moodSummary,
                    selectedTracks = matchedTracks,
                    recommendedGainsDb = gainsList,
                    recommendedPreampDb = preampDb,
                    recommendedTubeWarmth = tubeWarmth,
                    recommendedSubBass = subBass,
                    recommendedStereoWidth = stereoWidth,
                    acousticExplanation = acousticExplanation
                )

                // Automatically persist the AI-generated playlist in Room DB
                repository.savePlaylist(
                    PlaylistEntity(
                        name = playlistTitle,
                        moodDescription = moodSummary,
                        trackIdsCsv = matchedTracks.joinToString(",") { it.id },
                        aiReasoning = acousticExplanation,
                        recommendedPresetName = "IA: $playlistTitle"
                    )
                )

                _aiMoodUiState.value = AiMoodUiState.Success(result)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI Mood generation timed out", e)
                _aiMoodUiState.value = AiMoodUiState.Error(
                    "La consulta a Gemini superó los 60 segundos. Por favor intenta de nuevo."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI generation failed", e)
                _aiMoodUiState.value = AiMoodUiState.Error(
                    e.localizedMessage ?: "Error al generar la lista inteligente con IA."
                )
            }
        }
    }

    fun applyAiMoodResult(result: AiMoodResult) {
        _activeQueue.value = result.selectedTracks
        _activePresetName.value = "IA: ${result.playlistTitle}"
        _parametricBands.update { bands ->
            bands.mapIndexed { idx, band ->
                band.copy(gainDb = result.recommendedGainsDb.getOrElse(idx) { 0f })
            }
        }
        _amplifierState.update { amp ->
            amp.copy(
                preampGainDb = result.recommendedPreampDb,
                tubeWarmth = result.recommendedTubeWarmth,
                subBassBoost = result.recommendedSubBass,
                stereoWidth = result.recommendedStereoWidth,
                surroundEnabled = true
            )
        }
        audioEngine.updateDspParameters(_parametricBands.value, _amplifierState.value)
        if (result.selectedTracks.isNotEmpty()) {
            selectAndPlayTrack(result.selectedTracks.first())
        }
    }

    // Skin & Theme Preferences
    fun setSkinStyle(style: CyberSkinStyle) {
        viewModelScope.launch { repository.updateSkinStyle(style) }
    }

    fun setDarkTheme(isDark: Boolean) {
        viewModelScope.launch { repository.updateDarkTheme(isDark) }
    }

    fun setDynamicAlbumSkin(enabled: Boolean) {
        viewModelScope.launch { repository.updateDynamicAlbumSkin(enabled) }
    }

    fun setVisualizerMode(mode: VisualizerMode) {
        viewModelScope.launch { repository.updateVisualizerMode(mode) }
    }

    fun setHighContrastHud(enabled: Boolean) {
        viewModelScope.launch { repository.updateHighContrastHud(enabled) }
    }

    fun setGlowStrength(strength: Float) {
        viewModelScope.launch { repository.updateGlowStrength(strength) }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appCtx = context.applicationContext
                    val repo = CyberAmpRepository.getInstance(appCtx)
                    return CyberAmpViewModel(appCtx, repo) as T
                }
            }
    }
}
