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

## Run of 2026-11-21: JDK pin + T026–T029 (Phase 4, US2)

Stopped after T029 as instructed. Phase 5 (T030+) has not been started.

### Tasks done

| Task(s) | Result | Commit |
|---|---|---|
| Setup | Gradle daemon pinned to JDK 21 (`android/gradle/gradle-daemon-jvm.properties`); CI `android-test` moved to Temurin 21. Plain `./gradlew :app:testDebugUnitTest` passes 79/79 from a JDK 25 shell. | 8b0a9fa |
| T026–T027 | Android: every wheel event goes through `MenuNavigator`; `BackHandler(enabled = canGoBack)` sends a MENU press; `RouteScreen` shows `ComingSoonScreen` (`CenteredMessage`) for every route; strings `coming_soon`, `press_menu_to_go_back`. | 1198df5 |
| T028–T029 | iOS: every wheel event goes through `MenuNavigator`; `RouteView` shows `ComingSoonView` for every route; the same two keys added to the String Catalog. | c9c44de |

Tests written first and seen failing:
- Android `BeatSideAppTest` (Robolectric, 8 tests): 4 navigation tests failed before the wiring. The other 4 ("does nothing" cases: MENU at root, ring buttons, long press, back at root) passed from the start and stay as regression guards.
- iOS `NavigationUITests` (XCUITest, 6 tests): 3 navigation tests failed before the wiring, and the 3 "does nothing" guards passed.
- iOS `MenuStringsTests`: the placeholder keys were added to the test first and failed until the catalog had them.

### Test counts at the Phase 4 checkpoint

| Platform | Suite | Tests | Result |
|---|---|---|---|
| Android | `./gradlew :app:testDebugUnitTest` | 87: WheelTracker 42, MenuNavigator 16, WheelSettings 5, ListScroll 7, MenuStrings 3, ClickWheel 6, BeatSideApp 8 | all pass |
| iOS | `BeatSide` scheme (unit) | 67: WheelTracker 42, MenuNavigator 16, WheelSettings 5, MenuStrings 4 | all pass |
| iOS | `BeatSideUITests` scheme (XCUITest) | 6: NavigationUITests | all pass (iPhone 16, iOS 18.1 sim) |

### Decisions and deviations to review

1. **`gradle-daemon-jvm.properties` written by hand.** `./gradlew updateDaemonJvm --jvm-version=21`
   fails with "Toolchain download repositories have not been configured". The task also writes
   JDK download URLs, which need a toolchain resolver plugin (foojay). I didn't add a plugin; the
   file has only `toolchainVersion=21`, so Gradle uses a locally installed JDK 21 and errors
   clearly on a machine without one. To get auto-download instead, add the
   `org.gradle.toolchains.foojay-resolver-convention` plugin to `settings.gradle.kts` and re-run
   the task.
2. **CI `android-test` moved from JDK 17 to 21**, since a daemon pinned to 21 can't run on a
   runner that only has 17 set up.
3. ~~**Back at the root leaves the app (Android).**~~ Resolved: FR-014 reworded and acceptance
   scenario US2-9 added (spec amendment, 2026-11-21). Original note: T026 specifies `BackHandler(enabled = canGoBack)`,
   so at the root the system handles back. FR-014 says back must behave "identically to MENU",
   and MENU at the root does nothing, so read literally FR-014 would swallow back at the root and
   trap the user. Acceptance scenario 4 only covers a submenu. I followed T026, and
   `BeatSideAppTest` pins this behaviour (`system back at the root is left to the system`).
   If you agree, consider rewording FR-014 to "…while not at the root".
4. **Test hooks in production code.** Android `ClickWheelTestTag` (`Modifier.testTag`) and iOS
   `.accessibilityElement(children: .ignore).accessibilityIdentifier("click_wheel")` on the wheel.
   On iOS this also hides the decorative glyphs and "MENU" text from VoiceOver as separate
   elements; T046 adds the wheel's real label, adjustable trait and actions on top.
5. Every route shows the Coming Soon placeholder (per T027/T029), including Clock, About and
   Now Playing, until Phase 6.

### What to try by hand

On an Android phone/emulator and an iPhone/simulator:

1. **Open and back:** tap the center on Music, and the title becomes "Music" with Playlists to Genres.
   Tap MENU (top of the ring) to return to the root with **Music still highlighted**.
