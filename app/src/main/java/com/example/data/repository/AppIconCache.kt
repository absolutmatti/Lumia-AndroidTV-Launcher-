package com.example.data.repository

import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache

object AppIconCache {
    private val iconCache = LruCache<String, Bitmap>(200)
    private val bannerCache = LruCache<String, Bitmap?>(200)

    fun getIconBitmap(packageName: String, activityName: String, pm: PackageManager): Bitmap? {
        val key = "$packageName/$activityName"
        iconCache.get(key)?.let { return it }

        val drawable = try {
            val component = ComponentName(packageName, activityName)
            pm.getActivityIcon(component) ?: pm.getApplicationIcon(packageName)
        } catch (_: Exception) {
            try {
                pm.getApplicationIcon(packageName)
            } catch (_: Exception) {
                null
            }
        } ?: return null

        val bitmap = drawableToBitmap(drawable, 96, 96)
        if (bitmap != null) {
            iconCache.put(key, bitmap)
        }
        return bitmap
    }

    fun getBannerBitmap(packageName: String, activityName: String, pm: PackageManager): Bitmap? {
        val key = "$packageName/$activityName"
        val cached = bannerCache.get(key)
        if (cached != null) return cached

        val drawable = try {
            val component = ComponentName(packageName, activityName)
            pm.getActivityBanner(component) ?: pm.getApplicationBanner(packageName)
        } catch (_: Exception) {
            null
        }

        if (drawable == null) {
            // Can't cache null in LruCache directly, but we can store a sentinel or return null
            return null
        }

        val bitmap = drawableToBitmap(drawable, 320, 180)
        if (bitmap != null) {
            bannerCache.put(key, bitmap)
        }
        return bitmap
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
        iconCache.evictAll()
        bannerCache.evictAll()
    }
}
