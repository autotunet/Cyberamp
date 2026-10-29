package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SpatialAudioOff
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.audio.TelemetrySnapshot
import com.example.data.UserThemePreferences
import com.example.model.AmplifierState
import com.example.model.TrackItem
import com.example.model.VisualizerMode
import com.example.viewmodel.MainTab
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class DeckGestureMode {
    NONE,
    HORIZONTAL_TRACK,
    VERTICAL_VOLUME
}

@Composable
fun PlayerDeckScreen(
    currentTrack: TrackItem,
    isPlaying: Boolean,
    currentPositionSec: Float,
    shuffleEnabled: Boolean,
    repeatEnabled: Boolean,
    isFavorite: Boolean,
    activePresetName: String,
    amplifierState: AmplifierState,
    telemetry: TelemetrySnapshot,
    themePrefs: UserThemePreferences,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMasterVolumeChange: (Float) -> Unit,
    onToggleSurround: () -> Unit,
    onToggleSpatial360: () -> Unit = onToggleSurround,
    onCycleVisualizerMode: (VisualizerMode) -> Unit,
    onNavigateTab: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current

    val currentMasterVolume by rememberUpdatedState(amplifierState.masterVolume)
    val currentOnMasterVolumeChange by rememberUpdatedState(onMasterVolumeChange)
    val currentOnSkipNext by rememberUpdatedState(onSkipNext)
    val currentOnSkipPrevious by rememberUpdatedState(onSkipPrevious)

    var gestureMode by remember { mutableStateOf(DeckGestureMode.NONE) }
    var gestureHudLabel by remember { mutableStateOf("") }
    var gestureVolumePreview by remember { mutableFloatStateOf(amplifierState.masterVolume) }

    val haloAngle = if (isPlaying) {
        val infiniteTransition = rememberInfiniteTransition(label = "deckHalo")
        val animatedAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "haloAngle"
        )
        animatedAngle
    } else {
        0f
    }

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Cybernetic Winamp-Pro LCD Status & Hi-Res Telemetry Header
            CyberLcdHeaderCard(
                track = currentTrack,
                telemetry = telemetry,
                activePresetName = activePresetName,
                dynamicSkinActive = themePrefs.dynamicAlbumSkin,
                spatial360Active = amplifierState.spatial360Enabled,
                onOpenEq = { onNavigateTab(MainTab.EQ_AMP) },
                onOpenAi = { onNavigateTab(MainTab.AI_MOOD) }
            )

            // 2. Dynamic Album Cover Art with Custom Horizontal/Vertical Swipe Gestures, Haptics & 360° Spatial Halo
            com.example.ui.motion.CyberneticTrackTransition(
                targetTrack = currentTrack
            ) { animatedTrack ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deck_album_card")
                        .pointerInput(Unit) {
                            var totalDx = 0f
                            var totalDy = 0f
                            var startVolume = 0.85f
                            var lastHapticVolStep = -1
                            var lockedAxis = DeckGestureMode.NONE
                            var thresholdCrossedHapticFired = false

                            detectDragGestures(
                                onDragStart = {
                                    totalDx = 0f
                                    totalDy = 0f
                                    startVolume = currentMasterVolume
                                    gestureVolumePreview = startVolume
                                    lastHapticVolStep = (startVolume * 20f).roundToInt()
                                    lockedAxis = DeckGestureMode.NONE
                                    thresholdCrossedHapticFired = false
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDx += dragAmount.x
                                    totalDy += dragAmount.y

                                    if (lockedAxis == DeckGestureMode.NONE) {
                                        if (abs(totalDx) > 20f || abs(totalDy) > 20f) {
                                            lockedAxis = if (abs(totalDx) >= abs(totalDy)) {
                                                DeckGestureMode.HORIZONTAL_TRACK
                                            } else {
                                                DeckGestureMode.VERTICAL_VOLUME
                                            }
                                            gestureMode = lockedAxis
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }

                                    when (lockedAxis) {
                                        DeckGestureMode.HORIZONTAL_TRACK -> {
                                            val swipeThreshold = 76f
                                            if (abs(totalDx) >= swipeThreshold && !thresholdCrossedHapticFired) {
                                                thresholdCrossedHapticFired = true
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                            gestureHudLabel = when {
                                                totalDx < -swipeThreshold -> "SOLTAR: SIGUIENTE PISTA >>"
                                                totalDx > swipeThreshold -> "<< SOLTAR: PISTA ANTERIOR"
                                                totalDx < 0f -> "DESLIZA A LA IZQUIERDA: SIGUIENTE >>"
                                                else -> "<< DESLIZA A LA DERECHA: ANTERIOR"
                                            }
                                        }

                                        DeckGestureMode.VERTICAL_VOLUME -> {
                                            val heightPx = size.height.toFloat().coerceAtLeast(180f)
                                            val deltaVol = -totalDy / (heightPx * 0.75f)
                                            val newVol = (startVolume + deltaVol).coerceIn(0f, 1f)
                                            gestureVolumePreview = newVol
                                            val volStep = (newVol * 20f).roundToInt()
                                            if (volStep != lastHapticVolStep) {
                                                lastHapticVolStep = volStep
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            gestureHudLabel = "VOLUMEN MAESTRO: ${(newVol * 100).roundToInt()}%"
                                            currentOnMasterVolumeChange(newVol)
                                        }

                                        DeckGestureMode.NONE -> Unit
                                    }
                                },
                                onDragEnd = {
                                    if (lockedAxis == DeckGestureMode.HORIZONTAL_TRACK) {
                                        val swipeThreshold = 76f
                                        if (totalDx <= -swipeThreshold) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            currentOnSkipNext()
                                        } else if (totalDx >= swipeThreshold) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            currentOnSkipPrevious()
                                        }
                                    }
                                    gestureMode = DeckGestureMode.NONE
                                    gestureHudLabel = ""
                                },
                                onDragCancel = {
                                    gestureMode = DeckGestureMode.NONE
                                    gestureHudLabel = ""
                                }
                            )
                        },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    ),
                    border = BorderStroke(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                primary.copy(alpha = themePrefs.glowingBordersStrength),
                                secondary.copy(alpha = themePrefs.glowingBordersStrength * 0.8f)
                            )
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = primary.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, primary.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "${animatedTrack.format.badge} • ${animatedTrack.format.sampleRateLabel} / ${animatedTrack.format.bitDepthLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = secondary.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, secondary.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "${animatedTrack.format.bitrateKbps} kbps • ${animatedTrack.bpm} BPM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = secondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier.size(224.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Rotating Cybernetic Halo + 360° Spatial Surround Nodes Ring
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val angle = if (isPlaying) haloAngle else 0f
                                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                                val outerRadius = size.minDimension / 2f

                                rotate(angle) {
                                    drawCircle(
                                        brush = Brush.sweepGradient(
                                            listOf(primary, secondary, tertiary, primary)
                                        ),
                                        radius = outerRadius - 6.dp.toPx(),
                                        style = Stroke(width = 4.dp.toPx())
                                    )
                                }

                                // 360-Degree Spatial Audio Visual Ring around Album Cover
                                if (amplifierState.spatial360Enabled) {
                                    drawCircle(
                                        color = secondary.copy(alpha = 0.35f),
                                        radius = outerRadius,
                                        style = Stroke(width = 1.5.dp.toPx())
                                    )

                                    val nodeAnglesDeg = floatArrayOf(-30f, 30f, 0f, -135f, 135f, 180f)
                                    for (i in nodeAnglesDeg.indices) {
                                        val energy = telemetry.spatialRadarNodes.getOrElse(i) { 0.4f }
                                        val rad = (nodeAnglesDeg[i] - 90f) * (PI.toFloat() / 180f)
                                        val nx = centerOffset.x + cos(rad) * outerRadius
                                        val ny = centerOffset.y + sin(rad) * outerRadius
                                        drawCircle(
                                            color = secondary.copy(alpha = 0.28f + 0.5f * energy),
                                            radius = (4f + energy * 6f).dp.toPx(),
                                            center = Offset(nx, ny)
                                        )
                                        drawCircle(
                                            color = primary,
                                            radius = 3.dp.toPx(),
                                            center = Offset(nx, ny)
                                        )
                                    }

                                    // Live 360° Azimuth Vector Indicator
                                    val azRad = (telemetry.spatial360AzimuthDeg - 90f) * (PI.toFloat() / 180f)
                                    val vx = centerOffset.x + cos(azRad) * outerRadius
                                    val vy = centerOffset.y + sin(azRad) * outerRadius
                                    drawCircle(
                                        color = Color(0xFF00E676),
                                        radius = 6.dp.toPx(),
                                        center = Offset(vx, vy)
                                    )
                                }
                            }

                            Image(
                                painter = painterResource(id = animatedTrack.coverResId),
                                contentDescription = "Carátula de ${animatedTrack.album}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(186.dp)
                                    .clip(RoundedCornerShape(18.dp))
                            )

                            // Live Gesture HUD Overlay (Horizontal Track Swipe / Vertical Volume Swipe)
                            this@Column.AnimatedVisibility(
                                visible = gestureMode != DeckGestureMode.NONE,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .testTag("deck_gesture_hud_overlay"),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xE6050811),
                                    border = BorderStroke(1.5.dp, secondary)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = gestureHudLabel,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (gestureMode == DeckGestureMode.VERTICAL_VOLUME) {
                                            LinearProgressIndicator(
                                                progress = { gestureVolumePreview.coerceIn(0f, 1f) },
                                                color = secondary,
                                                trackColor = primary.copy(alpha = 0.2f),
                                                modifier = Modifier
                                                    .width(140.dp)
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Subtle Gesture Hint Strip (Horizontal Swipe = Track, Vertical Swipe = Volume)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(primary.copy(alpha = 0.08f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("deck_gesture_hint_badge")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Swipe,
                                contentDescription = null,
                                tint = primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Gestos Hapticos: ↔ Cambiar Pista • ↕ Volumen Maestro",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = animatedTrack.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${animatedTrack.artist} — ${animatedTrack.album}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${animatedTrack.genre} • ${animatedTrack.sourceType.label}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onToggleFavorite()
                                },
                                modifier = Modifier.testTag("favorite_track_button")
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Marcar favorito",
                                    tint = if (isFavorite) secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 3. 360-Degree Spatial Audio Visual Indicator & Soundstage Radar Card
            Spatial360SoundstageIndicatorCard(
                amplifierState = amplifierState,
                telemetry = telemetry,
                primaryColor = primary,
                secondaryColor = secondary,
                tertiaryColor = tertiary,
                onToggleSpatial360 = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleSpatial360()
                },
                onOpenSpatialSettings = { onNavigateTab(MainTab.SKINS) }
            )

            // 4. Real-Time Multi-Mode Visualizer Rack (64-Band Spectrum / Oscilloscope / Dual VU Meters)
            VisualizerRackCard(
                telemetry = telemetry,
                visualizerMode = themePrefs.visualizerMode,
                primaryColor = primary,
                secondaryColor = secondary,
                tertiaryColor = tertiary,
                onSelectMode = onCycleVisualizerMode
            )

            // 5. Precision Scrubbing & Transport Deck + Quick Master & 3D Surround Controls
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transport_controls_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, primary.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Timecode & Seekbar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDuration(currentPositionSec.toInt()),
                            style = MaterialTheme.typography.labelLarge,
                            color = primary
                        )
                        Text(
                            text = if (isPlaying) "DSP ENGINE ACTIVE • 64-BIT FLOAT" else "READY / STANDBY",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isPlaying) secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDuration(currentTrack.durationSec),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Slider(
                        value = currentPositionSec.coerceIn(0f, max(1f, currentTrack.durationSec.toFloat())),
                        onValueChange = onSeekTo,
                        valueRange = 0f..max(1f, currentTrack.durationSec.toFloat()),
                        colors = SliderDefaults.colors(
                            thumbColor = primary,
                            activeTrackColor = primary,
                            inactiveTrackColor = primary.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("playback_seek_slider")
                    )

                    // Main Transport Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleShuffle()
                            },
                            modifier = Modifier.testTag("shuffle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shuffle,
                                contentDescription = "Aleatorio",
                                tint = if (shuffleEnabled) primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        FilledIconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSkipPrevious()
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .size(50.dp)
                                .testTag("previous_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipPrevious,
                                contentDescription = "Anterior",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        FilledIconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTogglePlayPause()
                            },
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .size(68.dp)
                                .testTag("play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        FilledIconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSkipNext()
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .size(50.dp)
                                .testTag("next_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = "Siguiente",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleRepeat()
                            },
                            modifier = Modifier.testTag("repeat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Repeat,
                                contentDescription = "Repetir",
                                tint = if (repeatEnabled) primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Master Volume & Quick 3D Surround / Preamp Status Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VolumeUp,
                            contentDescription = "Volumen Maestro",
                            tint = primary
                        )
                        Slider(
                            value = amplifierState.masterVolume,
                            onValueChange = onMasterVolumeChange,
                            valueRange = 0f..1f,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("master_volume_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = secondary,
                                activeTrackColor = secondary
                            )
                        )
                        Text(
                            text = "${(amplifierState.masterVolume * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = secondary,
                            modifier = Modifier.width(44.dp)
                        )

                        FilterChip(
                            selected = amplifierState.spatial360Enabled,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleSpatial360()
                            },
                            label = {
                                Text(
                                    text = "360° Audio",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (amplifierState.spatial360Enabled) {
                                        Icons.Filled.SurroundSound
                                    } else {
                                        Icons.Filled.SpatialAudioOff
                                    },
                                    contentDescription = "Audio Espacial 360 grados",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("surround_quick_toggle")
                        )
                    }
                }
            }
        }
    }
}