2. **Selection is remembered:** rotate to Settings (2 detents), center, then MENU. You should be
   back at the root with **Settings** highlighted (acceptance scenario 2).
3. **Leaf screen:** Music, then Playlists, then center. You should see "Coming soon" and
   "Press MENU to go back." MENU returns to the Music list with Playlists highlighted.
4. **Deep then shallow:** go Music, then a lower row (e.g. Genres), then center, then MENU twice. Each
   level should come back with its own highlight, and the list should start at the top when you
   open a new submenu.
5. **MENU at the root:** nothing happens.
6. **Toggle rows:** Settings, then Clicker, then center. The suffix should flip On ↔ Off (the flags
   only change the label until Phase 5 adds sound and haptics).
7. **Long press:** hold the center ~1 s, then release. Nothing should open.
8. **Other ring buttons:** Next / Previous / Play-Pause taps do nothing.
9. **Android only, system back:** in any submenu or leaf, back gesture = MENU. At the root, back
   leaves the app (see decision 3).

## Run of 2026-11-21: spec amendment + T030–T035 (Phase 5, US3)

Stopped after T035 as instructed. Phase 6 (T036+) has not been started.

### Tasks done

| Task(s) | Result | Commit |
|---|---|---|
| Spec | FR-014 reworded (back at the root leaves the app), acceptance scenario US2-9 added, FR-018 now says the iOS click follows the Ring/Silent switch. | 2fed8e3 |
| T030–T031 | Android `FeedbackPlayer` (pure rules) + `SoundPoolClickSound` / `ViewHaptics`; `ClickWheel` plays feedback before dispatching each event, with the latest settings. | 1a19812 |
| T032–T033 | iOS `FeedbackPlayer` (pure rules, `Wheel/`) + `AudioClickSound` / `GeneratorHaptics` (`UI/FeedbackOutputs.swift`); `WheelTouchView` prepares haptics on touch-down and plays feedback before dispatching. | c69a8bf |
| T034–T035 | Verification: Settings rows show On/Off, Center flips them without navigating, and (Android, end to end) the new value applies to the very next tick or press. | a695f5b |

Tests written first and seen failing: `FeedbackPlayerTest` (Android, 9), `FeedbackPlayerTests` (iOS, 11),
the 4 feedback cases in `ClickWheelTest`, and `WheelTouchViewTests` (iOS, 5). All failed until the
types and parameters existed. The T034/T035 tests verify wiring that already existed since Phases 3–4, so
only the new `BeatSideApp(feedback:)` parameter made them fail first. The On/Off assertions passed
as soon as they compiled.

### Test counts at the Phase 5 checkpoint

| Platform | Suite | Tests | Result |
|---|---|---|---|
| Android | `./gradlew :app:testDebugUnitTest` | 104: WheelTracker 42, MenuNavigator 16, WheelSettings 5, ListScroll 7, MenuStrings 3, FeedbackPlayer 9, ClickWheel 10, BeatSideApp 12 | all pass |
| iOS | `BeatSide` scheme (unit) | 83: WheelTracker 42, MenuNavigator 16, WheelSettings 5, MenuStrings 4, FeedbackPlayer 11, WheelTouchView 5 | all pass |
| iOS | `BeatSideUITests` scheme | 8: NavigationUITests 6, SettingsUITests 2 | all pass |

### How the requirements are met

- **Never pause or duck other apps' music.** Android: `SoundPool` with `USAGE_ASSISTANCE_SONIFICATION`
  / `CONTENT_TYPE_SONIFICATION` and no audio-focus request. iOS: `AVAudioSession` category
  `.ambient` (mixes with others, no `.duckOthers`). Because it's `.ambient`, the Ring/Silent switch
  mutes the click, matching the amended FR-018.
- **Same-frame haptics on iOS.** Generators are prepared when a touch lands on the wheel (if
  Haptics is on) and re-prepared after every fire, so they stay warm through a rotation.
- **FR-021.** Both `FeedbackPlayer`s take the settings as an argument on every event. The wheel
  reads the latest value (`rememberUpdatedState` on Android, `updateUIView` on iOS). The press
  that toggles a setting still uses the old value, and the change applies from the next event.
  This is tested end to end on Android (`turning Haptics off stops the very next press haptic`).

