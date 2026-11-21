import Foundation
import Testing
@testable import BeatSide

// Every titleKey in BeatSideMenu must have an entry in Localizable.xcstrings. Keys are dynamic,
// so Xcode cannot extract them: they are added to the catalog by hand, and this test is the
// guard. Twin of the Android MenuStringsTest.kt.
struct MenuStringsTests {

    private let missingSentinel = "\u{0}missing"

    private func allNodes(_ node: MenuNode) -> [MenuNode] {
        if case .submenu(_, let children) = node {
            return [node] + children.flatMap(allNodes)
        }
        return [node]
    }

    /// True when the app bundle's compiled Localizable table has a value for `key`.
    private func hasCatalogEntry(_ key: String) -> Bool {
        let value = Bundle.main.localizedString(forKey: key, value: missingSentinel, table: "Localizable")
        return value != missingSentinel && value != key
    }

    @Test func testsRunInsideTheAppBundle() {
        #expect(Bundle.main.bundleIdentifier == "dev.douglasneto.BeatSide")
    }

    @Test func everyBeatSideMenuTitleKeyHasACatalogEntry() {
        let nodes = allNodes(BeatSideMenu.root)
        #expect(nodes.count == 17, "tree size from data-model.md")
        let missing = nodes.map(\.titleKey).filter { !hasCatalogEntry($0) }
        #expect(missing.isEmpty, "titleKeys with no Localizable.xcstrings entry: \(missing)")
    }

    @Test func wheelAndRowStringsHaveCatalogEntries() {
        let missing = ["setting_on", "setting_off", "wheel_menu"].filter { !hasCatalogEntry($0) }
        #expect(missing.isEmpty, "UI keys with no Localizable.xcstrings entry: \(missing)")
    }

    @Test func menuStringsResolvesTitlesThroughTheCatalog() {
        #expect(MenuStrings.title(for: BeatSideMenu.root) == "BeatSide")
        #expect(MenuStrings.title(for: .screen(titleKey: "menu_about", route: .about))
            == Bundle.main.localizedString(forKey: "menu_about", value: nil, table: "Localizable"))
    }
}
