package com.example.ui.components

import android.view.KeyEvent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.datastore.CardColorPreference
import com.example.data.model.AppItem
import com.example.data.repository.AppIconCache
import com.example.ui.theme.LumiaCardDefault
import com.example.ui.theme.LumiaTextPrimary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AppCard(
    app: AppItem,
    cardColorPreference: CardColorPreference,
    onCardClick: (AppItem) -> Unit,
    onCardLongClick: (AppItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Soft, organic TV focus transition (180ms ease-out)
    // Runs in graphicsLayer & draw phase so the card NEVER recomposes during focus animation
    val focusProgress by animateFloatAsState(
        targetValue = if (isFocused) 1.0f else 0.0f,
        animationSpec = tween(
            durationMillis = 180,
            easing = FastOutSlowInEasing
        ),
        label = "card_focus_progress"
    )

    val shape = RoundedCornerShape(12.dp)
    val coroutineScope = rememberCoroutineScope()
    var longPressJob by remember { mutableStateOf<Job?>(null) }
    var longPressTriggered by remember { mutableStateOf(false) }

    // Instant cached ImageBitmaps (O(1) memory lookup)
    val bannerBitmap = remember(app.packageName, app.hasBanner) {
        if (app.hasBanner) {
            AppIconCache.getBanner(app.packageName, app.activityName, pm)
        } else null
    }

    val iconBitmap = remember(app.packageName, app.lastUpdateTime) {
        AppIconCache.getIcon(app.packageName, app.activityName, pm)
    }

    val cardBgColor = remember(app.dynamicColors, cardColorPreference) {
        if (cardColorPreference == CardColorPreference.DYNAMIC) {
            Color(app.dynamicColors.backgroundColor)
        } else {
            LumiaCardDefault
        }
    }

    val textColor = remember(app.dynamicColors, cardColorPreference) {
        if (cardColorPreference == CardColorPreference.DYNAMIC) {
            Color(app.dynamicColors.textColor)
        } else {
            LumiaTextPrimary
        }
    }

    Box(
        modifier = modifier
            .testTag("app_card_${app.packageName}")
            .zIndex(if (isFocused) 10f else 1f)
            .graphicsLayer {
                val scale = 1.0f + (0.07f * focusProgress)
                scaleX = scale
                scaleY = scale
                // GPU RenderNode elevation shadow only when focused
                shadowElevation = (10.dp * focusProgress).toPx()
                this.shape = shape
                clip = false
            }
            .clip(shape)
            .background(cardBgColor)
            .drawWithContent {
                drawContent()
                // Focus border: drawn purely in draw phase without triggering recomposition
                if (focusProgress > 0.005f) {
                    val strokeWidth = 3.dp.toPx()
                    val halfStroke = strokeWidth / 2f
                    drawRoundRect(
                        color = Color.White.copy(alpha = focusProgress),
                        topLeft = Offset(halfStroke, halfStroke),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                        style = Stroke(width = strokeWidth)
                    )
                    // Soft white illumination sheen when focused
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.06f * focusProgress),
                        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                    )
                }
            }
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { keyEvent ->
                val native = keyEvent.nativeKeyEvent
                val isDpadCenter = native.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                        native.keyCode == KeyEvent.KEYCODE_ENTER
                val isMenu = native.keyCode == KeyEvent.KEYCODE_MENU

                if (isMenu) {
                    if (native.action == KeyEvent.ACTION_UP) {
                        onCardLongClick(app)
                    }
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
                                onCardLongClick(app)
                            }
                        }
                        return@onKeyEvent true
                    } else if (native.action == KeyEvent.ACTION_UP) {
                        longPressJob?.cancel()
                        if (!longPressTriggered) {
                            onCardClick(app)
                        }
                        longPressTriggered = false
                        return@onKeyEvent true
                    }
                }
                false
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onCardClick(app) }
            )
            .aspectRatio(16f / 9f)
    ) {
        if (bannerBitmap != null) {
            Image(
                bitmap = bannerBitmap,
                contentDescription = app.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = null,
                        modifier = Modifier.size(38.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.size(38.dp))
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = app.label,
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

