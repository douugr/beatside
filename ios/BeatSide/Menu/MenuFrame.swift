/// One entry on the navigation stack. `selectedIndex` is 0 for leaf screens.
struct MenuFrame: Equatable, Sendable {
    var node: MenuNode
    var selectedIndex: Int = 0
}
