package com.example.ui.components

import android.content.ComponentName
import android.graphics.drawable.Drawable
import android.view.KeyEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.datastore.CardColorPreference
import com.example.data.model.AppItem
import com.example.ui.theme.LumiaCardDefault
import com.example.ui.theme.LumiaFocusBorder
import com.example.ui.theme.LumiaTextPrimary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppCard(
    app: AppItem,
    cardColorPreference: CardColorPreference,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "card_scale"
    )

    val shape = RoundedCornerShape(12.dp)
    val coroutineScope = rememberCoroutineScope()
    var longPressJob by remember { mutableStateOf<Job?>(null) }
    var longPressTriggered by remember { mutableStateOf(false) }

    // Resolve banner and icon drawables
    val bannerDrawable = remember(app.packageName, app.hasBanner) {
        if (app.hasBanner) {
            try {
                val component = ComponentName(app.packageName, app.activityName)
                context.packageManager.getActivityBanner(component)
                    ?: context.packageManager.getApplicationBanner(app.packageName)
            } catch (_: Exception) {
                null
            }
        } else null
    }

    val iconDrawable = remember(app.packageName, app.lastUpdateTime) {
        try {
            val component = ComponentName(app.packageName, app.activityName)
            context.packageManager.getActivityIcon(component)
                ?: context.packageManager.getApplicationIcon(app.packageName)
        } catch (_: Exception) {
            null
        }
    }

    val cardBgColor = remember(app, cardColorPreference) {
        if (cardColorPreference == CardColorPreference.DYNAMIC) {
            Color(app.dynamicColors.backgroundColor)
        } else {
            LumiaCardDefault
        }
    }

    val textColor = remember(app, cardColorPreference) {
        if (cardColorPreference == CardColorPreference.DYNAMIC) {
            Color(app.dynamicColors.textColor)
        } else {
            LumiaTextPrimary
        }
    }

    Box(
        modifier = modifier
            .testTag("app_card_${app.packageName}")
            .scale(scale)
            .zIndex(if (isFocused) 10f else 1f)
            .shadow(
                elevation = if (isFocused) 16.dp else 4.dp,
                shape = shape,
                clip = false
            )
            .clip(shape)
            .background(cardBgColor)
            .then(
                if (isFocused) {
                    Modifier.border(BorderStroke(3.dp, LumiaFocusBorder), shape)
                } else Modifier
            )
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { keyEvent ->
                val native = keyEvent.nativeKeyEvent
                val isDpadCenter = native.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                        native.keyCode == KeyEvent.KEYCODE_ENTER
                val isMenu = native.keyCode == KeyEvent.KEYCODE_MENU

                if (isMenu && native.action == KeyEvent.ACTION_UP) {
                    onLongClick()
                    return@onKeyEvent true
                }

                if (isDpadCenter) {
                    if (native.action == KeyEvent.ACTION_DOWN) {
                        if (native.repeatCount == 0) {
                            longPressTriggered = false
                            longPressJob?.cancel()
                            longPressJob = coroutineScope.launch {
                                delay(500)
                                longPressTriggered = true
                                onLongClick()
                            }
                        }
                    } else if (native.action == KeyEvent.ACTION_UP) {
                        longPressJob?.cancel()
                        if (!longPressTriggered) {
                            onClick()
                        }
                        longPressTriggered = false
                        return@onKeyEvent true
                    }
                }
                false
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .aspectRatio(16f / 9f)
    ) {
        if (bannerDrawable != null) {
            // Full 16:9 banner
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(bannerDrawable)
                    .size(507, 285)
                    .crossfade(true)
                    .build(),
                contentDescription = app.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Fallback 16:9 card: Icon centered at upper part, label below
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (iconDrawable != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(iconDrawable)
                            .size(128, 128)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.size(38.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.size(38.dp))
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = app.label,
                    color = textColor,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