### Decisions and deviations to review

1. **`FeedbackPlayer` is split into pure rules + platform outputs** (`ClickSound`, `HapticOutput`
   interfaces/protocols). T030/T032 describe one class holding SoundPool/AVAudioPlayer. The split is
   what lets FR-018/019/021 be unit-tested with fakes, as you asked. On iOS the pure class is in
   `Wheel/FeedbackPlayer.swift` and the outputs are in `UI/FeedbackOutputs.swift` (T032 named
   `UI/FeedbackPlayer.swift`).
2. **Android press haptic uses `CONFIRM` from API 30**, not 33 as T030 says: the constant exists
   since API 30. Below 30 it's `VIRTUAL_KEY`. Ticks use `SEGMENT_TICK` (34+) / `CLOCK_TICK`.
3. **iOS click uses a 4-player `AVAudioPlayer` round-robin** (mirrors Android's 4 SoundPool
   streams) so rapid detents don't cut each other off.
4. **Injectable feedback for tests.** Android `ClickWheel(feedback =)` and `BeatSideApp(feedback =)`
   default to `rememberFeedbackPlayer()`. iOS `WheelTouchView(feedback:)` and its touch overrides
   delegate to `begin(at:)` / `move(to:)` / `end()` / `cancel()` so unit tests can drive it
   without `UITouch`.
5. **Long press fires the medium haptic** (T030/T032: "Press/LongPress → medium"). A center hold
   gives one buzz at 500 ms and none on release.
6. Android haptics go through `View.performHapticFeedback`, so they follow the system
   "Touch feedback" setting. If that setting is off, the in-app Haptics toggle has no visible
   effect.

### What to try by hand (real devices; simulators/emulators have no haptics)

1. **Click + haptic per detent:** rotate slowly. You should get one click and one light tick per
   15°, including at the first/last row where the highlight no longer moves (US1-2/3).
2. **Fast rotation:** spin quickly. Clicks should overlap rather than cut off or lag behind the
   highlight.
3. **Presses:** center, MENU, Next, Previous, Play/Pause should each give a medium buzz and **no**
   click. Holding the center gives one buzz at ~0.5 s and nothing on release.
4. **Clicker toggle:** Settings → Clicker → center (row shows Off) → rotate. You should feel ticks
   but hear no click. Toggle back On, and the very next detent clicks.
5. **Haptics toggle:** Settings → Haptics → center (that press still buzzes) → rotate and press.
   There should be no haptics at all, while clicks continue. Toggle back On.
6. **Other apps' music:** start music in Spotify/YouTube Music/Apple Music, open BeatSide and
   rotate. The music must keep playing at full volume, with clicks mixed on top.
7. **iOS Ring/Silent switch:** flip to Silent. There should be no click and haptics should still
   fire. Flip back to Ring and clicks should return.
8. **Settings are session-only (FR-030):** turn Clicker off, kill and relaunch the app. Clicker
   should be On again.

## Run of 2026-11-21: T036–T043 (Phase 6, US4)

Stopped after T043 as instructed. Phase 7 (T044+) has not been started.

### Tasks done

| Task(s) | Result | Commit |
|---|---|---|
| T036–T039 | Android `ClockFormat` (pure), `ClockScreen`, `AboutScreen`; `RouteScreen` routes Clock/About to real screens and everything else to Coming Soon; English strings for About and the wheel button accessibility labels. | 868bb4e |
| T040–T043 | iOS `ClockFormat` (pure), `ClockView`, `AboutInfo`, `AboutView`; `RouteView` wired the same way; the same keys added to the String Catalog. | b04c8bb |

Tests written first and seen failing:
- Android: `ClockFormatTest` (7, unresolved type), then 4 `BeatSideAppTest` cases for real Clock/About screens. Two "stays Coming soon" guards passed from the start.
- iOS: `ClockFormatTests` / `AboutInfoTests` (unresolved types), `MenuStringsTests` (the 7 new keys missing from the catalog), and `LeafScreensUITests` (Clock/About failed; the 2 placeholder guards passed).

### Test counts at the Phase 6 checkpoint

