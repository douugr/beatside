import Foundation

/// What the About screen shows that comes from the build (FR-025): the version is read from the
/// bundle's CFBundleShortVersionString (MARKETING_VERSION), never written as a literal.
enum AboutInfo {
    static func version(from infoDictionary: [String: Any]?) -> String {
        infoDictionary?["CFBundleShortVersionString"] as? String ?? ""
    }

    static var version: String { version(from: Bundle.main.infoDictionary) }
}
