package com.example.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R

enum class AudioSourceType(val label: String) {
    LOCAL_HIRES("Biblioteca Local Hi-Res"),
    STREAMING_CLOUD("Streaming Cloud Hi-Fi"),
    IMPORTED_DEVICE("Archivo del Dispositivo")
}

enum class LosslessFormat(
    val badge: String,
    val sampleRateLabel: String,
    val bitDepthLabel: String,
    val bitrateKbps: Int
) {
    FLAC_192("FLAC 24/192", "192 kHz", "24-bit", 4608),
    DSD_128("DSD128", "5.6 MHz", "1-bit DSD", 5644),
    WAV_384("PCM 32/384", "384 kHz", "32-bit Float", 9216),
    ALAC_96("ALAC 24/96", "96 kHz", "24-bit", 2822),
    MQA_STUDIO("MQA Master", "176.4 kHz", "24-bit", 3528),
    OPUS_HIFI("OPUS Stream", "48 kHz", "24-bit", 512)
}

enum class FilterType(val shortLabel: String, val fullName: String) {
    PEAKING("PK", "Peaking Bell"),
    LOW_SHELF("LS", "Low-Shelf"),
    HIGH_SHELF("HS", "High-Shelf")
}

data class ParametricBand(
    val index: Int,
    val frequencyHz: Int,
    val label: String,
    val gainDb: Float = 0f,      // -12f to +12f
    val qFactor: Float = 1.41f,  // 0.4f to 4.0f
    val filterType: FilterType = FilterType.PEAKING
)

data class AmplifierState(
    val preampGainDb: Float = 1.5f,          // -6f to +12f
    val masterVolume: Float = 0.85f,         // 0f to 1f
    val tubeWarmth: Float = 0.35f,           // 0f to 1f (EL34 Vacuum Tube Drive)
    val subBassBoost: Float = 0.40f,         // 0f to 1f
    val loudnessEnhancer: Float = 0.30f,     // 0f to 1f
    val surroundEnabled: Boolean = true,
    val spatial360Enabled: Boolean = true,   // 360-degree binaural HRTF / Haas surround simulation
    val spatialAutoOrbit: Boolean = true,    // Continuous 360-degree soundstage rotation
    val spatialAzimuthDeg: Float = 45f,      // 0f to 360f soundstage angle
    val surroundRoomSize: Float = 0.55f,     // 0f to 1f
    val stereoWidth: Float = 1.25f,          // 0.5f (crossfeed) to 2.0f (ultra wide)
    val balancePan: Float = 0f,              // -1f (Left) to +1f (Right)
    val pitchTempoRatio: Float = 1.0f        // 0.75f to 1.25f
)

enum class CyberSkinStyle(
    val id: String,
    val title: String,
    val subtitle: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val tertiaryHex: Long,
    val surfaceDarkHex: Long,
    val backgroundDarkHex: Long
) {
    CYBER_DECK_2099(
        id = "CYBER_DECK_2099",
        title = "Winamp Cyber-Deck 2099",
        subtitle = "Matriz LED ámbar/esmeralda clásica en chasis de titanio",
        primaryHex = 0xFF00FFA3,
        secondaryHex = 0xFFFFB800,
        tertiaryHex = 0xFF00E5FF,
        surfaceDarkHex = 0xFF101520,
        backgroundDarkHex = 0xFF070A10
    ),
    NEON_OUTRUN(
        id = "NEON_OUTRUN",
        title = "Neon Outrun Synth",
        subtitle = "Cian eléctrico y láser magenta de alta intensidad",
        primaryHex = 0xFF00F0FF,
        secondaryHex = 0xFFFF007F,
        tertiaryHex = 0xFFB537F2,
        surfaceDarkHex = 0xFF120D24,
        backgroundDarkHex = 0xFF080512
    ),
    OBSIDIAN_GOLD(
        id = "OBSIDIAN_GOLD",
        title = "Obsidian Gold Hi-Fi",
        subtitle = "Estética audiófila de oro líquido 24k sobre obsidiana",
        primaryHex = 0xFFFFC83B,
        secondaryHex = 0xFF00E699,
        tertiaryHex = 0xFFFF7B3B,
        surfaceDarkHex = 0xFF161412,
        backgroundDarkHex = 0xFF0A0908
    ),
    QUANTUM_MATRIX(
        id = "QUANTUM_MATRIX",
        title = "Quantum Bio-Matrix",
        subtitle = "Fósforo verde cibernético con telemetría de laboratorio",
        primaryHex = 0xFF2BFF60,
        secondaryHex = 0xFF00D4B2,
        tertiaryHex = 0xFFA6FF00,
        surfaceDarkHex = 0xFF0B1912,
        backgroundDarkHex = 0xFF050C08
    ),
    TOKYO_SAKURA_TUBE(
        id = "TOKYO_SAKURA_TUBE",
        title = "Tokyo Sakura Valve",
        subtitle = "Filamentos de válvulas EL34 cálidos y neón violeta",
        primaryHex = 0xFFFF6EC7,
        secondaryHex = 0xFFFF9E3B,
        tertiaryHex = 0xFF7B61FF,
        surfaceDarkHex = 0xFF1A1021,
        backgroundDarkHex = 0xFF0C0710
    )
}

