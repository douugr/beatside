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
        let missing = [
            "setting_on", "setting_off", "wheel_menu", "coming_soon", "press_menu_to_go_back",
            "about_version", "about_author",
            "a11y_wheel_center", "a11y_wheel_menu", "a11y_wheel_next", "a11y_wheel_previous", "a11y_wheel_play_pause",
            "a11y_wheel", "a11y_action_next", "a11y_action_previous", "a11y_action_select", "a11y_action_back",
            "a11y_row_submenu", "a11y_row_toggle", "a11y_screen_and_row",
        ].filter { !hasCatalogEntry($0) }
        #expect(missing.isEmpty, "UI keys with no Localizable.xcstrings entry: \(missing)")
    }

    @Test func menuStringsResolvesTitlesThroughTheCatalog() {
        #expect(MenuStrings.title(for: BeatSideMenu.root) == "BeatSide")
        #expect(MenuStrings.title(for: .screen(titleKey: "menu_about", route: .about))
            == Bundle.main.localizedString(forKey: "menu_about", value: nil, table: "Localizable"))
    }

    // MARK: SC-005 / FR-029: Brazilian Portuguese

    private func placeholders(_ text: String) -> [String] {
        text.matches(of: /%(\d+\$)?[@d]/).map { String($0.output.0) }.sorted()
    }

    @Test func everyEnglishKeyHasABrazilianPortugueseValueWithTheSamePlaceholders() throws {
        let englishURL = try #require(Bundle.main.url(forResource: "Localizable", withExtension: "strings",
                                                      subdirectory: nil, localization: "en"))
        let english = try #require(NSDictionary(contentsOf: englishURL) as? [String: String])
        #expect(english.count >= 37, "compiled English table looks incomplete: \(english.count) keys")

        let portuguesePath = try #require(Bundle.main.path(forResource: "pt-BR", ofType: "lproj"), "no pt-BR.lproj in the app")
        let portuguese = try #require(Bundle(path: portuguesePath))

        var missing: [String] = []
        for (key, englishValue) in english {
            let value = portuguese.localizedString(forKey: key, value: missingSentinel, table: "Localizable")
            if value == missingSentinel || value.isEmpty {
                missing.append(key)
            } else {
                #expect(placeholders(value) == placeholders(englishValue), "placeholders of \(key)")
            }
        }
        #expect(missing.isEmpty, "keys with no pt-BR value: \(missing.sorted())")
    }
}
