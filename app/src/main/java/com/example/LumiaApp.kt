package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.db.LumiaDatabase
import com.example.data.datastore.SettingsRepository
import com.example.data.monitor.NetworkMonitor
import com.example.data.monitor.TimeMonitor
import com.example.data.repository.AppRepository
import com.example.data.wallpaper.WallpaperManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class LumiaApp : Application(), ImageLoaderFactory {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var database: LumiaDatabase
        private set
    lateinit var appRepository: AppRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var networkMonitor: NetworkMonitor
        private set
    lateinit var timeMonitor: TimeMonitor
        private set
    lateinit var wallpaperManager: WallpaperManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = LumiaDatabase.getInstance(this)
        settingsRepository = SettingsRepository(this)
        appRepository = AppRepository(this, database, applicationScope)
        networkMonitor = NetworkMonitor(this)
        timeMonitor = TimeMonitor(this)
        wallpaperManager = WallpaperManager(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    // Keep cache bounded to 15% of max memory per Section 11 of the plan
                    .maxSizePercent(0.15)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(30L * 1024 * 1024) // 30 MB
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