| Platform | Suite | Tests | Result |
|---|---|---|---|
| Android | `./gradlew :app:testDebugUnitTest` | 117: WheelTracker 42, MenuNavigator 16, WheelSettings 5, ListScroll 7, MenuStrings 3, FeedbackPlayer 9, ClickWheel 10, BeatSideApp 18, ClockFormat 7 | all pass |
| iOS | `BeatSide` scheme (unit) | 94: WheelTracker 42, MenuNavigator 16, WheelSettings 5, MenuStrings 4, FeedbackPlayer 11, WheelTouchView 5, ClockFormat 8 (one parameterized over 9 templates), AboutInfo 3 | all pass |
| iOS | `BeatSideUITests` scheme | 12: Navigation 6, Settings 2, LeafScreens 4 | all pass (iPhone 16, iOS 18.1 sim) |

### How the requirements are met

- **Clock pattern decision is pure and tested** on both platforms: `ClockFormat.pattern` picks
  `HH:mm:ss` (24-hour) or `hh:mm:ss a` (12-hour), and `format`/`string` apply it for a locale.
  On iOS, `ClockFormat.uses24Hour(hourTemplate:)` also decides 12 vs 24 from the locale's `"j"`
  template (H/k vs h/K, quoted text ignored), so locales that put AM/PM first or use day periods
  are handled.
- **Thin observers.** Android: one `BroadcastReceiver` (TIME_CHANGED, which Android sends when the
  12/24-hour setting is switched, plus TIMEZONE_CHANGED and LOCALE_CHANGED) re-reads
  `DateFormat.is24HourFormat`. iOS: `.onReceive(NSLocale.currentLocaleDidChangeNotification)`
  re-reads the template. Both tick on second boundaries.
  `BeatSideAppTest` switches `TIME_12_24` from 12 to 24 while the Clock is open and checks the
  display follows. iOS has no automated equivalent: XCUITest can't change the system setting.
- **Version from the build**, never a literal: `BuildConfig.VERSION_NAME` (Android `versionName`)
  and `CFBundleShortVersionString` (iOS `MARKETING_VERSION`), both currently 0.1.0.
- **Routes:** Clock and About are real; Music items, Stopwatch, Games, Shuffle Songs and Now
  Playing show Coming Soon.

### Decisions and deviations to review

1. **Author string: "by Douglas Rodrigues Neto".** FR-025 requires an author but the spec
   doesn't give one; the name comes from the prototype's About screen
   (`prototype/wheel-and-menus`, `LcdScreen.kt`). The prototype's tagline and "© 2026" line
   were not carried over. Change `about_author` on both platforms if you want different wording.
2. **No `NowPlayingScreen.kt` / `NowPlayingView.swift`.** T037/T041 ask for them, but your
   instructions say Now Playing keeps the Coming Soon placeholder, so `Route.NowPlaying` maps to
   `ComingSoonScreen`/`ComingSoonView` and no extra file exists (Constitution VII).
3. **iOS uses the same fixed patterns as Android** instead of the `Date.FormatStyle` that T040
   mentions. That keeps the "which pattern for which setting" decision in one pure, tested
   function, as you asked. The AM/PM symbols still come from the current locale.
4. **12-hour times keep the leading zero** (`07:05:09 PM`), per the `hh:mm:ss a` pattern in T036.
   Switch to `h:mm:ss a` in both `ClockFormat`s if you prefer `7:05:09 PM`.
5. **Wheel button accessibility labels added now (English)**, per T039: `a11y_wheel_center`,
   `a11y_wheel_menu`, `a11y_wheel_next`, `a11y_wheel_previous`, `a11y_wheel_play_pause`. They're
   unused until Phase 7 (T045/T046). Android's resource shrinker may drop them from release builds
   until then, which is harmless.

### What to try by hand

1. **Clock:** Extras → Clock. You should see the current time, with seconds advancing once a
   second and no jitter in width.
2. **12/24 switch while open.** Android: Settings → System → Date & time → "Use 24-hour format".
   iOS: Settings → General → Date & Time → "24-Hour Time". Leave the Clock open and toggle. On
   return the display should already use the new format (e.g. `19:05:09` ↔ `07:05:09 PM`). I have
   **not** checked iOS by hand: the code relies on iOS posting the locale-change notification and
   on the `"j"` template reflecting the switch.
3. **Time zone change** with the Clock open (set time automatically off, pick another zone): the
   hour should change within a second.
4. **About:** Settings → About should show "BeatSide", "Version 0.1.0", "by Douglas Rodrigues Neto".
   Bump `versionName` / `MARKETING_VERSION`, rebuild, and the screen should follow.
