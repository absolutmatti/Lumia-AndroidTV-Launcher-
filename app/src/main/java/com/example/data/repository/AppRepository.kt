package com.example.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import com.example.data.db.LumiaDatabase
import com.example.data.model.AppItem
import com.example.data.model.FavoriteApp
import com.example.data.model.HiddenApp
import com.example.data.palette.PaletteExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

class AppRepository(
    private val context: Context,
    private val database: LumiaDatabase,
    private val scope: CoroutineScope
) {
    private val packageManager: PackageManager = context.packageManager
    private val favoriteDao = database.favoriteAppDao()
    private val hiddenDao = database.hiddenAppDao()

    // Flow that triggers when packages are installed, updated, or removed
    private val packageUpdatesFlow: Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                val pkgName = intent?.data?.schemeSpecificPart
                if (intent?.action == Intent.ACTION_PACKAGE_FULLY_REMOVED && pkgName != null) {
                    // Automatically clean up deleted packages from DB
                    scope.launchCleanup(pkgName)
                }
                AppIconCache.clear()
                trySend(Unit)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
            addDataScheme("package")
        }
        context.registerReceiver(receiver, filter)
        trySend(Unit) // initial emission

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    private fun CoroutineScope.launchCleanup(packageName: String) {
        launch(Dispatchers.IO) {
            favoriteDao.deleteByPackage(packageName)
            hiddenDao.deleteByPackage(packageName)
            val remainingFavs = favoriteDao.getAllFavoritesList()
            favoriteDao.renumberFavorites(remainingFavs)
        }
    }

    // Installed raw app items
    private val rawInstalledAppsFlow: Flow<List<AppItem>> = packageUpdatesFlow.combine(
        kotlinx.coroutines.flow.flowOf(Unit)
    ) { _, _ ->
        loadInstalledApps()
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    private suspend fun loadInstalledApps(): List<AppItem> = withContext(Dispatchers.Default) {
        val myPackage = context.packageName

        // 1. Leanback apps (priority)
        val leanbackIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val leanbackResolves = packageManager.queryIntentActivities(leanbackIntent, 0)

        // 2. Regular Launcher apps
        val standardIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val standardResolves = packageManager.queryIntentActivities(standardIntent, 0)

        // Map by package name, Leanback has priority
        val packageMap = mutableMapOf<String, AppItem>()

        for (ri in leanbackResolves) {
            val pkg = ri.activityInfo.packageName
            if (pkg == myPackage) continue
            val item = createAppItem(ri.activityInfo, isLeanback = true)
            packageMap[pkg] = item
        }

        for (ri in standardResolves) {
            val pkg = ri.activityInfo.packageName
            if (pkg == myPackage) continue
            if (!packageMap.containsKey(pkg)) {
                val item = createAppItem(ri.activityInfo, isLeanback = false)
                packageMap[pkg] = item
            }
        }

        // Sort alphabetically with system Collator
        val collator = Collator.getInstance(Locale.getDefault()).apply {
            strength = Collator.PRIMARY
        }

        packageMap.values.sortedWith { a, b ->
            collator.compare(a.label, b.label)
        }
    }

    private fun createAppItem(
        activityInfo: android.content.pm.ActivityInfo,
        isLeanback: Boolean
    ): AppItem {
        val pkg = activityInfo.packageName
        val label = try {
            activityInfo.loadLabel(packageManager).toString()
        } catch (_: Exception) {
            pkg
        }

        val hasBanner = try {
            activityInfo.loadBanner(packageManager) != null
        } catch (_: Exception) {
            false
        }

        val packageInfo = try {
            packageManager.getPackageInfo(pkg, 0)
        } catch (_: Exception) {
            null
        }

        val lastUpdateTime = packageInfo?.lastUpdateTime ?: 0L
        val isSystemApp = packageInfo?.applicationInfo?.let {
            (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } ?: false

        val cacheKey = "$pkg-$lastUpdateTime"
        val iconDrawable = try {
            activityInfo.loadIcon(packageManager)
        } catch (_: Exception) {
            null
        }

        val colors = PaletteExtractor.getColors(cacheKey, iconDrawable)

        // Pre-rasterize and cache icon and banner on background thread
        AppIconCache.getIcon(pkg, activityInfo.name, packageManager)
        if (hasBanner) {
            AppIconCache.getBanner(pkg, activityInfo.name, packageManager)
        }

        return AppItem(
            packageName = pkg,
            activityName = activityInfo.name,
            label = label,
            isLeanback = isLeanback,
            hasBanner = hasBanner,
            isSystemApp = isSystemApp,
            lastUpdateTime = lastUpdateTime,
            dynamicColors = colors
        )
    }

    // Room Favorites Flow
    val favoritesDaoFlow: Flow<List<FavoriteApp>> = favoriteDao.getAllFavorites()

    // Room Hidden Flow
    val hiddenDaoFlow: Flow<List<HiddenApp>> = hiddenDao.getAllHidden()

    // 1. Favorites Panel apps (up to 6, maintaining added order)
    val favoriteAppsFlow: Flow<List<AppItem>> = combine(
        rawInstalledAppsFlow,
        favoritesDaoFlow
    ) { apps, favorites ->
        val appMap = apps.associateBy { it.packageName }
        favorites.mapNotNull { fav -> appMap[fav.packageName] }
    }

    // 2. Grid Apps (Remaining apps: not hidden and not favorite)
    val gridAppsFlow: Flow<List<AppItem>> = combine(
        rawInstalledAppsFlow,
        favoritesDaoFlow,
        hiddenDaoFlow
    ) { apps, favorites, hidden ->
        val favoritePackages = favorites.map { it.packageName }.toSet()
        val hiddenPackages = hidden.map { it.packageName }.toSet()

        val collator = Collator.getInstance(Locale.getDefault())
        apps.filter { it.packageName !in favoritePackages && it.packageName !in hiddenPackages }
            .sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    // 3. Hidden apps list for Settings management
    val hiddenAppsListFlow: Flow<List<AppItem>> = combine(
        rawInstalledAppsFlow,
        hiddenDaoFlow
    ) { apps, hidden ->
        val hiddenPackages = hidden.map { it.packageName }.toSet()
        apps.filter { it.packageName in hiddenPackages }
    }

    suspend fun addFavorite(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val count = favoriteDao.getCount()
        if (count >= 6) return@withContext false

        val currentFavs = favoriteDao.getAllFavoritesList()
        if (currentFavs.any { it.packageName == packageName }) return@withContext true

        val nextPosition = currentFavs.size
        favoriteDao.insert(FavoriteApp(packageName, nextPosition))
        true
    }

    suspend fun removeFavorite(packageName: String) = withContext(Dispatchers.IO) {
        favoriteDao.deleteByPackage(packageName)
        val remaining = favoriteDao.getAllFavoritesList()
        favoriteDao.renumberFavorites(remaining)
    }

    suspend fun hideApp(packageName: String) = withContext(Dispatchers.IO) {
        // Also remove from favorites if present
        favoriteDao.deleteByPackage(packageName)
        val remaining = favoriteDao.getAllFavoritesList()
        favoriteDao.renumberFavorites(remaining)
        hiddenDao.insert(HiddenApp(packageName))
    }

    suspend fun unhideApp(packageName: String) = withContext(Dispatchers.IO) {
        hiddenDao.deleteByPackage(packageName)
    }

    suspend fun resetFavorites() = withContext(Dispatchers.IO) {
        favoriteDao.clearAll()
    }

    fun launchApp(context: Context, app: AppItem): Boolean {
        return try {
            val intent = packageManager.getLeanbackLaunchIntentForPackage(app.packageName)
                ?: packageManager.getLaunchIntentForPackage(app.packageName)

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun openAppInfo(context: Context, packageName: String): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun uninstallApp(context: Context, packageName: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
