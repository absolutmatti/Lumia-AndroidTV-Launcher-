package com.example.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.datastore.BackgroundPreference
import com.example.ui.components.AppCard
import com.example.ui.components.ContextMenuOverlay
import com.example.ui.components.CustomWallpaperPicker
import com.example.ui.components.FavoritesPanel
import com.example.ui.components.SettingsOverlay
import com.example.ui.components.StatusBar
import com.example.ui.components.TopGradientOverlay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun LauncherScreen(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    // System file picker for wallpaper
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setCustomWallpaperFromUri(uri)
        }
    }

    // Handle toast messages
    LaunchedEffect(uiState.toastMessage) {
        val msg = uiState.toastMessage
        if (msg != null) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    val firstFavoriteFocusRequester = remember { FocusRequester() }
    val gearFocusRequester = remember { FocusRequester() }

    val returnToStartView: () -> Unit = remember(gridState, uiState.favoriteApps) {
        {
            coroutineScope.launch {
                gridState.scrollToItem(0)
                try {
                    if (uiState.favoriteApps.isNotEmpty()) {
                        firstFavoriteFocusRequester.requestFocus()
                    } else {
                        gearFocusRequester.requestFocus()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // Handle navigation events (e.g. HOME key -> scroll to top and focus first favorite)
    LaunchedEffect(Unit) {
        viewModel.navEvents.collectLatest { event ->
            when (event) {
                NavigationEvent.ScrollToTopAndFocusFavorites -> {
                    returnToStartView()
                }
            }
        }
    }

    // Global Back Handler:
    // If dialogs/overlays open, dismiss them; if in grid, scroll to top and focus first favorite!
    BackHandler {
        if (!viewModel.handleBack()) {
            if (gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0) {
                returnToStartView()
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("launcher_screen")
    ) {
        val screenHeight = maxHeight

        // 1. Wallpaper background layer
        Box(modifier = Modifier.fillMaxSize()) {
            val wallpaperModel = if (uiState.settings.backgroundMode == BackgroundPreference.CUSTOM &&
                uiState.customWallpaperFile != null
            ) {
                uiState.customWallpaperFile
            } else {
                R.drawable.default_wallpaper
            }

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(wallpaperModel)
                    .crossfade(false)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dynamic Scrim overlay:
            // Calculate alpha inside graphicsLayer to completely prevent recomposition during scroll!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val firstIndex = gridState.firstVisibleItemIndex
                        val offset = gridState.firstVisibleItemScrollOffset
                        alpha = if (firstIndex > 0) {
                            0.55f
                        } else {
                            val progress = (offset / 500f).coerceIn(0f, 1f)
                            0.25f + (0.30f * progress)
                        }
                    }
                    .background(Color.Black)
            )

            // Top gradient behind the status bar (120dp high)
            TopGradientOverlay(
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        // 2. Fixed Status Bar at the top (zIndex 20f, height 48dp, top margin 16dp)
        StatusBar(
            timeDateState = uiState.timeDateState,
            showDate = uiState.settings.showDate,
            networkStatus = uiState.networkStatus,
            onSettingsClick = { viewModel.openSettings() },
            onWifiClick = { viewModel.openWifiSettings(context) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(20f)
        )

        // 3. Scrollable Grid: clipped below the status bar (at 64dp from top)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp)
                .clipToBounds()
        ) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(
                    start = 40.dp,
                    end = 40.dp,
                    bottom = 72.dp
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("app_grid")
            ) {
                // Item 0: Home-Header (Span 6 columns)
                item(
                    span = { GridItemSpan(6) },
                    contentType = "home_header",
                    key = "home_header"
                ) {
                    val headerHeight = (screenHeight - 104.dp).coerceAtLeast(160.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(headerHeight),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        FavoritesPanel(
                            favorites = uiState.favoriteApps,
                            cardColorPreference = uiState.settings.cardColor,
                            onAppClick = { app -> viewModel.onCardClick(context, app) },
                            onAppLongClick = { app -> viewModel.onCardLongClick(app, isFavorite = true) },
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }

                // Grid Apps (Remaining apps: not hidden and not favorites)
                items(
                    items = uiState.gridApps,
                    key = { it.packageName },
                    contentType = { "app_card" }
                ) { app ->
                    AppCard(
                        app = app,
                        cardColorPreference = uiState.settings.cardColor,
                        onClick = { viewModel.onCardClick(context, app) },
                        onLongClick = { viewModel.onCardLongClick(app, isFavorite = false) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 4. Context Menu Overlay (In-Screen, D-pad accessible)
        uiState.contextMenuTarget?.let { targetApp ->
            ContextMenuOverlay(
                app = targetApp,
                isFavorite = uiState.isContextMenuTargetFavorite,
                favoritesCount = uiState.favoriteApps.size,
                onAddFavorite = { viewModel.addFavorite(targetApp.packageName) },
                onRemoveFavorite = { viewModel.removeFavorite(targetApp.packageName) },
                onAppInfo = { viewModel.openAppInfo(context, targetApp.packageName) },
                onUninstall = { viewModel.uninstallApp(context, targetApp.packageName) },
                onHide = { viewModel.hideApp(targetApp.packageName) },
                onDismiss = { viewModel.closeContextMenu() }
            )
        }

        // 5. Settings Overlay (In-Screen, D-pad accessible)
        if (uiState.isSettingsOpen) {
            SettingsOverlay(
                settings = uiState.settings,
                hiddenApps = uiState.hiddenApps,
                showResetFavoritesDialog = uiState.showResetFavoritesDialog,
                onTimeFormatChange = { viewModel.setTimeFormat(it) },
                onShowDateChange = { viewModel.setShowDate(it) },
                onHomeFocusChange = { viewModel.setHomeFocus(it) },
                onCardColorChange = { viewModel.setCardColor(it) },
                onChooseWallpaperClick = {
                    try {
                        docPickerLauncher.launch(arrayOf("image/*"))
                    } catch (_: Exception) {
                        viewModel.openCustomWallpaperPicker()
                    }
                },
                onResetWallpaperClick = { viewModel.resetWallpaperToDefault() },
                onUnhideApp = { viewModel.unhideApp(it) },
                onRequestResetFavorites = { viewModel.requestResetFavorites() },
                onConfirmResetFavorites = { viewModel.confirmResetFavorites() },
                onDismissResetFavorites = { viewModel.dismissResetFavoritesDialog() },
                onSystemAction = { action -> viewModel.openSystemAction(context, action) },
                onDismiss = { viewModel.closeSettings() }
            )
        }

        // 6. In-App Custom Wallpaper MediaStore Picker
        if (uiState.isCustomPickerOpen) {
            CustomWallpaperPicker(
                images = uiState.deviceImages,
                isLoading = uiState.isLoadingDeviceImages,
                onSelectImage = { uri -> viewModel.setCustomWallpaperFromUri(uri) },
                onDismiss = { viewModel.closeCustomWallpaperPicker() }
            )
        }
    }
}