enum class VisualizerMode(val label: String) {
    SPECTRUM_64("Espectro 64-Bandas"),
    OSCILLOSCOPE("Osciloscopio Dual"),
    VU_METERS("Medidores VU Pro")
}

data class TrackItem(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val durationSec: Int,
    val bpm: Int,
    val format: LosslessFormat,
    val sourceType: AudioSourceType,
    @DrawableRes val coverResId: Int,
    val dominantPrimaryHex: Long,
    val dominantSecondaryHex: Long,
    val moodTags: List<String>,
    val synthProfileId: Int = 0,
    val localUriString: String? = null
)

val DEFAULT_FREQUENCIES = listOf(
    31 to "31",
    62 to "62",
    125 to "125",
    250 to "250",
    500 to "500",
    1000 to "1k",
    2000 to "2k",
    4000 to "4k",
    8000 to "8k",
    16000 to "16k"
)

fun createFlatBands(): List<ParametricBand> =
    DEFAULT_FREQUENCIES.mapIndexed { idx, (freq, lbl) ->
        val filter = when (idx) {
            0 -> FilterType.LOW_SHELF
            DEFAULT_FREQUENCIES.lastIndex -> FilterType.HIGH_SHELF
            else -> FilterType.PEAKING
        }
        ParametricBand(
            index = idx,
            frequencyHz = freq,
            label = lbl,
            gainDb = 0f,
            qFactor = 1.41f,
            filterType = filter
        )
    }

