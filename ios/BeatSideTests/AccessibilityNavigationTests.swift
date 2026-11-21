import Foundation
import Testing
import UIKit
@testable import BeatSide

// FR-027, FR-031, SC-004, SC-007 on iOS, through the real WheelTouchView accessibility elements
// (UITouch and VoiceOver gestures can't be driven from tests; the element API can).
@MainActor
struct AccessibilityNavigationTests {

    final class Recorder: ClickSound, HapticOutput, Announcer {
        var log: [String] = []
        var spoken: [String] = []
        func play() { log.append("click") }
        func tick() { log.append("tick-haptic") }
        func press() { log.append("press-haptic") }
        func prepare() {}
        func announce(_ text: String) { spoken.append(text) }
    }

    let recorder = Recorder()
    let view: WheelTouchView
    let model: BeatSideModel

    init() {
        let recorder = self.recorder
        let model = BeatSideModel(announcer: recorder)
        self.model = model
        view = WheelTouchView(feedback: FeedbackPlayer(sound: recorder, haptics: recorder))
        view.frame = CGRect(x: 0, y: 0, width: 200, height: 200)
        view.layoutIfNeeded()
        view.onEvent = { model.handle($0) }
    }

    private var elements: [UIAccessibilityElement] {
        (view.accessibilityElements ?? []).compactMap { $0 as? UIAccessibilityElement }
    }

    private var wheel: UIAccessibilityElement { elements[0] }

    private func perform(_ name: String) {
        let action = wheel.accessibilityCustomActions?.first { $0.name == name }
        #expect(action != nil, "no action \(name)")
        if let action { _ = action.actionHandler?(action) }
    }

    private func title() -> String { MenuStrings.title(for: model.menuState.current.node) }

    // MARK: FR-031: the wheel element

    @Test func theWheelIsAnAdjustableLabelledElement() {
        #expect(wheel.accessibilityLabel == "Wheel")
        #expect(wheel.accessibilityIdentifier == "click_wheel")
        #expect(wheel.accessibilityTraits.contains(.adjustable))
        #expect(wheel.accessibilityFrameInContainerSpace == view.bounds)
    }

    @Test func theWheelExposesTheFourNamedActions() {
        #expect(wheel.accessibilityCustomActions?.map(\.name) == ["Next item", "Previous item", "Select", "Go back"])
    }

    @Test func swipeUpAndDownMoveTheSelectionWithTheWheelFeedback() {
        wheel.accessibilityIncrement()
        #expect(model.menuState.current.selectedIndex == 1)
        #expect(recorder.log == ["click", "tick-haptic"])
        wheel.accessibilityDecrement()
        #expect(model.menuState.current.selectedIndex == 0)
        #expect(recorder.spoken == ["Extras, submenu", "Music, submenu"])
    }

    @Test func namedActionsGoThroughTheSamePath() {
        perform("Next item")
        perform("Select")
        #expect(title() == "Extras")
        perform("Go back")
        #expect(title() == "BeatSide")
        perform("Previous item")
        #expect(model.menuState.current.selectedIndex == 0)
        #expect(recorder.log == ["click", "tick-haptic", "press-haptic", "press-haptic", "click", "tick-haptic"])
        #expect(recorder.spoken == ["Extras, submenu", "Extras, Clock", "BeatSide, Extras, submenu", "Music, submenu"])
    }

    // MARK: FR-027: the five buttons

    @Test func theFiveButtonsAreLabelledButtonsThatPress() {
        let buttons = Array(elements.dropFirst())
        #expect(buttons.map { $0.accessibilityLabel ?? "" } ==
                ["Select button", "Menu button", "Next button", "Play/Pause button", "Previous button"])
        #expect(buttons.allSatisfy { $0.accessibilityTraits.contains(.button) })
        recorder.log.removeAll()
        _ = buttons[0].accessibilityActivate()
        #expect(title() == "Music")
        #expect(recorder.log == ["press-haptic"])
    }

    // MARK: SC-007: every leaf reachable and left with the actions alone

    @Test func everyLeafCanBeReachedAndLeftUsingOnlyTheAccessibilityActions() {
        var visited: [String] = []

        func explore(_ children: [MenuNode], parent: MenuNode) {
            for (index, child) in children.enumerated() {
                if case .toggle = child { continue }
                for _ in 0..<index { perform("Next item") }
                perform("Select")
                #expect(model.menuState.current.node == child)
                if case .submenu(_, let grandchildren) = child {
                    explore(grandchildren, parent: child)
                } else {
                    visited.append(child.titleKey)
                }
                perform("Go back")
                #expect(model.menuState.current.node == parent)
                for _ in 0..<index { perform("Previous item") }
            }
        }

        guard case .submenu(_, let top) = BeatSideMenu.root else { Issue.record("root"); return }
        explore(top, parent: BeatSideMenu.root)
        #expect(visited.count == 11) // 5 Music + 3 Extras + About + Shuffle Songs + Now Playing
        #expect(!model.menuState.canGoBack)
    }
}
