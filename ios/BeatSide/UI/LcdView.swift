import SwiftUI

/// The display: a title bar with the current node's title, then its content. Submenus show
/// their rows; leaf screens are drawn by `RouteView`.
struct LcdView: View {
    let state: MenuState
    let settings: WheelSettings

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: 8)
        VStack(spacing: 0) {
            Text(MenuStrings.title(for: state.current.node))
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(LcdColors.text)
                .lineLimit(1)
                .padding(.horizontal, 8)
                .frame(maxWidth: .infinity)
                .frame(height: 28)
                .background(LcdColors.titleBar)
            Rectangle().fill(LcdColors.border).frame(height: 1)
            content
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        }
        .background(LcdColors.background)
        .clipShape(shape)
        .overlay(shape.strokeBorder(LcdColors.border, lineWidth: 2))
    }

    @ViewBuilder
    private var content: some View {
        switch state.current.node {
        case .submenu(_, let children):
            MenuListView(children: children, selectedIndex: state.current.selectedIndex, settings: settings)
                // A new list (fresh scroll position) per stack level.
                .id(state.stack.count)
        case .screen(_, let route):
            RouteView(route: route)
        case .toggle:
            // Toggles are never pushed onto the stack (MenuNavigator emits toggleSetting instead).
            Color.clear
        }
    }
}

/// Draws the leaf screen for a route. Every route is a placeholder until Phase 6 (T042).
private struct RouteView: View {
    let route: Route

    var body: some View {
        switch route {
        case .comingSoon, .nowPlaying, .shuffleSongs, .clock, .about:
            ComingSoonView()
        }
    }
}
