import Foundation
import Testing
@testable import BeatSide

// FR-024 / SC-006: which pattern the Clock uses for the device's 12/24-hour setting, and how that
// setting is read from the locale's "j" hour template. Pure. Twin of the Android ClockFormatTest.kt.
struct ClockFormatTests {

    private func fmt(_ h: Int, _ m: Int, _ s: Int, uses24Hour: Bool) -> String {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC")!
        let date = calendar.date(from: DateComponents(year: 2026, month: 11, day: 21, hour: h, minute: m, second: s))!
        return ClockFormat.string(from: date, uses24Hour: uses24Hour,
                                  locale: Locale(identifier: "en_US_POSIX"), timeZone: TimeZone(identifier: "UTC")!)
    }

    // MARK: Pattern choice

    @Test func twentyFourHourSettingUsesHHmmss() {
        #expect(ClockFormat.pattern(uses24Hour: true) == "HH:mm:ss")
    }

    @Test func twelveHourSettingUsesHmmssWithAmPmAndNoLeadingZero() {
        #expect(ClockFormat.pattern(uses24Hour: false) == "h:mm:ss a")
    }

    @Test func eveningTime() {
        #expect(fmt(19, 5, 9, uses24Hour: true) == "19:05:09")
        #expect(fmt(19, 5, 9, uses24Hour: false) == "7:05:09 PM")
    }

    @Test func morningTimeIn12Hour() {
        #expect(fmt(9, 30, 0, uses24Hour: false) == "9:30:00 AM")
    }

    @Test func midnightIs00In24HourAnd12AMIn12Hour() {
        #expect(fmt(0, 0, 0, uses24Hour: true) == "00:00:00")
        #expect(fmt(0, 0, 0, uses24Hour: false) == "12:00:00 AM")
    }

    @Test func noonIs12InBothFormats() {
        #expect(fmt(12, 0, 0, uses24Hour: true) == "12:00:00")
        #expect(fmt(12, 0, 0, uses24Hour: false) == "12:00:00 PM")
    }

    // MARK: Reading the device setting from the "j" hour template

    @Test(arguments: [("HH", true), ("H", true), ("k", true), ("kk", true),
                      ("h a", false), ("a h", false), ("hh a", false), ("K a", false), ("Bh", false)])
    func hourTemplateTellsWhetherTheDeviceUses24Hour(template: String, expected: Bool) {
        #expect(ClockFormat.uses24Hour(hourTemplate: template) == expected)
    }

    @Test func quotedLiteralTextInTheTemplateIsIgnored() {
        #expect(ClockFormat.uses24Hour(hourTemplate: "h 'Hour'") == false)
        #expect(ClockFormat.uses24Hour(hourTemplate: "HH 'h'") == true)
    }
}
