package com.example

import com.example.data.EqPresetEntity
import com.example.viewmodel.ParametricEqualizerUiState
import com.example.viewmodel.ParametricEqualizerViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun parametricEqualizer_clampsBandGainToMinus12AndPlus12Db() {
        val viewModel = ParametricEqualizerViewModel()

        // Attempt to exceed +12.0dB and -12.0dB
        viewModel.setBandGain(bandIndex = 0, gainDb = 19.5f)
        viewModel.setBandGain(bandIndex = 9, gainDb = -24.0f)
        viewModel.setBandGain(bandIndex = 4, gainDb = 6.25f)

        val state = viewModel.uiState.value
        assertEquals(10, state.bands.size)
        assertEquals(ParametricEqualizerUiState.MAX_GAIN_DB, state.bands[0].gainDb, 0.001f)
        assertEquals(ParametricEqualizerUiState.MIN_GAIN_DB, state.bands[9].gainDb, 0.001f)
        assertEquals(6.25f, state.bands[4].gainDb, 0.001f)
        assertTrue(state.isDirtyFromPreset)
    }

    @Test
    fun parametricEqualizer_presetManagementAppliesSavesAndDeletes() {
        val viewModel = ParametricEqualizerViewModel()

        val testPreset = EqPresetEntity(
            id = 42,
            name = "Audiophile Reference Test",
            gainsCsv = "-15.0,-6.0,-3.0,0.0,1.5,3.0,4.5,6.0,9.0,18.0",
            qFactorsCsv = "1.0,1.2,1.4,1.4,1.5,1.5,1.6,1.8,2.0,2.2",
            preampDb = 2.5f,
            tubeWarmth = 0.6f,
            subBassBoost = 0.4f,
            stereoWidth = 1.4f,
            isBuiltIn = false
        )

        viewModel.applyPreset(testPreset)
        val afterApply = viewModel.uiState.value
        assertEquals("Audiophile Reference Test", afterApply.activePresetName)
        assertFalse(afterApply.isDirtyFromPreset)
        // Verify clamping on preset load (-15.0 -> -12.0, 18.0 -> +12.0)
        assertEquals(-12.0f, afterApply.bands[0].gainDb, 0.001f)
        assertEquals(12.0f, afterApply.bands[9].gainDb, 0.001f)

        // Save custom preset
        viewModel.setBandGain(2, 4.0f)
        viewModel.saveCustomPreset("Mi Curva Cyber")
        val afterSave = viewModel.uiState.value
        assertEquals("Mi Curva Cyber", afterSave.activePresetName)
        assertFalse(afterSave.isDirtyFromPreset)
        assertTrue(afterSave.presets.any { it.name == "Mi Curva Cyber" })

        // Reset flat
        viewModel.resetToFlat()
        assertTrue(viewModel.uiState.value.bands.all { it.gainDb == 0.0f })
    }

    @Test
    fun albumPaletteExtractor_extractsDominantAndSecondaryCyberAccents() {
        // Create synthetic pixel buffer with dominant electric cyan and secondary neon magenta
        val cyanArgb = 0xFF00F0FF.toInt()
        val magentaArgb = 0xFFFF007F.toInt()
        val darkBgArgb = 0xFF080A10.toInt()

        val pixels = IntArray(100) { idx ->
            when {
                idx < 55 -> cyanArgb
                idx < 85 -> magentaArgb
                else -> darkBgArgb
            }
        }

        val palette = com.example.ui.theme.AlbumPaletteExtractor.extractCyberPaletteFromPixels(pixels)
        assertTrue("Primary should have strong blue/green cyan components", palette.dominantPrimary.blue > 0.7f && palette.dominantPrimary.green > 0.7f)
        assertTrue("Secondary should have strong red magenta component", palette.vibrantSecondary.red > 0.7f)
    }

    @Test
    fun cyberneticAnimations_buildsKeyframeSpecsAndTransitions() {
        val glitchKeyframes = com.example.ui.motion.CyberneticAnimations.cyberGlitchAlphaKeyframes(300)
        val warpKeyframes = com.example.ui.motion.CyberneticAnimations.cyberWarpScaleKeyframes(320)
        val scanKeyframes = com.example.ui.motion.CyberneticAnimations.scanlineSweepKeyframes(340)

        assertEquals(300, glitchKeyframes.config.durationMillis)
        assertEquals(320, warpKeyframes.config.durationMillis)
        assertEquals(340, scanKeyframes.config.durationMillis)
    }

    @Test
    fun cyberGlassDrawerShapes_generateValidGenericOutlines() {
        val density = androidx.compose.ui.unit.Density(2f, 1f)
        val drawerShape = com.example.ui.navigation.CyberChamferedDrawerShape()
        val itemShape = com.example.ui.navigation.CyberAngledItemShape()

        val drawerOutline = drawerShape.createOutline(
            size = androidx.compose.ui.geometry.Size(600f, 1600f),
            layoutDirection = androidx.compose.ui.unit.LayoutDirection.Ltr,
            density = density
        )
        val itemOutline = itemShape.createOutline(
            size = androidx.compose.ui.geometry.Size(500f, 110f),
            layoutDirection = androidx.compose.ui.unit.LayoutDirection.Ltr,
            density = density
        )

        assertTrue(drawerOutline is androidx.compose.ui.graphics.Outline.Generic)
        assertTrue(itemOutline is androidx.compose.ui.graphics.Outline.Generic)
    }

    @Test
    fun spatialAudio360_togglesAndClampsAzimuthAngle() {
        val viewModel = ParametricEqualizerViewModel()

        viewModel.setSpatial360Enabled(false)
        assertFalse(viewModel.uiState.value.amplifierState.spatial360Enabled)

        viewModel.setSpatial360Enabled(true)
        assertTrue(viewModel.uiState.value.amplifierState.spatial360Enabled)
        assertTrue(viewModel.uiState.value.amplifierState.surroundEnabled)

        viewModel.setSpatialAzimuth(420f)
        assertEquals(360f, viewModel.uiState.value.amplifierState.spatialAzimuthDeg, 0.001f)
        assertFalse(viewModel.uiState.value.amplifierState.spatialAutoOrbit)

        viewModel.setSpatialAzimuth(-35f)
        assertEquals(0f, viewModel.uiState.value.amplifierState.spatialAzimuthDeg, 0.001f)
    }
}
