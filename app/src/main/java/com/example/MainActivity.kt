package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.TrackItem
import com.example.ui.motion.CyberneticScreenTransition
import com.example.ui.navigation.CyberGlassNavigationDrawer
import com.example.ui.screens.AiMoodOrganizerScreen
import com.example.ui.screens.EqAndAmpScreen
import com.example.ui.screens.LibraryAndStreamScreen
import com.example.ui.screens.PlayerDeckScreen
import com.example.ui.screens.SkinCustomizerScreen
import com.example.ui.theme.CyberAmpThemeProvider
import com.example.ui.theme.CyberAmpThemeTokens
import com.example.util.configureAppCheck
import com.example.viewmodel.CyberAmpViewModel
import com.example.viewmodel.MainTab
import com.example.viewmodel.ParametricEqualizerViewModel
import kotlin.math.max
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before UI or ViewModels
        configureAppCheck(this, intent)

        // STEP 2: Enable edge-to-edge rendering
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val viewModel: CyberAmpViewModel = viewModel(
                factory = CyberAmpViewModel.provideFactory(context)
            )
            val eqViewModel: ParametricEqualizerViewModel = viewModel(
                factory = ParametricEqualizerViewModel.provideFactory(context)
            )
            val eqUiState by eqViewModel.uiState.collectAsStateWithLifecycle()
            androidx.compose.runtime.LaunchedEffect(
                eqUiState.effectiveBandsForDsp,
                eqUiState.amplifierState,
                eqUiState.activePresetName
            ) {
                viewModel.syncFromEqualizerViewModel(
                    bands = eqUiState.effectiveBandsForDsp,
                    amp = eqUiState.amplifierState,
                    presetName = eqUiState.activePresetName
                )
            }
            val themePrefs by viewModel.themePreferences.collectAsStateWithLifecycle()
            val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()

            CyberAmpThemeProvider(
                themePrefs = themePrefs,
                currentTrack = currentTrack
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CyberAmpPrimaryLayout(
                        viewModel = viewModel,
                        eqViewModel = eqViewModel
                    )
                }
            }
        }
    }
}

