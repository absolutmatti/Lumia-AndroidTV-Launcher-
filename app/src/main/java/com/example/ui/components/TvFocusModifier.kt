package com.example.ui.components

import android.view.KeyEvent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent

/**
 * Ensures reliable D-Pad click interaction on Android TV devices
 * where standard Compose clickable may miss DPAD_CENTER / ENTER key events.
 */
fun Modifier.tvClickable(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit
): Modifier = this
    .focusable(enabled = enabled, interactionSource = interactionSource)
    .onKeyEvent { keyEvent ->
        if (!enabled) return@onKeyEvent false
        val native = keyEvent.nativeKeyEvent
        val isCenter = native.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                native.keyCode == KeyEvent.KEYCODE_ENTER

        if (isCenter) {
            if (native.action == KeyEvent.ACTION_DOWN) {
                // Consume down event so system doesn't synthesize multiple triggers
                return@onKeyEvent true
            } else if (native.action == KeyEvent.ACTION_UP) {
                onClick()
                return@onKeyEvent true
            }
        }
        false
    }
    .clickable(
        enabled = enabled,
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
    )
