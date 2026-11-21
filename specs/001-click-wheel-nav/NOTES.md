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

- **iOS simulator selection by name.** Locally, `-destination 'platform=iOS Simulator,name=iPhone 16'`
  fails because a name-only destination means `OS:latest` (iOS 26.5 here) and no iPhone 16
  exists on that runtime. Local runs used the device id instead. CI's "first available iPhone"
  pick could hit the same mismatch on a runner image; consider pinning `OS=` or using the
  device id from `simctl` (T052 covers CI finalization).
- The `NSBundle … principal class is nil` log line during iOS tests is harmless (Swift
  Testing bundles have no XCTest principal class).
