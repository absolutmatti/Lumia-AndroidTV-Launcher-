package com.example.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lumia_settings")

enum class TimeFormatPreference {
    SYSTEM,
    FORMAT_12H,
    FORMAT_24H
}

enum class HomeFocusPreference {
    FAVORITES,
    LAST_POSITION
}

enum class CardColorPreference {
    DYNAMIC,
    UNIFORM
}

enum class BackgroundPreference {
    DEFAULT,
    CUSTOM
}

data class LauncherSettings(
    val timeFormat: TimeFormatPreference = TimeFormatPreference.SYSTEM,
    val showDate: Boolean = true,
    val homeFocus: HomeFocusPreference = HomeFocusPreference.FAVORITES,
    val cardColor: CardColorPreference = CardColorPreference.DYNAMIC,
    val backgroundMode: BackgroundPreference = BackgroundPreference.DEFAULT
)

class SettingsRepository(private val context: Context) {
    companion object {
        val KEY_TIME_FORMAT = stringPreferencesKey("time_format")
        val KEY_SHOW_DATE = booleanPreferencesKey("show_date")
        val KEY_HOME_FOCUS = stringPreferencesKey("home_focus_mode")
        val KEY_CARD_COLOR = stringPreferencesKey("card_color_mode")
        val KEY_BACKGROUND_MODE = stringPreferencesKey("background_mode")
    }

    val settingsFlow: Flow<LauncherSettings> = context.dataStore.data.map { prefs ->
        val timeFormat = when (prefs[KEY_TIME_FORMAT]) {
            "12H" -> TimeFormatPreference.FORMAT_12H
            "24H" -> TimeFormatPreference.FORMAT_24H
            else -> TimeFormatPreference.SYSTEM
        }
        val showDate = prefs[KEY_SHOW_DATE] ?: true
        val homeFocus = when (prefs[KEY_HOME_FOCUS]) {
            "LAST_POSITION" -> HomeFocusPreference.LAST_POSITION
            else -> HomeFocusPreference.FAVORITES
        }
        val cardColor = when (prefs[KEY_CARD_COLOR]) {
            "UNIFORM" -> CardColorPreference.UNIFORM
            else -> CardColorPreference.DYNAMIC
        }
        val backgroundMode = when (prefs[KEY_BACKGROUND_MODE]) {
            "CUSTOM" -> BackgroundPreference.CUSTOM
            else -> BackgroundPreference.DEFAULT
        }
        LauncherSettings(
            timeFormat = timeFormat,
            showDate = showDate,
            homeFocus = homeFocus,
            cardColor = cardColor,
            backgroundMode = backgroundMode
        )
    }

    suspend fun setTimeFormat(format: TimeFormatPreference) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TIME_FORMAT] = when (format) {
                TimeFormatPreference.FORMAT_12H -> "12H"
                TimeFormatPreference.FORMAT_24H -> "24H"
                TimeFormatPreference.SYSTEM -> "SYSTEM"
            }
        }
    }

    suspend fun setShowDate(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SHOW_DATE] = show
        }
    }

    suspend fun setHomeFocus(mode: HomeFocusPreference) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HOME_FOCUS] = when (mode) {
                HomeFocusPreference.LAST_POSITION -> "LAST_POSITION"
                HomeFocusPreference.FAVORITES -> "FAVORITES"
            }
        }
    }

    suspend fun setCardColor(mode: CardColorPreference) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CARD_COLOR] = when (mode) {
                CardColorPreference.UNIFORM -> "UNIFORM"
                CardColorPreference.DYNAMIC -> "DYNAMIC"
            }
        }
    }

    suspend fun setBackgroundMode(mode: BackgroundPreference) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BACKGROUND_MODE] = when (mode) {
                BackgroundPreference.CUSTOM -> "CUSTOM"
                BackgroundPreference.DEFAULT -> "DEFAULT"
            }
        }
    }
}
