import Observation
import UIKit

/// Speaks a short text through the active screen reader. A fake records it in tests.
@MainActor
protocol Announcer: AnyObject {
    func announce(_ text: String)
}

/// Posts a VoiceOver announcement; does nothing when VoiceOver is off.
@MainActor
final class VoiceOverAnnouncer: Announcer {
    func announce(_ text: String) {
        UIAccessibility.post(notification: .announcement, argument: text)
    }
}

/// App state and event routing: every wheel event goes through `MenuNavigator`, toggles flip
/// `WheelSettings`, and every change to the selection, the screen or a setting is announced to
/// screen readers (FR-031).
@MainActor
@Observable
final class BeatSideModel {
    private(set) var menuState = MenuState.root(BeatSideMenu.root)
    private(set) var settings = WheelSettings()

    @ObservationIgnored private let announcer: Announcer
    @ObservationIgnored private let strings: AccessibilityStrings

    init(announcer: Announcer, strings: AccessibilityStrings = CatalogAccessibilityStrings()) {
        self.announcer = announcer
        self.strings = strings
    }

    func handle(_ event: WheelEvent) {
        let stateBefore = menuState
        let settingsBefore = settings
        let result = MenuNavigator.reduce(menuState, event)
        menuState = result.state
        switch result.effect {
        case .toggleSetting(let key):
            settings = settings.toggled(key)
        case .unhandled, nil:
            // Next / Previous / Play-Pause and long press have no effect in this slice (FR-011).
            break
        }
        if let text = MenuAccessibility.announcement(before: stateBefore, after: menuState,
                                                     settingsBefore: settingsBefore, settingsAfter: settings,
                                                     strings: strings) {
            announcer.announce(text)
        }
    }
}
