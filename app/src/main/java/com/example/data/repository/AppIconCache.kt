package com.example.data.repository

import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.concurrent.ConcurrentHashMap

object AppIconCache {
    private val iconCache = ConcurrentHashMap<String, ImageBitmap>()
    private val bannerCache = ConcurrentHashMap<String, ImageBitmap>()
    private val noBannerKeys = ConcurrentHashMap.newKeySet<String>()
    private val noIconKeys = ConcurrentHashMap.newKeySet<String>()

    fun getIcon(packageName: String, activityName: String, pm: PackageManager): ImageBitmap? {
        val key = "$packageName/$activityName"
        iconCache[key]?.let { return it }
        if (noIconKeys.contains(key)) return null

        val drawable = try {
            val component = ComponentName(packageName, activityName)
            pm.getActivityIcon(component) ?: pm.getApplicationIcon(packageName)
        } catch (_: Exception) {
            try {
                pm.getApplicationIcon(packageName)
            } catch (_: Exception) {
                null
            }
        }

        if (drawable == null) {
            noIconKeys.add(key)
            return null
        }

        val bitmap = drawableToBitmap(drawable, 96, 96)
        return if (bitmap != null) {
            val imageBitmap = bitmap.asImageBitmap()
            iconCache[key] = imageBitmap
            imageBitmap
        } else {
            noIconKeys.add(key)
            null
        }
    }

    fun getBanner(packageName: String, activityName: String, pm: PackageManager): ImageBitmap? {
        val key = "$packageName/$activityName"
        bannerCache[key]?.let { return it }
        if (noBannerKeys.contains(key)) return null

        val drawable = try {
            val component = ComponentName(packageName, activityName)
            pm.getActivityBanner(component) ?: pm.getApplicationBanner(packageName)
        } catch (_: Exception) {
            null
        }

        if (drawable == null) {
            noBannerKeys.add(key)
            return null
        }

        val bitmap = drawableToBitmap(drawable, 320, 180)
        return if (bitmap != null) {
            val imageBitmap = bitmap.asImageBitmap()
            bannerCache[key] = imageBitmap
            imageBitmap
        } else {
            noBannerKeys.add(key)
            null
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

    fun clear() {
        iconCache.clear()
        bannerCache.clear()
        noBannerKeys.clear()
        noIconKeys.clear()
    }
}

