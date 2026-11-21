import Foundation

/// Resolves a menu node's `titleKey` through Localizable.xcstrings.
///
/// Keys are dynamic, so Xcode cannot extract them into the catalog: every key is added to
/// Localizable.xcstrings by hand, and MenuStringsTests fails if one is missing.
enum MenuStrings {
    static func title(for node: MenuNode) -> String {
        String(localized: String.LocalizationValue(node.titleKey))
    }
}
