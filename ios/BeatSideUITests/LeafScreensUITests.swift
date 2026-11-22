import XCTest

/// US4 leaf screens (T040-T042): Clock and About are real; every other leaf is Coming soon.
/// The 12/24-hour switch can't be driven from XCUITest; it is covered by ClockFormatTests and
/// by hand (see NOTES.md).
@MainActor
final class LeafScreensUITests: XCTestCase {

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
    private func tapMenu() { ring(-90).tap() }

    private func rotate(detents: Int) {
        ring(180).press(forDuration: 0.05, thenDragTo: ring(180 + Double(detents * 15 + 5)))
    }

    private func shows(_ text: String, file: StaticString = #filePath, line: UInt = #line) {
        XCTAssertTrue(app.labelled(text).waitForExistence(timeout: 2), "expected \"\(text)\"", file: file, line: line)
    }

    private func label(matching regex: String) -> XCUIElement {
        app.staticTexts.matching(NSPredicate(format: "label MATCHES %@", regex)).firstMatch
    }

    func testClockShowsALiveTime() {
        rotate(detents: 1) // Extras
        tapCenter()
        tapCenter() // Clock
        let time = label(matching: #"\d{1,2}:\d{2}:\d{2}( (AM|PM))?"#)
        XCTAssertTrue(time.waitForExistence(timeout: 3), "no time label")
        let first = time.label
        let changed = expectation(for: NSPredicate(format: "label != %@", first), evaluatedWith: time)
        wait(for: [changed], timeout: 3)
        XCTAssertFalse(app.staticTexts["Coming soon"].exists)
    }

    func testAboutShowsNameVersionAndAuthor() {
        rotate(detents: 2) // Settings
        tapCenter()
        rotate(detents: 2) // About
        tapCenter()
        shows("BeatSide")
        XCTAssertTrue(label(matching: #"Version \d+\.\d+\.\d+"#).waitForExistence(timeout: 2), "no version")
        shows("by Douglas Rodrigues Neto")
        XCTAssertFalse(app.staticTexts["Coming soon"].exists)
    }

    func testStopwatchAndGamesStayComingSoon() {
        rotate(detents: 1) // Extras
        tapCenter()
        rotate(detents: 1) // Stopwatch
        tapCenter()
        shows("Coming soon")
        tapMenu()
        rotate(detents: 1) // Games
        tapCenter()
        shows("Coming soon")
    }

    func testShuffleSongsAndNowPlayingStayComingSoon() {
        rotate(detents: 3) // Shuffle Songs
        tapCenter()
        shows("Coming soon")
        tapMenu()
        rotate(detents: 1) // Now Playing
        tapCenter()
        shows("Coming soon")
    }
}
