import SwiftUI
import UIKit

/// The wheel. Draws the ring and buttons, turns touches into `WheelEvent`s through a
/// `WheelTracker`, plays feedback for each one with the current `settings`, then reports it
/// through `onEvent`. It knows nothing about menus or screens (Constitution II).
struct ClickWheelView: View {
    let settings: WheelSettings
    let onEvent: (WheelEvent) -> Void

    var body: some View {
        ZStack {
            WheelFace()
            WheelTouchSurface(settings: settings, onEvent: onEvent)
        }
        .aspectRatio(1, contentMode: .fit)
        // One element for UI tests; labels and the adjustable trait come in T046.
        .accessibilityElement(children: .ignore)
        .accessibilityIdentifier(Self.accessibilityID)
    }

    static let accessibilityID = "click_wheel"
}

private enum WheelColors {
    static let ringLight = Color(red: 0.957, green: 0.957, blue: 0.949)
    static let ringDark = Color(red: 0.839, green: 0.839, blue: 0.824)
    static let edge = Color(red: 0.741, green: 0.741, blue: 0.722)
    static let centerLight = Color(red: 0.914, green: 0.914, blue: 0.902)
    static let centerDark = Color(red: 0.812, green: 0.812, blue: 0.796)
    static let glyph = Color(red: 0.549, green: 0.549, blue: 0.533)
}

/// The static drawing: ring, center button and the four button labels.
private struct WheelFace: View {
    var body: some View {
        GeometryReader { geo in
            let d = min(geo.size.width, geo.size.height)
            let r = d / 2
            let ringMid = r * 0.68
            let centerDiameter = d * WheelTracker.centerRatio
            ZStack {
                Circle()
                    .fill(RadialGradient(colors: [WheelColors.ringLight, WheelColors.ringDark],
                                         center: .center, startRadius: 0, endRadius: r))
                Circle().strokeBorder(WheelColors.edge, lineWidth: 1)
                Circle()
                    .fill(LinearGradient(colors: [WheelColors.centerLight, WheelColors.centerDark],
                                         startPoint: .top, endPoint: .bottom))
                    .overlay(Circle().strokeBorder(WheelColors.edge, lineWidth: 1))
                    .frame(width: centerDiameter, height: centerDiameter)
                Text(String(localized: "wheel_menu"))
                    .font(.system(size: d * 0.055, weight: .bold))
                    .offset(y: -ringMid)
                Image(systemName: "forward.end.alt.fill").offset(x: ringMid)
                Image(systemName: "backward.end.alt.fill").offset(x: -ringMid)
                Image(systemName: "playpause.fill").offset(y: ringMid)
            }
            .font(.system(size: d * 0.06))
            .foregroundStyle(WheelColors.glyph)
            .frame(width: d, height: d)
            .position(x: geo.size.width / 2, y: geo.size.height / 2)
        }
    }
}

/// Bridges raw UIKit touches (including cancellation) to the tracker.
private struct WheelTouchSurface: UIViewRepresentable {
    let settings: WheelSettings
    let onEvent: (WheelEvent) -> Void

    func makeUIView(context: Context) -> WheelTouchView {
        let view = WheelTouchView(feedback: FeedbackPlayer(sound: AudioClickSound(), haptics: GeneratorHaptics()))
        view.settings = settings
        view.onEvent = onEvent
        return view
    }

    func updateUIView(_ view: WheelTouchView, context: Context) {
        view.settings = settings
        view.onEvent = onEvent
    }
}

final class WheelTouchView: UIView {
    var onEvent: (WheelEvent) -> Void = { _ in }
    /// Read on every event, so a toggle applies to the very next tick or press (FR-021).
    var settings = WheelSettings()

    private let feedback: FeedbackPlayer
    private var tracker = WheelTracker()
    private var activeTouch: UITouch?
    private var gestureActive = false
    private var longPressTask: Task<Void, Never>?

    init(feedback: FeedbackPlayer) {
        self.feedback = feedback
        super.init(frame: .zero)
        backgroundColor = .clear
        isMultipleTouchEnabled = false
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }

    /// Only the wheel's circle receives touches; the corners pass through.
    override func point(inside point: CGPoint, with event: UIEvent?) -> Bool {
        let r = bounds.width / 2
        return hypot(point.x - r, point.y - r) <= r
    }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard activeTouch == nil, let touch = touches.first else { return }
        if begin(at: touch.location(in: self)) { activeTouch = touch }
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let touch = activeTouch, touches.contains(touch) else { return }
        move(to: touch.location(in: self))
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let touch = activeTouch, touches.contains(touch) else { return }
        activeTouch = nil
        end()
    }

    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let touch = activeTouch, touches.contains(touch) else { return }
        activeTouch = nil
        cancel()
    }

    // Point-based gesture steps. The touch overrides above delegate here; unit tests drive them
    // directly because UITouch can't be constructed.

    /// Starts a gesture; returns false (and ignores it) when the point is outside the wheel.
    @discardableResult
    func begin(at point: CGPoint) -> Bool {
        guard !gestureActive, tracker.down(at: point, diameter: Double(bounds.width)) else { return false }
        gestureActive = true
        feedback.prepare(settings: settings)
        longPressTask = Task { @MainActor [weak self] in
            try? await Task.sleep(for: .milliseconds(WheelTracker.longPressMillis))
            guard !Task.isCancelled, let self else { return }
            // The tracker decides whether this counts (Center only, no slop, no ticks).
            self.dispatch(self.tracker.longPressTimeout())
        }
        return true
    }

    func move(to point: CGPoint) {
        guard gestureActive else { return }
        dispatch(tracker.move(to: point))
    }

    func end() {
        guard gestureActive else { return }
        stopGesture()
        dispatch(tracker.up())
    }

    func cancel() {
        guard gestureActive else { return }
        stopGesture()
        tracker.cancel()
    }

    private func stopGesture() {
        gestureActive = false
        longPressTask?.cancel()
        longPressTask = nil
    }

    /// Feedback first, in the same frame as the touch sample (Constitution II), then the event.
    private func dispatch(_ events: [WheelEvent]) {
        for event in events {
            feedback.play(event, settings: settings)
            onEvent(event)
        }
    }
}
