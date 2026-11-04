package com.example.data.repository

import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.LruCache

object AppIconCache {
    private val iconCache = LruCache<String, Drawable>(200)
    private val bannerCache = LruCache<String, Drawable?>(200)

    fun getIcon(packageName: String, activityName: String, pm: PackageManager): Drawable? {
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
        }

        if (drawable != null) {
            iconCache.put(key, drawable)
        }
        return drawable
    }

    fun getBanner(packageName: String, activityName: String, pm: PackageManager): Drawable? {
        val key = "$packageName/$activityName"
        // Check if cached (can be null for apps with no banner)
        val cached = bannerCache.get(key)
        if (cached != null) return cached

        val drawable = try {
            val component = ComponentName(packageName, activityName)
            pm.getActivityBanner(component) ?: pm.getApplicationBanner(packageName)
        } catch (_: Exception) {
            null
        }

        if (drawable != null) {
            bannerCache.put(key, drawable)
        }
        return drawable
    }

    fun clear() {
        iconCache.evictAll()
        bannerCache.evictAll()
    }
}
