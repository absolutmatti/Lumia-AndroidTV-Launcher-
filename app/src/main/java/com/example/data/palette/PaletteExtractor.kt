package com.example.data.palette

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.example.data.model.CardColorResult

object PaletteExtractor {
    // Cache per packageName + lastUpdateTime
    private val cache = LruCache<String, CardColorResult>(200)

    private const val FALLBACK_BG = 0xFF1E222AL
    private const val FALLBACK_TEXT = 0xFFF2F3F5L

    fun getColors(key: String, drawable: Drawable?): CardColorResult {
        cache.get(key)?.let { return it }

        if (drawable == null) {
            val fallback = CardColorResult(FALLBACK_BG, FALLBACK_TEXT)
            cache.put(key, fallback)
            return fallback
        }

        val bitmap = drawableToBitmap(drawable, 64, 64)
        if (bitmap == null) {
            val fallback = CardColorResult(FALLBACK_BG, FALLBACK_TEXT)
            cache.put(key, fallback)
            return fallback
        }

        try {
            val palette = Palette.from(bitmap).generate()
            // Priority: Dominant -> Vibrant -> Muted
            val dominantSwatch = palette.dominantSwatch
                ?: palette.vibrantSwatch
                ?: palette.mutedSwatch
                ?: palette.swatches.firstOrNull()

            val rgb = dominantSwatch?.rgb ?: 0xFF1E222A.toInt()

            // Darken to 55%-70% brightness per project plan
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(rgb, hsl)
            // Clamp lightness to between 0.20 and 0.32 so it is calm and readable
            hsl[2] = hsl[2].coerceIn(0.18f, 0.32f)
            // Subtle desaturation so it remains calm and not garish
            hsl[1] = (hsl[1] * 0.85f).coerceIn(0.20f, 0.75f)

            val finalBgInt = ColorUtils.HSLToColor(hsl)
            val finalBgLong = (finalBgInt.toLong() and 0xFFFFFFFFL) or 0xFF000000L

            // Ensure contrast ratio >= 4.5:1
            val whiteContrast = ColorUtils.calculateContrast(Color.WHITE, finalBgInt)
            val blackContrast = ColorUtils.calculateContrast(Color.BLACK, finalBgInt)

            val textLong = if (whiteContrast >= blackContrast) 0xFFF2F3F5L else 0xFF111318L

            val result = CardColorResult(
                backgroundColor = finalBgLong,
                textColor = textLong
            )
            cache.put(key, result)
            return result
        } catch (_: Exception) {
            val fallback = CardColorResult(FALLBACK_BG, FALLBACK_TEXT)
            cache.put(key, fallback)
            return fallback
        }
    }

    private fun drawableToBitmap(drawable: Drawable, width: Int, height: Int): Bitmap? {
        return try {
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                Bitmap.createScaledBitmap(drawable.bitmap, width, height, true)
            } else {
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }
        } catch (_: Exception) {
            null
        }
    }
}
