package dev.douglasneto.beatside.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import dev.douglasneto.beatside.ui.LcdColors
import kotlinx.coroutines.delay
import java.time.LocalTime

/**
 * Live local time, updated every second, in the device's 12/24-hour format (FR-024, SC-006).
 * The pattern decision is [ClockFormat]; this only observes the system: it re-reads the
 * setting when the time, time zone or locale changes (Android sends ACTION_TIME_CHANGED when the
 * 12/24-hour setting is switched), so the format updates while the screen is open.
 */
@Composable
fun ClockScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var is24Hour by remember { mutableStateOf(DateFormat.is24HourFormat(context)) }
    var systemChanges by remember { mutableIntStateOf(0) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                is24Hour = DateFormat.is24HourFormat(context)
                systemChanges++
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_LOCALE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    // Restarts on a system change so a new time zone shows at once; otherwise ticks on each second.
    val now by produceState(LocalTime.now(), systemChanges) {
        while (true) {
            value = LocalTime.now()
            delay(1_000L - value.nano / 1_000_000L)
        }
    }

    val locale = LocalConfiguration.current.locales[0]
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BasicText(
            ClockFormat.format(now, is24Hour, locale),
            style = TextStyle(
                color = LcdColors.Text,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum",
            ),
        )
    }
}
