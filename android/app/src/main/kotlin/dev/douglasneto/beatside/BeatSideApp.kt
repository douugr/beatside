package dev.douglasneto.beatside

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import dev.douglasneto.beatside.menu.BeatSideMenu
import dev.douglasneto.beatside.menu.MenuEffect
import dev.douglasneto.beatside.menu.MenuNavigator
import dev.douglasneto.beatside.menu.MenuState
import dev.douglasneto.beatside.settings.AppSettings
import dev.douglasneto.beatside.ui.LcdScreen
import dev.douglasneto.beatside.ui.theme.ClassicWhite
import dev.douglasneto.beatside.ui.theme.LocalPalette
import dev.douglasneto.beatside.wheel.ClickWheel
import dev.douglasneto.beatside.wheel.WheelEvent

@Composable
fun BeatSideApp() {
    var menuState by remember { mutableStateOf(MenuState.root(BeatSideMenu.root)) }
    var settings by remember { mutableStateOf(AppSettings()) }

    fun onWheelEvent(event: WheelEvent) {
        val result = MenuNavigator.reduce(menuState, event)
        menuState = result.state
        when (val effect = result.effect) {
            is MenuEffect.ToggleSetting -> settings = settings.toggled(effect.key)
            is MenuEffect.Unhandled -> Unit // Phase 1 routes these to playback.
            null -> Unit
        }
    }

    // The system Back gesture behaves like the MENU button.
    BackHandler(enabled = menuState.canGoBack) { menuState = MenuNavigator.back(menuState) }

    CompositionLocalProvider(LocalPalette provides ClassicWhite) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(LocalPalette.current.body)
                .safeDrawingPadding(),
        ) {
            val wheelSize = min(maxWidth * 0.78f, 300.dp)
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                LcdScreen(
                    state = menuState,
                    settings = settings,
                    modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
                )
                ClickWheel(
                    onEvent = ::onWheelEvent,
                    size = wheelSize,
                    sensitivity = settings.sensitivity,
                    clickerEnabled = settings.clicker,
                    hapticsEnabled = settings.haptics,
                )
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun BeatSideAppPreview() {
    BeatSideApp()
}
