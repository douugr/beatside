import SwiftUI

/// Placeholder for leaf screens that are not built yet (FR-026).
struct ComingSoonView: View {
    var body: some View {
        CenteredMessage(title: String(localized: "coming_soon"),
                        hint: String(localized: "press_menu_to_go_back"))
    }
}

/// A bold message with a smaller hint underneath, centered on the LCD.
struct CenteredMessage: View {
    let title: String
    let hint: String

    var body: some View {
        VStack(spacing: 8) {
            Text(title).font(.system(size: 20, weight: .bold))
            Text(hint).font(.system(size: 14))
        }
        .multilineTextAlignment(.center)
        .foregroundStyle(LcdColors.text)
        .padding(16)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
