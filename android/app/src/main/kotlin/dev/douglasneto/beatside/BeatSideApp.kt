package dev.douglasneto.beatside

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.douglasneto.beatside.menu.BeatSideMenu
import dev.douglasneto.beatside.menu.MenuEffect
import dev.douglasneto.beatside.menu.MenuNavigator
import dev.douglasneto.beatside.menu.MenuState
import dev.douglasneto.beatside.settings.WheelSettings
import dev.douglasneto.beatside.ui.LcdScreen
import dev.douglasneto.beatside.wheel.ClickWheel
import dev.douglasneto.beatside.wheel.WheelButton
import dev.douglasneto.beatside.wheel.WheelEvent

/** Test tag of the wheel, used by UI tests to send touches. */
const val ClickWheelTestTag = "click_wheel"

private val Body = Brush.verticalGradient(listOf(Color(0xFFFAFAF8), Color(0xFFE3E3DF)))

/**
 * Root composable: owns the menu state and the session settings, and routes every wheel event
 * through [MenuNavigator]. The system back gesture is a MENU press while there is a level to go
 * back to (FR-014); at the root it is left to the system.
 */
@Composable
fun BeatSideApp() {
    var menuState by remember { mutableStateOf(MenuState.root(BeatSideMenu.root)) }
    var settings by remember { mutableStateOf(WheelSettings()) }

    fun onWheelEvent(event: WheelEvent) {
        val result = MenuNavigator.reduce(menuState, event)
        menuState = result.state
        when (val effect = result.effect) {
            is MenuEffect.ToggleSetting -> settings = settings.toggled(effect.key)
            // Next / Previous / Play-Pause and long press have no effect in this slice (FR-011).
            is MenuEffect.Unhandled, null -> Unit
        }
    }

    BackHandler(enabled = menuState.canGoBack) {
        onWheelEvent(WheelEvent.Press(WheelButton.Menu))
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Body)
            .safeDrawingPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        LcdScreen(menuState, settings, Modifier.fillMaxWidth().weight(1f))
        ClickWheel(onEvent = ::onWheelEvent, modifier = Modifier.fillMaxWidth(0.82f).testTag(ClickWheelTestTag))
    }
}
