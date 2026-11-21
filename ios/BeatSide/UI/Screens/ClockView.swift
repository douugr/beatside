import SwiftUI

/// Live local time, updated on each second, in the device's 12/24-hour format (FR-024, SC-006).
/// The pattern decision is `ClockFormat`; this view only observes the system: it re-reads the
/// setting when the current locale changes (iOS posts that when 24-Hour Time is switched), so the
/// format updates while the screen is open.
struct ClockView: View {
    @State private var uses24Hour = ClockView.deviceUses24Hour()

    var body: some View {
        TimelineView(.periodic(from: Self.startOfCurrentSecond(), by: 1)) { context in
            Text(ClockFormat.string(from: context.date, uses24Hour: uses24Hour,
                                    locale: .autoupdatingCurrent, timeZone: .autoupdatingCurrent))
                .font(.system(size: 34, weight: .bold))
                .monospacedDigit()
                .foregroundStyle(LcdColors.text)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .onReceive(NotificationCenter.default.publisher(for: NSLocale.currentLocaleDidChangeNotification)) { _ in
            uses24Hour = Self.deviceUses24Hour()
        }
    }

    /// Ticks land on second boundaries, so the display changes as the second does.
    private static func startOfCurrentSecond() -> Date {
        Date(timeIntervalSinceReferenceDate: Date.now.timeIntervalSinceReferenceDate.rounded(.down))
    }

    /// The locale's "j" hour template reflects the user's 24-Hour Time setting.
    private static func deviceUses24Hour() -> Bool {
        let template = DateFormatter.dateFormat(fromTemplate: "j", options: 0, locale: .current) ?? "HH"
        return ClockFormat.uses24Hour(hourTemplate: template)
    }
}