/**
 * Real-Time 360-Degree Spatial Audio Soundstage Radar & Visual Indicator Card on the Playback Deck.
 */
@Composable
private fun Spatial360SoundstageIndicatorCard(
    amplifierState: AmplifierState,
    telemetry: TelemetrySnapshot,
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color,
    onToggleSpatial360: () -> Unit,
    onOpenSpatialSettings: () -> Unit
) {
    val active = amplifierState.spatial360Enabled
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("spatial_360_indicator_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (active) secondaryColor.copy(alpha = 0.65f) else primaryColor.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 360° Soundstage Radar Canvas
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF050811))
                    .clickable(onClick = onToggleSpatial360)
                    .testTag("spatial_360_radar_canvas"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val maxR = size.minDimension / 2f - 6f

                    // Concentric acoustic field rings
                    drawCircle(
                        color = primaryColor.copy(alpha = if (active) 0.35f else 0.12f),
                        radius = maxR,
                        style = Stroke(width = 1.4f)
                    )
                    drawCircle(
                        color = primaryColor.copy(alpha = if (active) 0.22f else 0.08f),
                        radius = maxR * 0.58f,
                        style = Stroke(width = 1f)
                    )
                    // Crosshairs
                    drawLine(
                        color = primaryColor.copy(alpha = 0.18f),
                        start = Offset(cx, 4f),
                        end = Offset(cx, size.height - 4f),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = primaryColor.copy(alpha = 0.18f),
                        start = Offset(4f, cy),
                        end = Offset(size.width - 4f, cy),
                        strokeWidth = 1f
                    )

                    if (active) {
                        val nodeAngles = floatArrayOf(-30f, 30f, 0f, -135f, 135f, 180f)
                        for (i in nodeAngles.indices) {
                            val energy = telemetry.spatialRadarNodes.getOrElse(i) { 0.4f }
                            val rad = (nodeAngles[i] - 90f) * (PI.toFloat() / 180f)
                            val nx = cx + cos(rad) * (maxR * 0.82f)
                            val ny = cy + sin(rad) * (maxR * 0.82f)
                            drawCircle(
                                color = secondaryColor.copy(alpha = 0.35f + 0.55f * energy),
                                radius = 3f + energy * 4.5f,
                                center = Offset(nx, ny)
                            )
                        }

                        // Orbiting 360° Azimuth Vector Beam
                        val azRad = (telemetry.spatial360AzimuthDeg - 90f) * (PI.toFloat() / 180f)
                        val bx = cx + cos(azRad) * maxR
                        val by = cy + sin(azRad) * maxR
                        drawLine(
                            color = tertiaryColor,
                            start = Offset(cx, cy),
                            end = Offset(bx, by),
                            strokeWidth = 2.4f,
                            cap = StrokeCap.Round
                        )
                        drawCircle(
                            color = tertiaryColor,
                            radius = 4.5f,
                            center = Offset(bx, by)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Radar,
                            contentDescription = null,
                            tint = if (active) secondaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (active) "CAMPO ESPACIAL 360° ACTIVO" else "AUDIO ESPACIAL 360° EN BYPASS",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (active) secondaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = if (active) {
                        String.format(
                            Locale.US,
                            "Acimut HRTF: %03.0f° • Sala 3D: %d%% • %s",
                            telemetry.spatial360AzimuthDeg,
                            (amplifierState.surroundRoomSize * 100).roundToInt(),
                            if (amplifierState.spatialAutoOrbit) "Órbita 360° Auto" else "Vector Fijo"
                        )
                    } else {
                        "Activa los filtros binaurales ITD/ILD para envolvencia esférica 360°"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = active,
                        onClick = onToggleSpatial360,
                        label = {
                            Text(
                                text = if (active) "360° ON" else "360° OFF",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.testTag("deck_spatial_360_chip")
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = primaryColor.copy(alpha = 0.14f),
                        modifier = Modifier
                            .clickable(onClick = onOpenSpatialSettings)
                            .testTag("open_spatial_settings_button")
                    ) {
                        Text(
                            text = "Calibrar 360° en Ajustes",
                            style = MaterialTheme.typography.labelSmall,
                            color = primaryColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CyberLcdHeaderCard(
    track: TrackItem,
    telemetry: TelemetrySnapshot,
    activePresetName: String,
    dynamicSkinActive: Boolean,
    spatial360Active: Boolean,
    onOpenEq: () -> Unit,
    onOpenAi: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, primary.copy(alpha = 0.45f))
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = primary
                    )
                    Column {
                        Text(
                            text = "CYBERAMP DSP MATRIX v4.9",
                            style = MaterialTheme.typography.labelLarge,
                            color = primary
                        )
                        Text(
                            text = "DR ${String.format(Locale.US, "%.1f", telemetry.dynamicRangeLu)} LU • ${if (spatial360Active) "360° HRTF ON" else "STEREO DIRECT"} • ${if (dynamicSkinActive) "SKIN DINÁMICA" else "SKIN FIJA"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (telemetry.peakClipping) Color(0xFFFF1744).copy(alpha = 0.2f) else primary.copy(alpha = 0.14f),
                    border = BorderStroke(
                        1.dp,
                        if (telemetry.peakClipping) Color(0xFFFF1744) else primary.copy(alpha = 0.6f)
                    )
                ) {
                    Text(
                        text = if (telemetry.peakClipping) "CLIP LIMITER" else "HEADROOM OK",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (telemetry.peakClipping) Color(0xFFFF5252) else primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenEq)
                        .testTag("quick_eq_preset_chip"),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Equalizer,
                            contentDescription = "Ecualizador",
                            tint = primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "EQ: $activePresetName",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenAi)
                        .testTag("quick_ai_mood_chip"),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "IA Mood",
                            tint = secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "IA MOOD MIX",
                            style = MaterialTheme.typography.labelSmall,
                            color = secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VisualizerRackCard(
    telemetry: TelemetrySnapshot,
    visualizerMode: VisualizerMode,
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color,
    onSelectMode: (VisualizerMode) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("visualizer_rack_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ANALIZADOR ESPECTRAL DSP",
                    style = MaterialTheme.typography.labelLarge,
                    color = primaryColor
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    VisualizerMode.entries.forEach { mode ->
                        FilterChip(
                            selected = visualizerMode == mode,
                            onClick = { onSelectMode(mode) },
                            label = {
                                Text(
                                    text = when (mode) {
                                        VisualizerMode.SPECTRUM_64 -> "64-Bandas"
                                        VisualizerMode.OSCILLOSCOPE -> "Onda"
                                        VisualizerMode.VU_METERS -> "VU Pro"
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = primaryColor.copy(alpha = 0.22f),
                                selectedLabelColor = primaryColor
                            )
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF05080E))
                    .padding(10.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Subtle cybernetic measurement grid
                    val gridLines = 4
                    for (g in 1 until gridLines) {
                        val y = h * (g.toFloat() / gridLines)
                        drawLine(
                            color = primaryColor.copy(alpha = 0.12f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f
                        )
                    }

                    when (visualizerMode) {
                        VisualizerMode.SPECTRUM_64 -> {
                            val bars = telemetry.spectrum64.size
                            val barSpace = w / bars
                            val barWidth = (barSpace * 0.72f).coerceAtLeast(2f)
                            for (i in 0 until bars) {
                                val mag = telemetry.spectrum64[i].coerceIn(0.04f, 1f)
                                val barHeight = mag * h
                                val x = i * barSpace
                                val y = h - barHeight
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(secondaryColor, primaryColor, tertiaryColor.copy(alpha = 0.7f)),
                                        startY = y,
                                        endY = h
                                    ),
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(3f, 3f)
                                )
                                // Winamp-style floating peak cap
                                val capY = (y - 5f).coerceAtLeast(0f)
                                drawRect(
                                    color = secondaryColor,
                                    topLeft = Offset(x, capY),
                                    size = Size(barWidth, 3f)
                                )
                            }
                        }

                        VisualizerMode.OSCILLOSCOPE -> {
                            val leftPath = Path()
                            val rightPath = Path()
                            val pts = telemetry.waveformLeft.size
                            for (i in 0 until pts) {
                                val x = (i.toFloat() / (pts - 1)) * w
                                val yL = (h * 0.35f) + telemetry.waveformLeft[i] * (h * 0.28f)
                                val yR = (h * 0.68f) + telemetry.waveformRight[i] * (h * 0.28f)
                                if (i == 0) {
                                    leftPath.moveTo(x, yL)
                                    rightPath.moveTo(x, yR)
                                } else {
                                    leftPath.lineTo(x, yL)
                                    rightPath.lineTo(x, yR)
                                }
                            }
                            drawPath(
                                path = leftPath,
                                color = primaryColor,
                                style = Stroke(width = 3f, cap = StrokeCap.Round)
                            )
                            drawPath(
                                path = rightPath,
                                color = secondaryColor,
                                style = Stroke(width = 3f, cap = StrokeCap.Round)
                            )
                        }

                        VisualizerMode.VU_METERS -> {
                            val normL = ((telemetry.vuLeftDb + 48f) / 51f).coerceIn(0.04f, 1f)
                            val normR = ((telemetry.vuRightDb + 48f) / 51f).coerceIn(0.04f, 1f)
                            val segments = 28
                            val segSpace = w / segments
                            val segW = segSpace * 0.76f
                            val barH = h * 0.32f

                            for (s in 0 until segments) {
                                val frac = s.toFloat() / segments
                                val segColor = when {
                                    frac > 0.85f -> Color(0xFFFF1744)
                                    frac > 0.68f -> secondaryColor
                                    else -> primaryColor
                                }
                                val activeL = frac <= normL
                                val activeR = frac <= normR

                                drawRoundRect(
                                    color = if (activeL) segColor else segColor.copy(alpha = 0.14f),
                                    topLeft = Offset(s * segSpace, h * 0.12f),
                                    size = Size(segW, barH),
                                    cornerRadius = CornerRadius(4f, 4f)
                                )
                                drawRoundRect(
                                    color = if (activeR) segColor else segColor.copy(alpha = 0.14f),
                                    topLeft = Offset(s * segSpace, h * 0.56f),
                                    size = Size(segW, barH),
                                    cornerRadius = CornerRadius(4f, 4f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatDuration(totalSeconds: Int): String {
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
