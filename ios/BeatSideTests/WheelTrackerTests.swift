import Testing
import Foundation
@testable import BeatSide

// WheelTracker contract tests — implements ALL 40 cases from
// specs/001-click-wheel-nav/contracts/wheel-behavior.md verbatim.
//
// diameter = 200, radius = 100, ringRadius = 80
// pointAt(degrees, r=80) = (cx + r*cos(deg), cy + r*sin(deg)) with cx=cy=100
struct WheelTrackerTests {

    let diameter: Double = 200
    let radius: Double = 100
    let ringRadius: Double = 80
    var cx: Double { radius }
    var cy: Double { radius }
    let tapSlop: Double = 8

    func makeTracker() -> WheelTracker {
        WheelTracker(tapSlop: tapSlop)
    }

    /// Returns (cx + r*cos(deg°), cy + r*sin(deg°))
    func pointAt(_ degrees: Double, r: Double? = nil) -> CGPoint {
        let actualR = r ?? ringRadius
        let rad = degrees * .pi / 180
        return CGPoint(x: cx + actualR * cos(rad), y: cy + actualR * sin(rad))
    }

    func dragSum(_ tracker: inout WheelTracker, from startAngle: Double, by totalDegrees: Double) -> Int {
        let start = pointAt(startAngle)
        _ = tracker.down(at: start, diameter: diameter)
        let steps = max(1, Int(abs(totalDegrees)))
        let stepDir: Double = totalDegrees > 0 ? 1 : -1
        var ticks = 0
        for i in 1...steps {
            let pt = pointAt(startAngle + stepDir * Double(i))
            let events = tracker.move(to: pt)
            ticks += events.compactMap { if case .tick(let d) = $0 { return d } else { return nil } }.reduce(0, +)
        }
        return ticks
    }

    // MARK: - Group 1: normalize

    @Test func N1_normalize_190_is_minus170() {
        #expect(abs(WheelTracker.normalize(190) - (-170)) < 0.001)
    }

    @Test func N2_normalize_minus190_is_170() {
        #expect(abs(WheelTracker.normalize(-190) - 170) < 0.001)
    }

    @Test func N3_normalize_180_is_180() {
        #expect(abs(WheelTracker.normalize(180) - 180) < 0.001)
    }

    @Test func N4_normalize_minus180_is_180() {
        #expect(abs(WheelTracker.normalize(-180) - 180) < 0.001)
    }

    @Test func N5_normalize_540_is_180() {
        #expect(abs(WheelTracker.normalize(540) - 180) < 0.001)
    }

    @Test func N6_normalize_0_is_0() {
        #expect(abs(WheelTracker.normalize(0) - 0) < 0.001)
    }

    @Test func N7_normalize_minus360_is_0() {
        #expect(abs(WheelTracker.normalize(-360) - 0) < 0.001)
    }

    // MARK: - Group 2: Detent counting

    @Test func D1_50deg_CW_gives_plus3() {
        var t = makeTracker()
        #expect(dragSum(&t, from: -90, by: +50) == 3)
    }

    @Test func D2_50deg_CCW_gives_minus3() {
        var t = makeTracker()
        #expect(dragSum(&t, from: -90, by: -50) == -3)
    }

    @Test func D3_20deg_CW_gives_plus1() {
        var t = makeTracker()
        #expect(dragSum(&t, from: -90, by: +20) == 1)
    }

    @Test func D4_20deg_CCW_gives_minus1() {
        var t = makeTracker()
        #expect(dragSum(&t, from: -90, by: -20) == -1)
    }

    // MARK: - Group 3: Boundary crossing

    @Test func B1_170_to_190_gives_plus1() {
        var t = makeTracker()
        #expect(dragSum(&t, from: 170, by: +20) == 1)
    }

    @Test func B2_minus170_to_minus190_gives_minus1() {
        var t = makeTracker()
        #expect(dragSum(&t, from: -170, by: -20) == -1)
    }

    // MARK: - Group 4: Jitter absorption

