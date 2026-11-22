import Foundation

/// Turns touch samples into `WheelEvent`s.
///
/// Pure logic — no UIKit/SwiftUI imports — so it runs in plain unit tests.
/// Coordinates are relative to the wheel's top-left corner, in points, with y growing down.
///
/// Call order for one gesture: `down`, any number of `move`, optionally
/// `longPressTimeout` (after `longPressMillis`), then `up` or `cancel`.
struct WheelTracker {
    static let centerRatio: Double = 0.36
    static let deadZoneRatio: Double = 0.20
    static let stepDegrees: Double = 15
    static let tapSlopPoints: Double = 8
    static let longPressMillis = 500

    private let tapSlop: Double

    private var active = false
    private var radius: Double = 0
    private var start = CGPoint.zero
    private var startButton = WheelButton.center
    private var startedInCenter = false
    private var lastAngle: Double?
    private var accumulator: Double = 0
    private var emittedTick = false
    private var movedBeyondSlop = false
    private var longPressFired = false

    init(tapSlop: Double = WheelTracker.tapSlopPoints) {
        self.tapSlop = tapSlop
    }

    /// Starts a gesture. `diameter` is the wheel's size in points.
    /// Returns false (and ignores the gesture) when the touch is outside the wheel.
    @discardableResult
    mutating func down(at point: CGPoint, diameter: Double) -> Bool {
        radius = diameter / 2
        let dx = Double(point.x) - radius
        let dy = Double(point.y) - radius
        guard let button = Self.buttonAt(dx: dx, dy: dy, radius: radius) else { return false }
        active = true
        start = point
        startButton = button
        startedInCenter = button == .center
        lastAngle = startedInCenter ? nil : Self.angleOf(dx: dx, dy: dy)
        accumulator = 0
        emittedTick = false
        movedBeyondSlop = false
        longPressFired = false
        return true
    }

    mutating func move(to point: CGPoint) -> [WheelEvent] {
        guard active else { return [] }
        if hypot(Double(point.x - start.x), Double(point.y - start.y)) > tapSlop {
            movedBeyondSlop = true
        }
        if startedInCenter || longPressFired { return [] }

        let dx = Double(point.x) - radius
        let dy = Double(point.y) - radius
        if hypot(dx, dy) < Self.deadZoneRatio * radius {
            lastAngle = nil
            return []
        }

        let angle = Self.angleOf(dx: dx, dy: dy)
        let previous = lastAngle
        lastAngle = angle
        guard let previous else { return [] }

        accumulator += Self.normalize(angle - previous)
        var events: [WheelEvent] = []
        while accumulator >= Self.stepDegrees {
            events.append(.tick(+1))
            accumulator -= Self.stepDegrees
        }
        while accumulator <= -Self.stepDegrees {
            events.append(.tick(-1))
            accumulator += Self.stepDegrees
        }
        if !events.isEmpty { emittedTick = true }
        return events
    }

    /// Call once, `longPressMillis` after `down`, if the gesture is still active.
    mutating func longPressTimeout() -> [WheelEvent] {
        guard active, !longPressFired, !emittedTick, !movedBeyondSlop else { return [] }
        // FR-010: long press is only valid for the Center button.
        guard startButton == .center else { return [] }
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

    /// Maps any angle to (-180°, 180°].
    static func normalize(_ degrees: Double) -> Double {
        var d = degrees.truncatingRemainder(dividingBy: 360)
        if d > 180 { d -= 360 }
        if d <= -180 { d += 360 }
        return d
    }

    /// Angle in degrees, screen coordinates: -90° is up, larger is clockwise.
    static func angleOf(dx: Double, dy: Double) -> Double {
        atan2(dy, dx) * 180 / .pi
    }

    /// Which button sits at an offset from the wheel's center, or nil outside the wheel.
    static func buttonAt(dx: Double, dy: Double, radius: Double) -> WheelButton? {
        let distance = hypot(dx, dy)
        if distance > radius { return nil }
        if distance < centerRatio * radius { return .center }
        let angle = angleOf(dx: dx, dy: dy)
        switch angle {
        case -135 ..< -45: return .menu
        case -45 ..< 45: return .next
        case 45 ..< 135: return .playPause
        default: return .previous
        }
    }
}
