package com.example.ui.components

import android.view.KeyEvent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.onKeyEvent

/**
 * Ensures reliable D-Pad click interaction on Android TV devices.
 * CRITICAL: A click is ONLY recognized if ACTION_DOWN was pressed while
 * this specific composable was focused, with repeatCount == 0 (fresh press),
 * completely preventing phantom clicks when an overlay appears while the user
 * is still holding down the button.
 */
fun Modifier.tvClickable(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit
): Modifier = this.composed {
    var isPressedDownOnMe by remember { mutableStateOf(false) }

    this
        .focusable(enabled = enabled, interactionSource = interactionSource)
        .onKeyEvent { keyEvent ->
            if (!enabled) return@onKeyEvent false
            val native = keyEvent.nativeKeyEvent
            val isCenter = native.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                    native.keyCode == KeyEvent.KEYCODE_ENTER

            if (isCenter) {
                if (native.action == KeyEvent.ACTION_DOWN) {
                    // Only a fresh press down (repeatCount == 0) counts as pressing this item
                    if (native.repeatCount == 0) {
                        isPressedDownOnMe = true
                    }
                    return@onKeyEvent true
                } else if (native.action == KeyEvent.ACTION_UP) {
                    val wasDown = isPressedDownOnMe
                    isPressedDownOnMe = false
                    if (wasDown) {
                        onClick()
                    }
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
}
