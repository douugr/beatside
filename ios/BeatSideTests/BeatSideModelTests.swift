import Testing
@testable import BeatSide

// BeatSideModel routes wheel events through MenuNavigator, applies toggles, and announces the new
// row or screen with the catalog strings (English on the test simulator).
@MainActor
struct BeatSideModelTests {

    final class FakeAnnouncer: Announcer {
        var spoken: [String] = []
        func announce(_ text: String) { spoken.append(text) }
    }

    let announcer = FakeAnnouncer()
    var model: BeatSideModel { BeatSideModel(announcer: announcer) }

    @Test func eventsNavigateAndAnnounceWithTheCatalogStrings() {
        let model = self.model
        model.handle(.tick(+1))
        model.handle(.press(.center))
        model.handle(.press(.center))
        model.handle(.press(.menu))
        model.handle(.press(.menu))
        model.handle(.tick(-1))
        model.handle(.tick(-1)) // clamped: nothing
        #expect(announcer.spoken == ["Extras, submenu", "Extras, Clock", "Clock", "Extras, Clock",
                                     "BeatSide, Extras, submenu", "Music, submenu"])
        #expect(!model.menuState.canGoBack)
    }

    @Test func centerOnAToggleFlipsTheSettingAndAnnouncesIt() {
        let model = self.model
        model.handle(.tick(+2))
        model.handle(.press(.center))
        model.handle(.press(.center))
        #expect(!model.settings.clickerEnabled)
        #expect(announcer.spoken.last == "Clicker, Off")
    }
}
