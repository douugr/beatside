# Implementation Plan: Click Wheel and Menu Navigation

**Branch**: `001-click-wheel-nav` | **Date**: 2026-11-21 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-click-wheel-nav/spec.md`

---

## Summary

Build the first usable slice of BeatSide: a retro click wheel that navigates a full menu tree on
both Android (Kotlin + Jetpack Compose) and iOS (Swift + SwiftUI). Neither platform project
exists yet; this feature creates both from scratch. The core interaction — rotating a finger
to scroll, tapping buttons to open and go back — must feel identical on both platforms because
the touch-to-event translation lives in a shared behavioral contract (`WheelTracker`) that both
platforms implement independently. All feedback, navigation, and accessibility flows through
pure, unit-tested types before any UI code is written.

---

## Technical Context

**Language/Version**:
- Android: Kotlin 2.3.0 (resolved by AGP 9.4.1 built-in Kotlin); no separate `kotlin-android` plugin.
- iOS: Swift 6.0 language mode.

**Primary Dependencies**:
- Android: AGP 9.4.1, Compose BOM 2026.08.00, `activity-compose` 1.10.1, `core-ktx` 1.16.0, JUnit 4.13.2.
- iOS: SwiftUI (system), AVFoundation (system), UIKit (system), XcodeGen (project generation only).

**Storage**: None (settings are session-only; FR-030).

**Testing**:
- Android: JUnit for pure logic; Compose UI test (`androidx.compose.ui.test`) for accessibility.
- iOS: Swift Testing for pure logic; XCUITest for accessibility labels and actions.

**Target Platform**:
- Android: API 26 (minSdk) → API 37 (compileSdk); targetSdk 36; portrait only.
- iOS: iOS 17.0 minimum; iPhone only; portrait only.

**Project Type**: Native mobile app (two independent codebases, one shared spec).

**Performance Goals**: Every tick must produce its click + haptic in the same frame as the
gesture event (Constitution II). No I/O on the main thread.

**Constraints**:
- No DI framework, no navigation library, no persistence layer (Constitution VII).
- Pure core types with no UI framework imports (Constitution III).
- No Apple trademarks in copy or assets (Constitution VI).
- All user-facing strings externalized; English + Brazilian Portuguese (FR-029).
- Gradle wrapper committed; Xcode project NOT committed.

**Scale/Scope**: Two native apps; single `:app` module on Android; single target on iOS.
Approximately 15 source files per platform.

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design below.*

| Principle | Status | Notes |
|---|---|---|
| I. Native per Platform | ✅ Pass | Kotlin + Compose on Android; Swift + SwiftUI on iOS. No cross-platform UI. |
| II. The Wheel Is the Interface | ✅ Pass | Screens consume only `WheelEvent`s. Feedback fires in the same frame. `BackHandler` mirrors MENU. |
| III. Pure Logic, Tested First | ✅ Pass | `WheelTracker`, `MenuNavigator`, `WheelSettings` have no UI imports. Tests written from `contracts/wheel-behavior.md` before implementation. |
| IV. Official APIs and Store Rules | ✅ Pass | `SoundPool`, `AVAudioSession`, `HapticFeedbackConstants`, `UIFeedbackGenerator` — all official APIs. |
| V. Privacy by Default | ✅ Pass | No analytics, no network, no storage. |
| VI. Original Identity | ✅ Pass | No "iPod" or "Click Wheel" anywhere. |
| VII. Small, Shippable Slices | ✅ Pass | This is the minimum viable slice. No code for future music features. |

**Post-Phase-1 re-check**: No violations found in the design. `Complexity Tracking` table omitted
(no violations to justify).

---

## Project Structure

### Documentation (this feature)

```text
specs/001-click-wheel-nav/
├── spec.md               # Accepted feature specification
├── plan.md               # This file
├── research.md           # Toolchain decisions and prototype analysis
├── data-model.md         # Core types (platform-neutral)
├── quickstart.md         # How to build, test, and validate
├── contracts/
│   └── wheel-behavior.md # Shared WheelTracker test cases (both platforms implement verbatim)
└── tasks.md              # Created by /speckit-tasks
```

### Source Code (repository root)

```text
android/
├── gradlew / gradlew.bat          # Committed Gradle wrapper scripts
├── gradle/
│   ├── libs.versions.toml         # Version catalog (AGP 9.4.1, Kotlin 2.3.0, Compose BOM 2026.08.00)
│   └── wrapper/
│       └── gradle-wrapper.properties  # Gradle 9.6.0
├── settings.gradle.kts            # rootProject.name = "BeatSide"; include(":app")
├── build.gradle.kts               # Root build file (plugin declarations only)
└── app/
    ├── build.gradle.kts           # :app module; compileSdk 37, minSdk 26, targetSdk 36
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── kotlin/dev/douglasneto/beatside/
        │   │   ├── MainActivity.kt            # Entry point; hosts the app composable
        │   │   ├── BeatSideApp.kt             # Root composable: state + ClickWheel + LcdScreen
        │   │   ├── wheel/
        │   │   │   ├── WheelButton.kt         # Enum: Center, Menu, Next, Previous, PlayPause
        │   │   │   ├── WheelEvent.kt          # Sealed: Tick, Press, LongPress
        │   │   │   ├── WheelTracker.kt        # Pure gesture logic (no Android imports)
        │   │   │   ├── ClickWheel.kt          # Composable: draws wheel, gesture input, feedback
        │   │   │   └── FeedbackPlayer.kt      # SoundPool + haptics; reads WheelSettings
        │   │   ├── menu/
        │   │   │   ├── MenuNode.kt            # Sealed: Submenu, Screen, Toggle; Route; SettingKey
        │   │   │   ├── BeatSideMenu.kt        # Static tree (FR-022–FR-026)
        │   │   │   ├── MenuState.kt           # MenuFrame, MenuState
        │   │   │   ├── MenuNavigator.kt       # Pure reducer
        │   │   │   └── MenuEffect.kt          # Sealed: ToggleSetting, Unhandled
        │   │   ├── settings/
        │   │   │   └── WheelSettings.kt       # clickerEnabled, hapticsEnabled (session-only)
        │   │   └── ui/
        │   │       ├── LcdScreen.kt           # Title bar + menu list + leaf screen router
        │   │       ├── MenuListContent.kt     # LazyColumn with scroll-to-visible logic
        │   │       └── screens/
        │   │           ├── ClockScreen.kt     # Live clock; DateFormat.is24HourFormat + broadcast
        │   │           ├── AboutScreen.kt     # App name, version, author
        │   │           └── ComingSoonScreen.kt
        │   └── res/
        │       ├── raw/click.wav              # Copied from prototype
        │       ├── values/strings.xml         # English strings (FR-029)
        │       └── values-pt-rBR/strings.xml  # Brazilian Portuguese strings
        └── test/
            └── kotlin/dev/douglasneto/beatside/
                ├── wheel/
                │   └── WheelTrackerTest.kt    # Implements all cases in contracts/wheel-behavior.md
                └── menu/
                    └── MenuNavigatorTest.kt   # Menu reducer unit tests

