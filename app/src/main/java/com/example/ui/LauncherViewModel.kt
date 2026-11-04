package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.LumiaApp
import com.example.R
import com.example.data.datastore.BackgroundPreference
import com.example.data.datastore.CardColorPreference
import com.example.data.datastore.HomeFocusPreference
import com.example.data.datastore.LauncherSettings
import com.example.data.datastore.TimeFormatPreference
import com.example.data.model.AppItem
import com.example.data.monitor.NetworkStatus
import com.example.data.monitor.TimeDateState
import com.example.data.wallpaper.LocalMediaImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface NavigationEvent {
    object ScrollToTopAndFocusFavorites : NavigationEvent
}

data class LauncherUiState(
    val favoriteApps: List<AppItem> = emptyList(),
    val gridApps: List<AppItem> = emptyList(),
    val hiddenApps: List<AppItem> = emptyList(),
    val settings: LauncherSettings = LauncherSettings(),
    val networkStatus: NetworkStatus = NetworkStatus.DISCONNECTED,
    val timeDateState: TimeDateState = TimeDateState(formattedTime = "--:--", formattedDate = ""),
    val customWallpaperFile: File? = null,
    val contextMenuTarget: AppItem? = null,
    val isContextMenuTargetFavorite: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val isCustomPickerOpen: Boolean = false,
    val deviceImages: List<LocalMediaImage> = emptyList(),
    val isLoadingDeviceImages: Boolean = false,
    val toastMessage: String? = null,
    val showResetFavoritesDialog: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LumiaApp
    private val appRepository = app.appRepository
    private val settingsRepository = app.settingsRepository
    private val networkMonitor = app.networkMonitor
    private val timeMonitor = app.timeMonitor
    private val wallpaperManager = app.wallpaperManager

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    private val _navEvents = MutableSharedFlow<NavigationEvent>(extraBufferCapacity = 1)
    val navEvents: SharedFlow<NavigationEvent> = _navEvents.asSharedFlow()

    init {
        // Collect settings
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                val customFile = if (settings.backgroundMode == BackgroundPreference.CUSTOM) {
                    wallpaperManager.getCustomWallpaperFile()
                } else null

                _uiState.update {
                    it.copy(
                        settings = settings,
                        customWallpaperFile = customFile
                    )
                }
            }
        }

        // Collect time based on settings time format
        viewModelScope.launch {
            settingsRepository.settingsFlow.flatMapLatest { settings ->
                timeMonitor.observeTime(settings.timeFormat)
            }.collect { timeDate ->
                _uiState.update { it.copy(timeDateState = timeDate) }
            }
        }

        // Collect network
        viewModelScope.launch {
            networkMonitor.networkStatusFlow.collect { status ->
                _uiState.update { it.copy(networkStatus = status) }
            }
        }

        // Collect Favorites
        viewModelScope.launch {
            appRepository.favoriteAppsFlow.collect { favorites ->
                _uiState.update { it.copy(favoriteApps = favorites) }
            }
        }

        // Collect Grid Apps
        viewModelScope.launch {
            appRepository.gridAppsFlow.collect { grid ->
                _uiState.update { it.copy(gridApps = grid) }
            }
        }

        // Collect Hidden Apps
        viewModelScope.launch {
            appRepository.hiddenAppsListFlow.collect { hidden ->
                _uiState.update { it.copy(hiddenApps = hidden) }
            }
        }
    }

    fun onCardClick(context: Context, appItem: AppItem) {
        val success = appRepository.launchApp(context, appItem)
        if (!success) {
            _uiState.update {
                it.copy(toastMessage = context.getString(R.string.toast_cannot_launch))
            }
        }
    }

    fun onCardLongClick(appItem: AppItem, isFavorite: Boolean) {
        _uiState.update {
            it.copy(
                contextMenuTarget = appItem,
                isContextMenuTargetFavorite = isFavorite
            )
        }
    }

    fun closeContextMenu() {
        _uiState.update { it.copy(contextMenuTarget = null) }
    }

    fun addFavorite(packageName: String) {
        viewModelScope.launch {
            val success = appRepository.addFavorite(packageName)
            if (!success) {
                _uiState.update {
                    it.copy(
                        toastMessage = getApplication<Application>().getString(R.string.favorites_full_hint),
                        contextMenuTarget = null
                    )
                }
            } else {
                _uiState.update { it.copy(contextMenuTarget = null) }
            }
        }
    }

    fun removeFavorite(packageName: String) {
        viewModelScope.launch {
            appRepository.removeFavorite(packageName)
            _uiState.update { it.copy(contextMenuTarget = null) }
        }
    }

    fun hideApp(packageName: String) {
        viewModelScope.launch {
            appRepository.hideApp(packageName)
            _uiState.update {
                it.copy(
                    contextMenuTarget = null,
                    toastMessage = getApplication<Application>().getString(R.string.toast_hidden)
                )
            }
        }
    }

    fun unhideApp(packageName: String) {
        viewModelScope.launch {
            appRepository.unhideApp(packageName)
        }
    }

    fun openAppInfo(context: Context, packageName: String) {
        closeContextMenu()
        val success = appRepository.openAppInfo(context, packageName)
        if (!success) {
            _uiState.update {
                it.copy(toastMessage = context.getString(R.string.toast_setting_unavailable))
            }
        }
    }

    fun uninstallApp(context: Context, packageName: String) {
        closeContextMenu()
        appRepository.uninstallApp(context, packageName)
    }

    fun openSettings() {
        _uiState.update { it.copy(isSettingsOpen = true, contextMenuTarget = null) }
    }

    fun closeSettings() {
        _uiState.update {
            it.copy(
                isSettingsOpen = false,
                isCustomPickerOpen = false,
                showResetFavoritesDialog = false
            )
        }
    }

    fun setTimeFormat(format: TimeFormatPreference) {
        viewModelScope.launch {
            settingsRepository.setTimeFormat(format)
        }
    }

    fun setShowDate(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowDate(show)
        }
    }

    fun setHomeFocus(focus: HomeFocusPreference) {
        viewModelScope.launch {
            settingsRepository.setHomeFocus(focus)
        }
    }

    fun setCardColor(colorPref: CardColorPreference) {
        viewModelScope.launch {
            settingsRepository.setCardColor(colorPref)
        }
    }

    fun requestResetFavorites() {
        _uiState.update { it.copy(showResetFavoritesDialog = true) }
    }

    fun dismissResetFavoritesDialog() {
        _uiState.update { it.copy(showResetFavoritesDialog = false) }
    }

    fun confirmResetFavorites() {
        viewModelScope.launch {
            appRepository.resetFavorites()
            _uiState.update { it.copy(showResetFavoritesDialog = false) }
        }
    }

    fun openCustomWallpaperPicker() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCustomPickerOpen = true, isLoadingDeviceImages = true) }
            val images = wallpaperManager.loadDeviceImages()
            _uiState.update {
                it.copy(
                    isLoadingDeviceImages = false,
                    deviceImages = images,
                    toastMessage = if (images.isEmpty()) {
                        getApplication<Application>().getString(R.string.toast_no_images)
                    } else null
                )
            }
        }
    }

    fun closeCustomWallpaperPicker() {
        _uiState.update { it.copy(isCustomPickerOpen = false) }
    }

    fun setCustomWallpaperFromUri(uri: Uri) {
        viewModelScope.launch {
            val saved = wallpaperManager.saveCustomWallpaper(uri)
            if (saved) {
                settingsRepository.setBackgroundMode(BackgroundPreference.CUSTOM)
                val customFile = wallpaperManager.getCustomWallpaperFile()
                _uiState.update {
                    it.copy(
                        customWallpaperFile = customFile,
                        isCustomPickerOpen = false,
                        toastMessage = getApplication<Application>().getString(R.string.toast_wallpaper_saved)
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        toastMessage = getApplication<Application>().getString(R.string.toast_cannot_launch)
                    )
                }
            }
        }
    }

    fun resetWallpaperToDefault() {
        viewModelScope.launch {
            wallpaperManager.resetCustomWallpaper()
            settingsRepository.setBackgroundMode(BackgroundPreference.DEFAULT)
            _uiState.update {
                it.copy(
                    customWallpaperFile = null,
                    toastMessage = getApplication<Application>().getString(R.string.toast_wallpaper_reset)
                )
            }
        }
    }

    fun openWifiSettings(context: Context) {
        val intents = listOf(
            Intent(Settings.ACTION_WIFI_SETTINGS),
            Intent(Settings.ACTION_WIRELESS_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return
                }
            } catch (_: Exception) {}
        }
        _uiState.update {
            it.copy(toastMessage = context.getString(R.string.toast_setting_unavailable))
        }
    }

    fun openSystemAction(context: Context, action: String) {
        try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                _uiState.update {
                    it.copy(toastMessage = context.getString(R.string.toast_setting_unavailable))
                }
            }
        } catch (_: Exception) {
            _uiState.update {
                it.copy(toastMessage = context.getString(R.string.toast_setting_unavailable))
            }
        }
    }

    fun onHomePressed() {
        // Closes all overlays
        val wasOverlayOpen = _uiState.value.isSettingsOpen ||
                _uiState.value.contextMenuTarget != null ||
                _uiState.value.isCustomPickerOpen

        _uiState.update {
            it.copy(
                isSettingsOpen = false,
                contextMenuTarget = null,
                isCustomPickerOpen = false,
                showResetFavoritesDialog = false
            )
        }

        if (_uiState.value.settings.homeFocus == HomeFocusPreference.FAVORITES) {
            _navEvents.tryEmit(NavigationEvent.ScrollToTopAndFocusFavorites)
        }
    }

    fun handleBack(): Boolean {
        val state = _uiState.value
        if (state.showResetFavoritesDialog) {
            _uiState.update { it.copy(showResetFavoritesDialog = false) }
            return true
        }
        if (state.isCustomPickerOpen) {
            _uiState.update { it.copy(isCustomPickerOpen = false) }
            return true
        }
        if (state.isSettingsOpen) {
            _uiState.update { it.copy(isSettingsOpen = false) }
            return true
        }
        if (state.contextMenuTarget != null) {
            _uiState.update { it.copy(contextMenuTarget = null) }
            return true
        }
        return false
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
