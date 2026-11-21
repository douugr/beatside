import Foundation

/// Localized pieces for screen-reader labels. The String Catalog in the app, a fake in tests.
protocol AccessibilityStrings {
    func title(_ titleKey: String) -> String
    func submenuRow(_ title: String) -> String
    func toggleRow(_ title: String, isOn: Bool) -> String
    func screenAndRow(_ screen: String, _ row: String) -> String
}

/// Screen-reader labels for menu rows (FR-028) and what to announce after an event (FR-031).
/// Pure apart from the `AccessibilityStrings` it is given. Twin of the Android MenuAccessibility.kt.
enum MenuAccessibility {

    /// "Music, submenu", "Clicker, On", or just the name for a leaf screen.
    static func rowLabel(_ node: MenuNode, settings: WheelSettings, strings: AccessibilityStrings) -> String {
        let title = strings.title(node.titleKey)
        switch node {
        case .submenu: return strings.submenuRow(title)
        case .screen: return title
        case .toggle(_, let key): return strings.toggleRow(title, isOn: settings.isOn(key))
        }
    }

    /// What to say after an event took the app from `before` to `after`, or nil if nothing a
    /// listener needs changed: a new screen is announced by its title (plus its highlighted row for
    /// a submenu), a new selection by its row label, a toggled setting by its row with the new state.
    static func announcement(before: MenuState, after: MenuState,
                             settingsBefore: WheelSettings, settingsAfter: WheelSettings,
                             strings: AccessibilityStrings) -> String? {
        let frame = after.current
        let screenChanged = after.stack.count != before.stack.count || frame.node != before.current.node
        var selectedRow: String?
        if case .submenu(_, let children) = frame.node, children.indices.contains(frame.selectedIndex) {
            selectedRow = rowLabel(children[frame.selectedIndex], settings: settingsAfter, strings: strings)
        }
        if screenChanged {
            let title = strings.title(frame.node.titleKey)
            return selectedRow.map { strings.screenAndRow(title, $0) } ?? title
        }
        if frame.selectedIndex != before.current.selectedIndex { return selectedRow }
        if settingsAfter != settingsBefore { return selectedRow }
        return nil
    }
}

/// `AccessibilityStrings` from Localizable.xcstrings, so labels follow the device language.
struct CatalogAccessibilityStrings: AccessibilityStrings {
    func title(_ titleKey: String) -> String {
        String(localized: String.LocalizationValue(titleKey))
    }

    func submenuRow(_ title: String) -> String {
        String(format: String(localized: "a11y_row_submenu"), title)
    }

    func toggleRow(_ title: String, isOn: Bool) -> String {
        let state = isOn ? String(localized: "setting_on") : String(localized: "setting_off")
        return String(format: String(localized: "a11y_row_toggle"), title, state)
    }

    func screenAndRow(_ screen: String, _ row: String) -> String {
        String(format: String(localized: "a11y_screen_and_row"), screen, row)
    }
}
