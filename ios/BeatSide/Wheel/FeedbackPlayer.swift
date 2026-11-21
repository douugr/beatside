/// Plays the click sound.
@MainActor
protocol ClickSound: AnyObject {
    func play()
}

/// Fires haptics: a light one per tick, a medium one per press.
@MainActor
protocol HapticOutput: AnyObject {
    func tick()
    func press()
    /// Warms the haptic engine so the next tick or press fires without latency.
    func prepare()
}

/// Turns wheel events into feedback. Pure: no UIKit/AVFoundation, so the rules are unit-tested
/// with fakes. `settings` is read on every call, so a toggle applies to the very next event
/// (FR-021).
///
/// - Tick: click if Clicker is on, light haptic if Haptics is on (FR-018).
/// - Press / long press: medium haptic if Haptics is on, never a click (FR-019).
@MainActor
final class FeedbackPlayer {
    private let sound: ClickSound
    private let haptics: HapticOutput

    init(sound: ClickSound, haptics: HapticOutput) {
        self.sound = sound
        self.haptics = haptics
    }

    func play(_ event: WheelEvent, settings: WheelSettings) {
        switch event {
        case .tick:
            if settings.clickerEnabled { sound.play() }
            if settings.hapticsEnabled { haptics.tick() }
        case .press, .longPress:
            if settings.hapticsEnabled { haptics.press() }
        }
    }

    /// Call when a touch lands on the wheel, ahead of the first tick or press.
    func prepare(settings: WheelSettings) {
        if settings.hapticsEnabled { haptics.prepare() }
    }
}