5. **Placeholders:** each Music item, Stopwatch, Games, Shuffle Songs and Now Playing should show "Coming soon".
   MENU returns to the parent with the same row highlighted (US4-4).
6. **Wheel on leaf screens:** rotating on Clock/About should still click and buzz but not
   change anything; MENU goes back.

## Run of 2026-11-21: T044–T053 (Phase 7, polish)

Feature 001 is implemented on both platforms. T053 (the manual acceptance run) is **left unchecked
in tasks.md** for you to tick after running the checklist at the end of this file on your phones.

### Tasks done

| Task(s) | Result | Commit |
|---|---|---|
| Clock change | 12-hour pattern `hh:mm:ss a` → `h:mm:ss a` on both platforms (supersedes Phase 6 decision 4). | 0091b62 |
| T044, T045, T050 | Android: wheel custom actions + labelled button zones, row labels, announcements; `MenuAccessibilityTest`, Robolectric `AccessibilityTest` (API 35). | b234ed9 |
| T046, T047, T051 | iOS: adjustable "Wheel" element + named actions + labelled buttons, row labels, `BeatSideModel` with announcements; unit tests + `AccessibilityUITests` (incl. accessibility audit). | 3d3c6d7 |
| T048 | Android pt-BR (`values-pt-rBR`), `TranslationsTest`, `PortugueseTest`. | ceb8e64 |
| T049 | iOS pt-BR in the String Catalog, catalog completeness test, `LocalizationUITests`. | 5642663 |
| T052 | CI verified (all suites already run; simulator picker checked); README section on CI and required checks. | a50783e |
| T053 | Not run (no devices). The manual acceptance checklist is at the end of this file. | — |

Tests first: the ClockFormat change (3 failures per platform), `MenuAccessibilityTest(s)`,
`AccessibilityTest`, `BeatSideModelTests`, `AccessibilityNavigationTests`, `TranslationsTest`,
`PortugueseTest` and the pt-BR catalog check all failed before their code existed (most as missing
types; the translation tests on missing values). `AccessibilityUITests` and `LocalizationUITests`
were written after the code, as end-to-end checks of the running app.

### Test counts at the end of feature 001

| Platform | Suite | Tests | Result |
|---|---|---|---|
| Android | `./gradlew :app:testDebugUnitTest` | 141: WheelTracker 42, MenuNavigator 16, WheelSettings 5, ListScroll 7, MenuStrings 3, FeedbackPlayer 9, ClickWheel 10, BeatSideApp 18, ClockFormat 7, MenuAccessibility 11, Accessibility 7, Translations 3, Portuguese 3 | all pass |
| iOS | `BeatSide` scheme (unit) | 114: WheelTracker 42, MenuNavigator 16, WheelSettings 5, MenuStrings 5, FeedbackPlayer 11, WheelTouchView 5, ClockFormat 8, AboutInfo 3, MenuAccessibility 11, BeatSideModel 2, AccessibilityNavigation 6 | all pass on iOS 18.1 (iPhone 16) and iOS 26.5 (iPhone 17 Pro) |
| iOS | `BeatSideUITests` scheme | 17: Navigation 6, Settings 2, LeafScreens 4, Accessibility 4, Localization 1 | all pass on both simulators |

### How the requirements are met

- **FR-031 actions through the same path.** Android: `ClickWheel` custom actions call the same
  `dispatch` as touches (feedback, then `onEvent` → `MenuNavigator`). iOS: the "Wheel" element's
  increment/decrement (VoiceOver swipe up/down) and its four named actions call
  `WheelTouchView.dispatch`. Tests check the click/haptic for each action on both platforms.
- **Announcements.** `MenuAccessibility.announcement` (pure, same rules on both) decides the text:
  - a new selection → the row label ("Extras, submenu");
  - a new screen → its title, plus the highlighted row for a submenu ("Extras, Clock", "Clock");
  - a toggled setting → the row with its new state ("Clicker, Off");
  - nothing when nothing changed, e.g. a clamped tick or MENU at the root.

  Android announces through `View.announceForAccessibility`, iOS through
  `UIAccessibility.post(.announcement)`. Both go through an injectable `Announcer`, and the exact
  texts are asserted.