    @Test func J1_oscillation_no_ticks_up_empty() {
        var t = makeTracker()
        let start = pointAt(0)
        _ = t.down(at: start, diameter: diameter)
        var anyTick = false
        for _ in 0..<5 {
            let p1 = pointAt(10)
            let p2 = pointAt(-10)
            if t.move(to: p1).contains(where: { if case .tick = $0 { return true }; return false }) { anyTick = true }
            if t.move(to: p2).contains(where: { if case .tick = $0 { return true }; return false }) { anyTick = true }
        }
        #expect(!anyTick, "No ticks should be emitted during jitter")
        // Movement of ~14pt exceeds 8pt tap slop — up returns empty
        let upEvents = t.up()
        #expect(upEvents.isEmpty, "up() should return empty after slop exceeded")
    }

    // MARK: - Group 5: Dead zone

    @Test func DZ1_move_into_dead_zone_returns_empty() {
        var t = makeTracker()
        let start = pointAt(-90)
        _ = t.down(at: start, diameter: diameter)
        let dzPoint = CGPoint(x: cx + 15, y: cy)
        let events = t.move(to: dzPoint)
        #expect(events.isEmpty)
    }

    @Test func DZ2_after_dead_zone_re_anchor_at_180_returns_empty() {
        var t = makeTracker()
        _ = t.down(at: pointAt(-90), diameter: diameter)
        _ = t.move(to: CGPoint(x: cx + 15, y: cy))
        let reAnchor = pointAt(180)
        let moveEvents = t.move(to: reAnchor)
        #expect(moveEvents.isEmpty)
    }

    @Test func DZ3_after_re_anchor_move_to_200_gives_plus1() {
        var t = makeTracker()
        _ = t.down(at: pointAt(-90), diameter: diameter)
        _ = t.move(to: CGPoint(x: cx + 15, y: cy))
        _ = t.move(to: pointAt(180))
        let events = t.move(to: pointAt(200))
        let sum = events.compactMap { if case .tick(let d) = $0 { return d } else { return nil } }.reduce(0, +)
        #expect(sum == 1)
    }

    // MARK: - Group 6: Touch that starts in center

    @Test func C1_down_at_center_returns_true() {
        var t = makeTracker()
        let accepted = t.down(at: CGPoint(x: cx, y: cy), diameter: diameter)
        #expect(accepted)
    }

    @Test func C2_after_center_down_move_to_ring_empty() {
        var t = makeTracker()
        _ = t.down(at: CGPoint(x: cx, y: cy), diameter: diameter)
        let moveEvents = t.move(to: pointAt(0))
        #expect(moveEvents.isEmpty)
    }

    @Test func C3_center_down_move_beyond_slop_up_empty() {
        var t = makeTracker()
        _ = t.down(at: CGPoint(x: cx, y: cy), diameter: diameter)
        _ = t.move(to: pointAt(0)) // ringRadius=80 > tapSlop=8
        let upEvents = t.up()
        #expect(upEvents.isEmpty)
    }

    @Test func C4_center_down_no_move_up_returns_press_center() {
        var t = makeTracker()
        _ = t.down(at: CGPoint(x: cx, y: cy), diameter: diameter)
        let upEvents = t.up()
        #expect(upEvents == [.press(.center)])
    }

    // MARK: - Group 7: Touch outside wheel

    @Test func O1_down_outside_radius_returns_false() {
        var t = makeTracker()
        let accepted = t.down(at: CGPoint(x: cx + radius + 1, y: cy), diameter: diameter)
        #expect(!accepted)
    }

    // MARK: - Group 8: Tap zone classification

    func tap(degrees: Double, r: Double? = nil) -> [WheelEvent] {
        var t = makeTracker()
        let pt = pointAt(degrees, r: r)
        _ = t.down(at: pt, diameter: diameter)
        return t.up()
    }

    @Test func T1_tap_center_returns_press_center() {
        #expect(tap(degrees: 0, r: 0) == [.press(.center)])
    }

    @Test func T2_tap_top_ring_returns_press_menu() {
        #expect(tap(degrees: -90) == [.press(.menu)])
    }

