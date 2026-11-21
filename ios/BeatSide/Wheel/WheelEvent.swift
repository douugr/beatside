/// What the wheel reports to screens. Screens never see raw touches.
enum WheelEvent: Equatable, Hashable, Sendable {
    /// One detent. +1 is clockwise (down a list), -1 counter-clockwise.
    case tick(Int)
    case press(WheelButton)
    /// Only ever emitted for `.center` (FR-010).
    case longPress(WheelButton)
}
