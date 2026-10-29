package com.example.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Holds the dominant and accent colors extracted from an album cover bitmap,
 * tuned for cybernetic UI contrast and neon glow aesthetics.
 */
data class ExtractedAlbumPalette(
    val dominantPrimary: Color,
    val vibrantSecondary: Color,
    val ambientTertiary: Color,
    val deepSurface: Color,
    val deepBackground: Color
)

object AlbumPaletteExtractor {
    private val paletteCache = ConcurrentHashMap<Int, ExtractedAlbumPalette>()

    suspend fun extractFromResource(
        context: Context,
        @DrawableRes drawableResId: Int,
        fallbackPrimaryHex: Long = 0xFF00F0FF,
        fallbackSecondaryHex: Long = 0xFFFF007F
    ): ExtractedAlbumPalette = withContext(Dispatchers.Default) {
        paletteCache[drawableResId]?.let { return@withContext it }

        val extracted = runCatching {
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inSampleSize = 8
            }
            val bitmap = BitmapFactory.decodeResource(context.resources, drawableResId, options)
            if (bitmap != null) {
                val scaled = Bitmap.createScaledBitmap(bitmap, 36, 36, true)
                val pixels = IntArray(scaled.width * scaled.height)
                scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
                if (scaled !== bitmap) scaled.recycle()
                bitmap.recycle()
                extractCyberPaletteFromPixels(pixels, fallbackPrimaryHex, fallbackSecondaryHex)
            } else {
                buildFallbackPalette(fallbackPrimaryHex, fallbackSecondaryHex)
            }
        }.getOrElse {
            buildFallbackPalette(fallbackPrimaryHex, fallbackSecondaryHex)
        }

