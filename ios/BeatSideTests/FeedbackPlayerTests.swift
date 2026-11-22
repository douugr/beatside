import Testing
@testable import BeatSide

// FeedbackPlayer decides, per event and with the settings at that moment, whether to click and
// which haptic to fire (FR-018, FR-019, FR-021). Fakes record the outputs; no real audio.
// Mirrors the Android FeedbackPlayerTest.kt, plus haptic preparation (iOS only).
@MainActor
struct FeedbackPlayerTests {

    final class FakeOutputs: ClickSound, HapticOutput {
        var log: [String] = []
        func play() { log.append("click") }
        func tick() { log.append("tick-haptic") }
        func press() { log.append("press-haptic") }
        func prepare() { log.append("prepare") }
    }

    let fake = FakeOutputs()
    var player: FeedbackPlayer { FeedbackPlayer(sound: fake, haptics: fake) }

    let allOn = WheelSettings()
    let clickerOff = WheelSettings(clickerEnabled: false, hapticsEnabled: true)
    let hapticsOff = WheelSettings(clickerEnabled: true, hapticsEnabled: false)
    let allOff = WheelSettings(clickerEnabled: false, hapticsEnabled: false)

    func outputs(_ event: WheelEvent, _ settings: WheelSettings) -> [String] {
        fake.log.removeAll()
        player.play(event, settings: settings)
        return fake.log
    }

    // MARK: FR-018: every tick clicks and fires a light haptic, each unless disabled

    @Test func tickWithEverythingOnClicksAndFiresTheTickHaptic() {
        #expect(outputs(.tick(+1), allOn) == ["click", "tick-haptic"])
        #expect(outputs(.tick(-1), allOn) == ["click", "tick-haptic"])
    }

    @Test func tickWithClickerOffFiresOnlyTheHaptic() {
        #expect(outputs(.tick(+1), clickerOff) == ["tick-haptic"])
    }

    @Test func tickWithHapticsOffOnlyClicks() {
        #expect(outputs(.tick(+1), hapticsOff) == ["click"])
    }

    @Test func tickWithBothOffDoesNothing() {
        #expect(outputs(.tick(+1), allOff).isEmpty)
    }

    // MARK: FR-019: every press fires a medium haptic and never clicks

    @Test func everyButtonPressFiresThePressHapticAndNeverClicks() {
        for button in [WheelButton.center, .menu, .next, .previous, .playPause] {
            #expect(outputs(.press(button), allOn) == ["press-haptic"], "press \(button)")
        }
    }

    @Test func pressWithHapticsOffDoesNothingEvenWithClickerOn() {
        #expect(outputs(.press(.center), hapticsOff).isEmpty)
    }

    @Test func longPressFiresThePressHapticAndNeverClicks() {
        #expect(outputs(.longPress(.center), allOn) == ["press-haptic"])
        #expect(outputs(.longPress(.center), hapticsOff).isEmpty)
    }

    // MARK: FR-021: a toggle applies to the very next event

    @Test func togglingClickerAppliesToTheVeryNextTick() {
        var settings = allOn
        #expect(outputs(.tick(+1), settings) == ["click", "tick-haptic"])
        settings = settings.toggled(.clicker)
        #expect(outputs(.tick(+1), settings) == ["tick-haptic"])
        settings = settings.toggled(.clicker)
        #expect(outputs(.tick(+1), settings) == ["click", "tick-haptic"])
    }

    @Test func togglingHapticsAppliesToTheVeryNextPress() {
        var settings = allOn
        #expect(outputs(.press(.center), settings) == ["press-haptic"])
        settings = settings.toggled(.haptics)
        #expect(outputs(.press(.center), settings).isEmpty)
        #expect(outputs(.tick(+1), settings) == ["click"])
    }

    // MARK: Preparing haptics ahead of use (same-frame ticks)

    @Test func prepareWarmsTheHapticsWhenEnabled() {
        fake.log.removeAll()
        player.prepare(settings: allOn)
        #expect(fake.log == ["prepare"])
    }

    @Test func prepareSkipsTheHapticsWhenDisabled() {
        fake.log.removeAll()
        player.prepare(settings: hapticsOff)
        #expect(fake.log.isEmpty)
    }
}
