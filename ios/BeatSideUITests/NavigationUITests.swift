import XCTest

/// US2 (open items and go back) driven through the real app with touches on the wheel.
/// iOS has no system back gesture for this (FR-014 is Android-only).
@MainActor
final class NavigationUITests: XCTestCase {

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

    /// A point on the ring at `degrees` (screen coordinates, -90° is the top), 80% of the radius.
    private func ring(_ degrees: Double) -> XCUICoordinate {
        let rad = degrees * .pi / 180
        return wheel.coordinate(withNormalizedOffset: CGVector(dx: 0.5 + 0.4 * cos(rad), dy: 0.5 + 0.4 * sin(rad)))
    }

    private var center: XCUICoordinate { wheel.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)) }

    private func tapMenu() { ring(-90).tap() }

    /// Two clockwise detents: a 35° drag along the left of the ring (35 / 15 = 2.33).
    private func rotateTwoDetents() {
        ring(180).press(forDuration: 0.05, thenDragTo: ring(215))
    }

    private func shows(_ text: String, file: StaticString = #filePath, line: UInt = #line) {
        XCTAssertTrue(app.labelled(text).waitForExistence(timeout: 2), "expected \"\(text)\"", file: file, line: line)
    }

    private func hides(_ text: String, file: StaticString = #filePath, line: UInt = #line) {
        XCTAssertFalse(app.labelled(text).exists, "unexpected \"\(text)\"", file: file, line: line)
    }

    func testCenterOpensTheHighlightedSubmenu() {
        center.tap()
        shows("Playlists")
        hides("Extras")
    }

    func testCenterOnSettingsOpensItAndMenuReturnsToTheRoot() {
        rotateTwoDetents()
        center.tap()
        shows("Clicker")
        shows("Haptics")
        hides("Music")

        tapMenu()
        shows("Music")
        shows("Settings")
        hides("Clicker")
    }

    func testMenuAtTheRootDoesNothing() {
        tapMenu()
        shows("BeatSide")
        shows("Music")
        shows("Now Playing")
    }

    func testCenterOnALeafShowsComingSoonAndMenuGoesBack() {
        center.tap() // Music
        shows("Playlists")
        center.tap() // Playlists
        shows("Coming soon")
        shows("Press MENU to go back.")
        hides("Artists")

        tapMenu()
        shows("Artists")
        hides("Coming soon")
    }

    func testLongPressOnCenterDoesNotOpenAnything() {
        center.press(forDuration: 0.8)
        shows("Music")
        hides("Playlists")
    }

    func testNextPreviousAndPlayPauseDoNotNavigate() {
        ring(0).tap()
        ring(180).tap()
        ring(90).tap()
        shows("Music")
        hides("Playlists")
    }
}
