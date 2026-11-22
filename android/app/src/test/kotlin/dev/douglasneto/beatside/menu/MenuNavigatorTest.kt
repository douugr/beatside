package dev.douglasneto.beatside.menu

import dev.douglasneto.beatside.wheel.WheelButton
import dev.douglasneto.beatside.wheel.WheelEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MenuNavigator tests — one or more cases per row of the MenuNavigator behavioral table in
 * specs/001-click-wheel-nav/data-model.md. The iOS suite (MenuNavigatorTests.swift) mirrors
 * these cases.
 */
class MenuNavigatorTest {

    private val songs = MenuNode.Screen("menu_songs", Route.ComingSoon)
    private val albums = MenuNode.Screen("menu_albums", Route.ComingSoon)
    private val music = MenuNode.Submenu("menu_music", listOf(songs, albums))
    private val clicker = MenuNode.Toggle("menu_clicker", SettingKey.Clicker)
    private val about = MenuNode.Screen("menu_about", Route.About)
    private val root = MenuNode.Submenu("app_name", listOf(music, clicker, about))

    private val center = WheelEvent.Press(WheelButton.Center)
    private val menu = WheelEvent.Press(WheelButton.Menu)

    private fun MenuState.on(event: WheelEvent) = MenuNavigator.reduce(this, event)

    // Tick(d) on a Submenu: selectedIndex clamped to [0, children.count - 1]

    @Test fun `tick moves the selection down and up`() {
        val down = MenuState.root(root).on(WheelEvent.Tick(+1))
        assertEquals(1, down.state.current.selectedIndex)
        assertNull(down.effect)
        val up = down.state.on(WheelEvent.Tick(-1))
        assertEquals(0, up.state.current.selectedIndex)
        assertNull(up.effect)
    }

    @Test fun `tick clamps at the last row`() {
        var state = MenuState.root(root)
        repeat(5) { state = state.on(WheelEvent.Tick(+1)).state }
        assertEquals(2, state.current.selectedIndex)
    }

    @Test fun `tick clamps at the first row`() {
        val result = MenuState.root(root).on(WheelEvent.Tick(-1))
        assertEquals(0, result.state.current.selectedIndex)
        assertNull(result.effect)
    }

    // Tick(d) on a Screen: unchanged, Unhandled(Tick)

    @Test fun `tick on a screen is unhandled`() {
        val onScreen = MenuState.root(root).on(WheelEvent.Tick(+1)).state
            .on(WheelEvent.Tick(+1)).state.on(center).state
        assertSame(about, onScreen.current.node)
        val result = onScreen.on(WheelEvent.Tick(+1))
        assertEquals(onScreen, result.state)
        assertEquals(MenuEffect.Unhandled(WheelEvent.Tick(+1)), result.effect)
    }

    @Test fun `center on a screen is unhandled`() {
        val onScreen = MenuState.root(root).on(center).state.on(center).state
        assertSame(songs, onScreen.current.node)
        val result = onScreen.on(center)
        assertEquals(onScreen, result.state)
        assertEquals(MenuEffect.Unhandled(center), result.effect)
    }

    // Press(Center) on a Submenu: child Submenu or Screen is pushed

    @Test fun `center on a submenu row pushes it with the first row selected`() {
        val result = MenuState.root(root).on(center)
        assertEquals(2, result.state.stack.size)
        assertSame(music, result.state.current.node)
        assertEquals(0, result.state.current.selectedIndex)
        assertNull(result.effect)
    }

    @Test fun `center on a screen row pushes the screen`() {
        val result = MenuState.root(root).on(center).state.on(WheelEvent.Tick(+1)).state.on(center)
        assertEquals(3, result.state.stack.size)
        assertSame(albums, result.state.current.node)
        assertNull(result.effect)
    }

    // Press(Center) on a Toggle row: unchanged, ToggleSetting(key)

    @Test fun `center on a toggle row emits ToggleSetting without navigating`() {
        val onClicker = MenuState.root(root).on(WheelEvent.Tick(+1)).state
        val result = onClicker.on(center)
        assertEquals(onClicker, result.state)
        assertEquals(MenuEffect.ToggleSetting(SettingKey.Clicker), result.effect)
    }

    // Press(Menu) with canGoBack: pop

