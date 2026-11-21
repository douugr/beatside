# Research: Click Wheel and Menu Navigation

**Feature**: 001-click-wheel-nav | **Date**: 2026-11-21

---

## 1. Toolchain Versions

### Decision: Start with the prototype's candidate toolchain (unverified — build must be confirmed)

The `prototype/wheel-and-menus` branch contains an Android Gradle project that was **never
compiled**. Its `libs.versions.toml` and Gradle wrapper pins were chosen deliberately for AGP 9
"built-in Kotlin" mode and are taken as a starting point, not as a proven build. The versions
below are candidates; task T001a verifies them by running `./gradlew :app:assembleDebug` and
updating this table with the actual working versions.

| Component | Candidate version | Notes |
|---|---|---|
| Android Gradle Plugin | 9.4.1 | AGP 9 compiles Kotlin itself; no `kotlin-android` plugin needed |
| Kotlin | 2.3.0 | Must match the version AGP 9.4.1 resolves internally; also drives the Compose compiler plugin |
| Gradle wrapper | 9.6.0 | From `gradle-wrapper.properties` in the prototype |
| Compose BOM | 2026.08.00 | Determines all Compose artifact versions; pins `compileSdk = 37` |
| `compileSdk` | 37 | Required by Compose BOM 2026.08.00 |
| `targetSdk` | 36 | Current Google Play policy requirement |
| `minSdk` | 26 | Specified in the feature requirements |
| `activity-compose` | 1.10.1 | From prototype |
| `core-ktx` | 1.16.0 | From prototype |
| `robolectric` | 4.14.1 | For Compose semantics tests in the unit-test job (T050) |
| iOS deployment target | 17.0 | Specified; covers `sensoryFeedback` and Swift 6 |
| Swift language mode | 6.0 | Specified; enforced via `SWIFT_VERSION = 6.0` in project.yml |
| XcodeGen | latest stable | Generates `ios/BeatSide.xcodeproj`; not committed |

**⚠️ Build confirmation required (T001a)**: After scaffolding the Android project (T001), run
`./gradlew :app:assembleDebug`. If the build fails due to version incompatibilities, adjust
`libs.versions.toml` and `gradle-wrapper.properties` until it succeeds, then update the
"Candidate version" column above with the confirmed working values.

**Rationale**: The prototype's configuration was written with AGP 9 "built-in Kotlin" mode in
mind and represents a reasonable starting point. However, until a successful build is confirmed,
no version should be treated as authoritative.

**Alternatives considered**: Upgrading to any newer patch version would require re-verifying
Kotlin/AGP compatibility. Deferred to the next feature unless a security or build issue arises.

---

## 2. Prototype Analysis

The `prototype/wheel-and-menus` branch contains five files directly relevant to this feature.
Each is assessed below. **The prototype was never compiled; all verdicts are based on code review
only. Any file marked "Reusable" must still pass the contract tests (T006/T008) before being
accepted.**

### Android — `WheelTracker.kt`

**Verdict: Reusable with one fix (pending build verification).**

The logic is correct for this spec:
- Accumulator-based detent counting (FR-003, FR-005).
- `normalize` mapping to (−180°, 180°] (FR-004).
- Dead-zone drop-and-reanchor at 20 % of radius (FR-006).
- Touch-starts-on-center suppresses rotation (FR-007).
- Tap detection: gesture ends without tick and within 8 dp slop (FR-008).
- Button zones with correct half-open intervals (FR-009).
- `longPressTimeout` for 500 ms (FR-010).

**Fix required (FR-010)**: `longPressTimeout()` currently fires `LongPress(startButton)` for
*any* button, including ring buttons. The spec restricts long-press to `Center` only. Fix: guard
with `if (startButton != WheelButton.Center) return emptyList()` inside `longPressTimeout`.

**Other deltas**: The `WheelSensitivity` enum includes Low/Normal/High variants and is referenced
in `ClickWheel`. Since sensitivity options are out of scope, keep the enum but hard-wire Normal
in the ClickWheel and do not expose it in `WheelSettings`.

### Android — `MenuNavigator.kt` and `MenuNode.kt`

**Verdict: Reusable with one structural change.**

`MenuNavigator` is a pure reducer that already satisfies FR-012 through FR-016. `MenuNode` and
`BeatSideMenu.root` define the full menu tree required by FR-022 through FR-026. `AppSettings`
covers `WheelSettings` (FR-030); remove the `sensitivity` field which is out of scope.

**Change required (FR-029 / data-model.md)**: The prototype's `MenuNode` stores `title: String`
with hardcoded English literals. This must be changed to `titleKey: String` (a string resource
key) so the UI layer can resolve it via `stringResource(titleKey)`. See data-model.md §MenuNode.

