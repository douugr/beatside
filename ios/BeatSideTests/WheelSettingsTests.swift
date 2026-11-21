import Testing
@testable import BeatSide

// WheelSettings (data-model.md): both default to true; toggling flips only the named field.
// Mirrors the Android WheelSettingsTest.kt.
struct WheelSettingsTests {

    @Test func defaultsAreBothEnabled() {
        #expect(WheelSettings() == WheelSettings(clickerEnabled: true, hapticsEnabled: true))
    }

    @Test func togglingClickerFlipsOnlyClickerEnabled() {
        #expect(WheelSettings().toggled(.clicker) == WheelSettings(clickerEnabled: false, hapticsEnabled: true))
    }

    @Test func togglingHapticsFlipsOnlyHapticsEnabled() {
        #expect(WheelSettings().toggled(.haptics) == WheelSettings(clickerEnabled: true, hapticsEnabled: false))
    }

    @Test func togglingTwiceRestoresTheOriginalValue() {
        let s = WheelSettings()
        #expect(s.toggled(.clicker).toggled(.clicker) == s)
        #expect(s.toggled(.haptics).toggled(.haptics) == s)
    }

    @Test func isOnReportsTheFieldForEachKey() {
        let s = WheelSettings(clickerEnabled: false, hapticsEnabled: true)
        #expect(!s.isOn(.clicker))
        #expect(s.isOn(.haptics))
    }
}