ios/
├── project.yml                    # XcodeGen spec; generates BeatSide.xcodeproj (not committed)
└── BeatSide/
    ├── Resources/
    │   ├── click.wav              # Copied from prototype
    │   └── Localizable.xcstrings  # String Catalog: en + pt-BR (FR-029)
    ├── Wheel/
    │   ├── WheelButton.swift      # Enum: center, menu, next, previous, playPause
    │   ├── WheelEvent.swift       # Enum: tick, press, longPress
    │   └── WheelTracker.swift     # Pure gesture logic (no UIKit imports)
    ├── Menu/
    │   ├── MenuNode.swift         # Indirect enum: submenu, screen, toggle; Route; SettingKey
    │   ├── BeatSideMenu.swift     # Static tree
    │   ├── MenuState.swift        # MenuFrame, MenuState
    │   ├── MenuNavigator.swift    # Pure reducer
    │   └── MenuEffect.swift       # Enum: toggleSetting, unhandled
    ├── Settings/
    │   └── WheelSettings.swift    # clickerEnabled, hapticsEnabled (session-only)
    ├── UI/
    │   ├── BeatSideView.swift     # Root view: state + ClickWheelView + LcdView
    │   ├── ClickWheelView.swift   # UIViewRepresentable or Canvas; gesture + feedback + a11y
    │   ├── FeedbackPlayer.swift   # AVAudioPlayer + UIFeedbackGenerator; reads WheelSettings
    │   ├── LcdView.swift          # Title bar + content router
    │   ├── MenuListView.swift     # List with scroll-to-visible
    │   └── Screens/
    │       ├── ClockView.swift    # Live clock; Date.FormatStyle; locale change observer
    │       ├── AboutView.swift
    │       └── ComingSoonView.swift
    └── BeatSideApp.swift          # @main entry point