    @Test func T3_tap_right_ring_returns_press_next() {
        #expect(tap(degrees: 0) == [.press(.next)])
    }

    @Test func T4_tap_bottom_ring_returns_press_playPause() {
        #expect(tap(degrees: 90) == [.press(.playPause)])
    }

    @Test func T5_tap_left_ring_returns_press_previous() {
        #expect(tap(degrees: 180) == [.press(.previous)])
    }

    @Test func T7_tap_minus134_5_returns_menu() {
        #expect(tap(degrees: -134.5) == [.press(.menu)])
    }

    @Test func T8_tap_minus135_5_returns_previous() {
        #expect(tap(degrees: -135.5) == [.press(.previous)])
    }

    @Test func T10_tap_minus44_5_returns_next() {
        #expect(tap(degrees: -44.5) == [.press(.next)])
    }

    @Test func T11_tap_minus45_5_returns_menu() {
        #expect(tap(degrees: -45.5) == [.press(.menu)])
    }

    @Test func T13_tap_44_5_returns_next() {
        #expect(tap(degrees: 44.5) == [.press(.next)])
    }

    @Test func T14_tap_45_5_returns_playPause() {
        #expect(tap(degrees: 45.5) == [.press(.playPause)])
    }

    @Test func T16_tap_134_5_returns_playPause() {
        #expect(tap(degrees: 134.5) == [.press(.playPause)])
    }

    @Test func T17_tap_135_5_returns_previous() {
        #expect(tap(degrees: 135.5) == [.press(.previous)])
    }

    // MARK: - Group 9: Long press (Center only)

    @Test func LP1_center_longPress_then_up_empty() {
        var t = makeTracker()
        _ = t.down(at: CGPoint(x: cx, y: cy), diameter: diameter)
        let lp = t.longPressTimeout()
        #expect(lp == [.longPress(.center)])
        let upEvents = t.up()
        #expect(upEvents.isEmpty)
    }

    @Test func LP2_ring_longPress_returns_empty_up_returns_press() {
        var t = makeTracker()
        _ = t.down(at: pointAt(-90), diameter: diameter) // Menu
        let lpEvents = t.longPressTimeout()
        #expect(lpEvents.isEmpty)
        let upEvents = t.up()
        #expect(upEvents == [.press(.menu)])
    }

    @Test func LP3_center_beyond_slop_longPress_empty() {
        var t = makeTracker()
        _ = t.down(at: CGPoint(x: cx, y: cy), diameter: diameter)
        _ = t.move(to: pointAt(0)) // exceeds slop
        let lpEvents = t.longPressTimeout()
        #expect(lpEvents.isEmpty)
    }

    // MARK: - Group 10: Ring button hold

    @Test func RH1_menu_hold_longPress_empty_up_press_menu() {
        var t = makeTracker()
        _ = t.down(at: pointAt(-90), diameter: diameter)
        let lpEvents = t.longPressTimeout()
        #expect(lpEvents.isEmpty)
        let upEvents = t.up()
        #expect(upEvents == [.press(.menu)])
    }

    @Test func RH2_next_hold_longPress_empty_up_press_next() {
        var t = makeTracker()
        _ = t.down(at: pointAt(0), diameter: diameter)
        let lpEvents = t.longPressTimeout()
        #expect(lpEvents.isEmpty)
        let upEvents = t.up()
        #expect(upEvents == [.press(.next)])
    }

    @Test func RH2_playPause_hold_longPress_empty_up_press_playPause() {
        var t = makeTracker()
        _ = t.down(at: pointAt(90), diameter: diameter)
        let lpEvents = t.longPressTimeout()
        #expect(lpEvents.isEmpty)
        let upEvents = t.up()
        #expect(upEvents == [.press(.playPause)])
    }

    @Test func RH2_previous_hold_longPress_empty_up_press_previous() {
        var t = makeTracker()
        _ = t.down(at: pointAt(180), diameter: diameter)
        let lpEvents = t.longPressTimeout()
        #expect(lpEvents.isEmpty)
        let upEvents = t.up()
        #expect(upEvents == [.press(.previous)])
    }
}
