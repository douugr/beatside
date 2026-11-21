package dev.douglasneto.beatside.menu

import dev.douglasneto.beatside.wheel.WheelButton
import dev.douglasneto.beatside.wheel.WheelEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuNavigatorTest {

    private val songs = MenuNode.Screen("Songs", Route.ComingSoon)
    private val music = MenuNode.Submenu("Music", listOf(songs))
    private val clicker = MenuNode.Toggle("Clicker", SettingKey.Clicker)
    private val root = MenuNode.Submenu("Root", listOf(music, clicker))

    private val center = WheelEvent.Press(WheelButton.Center)
    private val menu = WheelEvent.Press(WheelButton.Menu)

    private fun MenuState.on(event: WheelEvent) = MenuNavigator.reduce(this, event)

    @Test
    fun `ticks move the selection and clamp at both ends`() {
        var state = MenuState.root(root)
        state = state.on(WheelEvent.Tick(+1)).state
        assertEquals(1, state.current.selectedIndex)
        state = state.on(WheelEvent.Tick(+1)).state
        assertEquals("clamps at the last item", 1, state.current.selectedIndex)
        state = state.on(WheelEvent.Tick(-1)).state.on(WheelEvent.Tick(-1)).state
        assertEquals("clamps at the first item", 0, state.current.selectedIndex)
    }

    @Test
    fun `center enters a submenu and menu goes back`() {
        val inside = MenuState.root(root).on(center).state
        assertSame(music, inside.current.node)
        val back = inside.on(menu).state
        assertSame(root, back.current.node)
    }

    @Test
    fun `menu at the root does nothing`() {
        val state = MenuState.root(root)
        assertEquals(state, state.on(menu).state)
    }

    @Test
    fun `center on a toggle emits an effect without navigating`() {
        val onClicker = MenuState.root(root).on(WheelEvent.Tick(+1)).state
        val result = onClicker.on(center)
        assertEquals(onClicker, result.state)
        assertEquals(MenuEffect.ToggleSetting(SettingKey.Clicker), result.effect)
    }

    @Test
    fun `screens receive wheel events as unhandled`() {
        val onScreen = MenuState.root(root).on(center).state.on(center).state
        assertSame(songs, onScreen.current.node)
        val result = onScreen.on(WheelEvent.Tick(+1))
        assertTrue(result.effect is MenuEffect.Unhandled)
        assertEquals(onScreen, result.state)
    }

    @Test
    fun `play pause in a menu is unhandled`() {
        val result = MenuState.root(root).on(WheelEvent.Press(WheelButton.PlayPause))
        assertEquals(MenuEffect.Unhandled(WheelEvent.Press(WheelButton.PlayPause)), result.effect)
    }
}
