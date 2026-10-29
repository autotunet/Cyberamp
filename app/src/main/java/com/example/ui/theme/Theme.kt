package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.data.UserThemePreferences
import com.example.model.TrackItem

/**
 * Cybernetic UI accent tokens updated dynamically from either the active album's dominant
 * palette or the selected cybernetic chassis skin.
 */
@Immutable
data class CyberneticAccents(
    val neonPrimary: Color = Color(0xFF00FFA3),
    val neonSecondary: Color = Color(0xFFFFB800),
    val laserTertiary: Color = Color(0xFF00E5FF),
    val lcdReadoutColor: Color = Color(0xFF00FFA3),
    val peakClipColor: Color = Color(0xFFFF1744),
    val glowIntensity: Float = 0.85f,
    val isDynamicAlbumActive: Boolean = true,
    val extractedPalette: ExtractedAlbumPalette = AlbumPaletteExtractor.buildFallbackPalette(
        primaryHex = 0xFF00FFA3,
        secondaryHex = 0xFFFFB800
    )
) {
    val chassisBorderBrush: Brush
        get() = Brush.linearGradient(
            colors = listOf(
                neonPrimary.copy(alpha = glowIntensity),
                neonSecondary.copy(alpha = (glowIntensity * 0.85f).coerceIn(0.1f, 1f)),
                laserTertiary.copy(alpha = (glowIntensity * 0.7f).coerceIn(0.1f, 1f))
            )
        )

    val ambientHaloBrush: Brush
        get() = Brush.sweepGradient(
            colors = listOf(neonPrimary, neonSecondary, laserTertiary, neonPrimary)
        )

    val spectrumBarBrush: Brush
        get() = Brush.verticalGradient(
            colors = listOf(neonSecondary, neonPrimary, laserTertiary.copy(alpha = 0.75f))
        )
}

val LocalCyberneticAccents = staticCompositionLocalOf { CyberneticAccents() }

object CyberAmpThemeTokens {
    val accents: CyberneticAccents
        @Composable
        @ReadOnlyComposable
        get() = LocalCyberneticAccents.current
}

/**
 * Dynamic Compose ThemeProvider that extracts dominant colors from the currently playing
 * album art bitmap and animates Material 3 color tokens and cybernetic UI accents in real time.
 */
