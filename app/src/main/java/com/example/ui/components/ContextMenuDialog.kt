package com.example.ui.components

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.data.model.AppItem
import com.example.data.repository.AppIconCache
import com.example.ui.theme.LumiaDestructive
import com.example.ui.theme.LumiaFocusBorder
import com.example.ui.theme.LumiaOverlayBg
import com.example.ui.theme.LumiaOverlayScrim
import com.example.ui.theme.LumiaTextPrimary
import com.example.ui.theme.LumiaTextSecondary
import kotlinx.coroutines.delay

@Composable
fun ContextMenuOverlay(
    app: AppItem,
    isFavorite: Boolean,
    favoritesCount: Int,
    onAddFavorite: () -> Unit,
    onRemoveFavorite: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    onHide: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val firstItemFocusRequester = remember { FocusRequester() }
    val openTimeMs = remember { System.currentTimeMillis() }

    val iconBitmap = remember(app.packageName, app.lastUpdateTime) {
        AppIconCache.getIcon(app.packageName, app.activityName, pm)
    }

    // Explicitly focus the first context menu action once layout is attached
    LaunchedEffect(app.packageName) {
        delay(60)
        try {
            firstItemFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(100f)
            .background(LumiaOverlayScrim)
            .onKeyEvent { keyEvent ->
                val native = keyEvent.nativeKeyEvent
                if (native.keyCode == KeyEvent.KEYCODE_BACK) {
                    if (native.action == KeyEvent.ACTION_UP) {
                        onDismiss()
                    }
                    return@onKeyEvent true
                }
                false
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        val cardShape = RoundedCornerShape(20.dp)

        Column(
            modifier = Modifier
                .testTag("context_menu_dialog")
                .width(360.dp)
                .clip(cardShape)
                .background(LumiaOverlayBg)
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), cardShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Consume click inside dialog body
                )
                .padding(24.dp)
        ) {
            // Header: App icon and title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Text(
                    text = app.label,
                    color = LumiaTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // 1. Favorite Action
            val isFavoritesFull = !isFavorite && favoritesCount >= 6
            val favoriteLabel = if (isFavorite) {
                stringResource(R.string.menu_remove_favorite)
            } else if (isFavoritesFull) {
                stringResource(R.string.favorites_full_hint)
            } else {
                stringResource(R.string.menu_add_favorite)
            }

            ContextMenuItem(
                testTag = "menu_item_favorite",
                title = favoriteLabel,
                icon = if (isFavorite) Icons.Default.StarOutline else Icons.Default.Star,
                enabled = true, // Always focusable so D-Pad navigation never gets lost
                focusRequester = firstItemFocusRequester,
                onClick = {
                    if (System.currentTimeMillis() - openTimeMs > 250L) {
                        if (isFavorite) {
                            onRemoveFavorite()
                        } else if (!isFavoritesFull) {
                            onAddFavorite()
                        }
                    }
                }
            )

            // 2. App Info
            ContextMenuItem(
                testTag = "menu_item_app_info",
                title = stringResource(R.string.menu_app_info),
                icon = Icons.Default.Info,
                enabled = true,
                onClick = {
                    if (System.currentTimeMillis() - openTimeMs > 250L) {
                        onAppInfo()
                    }
                }
            )

            // 3. Uninstall (only for non-system apps)
            if (!app.isSystemApp) {
                ContextMenuItem(
                    testTag = "menu_item_uninstall",
                    title = stringResource(R.string.menu_uninstall),
                    icon = Icons.Default.Delete,
                    enabled = true,
                    onClick = {
                        if (System.currentTimeMillis() - openTimeMs > 250L) {
                            onUninstall()
                        }
                    }
                )
            }

            // 4. Hide (destructive action)
            ContextMenuItem(
                testTag = "menu_item_hide",
                title = stringResource(R.string.menu_hide),
                icon = Icons.Default.VisibilityOff,
                isDestructive = true,
                enabled = true,
                onClick = {
                    if (System.currentTimeMillis() - openTimeMs > 250L) {
                        onHide()
                    }
                }
            )
        }
    }
}

@Composable
private fun ContextMenuItem(
    testTag: String,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDestructive: Boolean = false,
    focusRequester: FocusRequester? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)

    // Butter-smooth scale spring
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.03f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = 380f
        ),
        label = "menu_item_scale"
    )

    // Soft cross-fade on focus border & background
    val focusAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "menu_item_focus_alpha"
    )

    val itemColor = when {
        !enabled -> LumiaTextSecondary.copy(alpha = 0.4f)
        isDestructive -> if (isFocused) Color(0xFFFF5252) else LumiaDestructive
        else -> if (isFocused) Color.White else LumiaTextPrimary
    }

    Row(
        modifier = modifier
            .testTag(testTag)
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(Color.White.copy(alpha = 0.22f * focusAlpha))
            .border(
                BorderStroke(2.dp, LumiaFocusBorder.copy(alpha = focusAlpha)),
                shape
            )
            .tvClickable(
                enabled = enabled,
                interactionSource = interactionSource,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = itemColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            color = itemColor,
            fontSize = 16.sp,
            fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