val BUILTIN_TRACKS = listOf(
    TrackItem(
        id = "trk_cyber_1",
        title = "Hyperdrive Protocol 2099",
        artist = "Valkyrie Synth",
        album = "Cyber Odyssey",
        genre = "Cyberpunk Synthwave",
        durationSec = 248,
        bpm = 128,
        format = LosslessFormat.FLAC_192,
        sourceType = AudioSourceType.LOCAL_HIRES,
        coverResId = R.drawable.img_album_cyber_odyssey_1793260650385,
        dominantPrimaryHex = 0xFF00F0FF,
        dominantSecondaryHex = 0xFFFF00A0,
        moodTags = listOf("energía", "enfoque", "nocturno", "cibernético", "programación"),
        synthProfileId = 0
    ),
    TrackItem(
        id = "trk_quantum_2",
        title = "Sub-Zero Monolith",
        artist = "Klangwerk DSP",
        album = "Quantum Bass",
        genre = "Audiophile IDM / Bass",
        durationSec = 215,
        bpm = 140,
        format = LosslessFormat.WAV_384,
        sourceType = AudioSourceType.LOCAL_HIRES,
        coverResId = R.drawable.img_album_quantum_bass_1793260651433,
        dominantPrimaryHex = 0xFF00FF88,
        dominantSecondaryHex = 0xFFFFC83B,
        moodTags = listOf("intenso", "gimnasio", "bajos", "potencia", "euforia"),
        synthProfileId = 1
    ),
    TrackItem(
        id = "trk_nocturne_3",
        title = "Shinjuku Valve Rain",
        artist = "Akira & The EL34 Quartet",
        album = "Neural Nocturne",
        genre = "Neo-Tokyo Lo-Fi Jazz Fusion",
        durationSec = 274,
        bpm = 88,
        format = LosslessFormat.DSD_128,
        sourceType = AudioSourceType.LOCAL_HIRES,
        coverResId = R.drawable.img_album_neural_nocturne_1793260651624,
        dominantPrimaryHex = 0xFFFF9E3B,
        dominantSecondaryHex = 0xFF9D65FF,
        moodTags = listOf("relajado", "lluvia", "melancólico", "estudio", "cálido", "jazz"),
        synthProfileId = 2
    ),
    TrackItem(
        id = "trk_solar_4",
        title = "Crystalline Event Horizon",
        artist = "Aetherius Spatial",
        album = "Solar Horizon",
        genre = "Progressive Ambient 3D",
        durationSec = 312,
        bpm = 72,
        format = LosslessFormat.ALAC_96,
        sourceType = AudioSourceType.LOCAL_HIRES,
        coverResId = R.drawable.img_album_solar_horizon_1793260652764,
        dominantPrimaryHex = 0xFFC56BFF,
        dominantSecondaryHex = 0xFFFFB84D,
        moodTags = listOf("paz", "meditación", "espacial", "sueño", "envolvente"),
        synthProfileId = 3
    ),
    TrackItem(
        id = "trk_cyber_5",
        title = "Chrome Velocity Overdrive",
        artist = "Valkyrie Synth",
        album = "Cyber Odyssey",
        genre = "Darksynth / Outrun",
        durationSec = 198,
        bpm = 135,
        format = LosslessFormat.FLAC_192,
        sourceType = AudioSourceType.LOCAL_HIRES,
        coverResId = R.drawable.img_album_cyber_odyssey_1793260650385,
        dominantPrimaryHex = 0xFF00E5FF,
        dominantSecondaryHex = 0xFFFF2A85,
        moodTags = listOf("velocidad", "autopista", "adrenalina", "acción", "energía"),
        synthProfileId = 4
    ),
    TrackItem(
        id = "trk_stream_6",
        title = "Neo-Tokyo Underground FM [LIVE]",
        artist = "CyberCast Hi-Res Network",
        album = "Cloud Stream 192kHz",
        genre = "Synthwave / Chillwave Stream",
        durationSec = 360,
        bpm = 118,
        format = LosslessFormat.MQA_STUDIO,
        sourceType = AudioSourceType.STREAMING_CLOUD,
        coverResId = R.drawable.img_album_cyber_odyssey_1793260650385,
        dominantPrimaryHex = 0xFF00F0FF,
        dominantSecondaryHex = 0xFF7B61FF,
        moodTags = listOf("streaming", "nocturno", "fresco", "productividad", "cibernético"),
        synthProfileId = 0
    ),
    TrackItem(
        id = "trk_stream_7",
        title = "Obsidian Sub-Bass Lab [STREAM]",
        artist = "Audiophile Master Relay",
        album = "Cloud Stream DSD",
        genre = "Hi-Res Bass & Dub Techno",
        durationSec = 300,
        bpm = 126,
        format = LosslessFormat.OPUS_HIFI,
        sourceType = AudioSourceType.STREAMING_CLOUD,
        coverResId = R.drawable.img_album_quantum_bass_1793260651433,
        dominantPrimaryHex = 0xFFFFC83B,
        dominantSecondaryHex = 0xFF00FF88,
        moodTags = listOf("streaming", "profundo", "hipnótico", "bajos", "concentración"),
        synthProfileId = 1
    ),
    TrackItem(
        id = "trk_stream_8",
        title = "Sakura Tube Lounge 24/7 [STREAM]",
        artist = "Tokyo Vinyl & Valve Relay",
        album = "Cloud Stream FLAC",
        genre = "Warm Tube Jazz & Downtempo",
        durationSec = 340,
        bpm = 84,
        format = LosslessFormat.FLAC_192,
        sourceType = AudioSourceType.STREAMING_CLOUD,
        coverResId = R.drawable.img_album_neural_nocturne_1793260651624,
        dominantPrimaryHex = 0xFFFF7EB6,
        dominantSecondaryHex = 0xFFFFB347,
        moodTags = listOf("streaming", "café", "lectura", "tranquilo", "acústico"),
        synthProfileId = 2
    )
)