    @Test fun `menu pops back to the parent with its row still highlighted`() {
        val inSubmenu = MenuState.root(root).on(center).state.on(WheelEvent.Tick(+1)).state
        val onScreen = inSubmenu.on(center).state
        val result = onScreen.on(menu)
        assertEquals(inSubmenu, result.state)
        assertEquals(1, result.state.current.selectedIndex)
        assertNull(result.effect)
    }

    @Test fun `menu from a first-level submenu returns to the root`() {
        val result = MenuState.root(root).on(center).state.on(menu)
        assertEquals(MenuState.root(root), result.state)
        assertNull(result.effect)
    }

    // Press(Menu) at root: unchanged, no effect

    @Test fun `menu at the root does nothing`() {
        val state = MenuState.root(root).on(WheelEvent.Tick(+1)).state
        val result = state.on(menu)
        assertEquals(state, result.state)
        assertEquals(false, result.state.canGoBack)
        assertNull(result.effect)
    }

    // Press(Next / Previous / PlayPause): unchanged, Unhandled(press)

    @Test fun `next previous and play pause are unhandled in a submenu`() {
        val state = MenuState.root(root)
        for (button in listOf(WheelButton.Next, WheelButton.Previous, WheelButton.PlayPause)) {
            val press = WheelEvent.Press(button)
            val result = state.on(press)
            assertEquals(state, result.state)
            assertEquals(MenuEffect.Unhandled(press), result.effect)
        }
    }

    @Test fun `next previous and play pause are unhandled on a screen`() {
        val onScreen = MenuState.root(root).on(center).state.on(center).state
        for (button in listOf(WheelButton.Next, WheelButton.Previous, WheelButton.PlayPause)) {
            val press = WheelEvent.Press(button)
            val result = onScreen.on(press)
            assertEquals(onScreen, result.state)
            assertEquals(MenuEffect.Unhandled(press), result.effect)
        }
    }

    // LongPress(Center): unchanged, Unhandled(longPress)

    @Test fun `long press is unhandled in a submenu and on a screen`() {
        val longPress = WheelEvent.LongPress(WheelButton.Center)
        val inMenu = MenuState.root(root)
        val menuResult = inMenu.on(longPress)
        assertEquals(inMenu, menuResult.state)
        assertEquals(MenuEffect.Unhandled(longPress), menuResult.effect)

        val onScreen = inMenu.on(center).state.on(center).state
        val screenResult = onScreen.on(longPress)
        assertEquals(onScreen, screenResult.state)
        assertEquals(MenuEffect.Unhandled(longPress), screenResult.effect)
    }

    // MenuState derived properties

    @Test fun `root state has one frame and cannot go back`() {
        val state = MenuState.root(root)
        assertEquals(1, state.stack.size)
        assertSame(root, state.current.node)
        assertEquals(0, state.current.selectedIndex)
        assertEquals(false, state.canGoBack)
    }

    // BeatSideMenu static tree (data-model.md)

    @Test fun `BeatSideMenu tree matches the data model`() {
        val r = BeatSideMenu.root
        assertEquals("app_name", r.titleKey)
        assertEquals(
            listOf("menu_music", "menu_extras", "menu_settings", "menu_shuffle_songs", "menu_now_playing"),
            r.children.map { it.titleKey },
        )
        val music = r.children[0] as MenuNode.Submenu
        assertEquals(
            listOf("menu_playlists", "menu_artists", "menu_albums", "menu_songs", "menu_genres"),
            music.children.map { it.titleKey },
        )
        assertTrue(music.children.all { it == MenuNode.Screen(it.titleKey, Route.ComingSoon) })
        val extras = r.children[1] as MenuNode.Submenu
        assertEquals(
            listOf(
                MenuNode.Screen("menu_clock", Route.Clock),
                MenuNode.Screen("menu_stopwatch", Route.ComingSoon),
                MenuNode.Screen("menu_games", Route.ComingSoon),
            ),
            extras.children,
        )
        val settings = r.children[2] as MenuNode.Submenu
        assertEquals(
            listOf(
                MenuNode.Toggle("menu_clicker", SettingKey.Clicker),
                MenuNode.Toggle("menu_haptics", SettingKey.Haptics),
                MenuNode.Screen("menu_about", Route.About),
            ),
            settings.children,
        )
        assertEquals(MenuNode.Screen("menu_shuffle_songs", Route.ShuffleSongs), r.children[3])
        assertEquals(MenuNode.Screen("menu_now_playing", Route.NowPlaying), r.children[4])
    }
}
