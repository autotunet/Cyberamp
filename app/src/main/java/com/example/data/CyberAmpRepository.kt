package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.CyberSkinStyle
import com.example.model.VisualizerMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "cyberamp_prefs")

data class UserThemePreferences(
    val skinStyle: CyberSkinStyle = CyberSkinStyle.CYBER_DECK_2099,
    val isDarkTheme: Boolean = true,
    val dynamicAlbumSkin: Boolean = true,
    val visualizerMode: VisualizerMode = VisualizerMode.SPECTRUM_64,
    val highContrastHud: Boolean = false,
    val glowingBordersStrength: Float = 0.85f
)

class CyberAmpRepository(
    private val context: Context,
    private val dao: CyberAmpDao
) {
    private object Keys {
        val SKIN_STYLE = stringPreferencesKey("skin_style")
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val DYNAMIC_ALBUM_SKIN = booleanPreferencesKey("dynamic_album_skin")
        val VISUALIZER_MODE = stringPreferencesKey("visualizer_mode")
        val HIGH_CONTRAST_HUD = booleanPreferencesKey("high_contrast_hud")
        val GLOW_STRENGTH = floatPreferencesKey("glow_strength")
    }

    val themePreferences: Flow<UserThemePreferences> = context.dataStore.data.map { prefs ->
        val skinId = prefs[Keys.SKIN_STYLE] ?: CyberSkinStyle.CYBER_DECK_2099.id
        val skin = CyberSkinStyle.entries.find { it.id == skinId } ?: CyberSkinStyle.CYBER_DECK_2099
        val visName = prefs[Keys.VISUALIZER_MODE] ?: VisualizerMode.SPECTRUM_64.name
        val visMode = VisualizerMode.entries.find { it.name == visName } ?: VisualizerMode.SPECTRUM_64

        UserThemePreferences(
            skinStyle = skin,
            isDarkTheme = prefs[Keys.IS_DARK_THEME] ?: true,
            dynamicAlbumSkin = prefs[Keys.DYNAMIC_ALBUM_SKIN] ?: true,
            visualizerMode = visMode,
            highContrastHud = prefs[Keys.HIGH_CONTRAST_HUD] ?: false,
            glowingBordersStrength = prefs[Keys.GLOW_STRENGTH] ?: 0.85f
        )
    }

    val eqPresets: Flow<List<EqPresetEntity>> = dao.getAllEqPresets()
    val playlists: Flow<List<PlaylistEntity>> = dao.getAllPlaylists()
    val favoriteTrackIds: Flow<Set<String>> = dao.getAllFavorites().map { list ->
        list.map { it.trackId }.toSet()
    }

    suspend fun updateSkinStyle(style: CyberSkinStyle) {
        context.dataStore.edit { it[Keys.SKIN_STYLE] = style.id }
    }

    suspend fun updateDarkTheme(isDark: Boolean) {
        context.dataStore.edit { it[Keys.IS_DARK_THEME] = isDark }
    }

    suspend fun updateDynamicAlbumSkin(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_ALBUM_SKIN] = enabled }
    }

    suspend fun updateVisualizerMode(mode: VisualizerMode) {
        context.dataStore.edit { it[Keys.VISUALIZER_MODE] = mode.name }
    }

    suspend fun updateHighContrastHud(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HIGH_CONTRAST_HUD] = enabled }
    }

    suspend fun updateGlowStrength(strength: Float) {
        context.dataStore.edit { it[Keys.GLOW_STRENGTH] = strength.coerceIn(0.1f, 1f) }
    }

    suspend fun saveCustomEqPreset(preset: EqPresetEntity) {
        dao.insertEqPreset(preset)
    }

    suspend fun deleteCustomEqPreset(id: Int) {
        dao.deleteCustomEqPreset(id)
    }

    suspend fun savePlaylist(playlist: PlaylistEntity) {
        dao.insertPlaylist(playlist)
    }

    suspend fun deletePlaylist(id: Int) {
        dao.deletePlaylist(id)
    }

    suspend fun toggleFavorite(trackId: String, currentlyFavorite: Boolean) {
        if (currentlyFavorite) {
            dao.removeFavorite(trackId)
        } else {
            dao.addFavorite(FavoriteTrackEntity(trackId))
        }
    }

    private val seedMutex = kotlinx.coroutines.sync.Mutex()
    @Volatile
    private var isSeeded = false

    suspend fun seedInitialDataIfNeeded() {
        if (isSeeded) return
        seedMutex.lock()
        try {
            if (isSeeded) return
            if (dao.getEqPresetCount() == 0) {
                val defaultPresets = listOf(
                    EqPresetEntity(
                        name = "Winamp Classic V-Curve",
                        gainsCsv = "5.5,4.2,2.0,-1.5,-2.5,-1.0,2.2,4.5,6.0,6.5",
                        qFactorsCsv = "1.2,1.3,1.4,1.5,1.6,1.5,1.4,1.3,1.2,1.1",
                        preampDb = 2.0f,
                        tubeWarmth = 0.30f,
                        subBassBoost = 0.55f,
                        stereoWidth = 1.35f,
                        isBuiltIn = true
                    ),
                    EqPresetEntity(
                        name = "Flat Studio Reference",
                        gainsCsv = "0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0",
                        qFactorsCsv = "1.41,1.41,1.41,1.41,1.41,1.41,1.41,1.41,1.41,1.41",
                        preampDb = 0.0f,
                        tubeWarmth = 0.0f,
                        subBassBoost = 0.0f,
                        stereoWidth = 1.0f,
                        isBuiltIn = true
                    ),
                    EqPresetEntity(
                        name = "Cyber Sub-Bass Overdrive",
                        gainsCsv = "8.5,7.0,4.5,1.0,-1.5,-2.0,0.5,3.0,4.5,5.0",
                        qFactorsCsv = "0.9,1.1,1.3,1.5,1.8,1.6,1.4,1.3,1.2,1.0",
                        preampDb = 3.0f,
                        tubeWarmth = 0.45f,
                        subBassBoost = 0.85f,
                        stereoWidth = 1.50f,
                        isBuiltIn = true
                    ),
                    EqPresetEntity(
                        name = "EL34 Tube Warmth & Jazz",
                        gainsCsv = "3.0,3.8,2.5,1.2,0.5,-0.5,1.5,2.8,2.0,1.0",
                        qFactorsCsv = "1.0,1.2,1.4,1.2,1.2,1.5,1.3,1.2,1.1,1.0",
                        preampDb = 1.5f,
                        tubeWarmth = 0.80f,
                        subBassBoost = 0.35f,
                        stereoWidth = 1.25f,
                        isBuiltIn = true
                    ),
                    EqPresetEntity(
                        name = "3D Holographic Surround",
                        gainsCsv = "4.0,3.0,0.5,-1.0,1.5,3.2,4.8,6.0,7.2,6.0",
                        qFactorsCsv = "1.1,1.3,1.5,1.8,2.0,1.7,1.4,1.2,1.1,1.0",
                        preampDb = 2.5f,
                        tubeWarmth = 0.25f,
                        subBassBoost = 0.45f,
                        stereoWidth = 1.85f,
                        isBuiltIn = true
                    )
                )
                defaultPresets.forEach { dao.insertEqPreset(it) }
            }

            if (dao.getPlaylistCount() == 0) {
                dao.insertPlaylist(
                    PlaylistEntity(
                        name = "Sesión Nocturna Cyberpunk",
                        moodDescription = "Enfoque profundo nocturno con sintetizadores analógicos y alta definición",
                        trackIdsCsv = "trk_cyber_1,trk_nocturne_3,trk_stream_6,trk_cyber_5",
                        aiReasoning = "Seleccionada para mantener concentración elevada con bajos precisos y atmósfera inmersiva 24-bit/192kHz.",
                        recommendedPresetName = "Winamp Classic V-Curve"
                    )
                )
                dao.insertPlaylist(
                    PlaylistEntity(
                        name = "Audiophile Hi-Res Showcase",
                        moodDescription = "Prueba de rango dinámico extremo, DSD128 y espacialidad 3D",
                        trackIdsCsv = "trk_quantum_2,trk_solar_4,trk_nocturne_3,trk_stream_7",
                        aiReasoning = "Recorrido por grabaciones PCM 384kHz y DSD128 para evaluar separación instrumental y sub-graves.",
                        recommendedPresetName = "3D Holographic Surround"
                    )
                )
            }
            isSeeded = true
        } finally {
            seedMutex.unlock()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: CyberAmpRepository? = null

        fun getInstance(context: Context): CyberAmpRepository {
            return INSTANCE ?: synchronized(this) {
                val appCtx = context.applicationContext
                val db = CyberAmpDatabase.getInstance(appCtx)
                val instance = CyberAmpRepository(appCtx, db.dao())
                INSTANCE = instance
                instance
            }
        }
    }
}
