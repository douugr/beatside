# Quickstart Validation Guide: Click Wheel and Menu Navigation

**Feature**: 001-click-wheel-nav | **Date**: 2026-11-21

---

## Prerequisites

| Tool | Minimum version | Install |
|---|---|---|
| JDK | 17 | `brew install openjdk@17` or via Android Studio |
| Android Studio | Ladybug or later | For emulator / device runs |
| Xcode | 16+ | From the Mac App Store |
| XcodeGen | latest | `brew install xcodegen` |
| Homebrew | any | Required for XcodeGen on macOS |

---

## Android

### 1. Build and run unit tests

```bash
cd android
./gradlew :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`. All tests in `WheelTrackerTest` and `MenuNavigatorTest` pass.

### 2. Run on a device or emulator

Open `android/` as an Android Studio project. Run the `:app` configuration on any API 26+
device or emulator. The app opens to the root menu ("BeatSide") with a click wheel below.

### 3. Manual wheel smoke test

1. Rotate a finger clockwise on the wheel ring → selection moves down one row per 15° of travel.
2. Tap the MENU ring area (top) → nothing at root; in a submenu, go back.
3. Tap the center button on "Settings" → Settings submenu opens.
4. Toggle "Clicker" off → rotate the wheel → no click sound; haptic still fires.
5. Navigate to Extras → Clock → current time displayed, updating every second.

### 4. Accessibility smoke test

Enable TalkBack on the device. Focus the wheel (swipe to it). Use TalkBack's custom actions
menu: verify "Next item", "Previous item", "Select", and "Go back" are available. Invoke
"Next item" → TalkBack announces the newly selected row name.

---

## iOS

### 1. Generate the Xcode project

```bash
cd ios
xcodegen generate
```

Expected: `ios/BeatSide.xcodeproj` created with no errors.

### 2. Build and run unit tests

```bash
xcodebuild test \
  -project ios/BeatSide.xcodeproj \
  -scheme BeatSide \
  -destination 'platform=iOS Simulator,name=iPhone 16'
```

Expected: all tests in `WheelTrackerTests` and `MenuNavigatorTests` pass.

### 3. Run on a simulator or device

Open `ios/BeatSide.xcodeproj` in Xcode. Run on an iPhone 17 simulator (or any iPhone; portrait
only). The same root menu and wheel appear.

### 4. Manual wheel smoke test (same as Android)

Same five steps as the Android section above.

### 5. Accessibility smoke test

Enable VoiceOver on the simulator (Settings → Accessibility → VoiceOver). Focus the wheel.
Swipe up and down (adjustable): selection moves up/down. Verify VoiceOver announces the row
name. Use the "Actions" rotor to find "Select" and "Go back".

---

## CI validation

Push a branch to GitHub. The workflow `.github/workflows/ci.yml` runs:

1. **android-test** (ubuntu-latest): `./gradlew :app:testDebugUnitTest` — must pass.
2. **ios-test** (macos-latest): `xcodegen generate && xcodebuild test …` — must pass.

Both jobs must be green before a pull request to `main` can be merged.

---

## Acceptance checklist (SC-001 to SC-007)

| Criterion | How to verify |
|---|---|
| SC-001: reach any leaf in ≤ 10 gestures | Manual: start from root, count inputs to Clock, About, etc. |
| SC-002: 15° = exactly 1 tick | Automated: `WheelTrackerTest.D-1` (50° → 3 ticks) |
| SC-003: Setting change takes effect immediately | Manual: toggle Clicker, rotate wheel once |
| SC-004: accessibility labels present | Automated: Compose UI test on Android; XCUITest on iOS |
| SC-005: no missing strings in pt-BR | Manual: set device to Portuguese (Brazil), navigate all menus |
| SC-006: Clock shows correct time and format | Manual: open Clock, observe 5 seconds; change 12/24-hour setting |
| SC-007: screen-reader full-tree navigation | Manual TalkBack/VoiceOver + accessibility action test |

---

## Key file references

| Artifact | Path |
|---|---|
| Wheel behavior contract | `specs/001-click-wheel-nav/contracts/wheel-behavior.md` |
| Data model | `specs/001-click-wheel-nav/data-model.md` |
| Spec | `specs/001-click-wheel-nav/spec.md` |
| Android source | `android/app/src/main/kotlin/dev/douglasneto/beatside/` |
| Android tests | `android/app/src/test/kotlin/dev/douglasneto/beatside/` |
| iOS source | `ios/BeatSide/` |
| iOS tests | `ios/BeatSideTests/` |
