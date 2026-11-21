/// A side effect the navigator cannot handle on its own.
enum MenuEffect: Equatable, Sendable {
    /// Flip a setting; handled by the UI layer.
    case toggleSetting(SettingKey)
    /// The event has no meaning to the menus here (e.g. Play/Pause); route it elsewhere.
    case unhandled(WheelEvent)
}