BeatSideTests/
    ├── WheelTrackerTests.swift    # Implements all cases in contracts/wheel-behavior.md
    └── MenuNavigatorTests.swift

.github/
└── workflows/
    └── ci.yml                     # Android unit tests + iOS XcodeGen + tests
```

**Structure Decision**: Each platform gets its own full project under `android/` and `ios/`. The
`specs/` tree holds shared design artifacts. There is no shared source code between platforms;
the shared contract is `contracts/wheel-behavior.md`.

---

## Design Decisions

### WheelTracker — fix applied to prototype

The prototype's `longPressTimeout` fires for all five buttons. Per FR-010, only `Center`
triggers a long-press. Fix: guard with `if (startButton != Center) return []`.
Everything else in the prototype WheelTracker is reusable as-is.

### MenuNavigator — reuse as-is

The prototype's pure reducer already satisfies FR-012 through FR-016. The `AppSettings` class
is replaced by the simpler `WheelSettings` (removes the `sensitivity` field, out of scope).

### FeedbackPlayer — single responsibility

Extracted from `ClickWheel` into its own class so it can be called from both physical gestures
and accessibility actions with identical behavior (derived requirement from FR-031 clarification).

### Clock — platform-specific time formatting

- Android: `DateFormat.is24HourFormat(context)` + `BroadcastReceiver` for `ACTION_TIME_CHANGED`.
- iOS: `Date.FormatStyle` + `NotificationCenter.default.publisher(for: NSLocale.currentLocaleDidChangeNotification)`.

Both re-read the preference without restarting the app (FR-024).

### Accessibility (FR-031)

- Android: `Modifier.semantics { customActions = listOf(...) }` on `ClickWheel`. After every
  navigation action, post an accessibility announcement with the new row's label.
- iOS: `ClickWheelView` conforms to UIAccessibility adjustable protocol; `accessibilityIncrement`
  / `accessibilityDecrement` map to ticks; named actions "Select" and "Go back" via
  `accessibilityCustomActions`. Post `.announcement` after each action.

### Strings

Prototype strings are hardcoded. All user-facing text must be moved to:
- Android: `res/values/strings.xml` (en) + `res/values-pt-rBR/strings.xml` (pt-BR).
- iOS: `Localizable.xcstrings` with `en` and `pt-BR` keys.

### CI

`.github/workflows/ci.yml` with two jobs:
1. `android-test`: `ubuntu-latest`, JDK 17, `./gradlew :app:testDebugUnitTest`.
2. `ios-test`: `macos-latest`, Homebrew XcodeGen, `xcodegen generate && xcodebuild test`.
Both triggered on `pull_request` to `main`.

---

## Implementation Sequence

The tasks file (created by `/speckit-tasks`) will reflect this ordering:

1. **Write shared test contract** (`contracts/wheel-behavior.md` already done in this plan).
2. **Android core** (pure types: WheelTracker fix, MenuNavigator, WheelSettings) — tests first.
3. **iOS core** (same types in Swift) — same test cases, fail first.
4. **Android UI** (ClickWheel, FeedbackPlayer, LcdScreen, leaf screens, strings, Clock).
5. **iOS UI** (same components in SwiftUI, String Catalog, Clock).
6. **Accessibility** (both platforms): actions, announcements, labels.
7. **CI workflow**.
8. **Localization** (pt-BR strings on both platforms).
