import SwiftUI

/// App name, version and author (FR-025). The version comes from the bundle via `AboutInfo`.
struct AboutView: View {
    var body: some View {
        VStack(spacing: 6) {
            Text(String(localized: "app_name"))
                .font(.system(size: 22, weight: .bold))
            Text(String(format: String(localized: "about_version"), AboutInfo.version))
            Text(String(localized: "about_author"))
        }
        .font(.system(size: 15))
        .multilineTextAlignment(.center)
        .foregroundStyle(LcdColors.text)
        .padding(16)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
