package com.example.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material3.DrawerState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.model.TrackItem
import com.example.ui.theme.CyberAmpThemeTokens
import com.example.viewmodel.MainTab

/**
 * Custom futuristic chamfered & notched drawer shape for the CyberAmp Glassmorphism Drawer.
 * Features angled top-right and bottom-right bevel cuts plus a precision HUD tech indentation
 * along the right edge.
 */
class CyberChamferedDrawerShape(
    private val topChamferDp: Dp = 28.dp,
    private val bottomChamferDp: Dp = 34.dp,
    private val notchDepthDp: Dp = 8.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val topCut = with(density) { topChamferDp.toPx() }.coerceAtMost(size.width * 0.25f)
        val botCut = with(density) { bottomChamferDp.toPx() }.coerceAtMost(size.width * 0.25f)
        val notch = with(density) { notchDepthDp.toPx() }.coerceAtMost(size.width * 0.08f)

        val path = buildPath(size.width, size.height, topCut, botCut, notch)
        return Outline.Generic(path)
    }

    companion object {
        fun buildPath(
            w: Float,
            h: Float,
            topCut: Float,
            botCut: Float,
            notch: Float
        ): Path = Path().apply {
            moveTo(0f, 0f)
            lineTo(w - topCut, 0f)
            lineTo(w, topCut)
            // Upper-mid edge down to cybernetic HUD notch
            lineTo(w, h * 0.38f)
            lineTo(w - notch, h * 0.41f)
            lineTo(w - notch, h * 0.59f)
            lineTo(w, h * 0.62f)
            // Lower edge to bottom chamfer
            lineTo(w, h - botCut)
            lineTo(w - botCut, h)
            lineTo(0f, h)
            close()
        }
    }
}

/**
 * Custom angled parallelogram/chamfered card shape for navigation items inside the drawer.
 */
class CyberAngledItemShape(
    private val cornerCutDp: Dp = 14.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cut = with(density) { cornerCutDp.toPx() }.coerceAtMost(size.height * 0.45f)
        val path = Path().apply {
            moveTo(cut, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height - cut)
            lineTo(size.width - cut, size.height)
            lineTo(0f, size.height)
            lineTo(0f, cut)
            close()
        }
        return Outline.Generic(path)
    }
}

data class DrawerNavEntry(
    val tab: MainTab,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val testTag: String
)

val CYBER_DRAWER_ENTRIES = listOf(
    DrawerNavEntry(
        tab = MainTab.DECK,
        title = "Deck Principal",
        subtitle = "Transporte Hi-Res, Espectro 64-Bandas y VU",
        icon = Icons.Filled.Album,
        testTag = "drawer_item_deck"
    ),
    DrawerNavEntry(
        tab = MainTab.EQ_AMP,
        title = "EQ Paramétrico & Amp",
        subtitle = "10 Bandas [-12dB..+12dB], Válvulas EL34 y 3D",
        icon = Icons.Filled.Equalizer,
        testTag = "drawer_item_eq_amp"
    ),
    DrawerNavEntry(
        tab = MainTab.LIBRARY,
        title = "Biblioteca & Streaming",
        subtitle = "FLAC 24/192, DSD128, Cloud y Archivos Locales",
        icon = Icons.Filled.LibraryMusic,
        testTag = "drawer_item_library"
    ),
    DrawerNavEntry(
        tab = MainTab.AI_MOOD,
        title = "IA Mood Organizer",
        subtitle = "Listas y calibración acústica en tiempo real",
        icon = Icons.Filled.AutoAwesome,
        testTag = "drawer_item_ai_mood"
    ),
    DrawerNavEntry(
        tab = MainTab.SKINS,
        title = "Skins & Chasis",
        subtitle = "Skins dinámicas por álbum y temas cibernéticos",
        icon = Icons.Filled.Palette,
        testTag = "drawer_item_skins"
    )
)

/**
 * Responsive Material 3 Navigation Drawer with futuristic glassmorphism, custom chamfered
 * geometry, specular glass overlays, and animated background blur when open on compact/medium screens.
 */
