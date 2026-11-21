import Foundation

/// The five physical buttons of the wheel. See docs/WHEEL.md.
enum WheelButton: Equatable, Sendable {
    case center, menu, next, previous, playPause
}

/// What the wheel reports to screens. Screens never see raw touches.
enum WheelEvent: Equatable, Sendable {
    /// One detent. `+1` is clockwise (down a list), `-1` counter-clockwise.
    case tick(Int)
    case press(WheelButton)
    case longPress(WheelButton)
}

enum WheelSensitivity: Double, Sendable {
    case low = 20
    case normal = 15
    case high = 10

    var degreesPerTick: Double { rawValue }
}

/// Turns touch samples into `WheelEvent`s, following docs/WHEEL.md.
///
/// Pure logic with no UI framework imports, so it runs in plain unit tests.
/// Coordinates are relative to the wheel's top-left corner, in points, with y growing down.
///
/// Call order for one gesture: `down`, any number of `move`, optionally
/// `longPressTimeout` (after `longPressDelay` if the finger is still down), then `up`.
struct WheelTracker {
    static let centerRatio = 0.36
    static let deadZoneRatio = 0.20
    static let longPressDelay: Duration = .milliseconds(500)

    var sensitivity: WheelSensitivity
    let tapSlop: Double

    private var active = false
    private var radius = 0.0
    private var start = CGPoint.zero
    private var startButton = WheelButton.center
    private var startedInCenter = false
    private var lastAngle: Double?
    private var accumulator = 0.0
    private var emittedTick = false
    private var movedBeyondSlop = false
    private var longPressFired = false

    init(sensitivity: WheelSensitivity = .normal, tapSlop: Double = 8) {
        self.sensitivity = sensitivity
        self.tapSlop = tapSlop
    }

    var isActive: Bool { active }

    /// Starts a gesture. Returns false (and ignores the gesture) when the touch is outside the wheel.
    mutating func down(at point: CGPoint, diameter: Double) -> Bool {
        radius = diameter / 2
        let dx = point.x - radius, dy = point.y - radius
        guard let button = Self.button(dx: dx, dy: dy, radius: radius) else { return false }
        active = true
        start = point
        startButton = button
        startedInCenter = button == .center
        lastAngle = startedInCenter ? nil : Self.angle(dx: dx, dy: dy)
        accumulator = 0
        emittedTick = false
        movedBeyondSlop = false
        longPressFired = false
        return true
    }

    mutating func move(to point: CGPoint) -> [WheelEvent] {
        guard active else { return [] }
        if hypot(point.x - start.x, point.y - start.y) > tapSlop { movedBeyondSlop = true }
        if startedInCenter || longPressFired { return [] }

        let dx = point.x - radius, dy = point.y - radius
        if hypot(dx, dy) < Self.deadZoneRatio * radius {
            // The angle is unstable near the center: drop the anchor and wait.
            lastAngle = nil
            return []
        }

        let angle = Self.angle(dx: dx, dy: dy)
        defer { lastAngle = angle }
        guard let previous = lastAngle else { return [] } // Re-anchor after the dead zone.

        accumulator += Self.normalize(angle - previous)
        let step = sensitivity.degreesPerTick
        var events: [WheelEvent] = []
        while accumulator >= step {
            events.append(.tick(+1))
            accumulator -= step
        }
        while accumulator <= -step {
            events.append(.tick(-1))
            accumulator += step
        }
        if !events.isEmpty { emittedTick = true }
        return events
    }

    /// Call once, `longPressDelay` after `down`, if the gesture is still active.
    mutating func longPressTimeout() -> [WheelEvent] {
        guard active, !longPressFired, !emittedTick, !movedBeyondSlop else { return [] }
        longPressFired = true
        return [.longPress(startButton)]
    }

    mutating func up() -> [WheelEvent] {
        guard active else { return [] }
        active = false
        if longPressFired || emittedTick || movedBeyondSlop { return [] }
        return [.press(startButton)]
    }

    mutating func cancel() {
        active = false
    }

    // MARK: - Geometry

    /// Maps any angle to (-180°, 180°].
    static func normalize(_ degrees: Double) -> Double {
        var d = degrees.truncatingRemainder(dividingBy: 360)
        if d > 180 { d -= 360 }
        if d <= -180 { d += 360 }
        return d
    }

    /// Angle in degrees, screen coordinates: -90° is up, larger is clockwise.
    static func angle(dx: Double, dy: Double) -> Double {
        atan2(dy, dx) * 180 / .pi
    }

    /// Which button sits at an offset from the wheel's center, or nil outside the wheel.
    static func button(dx: Double, dy: Double, radius: Double) -> WheelButton? {
        let distance = hypot(dx, dy)
        if distance > radius { return nil }
        if distance < centerRatio * radius { return .center }
        switch angle(dx: dx, dy: dy) {
        case -135 ..< -45: return .menu
        case -45 ..< 45: return .next
        case 45 ..< 135: return .playPause
        default: return .previous
        }
    }
}
