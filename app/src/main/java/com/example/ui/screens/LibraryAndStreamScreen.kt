package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.PlaylistEntity
import com.example.model.AudioSourceType
import com.example.model.TrackItem

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryAndStreamScreen(
    allTracks: List<TrackItem>,
    currentTrack: TrackItem,
    isPlaying: Boolean,
    favoriteTrackIds: Set<String>,
    playlists: List<PlaylistEntity>,
    searchQuery: String,
    selectedSourceFilter: AudioSourceType?,
    onSearchQueryChange: (String) -> Unit,
    onSourceFilterChange: (AudioSourceType?) -> Unit,
    onSelectTrack: (TrackItem) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onImportLocalUri: (Uri) -> Unit,
    onLoadPlaylist: (PlaylistEntity) -> Unit,
    onDeletePlaylist: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    val openAudioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportLocalUri(uri)
        }
    }

    val filteredTracks = allTracks.filter { track ->
        val matchesSource = selectedSourceFilter == null || track.sourceType == selectedSourceFilter
        val q = searchQuery.trim().lowercase()
        val matchesQuery = q.isEmpty() ||
            track.title.lowercase().contains(q) ||
            track.artist.lowercase().contains(q) ||
            track.genre.lowercase().contains(q) ||
            track.format.badge.lowercase().contains(q) ||
            track.moodTags.any { it.lowercase().contains(q) }
        matchesSource && matchesQuery
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "BIBLIOTECA HI-RES & STREAMING",
                            style = MaterialTheme.typography.titleLarge,
                            color = primary
                        )
                        Text(
                            text = "Soporte FLAC 24/192, DSD128, PCM 384kHz, MQA y Cloud Streams",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { openAudioPicker.launch(arrayOf("audio/*")) },
                        modifier = Modifier.testTag("import_local_audio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FolderOpen,
                            contentDescription = "Importar archivo local",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Importar", style = MaterialTheme.typography.labelSmall)
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Buscar pista, artista, formato (FLAC, DSD) o estado de ánimo...") },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = "Buscar", tint = primary)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library_search_input")
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedSourceFilter == null,
                        onClick = { onSourceFilterChange(null) },
                        label = { Text("Todas (${allTracks.size})", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("filter_all_chip")
                    )
                    FilterChip(
                        selected = selectedSourceFilter == AudioSourceType.LOCAL_HIRES,
                        onClick = { onSourceFilterChange(AudioSourceType.LOCAL_HIRES) },
                        leadingIcon = {
                            Icon(Icons.Filled.LibraryMusic, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Local Hi-Res", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("filter_local_chip")
                    )
                    FilterChip(
                        selected = selectedSourceFilter == AudioSourceType.STREAMING_CLOUD,
                        onClick = { onSourceFilterChange(AudioSourceType.STREAMING_CLOUD) },
                        leadingIcon = {
                            Icon(Icons.Filled.CloudQueue, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Streaming Cloud", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("filter_streaming_chip")
                    )
                }

                // Saved & AI Playlists Horizontal Carousel
                if (playlists.isNotEmpty()) {
                    Text(
                        text = "LISTAS DE REPRODUCCIÓN INTELIGENTES",
                        style = MaterialTheme.typography.labelLarge,
                        color = secondary
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(playlists, key = { it.id }) { playlist ->
                            Card(
                                modifier = Modifier
                                    .width(245.dp)
                                    .clickable { onLoadPlaylist(playlist) }
                                    .testTag("playlist_card_${playlist.id}"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, secondary.copy(alpha = 0.45f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.QueueMusic,
                                                contentDescription = null,
                                                tint = secondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = playlist.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeletePlaylist(playlist.id) },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.DeleteOutline,
                                                contentDescription = "Eliminar lista",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = playlist.moodDescription,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = primary.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = "EQ: ${playlist.recommendedPresetName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        items(filteredTracks, key = { it.id }) { track ->
            val isCurrent = track.id == currentTrack.id
            val isFav = favoriteTrackIds.contains(track.id)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .clickable { onSelectTrack(track) }
                    .testTag("track_item_card_${track.id}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) {
                        primary.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                border = BorderStroke(
                    width = if (isCurrent) 1.5.dp else 1.dp,
                    color = if (isCurrent) primary else primary.copy(alpha = 0.25f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = track.coverResId),
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isCurrent) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.GraphicEq else Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isCurrent) primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = "${track.artist} • ${track.genre}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = track.format.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = secondary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${track.bpm} BPM • ${formatDuration(track.durationSec)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = secondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { onToggleFavorite(track.id) },
                        modifier = Modifier.testTag("fav_btn_${track.id}")
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (isFav) secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
