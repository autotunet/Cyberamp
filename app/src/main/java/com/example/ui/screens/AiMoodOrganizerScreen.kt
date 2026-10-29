package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.TrackItem
import com.example.viewmodel.AiMoodResult
import com.example.viewmodel.AiMoodUiState
import java.util.Locale

private val QUICK_MOOD_PROMPTS = listOf(
    "Noche lluviosa de programación cyberpunk y enfoque profundo",
    "Entrenamiento de máxima adrenalina con sub-bajos potentes",
    "Relajación nocturna en Tokio con calidez de válvulas EL34",
    "Viaje espacial meditativo con sonido envolvente 3D holográfico",
    "Conducción nocturna a alta velocidad por autopista de neón"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiMoodOrganizerScreen(
    uiState: AiMoodUiState,
    onGenerateMoodMix: (String) -> Unit,
    onApplyAiResult: (AiMoodResult) -> Unit,
    onSelectTrack: (TrackItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val scrollState = rememberScrollState()

    var moodPrompt by rememberSaveable {
        mutableStateOf("Noche lluviosa de programación cyberpunk y enfoque profundo")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_mood_header_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, primary.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Psychology,
                            contentDescription = null,
                            tint = primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "IA ORGANIZADOR ACÚSTICO & MOOD",
                                style = MaterialTheme.typography.titleLarge,
                                color = primary
                            )
                            Text(
                                text = "Impulsado por Gemini en tiempo real • Organiza tus listas y calibra el EQ paramétrico de 10 bandas según tu estado de ánimo.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = moodPrompt,
                        onValueChange = { moodPrompt = it },
                        label = { Text("Describe tu estado de ánimo, ambiente o actividad actual") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_mood_prompt_input")
                    )

                    Text(
                        text = "Sugerencias rápidas en tiempo real:",
                        style = MaterialTheme.typography.labelSmall,
                        color = secondary
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QUICK_MOOD_PROMPTS.forEachIndexed { idx, suggestion ->
                            SuggestionChip(
                                onClick = {
                                    moodPrompt = suggestion
                                    onGenerateMoodMix(suggestion)
                                },
                                label = {
                                    Text(
                                        text = suggestion,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                modifier = Modifier.testTag("quick_mood_suggestion_$idx")
                            )
                        }
                    }

                    Button(
                        onClick = { onGenerateMoodMix(moodPrompt) },
                        enabled = uiState !is AiMoodUiState.Loading && moodPrompt.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("generate_ai_playlist_button")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState is AiMoodUiState.Loading) {
                                "Analizando Espectro y Estado de Ánimo..."
                            } else {
                                "Organizar Lista & Calibrar EQ con IA"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            when (uiState) {
                is AiMoodUiState.Idle -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        ),
                        border = BorderStroke(1.dp, primary.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = secondary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Motor de Inteligencia Acústica Listo",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Escribe cómo te sientes o selecciona un escenario arriba para que Gemini ordene las pistas Hi-Res ideales y diseñe una curva de ecualización paramétrica a medida.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is AiMoodUiState.Loading -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_mood_loading_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = primary)
                            Text(
                                text = "Sintetizando perfil psicoacústico y seleccionando pistas Lossless...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = primary
                            )
                        }
                    }
                }

                is AiMoodUiState.Error -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_mood_error_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Error de Generación IA",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = uiState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                is AiMoodUiState.Success -> {
                    val res = uiState.result
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_mood_result_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.5.dp, secondary.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = res.playlistTitle,
                                style = MaterialTheme.typography.headlineMedium,
                                color = primary
                            )
                            Text(
                                text = res.moodSummary,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, primary.copy(alpha = 0.35f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Equalizer,
                                            contentDescription = null,
                                            tint = secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "CALIBRACIÓN PARAMÉTRICA & AMPLIFICADOR RECOMENDADO",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = secondary
                                        )
                                    }
                                    Text(
                                        text = res.acousticExplanation,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Preamp: ${String.format(Locale.US, "%+.1fdB", res.recommendedPreampDb)} • Válvulas EL34: ${(res.recommendedTubeWarmth * 100).toInt()}% • Sub-Bass: ${(res.recommendedSubBass * 100).toInt()}% • Estéreo 3D: ${(res.recommendedStereoWidth * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = primary
                                    )
                                }
                            }

                            Button(
                                onClick = { onApplyAiResult(res) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("apply_ai_result_button")
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Aplicar EQ Paramétrico + Reproducir Lista",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            Text(
                                text = "PISTAS SELECCIONADAS POR IA (${res.selectedTracks.size}):",
                                style = MaterialTheme.typography.labelLarge,
                                color = primary
                            )

                            res.selectedTracks.forEachIndexed { idx, track ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectTrack(track) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "#${idx + 1}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = secondary
                                        )
                                        Image(
                                            painter = painterResource(id = track.coverResId),
                                            contentDescription = track.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = track.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${track.artist} • ${track.format.badge}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
