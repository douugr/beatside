import Foundation
import Testing
@testable import BeatSide

// FR-025: About reads the version from the bundle so it never drifts from MARKETING_VERSION.
struct AboutInfoTests {

    @Test func versionIsCFBundleShortVersionString() {
        #expect(AboutInfo.version(from: ["CFBundleShortVersionString": "1.2.3", "CFBundleVersion": "42"]) == "1.2.3")
    }

    @Test func missingVersionIsEmpty() {
        #expect(AboutInfo.version(from: nil) == "")
        #expect(AboutInfo.version(from: [:]) == "")
    }

    @Test func appBundleVersionMatchesItsInfoDictionary() {
        let expected = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String
        #expect(expected?.isEmpty == false)
        #expect(AboutInfo.version == expected)
    }
}
