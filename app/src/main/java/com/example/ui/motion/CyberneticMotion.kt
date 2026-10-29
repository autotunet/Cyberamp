package com.example.ui.motion

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.KeyframesSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.model.TrackItem
import com.example.ui.theme.CyberAmpThemeTokens
import com.example.viewmodel.MainTab

/**
 * Defines custom Jetpack Compose transitions and keyframe animations that deliver
 * a fluid, futuristic cybernetic feel when switching screens or audio tracks.
 */
object CyberneticAnimations {

    const val SCREEN_TRANSITION_DURATION_MS = 300
    const val TRACK_TRANSITION_DURATION_MS = 320
    const val SCANLINE_SWEEP_DURATION_MS = 340

    /**
     * Precision cybernetic snap easing (rapid initial acceleration with smooth hydraulic lock).
     */
    val CyberSnapEasing = CubicBezierEasing(0.16f, 1.0f, 0.30f, 1.0f)

    /**
     * Holographic materialization alpha keyframes:
     * Simulates a high-tech optical HUD lock-on with a subtle micro-pulse before full opacity.
     */
    fun cyberGlitchAlphaKeyframes(
        durationMs: Int = SCREEN_TRANSITION_DURATION_MS
    ): KeyframesSpec<Float> = keyframes {
        durationMillis = durationMs
        0.0f at 0 using FastOutLinearInEasing
        0.74f at (durationMs * 0.22f).toInt() using LinearOutSlowInEasing
        0.48f at (durationMs * 0.36f).toInt() using FastOutLinearInEasing
        0.92f at (durationMs * 0.65f).toInt() using FastOutSlowInEasing
        1.0f at durationMs
    }

    /**
     * Cybernetic warp-in scale keyframes:
     * Expands from 0.88x with a crisp 1.03x holographic overshoot before locking to 1.0x.
     */
    fun cyberWarpScaleKeyframes(
        durationMs: Int = TRACK_TRANSITION_DURATION_MS
    ): KeyframesSpec<Float> = keyframes {
        durationMillis = durationMs
        0.88f at 0 using FastOutSlowInEasing
        1.032f at (durationMs * 0.62f).toInt() using CyberSnapEasing
        0.994f at (durationMs * 0.82f).toInt() using LinearOutSlowInEasing
        1.0f at durationMs
    }

    /**
     * Laser scanline vertical sweep keyframes (0f -> 1f normalized Y position).
     */
    fun scanlineSweepKeyframes(
        durationMs: Int = SCANLINE_SWEEP_DURATION_MS
    ): KeyframesSpec<Float> = keyframes {
        durationMillis = durationMs
        0.0f at 0 using FastOutLinearInEasing
        0.58f at (durationMs * 0.45f).toInt() using CyberSnapEasing
        1.0f at durationMs using LinearOutSlowInEasing
    }

    /**
     * Directional cybernetic transition between top-level navigation tabs.
     */
    fun screenTabTransitionSpec(
        initialTab: MainTab,
        targetTab: MainTab
    ): ContentTransform {
        val direction = if (targetTab.ordinal >= initialTab.ordinal) 1 else -1

        val enter = slideInHorizontally(
            animationSpec = tween(
                durationMillis = SCREEN_TRANSITION_DURATION_MS,
                easing = CyberSnapEasing
            ),
            initialOffsetX = { fullWidth -> (fullWidth * 0.24f * direction).toInt() }
        ) + fadeIn(
            animationSpec = cyberGlitchAlphaKeyframes(SCREEN_TRANSITION_DURATION_MS)
        ) + scaleIn(
            animationSpec = cyberWarpScaleKeyframes(SCREEN_TRANSITION_DURATION_MS),
            initialScale = 0.94f
        )

        val exit = slideOutHorizontally(
            animationSpec = tween(
                durationMillis = (SCREEN_TRANSITION_DURATION_MS * 0.75f).toInt(),
                easing = FastOutLinearInEasing
            ),
            targetOffsetX = { fullWidth -> (-fullWidth * 0.18f * direction).toInt() }
        ) + fadeOut(
            animationSpec = tween(durationMillis = 190)
        ) + scaleOut(
            animationSpec = tween(durationMillis = 210),
            targetScale = 0.95f
        )

        return ContentTransform(
            targetContentEnter = enter,
            initialContentExit = exit,
            sizeTransform = SizeTransform(clip = false)
        )
    }

