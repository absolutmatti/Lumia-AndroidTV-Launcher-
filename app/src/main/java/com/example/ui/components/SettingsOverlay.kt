package com.example.ui.components

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.datastore.CardColorPreference
import com.example.data.datastore.HomeFocusPreference
import com.example.data.datastore.LauncherSettings
import com.example.data.datastore.TimeFormatPreference
import com.example.data.model.AppItem
import com.example.ui.theme.LumiaAccent
import com.example.ui.theme.LumiaDestructive
import com.example.ui.theme.LumiaFocusBorder
import com.example.ui.theme.LumiaOverlayBg
import com.example.ui.theme.LumiaOverlayScrim
import com.example.ui.theme.LumiaTextPrimary
import com.example.ui.theme.LumiaTextSecondary

enum class SettingsCategory {
    GENERAL,
    APPEARANCE,
    APPS,
    SYSTEM,
    ABOUT
}

@Composable
fun SettingsOverlay(
    settings: LauncherSettings,
    hiddenApps: List<AppItem>,
    showResetFavoritesDialog: Boolean,
    onTimeFormatChange: (TimeFormatPreference) -> Unit,
    onShowDateChange: (Boolean) -> Unit,
    onHomeFocusChange: (HomeFocusPreference) -> Unit,
    onCardColorChange: (CardColorPreference) -> Unit,
    onChooseWallpaperClick: () -> Unit,
    onResetWallpaperClick: () -> Unit,
    onUnhideApp: (String) -> Unit,
    onRequestResetFavorites: () -> Unit,
    onConfirmResetFavorites: () -> Unit,
    onDismissResetFavorites: () -> Unit,
    onSystemAction: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(SettingsCategory.GENERAL) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler(onBack = onDismiss)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LumiaOverlayScrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            val panelShape = RoundedCornerShape(24.dp)

            Row(
                modifier = Modifier
                    .testTag("settings_overlay_dialog")
                    .width(780.dp)
                    .height(460.dp)
                    .clip(panelShape)
                    .background(LumiaOverlayBg)
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), panelShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // consume click
                    )
            ) {
                // Left Column: Categories
                Column(
                    modifier = Modifier
                        .width(240.dp)
                        .fillMaxHeight()
                        .background(Color.Black.copy(alpha = 0.25f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        color = LumiaTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(4.dp))

                    CategoryTabItem(
                        title = stringResource(R.string.category_general),
                        icon = Icons.Default.Tune,
                        isSelected = selectedCategory == SettingsCategory.GENERAL,
                        onClick = { selectedCategory = SettingsCategory.GENERAL }
                    )

                    CategoryTabItem(
                        title = stringResource(R.string.category_appearance),
                        icon = Icons.Default.ColorLens,
                        isSelected = selectedCategory == SettingsCategory.APPEARANCE,
                        onClick = { selectedCategory = SettingsCategory.APPEARANCE }
                    )

                    CategoryTabItem(
                        title = stringResource(R.string.category_apps),
                        icon = Icons.Default.Apps,
                        isSelected = selectedCategory == SettingsCategory.APPS,
                        onClick = { selectedCategory = SettingsCategory.APPS }
                    )

                    CategoryTabItem(
                        title = stringResource(R.string.category_system),
                        icon = Icons.Default.PhoneAndroid,
                        isSelected = selectedCategory == SettingsCategory.SYSTEM,
                        onClick = { selectedCategory = SettingsCategory.SYSTEM }
                    )

                    CategoryTabItem(
                        title = stringResource(R.string.category_about),
                        icon = Icons.Default.Info,
                        isSelected = selectedCategory == SettingsCategory.ABOUT,
                        onClick = { selectedCategory = SettingsCategory.ABOUT }
                    )
                }

                // Right Column: Content for selected category
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(24.dp)
                ) {
                    when (selectedCategory) {
                        SettingsCategory.GENERAL -> GeneralSettingsContent(
                            settings = settings,
                            onTimeFormatChange = onTimeFormatChange,
                            onShowDateChange = onShowDateChange,
                            onHomeFocusChange = onHomeFocusChange
                        )
                        SettingsCategory.APPEARANCE -> AppearanceSettingsContent(
                            settings = settings,
                            onCardColorChange = onCardColorChange,
                            onChooseWallpaperClick = onChooseWallpaperClick,
                            onResetWallpaperClick = onResetWallpaperClick
                        )
                        SettingsCategory.APPS -> AppsSettingsContent(
                            hiddenApps = hiddenApps,
                            onUnhideApp = onUnhideApp,
                            onRequestResetFavorites = onRequestResetFavorites
                        )
                        SettingsCategory.SYSTEM -> SystemSettingsContent(
                            onSystemAction = onSystemAction
                        )
                        SettingsCategory.ABOUT -> AboutSettingsContent()
                    }
                }
            }
        }
    }

    if (showResetFavoritesDialog) {
        AlertDialog(
            onDismissRequest = onDismissResetFavorites,
            title = {
                Text(
                    text = stringResource(R.string.pref_reset_favorites),
                    color = LumiaTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.reset_favorites_confirm),
                    color = LumiaTextSecondary,
                    fontSize = 15.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmResetFavorites,
                    colors = ButtonDefaults.textButtonColors(contentColor = LumiaDestructive)
                ) {
                    Text(stringResource(R.string.action_reset), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissResetFavorites,
                    colors = ButtonDefaults.textButtonColors(contentColor = LumiaTextPrimary)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            containerColor = LumiaOverlayBg,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun CategoryTabItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                when {
                    isFocused -> Color.White.copy(alpha = 0.20f)
                    isSelected -> Color.White.copy(alpha = 0.10f)
                    else -> Color.Transparent
                }
            )
            .then(
                if (isFocused) Modifier.border(BorderStroke(2.dp, LumiaFocusBorder), shape)
                else Modifier
            )
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected || isFocused) LumiaTextPrimary else LumiaTextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = if (isSelected || isFocused) LumiaTextPrimary else LumiaTextSecondary,
            fontSize = 15.sp,
            fontWeight = if (isSelected || isFocused) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun GeneralSettingsContent(
    settings: LauncherSettings,
    onTimeFormatChange: (TimeFormatPreference) -> Unit,
    onShowDateChange: (Boolean) -> Unit,
    onHomeFocusChange: (HomeFocusPreference) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Show Date Switch
        SettingToggleRow(
            title = stringResource(R.string.pref_show_date),
            description = stringResource(R.string.pref_show_date_desc),
            checked = settings.showDate,
            onCheckedChange = onShowDateChange
        )

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

        // Time Format
        Text(
            text = stringResource(R.string.pref_time_format),
            color = LumiaTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingRadioOption(
                label = stringResource(R.string.time_format_system),
                selected = settings.timeFormat == TimeFormatPreference.SYSTEM,
                onClick = { onTimeFormatChange(TimeFormatPreference.SYSTEM) },
                modifier = Modifier.weight(1f)
            )
            SettingRadioOption(
                label = stringResource(R.string.time_format_12h),
                selected = settings.timeFormat == TimeFormatPreference.FORMAT_12H,
                onClick = { onTimeFormatChange(TimeFormatPreference.FORMAT_12H) },
                modifier = Modifier.weight(1f)
            )
            SettingRadioOption(
                label = stringResource(R.string.time_format_24h),
                selected = settings.timeFormat == TimeFormatPreference.FORMAT_24H,
                onClick = { onTimeFormatChange(TimeFormatPreference.FORMAT_24H) },
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

        // Home Focus Mode
        Text(
            text = stringResource(R.string.pref_home_focus),
            color = LumiaTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingRadioOption(
                label = stringResource(R.string.home_focus_favorites),
                selected = settings.homeFocus == HomeFocusPreference.FAVORITES,
                onClick = { onHomeFocusChange(HomeFocusPreference.FAVORITES) },
                modifier = Modifier.weight(1f)
            )
            SettingRadioOption(
                label = stringResource(R.string.home_focus_last_position),
                selected = settings.homeFocus == HomeFocusPreference.LAST_POSITION,
                onClick = { onHomeFocusChange(HomeFocusPreference.LAST_POSITION) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AppearanceSettingsContent(
    settings: LauncherSettings,
    onCardColorChange: (CardColorPreference) -> Unit,
    onChooseWallpaperClick: () -> Unit,
    onResetWallpaperClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Wallpaper Actions
        Text(
            text = stringResource(R.string.pref_wallpaper),
            color = LumiaTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingActionButton(
                label = stringResource(R.string.action_choose_image),
                icon = Icons.Default.Wallpaper,
                onClick = onChooseWallpaperClick,
                modifier = Modifier.weight(1f)
            )
            SettingActionButton(
                label = stringResource(R.string.action_reset_wallpaper),
                icon = Icons.Default.Restore,
                onClick = onResetWallpaperClick,
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

        // Card Color Scheme
        Text(
            text = stringResource(R.string.pref_card_colors),
            color = LumiaTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingRadioOption(
                label = stringResource(R.string.card_color_dynamic),
                selected = settings.cardColor == CardColorPreference.DYNAMIC,
                onClick = { onCardColorChange(CardColorPreference.DYNAMIC) },
                modifier = Modifier.weight(1f)
            )
            SettingRadioOption(
                label = stringResource(R.string.card_color_uniform),
                selected = settings.cardColor == CardColorPreference.UNIFORM,
                onClick = { onCardColorChange(CardColorPreference.UNIFORM) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AppsSettingsContent(
    hiddenApps: List<AppItem>,
    onUnhideApp: (String) -> Unit,
    onRequestResetFavorites: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingActionButton(
            label = stringResource(R.string.pref_reset_favorites),
            icon = Icons.Default.Refresh,
            isDestructive = true,
            onClick = onRequestResetFavorites,
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

        Text(
            text = stringResource(R.string.pref_hidden_apps),
            color = LumiaTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        if (hiddenApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_hidden_apps),
                    color = LumiaTextSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(hiddenApps, key = { it.packageName }) { app ->
                    HiddenAppItemRow(
                        app = app,
                        onUnhide = { onUnhideApp(app.packageName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HiddenAppItemRow(
    app: AppItem,
    onUnhide: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isFocused) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
            .then(
                if (isFocused) Modifier.border(BorderStroke(2.dp, LumiaFocusBorder), shape)
                else Modifier
            )
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onUnhide
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = app.label,
            color = LumiaTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = null,
                tint = LumiaAccent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.unhide_app),
                color = LumiaAccent,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun SystemSettingsContent(
    onSystemAction: (String) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val systemEntries = listOf(
        Pair(stringResource(R.string.sys_wifi), Settings.ACTION_WIFI_SETTINGS),
        Pair(stringResource(R.string.sys_display), Settings.ACTION_DISPLAY_SETTINGS),
        Pair(stringResource(R.string.sys_sound), Settings.ACTION_SOUND_SETTINGS),
        Pair(stringResource(R.string.sys_all_settings), Settings.ACTION_SETTINGS),
        Pair(stringResource(R.string.sys_home_launcher), Settings.ACTION_HOME_SETTINGS)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        systemEntries.forEach { (label, action) ->
            SettingActionButton(
                label = label,
                icon = Icons.Default.Settings,
                onClick = { onSystemAction(action) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AboutSettingsContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.app_name),
            color = LumiaTextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.about_version),
            color = LumiaAccent,
            fontSize = 14.sp
        )
        Text(
            text = stringResource(R.string.about_desc),
            color = LumiaTextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.about_licenses),
            color = LumiaTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = stringResource(R.string.licenses_text),
            color = LumiaTextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isFocused) Color.White.copy(alpha = 0.15f) else Color.Transparent)
            .then(
                if (isFocused) Modifier.border(BorderStroke(2.dp, LumiaFocusBorder), shape)
                else Modifier
            )
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = LumiaTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(text = description, color = LumiaTextSecondary, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = LumiaAccent
            )
        )
    }
}

@Composable
private fun SettingRadioOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                when {
                    isFocused -> Color.White.copy(alpha = 0.20f)
                    selected -> Color.White.copy(alpha = 0.12f)
                    else -> Color.White.copy(alpha = 0.04f)
                }
            )
            .then(
                if (isFocused) Modifier.border(BorderStroke(2.dp, LumiaFocusBorder), shape)
                else if (selected) Modifier.border(BorderStroke(1.dp, LumiaAccent.copy(alpha = 0.6f)), shape)
                else Modifier
            )
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected || isFocused) LumiaTextPrimary else LumiaTextSecondary,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SettingActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)

    val itemColor = if (isDestructive) LumiaDestructive else LumiaTextPrimary

    Row(
        modifier = modifier
            .clip(shape)
            .background(
                if (isFocused) Color.White.copy(alpha = 0.20f)
                else Color.White.copy(alpha = 0.05f)
            )
            .then(
                if (isFocused) Modifier.border(BorderStroke(2.dp, LumiaFocusBorder), shape)
                else Modifier
            )
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = itemColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            color = itemColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
