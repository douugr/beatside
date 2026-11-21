# Implementation notes: 001-click-wheel-nav

## Run of 2026-11-21: T008–T015 (Phase 2, Foundational)

Stopped after T015 as instructed. Phase 3 (T016+) has not been started.

### Tasks done

| Task | Result | Commit |
|---|---|---|
| T008 | iOS `WheelTrackerTests.swift`: 42 tests covering all 40 contract cases (RH-2 split per button, as on Android). Confirmed failing (`cannot find type 'WheelTracker'`) before T009. | 8494c9e |
| T009 | iOS `WheelButton`, `WheelEvent`, `WheelTracker` (pure struct, Foundation only; FR-010 guard). All 42 pass. | 8494c9e |
| T010 | Android `MenuNavigatorTest.kt`: 16 tests, every row of the data-model.md table plus the `BeatSideMenu` tree. Confirmed failing (unresolved types) before T011. | 6b18061 |
| T011 | Android `MenuNode` (`titleKey`), `Route`, `SettingKey`, `MenuFrame`, `MenuState`, `MenuEffect`, `BeatSideMenu`, `MenuNavigator` (+ `MenuResult`). All pass. | 6b18061 |
| T012 | Android `WheelSettings`. Added `WheelSettingsTest.kt` (5 tests) written and failed first. | 6b18061 |
| T013 | iOS `MenuNavigatorTests.swift`: the same 16 cases. Confirmed failing before T014. | this commit |
| T014 | iOS menu types, `BeatSideMenu` (same keys as Android) and `MenuNavigator` (+ `MenuResult`). All pass. | this commit |
| T015 | iOS `WheelSettings`. Added `WheelSettingsTests.swift` (5 tests) written and failed first. | this commit |

### Test counts at the Phase 2 checkpoint

| Platform | Command | Tests | Result |
|---|---|---|---|
| Android | `./gradlew :app:testDebugUnitTest` | 63 (WheelTracker 42, MenuNavigator 16, WheelSettings 5) | all pass |
| iOS | `xcodegen generate` + `xcodebuild test -scheme BeatSide` (iPhone 16, iOS 18.1 sim) | 63 (WheelTracker 42, MenuNavigator 16, WheelSettings 5) | all pass |

### Decisions and deviations to review

1. **`ios/BeatSide/BeatSideApp.swift` stub created early (T025 territory).** The `BeatSide` app
   target had no Swift sources, so the test bundle failed with `unable to resolve module
   dependency: 'BeatSide'` before any test could compile. I added a minimal `@main` App with an
   `EmptyView()` window (no strings) so the module exists. T025 should replace its body.
2. **Pre-existing untracked `WheelTrackerTests.swift`** (from an earlier session) was used as
   the T008 starting point after review. Two compile fixes, no assertion changes: `.isEmpty()`
   to `.isEmpty`; mutating calls moved out of `#expect(...)` into locals (the macro captures
   the tracker immutably).
3. **`Press(Center)` on a leaf Screen returns `Unhandled`.** The data-model.md table doesn't list
   this cell explicitly. I followed T010's "Screen→Unhandled" and the prototype: on a Screen,
   every event except `Press(Menu)` is `Unhandled`, so the screen can consume it. Both platforms
   test this (`center on a screen is unhandled`). If you want different behavior, update
   data-model.md and both tests.
4. **WheelSettings tests added** (5 per platform). The tasks list no test task for T012/T015, but
   Constitution III requires tests-first for state reducers. I also added `isOn(key)`, which the
   "On"/"Off" row suffix (FR-017, T017/T022) needs.
5. **`MenuResult`** wraps the `(MenuState, MenuEffect?)` pair from the data model on both
   platforms. It lives in `MenuNavigator.kt` / `MenuNavigator.swift`.

### Open items

- ~~**iOS simulator selection by name.**~~ Resolved in 6d3aa39 (CI picks an iPhone on the newest
  runtime by UDID). Original note: locally, `-destination 'platform=iOS Simulator,name=iPhone 16'`
  fails because a name-only destination means `OS:latest` (iOS 26.5 here) and no iPhone 16
  exists on that runtime. Local runs used the device id instead. CI's "first available iPhone"
  pick could hit the same mismatch on a runner image; consider pinning `OS=` or using the
  device id from `simctl` (T052 covers CI finalization).
- The `NSBundle … principal class is nil` log line during iOS tests is harmless (Swift
  Testing bundles have no XCTest principal class).

## Run of 2026-11-21: T016–T025 (Phase 3, US1)

Stopped after T025 as instructed. Phase 4 (T026+) has not been started.

### Tasks done

| Task(s) | Result | Commit |
|---|---|---|
| (extra) | Root titleKey renamed `beatside` → `app_name` on both platforms, plus data-model.md and tasks.md. MenuNavigator tests changed first and failed (1 per platform). | f81ad14 |
| T016–T020 | Android `ClickWheel`, `MenuListContent`, `LcdScreen`, `BeatSideApp`, `MainActivity`, plus `MenuStrings` (explicit titleKey → `R.string` map), `ListScroll` (pure FR-016 math), `LcdColors`, English `strings.xml`. | f4994b0, b7ff249 |
| T021–T025 | iOS `ClickWheelView`, `MenuListView`, `LcdView`, `BeatSideView`, `BeatSideApp`, plus `MenuStrings` (`String(localized:)`), `LcdColors`, English `Localizable.xcstrings`. | 15f41e0 |