@Composable
fun CyberAmpThemeProvider(
    themePrefs: UserThemePreferences = UserThemePreferences(),
    currentTrack: TrackItem? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val skin = themePrefs.skinStyle
    val useDynamicAlbum = themePrefs.dynamicAlbumSkin && currentTrack != null

    val extractedPalette by produceState(
        initialValue = AlbumPaletteExtractor.buildFallbackPalette(
            primaryHex = currentTrack?.dominantPrimaryHex ?: skin.primaryHex,
            secondaryHex = currentTrack?.dominantSecondaryHex ?: skin.secondaryHex
        ),
        key1 = currentTrack?.id,
        key2 = currentTrack?.coverResId
    ) {
        if (currentTrack != null) {
            value = AlbumPaletteExtractor.extractFromResource(
                context = context,
                drawableResId = currentTrack.coverResId,
                fallbackPrimaryHex = currentTrack.dominantPrimaryHex,
                fallbackSecondaryHex = currentTrack.dominantSecondaryHex
            )
        }
    }

    val targetPrimary = if (useDynamicAlbum) {
        extractedPalette.dominantPrimary
    } else {
        Color(skin.primaryHex)
    }

    val targetSecondary = if (useDynamicAlbum) {
        extractedPalette.vibrantSecondary
    } else {
        Color(skin.secondaryHex)
    }

    val targetTertiary = if (useDynamicAlbum) {
        extractedPalette.ambientTertiary
    } else {
        Color(skin.tertiaryHex)
    }

    val targetDarkSurface = if (themePrefs.highContrastHud) {
        Color(0xFF0A0E16)
    } else if (useDynamicAlbum) {
        extractedPalette.deepSurface
    } else {
        Color(skin.surfaceDarkHex)
    }

    val targetDarkBackground = if (themePrefs.highContrastHud) {
        Color(0xFF030508)
    } else if (useDynamicAlbum) {
        extractedPalette.deepBackground
    } else {
        Color(skin.backgroundDarkHex)
    }

    val animatedPrimary by animateColorAsState(
        targetValue = targetPrimary,
        animationSpec = tween(durationMillis = 600),
        label = "cyberPrimaryAnim"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = targetSecondary,
        animationSpec = tween(durationMillis = 600),
        label = "cyberSecondaryAnim"
    )
    val animatedTertiary by animateColorAsState(
        targetValue = targetTertiary,
        animationSpec = tween(durationMillis = 600),
        label = "cyberTertiaryAnim"
    )
    val animatedDarkSurface by animateColorAsState(
        targetValue = targetDarkSurface,
        animationSpec = tween(durationMillis = 600),
        label = "cyberSurfaceAnim"
    )
    val animatedDarkBackground by animateColorAsState(
        targetValue = targetDarkBackground,
        animationSpec = tween(durationMillis = 600),
        label = "cyberBgAnim"
    )

    val colorScheme = if (themePrefs.isDarkTheme) {
        darkColorScheme(
            primary = animatedPrimary,
            onPrimary = Color(0xFF05080D),
            primaryContainer = animatedPrimary.copy(alpha = 0.18f),
            onPrimaryContainer = animatedPrimary,
            secondary = animatedSecondary,
            onSecondary = Color(0xFF05080D),
            secondaryContainer = animatedSecondary.copy(alpha = 0.18f),
            onSecondaryContainer = animatedSecondary,
            tertiary = animatedTertiary,
            background = animatedDarkBackground,
            onBackground = Color(0xFFECF4FF),
            surface = animatedDarkSurface,
            onSurface = Color(0xFFECF4FF),
            surfaceVariant = animatedDarkSurface.copy(alpha = 0.85f),
            onSurfaceVariant = Color(0xFF9EB0C8),
            outline = animatedPrimary.copy(alpha = 0.45f)
        )
    } else {
        lightColorScheme(
            primary = animatedPrimary.copy(
                red = (animatedPrimary.red * 0.72f),
                green = (animatedPrimary.green * 0.72f),
                blue = (animatedPrimary.blue * 0.78f)
            ),
            onPrimary = Color.White,
            primaryContainer = animatedPrimary.copy(alpha = 0.16f),
            onPrimaryContainer = Color(0xFF0B1320),
            secondary = animatedSecondary.copy(
                red = (animatedSecondary.red * 0.78f),
                green = (animatedSecondary.green * 0.72f),
                blue = (animatedSecondary.blue * 0.78f)
            ),
            onSecondary = Color.White,
            secondaryContainer = animatedSecondary.copy(alpha = 0.16f),
            onSecondaryContainer = Color(0xFF0B1320),
            tertiary = animatedTertiary,
            background = Color(0xFFF0F4FA),
            onBackground = Color(0xFF0D1522),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0D1522),
            surfaceVariant = Color(0xFFE2EAF4),
            onSurfaceVariant = Color(0xFF3E5068),
            outline = animatedPrimary.copy(alpha = 0.55f)
        )
    }

    val cyberneticAccents = CyberneticAccents(
        neonPrimary = animatedPrimary,
        neonSecondary = animatedSecondary,
        laserTertiary = animatedTertiary,
        lcdReadoutColor = animatedPrimary,
        glowIntensity = themePrefs.glowingBordersStrength,
        isDynamicAlbumActive = useDynamicAlbum,
        extractedPalette = extractedPalette
    )

    CompositionLocalProvider(LocalCyberneticAccents provides cyberneticAccents) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun CyberAmpTheme(
    themePrefs: UserThemePreferences = UserThemePreferences(),
    currentTrack: TrackItem? = null,
    content: @Composable () -> Unit
) {
    CyberAmpThemeProvider(
        themePrefs = themePrefs,
        currentTrack = currentTrack,
        content = content
    )
}
