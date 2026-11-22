/// The navigation stack. Never empty; the root is always at index 0.
struct MenuState: Equatable, Sendable {
    let stack: [MenuFrame]

    init(stack: [MenuFrame]) {
        precondition(!stack.isEmpty, "MenuState stack must not be empty")
        self.stack = stack
    }

    var current: MenuFrame { stack[stack.count - 1] }
    var canGoBack: Bool { stack.count > 1 }

    static func root(_ node: MenuNode) -> MenuState {
        MenuState(stack: [MenuFrame(node: node)])
    }
}
