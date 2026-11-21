import SwiftUI
import UIKit

/// The wheel. Draws the ring and buttons, turns touches into `WheelEvent`s through a
/// `WheelTracker`, and reports them through `onEvent`. It knows nothing about menus or screens
/// (Constitution II). Feedback is wired in T033.
struct ClickWheelView: View {
    let onEvent: (WheelEvent) -> Void

    var body: some View {
        ZStack {
            WheelFace()
            WheelTouchSurface(onEvent: onEvent)
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
    let onEvent: (WheelEvent) -> Void

    func makeUIView(context: Context) -> WheelTouchView {
        let view = WheelTouchView()
        view.onEvent = onEvent
        return view
    }

    func updateUIView(_ view: WheelTouchView, context: Context) {
        view.onEvent = onEvent
    }
}

final class WheelTouchView: UIView {
    var onEvent: (WheelEvent) -> Void = { _ in }

    private var tracker = WheelTracker()
    private var activeTouch: UITouch?
    private var longPressTask: Task<Void, Never>?

    override init(frame: CGRect) {
        super.init(frame: frame)
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
        guard tracker.down(at: touch.location(in: self), diameter: Double(bounds.width)) else { return }
        activeTouch = touch
        longPressTask = Task { @MainActor [weak self] in
            try? await Task.sleep(for: .milliseconds(WheelTracker.longPressMillis))
            guard !Task.isCancelled, let self else { return }
            // The tracker decides whether this counts (Center only, no slop, no ticks).
            self.dispatch(self.tracker.longPressTimeout())
        }
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let touch = activeTouch, touches.contains(touch) else { return }
        dispatch(tracker.move(to: touch.location(in: self)))
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let touch = activeTouch, touches.contains(touch) else { return }
        endGesture()
        dispatch(tracker.up())
    }

    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let touch = activeTouch, touches.contains(touch) else { return }
        endGesture()
        tracker.cancel()
    }

    private func endGesture() {
        longPressTask?.cancel()
        longPressTask = nil
        activeTouch = nil
    }

    private func dispatch(_ events: [WheelEvent]) {
        events.forEach(onEvent)
    }
}
