import XCTest

/// T051 (FR-027, FR-028, FR-031, SC-004): what VoiceOver sees in the running app. The actions
/// themselves can't be invoked from XCUITest; AccessibilityNavigationTests (unit) drives them,
/// including SC-007.
@MainActor
final class AccessibilityUITests: XCTestCase {

    private var app: XCUIApplication!

    override func setUp() async throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launch()
        XCTAssertTrue(app.wheel.waitForExistence(timeout: 5), "wheel not found")
    }

    func testTheWheelIsOneLabelledElement() {
        XCTAssertEqual(app.wheel.label, "Wheel")
    }

    func testTheFiveWheelButtonsAreLabelledButtons() {
        for label in ["Select button", "Menu button", "Next button", "Previous button", "Play/Pause button"] {
            XCTAssertTrue(app.buttons[label].exists, "no button \"\(label)\"")
        }
    }

    func testRowsSayTheirRoleOrState() {
        for label in ["Music, submenu", "Extras, submenu", "Settings, submenu", "Shuffle Songs", "Now Playing"] {
            XCTAssertTrue(app.descendants(matching: .any)[label].exists, "no row \"\(label)\"")
        }
        app.wheel.coordinate(withNormalizedOffset: CGVector(dx: 0.1, dy: 0.5))
            .press(forDuration: 0.05, thenDragTo: app.wheel.coordinate(withNormalizedOffset: CGVector(dx: 0.172, dy: 0.270)))
        app.buttons["Select button"].tap()
        for label in ["Clicker, On", "Haptics, On", "About"] {
            XCTAssertTrue(app.descendants(matching: .any)[label].waitForExistence(timeout: 2), "no row \"\(label)\"")
        }
    }

    func testEveryElementHasAUsableDescription() throws {
        try app.performAccessibilityAudit(for: [.sufficientElementDescription])
    }
}
