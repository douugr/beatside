import Testing
@testable import BeatSide

// MenuNavigator tests — one or more cases per row of the MenuNavigator behavioral table in
// specs/001-click-wheel-nav/data-model.md. Mirrors the Android MenuNavigatorTest.kt case by case.
struct MenuNavigatorTests {

    let songs = MenuNode.screen(titleKey: "menu_songs", route: .comingSoon)
    let albums = MenuNode.screen(titleKey: "menu_albums", route: .comingSoon)
    let clicker = MenuNode.toggle(titleKey: "menu_clicker", key: .clicker)
    let about = MenuNode.screen(titleKey: "menu_about", route: .about)
    var music: MenuNode { .submenu(titleKey: "menu_music", children: [songs, albums]) }
    var root: MenuNode { .submenu(titleKey: "beatside", children: [music, clicker, about]) }

    let center = WheelEvent.press(.center)
    let menu = WheelEvent.press(.menu)

    func on(_ state: MenuState, _ event: WheelEvent) -> MenuResult {
        MenuNavigator.reduce(state, event)
    }

    // MARK: Tick(d) on a Submenu: selectedIndex clamped to [0, children.count - 1]

    @Test func tickMovesTheSelectionDownAndUp() {
        let down = on(.root(root), .tick(+1))
        #expect(down.state.current.selectedIndex == 1)
        #expect(down.effect == nil)
        let up = on(down.state, .tick(-1))
        #expect(up.state.current.selectedIndex == 0)
        #expect(up.effect == nil)
    }

    @Test func tickClampsAtTheLastRow() {
        var state = MenuState.root(root)
        for _ in 0..<5 { state = on(state, .tick(+1)).state }
        #expect(state.current.selectedIndex == 2)
    }

    @Test func tickClampsAtTheFirstRow() {
        let result = on(.root(root), .tick(-1))
        #expect(result.state.current.selectedIndex == 0)
        #expect(result.effect == nil)
    }

    // MARK: Tick(d) on a Screen: unchanged, Unhandled(Tick)

    @Test func tickOnAScreenIsUnhandled() {
        let onScreen = on(on(on(.root(root), .tick(+1)).state, .tick(+1)).state, center).state
        #expect(onScreen.current.node == about)
        let result = on(onScreen, .tick(+1))
        #expect(result.state == onScreen)
        #expect(result.effect == .unhandled(.tick(+1)))
    }

    @Test func centerOnAScreenIsUnhandled() {
        let onScreen = on(on(.root(root), center).state, center).state
        #expect(onScreen.current.node == songs)
        let result = on(onScreen, center)
        #expect(result.state == onScreen)
        #expect(result.effect == .unhandled(center))
    }

    // MARK: Press(Center) on a Submenu: child Submenu or Screen is pushed

    @Test func centerOnASubmenuRowPushesItWithTheFirstRowSelected() {
        let result = on(.root(root), center)
        #expect(result.state.stack.count == 2)
        #expect(result.state.current.node == music)
        #expect(result.state.current.selectedIndex == 0)
        #expect(result.effect == nil)
    }

    @Test func centerOnAScreenRowPushesTheScreen() {
        let result = on(on(on(.root(root), center).state, .tick(+1)).state, center)
        #expect(result.state.stack.count == 3)
        #expect(result.state.current.node == albums)
        #expect(result.effect == nil)
    }

    // MARK: Press(Center) on a Toggle row: unchanged, ToggleSetting(key)

    @Test func centerOnAToggleRowEmitsToggleSettingWithoutNavigating() {
        let onClicker = on(.root(root), .tick(+1)).state
        let result = on(onClicker, center)
        #expect(result.state == onClicker)
        #expect(result.effect == .toggleSetting(.clicker))
    }

    // MARK: Press(Menu) with canGoBack: pop

    @Test func menuPopsBackToTheParentWithItsRowStillHighlighted() {
        let inSubmenu = on(on(.root(root), center).state, .tick(+1)).state
        let onScreen = on(inSubmenu, center).state
        let result = on(onScreen, menu)
        #expect(result.state == inSubmenu)
        #expect(result.state.current.selectedIndex == 1)
        #expect(result.effect == nil)
    }

