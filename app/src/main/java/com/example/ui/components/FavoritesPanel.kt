package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.datastore.CardColorPreference
import com.example.data.model.AppItem
import com.example.ui.theme.LumiaFavoritesPanelBg
import com.example.ui.theme.LumiaFavoritesPanelBorder
import com.example.ui.theme.LumiaTextSecondary

@Composable
fun FavoritesPanel(
    favorites: List<AppItem>,
    cardColorPreference: CardColorPreference,
    onAppClick: (AppItem) -> Unit,
    onAppLongClick: (AppItem) -> Unit,
    modifier: Modifier = Modifier,
    firstItemFocusRequester: FocusRequester? = null
) {
    val panelShape = RoundedCornerShape(28.dp)

    Box(
        modifier = modifier
            .testTag("favorites_panel")
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(panelShape)
            .background(LumiaFavoritesPanelBg)
            .border(BorderStroke(1.dp, LumiaFavoritesPanelBorder), panelShape)
            .padding(horizontal = 28.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        if (favorites.isEmpty()) {
            Text(
                text = stringResource(R.string.favorites_empty_hint),
                color = LumiaTextSecondary,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 6) {
                    val app = favorites.getOrNull(i)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(16f / 9f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (app != null) {
                            val cardModifier = if (i == 0 && firstItemFocusRequester != null) {
                                Modifier.fillMaxWidth().focusRequester(firstItemFocusRequester)
                            } else {
                                Modifier.fillMaxWidth()
                            }
                            AppCard(
                                app = app,
                                cardColorPreference = cardColorPreference,
                                onCardClick = onAppClick,
                                onCardLongClick = onAppLongClick,
                                modifier = cardModifier
                            )
                        } else {
                            EmptySlotPlaceholder()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySlotPlaceholder(
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.03f))
            .border(
                BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                shape
            )
    )
}
