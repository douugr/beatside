import SwiftUI

/// Root view: owns the menu state and the session settings, and routes every wheel event through
/// `MenuNavigator`: Center opens, MENU goes back (FR-012, FR-013). iOS has no system back gesture
/// for this; FR-014 is Android-only.
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
        let result = MenuNavigator.reduce(menuState, event)
        menuState = result.state
        switch result.effect {
        case .toggleSetting(let key):
            settings = settings.toggled(key)
        case .unhandled, nil:
            // Next / Previous / Play-Pause and long press have no effect in this slice (FR-011).
            break
        }
    }
}
