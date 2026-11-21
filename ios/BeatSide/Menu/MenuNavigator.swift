/// The result of one reduction: the new state and an optional effect for the UI layer.
struct MenuResult: Equatable, Sendable {
    let state: MenuState
    var effect: MenuEffect? = nil
}

/// Pure reducer: (state, wheel event) → (new state, optional effect). No I/O.
///
/// - Ticks move the selection in a submenu and clamp at both ends (FR-015).
/// - Center opens the selected submenu or screen, or emits `.toggleSetting` (FR-012).
/// - Menu goes back one level; at the root it does nothing (FR-013).
/// - Next / Previous / Play/Pause and long press are `.unhandled` (FR-011).
/// - On a `.screen`, every event except Menu belongs to the screen: `.unhandled`.
struct MenuNavigator {

    static func reduce(_ state: MenuState, _ event: WheelEvent) -> MenuResult {
        if event == .press(.menu) {
            return MenuResult(state: back(state))
        }

        let frame = state.current
        guard case .submenu(_, let children) = frame.node else {
            return MenuResult(state: state, effect: .unhandled(event))
        }

        switch event {
        case .tick(let direction):
            let last = max(children.count - 1, 0)
            let index = min(max(frame.selectedIndex + direction, 0), last)
            var updated = frame
            updated.selectedIndex = index
            return MenuResult(state: MenuState(stack: state.stack.dropLast() + [updated]))
        case .press(.center):
            return select(state, children: children, index: frame.selectedIndex)
        case .press, .longPress:
            return MenuResult(state: state, effect: .unhandled(event))
        }
    }

    /// Pops one level; at the root returns `state` unchanged.
    static func back(_ state: MenuState) -> MenuState {
        state.canGoBack ? MenuState(stack: Array(state.stack.dropLast())) : state
    }

    private static func select(_ state: MenuState, children: [MenuNode], index: Int) -> MenuResult {
        guard children.indices.contains(index) else { return MenuResult(state: state) }
        let child = children[index]
        switch child {
        case .submenu, .screen:
            return MenuResult(state: MenuState(stack: state.stack + [MenuFrame(node: child)]))
        case .toggle(_, let key):
            return MenuResult(state: state, effect: .toggleSetting(key))
        }
    }
}
