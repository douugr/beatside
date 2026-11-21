import XCTest

/// US3 Settings (T035): toggle rows show On/Off and flip immediately on Center (FR-020, FR-021).
/// Sound and haptics themselves are covered by FeedbackPlayerTests / WheelTouchViewTests.
@MainActor
final class SettingsUITests: XCTestCase {

    private var app: XCUIApplication!
    private var wheel: XCUIElement!

    override func setUp() async throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launch()
        wheel = app.wheel
        XCTAssertTrue(wheel.waitForExistence(timeout: 5), "wheel not found")
    }

    private func ring(_ degrees: Double) -> XCUICoordinate {
        let rad = degrees * .pi / 180
        return wheel.coordinate(withNormalizedOffset: CGVector(dx: 0.5 + 0.4 * cos(rad), dy: 0.5 + 0.4 * sin(rad)))
    }

    private func tapCenter() { wheel.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap() }

    /// Clockwise detents along the left of the ring: 15° each plus 5° margin.
    private func rotate(detents: Int) {
        ring(180).press(forDuration: 0.05, thenDragTo: ring(180 + Double(detents * 15 + 5)))
    }

    private func count(_ state: String) -> Int { app.rowCount(withState: state) }

    private func openSettings() {
        rotate(detents: 2)
        tapCenter()
        XCTAssertTrue(app.labelled("Clicker").waitForExistence(timeout: 2))
    }

    func testSettingsShowsClickerAndHapticsOnByDefault() {
        openSettings()
        XCTAssertEqual(count("On"), 2)
        XCTAssertEqual(count("Off"), 0)
    }

    func testCenterOnAToggleFlipsItsRowBetweenOnAndOff() {
        openSettings()
        tapCenter() // Clicker
        XCTAssertTrue(app.labelled("Clicker, Off").waitForExistence(timeout: 2))
        XCTAssertEqual(count("On"), 1)
        XCTAssertEqual(count("Off"), 1)

        rotate(detents: 1) // Haptics
        tapCenter()
        XCTAssertEqual(count("Off"), 2)

        tapCenter() // Haptics back On
        XCTAssertEqual(count("On"), 1)
        XCTAssertTrue(app.labelled("Clicker").exists, "toggles never navigate")
    }
}
