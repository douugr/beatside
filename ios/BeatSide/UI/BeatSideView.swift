import SwiftUI

/// Root view: the LCD over the wheel. State and event routing live in `BeatSideModel`
/// (Center opens, MENU goes back; iOS has no system back gesture for this, FR-014 is Android-only).
struct BeatSideView: View {
    @State private var model = BeatSideModel(announcer: VoiceOverAnnouncer())

    var body: some View {
        VStack(spacing: 28) {
            LcdView(state: model.menuState, settings: model.settings)
                .frame(maxHeight: .infinity)
            ClickWheelView(settings: model.settings, onEvent: model.handle)
                .padding(.horizontal, 30)
        }
        .padding(20)
        .background {
            LinearGradient(colors: [Color(white: 0.98), Color(red: 0.89, green: 0.89, blue: 0.875)],
                           startPoint: .top, endPoint: .bottom)
                .ignoresSafeArea()
        }
    }
}