- **FR-028 row labels:** "Music, submenu", "Clicker, On" / "Clique, Desligado", "Música, submenu".
- **SC-005:** `TranslationsTest` (Android) and the catalog test (iOS) fail on any key without a
  pt-BR value or with different placeholders.
- **SC-007:** `AccessibilityTest` (Android, Robolectric) and `AccessibilityNavigationTests` (iOS,
  through the real accessibility elements) walk to all 11 leaves and back to the root using only
  the four actions.

### Decisions and deviations to review

1. **Announcements fire on any state change, not only after accessibility actions.** That's
   simpler and gives the same result: both platform APIs do nothing when no screen reader is
   running, and with one running a physical tick is announced like "Next item".
2. **Toggling a setting is also announced** ("Clicker, Off"). Strictly it changes neither the
   selection nor the screen, but without it a screen-reader user gets no confirmation.
3. **Announcement for a new submenu = screen + highlighted row** ("Music, Playlists"); for a leaf,
   the title only ("Clock"). The leaf's content (time, "Coming soon") is read by the screen reader
   when focused.
4. **Android `announceForAccessibility` is deprecated from API 36** (live regions recommended).
   I kept it because you asked for an announcement and it still works with TalkBack; the
   deprecation warning is suppressed in `ViewAnnouncer`. Worth revisiting before targeting 37.
5. **Android buttons are semantics-only zones** over the drawn wheel (no pointer input, so touches
   still reach the gesture handler); the same fractions are used for the iOS button elements.
   TalkBack/VoiceOver users can focus and double-tap them; Next/Previous/Play-Pause do nothing in
   this slice (FR-011).
6. **iOS: four named actions *and* the adjustable trait.** T046 asks for adjustable + Select/Go
   back, while T051 expects four named actions; both are present, so "Next item"/"Previous item"
   are reachable by swipe up/down and from the actions rotor.
7. **iOS event handling moved into `BeatSideModel`** (`@Observable`) so announcements can be
   unit-tested; `BeatSideView` is now layout only.
8. **iOS rows are single elements** (`.accessibilityElement(children: .ignore)`); Android rows merge
   their texts and set a content description. On Android I expect TalkBack to read the content
   description, which you should confirm in the checklist.
9. **Unit-test language pinned to en/US** in `ios/project.yml`. Your simulator runs in pt-BR, and
   once pt-BR strings existed, tests asserting English text got Portuguese.