Tests written first and seen failing: `MenuStringsTest` / `MenuStringsTests` (walk all 17
BeatSideMenu nodes; Android also checks each mapping points at the same-named resource),
`ListScrollTest` (7 cases). iOS catalog tests were seen failing with an empty catalog (3 failures)
before the entries were added.

### Test counts at the Phase 3 checkpoint

| Platform | Tests | Result |
|---|---|---|
| Android | 79: WheelTracker 42, MenuNavigator 16, WheelSettings 5, ListScroll 7, MenuStrings 3, ClickWheel 6 (Robolectric) | all pass **on JDK 22**; on JDK 25 the 6 ClickWheel tests fail (see open item) |
| iOS | 67: WheelTracker 42, MenuNavigator 16, WheelSettings 5, MenuStrings 4 | all pass (iPhone 16, iOS 18.1 sim) |

### Decisions and deviations to review

1. **`ClickWheelTest` (Robolectric) added beyond the tasks.** No emulator AVD exists on this Mac,
   so this was the only way to exercise the Compose gesture glue (pointerInput, long-press
   timeout) with real touch input. It covers: 50° drag → 3 ticks, 20° CCW → 1 tick up, center
   tap, MENU tap, center hold → LongPress only, ring hold → Press only. Written after
   `ClickWheel.kt` (verification of UI glue, not pure logic). Added
   `debugImplementation(ui-test-manifest)`, which `createComposeRule()` needs (T050 needs it too).
2. **`ListScroll` pure helper (Android).** Compose has no "scroll the minimum amount" call, so the
   math lives in a pure, tested function; rows are a fixed 34 dp so positions are exact. iOS uses
   `ScrollViewProxy.scrollTo(_:)` with a nil anchor, which already scrolls minimally.
3. **Android theme** changed to `Theme.DeviceDefault.Light.NoActionBar` so the platform action
   bar doesn't duplicate the LCD title bar.
4. **iOS long press uses a `Task` sleep** instead of `DispatchWorkItem` (T021 wording): same 500 ms
   behaviour and cancellable, and it compiles cleanly under Swift 6 strict concurrency.
5. **iOS wheel input uses a UIKit touch surface** (`WheelTouchView`) under a SwiftUI-drawn face.
   Raw touches give exact began/moved/ended/cancelled mapping to `WheelTracker`. T046 will
   also add the adjustable accessibility trait there. Ring glyphs are SF Symbols on iOS and drawn
   paths on Android; "MENU" is a localized string (`wheel_menu`) on both.
6. **Phase 3 routes ticks only** (per T019/T024). Center/MENU presses are emitted but ignored until
   T026/T028, so you can't open submenus yet.
7. **List keyed per stack level** on both platforms so a newly opened submenu starts at the top
   (b7ff249). This doesn't matter until Phase 4, but it keeps the two platforms the same.
8. Extra string keys added now (English only): `setting_on`, `setting_off`, `wheel_menu`.

### Open items

- ~~**JDK for Robolectric.**~~ Resolved 2026-11-21: the Gradle daemon is pinned to JDK 21 in
  `android/gradle/gradle-daemon-jvm.properties`, and CI's `android-test` job now sets up
  Temurin 21. A plain `./gradlew :app:testDebugUnitTest` passes all 79 tests from a JDK 25 shell.
  The file was written by hand: `./gradlew updateDaemonJvm --jvm-version=21` fails with "Toolchain
  download repositories have not been configured", because it also writes JDK download URLs,
  and those need a toolchain resolver plugin (e.g. foojay) that this build does not use. Without
  URLs, a machine with no JDK 21 installed gets a clear Gradle error instead of an automatic
  download. Original note: Gradle here runs on JDK 25, and Robolectric
  4.14.1 fails with `Unsupported class file major version 69`. I tried three times (JDK 22
  run, which works; a JUnit `@BeforeClass` assumption, which compiled only after a fix and then
  skipped just 1 of 6). Then I stopped, per the three-failure rule, and removed the guard. To run the full
  Android suite now: `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home ./gradlew :app:testDebugUnitTest`.
  CI (JDK 17) is unaffected. Options: pin a Gradle daemon/toolchain JVM (17 or 21), or move to
  a Robolectric release that supports JDK 25. T050 depends on this too.
- ~~**Rotation not tried by hand on either platform.**~~ Resolved 2026-11-21: Douglas tested the
  wheel by hand on a physical iPhone and a physical Android phone; rotation, boundary crossing,
  jitter, center crossing and clamping all behaved as specified on both. Original note: Android has no AVD here; on iOS I launched
  the app and checked the layout by screenshot (root list, highlight, wheel), but couldn't inject
  drags into the simulator. Android rotation is covered by `ClickWheelTest`; the iOS touch
  surface has no automated test yet.

### How to see the result

- **Android Studio:** open `android/`, let Gradle sync, choose the `app` run configuration
  (created automatically for the application module) and run on an emulator or device (API 26+).
  The Gradle daemon JVM is pinned to 21 by `gradle/gradle-daemon-jvm.properties`.
- **Xcode:** `cd ios && xcodegen generate && open BeatSide.xcodeproj`, choose the `BeatSide`
  scheme and an iPhone simulator, then Run (⌘R). ⌘U runs the unit tests.
- **What to expect:** the root menu ("BeatSide" title, Music highlighted). Drag a finger around
  the ring: the highlight moves one row per 15°, clockwise down, and stops at the first/last
  row. Taps do nothing yet, and there's no click or haptic until Phase 5.
