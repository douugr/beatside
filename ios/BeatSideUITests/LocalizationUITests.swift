import XCTest

/// SC-005 / FR-028 / FR-029: the running app in Brazilian Portuguese.
@MainActor
final class LocalizationUITests: XCTestCase {

    func testTheAppIsInBrazilianPortuguese() {
        let app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(pt-BR)", "-AppleLocale", "pt_BR"]
        app.launch()
        XCTAssertTrue(app.wheel.waitForExistence(timeout: 5), "wheel not found")
        XCTAssertEqual(app.wheel.label, "Roda")
        for label in ["Música, submenu", "Extras, submenu", "Configurações, submenu", "Músicas aleatórias", "Tocando agora"] {
            XCTAssertTrue(app.descendants(matching: .any)[label].exists, "no row \"\(label)\"")
        }
        XCTAssertTrue(app.buttons["Botão Menu"].exists)
    }
}
