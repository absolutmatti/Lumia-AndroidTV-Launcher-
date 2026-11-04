package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.monitor.NetworkStatus
import com.example.data.monitor.TimeDateState
import com.example.ui.theme.LumiaFocusBorder
import com.example.ui.theme.LumiaTextPrimary

@Composable
fun TopGradientOverlay(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.40f),
                        Color.Black.copy(alpha = 0.20f),
                        Color.Transparent
                    )
                )
            )
    )
}

@Composable
fun StatusBar(
    timeDateState: TimeDateState,
    showDate: Boolean,
    networkStatus: NetworkStatus,
    onSettingsClick: () -> Unit,
    onWifiClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textShadow = Shadow(
        color = Color.Black.copy(alpha = 0.6f),
        offset = Offset(1f, 2f),
        blurRadius = 4f
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 16.dp)
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side: Gear and Wi-Fi icons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusBarIconButton(
                testTag = "status_settings_button",
                contentDescription = stringResource(R.string.settings_title),
                onClick = onSettingsClick
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = LumiaTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            StatusBarIconButton(
                testTag = "status_wifi_button",
                contentDescription = stringResource(R.string.wifi_settings),
                onClick = onWifiClick
            ) {
                val iconVector = when (networkStatus) {
                    NetworkStatus.WIFI -> Icons.Default.Wifi
                    NetworkStatus.ETHERNET -> Icons.Default.SettingsEthernet
                    NetworkStatus.DISCONNECTED -> Icons.Default.WifiOff
                }
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = LumiaTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Right side: Date and Time
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (showDate && timeDateState.formattedDate.isNotEmpty()) {
                Text(
                    text = timeDateState.formattedDate,
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        color = LumiaTextPrimary,
                        shadow = textShadow
                    )
                )
            }

            Text(
                text = timeDateState.formattedTime,
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    color = LumiaTextPrimary,
                    shadow = textShadow
                )
            )
        }
    }
}

@Composable
private fun StatusBarIconButton(
    testTag: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .testTag(testTag)
            .size(44.dp)
            .clip(shape)
            .background(
                if (isFocused) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.25f)
            )
            .then(
                if (isFocused) {
                    Modifier.border(BorderStroke(2.dp, LumiaFocusBorder), shape)
                } else Modifier
            )
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClickLabel = contentDescription,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