private data class NavDestination(
    val tab: MainTab,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val NAV_DESTINATIONS = listOf(
    NavDestination(
        tab = MainTab.DECK,
        labelResId = R.string.nav_deck,
        selectedIcon = Icons.Filled.Album,
        unselectedIcon = Icons.Outlined.Album,
        testTag = "nav_tab_deck"
    ),
    NavDestination(
        tab = MainTab.EQ_AMP,
        labelResId = R.string.nav_eq_amp,
        selectedIcon = Icons.Filled.Equalizer,
        unselectedIcon = Icons.Outlined.Equalizer,
        testTag = "nav_tab_eq_amp"
    ),
    NavDestination(
        tab = MainTab.LIBRARY,
        labelResId = R.string.nav_library,
        selectedIcon = Icons.Filled.LibraryMusic,
        unselectedIcon = Icons.Outlined.LibraryMusic,
        testTag = "nav_tab_library"
    ),
    NavDestination(
        tab = MainTab.AI_MOOD,
        labelResId = R.string.nav_ai_mood,
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
        testTag = "nav_tab_ai_mood"
    ),
    NavDestination(
        tab = MainTab.SKINS,
        labelResId = R.string.nav_skins,
        selectedIcon = Icons.Filled.Palette,
        unselectedIcon = Icons.Outlined.Palette,
        testTag = "nav_tab_skins"
    )
)

@Composable
fun CyberAmpPrimaryLayout(
    viewModel: CyberAmpViewModel,
    eqViewModel: ParametricEqualizerViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPositionSec by viewModel.currentPositionSec.collectAsStateWithLifecycle()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsStateWithLifecycle()
    val repeatEnabled by viewModel.repeatEnabled.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteTrackIds.collectAsStateWithLifecycle()
    val eqUiState by eqViewModel.uiState.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedSourceFilter by viewModel.selectedSourceFilter.collectAsStateWithLifecycle()
    val aiMoodUiState by viewModel.aiMoodUiState.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val themePrefs by viewModel.themePreferences.collectAsStateWithLifecycle()

    val cyberAccents = CyberAmpThemeTokens.accents
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Close modal drawer first on Back press, or return to primary Deck from secondary tabs
    BackHandler(enabled = drawerState.isOpen || selectedTab != MainTab.DECK) {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else {
            viewModel.selectTab(MainTab.DECK)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            cyberAccents.neonPrimary.copy(alpha = 0.14f * cyberAccents.glowIntensity),
                            cyberAccents.neonSecondary.copy(alpha = 0.07f * cyberAccents.glowIntensity),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.22f),
                        radius = max(size.width, size.height) * 0.75f
                    )
                )
            }
            .testTag("cyberamp_root_layout")
    ) {
        val isPermanentDrawer = maxWidth >= 840.dp

        CyberGlassNavigationDrawer(
            isPermanentDrawer = isPermanentDrawer,
            drawerState = drawerState,
            isDrawerOpen = drawerState.isOpen,
            selectedTab = selectedTab,
            currentTrack = currentTrack,
            activePresetName = eqUiState.activePresetName,
            surroundEnabled = eqUiState.amplifierState.surroundEnabled,
            dynamicSkinEnabled = themePrefs.dynamicAlbumSkin,
            isDarkTheme = themePrefs.isDarkTheme,
            onSelectTab = viewModel::selectTab,
            onCloseDrawer = {
                coroutineScope.launch { drawerState.close() }
            },
            onToggleSurround = {
                eqViewModel.updateAmplifier { it.copy(surroundEnabled = !it.surroundEnabled) }
            },
            onToggleDynamicSkin = {
                viewModel.setDynamicAlbumSkin(!themePrefs.dynamicAlbumSkin)
            },
            onToggleDarkTheme = {
                viewModel.setDarkTheme(!themePrefs.isDarkTheme)
            }
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    CyberAmpTopHudBar(
                        currentTrack = currentTrack,
                        activePresetName = eqUiState.activePresetName,
                        isDarkTheme = themePrefs.isDarkTheme,
                        isDynamicSkin = themePrefs.dynamicAlbumSkin,
                        showMenuButton = !isPermanentDrawer,
                        onOpenDrawer = {
                            coroutineScope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        },
                        onToggleDarkTheme = { viewModel.setDarkTheme(!themePrefs.isDarkTheme) },
                        onNavigateToSkins = { viewModel.selectTab(MainTab.SKINS) }
                    )
                },
                bottomBar = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        AnimatedVisibility(
                            visible = selectedTab != MainTab.DECK,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            CyberAmpMiniPlayerDock(
                                track = currentTrack,
                                isPlaying = isPlaying,
                                currentPositionSec = currentPositionSec,
                                onTogglePlayPause = viewModel::togglePlayPause,
                                onSkipNext = viewModel::skipNext,
                                onOpenDeck = { viewModel.selectTab(MainTab.DECK) }
                            )
                        }

                        if (!isPermanentDrawer) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                modifier = Modifier.testTag("cyberamp_bottom_nav")
                            ) {
                                NAV_DESTINATIONS.forEach { dest ->
                                    val selected = selectedTab == dest.tab
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { viewModel.selectTab(dest.tab) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = stringResource(dest.labelResId)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = stringResource(dest.labelResId),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        modifier = Modifier.testTag(dest.testTag)
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                CyberneticScreenTransition(
                    targetTab = selectedTab,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) { tab ->
                    when (tab) {
                        MainTab.DECK -> PlayerDeckScreen(
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            currentPositionSec = currentPositionSec,
                            shuffleEnabled = shuffleEnabled,
                            repeatEnabled = repeatEnabled,
                            isFavorite = favoriteIds.contains(currentTrack.id),
                            activePresetName = eqUiState.activePresetName,
                            amplifierState = eqUiState.amplifierState,
                            telemetry = telemetry,
                            themePrefs = themePrefs,
                            onTogglePlayPause = viewModel::togglePlayPause,
                            onSkipNext = viewModel::skipNext,
                            onSkipPrevious = viewModel::skipPrevious,
                            onSeekTo = viewModel::seekTo,
                            onToggleShuffle = viewModel::toggleShuffle,
                            onToggleRepeat = viewModel::toggleRepeat,
                            onToggleFavorite = { viewModel.toggleFavorite(currentTrack.id) },
                            onMasterVolumeChange = { vol ->
                                eqViewModel.updateAmplifier { it.copy(masterVolume = vol) }
                            },
                            onToggleSurround = {
                                eqViewModel.updateAmplifier { it.copy(surroundEnabled = !it.surroundEnabled) }
                            },
                            onToggleSpatial360 = {
                                eqViewModel.setSpatial360Enabled(!eqUiState.amplifierState.spatial360Enabled)
                            },
                            onCycleVisualizerMode = viewModel::setVisualizerMode,
                            onNavigateTab = viewModel::selectTab
                        )

                        MainTab.EQ_AMP -> EqAndAmpScreen(
                            eqState = eqUiState,
                            onSelectBand = eqViewModel::selectBand,
                            onBandGainChange = eqViewModel::setBandGain,
                            onBandQChange = eqViewModel::setBandQFactor,
                            onCycleBandFilterType = eqViewModel::cycleBandFilterType,
                            onToggleEqEnabled = eqViewModel::setEqualizerEnabled,
                            onToggleAutoGain = eqViewModel::setAutoGainCompensation,
                            onResetFlat = eqViewModel::resetToFlat,
                            onInvertCurve = eqViewModel::invertEqCurve,
                            onApplyPreset = eqViewModel::applyPreset,
                            onSaveCustomPreset = eqViewModel::saveCustomPreset,
                            onDeleteCustomPreset = eqViewModel::deleteCustomPreset,
                            onUpdateAmplifier = eqViewModel::updateAmplifier
                        )

                        MainTab.LIBRARY -> LibraryAndStreamScreen(
                            allTracks = allTracks,
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            favoriteTrackIds = favoriteIds,
                            playlists = playlists,
                            searchQuery = searchQuery,
                            selectedSourceFilter = selectedSourceFilter,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onSourceFilterChange = viewModel::updateSourceFilter,
                            onSelectTrack = { track ->
                                viewModel.selectAndPlayTrack(track)
                                viewModel.selectTab(MainTab.DECK)
                            },
                            onToggleFavorite = viewModel::toggleFavorite,
                            onImportLocalUri = viewModel::importLocalAudioTrack,
                            onLoadPlaylist = viewModel::loadPlaylist,
                            onDeletePlaylist = viewModel::deletePlaylist
                        )

                        MainTab.AI_MOOD -> AiMoodOrganizerScreen(
                            uiState = aiMoodUiState,
                            onGenerateMoodMix = viewModel::generateMoodPlaylistAndEq,
                            onApplyAiResult = { res ->
                                eqViewModel.setAllBandGains(
                                    gainsDb = res.recommendedGainsDb,
                                    presetLabel = "IA: ${res.playlistTitle}"
                                )
                                eqViewModel.updateAmplifier { amp ->
                                    amp.copy(
                                        preampGainDb = res.recommendedPreampDb,
                                        tubeWarmth = res.recommendedTubeWarmth,
                                        subBassBoost = res.recommendedSubBass,
                                        stereoWidth = res.recommendedStereoWidth,
                                        surroundEnabled = true
                                    )
                                }
                                viewModel.applyAiMoodResult(res)
                                viewModel.selectTab(MainTab.DECK)
                            },
                            onSelectTrack = { track ->
                                viewModel.selectAndPlayTrack(track)
                                viewModel.selectTab(MainTab.DECK)
                            }
                        )

                        MainTab.SKINS -> SkinCustomizerScreen(
                            themePrefs = themePrefs,
                            currentTrack = currentTrack,
                            amplifierState = eqUiState.amplifierState,
                            telemetry = telemetry,
                            onSelectSkinStyle = viewModel::setSkinStyle,
                            onToggleDarkTheme = viewModel::setDarkTheme,
                            onToggleDynamicAlbumSkin = viewModel::setDynamicAlbumSkin,
                            onSelectVisualizerMode = viewModel::setVisualizerMode,
                            onToggleHighContrastHud = viewModel::setHighContrastHud,
                            onGlowStrengthChange = viewModel::setGlowStrength,
                            onUpdateAmplifier = eqViewModel::updateAmplifier
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CyberAmpTopHudBar(
    currentTrack: TrackItem,
    activePresetName: String,
    isDarkTheme: Boolean,
    isDynamicSkin: Boolean,
    showMenuButton: Boolean,
    onOpenDrawer: () -> Unit,
    onToggleDarkTheme: () -> Unit,
    onNavigateToSkins: () -> Unit
) {
    val accents = CyberAmpThemeTokens.accents
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cyberamp_top_hud_bar"),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(0.5.dp, accents.neonPrimary.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (showMenuButton) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("top_hud_drawer_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Abrir panel de navegación",
                            tint = accents.neonPrimary
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accents.neonPrimary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GraphicEq,
                            contentDescription = null,
                            tint = accents.neonPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "CYBERAMP",
                            style = MaterialTheme.typography.titleMedium,
                            color = accents.neonPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accents.neonSecondary.copy(alpha = 0.18f),
                            border = BorderStroke(0.8.dp, accents.neonSecondary.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = currentTrack.format.badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = accents.neonSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "EQ: $activePresetName • ${if (isDynamicSkin) "Skin Dinámica" else "Skin Fija"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(accents.neonPrimary)
                        .clickable(onClick = onNavigateToSkins)
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onToggleDarkTheme,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("top_hud_theme_toggle")
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                        contentDescription = "Alternar tema oscuro/claro",
                        tint = accents.neonPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun CyberAmpMiniPlayerDock(
    track: TrackItem,
    isPlaying: Boolean,
    currentPositionSec: Float,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onOpenDeck: () -> Unit
) {
    val accents = CyberAmpThemeTokens.accents
    val progress = (currentPositionSec / max(1f, track.durationSec.toFloat())).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDeck)
            .testTag("cyberamp_mini_player_dock"),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        border = BorderStroke(1.dp, accents.neonPrimary.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = { progress },
                color = accents.neonPrimary,
                trackColor = accents.neonPrimary.copy(alpha = 0.15f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = track.coverResId),
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${track.artist} • ${track.format.sampleRateLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = accents.neonPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledIconButton(
                    onClick = onTogglePlayPause,
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = accents.neonPrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("mini_player_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir"
                    )
                }

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier.testTag("mini_player_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Siguiente",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun CyberAmpApp(
    viewModel: CyberAmpViewModel,
    eqViewModel: ParametricEqualizerViewModel
) {
    CyberAmpPrimaryLayout(
        viewModel = viewModel,
        eqViewModel = eqViewModel
    )
}