@Composable
fun CyberGlassNavigationDrawer(
    isPermanentDrawer: Boolean,
    drawerState: DrawerState,
    isDrawerOpen: Boolean,
    selectedTab: MainTab,
    currentTrack: TrackItem,
    activePresetName: String,
    surroundEnabled: Boolean,
    dynamicSkinEnabled: Boolean,
    isDarkTheme: Boolean,
    onSelectTab: (MainTab) -> Unit,
    onCloseDrawer: () -> Unit,
    onToggleSurround: () -> Unit,
    onToggleDynamicSkin: () -> Unit,
    onToggleDarkTheme: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val drawerShape = androidx.compose.runtime.remember { CyberChamferedDrawerShape() }
    val accents = CyberAmpThemeTokens.accents

    // Animated blur radius and glass tint overlay for the main viewport when Modal drawer opens
    val contentBlurRadius by animateDpAsState(
        targetValue = if (!isPermanentDrawer && isDrawerOpen) 14.dp else 0.dp,
        animationSpec = tween(durationMillis = 280),
        label = "drawerBackdropBlur"
    )
    val backdropOverlayAlpha by animateFloatAsState(
        targetValue = if (!isPermanentDrawer && isDrawerOpen) 0.42f else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "drawerBackdropAlpha"
    )

    if (isPermanentDrawer) {
        PermanentNavigationDrawer(
            modifier = modifier.testTag("cyber_permanent_drawer"),
            drawerContent = {
                PermanentDrawerSheet(
                    drawerShape = drawerShape,
                    drawerContainerColor = Color.Transparent,
                    modifier = Modifier.width(300.dp)
                ) {
                    CyberGlassDrawerSheetContent(
                        selectedTab = selectedTab,
                        currentTrack = currentTrack,
                        activePresetName = activePresetName,
                        surroundEnabled = surroundEnabled,
                        dynamicSkinEnabled = dynamicSkinEnabled,
                        isDarkTheme = isDarkTheme,
                        showCloseButton = false,
                        drawerShape = drawerShape,
                        onSelectTab = onSelectTab,
                        onCloseDrawer = onCloseDrawer,
                        onToggleSurround = onToggleSurround,
                        onToggleDynamicSkin = onToggleDynamicSkin,
                        onToggleDarkTheme = onToggleDarkTheme
                    )
                }
            },
            content = content
        )
    } else {
        ModalNavigationDrawer(
            modifier = modifier.testTag("cyber_modal_drawer"),
            drawerState = drawerState,
            scrimColor = accents.extractedPalette.deepBackground.copy(alpha = 0.62f),
            drawerContent = {
                ModalDrawerSheet(
                    drawerShape = drawerShape,
                    drawerContainerColor = Color.Transparent,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    modifier = Modifier.width(310.dp)
                ) {
                    CyberGlassDrawerSheetContent(
                        selectedTab = selectedTab,
                        currentTrack = currentTrack,
                        activePresetName = activePresetName,
                        surroundEnabled = surroundEnabled,
                        dynamicSkinEnabled = dynamicSkinEnabled,
                        isDarkTheme = isDarkTheme,
                        showCloseButton = true,
                        drawerShape = drawerShape,
                        onSelectTab = { tab ->
                            onSelectTab(tab)
                            onCloseDrawer()
                        },
                        onCloseDrawer = onCloseDrawer,
                        onToggleSurround = onToggleSurround,
                        onToggleDynamicSkin = onToggleDynamicSkin,
                        onToggleDarkTheme = onToggleDarkTheme
                    )
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (contentBlurRadius > 0.5.dp) {
                            Modifier.blur(contentBlurRadius)
                        } else {
                            Modifier
                        }
                    )
                    .drawWithContent {
                        drawContent()
                        if (backdropOverlayAlpha > 0.01f) {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        accents.neonPrimary.copy(alpha = backdropOverlayAlpha * 0.35f),
                                        accents.extractedPalette.deepBackground.copy(alpha = backdropOverlayAlpha)
                                    )
                                )
                            )
                        }
                    }
            ) {
                content()
            }
        }
    }
}