    /**
     * Futuristic holographic transition when switching between tracks on the Player Deck.
     */
    fun trackChangeTransitionSpec(forward: Boolean = true): ContentTransform {
        val dir = if (forward) 1 else -1

        val enter = slideInHorizontally(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            initialOffsetX = { width -> (width * 0.30f * dir).toInt() }
        ) + slideInVertically(
            animationSpec = tween(TRACK_TRANSITION_DURATION_MS, easing = CyberSnapEasing),
            initialOffsetY = { height -> (height * 0.08f).toInt() }
        ) + fadeIn(
            animationSpec = cyberGlitchAlphaKeyframes(TRACK_TRANSITION_DURATION_MS)
        ) + scaleIn(
            animationSpec = cyberWarpScaleKeyframes(TRACK_TRANSITION_DURATION_MS),
            initialScale = 0.88f
        )

        val exit = slideOutHorizontally(
            animationSpec = tween(220, easing = FastOutLinearInEasing),
            targetOffsetX = { width -> (-width * 0.25f * dir).toInt() }
        ) + slideOutVertically(
            animationSpec = tween(220),
            targetOffsetY = { height -> (-height * 0.06f).toInt() }
        ) + fadeOut(
            animationSpec = tween(180)
        ) + scaleOut(
            animationSpec = tween(200),
            targetScale = 0.90f
        )

        return ContentTransform(
            targetContentEnter = enter,
            initialContentExit = exit,
            sizeTransform = SizeTransform(clip = false)
        )
    }
}

/**
 * Wraps screen tab switching in a directional cybernetic transition with an animated
 * neon laser scanline sweep across the viewport.
 */
@Composable
fun CyberneticScreenTransition(
    targetTab: MainTab,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedContentScope.(MainTab) -> Unit
) {
    val accents = CyberAmpThemeTokens.accents
    val scanProgress = remember { Animatable(1f) }

    LaunchedEffect(targetTab) {
        scanProgress.snapTo(0f)
        scanProgress.animateTo(
            targetValue = 1f,
            animationSpec = CyberneticAnimations.scanlineSweepKeyframes()
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                val progress = scanProgress.value
                if (progress in 0.01f..0.98f) {
                    val scanY = size.height * progress
                    val beamHeight = 36f
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                accents.neonPrimary.copy(alpha = 0.22f * (1f - progress)),
                                accents.neonSecondary.copy(alpha = 0.40f * (1f - progress)),
                                Color.Transparent
                            ),
                            startY = (scanY - beamHeight).coerceAtLeast(0f),
                            endY = (scanY + beamHeight).coerceAtMost(size.height)
                        ),
                        topLeft = Offset(0f, (scanY - beamHeight).coerceAtLeast(0f)),
                        size = Size(size.width, beamHeight * 2f)
                    )
                }
            }
    ) {
        AnimatedContent(
            targetState = targetTab,
            transitionSpec = {
                CyberneticAnimations.screenTabTransitionSpec(initialState, targetState)
            },
            label = "cyberScreenTransition",
            content = content
        )
    }
}

/**
 * Wraps track/album artwork changes in a keyframe holographic materialization transition
 * with a glowing cybernetic scanline and corner HUD reticles.
 */
@Composable
fun CyberneticTrackTransition(
    targetTrack: TrackItem,
    modifier: Modifier = Modifier,
    content: @Composable (TrackItem) -> Unit
) {
    val accents = CyberAmpThemeTokens.accents
    val scanProgress = remember { Animatable(1f) }

    LaunchedEffect(targetTrack.id) {
        scanProgress.snapTo(0f)
        scanProgress.animateTo(
            targetValue = 1f,
            animationSpec = CyberneticAnimations.scanlineSweepKeyframes()
        )
    }

    AnimatedContent(
        targetState = targetTrack,
        transitionSpec = {
            CyberneticAnimations.trackChangeTransitionSpec(forward = true)
        },
        contentKey = { it.id },
        modifier = modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                val progress = scanProgress.value
                if (progress in 0.01f..0.98f) {
                    val y = size.height * progress
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                accents.neonPrimary.copy(alpha = 0.75f * (1f - progress)),
                                accents.neonSecondary.copy(alpha = 0.85f * (1f - progress)),
                                Color.Transparent
                            )
                        ),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 3f
                    )
                }
            },
        label = "cyberTrackTransition"
    ) { track ->
        content(track)
    }
}
