import Testing
@testable import BeatSide

// Row labels (FR-028) and what is announced after an event (FR-031). Pure: the strings come from
// a fake with the English values. Twin of the Android MenuAccessibilityTest.kt.
struct MenuAccessibilityTests {

    struct FakeStrings: AccessibilityStrings {
        let titles = ["app_name": "BeatSide", "menu_music": "Music", "menu_playlists": "Playlists",
                      "menu_extras": "Extras", "menu_clock": "Clock", "menu_settings": "Settings",
                      "menu_clicker": "Clicker", "menu_haptics": "Haptics"]
        func title(_ titleKey: String) -> String { titles[titleKey]! }
        func submenuRow(_ title: String) -> String { "\(title), submenu" }
        func toggleRow(_ title: String, isOn: Bool) -> String { "\(title), \(isOn ? "On" : "Off")" }
        func screenAndRow(_ screen: String, _ row: String) -> String { "\(screen), \(row)" }
    }

    let strings = FakeStrings()
    let settings = WheelSettings()
    let root = MenuState.root(BeatSideMenu.root)

    // MARK: FR-028: row labels

    @Test func submenuRowSaysItsNameAndTheSubmenuRole() {
        #expect(MenuAccessibility.rowLabel(.submenu(titleKey: "menu_music", children: []), settings: settings, strings: strings) == "Music, submenu")
    }

    @Test func screenRowSaysItsName() {
        #expect(MenuAccessibility.rowLabel(.screen(titleKey: "menu_clock", route: .clock), settings: settings, strings: strings) == "Clock")
    }

    @Test func toggleRowSaysItsNameAndItsState() {
        let clicker = MenuNode.toggle(titleKey: "menu_clicker", key: .clicker)
        #expect(MenuAccessibility.rowLabel(clicker, settings: settings, strings: strings) == "Clicker, On")
        #expect(MenuAccessibility.rowLabel(clicker, settings: settings.toggled(.clicker), strings: strings) == "Clicker, Off")
    }

    // MARK: FR-031: announcements

    private func announce(_ before: MenuState, _ event: WheelEvent) -> String? {
        let result = MenuNavigator.reduce(before, event)
        var after = settings
        if case .toggleSetting(let key) = result.effect { after = settings.toggled(key) }
        return MenuAccessibility.announcement(before: before, after: result.state,
                                              settingsBefore: settings, settingsAfter: after, strings: strings)
    }

    private func on(_ state: MenuState, _ events: WheelEvent...) -> MenuState {
        events.reduce(state) { MenuNavigator.reduce($0, $1).state }
    }

    @Test func aTickThatMovesTheSelectionAnnouncesTheNewRow() {
        #expect(announce(root, .tick(+1)) == "Extras, submenu")
    }

    @Test func aTickClampedAtTheFirstRowAnnouncesNothing() {
        #expect(announce(root, .tick(-1)) == nil)
    }

    @Test func openingASubmenuAnnouncesTheScreenAndItsHighlightedRow() {
        #expect(announce(root, .press(.center)) == "Music, Playlists")
    }

    @Test func openingALeafAnnouncesTheScreenTitle() {
        #expect(announce(on(root, .tick(+1), .press(.center)), .press(.center)) == "Clock")
    }

    @Test func goingBackAnnouncesTheParentScreenAndItsHighlightedRow() {
        #expect(announce(on(root, .tick(+1), .press(.center)), .press(.menu)) == "BeatSide, Extras, submenu")
    }

    @Test func menuAtTheRootAnnouncesNothing() {
        #expect(announce(root, .press(.menu)) == nil)
    }

    @Test func togglingASettingAnnouncesTheRowWithItsNewState() {
        #expect(announce(on(root, .tick(+2), .press(.center)), .press(.center)) == "Clicker, Off")
    }

    @Test func unhandledEventsAnnounceNothing() {
        #expect(announce(root, .press(.playPause)) == nil)
        #expect(announce(root, .longPress(.center)) == nil)
    }
}