@Composable
private fun CyberGlassDrawerSheetContent(
    selectedTab: MainTab,
    currentTrack: TrackItem,
    activePresetName: String,
    surroundEnabled: Boolean,
    dynamicSkinEnabled: Boolean,
    isDarkTheme: Boolean,
    showCloseButton: Boolean,
    drawerShape: CyberChamferedDrawerShape,
    onSelectTab: (MainTab) -> Unit,
    onCloseDrawer: () -> Unit,
    onToggleSurround: () -> Unit,
    onToggleDynamicSkin: () -> Unit,
    onToggleDarkTheme: () -> Unit
) {
    val accents = CyberAmpThemeTokens.accents
    val baseSurface = MaterialTheme.colorScheme.surface
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .clip(drawerShape)
            .drawBehind {
                // 1. Translucent Frosted Base Layer
                drawRect(color = baseSurface.copy(alpha = if (isDarkTheme) 0.78f else 0.86f))

                // 2. Ambient Blurred Neon Light Orbs behind the frosted glass
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accents.neonPrimary.copy(alpha = 0.28f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.25f, size.height * 0.16f),
                        radius = size.width * 0.85f
                    ),
                    center = Offset(size.width * 0.25f, size.height * 0.16f),
                    radius = size.width * 0.85f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accents.neonSecondary.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.80f, size.height * 0.72f),
                        radius = size.width * 0.75f
                    ),
                    center = Offset(size.width * 0.80f, size.height * 0.72f),
                    radius = size.width * 0.75f
                )

                // 3. Diagonal Specular Glass Sheen Overlay
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDarkTheme) 0.14f else 0.45f),
                            Color.White.copy(alpha = 0.03f),
                            accents.neonPrimary.copy(alpha = 0.08f),
                            Color.White.copy(alpha = if (isDarkTheme) 0.09f else 0.25f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    )
                )

                // 4. Precision Neon Edge Trace along the chamfered polygon contour
                val topCut = 28.dp.toPx().coerceAtMost(size.width * 0.25f)
                val botCut = 34.dp.toPx().coerceAtMost(size.width * 0.25f)
                val notch = 8.dp.toPx().coerceAtMost(size.width * 0.08f)
                val outlinePath = CyberChamferedDrawerShape.buildPath(
                    size.width,
                    size.height,
                    topCut,
                    botCut,
                    notch
                )
                drawPath(
                    path = outlinePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accents.neonPrimary.copy(alpha = 0.85f),
                            accents.neonSecondary.copy(alpha = 0.75f),
                            accents.laserTertiary.copy(alpha = 0.85f)
                        )
                    ),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("cyber_glass_drawer_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Drawer Brand & Close Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CyberAngledItemShape(8.dp))
                            .background(accents.neonPrimary.copy(alpha = 0.22f))
                            .border(1.dp, accents.neonPrimary, CyberAngledItemShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GraphicEq,
                            contentDescription = null,
                            tint = accents.neonPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "CYBERAMP OS",
                            style = MaterialTheme.typography.titleMedium,
                            color = accents.neonPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "GLASS HUD COMMAND DECK",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (showCloseButton) {
                    IconButton(
                        onClick = onCloseDrawer,
                        modifier = Modifier.testTag("drawer_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cerrar menú",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Frosted Glass Now-Playing Telemetry Pod
            val podShape = CyberAngledItemShape(14.dp)
            Surface(
                shape = podShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            accents.neonPrimary.copy(alpha = 0.65f),
                            accents.neonSecondary.copy(alpha = 0.45f)
                        )
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = currentTrack.coverResId),
                        contentDescription = currentTrack.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CyberAngledItemShape(10.dp))
                            .border(1.dp, accents.neonPrimary, CyberAngledItemShape(10.dp))
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentTrack.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentTrack.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = accents.neonPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${currentTrack.format.badge} • $activePresetName",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Text(
                text = "MÓDULOS DEL SISTEMA",
                style = MaterialTheme.typography.labelSmall,
                color = accents.neonSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Custom Glassmorphic Angled Navigation Items
            CYBER_DRAWER_ENTRIES.forEach { entry ->
                val isSelected = selectedTab == entry.tab
                val itemShape = CyberAngledItemShape(12.dp)
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        accents.neonPrimary.copy(alpha = 0.20f)
                    } else {
                        Color.White.copy(alpha = if (isDarkTheme) 0.05f else 0.35f)
                    },
                    animationSpec = tween(220),
                    label = "drawerItemBg"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        accents.neonPrimary
                    } else {
                        accents.neonPrimary.copy(alpha = 0.22f)
                    },
                    animationSpec = tween(220),
                    label = "drawerItemBorder"
                )

                Surface(
                    shape = itemShape,
                    color = bgColor,
                    border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(itemShape)
                        .clickable { onSelectTab(entry.tab) }
                        .testTag(entry.testTag)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) accents.neonPrimary else accents.neonPrimary.copy(alpha = 0.14f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = entry.icon,
                                contentDescription = entry.title,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else accents.neonPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isSelected) accents.neonPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = entry.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Quick Glassmorphic DSP & Skin Toggles Footer
            Surface(
                shape = CyberAngledItemShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                border = BorderStroke(1.dp, accents.neonSecondary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ACCESO RÁPIDO DSP & ÓPTICA",
                        style = MaterialTheme.typography.labelSmall,
                        color = accents.neonSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = surroundEnabled,
                            onClick = onToggleSurround,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.SurroundSound,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text("3D Audio", style = MaterialTheme.typography.labelSmall)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("drawer_quick_surround_chip")
                        )

                        FilterChip(
                            selected = dynamicSkinEnabled,
                            onClick = onToggleDynamicSkin,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Palette,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text("Auto-Skin", style = MaterialTheme.typography.labelSmall)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("drawer_quick_dynamic_skin_chip")
                        )
                    }

                    FilterChip(
                        selected = isDarkTheme,
                        onClick = onToggleDarkTheme,
                        leadingIcon = {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                text = if (isDarkTheme) "Modo Oscuro OLED" else "Modo Claro Estudio",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("drawer_quick_theme_chip")
                    )
                }
            }
        }
    }
}
