package dev.douglasneto.beatside.ui

import dev.douglasneto.beatside.menu.BeatSideMenu
import dev.douglasneto.beatside.menu.MenuNavigator
import dev.douglasneto.beatside.menu.MenuNode
import dev.douglasneto.beatside.menu.MenuState
import dev.douglasneto.beatside.menu.Route
import dev.douglasneto.beatside.menu.SettingKey
import dev.douglasneto.beatside.settings.WheelSettings
import dev.douglasneto.beatside.wheel.WheelButton
import dev.douglasneto.beatside.wheel.WheelEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Row labels (FR-028) and what is announced after an event (FR-031). Pure: the strings come from
 * a fake with the English values. Twin of MenuAccessibilityTests.swift.
 */
class MenuAccessibilityTest {

    private val titles = mapOf(
        "app_name" to "BeatSide", "menu_music" to "Music", "menu_playlists" to "Playlists",
        "menu_artists" to "Artists", "menu_extras" to "Extras", "menu_clock" to "Clock",
        "menu_settings" to "Settings", "menu_clicker" to "Clicker", "menu_haptics" to "Haptics",
    )
    private val strings = object : AccessibilityStrings {
        override fun title(titleKey: String) = titles.getValue(titleKey)
        override fun submenuRow(title: String) = "$title, submenu"
        override fun toggleRow(title: String, isOn: Boolean) = "$title, ${if (isOn) "On" else "Off"}"
        override fun screenAndRow(screen: String, row: String) = "$screen, $row"
    }

    private val settings = WheelSettings()

    // FR-028: row labels

    @Test fun `submenu row says its name and the submenu role`() =
        assertEquals("Music, submenu", MenuAccessibility.rowLabel(MenuNode.Submenu("menu_music", emptyList()), settings, strings))

    @Test fun `screen row says its name`() =
        assertEquals("Clock", MenuAccessibility.rowLabel(MenuNode.Screen("menu_clock", Route.Clock), settings, strings))

    @Test fun `toggle row says its name and its state`() {
        val clicker = MenuNode.Toggle("menu_clicker", SettingKey.Clicker)
        assertEquals("Clicker, On", MenuAccessibility.rowLabel(clicker, settings, strings))
        assertEquals("Clicker, Off", MenuAccessibility.rowLabel(clicker, settings.toggled(SettingKey.Clicker), strings))
    }

    // FR-031: announcements

    private val root = MenuState.root(BeatSideMenu.root)

    private fun announce(before: MenuState, event: WheelEvent, settingsBefore: WheelSettings = settings): String? {
        val result = MenuNavigator.reduce(before, event)
        val settingsAfter = (result.effect as? dev.douglasneto.beatside.menu.MenuEffect.ToggleSetting)
            ?.let { settingsBefore.toggled(it.key) } ?: settingsBefore
        return MenuAccessibility.announcement(before, result.state, settingsBefore, settingsAfter, strings)
    }

    private fun MenuState.on(event: WheelEvent) = MenuNavigator.reduce(this, event).state

    @Test fun `a tick that moves the selection announces the new row`() {
        assertEquals("Extras, submenu", announce(root, WheelEvent.Tick(+1)))
    }

    @Test fun `a tick clamped at the first row announces nothing`() {
        assertNull(announce(root, WheelEvent.Tick(-1)))
    }

    @Test fun `opening a submenu announces the screen and its highlighted row`() {
        assertEquals("Music, Playlists", announce(root, WheelEvent.Press(WheelButton.Center)))
    }

    @Test fun `opening a leaf announces the screen title`() {
        val inExtras = root.on(WheelEvent.Tick(+1)).on(WheelEvent.Press(WheelButton.Center))
        assertEquals("Clock", announce(inExtras, WheelEvent.Press(WheelButton.Center)))
    }

    @Test fun `going back announces the parent screen and its highlighted row`() {
        val inExtras = root.on(WheelEvent.Tick(+1)).on(WheelEvent.Press(WheelButton.Center))
        assertEquals("BeatSide, Extras, submenu", announce(inExtras, WheelEvent.Press(WheelButton.Menu)))
    }

    @Test fun `MENU at the root announces nothing`() {
        assertNull(announce(root, WheelEvent.Press(WheelButton.Menu)))
    }

    @Test fun `toggling a setting announces the row with its new state`() {
        val onClicker = root.on(WheelEvent.Tick(+2)).on(WheelEvent.Press(WheelButton.Center))
        assertEquals("Clicker, Off", announce(onClicker, WheelEvent.Press(WheelButton.Center)))
    }

    @Test fun `unhandled events announce nothing`() {
        assertNull(announce(root, WheelEvent.Press(WheelButton.PlayPause)))
        assertNull(announce(root, WheelEvent.LongPress(WheelButton.Center)))
    }
}
