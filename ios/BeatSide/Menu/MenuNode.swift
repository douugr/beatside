/// One entry in the menu tree. The whole app is a tree of these.
///
/// `titleKey` is a locale-neutral string key (e.g. "menu_music") resolved by the UI layer with
/// `String(localized:)`, so this core stays free of UI imports (FR-029).
indirect enum MenuNode: Equatable, Sendable {
    /// A list of more entries.
    case submenu(titleKey: String, children: [MenuNode])
    /// A leaf screen, drawn by the UI layer according to `route`.
    case screen(titleKey: String, route: Route)
    /// An on/off setting, shown as "On"/"Off" on its row. Never pushed onto the stack.
    case toggle(titleKey: String, key: SettingKey)

    var titleKey: String {
        switch self {
        case .submenu(let titleKey, _), .screen(let titleKey, _), .toggle(let titleKey, _):
            return titleKey
        }
    }
}
