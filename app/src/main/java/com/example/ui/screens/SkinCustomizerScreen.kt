package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.TelemetrySnapshot
import com.example.data.UserThemePreferences
import com.example.model.AmplifierState
import com.example.model.CyberSkinStyle
import com.example.model.TrackItem
import com.example.model.VisualizerMode
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun SkinCustomizerScreen(
    themePrefs: UserThemePreferences,
    currentTrack: TrackItem,
    amplifierState: AmplifierState = AmplifierState(),
    telemetry: TelemetrySnapshot = TelemetrySnapshot(),
    onSelectSkinStyle: (CyberSkinStyle) -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onToggleDynamicAlbumSkin: (Boolean) -> Unit,
    onSelectVisualizerMode: (VisualizerMode) -> Unit,
    onToggleHighContrastHud: (Boolean) -> Unit,
    onGlowStrengthChange: (Float) -> Unit,
    onUpdateAmplifier: ((AmplifierState) -> AmplifierState) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 360-Degree Spatial Audio Simulation Settings Card (Binaural HRTF & Surround Soundstage)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("spatial_360_settings_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, secondary.copy(alpha = 0.55f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SurroundSound,
                                contentDescription = null,
                                tint = secondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "AUDIO ESPACIAL 360° (SURROUND HRTF)",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = secondary
                                )
                                Text(
                                    text = "Simulación envolvente esférica de 360 grados con filtros binaurales ITD/ILD y reflexiones de sala",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = amplifierState.spatial360Enabled,
                            onCheckedChange = { enabled ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onUpdateAmplifier {
                                    it.copy(
                                        spatial360Enabled = enabled,
                                        surroundEnabled = if (enabled) true else it.surroundEnabled
                                    )
                                }
                            },
                            modifier = Modifier.testTag("spatial_360_settings_switch")
                        )
                    }

                    // Live 360-Degree Binaural Radar & Speaker Array Status inside Settings
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, primary.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF050811)),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val cx = size.width / 2f
                                    val cy = size.height / 2f
                                    val r = size.minDimension / 2f - 5f
                                    drawCircle(
                                        color = primary.copy(alpha = 0.3f),
                                        radius = r,
                                        style = Stroke(width = 1.4f)
                                    )
                                    if (amplifierState.spatial360Enabled) {
                                        val azDeg = if (amplifierState.spatialAutoOrbit) {
                                            telemetry.spatial360AzimuthDeg
                                        } else {
                                            amplifierState.spatialAzimuthDeg
                                        }
                                        val rad = (azDeg - 90f) * (PI.toFloat() / 180f)
                                        val tx = cx + cos(rad) * r
                                        val ty = cy + sin(rad) * r
                                        drawLine(
                                            color = secondary,
                                            start = Offset(cx, cy),
                                            end = Offset(tx, ty),
                                            strokeWidth = 2.4f,
                                            cap = StrokeCap.Round
                                        )
                                        drawCircle(
                                            color = tertiary,
                                            radius = 4.5f,
                                            center = Offset(tx, ty)
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Radar,
                                        contentDescription = null,
                                        tint = primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (amplifierState.spatial360Enabled) {
                                            "Matriz 5.1 / 360° Binaural Activa"
                                        } else {
                                            "Matriz 360° Desactivada (Estéreo Directo)"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "Vector Acimutal: %03.0f° • Amplitud: %d%%",
                                        if (amplifierState.spatialAutoOrbit) telemetry.spatial360AzimuthDeg else amplifierState.spatialAzimuthDeg,
                                        (amplifierState.stereoWidth * 100).roundToInt()
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = primary
                                )
                            }
                        }
                    }

                    // Auto-Orbit 360° Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Órbita Acústica 360° Automática",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Rota suavemente el foco espacial alrededor del oyente en tiempo real",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = amplifierState.spatialAutoOrbit,
                            onCheckedChange = { orbit ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onUpdateAmplifier { it.copy(spatialAutoOrbit = orbit) }
                            },
                            modifier = Modifier.testTag("spatial_auto_orbit_switch")
                        )
                    }

                    // Manual Azimuth Angle Control (0° - 360°)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Ángulo Acimutal 360° (Foco Direccional)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${amplifierState.spatialAzimuthDeg.roundToInt()}°",
                                style = MaterialTheme.typography.labelLarge,
                                color = secondary
                            )
                        }
                        CyberHorizontalMeterSlider(
                            value = amplifierState.spatialAzimuthDeg,
                            valueRange = 0f..360f,
                            accentColor = secondary,
                            secondaryColor = primary,
                            onValueChange = { deg ->
                                onUpdateAmplifier {
                                    it.copy(
                                        spatialAzimuthDeg = deg,
                                        spatialAutoOrbit = false,
                                        spatial360Enabled = true
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .testTag("spatial_azimuth_slider")
                        )
                    }

                    // Acoustic Chamber Depth / Room Size Control
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Profundidad de Cámara Esférica 360°",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${(amplifierState.surroundRoomSize * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = primary
                            )
                        }
                        CyberHorizontalMeterSlider(
                            value = amplifierState.surroundRoomSize,
                            valueRange = 0f..1f,
                            accentColor = primary,
                            secondaryColor = tertiary,
                            onValueChange = { room ->
                                onUpdateAmplifier {
                                    it.copy(
                                        surroundRoomSize = room,
                                        spatial360Enabled = true
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .testTag("spatial_room_size_slider")
                        )
                    }
                }
            }

            // 2. Dynamic Album Skin & Dark/Light Mode Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dynamic_skin_settings_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, primary.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = null,
                            tint = primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Column {
                            Text(
                                text = "PERSONALIZACIÓN & SKINS DINÁMICAS",
                                style = MaterialTheme.typography.titleLarge,
                                color = primary
                            )
                            Text(
                                text = "Adapta el chasis cibernético, colores de carátula y modo oscuro/claro",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Dynamic Album Art Skinning Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Album,
                                contentDescription = null,
                                tint = secondary
                            )
                            Column {
                                Text(
                                    text = "Skin Dinámica por Carátula de Álbum",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Extrae el espectro cromático de '${currentTrack.album}' con transiciones suaves",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = themePrefs.dynamicAlbumSkin,
                            onCheckedChange = onToggleDynamicAlbumSkin,
                            modifier = Modifier.testTag("dynamic_album_skin_switch")
                        )
                    }

                    // Live Extracted Dominant Album Art Swatches Preview
                    val cyberAccents = com.example.ui.theme.CyberAmpThemeTokens.accents
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Espectro extraído de carátula:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(cyberAccents.extractedPalette.dominantPrimary)
                            )
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(cyberAccents.extractedPalette.vibrantSecondary)
                            )
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(cyberAccents.extractedPalette.ambientTertiary)
                            )
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(cyberAccents.extractedPalette.deepSurface)
                            )
                        }
                    }

                    // Dark vs Light Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (themePrefs.isDarkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                contentDescription = null,
                                tint = primary
                            )
                            Column {
                                Text(
                                    text = if (themePrefs.isDarkTheme) "Tema Oscuro Cibernético (Activo)" else "Tema Claro de Estudio Diurno (Activo)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Alterna entre atmósfera nocturna OLED y estudio de alta luminosidad",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = themePrefs.isDarkTheme,
                            onCheckedChange = onToggleDarkTheme,
                            modifier = Modifier.testTag("dark_light_theme_switch")
                        )
                    }

                    // High Contrast HUD Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Contrast,
                                contentDescription = null,
                                tint = secondary
                            )
                            Column {
                                Text(
                                    text = "Chasis OLED de Ultra-Contraste",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Fondo negro profundo para máxima legibilidad de medidores",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = themePrefs.highContrastHud,
                            onCheckedChange = onToggleHighContrastHud,
                            modifier = Modifier.testTag("high_contrast_hud_switch")
                        )
                    }

                    // Neon Border Glow Slider
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Intensidad de Neón y Halo del Chasis",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${(themePrefs.glowingBordersStrength * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = primary
                            )
                        }
                        Slider(
                            value = themePrefs.glowingBordersStrength,
                            onValueChange = onGlowStrengthChange,
                            valueRange = 0.15f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = primary,
                                activeTrackColor = primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("glow_strength_slider")
                        )
                    }
                }
            }

            // 3. Cybernetic Skin Styles Selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, primary.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "COLECCIÓN DE SKINS CIBERNÉTICAS",
                        style = MaterialTheme.typography.labelLarge,
                        color = primary
                    )

                    CyberSkinStyle.entries.forEach { skin ->
                        val isSelected = themePrefs.skinStyle == skin
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectSkinStyle(skin) }
                                .testTag("skin_style_card_${skin.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    Color(skin.primaryHex).copy(alpha = 0.14f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                }
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(skin.primaryHex) else Color(skin.primaryHex).copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color(skin.primaryHex))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color(skin.secondaryHex))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color(skin.tertiaryHex))
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = skin.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = skin.subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Seleccionado",
                                        tint = Color(skin.primaryHex)
                                    )
                                } else {
                                    RadioButton(
                                        selected = false,
                                        onClick = { onSelectSkinStyle(skin) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Default Visualizer Mode Preference
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, secondary.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "MODO DE VISUALIZADOR EN TIEMPO REAL",
                        style = MaterialTheme.typography.labelLarge,
                        color = secondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VisualizerMode.entries.forEach { mode ->
                            FilterChip(
                                selected = themePrefs.visualizerMode == mode,
                                onClick = { onSelectVisualizerMode(mode) },
                                label = {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.testTag("skin_vis_mode_${mode.name}")
                            )
                        }
                    }
                }
            }
        }
    }
}
