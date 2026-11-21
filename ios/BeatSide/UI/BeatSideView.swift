import SwiftUI

/// Root view: owns the menu state and the session settings, and routes wheel events through
/// `MenuNavigator`. Phase 3 (US1) routes ticks only; presses are wired in T028.
struct BeatSideView: View {
    @State private var menuState = MenuState.root(BeatSideMenu.root)
    @State private var settings = WheelSettings()

    var body: some View {
        VStack(spacing: 28) {
            LcdView(state: menuState, settings: settings)
                .frame(maxHeight: .infinity)
            ClickWheelView(onEvent: handle)
                .padding(.horizontal, 30)
        }
        .padding(20)
        .background {
            LinearGradient(colors: [Color(white: 0.98), Color(red: 0.89, green: 0.89, blue: 0.875)],
                           startPoint: .top, endPoint: .bottom)
                .ignoresSafeArea()
        }
    }

    private func handle(_ event: WheelEvent) {
        guard case .tick = event else { return }
        let result = MenuNavigator.reduce(menuState, event)
        menuState = result.state
        if case .toggleSetting(let key) = result.effect {
            settings = settings.toggled(key)
        }
    }
}
