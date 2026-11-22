import XCTest

extension XCUIApplication {
    /// The wheel's accessibility element (identifier "click_wheel"), whatever its element type.
    var wheel: XCUIElement { descendants(matching: .any)["click_wheel"] }

    /// An element whose accessibility label is `text`, or a row whose label starts with `text, `
    /// (rows read as "Music, submenu" / "Clicker, On" for VoiceOver, FR-028).
    func labelled(_ text: String) -> XCUIElement {
        descendants(matching: .any)
            .matching(NSPredicate(format: "label == %@ OR label BEGINSWITH %@", text, text + ", "))
            .firstMatch
    }

    /// Number of toggle rows showing `state` ("On" / "Off").
    func rowCount(withState state: String) -> Int {
        descendants(matching: .any).matching(NSPredicate(format: "label ENDSWITH %@", ", " + state)).count
    }
}
