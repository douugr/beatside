import Foundation
import Testing
@testable import BeatSide

// Feedback wiring (T033): WheelTouchView prepares haptics when a touch lands, plays feedback for
// every event before dispatching it, and reads the settings current at that moment. UITouch can't
// be built in a unit test, so the touch overrides delegate to begin/move/end, which are driven here.
// Mirrors the feedback cases in the Android ClickWheelTest.kt.
@MainActor
struct WheelTouchViewTests {

    final class Recorder: ClickSound, HapticOutput {
        var log: [String] = []
        func play() { log.append("click") }
        func tick() { log.append("tick-haptic") }
        func press() { log.append("press-haptic") }
        func prepare() { log.append("prepare") }
    }

    let recorder = Recorder()
    let view: WheelTouchView

    init() {
        let recorder = self.recorder
        view = WheelTouchView(feedback: FeedbackPlayer(sound: recorder, haptics: recorder))
        view.frame = CGRect(x: 0, y: 0, width: 200, height: 200)
        view.onEvent = { recorder.log.append("event:\($0)") }
    }

    private func ring(_ degrees: Double) -> CGPoint {
        let rad = degrees * .pi / 180
        return CGPoint(x: 100 + 80 * cos(rad), y: 100 + 80 * sin(rad))
    }

    private func drag(detents: Int) {
        view.begin(at: ring(-90))
        for deg in 1...(detents * 15 + 5) { view.move(to: ring(-90 + Double(deg))) }
        view.end()
    }

    @Test func aCenterTapPreparesThenBuzzesBeforeDispatching() {
        view.begin(at: CGPoint(x: 100, y: 100))
        view.end()
        #expect(recorder.log == ["prepare", "press-haptic", "event:press(BeatSide.WheelButton.center)"])
    }

    @Test func eachTickClicksAndBuzzesBeforeItIsDispatched() {
        drag(detents: 2)
        let tick = "event:tick(1)"
        #expect(recorder.log == ["prepare", "click", "tick-haptic", tick, "click", "tick-haptic", tick])
    }

    @Test func turningClickerOffAppliesToTheVeryNextTick() {
        drag(detents: 1)
        view.settings = view.settings.toggled(.clicker)
        recorder.log.removeAll()
        drag(detents: 1)
        #expect(recorder.log == ["prepare", "tick-haptic", "event:tick(1)"])
    }

    @Test func turningHapticsOffSkipsPrepareAndThePressHaptic() {
        view.settings = WheelSettings(clickerEnabled: true, hapticsEnabled: false)
        view.begin(at: CGPoint(x: 100, y: 100))
        view.end()
        #expect(recorder.log == ["event:press(BeatSide.WheelButton.center)"])
    }

    @Test func aTouchOutsideTheWheelDoesNothing() {
        view.begin(at: CGPoint(x: 199, y: 1)) // corner: outside the circle
        view.end()
        #expect(recorder.log.isEmpty)
    }
}
