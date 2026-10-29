package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.EqPresetEntity
import com.example.model.AmplifierState
import com.example.model.ParametricBand
import com.example.viewmodel.ParametricEqualizerUiState
import java.util.Locale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EqAndAmpScreen(
    eqState: ParametricEqualizerUiState,
    onSelectBand: (Int) -> Unit,
    onBandGainChange: (Int, Float) -> Unit,
    onBandQChange: (Int, Float) -> Unit,
    onCycleBandFilterType: (Int) -> Unit,
    onToggleEqEnabled: (Boolean) -> Unit,
    onToggleAutoGain: (Boolean) -> Unit,
    onResetFlat: () -> Unit,
    onInvertCurve: () -> Unit,
    onApplyPreset: (EqPresetEntity) -> Unit,
    onSaveCustomPreset: (String) -> Unit,
    onDeleteCustomPreset: (Int) -> Unit,
    onUpdateAmplifier: ((AmplifierState) -> AmplifierState) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current

    val bands = eqState.bands
    val selectedBandIndex = eqState.selectedBandIndex
    val amplifierState = eqState.amplifierState
    val presets = eqState.presets
    val activePresetName = eqState.activePresetName

    var showSavePresetRow by remember { mutableStateOf(false) }
    var customPresetNameInput by remember { mutableStateOf("") }

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
            // 1. Parametric Frequency Response Curve & Cybernetic Fader Console Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("parametric_eq_curve_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, primary.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ECUALIZADOR PARAMÉTRICO 10-BANDAS",
                                style = MaterialTheme.typography.titleLarge,
                                color = primary
                            )
                            Text(
                                text = "Preset: $activePresetName${if (eqState.isDirtyFromPreset) " *" else ""} • Rango [-12dB, +12dB]",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = eqState.isEqualizerEnabled,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleEqEnabled(it)
                            },
                            modifier = Modifier.testTag("eq_master_enable_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onResetFlat()
                            },
                            modifier = Modifier.testTag("eq_flat_reset_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.RestartAlt,
                                contentDescription = "Plano",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("0 dB", style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onInvertCurve()
                            },
                            modifier = Modifier.testTag("eq_invert_curve_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SwapVert,
                                contentDescription = "Invertir curva",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Invertir", style = MaterialTheme.typography.labelSmall)
                        }

                        FilterChip(
                            selected = eqState.autoGainCompensation,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleAutoGain(!eqState.autoGainCompensation)
                            },
                            label = {
                                Text(
                                    text = "Auto-Gain (${String.format(Locale.US, "%+.1fdB", eqState.computedHeadroomOffsetDb)})",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.testTag("eq_auto_gain_chip")
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = { showSavePresetRow = !showSavePresetRow },
                            modifier = Modifier.testTag("eq_save_preset_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Save,
                                contentDescription = "Guardar Preset",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Guardar", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (showSavePresetRow) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customPresetNameInput,
                                onValueChange = { customPresetNameInput = it },
                                label = { Text("Nombre del Preset Personalizado") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("custom_preset_name_input")
                            )
                            Button(
                                onClick = {
                                    onSaveCustomPreset(customPresetNameInput)
                                    customPresetNameInput = ""
                                    showSavePresetRow = false
                                },
                                modifier = Modifier.testTag("confirm_save_preset_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Text("Crear")
                            }
                        }
                    }

                    // Interactive Parametric Response Curve Canvas (20Hz - 20kHz) with Touch Node Dragging
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CURVA INTERACTIVA 20Hz–20kHz (ARRASTRA NODOS EN TIEMPO REAL)",
                                style = MaterialTheme.typography.labelSmall,
                                color = primary
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "%s Hz: %+.1f dB (Q %.2f)",
                                    eqState.selectedBand.label,
                                    eqState.selectedBand.gainDb,
                                    eqState.selectedBand.qFactor
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(175.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF05080F))
                                .padding(10.dp)
                        ) {
                            ParametricCurveCanvas(
                                bands = eqState.effectiveBandsForDsp,
                                selectedBandIndex = selectedBandIndex,
                                preampGainDb = amplifierState.preampGainDb + eqState.computedHeadroomOffsetDb,
                                primaryColor = if (eqState.isEqualizerEnabled) primary else primary.copy(alpha = 0.4f),
                                secondaryColor = secondary,
                                tertiaryColor = tertiary,
                                onSelectBand = onSelectBand,
                                onBandGainChange = onBandGainChange
                            )
                        }
                    }

                    // Selected Band Fine-Tuning Inspector (Frequency, Gain, Q-Factor, Filter Type)
                    val activeBand = eqState.selectedBand
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, secondary.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "BANDA ${activeBand.index + 1} (${activeBand.label} Hz) • ${activeBand.filterType.fullName}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = secondary
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = secondary.copy(alpha = 0.2f),
                                    modifier = Modifier
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onCycleBandFilterType(activeBand.index)
                                        }
                                        .testTag("cycle_filter_type_button")
                                ) {
                                    Text(
                                        text = "Filtro: ${activeBand.filterType.shortLabel} (Cambiar)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = secondary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Q-Factor: ${String.format(Locale.US, "%.2f", activeBand.qFactor)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.width(105.dp)
                                )
                                CyberHorizontalMeterSlider(
                                    value = activeBand.qFactor,
                                    valueRange = ParametricEqualizerUiState.MIN_Q_FACTOR..ParametricEqualizerUiState.MAX_Q_FACTOR,
                                    accentColor = secondary,
                                    secondaryColor = primary,
                                    onValueChange = { onBandQChange(activeBand.index, it) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(32.dp)
                                        .testTag("selected_band_q_slider")
                                )
                            }
                        }
                    }

                    // 10-Band Cybernetic Vertical LED Fader Rack (Real-Time Multi-Segment Hardware Console)
                    Text(
                        text = "CONSOLA DE FADERS VERTICALES CIBERNÉTICOS (LED SEGMENTADO)",
                        style = MaterialTheme.typography.labelSmall,
                        color = primary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF060A12))
                            .padding(horizontal = 10.dp, vertical = 12.dp)
                            .testTag("cyber_vertical_fader_rack"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        bands.forEach { band ->
                            val isSelected = band.index == selectedBandIndex
                            CyberVerticalLedFaderStrip(
                                band = band,
                                isSelected = isSelected,
                                primaryColor = primary,
                                secondaryColor = secondary,
                                onSelectBand = { onSelectBand(band.index) },
                                onGainChange = { newGain ->
                                    onSelectBand(band.index)
                                    onBandGainChange(band.index, newGain)
                                },
                                modifier = Modifier.testTag("eq_vertical_fader_${band.index}")
                            )
                        }
                    }

                    // 10-Band Individual Precision Bipolar Cyber Sliders (-12dB to +12dB)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        bands.forEach { band ->
                            val isSelected = band.index == selectedBandIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) primary.copy(alpha = 0.12f) else Color.Transparent
                                    )
                                    .clickable { onSelectBand(band.index) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${band.label}Hz",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.width(48.dp)
                                )

                                CyberBipolarLedFaderSlider(
                                    gainDb = band.gainDb,
                                    isSelected = isSelected,
                                    primaryColor = primary,
                                    secondaryColor = secondary,
                                    onGainChange = { newGain ->
                                        onSelectBand(band.index)
                                        onBandGainChange(band.index, newGain)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .testTag("eq_band_slider_${band.index}")
                                )

                                Text(
                                    text = String.format(Locale.US, "%+.1fdB", band.gainDb),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (band.gainDb >= 0f) primary else secondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.width(54.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Presets Bank Card (Built-In + User Saved in Room DB)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, primary.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "BANCO DE PRESETS ACÚSTICOS",
                        style = MaterialTheme.typography.labelLarge,
                        color = primary
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { preset ->
                            val selected = preset.name == activePresetName
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onApplyPreset(preset)
                                    },
                                    label = {
                                        Text(
                                            text = preset.name,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier.testTag("preset_chip_${preset.id}")
                                )
                                if (!preset.isBuiltIn) {
                                    IconButton(
                                        onClick = { onDeleteCustomPreset(preset.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "Eliminar preset",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Pro Amplifier, Vacuum Tube EL34 Saturation & 3D Surround Rack
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("amplifier_rack_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, secondary.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = null,
                            tint = secondary
                        )
                        Text(
                            text = "AMPLIFICADOR & SONIDO ENVOLVENTE 3D",
                            style = MaterialTheme.typography.titleLarge,
                            color = secondary
                        )
                    }

                    AmpControlSliderRow(
                        label = "Preamp Gain (Preamplificador)",
                        valueText = String.format(Locale.US, "%+.1f dB", amplifierState.preampGainDb),
                        value = amplifierState.preampGainDb,
                        range = -6f..12f,
                        accentColor = primary,
                        testTag = "amp_preamp_slider",
                        onValueChange = { v -> onUpdateAmplifier { it.copy(preampGainDb = v) } }
                    )

                    AmpControlSliderRow(
                        label = "Saturación de Válvulas EL34 (Tube Warmth)",
                        valueText = "${(amplifierState.tubeWarmth * 100).toInt()}%",
                        value = amplifierState.tubeWarmth,
                        range = 0f..1f,
                        accentColor = Color(0xFFFF9E3B),
                        testTag = "amp_tube_warmth_slider",
                        onValueChange = { v -> onUpdateAmplifier { it.copy(tubeWarmth = v) } }
                    )

                    AmpControlSliderRow(
                        label = "Sub-Bass Harmonic Booster",
                        valueText = "${(amplifierState.subBassBoost * 100).toInt()}%",
                        value = amplifierState.subBassBoost,
                        range = 0f..1f,
                        accentColor = primary,
                        testTag = "amp_sub_bass_slider",
                        onValueChange = { v -> onUpdateAmplifier { it.copy(subBassBoost = v) } }
                    )

                    AmpControlSliderRow(
                        label = "Loudness Enhancer (Compensación Dinámica)",
                        valueText = "${(amplifierState.loudnessEnhancer * 100).toInt()}%",
                        value = amplifierState.loudnessEnhancer,
                        range = 0f..1f,
                        accentColor = secondary,
                        testTag = "amp_loudness_slider",
                        onValueChange = { v -> onUpdateAmplifier { it.copy(loudnessEnhancer = v) } }
                    )

                    // 3D Surround & 360 Spatial Section
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, primary.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SurroundSound,
                                        contentDescription = null,
                                        tint = primary
                                    )
                                    Column {
                                        Text(
                                            text = "Motor Holográfico Envolvente 3D & 360°",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Procesamiento Mid/Side, HRTF binaural y virtualización",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = amplifierState.surroundEnabled,
                                    onCheckedChange = { enabled ->
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onUpdateAmplifier { it.copy(surroundEnabled = enabled) }
                                    },
                                    modifier = Modifier.testTag("surround_enable_switch")
                                )
                            }

                            AmpControlSliderRow(
                                label = "Amplitud Estéreo (Crossfeed a Holográfico)",
                                valueText = "${(amplifierState.stereoWidth * 100).toInt()}%",
                                value = amplifierState.stereoWidth,
                                range = 0.5f..2.0f,
                                accentColor = primary,
                                testTag = "amp_stereo_width_slider",
                                onValueChange = { v -> onUpdateAmplifier { it.copy(stereoWidth = v) } }
                            )

                            AmpControlSliderRow(
                                label = "Tamaño de Sala Acústica 3D",
                                valueText = "${(amplifierState.surroundRoomSize * 100).toInt()}%",
                                value = amplifierState.surroundRoomSize,
                                range = 0f..1f,
                                accentColor = tertiary,
                                testTag = "amp_room_size_slider",
                                onValueChange = { v -> onUpdateAmplifier { it.copy(surroundRoomSize = v) } }
                            )

                            AmpControlSliderRow(
                                label = "Balance Estéreo (L / R)",
                                valueText = when {
                                    amplifierState.balancePan < -0.05f -> "L ${(amplifierState.balancePan * -100).toInt()}%"
                                    amplifierState.balancePan > 0.05f -> "R ${(amplifierState.balancePan * 100).toInt()}%"
                                    else -> "Centro"
                                },
                                value = amplifierState.balancePan,
                                range = -1f..1f,
                                accentColor = secondary,
                                testTag = "amp_balance_slider",
                                onValueChange = { v -> onUpdateAmplifier { it.copy(balancePan = v) } }
                            )

                            AmpControlSliderRow(
                                label = "Pitch & Tempo DSP Master",
                                valueText = String.format(Locale.US, "%.2fx", amplifierState.pitchTempoRatio),
                                value = amplifierState.pitchTempoRatio,
                                range = 0.75f..1.25f,
                                accentColor = primary,
                                testTag = "amp_pitch_tempo_slider",
                                onValueChange = { v -> onUpdateAmplifier { it.copy(pitchTempoRatio = v) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive 2D Parametric Response Curve Canvas (20Hz - 20kHz, -12dB to +12dB)
 * Supports direct touch and drag manipulation of the 10 parametric frequency nodes.
 */
@Composable
private fun ParametricCurveCanvas(
    bands: List<ParametricBand>,
    selectedBandIndex: Int,
    preampGainDb: Float,
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color,
    onSelectBand: (Int) -> Unit,
    onBandGainChange: (Int, Float) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val currentBands by rememberUpdatedState(bands)
    val currentOnSelectBand by rememberUpdatedState(onSelectBand)
    val currentOnBandGainChange by rememberUpdatedState(onBandGainChange)

    val minLog = remember { ln(25f) }
    val maxLog = remember { ln(18000f) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .testTag("parametric_eq_interactive_canvas")
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    val h = size.height.toFloat().coerceAtLeast(1f)
                    val midY = h / 2f
                    val nearestIdx = currentBands.indices.minByOrNull { idx ->
                        val b = currentBands[idx]
                        val normX = ((ln(b.frequencyHz.toFloat()) - minLog) / (maxLog - minLog))
                            .coerceIn(0.02f, 0.98f)
                        abs(offset.x - normX * w)
                    } ?: 0
                    val gainDb = (((midY - offset.y) / (h * 0.44f)) * 12f)
                        .coerceIn(ParametricEqualizerUiState.MIN_GAIN_DB, ParametricEqualizerUiState.MAX_GAIN_DB)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    currentOnSelectBand(nearestIdx)
                    currentOnBandGainChange(nearestIdx, gainDb)
                }
            }
            .pointerInput(Unit) {
                var activeDragBand = -1
                var lastTickHalfDb = Int.MIN_VALUE
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        activeDragBand = currentBands.indices.minByOrNull { idx ->
                            val b = currentBands[idx]
                            val normX = ((ln(b.frequencyHz.toFloat()) - minLog) / (maxLog - minLog))
                                .coerceIn(0.02f, 0.98f)
                            abs(startOffset.x - normX * w)
                        } ?: 0
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        currentOnSelectBand(activeDragBand)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        val midY = h / 2f
                        // Allow sliding across bands horizontally or adjusting the locked band vertically
                        val nearestIdx = currentBands.indices.minByOrNull { idx ->
                            val b = currentBands[idx]
                            val normX = ((ln(b.frequencyHz.toFloat()) - minLog) / (maxLog - minLog))
                                .coerceIn(0.02f, 0.98f)
                            abs(change.position.x - normX * w)
                        } ?: activeDragBand.coerceAtLeast(0)
                        activeDragBand = nearestIdx
                        val rawGain = (((midY - change.position.y) / (h * 0.44f)) * 12f)
                            .coerceIn(ParametricEqualizerUiState.MIN_GAIN_DB, ParametricEqualizerUiState.MAX_GAIN_DB)
                        val snappedGain = if (abs(rawGain) < 0.25f) 0f else rawGain
                        val tickHalfDb = (snappedGain * 2f).roundToInt()
                        if (tickHalfDb != lastTickHalfDb) {
                            lastTickHalfDb = tickHalfDb
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        currentOnSelectBand(activeDragBand)
                        currentOnBandGainChange(activeDragBand, snappedGain)
                    }
                )
            }
    ) {
        val w = size.width
        val h = size.height
        val midY = h / 2f

        // Zero dB reference line & +/- 6dB / +/- 12dB grid lines
        drawLine(
            color = primaryColor.copy(alpha = 0.35f),
            start = Offset(0f, midY),
            end = Offset(w, midY),
            strokeWidth = 1.6f
        )
        drawLine(
            color = primaryColor.copy(alpha = 0.14f),
            start = Offset(0f, h * 0.28f),
            end = Offset(w, h * 0.28f),
            strokeWidth = 1f
        )
        drawLine(
            color = primaryColor.copy(alpha = 0.14f),
            start = Offset(0f, h * 0.72f),
            end = Offset(w, h * 0.72f),
            strokeWidth = 1f
        )

        // Vertical log-frequency grid lines for each of the 10 bands
        bands.forEachIndexed { idx, band ->
            val normX = ((ln(band.frequencyHz.toFloat()) - minLog) / (maxLog - minLog)).coerceIn(0.02f, 0.98f)
            val x = normX * w
            val isSelected = idx == selectedBandIndex
            drawLine(
                color = if (isSelected) secondaryColor.copy(alpha = 0.35f) else primaryColor.copy(alpha = 0.08f),
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = if (isSelected) 1.5f else 1f
            )
        }

        val curvePath = Path()
        val fillPath = Path()
        val steps = 130

        for (s in 0..steps) {
            val normX = s.toFloat() / steps
            val x = normX * w
            val freq = exp(minLog + normX * (maxLog - minLog))

            // Sum parametric bell contributions in log-frequency space
            var totalDb = preampGainDb * 0.25f
            for (b in bands) {
                val distOctaves = ln(freq / b.frequencyHz.toFloat()) / ln(2f)
                val bandwidth = (1.35f / b.qFactor.coerceAtLeast(0.4f))
                val bell = exp(-(distOctaves * distOctaves) / (2f * bandwidth * bandwidth * 0.35f))
                totalDb += b.gainDb * bell
            }

            val clampedDb = totalDb.coerceIn(-14f, 14f)
            val y = midY - (clampedDb / 14f) * (h * 0.44f)

            if (s == 0) {
                curvePath.moveTo(x, y)
                fillPath.moveTo(x, midY)
                fillPath.lineTo(x, y)
            } else {
                curvePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(w, midY)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.38f),
                    secondaryColor.copy(alpha = 0.14f),
                    Color.Transparent
                )
            )
        )

        drawPath(
            path = curvePath,
            brush = Brush.horizontalGradient(listOf(primaryColor, secondaryColor, tertiaryColor)),
            style = Stroke(width = 3.8f, cap = StrokeCap.Round)
        )

        // Draw Interactive Band Nodes & Crosshairs
        bands.forEachIndexed { idx, band ->
            val normX = ((ln(band.frequencyHz.toFloat()) - minLog) / (maxLog - minLog)).coerceIn(0.02f, 0.98f)
            val x = normX * w
            val y = midY - (band.gainDb.coerceIn(-12f, 12f) / 14f) * (h * 0.44f)
            val isSelected = idx == selectedBandIndex

            // Stem from 0 dB center line to node
            drawLine(
                color = if (isSelected) secondaryColor.copy(alpha = 0.65f) else primaryColor.copy(alpha = 0.28f),
                start = Offset(x, midY),
                end = Offset(x, y),
                strokeWidth = if (isSelected) 2.2f else 1.2f
            )

            if (isSelected) {
                // Q-factor bandwidth aura ring
                val qRadius = (24f / band.qFactor.coerceIn(0.5f, 3.5f)).coerceIn(12f, 28f)
                drawCircle(
                    color = secondaryColor.copy(alpha = 0.25f),
                    radius = qRadius,
                    center = Offset(x, y)
                )
                drawCircle(
                    color = secondaryColor,
                    radius = 12f,
                    center = Offset(x, y),
                    style = Stroke(width = 1.8f)
                )
            }
            drawCircle(
                color = if (isSelected) secondaryColor else primaryColor,
                radius = if (isSelected) 7.5f else 5.5f,
                center = Offset(x, y)
            )
        }
    }
}

/**
 * Custom Vertical Cybernetic LED-Segmented Fader Strip for the 10-band rack.
 */
@Composable
private fun CyberVerticalLedFaderStrip(
    band: ParametricBand,
    isSelected: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    onSelectBand: () -> Unit,
    onGainChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val currentOnGainChange by rememberUpdatedState(onGainChange)
    val currentOnSelect by rememberUpdatedState(onSelectBand)

    Column(
        modifier = modifier
            .width(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) primaryColor.copy(alpha = 0.16f) else Color(0xFF0B101B)
            )
            .clickable { onSelectBand() }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = String.format(Locale.US, "%+.1f", band.gainDb),
            style = MaterialTheme.typography.labelSmall,
            color = if (band.gainDb >= 0f) primaryColor else secondaryColor,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier
                .width(28.dp)
                .height(132.dp)
                .pointerInput(band.index) {
                    detectTapGestures { offset ->
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        val norm = (1f - (offset.y / h)).coerceIn(0f, 1f)
                        val rawDb = -12f + norm * 24f
                        val snapped = if (abs(rawDb) < 0.4f) 0f else rawDb
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        currentOnSelect()
                        currentOnGainChange(snapped)
                    }
                }
                .pointerInput(band.index) {
                    var lastStep = Int.MIN_VALUE
                    detectDragGestures(
                        onDragStart = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentOnSelect()
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val h = size.height.toFloat().coerceAtLeast(1f)
                            val norm = (1f - (change.position.y / h)).coerceIn(0f, 1f)
                            val rawDb = -12f + norm * 24f
                            val snapped = if (abs(rawDb) < 0.35f) 0f else rawDb
                            val step = (snapped * 2f).roundToInt()
                            if (step != lastStep) {
                                lastStep = step
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            currentOnGainChange(snapped)
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val midY = h / 2f

                // Center slot rail
                drawRoundRect(
                    color = Color(0xFF04070D),
                    topLeft = Offset(w * 0.38f, 4f),
                    size = Size(w * 0.24f, h - 8f),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // Segmented LED ticks along the fader slot (-12dB to +12dB)
                val ledCount = 17
                val normGain = ((band.gainDb.coerceIn(-12f, 12f) + 12f) / 24f).coerceIn(0f, 1f)
                val thumbY = (1f - normGain) * (h - 16f) + 8f

                for (i in 0 until ledCount) {
                    val segNorm = 1f - (i.toFloat() / (ledCount - 1)) // 1.0 (+12dB) at top, 0.0 (-12dB) at bottom
                    val y = 8f + (i.toFloat() / (ledCount - 1)) * (h - 16f)
                    val isCenterZero = i == ledCount / 2
                    val isLit = if (band.gainDb >= 0f) {
                        segNorm in 0.5f..normGain
                    } else {
                        segNorm in normGain..0.5f
                    }
                    val ledColor = when {
                        isCenterZero -> Color.White.copy(alpha = 0.8f)
                        isLit && band.gainDb >= 0f -> primaryColor
                        isLit && band.gainDb < 0f -> secondaryColor
                        else -> primaryColor.copy(alpha = 0.14f)
                    }
                    val tickW = if (isCenterZero) w * 0.88f else w * 0.68f
                    drawLine(
                        color = ledColor,
                        start = Offset((w - tickW) / 2f, y),
                        end = Offset((w + tickW) / 2f, y),
                        strokeWidth = if (isCenterZero || isLit) 2.4f else 1.4f,
                        cap = StrokeCap.Round
                    )
                }

                // Glowing active bar from 0dB center to thumb
                val topBar = minOf(midY, thumbY)
                val barHeight = abs(thumbY - midY).coerceAtLeast(2f)
                drawRoundRect(
                    color = (if (band.gainDb >= 0f) primaryColor else secondaryColor).copy(alpha = 0.55f),
                    topLeft = Offset(w * 0.34f, topBar),
                    size = Size(w * 0.32f, barHeight),
                    cornerRadius = CornerRadius(3f, 3f)
                )

                // Cybernetic Fader Cap Thumb
                val capH = 14f
                val activeColor = if (isSelected) secondaryColor else primaryColor
                drawRoundRect(
                    color = Color(0xFF141E30),
                    topLeft = Offset(1f, thumbY - capH / 2f),
                    size = Size(w - 2f, capH),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(1f, thumbY - capH / 2f),
                    size = Size(w - 2f, capH),
                    cornerRadius = CornerRadius(4f, 4f),
                    style = Stroke(width = 2f)
                )
                drawLine(
                    color = Color.White,
                    start = Offset(5f, thumbY),
                    end = Offset(w - 5f, thumbY),
                    strokeWidth = 2f
                )
            }
        }

        Text(
            text = band.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Custom Horizontal Bipolar Cybernetic LED Fader Slider (-12dB to +12dB) with 0dB center detent and haptic ticks.
 */
@Composable
private fun CyberBipolarLedFaderSlider(
    gainDb: Float,
    isSelected: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    onGainChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val currentOnGainChange by rememberUpdatedState(onGainChange)

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    val norm = (offset.x / w).coerceIn(0f, 1f)
                    val rawDb = -12f + norm * 24f
                    val snapped = if (abs(rawDb) < 0.35f) 0f else rawDb
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    currentOnGainChange(snapped)
                }
            }
            .pointerInput(Unit) {
                var lastHalfDbStep = Int.MIN_VALUE
                detectDragGestures(
                    onDragStart = { offset ->
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val norm = (offset.x / w).coerceIn(0f, 1f)
                        val rawDb = -12f + norm * 24f
                        val snapped = if (abs(rawDb) < 0.35f) 0f else rawDb
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        currentOnGainChange(snapped)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val norm = (change.position.x / w).coerceIn(0f, 1f)
                        val rawDb = -12f + norm * 24f
                        val snapped = if (abs(rawDb) < 0.35f) 0f else rawDb
                        val step = (snapped * 2f).roundToInt()
                        if (step != lastHalfDbStep) {
                            lastHalfDbStep = step
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        currentOnGainChange(snapped)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val midX = w / 2f
            val midY = h / 2f
            val norm = ((gainDb.coerceIn(-12f, 12f) + 12f) / 24f).coerceIn(0f, 1f)
            val thumbX = norm * w

            // Dark cybernetic recessed slot
            drawRoundRect(
                color = Color(0xFF070B14),
                topLeft = Offset(0f, midY - 6f),
                size = Size(w, 12f),
                cornerRadius = CornerRadius(6f, 6f)
            )

            // 24 Segmented LED bars (-12dB to +12dB)
            val segments = 24
            val segGap = w / segments
            for (s in 0..segments) {
                val segNorm = s.toFloat() / segments
                val x = segNorm * w
                val isCenter = s == segments / 2
                val isLit = if (gainDb >= 0f) {
                    segNorm in 0.5f..norm
                } else {
                    segNorm in norm..0.5f
                }
                val segColor = when {
                    isCenter -> Color.White.copy(alpha = 0.85f)
                    isLit && gainDb >= 0f -> primaryColor
                    isLit && gainDb < 0f -> secondaryColor
                    else -> primaryColor.copy(alpha = 0.16f)
                }
                val tickH = if (isCenter) h * 0.72f else if (isLit) h * 0.52f else h * 0.32f
                drawLine(
                    color = segColor,
                    start = Offset(x, midY - tickH / 2f),
                    end = Offset(x, midY + tickH / 2f),
                    strokeWidth = if (isCenter) 2.5f else segGap * 0.42f,
                    cap = StrokeCap.Round
                )
            }

            // Active bipolar neon beam from 0dB center to thumb
            val startX = minOf(midX, thumbX)
            val beamW = abs(thumbX - midX).coerceAtLeast(2f)
            val activeAccent = if (gainDb >= 0f) primaryColor else secondaryColor
            drawRoundRect(
                color = activeAccent.copy(alpha = 0.45f),
                topLeft = Offset(startX, midY - 4f),
                size = Size(beamW, 8f),
                cornerRadius = CornerRadius(4f, 4f)
            )

            // Cybernetic Chamfered Fader Thumb
            val capW = 16f
            val capH = h * 0.78f
            val clampedThumbX = thumbX.coerceIn(capW / 2f, w - capW / 2f)
            drawRoundRect(
                color = Color(0xFF101929),
                topLeft = Offset(clampedThumbX - capW / 2f, midY - capH / 2f),
                size = Size(capW, capH),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = if (isSelected) secondaryColor else activeAccent,
                topLeft = Offset(clampedThumbX - capW / 2f, midY - capH / 2f),
                size = Size(capW, capH),
                cornerRadius = CornerRadius(4f, 4f),
                style = Stroke(width = if (isSelected) 2.4f else 1.8f)
            )
            drawLine(
                color = Color.White,
                start = Offset(clampedThumbX, midY - capH * 0.28f),
                end = Offset(clampedThumbX, midY + capH * 0.28f),
                strokeWidth = 2f
            )
        }
    }
}

/**
 * Custom Cybernetic Unipolar LED Meter Slider for Q-Factor and Amplifier Rack parameters.
 */
@Composable
fun CyberHorizontalMeterSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    secondaryColor: Color,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val minVal = valueRange.start
    val maxVal = valueRange.endInclusive
    val span = (maxVal - minVal).coerceAtLeast(0.0001f)

    Box(
        modifier = modifier
            .pointerInput(minVal, maxVal) {
                detectTapGestures { offset ->
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    val norm = (offset.x / w).coerceIn(0f, 1f)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    currentOnValueChange(minVal + norm * span)
                }
            }
            .pointerInput(minVal, maxVal) {
                var lastTick = Int.MIN_VALUE
                detectDragGestures(
                    onDragStart = { offset ->
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val norm = (offset.x / w).coerceIn(0f, 1f)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        currentOnValueChange(minVal + norm * span)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val norm = (change.position.x / w).coerceIn(0f, 1f)
                        val tick = (norm * 20f).roundToInt()
                        if (tick != lastTick) {
                            lastTick = tick
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        currentOnValueChange(minVal + norm * span)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val midY = h / 2f
            val norm = ((value - minVal) / span).coerceIn(0f, 1f)
            val thumbX = norm * w

            drawRoundRect(
                color = Color(0xFF070B14),
                topLeft = Offset(0f, midY - 6f),
                size = Size(w, 12f),
                cornerRadius = CornerRadius(6f, 6f)
            )

            val segments = 22
            val segW = (w / segments) * 0.65f
            for (i in 0 until segments) {
                val segNorm = i.toFloat() / (segments - 1)
                val x = (i.toFloat() / segments) * w
                val isLit = segNorm <= norm
                drawRoundRect(
                    color = if (isLit) accentColor else accentColor.copy(alpha = 0.16f),
                    topLeft = Offset(x, midY - 4.5f),
                    size = Size(segW, 9f),
                    cornerRadius = CornerRadius(2.5f, 2.5f)
                )
            }

            val capW = 16f
            val capH = h * 0.76f
            val clampedX = thumbX.coerceIn(capW / 2f, w - capW / 2f)
            drawRoundRect(
                color = Color(0xFF101929),
                topLeft = Offset(clampedX - capW / 2f, midY - capH / 2f),
                size = Size(capW, capH),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = accentColor,
                topLeft = Offset(clampedX - capW / 2f, midY - capH / 2f),
                size = Size(capW, capH),
                cornerRadius = CornerRadius(4f, 4f),
                style = Stroke(width = 2f)
            )
            drawLine(
                color = secondaryColor,
                start = Offset(clampedX, midY - capH * 0.25f),
                end = Offset(clampedX, midY + capH * 0.25f),
                strokeWidth = 2f
            )
        }
    }
}

@Composable
private fun AmpControlSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    testTag: String,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelLarge,
                color = accentColor
            )
        }
        CyberHorizontalMeterSlider(
            value = value,
            valueRange = range,
            accentColor = accentColor,
            secondaryColor = Color.White,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .testTag(testTag)
        )
    }
}