### Android — `ClickWheel.kt`

**Verdict: Reusable with additions.**

The drawing, gesture tracking, long-press timeout, and feedback dispatch are correct. Additions
needed: (a) semantics block with four custom accessibility actions (FR-031); (b) accessibility
announcement on selection change (FR-031 derived); (c) haptic constant upgrade for API 34+
(`SEGMENT_TICK` for ticks, `CONFIRM` or `KEYBOARD_PRESS` for presses).

### Android — `LcdScreen.kt`

**Verdict: Reusable with additions.**

The LCD layout, menu list (with scroll-to-visible logic), and leaf screens are correct.
Additions needed: (a) rows must resolve `child.titleKey` via `stringResource(child.titleKey)`
instead of using `child.title` directly (FR-029); (b) fix `ClockScreen` to use
`DateFormat.is24HourFormat(context)` and re-read it on `TIME_TICK` broadcast (FR-024).
Menu row accessibility labels must include role and state (FR-028).

### iOS — `WheelTracker.swift`

**Verdict: Reusable with the same one fix as Android.**

The Swift implementation mirrors the Kotlin one exactly (same constants, same algorithm). Apply
the same FR-010 fix: guard `longPressTimeout` to return `[]` unless `startButton == .center`.

---

## 3. Sound and Haptics

### Android

- **Click sound**: `SoundPool` with `USAGE_ASSISTANCE_SONIFICATION` and
  `CONTENT_TYPE_SONIFICATION` does not duck or stop other audio (confirmed in prototype).
  The WAV file at `res/raw/click.wav` will be copied from the prototype.
- **Tick haptic**: API 34+ constant `HapticFeedbackConstants.SEGMENT_TICK` for a light detent
  feel; fallback to `CLOCK_TICK` on API 26–33.
- **Press haptic**: `HapticFeedbackConstants.CONFIRM` (API 33+) or `VIRTUAL_KEY` (older).

### iOS

- **Click sound**: `AVAudioSession` category `.ambient` (mixes with other audio). Load the same
  `click.wav` with `AVAudioPlayer` or `SystemSoundID`; the prototype ships the asset at
  `ios/BeatSide/Resources/click.wav`.
- **Tick haptic**: `UISelectionFeedbackGenerator` or SwiftUI `.sensoryFeedback(.selection, ...)`.
- **Press haptic**: `UIImpactFeedbackGenerator(.medium)` or `.sensoryFeedback(.impact(weight: .medium), ...)`.

---

## 4. Clock Format

### Android

`DateFormat.is24HourFormat(context)` reads the system preference. Register a
`BroadcastReceiver` for `Intent.ACTION_TIME_CHANGED` and `Intent.ACTION_TIMEZONE_CHANGED` while
the Clock screen is composed; use `DisposableEffect` to unregister on dispose.

### iOS

`Date().formatted(.dateTime.hour().minute().second())` with the current `Locale` automatically
respects the user's 12/24-hour preference. Observe `NSLocale.currentLocaleDidChangeNotification`
to trigger a re-render if the preference changes while the screen is open.

---

## 5. Accessibility

### Android

Compose `Modifier.semantics { ... }` with `customActions` for the four wheel actions:
"Next item", "Previous item", "Select", "Go back". Each action dispatches the equivalent
`WheelEvent`. After every tick triggered by an accessibility action, call
`view.announceForAccessibility(rowTitle)` or use a live region on the selection row so TalkBack
reads the new row name without a manual call. `rowTitle` is resolved via
`context.getString(child.titleKey)`.

### iOS

Implement `UIAccessibility` adjustable actions: `accessibilityIncrement()` → "Next item" tick,
`accessibilityDecrement()` → "Previous item" tick. Add named actions "Select" and "Go back" via
`accessibilityCustomActions`. Post `UIAccessibility.post(notification: .announcement, argument:
rowTitle)` after any navigation action to announce the newly focused row. `rowTitle` is resolved
via `String(localized: child.titleKey)`.

---

## 6. CI

- **Android job**: `ubuntu-latest`, JDK 17, `./gradlew :app:testDebugUnitTest`. Compose
  accessibility tests (T050) run here via Robolectric — no emulator required.
- **iOS unit-test job**: `macos-latest`, XcodeGen via Homebrew, `xcodegen generate`, then
  `xcodebuild test` with the `BeatSide` scheme (unit tests in `BeatSideTests`).
- **iOS UI-test job**: same macOS runner, `xcodebuild test` with the `BeatSideUITests` scheme
  (XCUITest accessibility tests in `BeatSideUITests` — `bundle.ui-testing` target).
- All jobs run on `pull_request` targeting `main`.