    @Test func menuFromAFirstLevelSubmenuReturnsToTheRoot() {
        let result = on(on(.root(root), center).state, menu)
        #expect(result.state == MenuState.root(root))
        #expect(result.effect == nil)
    }

    // MARK: Press(Menu) at root: unchanged, no effect

    @Test func menuAtTheRootDoesNothing() {
        let state = on(.root(root), .tick(+1)).state
        let result = on(state, menu)
        #expect(result.state == state)
        #expect(!result.state.canGoBack)
        #expect(result.effect == nil)
    }

    // MARK: Press(Next / Previous / PlayPause): unchanged, Unhandled(press)

    @Test func nextPreviousAndPlayPauseAreUnhandledInASubmenu() {
        let state = MenuState.root(root)
        for button in [WheelButton.next, .previous, .playPause] {
            let result = on(state, .press(button))
            #expect(result.state == state)
            #expect(result.effect == .unhandled(.press(button)))
        }
    }

    @Test func nextPreviousAndPlayPauseAreUnhandledOnAScreen() {
        let onScreen = on(on(.root(root), center).state, center).state
        for button in [WheelButton.next, .previous, .playPause] {
            let result = on(onScreen, .press(button))
            #expect(result.state == onScreen)
            #expect(result.effect == .unhandled(.press(button)))
        }
    }

    // MARK: LongPress(Center): unchanged, Unhandled(longPress)

    @Test func longPressIsUnhandledInASubmenuAndOnAScreen() {
        let longPress = WheelEvent.longPress(.center)
        let inMenu = MenuState.root(root)
        let menuResult = on(inMenu, longPress)
        #expect(menuResult.state == inMenu)
        #expect(menuResult.effect == .unhandled(longPress))

        let onScreen = on(on(inMenu, center).state, center).state
        let screenResult = on(onScreen, longPress)
        #expect(screenResult.state == onScreen)
        #expect(screenResult.effect == .unhandled(longPress))
    }

    // MARK: MenuState derived properties

    @Test func rootStateHasOneFrameAndCannotGoBack() {
        let state = MenuState.root(root)
        #expect(state.stack.count == 1)
        #expect(state.current.node == root)
        #expect(state.current.selectedIndex == 0)
        #expect(!state.canGoBack)
    }

    // MARK: BeatSideMenu static tree (data-model.md)

    @Test func beatSideMenuTreeMatchesTheDataModel() throws {
        let r = BeatSideMenu.root
        #expect(r.titleKey == "beatside")
        guard case .submenu(_, let top) = r else { Issue.record("root is not a submenu"); return }
        #expect(top.map(\.titleKey) == ["menu_music", "menu_extras", "menu_settings", "menu_shuffle_songs", "menu_now_playing"])

        guard case .submenu(_, let musicChildren) = top[0] else { Issue.record("music is not a submenu"); return }
        #expect(musicChildren == ["menu_playlists", "menu_artists", "menu_albums", "menu_songs", "menu_genres"]
            .map { MenuNode.screen(titleKey: $0, route: .comingSoon) })

        guard case .submenu(_, let extras) = top[1] else { Issue.record("extras is not a submenu"); return }
        #expect(extras == [
            .screen(titleKey: "menu_clock", route: .clock),
            .screen(titleKey: "menu_stopwatch", route: .comingSoon),
            .screen(titleKey: "menu_games", route: .comingSoon),
        ])

        guard case .submenu(_, let settings) = top[2] else { Issue.record("settings is not a submenu"); return }
        #expect(settings == [
            .toggle(titleKey: "menu_clicker", key: .clicker),
            .toggle(titleKey: "menu_haptics", key: .haptics),
            .screen(titleKey: "menu_about", route: .about),
        ])

        #expect(top[3] == .screen(titleKey: "menu_shuffle_songs", route: .shuffleSongs))
        #expect(top[4] == .screen(titleKey: "menu_now_playing", route: .nowPlaying))
    }
}
