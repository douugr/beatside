/// Wheel feedback settings. Session-only: not persisted across launches (FR-030).
/// Toggling is the only mutation; the change applies to the next wheel event (FR-021).
struct WheelSettings: Equatable, Sendable {
    var clickerEnabled: Bool = true
    var hapticsEnabled: Bool = true

    func isOn(_ key: SettingKey) -> Bool {
        switch key {
        case .clicker: clickerEnabled
        case .haptics: hapticsEnabled
        }
    }

    func toggled(_ key: SettingKey) -> WheelSettings {
        var copy = self
        switch key {
        case .clicker: copy.clickerEnabled.toggle()
        case .haptics: copy.hapticsEnabled.toggle()
        }
        return copy
    }
}