10. **Translations to confirm** (same on both platforms): Settings → "Configurações", Clicker →
    "Clique", Haptics → "Vibração", On/Off → "Ligado"/"Desligado", Now Playing → "Tocando agora",
    Shuffle Songs → "Músicas aleatórias", the wheel → "Roda". Kept in English as BR users know
    them: "Playlists", "Extras", "MENU" (the button's name). `app_name` is untranslatable on Android.
11. **The wheel's accessibility name is "Wheel" / "Roda"**, deliberately not "Click Wheel"
    (Constitution VI).

### FR / SC coverage

Every FR and SC in spec.md is covered. Rows marked *manual* also need the checklist below,
because they depend on real hardware, system settings or a screen reader.

| Req | Covered by |
|---|---|
| FR-001 layout, portrait | `LcdScreen`/`LcdView` + wheel; portrait in manifest / project.yml; screenshot check (Phase 3) · *manual* |
| FR-002 title bar + content | title bar tested by `AccessibilityTest` (`LcdTitleTestTag`), `NavigationUITests` |
| FR-003 15° per detent | WheelTracker D-1–D-4 (both) |
| FR-004 normalization | WheelTracker N-1–N-7, B-1/B-2 (both) |
| FR-005 accumulator | WheelTracker D-*, J-1 (both) |
| FR-006 dead zone | WheelTracker DZ-1–DZ-3 (both) |
| FR-007 center start, no ticks | WheelTracker C-1–C-3 (both) |
| FR-008 tap classification | WheelTracker C-4, T-*, J-1 (both); `ClickWheelTest`, `WheelTouchViewTests` |
| FR-009 zone intervals | WheelTracker T-1–T-17 (both) |
| FR-010 long press, Center only | WheelTracker LP-*, RH-* (both); `ClickWheelTest`, `NavigationUITests` |
| FR-011 Next/Prev/PlayPause emitted, no effect | MenuNavigator tests (both); `BeatSideAppTest`, `NavigationUITests` |
| FR-012 Center opens | MenuNavigator, `BeatSideAppTest`, `NavigationUITests` |
| FR-013 MENU back, nothing at root | same |
| FR-014 Android back (root leaves) | `BeatSideAppTest` (both cases) |
| FR-015 clamping | MenuNavigator (both) |
| FR-016 minimal scroll | `ListScrollTest` (Android); iOS `scrollTo(nil anchor)` · *manual* |
| FR-017 › / On / Off | `SettingsUITests`, `BeatSideAppTest` |
| FR-018 click + light haptic per tick (iOS silent switch) | `FeedbackPlayerTest(s)`, `ClickWheelTest`, `WheelTouchViewTests` · *manual* (sound, haptics, silent switch) |
| FR-019 medium haptic per press, no click | same |
| FR-020 Settings toggles | `BeatSideAppTest`, `SettingsUITests` |
| FR-021 next-event effect | `FeedbackPlayerTest(s)`, `ClickWheelTest`, `WheelTouchViewTests`, `BeatSideAppTest` |
| FR-022–FR-023 menu tree, Coming soon | MenuNavigator tree test (both), `LeafScreensUITests`, `BeatSideAppTest` |
| FR-024 Clock | `ClockFormatTest(s)`; Android 12↔24 switch while open (`BeatSideAppTest`); iOS · *manual* |
| FR-025 About | `BeatSideAppTest`, `AboutInfoTests`, `LeafScreensUITests` |
| FR-026 placeholders | `BeatSideAppTest`, `LeafScreensUITests` |
| FR-027 button labels | `AccessibilityTest`, `AccessibilityNavigationTests`, `AccessibilityUITests` |
| FR-028 row labels | `MenuAccessibilityTest(s)`, `AccessibilityTest`, `AccessibilityUITests`, `PortugueseTest`, `LocalizationUITests` |
| FR-029 externalized en + pt-BR | `MenuStringsTest(s)`, `TranslationsTest`, catalog pt-BR test, `PortugueseTest`, `LocalizationUITests` |
| FR-030 session-only settings | no persistence code exists · *manual* (relaunch) |
| FR-031 four actions | `AccessibilityTest`, `AccessibilityNavigationTests` · *manual* (TalkBack/VoiceOver) |
| SC-001 any leaf ≤ 10 inputs | tree depth: the longest path from launch is 6 inputs (e.g. Music → Genres: center, 4 detents, center) · *manual* |
| SC-002 no tick bursts | WheelTracker B-1/B-2, D-* (both) |
| SC-003 setting applies next interaction | FR-021 tests · *manual* |
| SC-004 accessibility inspection | `AccessibilityTest` (labels present), `AccessibilityUITests` (`performAccessibilityAudit`) |
| SC-005 pt-BR complete | `TranslationsTest`, catalog pt-BR test · *manual* (read every screen) |
| SC-006 Clock 5 s / both formats | `ClockFormatTest(s)`, Android switch test · *manual* |
| SC-007 screen-reader full tree | `AccessibilityTest` / `AccessibilityNavigationTests` walk every leaf · *manual* |

## Manual acceptance checklist (T053)

Run on a physical Android phone and a physical iPhone. Tick each box per platform. When all
pass, check off T053 in tasks.md.

### Setup
- [ ] Android: install from Android Studio (`app`, debug). iOS: run the `BeatSide` scheme on the device.
- [ ] Device language English, 12-hour time, Touch feedback (Android) on, Ring mode (iOS).

### Layout and wheel (FR-001–FR-011, SC-002)
- [ ] App is portrait only; the LCD (title bar + list) sits above the wheel.
- [ ] Rotate slowly clockwise: one row down per ~15°, one click and one light tick per detent.
- [ ] Rotate counter-clockwise: one row up per detent. At first/last row the highlight stops but click + tick still fire.
- [ ] Spin several full turns, crossing the left side of the wheel repeatedly: no bursts or skipped rows (SC-002).
- [ ] Drag through the center and out the other side: no ticks while in the middle; rotation resumes cleanly.
- [ ] Start a drag on the center button and move onto the ring: no ticks.
- [ ] Tap MENU, Next, Previous, Play/Pause, center: each gives a medium buzz, no click.
- [ ] Hold the center ~1 s: one buzz at ~0.5 s, nothing opens on release.
- [ ] Hold a ring button ~1 s, then release: it acts as a normal tap on release.

### Navigation (FR-012–FR-017, SC-001)
- [ ] Center opens the highlighted row; MENU goes back with the same row highlighted.
- [ ] MENU at the root does nothing.
- [ ] Android only: back gesture = MENU in submenus/leaves; at the root it leaves the app.
- [ ] Open a long list (Music) on the smallest phone you have: the highlighted row is always fully visible, and the list scrolls only as much as needed.
- [ ] Submenu/screen rows show "›"; Clicker/Haptics show On/Off.
- [ ] SC-001: reach Genres, Clock, About, Now Playing; each takes ≤ 10 inputs from launch.

### Feedback and settings (FR-018–FR-021, FR-030, SC-003)
- [ ] Settings → Clicker → Off: the very next detent is silent but still buzzes. On again: clicks return.
- [ ] Settings → Haptics → Off: the very next detent/press has no buzz; clicks continue. On again.
- [ ] Music playing in another app keeps playing at full volume while you rotate.
- [ ] iOS: Silent switch on → no click, haptics still fire.
- [ ] Turn Clicker off, kill and relaunch: Clicker is On again (FR-030).

### Leaf screens (FR-022–FR-026, SC-006)
- [ ] Every Music item, Stopwatch, Games, Shuffle Songs, Now Playing → "Coming soon" + "Press MENU to go back."
- [ ] About → "BeatSide", "Version 0.1.0", "by Douglas Rodrigues Neto".
- [ ] Clock: correct local time, seconds advance once per second; watch for 5 s (SC-006).
- [ ] With Clock open, switch the system to 24-hour: the display changes to `19:05:09` style without leaving the screen. Switch back: `7:05:09 PM` style (no leading zero).
- [ ] With Clock open, change the time zone: the hour updates.

### Brazilian Portuguese (FR-029, SC-005)
- [ ] Set the device to Português (Brasil). Visit every screen: no English left and no raw keys (e.g. `menu_music`).
- [ ] Check that the wording reads naturally (see decision 10 above).
- [ ] About shows "Versão 0.1.0" and "por Douglas Rodrigues Neto"; placeholders say "Em breve" / "Pressione MENU para voltar."

### TalkBack (Android) (FR-027, FR-028, FR-031, SC-004, SC-007)
- [ ] Turn on TalkBack. Focus the wheel: it is read as "Wheel".
- [ ] Open the actions menu (three-finger tap or swipe up-then-right): "Next item", "Previous item", "Select", "Go back".
- [ ] "Next item" moves the selection, clicks and buzzes, and TalkBack announces the new row (e.g. "Extras, submenu").
- [ ] "Select" opens it and announces "Extras, Clock"; "Select" again announces "Clock"; "Go back" returns.
- [ ] Toggle Clicker with "Select": announced "Clicker, Off".
- [ ] Explore by touch over the wheel: "Select button", "Menu button", "Next button", "Previous button", "Play/Pause button".
- [ ] Focus a row: it reads once as "Music, submenu" / "Clicker, On" (not the name twice; see decision 8).
- [ ] SC-007: reach every leaf and return to the root using only the wheel's actions.
- [ ] Repeat the announcements check in Português (Brasil): e.g. "Música, submenu", "Clique, Desligado".

### VoiceOver (iOS) (FR-027, FR-028, FR-031, SC-004, SC-007)
- [ ] Turn on VoiceOver. Focus the wheel: "Wheel, adjustable".
- [ ] Swipe up/down: next/previous item with click + haptic, and the new row is announced.
- [ ] Actions rotor (swipe up/down after selecting "Actions"): "Next item", "Previous item", "Select", "Go back".
- [ ] "Select" opens and announces; "Go back" returns and announces the parent and its row.
- [ ] Toggle Clicker with "Select": announced "Clicker, Off".
- [ ] Swipe right through the wheel's buttons: "Select button", "Menu button", "Next button", "Play/Pause button", "Previous button"; double-tap presses.
- [ ] Rows read as "Music, submenu" / "Clicker, On".
- [ ] SC-007: reach every leaf and return to the root using only the wheel.
- [ ] Repeat in Português (Brasil): "Roda", "Próximo item", "Música, submenu".