        paletteCache[drawableResId] = extracted
        extracted
    }

    /**
     * Deterministic HSV histogram & vibrance clustering over raw ARGB pixels to extract
     * dominant primary, complementary secondary, and cybernetic ambient colors.
     */
    fun extractCyberPaletteFromPixels(
        pixels: IntArray,
        fallbackPrimaryHex: Long = 0xFF00F0FF,
        fallbackSecondaryHex: Long = 0xFFFF007F
    ): ExtractedAlbumPalette {
        if (pixels.isEmpty()) {
            return buildFallbackPalette(fallbackPrimaryHex, fallbackSecondaryHex)
        }

        // 12 hue buckets (30 degrees each) weighted by saturation & value
        val bucketWeight = FloatArray(12)
        val bucketR = FloatArray(12)
        val bucketG = FloatArray(12)
        val bucketB = FloatArray(12)

        for (argb in pixels) {
            val r = ((argb shr 16) and 0xFF) / 255f
            val g = ((argb shr 8) and 0xFF) / 255f
            val b = (argb and 0xFF) / 255f

            val maxC = max(r, max(g, b))
            val minC = min(r, min(g, b))
            val delta = maxC - minC

            val sat = if (maxC <= 0.001f) 0f else delta / maxC
            val value = maxC

            // Ignore near-black, near-white, or desaturated gray pixels when hunting for neon accents
            if (value < 0.15f || sat < 0.18f) continue

            val hueDeg = when {
                delta <= 0.0001f -> 0f
                maxC == r -> 60f * (((g - b) / delta) % 6f)
                maxC == g -> 60f * (((b - r) / delta) + 2f)
                else -> 60f * (((r - g) / delta) + 4f)
            }.let { if (it < 0f) it + 360f else it }

            val bucket = ((hueDeg / 30f).toInt()).coerceIn(0, 11)
            // Favor high saturation and bright value for cybernetic glow
            val weight = (sat * sat) * (0.4f + 0.6f * value)
            bucketWeight[bucket] += weight
            bucketR[bucket] += r * weight
            bucketG[bucket] += g * weight
            bucketB[bucket] += b * weight
        }

        var primaryBucket = -1
        var bestWeight = 0f
        for (i in 0 until 12) {
            if (bucketWeight[i] > bestWeight) {
                bestWeight = bucketWeight[i]
                primaryBucket = i
            }
        }

        if (primaryBucket == -1 || bestWeight <= 0.001f) {
            return buildFallbackPalette(fallbackPrimaryHex, fallbackSecondaryHex)
        }

        // Find secondary bucket with distinct hue distance (>= 2 buckets away if possible)
        var secondaryBucket = -1
        var secondBestWeight = 0f
        for (i in 0 until 12) {
            val hueDistance = min(abs(i - primaryBucket), 12 - abs(i - primaryBucket))
            if (hueDistance >= 2 && bucketWeight[i] > secondBestWeight) {
                secondBestWeight = bucketWeight[i]
                secondaryBucket = i
            }
        }

        val rawPrimary = Color(
            red = (bucketR[primaryBucket] / bestWeight).coerceIn(0f, 1f),
            green = (bucketG[primaryBucket] / bestWeight).coerceIn(0f, 1f),
            blue = (bucketB[primaryBucket] / bestWeight).coerceIn(0f, 1f)
        )
        val boostedPrimary = boostCyberneticVibrance(rawPrimary)

        val boostedSecondary = if (secondaryBucket != -1 && secondBestWeight > 0.001f) {
            val rawSec = Color(
                red = (bucketR[secondaryBucket] / secondBestWeight).coerceIn(0f, 1f),
                green = (bucketG[secondaryBucket] / secondBestWeight).coerceIn(0f, 1f),
                blue = (bucketB[secondaryBucket] / secondBestWeight).coerceIn(0f, 1f)
            )
            boostCyberneticVibrance(rawSec)
        } else {
            Color(fallbackSecondaryHex)
        }

        val ambientTertiary = Color(
            red = ((boostedPrimary.red + boostedSecondary.blue) * 0.5f).coerceIn(0.2f, 1f),
            green = ((boostedPrimary.green * 0.4f + boostedSecondary.green * 0.6f)).coerceIn(0.15f, 1f),
            blue = ((boostedPrimary.blue * 0.6f + boostedSecondary.red * 0.4f)).coerceIn(0.25f, 1f)
        )

        val deepSurface = Color(
            red = (boostedPrimary.red * 0.09f + 0.04f).coerceIn(0.03f, 0.16f),
            green = (boostedPrimary.green * 0.09f + 0.05f).coerceIn(0.03f, 0.16f),
            blue = (boostedPrimary.blue * 0.11f + 0.07f).coerceIn(0.04f, 0.20f)
        )

        val deepBackground = Color(
            red = (deepSurface.red * 0.52f).coerceIn(0.01f, 0.09f),
            green = (deepSurface.green * 0.52f).coerceIn(0.01f, 0.09f),
            blue = (deepSurface.blue * 0.58f).coerceIn(0.02f, 0.12f)
        )

        return ExtractedAlbumPalette(
            dominantPrimary = boostedPrimary,
            vibrantSecondary = boostedSecondary,
            ambientTertiary = ambientTertiary,
            deepSurface = deepSurface,
            deepBackground = deepBackground
        )
    }

    private fun boostCyberneticVibrance(color: Color): Color {
        val maxChannel = max(color.red, max(color.green, color.blue)).coerceAtLeast(0.01f)
        val scale = (0.95f / maxChannel).coerceIn(1.0f, 2.2f)
        return Color(
            red = (color.red * scale).coerceIn(0.08f, 1f),
            green = (color.green * scale).coerceIn(0.08f, 1f),
            blue = (color.blue * scale).coerceIn(0.08f, 1f)
        )
    }

    fun buildFallbackPalette(
        primaryHex: Long,
        secondaryHex: Long
    ): ExtractedAlbumPalette {
        val primary = Color(primaryHex)
        val secondary = Color(secondaryHex)
        val tertiary = Color(
            red = ((primary.red + secondary.red) * 0.5f).coerceIn(0.2f, 1f),
            green = ((primary.green + secondary.green) * 0.5f).coerceIn(0.2f, 1f),
            blue = ((primary.blue + secondary.blue) * 0.5f).coerceIn(0.2f, 1f)
        )
        return ExtractedAlbumPalette(
            dominantPrimary = primary,
            vibrantSecondary = secondary,
            ambientTertiary = tertiary,
            deepSurface = Color(0xFF101522),
            deepBackground = Color(0xFF070A11)
        )
    }
}
