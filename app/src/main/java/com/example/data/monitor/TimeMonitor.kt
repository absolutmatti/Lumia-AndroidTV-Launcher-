package com.example.data.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import com.example.data.datastore.TimeFormatPreference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TimeDateState(
    val formattedTime: String,
    val formattedDate: String
)

class TimeMonitor(private val context: Context) {

    fun observeTime(timeFormatPreference: TimeFormatPreference): Flow<TimeDateState> = callbackFlow {
        fun update() {
            val now = Date()
            val locale = Locale.getDefault()

            // 1. Time string
            val timePattern = when (timeFormatPreference) {
                TimeFormatPreference.FORMAT_12H -> "h:mm a"
                TimeFormatPreference.FORMAT_24H -> "HH:mm"
                TimeFormatPreference.SYSTEM -> {
                    if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
                }
            }
            val timeStr = SimpleDateFormat(timePattern, locale).format(now)

            // 2. Date string: "EEEdMMM" -> "Do., 7. März" / "Thu, Mar 7"
            val dateSkeleton = "EEEdMMM"
            val bestPattern = DateFormat.getBestDateTimePattern(locale, dateSkeleton)
            val dateStr = SimpleDateFormat(bestPattern, locale).format(now)

            trySend(TimeDateState(formattedTime = timeStr, formattedDate = dateStr))
        }

        update()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                update()
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_LOCALE_CHANGED)
        }

        context.registerReceiver(receiver, filter)

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }
}
