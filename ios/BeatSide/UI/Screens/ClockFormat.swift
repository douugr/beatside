import Foundation

/// The Clock's formatting decision (FR-024, SC-006). Pure: the screen supplies whether the device
/// uses 24-hour time (read from the locale's "j" hour template) and the locale; this decides the
/// pattern. Twin of the Android ClockFormat.kt.
enum ClockFormat {
    static let pattern24Hour = "HH:mm:ss"
    static let pattern12Hour = "h:mm:ss a"

    static func pattern(uses24Hour: Bool) -> String {
        uses24Hour ? pattern24Hour : pattern12Hour
    }

    /// `hourTemplate` is `DateFormatter.dateFormat(fromTemplate: "j", …)`, which reflects the
    /// user's 24-Hour Time switch. 24-hour symbols are H and k; 12-hour are h and K. Quoted
    /// literal text is ignored.
    static func uses24Hour(hourTemplate: String) -> Bool {
        var inQuote = false
        for character in hourTemplate {
            if character == "'" { inQuote.toggle(); continue }
            if !inQuote, character == "H" || character == "k" { return true }
        }
        return false
    }

    static func string(from date: Date, uses24Hour: Bool, locale: Locale, timeZone: TimeZone) -> String {
        let formatter = DateFormatter()
        formatter.locale = locale
        formatter.timeZone = timeZone
        formatter.dateFormat = pattern(uses24Hour: uses24Hour)
        return formatter.string(from: date)
    }
}
