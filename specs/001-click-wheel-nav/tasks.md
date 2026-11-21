# Tasks: Click Wheel and Menu Navigation

**Input**: Design documents from `specs/001-click-wheel-nav/`

**Prerequisites**: plan.md ✅ spec.md ✅ research.md ✅ data-model.md ✅ contracts/ ✅ quickstart.md ✅

**Tests**: Required by Constitution III — pure logic tests MUST be written first and MUST fail before implementation begins.

**Platforms**: Every phase delivers work on both Android (`android/`) and iOS (`ios/`). Tasks for the two platforms within a phase are independent and may be done in parallel (marked `[P]`).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel with other [P] tasks in the same group
- **[Story]**: User story this task belongs to (US1–US4)

---

## Phase 1: Setup

**Purpose**: Create both native project scaffolds and lay the shared development infrastructure. Neither `android/` nor `ios/` exists yet.

- [x] T001 Create Android project scaffold: `android/settings.gradle.kts`, `android/build.gradle.kts`, `android/gradle.properties`, `android/app/build.gradle.kts` (compileSdk 37, minSdk 26, targetSdk 36, applicationId dev.douglasneto.beatside, `buildFeatures { compose = true; buildConfig = true }`, Compose BOM enabled), `android/gradle/libs.versions.toml` (AGP 9.4.1, Kotlin 2.3.0, Compose BOM 2026.08.00, activity-compose 1.10.1, core-ktx 1.16.0, junit 4.13.2, robolectric 4.14.1), `android/gradle/wrapper/gradle-wrapper.properties` (Gradle 9.6.0), `android/gradlew`, `android/gradlew.bat`, `android/app/src/main/AndroidManifest.xml` (portrait-only, dev.douglasneto.beatside)

- [x] T001a Run `cd android && ./gradlew :app:assembleDebug`; fix any AGP / Kotlin / Compose version incompatibilities until the build succeeds; update `android/gradle/libs.versions.toml` and `android/gradle/wrapper/gradle-wrapper.properties` with the confirmed working versions; record the final versions in `specs/001-click-wheel-nav/research.md` §1 (the prototype was never compiled — these are candidate versions only)

- [x] T002 [P] Create iOS project definition: `ios/project.yml` with targets: `BeatSide` (type `application`, iOS 17, Swift 6, bundle ID dev.douglasneto.BeatSide, iPhone-only portrait); `BeatSideTests` (type `bundle.unit-test`, depends on BeatSide); `BeatSideUITests` (type `bundle.ui-testing`, depends on BeatSide) — add a `BeatSideUITests` scheme as well; run `xcodegen generate` from `ios/` to verify the project generates without error (the generated `.xcodeproj` is NOT committed)

- [x] T003 Copy click.wav from prototype: `android/app/src/main/res/raw/click.wav` and `ios/BeatSide/Resources/click.wav` (source: `git show origin/prototype/wheel-and-menus:android/app/src/main/res/raw/click.wav`)

- [x] T004 [P] Create string resource stubs: `android/app/src/main/res/values/strings.xml` (empty `<resources/>`, en) and `android/app/src/main/res/values-pt-rBR/strings.xml` (empty `<resources/>`, pt-BR); create iOS String Catalog stub `ios/BeatSide/Resources/Localizable.xcstrings` (en + pt-BR locales, no entries yet)

- [x] T005 [P] Create stub GitHub Actions CI workflow `.github/workflows/ci.yml` with jobs: `android-test` (ubuntu-latest, JDK 17, `./gradlew :app:testDebugUnitTest` — includes Robolectric Compose tests); `ios-test` (macos-latest, Homebrew XcodeGen, `cd ios && xcodegen generate && xcodebuild test -project BeatSide.xcodeproj -scheme BeatSide -destination 'platform=iOS Simulator,name=iPhone 16'`); `ios-ui-test` (macos-latest, XcodeGen, `xcodebuild test -scheme BeatSideUITests …`); all jobs trigger on `pull_request` to `main`

**Checkpoint**: Both project scaffolds build (T001a confirms Android); CI workflow file is committed; click.wav present on both platforms.

---

## Phase 2: Foundational — Pure Core Types

**Purpose**: Implement `WheelTracker`, `MenuNavigator`, and `WheelSettings` with their full test suites on both platforms. These are pure types with no UI framework imports (Constitution III). **No user story work begins until this phase is complete.**

> ⚠️ **Tests MUST be written first and confirmed failing before the implementation task is started (Constitution III).**

### Android WheelTracker

- [x] T006 Write `android/app/src/test/kotlin/dev/douglasneto/beatside/wheel/WheelTrackerTest.kt` implementing all **40** cases from `specs/001-click-wheel-nav/contracts/wheel-behavior.md`: groups N-1–N-7, D-1–D-4, B-1–B-2, J-1, DZ-1–DZ-3, C-1–C-4, O-1, T-1–T-5 + T-7/T-8/T-10/T-11/T-13/T-14/T-16/T-17, LP-1–LP-3, RH-1–RH-2 — confirm ALL tests fail (types do not exist yet)

- [x] T007 Implement `android/app/src/main/kotlin/dev/douglasneto/beatside/wheel/WheelButton.kt` (enum: Center, Menu, Next, Previous, PlayPause), `WheelEvent.kt` (sealed: Tick, Press, LongPress per `data-model.md` WheelEvent), `WheelTracker.kt` (pure logic per `data-model.md` WheelTracker contract; apply FR-010 fix: `longPressTimeout` returns empty unless `startButton == Center`; CENTER_RATIO 0.36, DEAD_ZONE_RATIO 0.20, STEP_DEGREES 15.0, TAP_SLOP 8 dp, LONG_PRESS_MS 500) — run T006 tests and confirm all pass

### iOS WheelTracker (parallel with T006–T007)

- [ ] T008 [P] Write `ios/BeatSideTests/WheelTrackerTests.swift` implementing the same **40** cases from `specs/001-click-wheel-nav/contracts/wheel-behavior.md` as T006 — confirm ALL tests fail

- [ ] T009 [P] Implement `ios/BeatSide/Wheel/WheelButton.swift` (enum), `WheelEvent.swift` (enum), `WheelTracker.swift` (pure `struct`, no UIKit imports; same contract as Android; apply FR-010 fix: `longPressTimeout` returns `[]` unless `startButton == .center`) — run T008 tests and confirm all pass

### Android Menu Core

- [ ] T010 Write `android/app/src/test/kotlin/dev/douglasneto/beatside/menu/MenuNavigatorTest.kt` covering all rows of the MenuNavigator behavioral table in `data-model.md` (Tick clamping, Center→Submenu, Center→Toggle→ToggleSetting effect, Press(Menu)→back, Press(Menu) at root no-op, Screen→Unhandled, non-Center/Menu Press→Unhandled, LongPress→Unhandled) — confirm all fail

- [ ] T011 Implement `android/app/src/main/kotlin/dev/douglasneto/beatside/menu/MenuNode.kt` (sealed: Submenu, Screen, Toggle; each with `titleKey: String` per `data-model.md` MenuNode — NOT `title: String`), `Route.kt` (enum: ComingSoon, NowPlaying, ShuffleSongs, Clock, About per `data-model.md` Route), `SettingKey.kt` (enum: Clicker, Haptics per `data-model.md` SettingKey), `MenuFrame.kt`, `MenuState.kt`, `MenuEffect.kt` (sealed: ToggleSetting, Unhandled per `data-model.md` MenuEffect), `BeatSideMenu.kt` (static tree using `titleKey` string constants per `data-model.md` BeatSideMenu, e.g. `titleKey = "beatside"`, `titleKey = "menu_music"`, etc.), `MenuNavigator.kt` (pure reducer per `data-model.md` MenuNavigator contract) — run T010 tests and confirm all pass

- [ ] T012 [P] Implement `android/app/src/main/kotlin/dev/douglasneto/beatside/settings/WheelSettings.kt` (data class: `clickerEnabled: Boolean = true`, `hapticsEnabled: Boolean = true`, `toggled(key)` method per `data-model.md` WheelSettings; session-only, no persistence — FR-030)

### iOS Menu Core (parallel with T010–T012)

- [ ] T013 [P] Write `ios/BeatSideTests/MenuNavigatorTests.swift` covering the same behavioral table as T010 — confirm all fail

- [ ] T014 [P] Implement `ios/BeatSide/Menu/MenuNode.swift` (indirect enum with `titleKey: String` per `data-model.md` MenuNode — NOT `title: String`), `Route.swift`, `SettingKey.swift`, `MenuFrame.swift`, `MenuState.swift`, `MenuEffect.swift`, `BeatSideMenu.swift` (static tree using `titleKey` string constants matching the keys in T011), `MenuNavigator.swift` (pure `struct` reducer) — run T013 tests and confirm all pass

- [ ] T015 [P] Implement `ios/BeatSide/Settings/WheelSettings.swift` (struct: `clickerEnabled`, `hapticsEnabled`, `toggled(_:)` per `data-model.md` WheelSettings; session-only — FR-030)

**Checkpoint**: `./gradlew :app:testDebugUnitTest` and `xcodebuild test` both green. All 40 wheel-behavior contract cases pass on both platforms.

---

## Phase 3: User Story 1 — Rotate Wheel to Navigate a List (Priority: P1) 🎯 MVP

**Goal**: The app opens to the root menu. Rotating a finger on the wheel ring moves the highlighted row one step per 15° of travel, clamped at first/last row, with the list scrolling to keep the selection visible. No button presses needed yet.

> ⚠️ **Feedback dependency**: US1 acceptance scenarios 1–3 require a click sound and haptic on every detent. `FeedbackPlayer` is introduced in Phase 5 (T030/T032). Until then, Phase 3 validates row movement and clamping only. The click + haptic criteria are deferred to Phase 5. The Phase 3 Independent Test below reflects this.

**Independent Test (Phase 3 scope)**: Launch the app; rotate clockwise — selection moves down one row per detent. Rotate counter-clockwise — selection moves up. At the last row, further clockwise rotation does not move or wrap. No feedback is expected at this stage.

### Android — US1

- [ ] T016 [US1] Implement `android/app/src/main/kotlin/dev/douglasneto/beatside/wheel/ClickWheel.kt`: Compose `Box` with `pointerInput` forwarding touch events to `WheelTracker`; long-press timeout coroutine (`withTimeoutOrNull(500)`); `Canvas` drawing (ring gradient, center button, MENU/Next/Previous/PlayPause labels); dispatch `WheelEvent`s via callback; no feedback yet — `FeedbackPlayer` wired in T031 (Phase 5) (FR-001, FR-003–FR-010)

- [ ] T017 [US1] Implement `android/app/src/main/kotlin/dev/douglasneto/beatside/ui/MenuListContent.kt`: `LazyColumn` with `itemsIndexed`; highlighted row styled with selection gradient; row suffix "›" for Submenu/Screen, "On"/"Off" for Toggle (FR-017); `LaunchedEffect(selectedIndex)` scroll-to-visible logic (FR-016); row label resolved via `stringResource(child.titleKey)` — NOT `child.title` (FR-029, `data-model.md` MenuNode.titleKey)

- [ ] T018 [US1] Implement `android/app/src/main/kotlin/dev/douglasneto/beatside/ui/LcdScreen.kt`: title bar resolved via `stringResource(frame.node.titleKey)` + `MenuListContent` for Submenu nodes, placeholder `Box` for Screen nodes; border + background per retro aesthetic (FR-001, FR-002)

- [ ] T019 [US1] Implement `android/app/src/main/kotlin/dev/douglasneto/beatside/BeatSideApp.kt`: root composable holding `MenuState` and `WheelSettings` as `remember`/`mutableStateOf`; passes `Tick` events from `ClickWheel.onEvent` through `MenuNavigator.reduce`; passes `ToggleSetting` effects to `WheelSettings.toggled`; wires `ClickWheel` + `LcdScreen` in a `Column` (portrait, FR-001)

- [ ] T020 [US1] Implement `android/app/src/main/kotlin/dev/douglasneto/beatside/MainActivity.kt`: `ComponentActivity` with `setContent { BeatSideApp() }`; apply portrait-only orientation lock in `AndroidManifest.xml`

### iOS — US1 (parallel with T016–T020)

- [ ] T021 [P] [US1] Implement `ios/BeatSide/UI/ClickWheelView.swift`: `UIViewRepresentable` (or `Canvas`+gesture) forwarding `UITouch` events to `WheelTracker`; long-press `DispatchWorkItem` after 500 ms; drawing layer (ring, center button, button labels); dispatches `WheelEvent`s via callback closure; no feedback yet — `FeedbackPlayer` wired in T033 (Phase 5)

- [ ] T022 [P] [US1] Implement `ios/BeatSide/UI/MenuListView.swift`: `List` or `ScrollViewReader`+`ForEach` showing `MenuNode.Submenu` children; highlighted row; suffix "›" / "On" / "Off" (FR-017); scroll-to-visible on `selectedIndex` change (FR-016); row label resolved via `String(localized: child.titleKey)` — NOT `child.titleKey` literally (FR-029, `data-model.md` MenuNode.titleKey)

- [ ] T023 [P] [US1] Implement `ios/BeatSide/UI/LcdView.swift`: title bar resolved via `String(localized: frame.node.titleKey)` + `MenuListView` for Submenu, placeholder `EmptyView` for Screen; retro styling (FR-001, FR-002)

- [ ] T024 [P] [US1] Implement `ios/BeatSide/UI/BeatSideView.swift`: root `View` holding `@State` `MenuState` and `WheelSettings`; routes `Tick` → `MenuNavigator.reduce`; routes `ToggleSetting` → `WheelSettings.toggled`; `VStack` with `LcdView` + `ClickWheelView` (portrait)

- [ ] T025 [P] [US1] Implement `ios/BeatSide/BeatSideApp.swift`: `@main App` struct with `WindowGroup { BeatSideView() }`; portrait-only locked in `project.yml` via `UIInterfaceOrientationPortrait`

**Checkpoint**: App launches on Android emulator and iOS simulator. Rotating the wheel on the root menu scrolls the selection. At boundaries, selection stays clamped. `xcodegen generate` still works. (Click + haptic not expected until Phase 5.)

---

## Phase 4: User Story 2 — Open Items and Go Back (Priority: P2)

**Goal**: Center button tap opens the highlighted item (enters a submenu or leaf screen). MENU button goes back one level. At the root, MENU does nothing. Android system back gesture mirrors MENU (FR-014, Android only). Center button long-press is detected but does nothing yet.

**Independent Test**: Navigate into Settings submenu with center button; press MENU to return; verify back-stack depth. Press MENU at the root — nothing happens. Hold center for 500 ms — no crash, no nav action.

### Android — US2

- [ ] T026 [US2] Wire `Press(Center)` and `Press(Menu)` events in `android/app/src/main/kotlin/dev/douglasneto/beatside/BeatSideApp.kt`: `MenuNavigator.reduce(state, Press(Center))` enters items; `MenuNavigator.reduce(state, Press(Menu))` goes back; add `BackHandler(enabled = state.canGoBack) { /* Press(Menu) */ }` for system back gesture (FR-012, FR-013, FR-014 — Android only)

- [ ] T027 [US2] Add `RouteScreen` composable to `android/app/src/main/kotlin/dev/douglasneto/beatside/ui/LcdScreen.kt` routing `MenuNode.Screen` nodes to their leaf screen by `Route` value; for now all routes render `ComingSoonScreen.kt` stub (`CenteredMessage(stringResource(R.string.coming_soon), stringResource(R.string.press_menu_to_go_back))`) — FR-023, FR-026

### iOS — US2 (parallel with T026–T027)

- [ ] T028 [P] [US2] Wire `press(.center)` and `press(.menu)` events in `ios/BeatSide/UI/BeatSideView.swift`: center press → `MenuNavigator.reduce`; menu press → `MenuNavigator.back` (FR-012, FR-013); no system back gesture on iOS — FR-014 is Android-only

- [ ] T029 [P] [US2] Add `RouteView` switch to `ios/BeatSide/UI/LcdView.swift` routing `MenuNode.Screen` by `Route`; stub all routes with `ComingSoonView.swift` (localized "Coming soon" + "Press MENU to go back." text) — FR-023, FR-026

**Checkpoint**: Full forward-and-back navigation loop works on both platforms. Navigating into every submenu and back to root leaves state correct. Android back swipe behaves like MENU.

---

## Phase 5: User Story 3 — Feedback and Settings (Priority: P3)

**Goal**: Every detent plays a click and fires a light haptic. Every button press fires a medium haptic, no click. Settings → Clicker and Haptics toggles work immediately (next gesture). This phase also satisfies the deferred feedback criteria from US1 acceptance scenarios 1–3.

**Independent Test**: Navigate to Settings → toggle Clicker Off → rotate wheel → no click sound. Toggle Haptics Off → rotate → no haptic. Toggle both back On → both return. After this phase, US1 acceptance scenarios 1–3 (click + haptic on every detent) are also met.

### Android — US3

- [ ] T030 [US3] Implement `android/app/src/main/kotlin/dev/douglasneto/beatside/wheel/FeedbackPlayer.kt`: `SoundPool` (USAGE_ASSISTANCE_SONIFICATION, CONTENT_TYPE_SONIFICATION, maxStreams 4) loading `R.raw.click`; `play(event, settings)` method: Tick → play click if `settings.clickerEnabled`, fire `HapticFeedbackConstants.SEGMENT_TICK` (API 34+) or `CLOCK_TICK` (API < 34) if `settings.hapticsEnabled`; Press/LongPress → fire `CONFIRM` (API 33+) or `VIRTUAL_KEY` if `settings.hapticsEnabled`; `DisposableEffect` release (FR-018, FR-019, FR-021)

- [ ] T031 [US3] Wire `FeedbackPlayer` into `android/app/src/main/kotlin/dev/douglasneto/beatside/wheel/ClickWheel.kt`: add `FeedbackPlayer.play(event, wheelSettings)` call in the dispatch function; `WheelSettings` passed as parameter (FR-018, FR-019, FR-021); satisfies deferred US1 acceptance scenarios 1–3

### iOS — US3 (parallel with T030–T031)

- [ ] T032 [P] [US3] Implement `ios/BeatSide/UI/FeedbackPlayer.swift`: `AVAudioSession.sharedInstance().setCategory(.ambient)` + `AVAudioPlayer` loading `click.wav`; `play(event:settings:)` method mirroring Android logic: Tick → play if clickerEnabled, `UISelectionFeedbackGenerator.selectionChanged()` if hapticsEnabled; Press/LongPress → `UIImpactFeedbackGenerator(.medium).impactOccurred()` if hapticsEnabled (FR-018, FR-019, FR-021)

- [ ] T033 [P] [US3] Wire `FeedbackPlayer` into `ios/BeatSide/UI/ClickWheelView.swift`; pass `WheelSettings` from `BeatSideView` (FR-018, FR-019); satisfies deferred US1 acceptance scenarios 1–3

### Settings leaf screen (both platforms)

- [ ] T034 [US3] Verify Settings leaf screen flow on Android: `LcdScreen.kt` already renders `MenuNode.Toggle` rows via `MenuListContent` with "On"/"Off" suffix; confirm `ToggleSetting` effect → `WheelSettings.toggled()` state update → immediate re-render of suffix (FR-020, FR-021); no separate file needed beyond the existing wiring confirmed in T026

- [ ] T035 [P] [US3] Same verification on iOS in `BeatSideView.swift` / `MenuListView.swift` — confirm Toggle rows show "On"/"Off" and toggling via center button updates `WheelSettings` and re-renders suffix immediately (FR-020, FR-021)

**Checkpoint**: Click and haptic fire on every detent. Toggling Settings in-app silences click or haptic on the very next rotation. US1 acceptance scenarios 1–3 are now fully satisfied.

---

## Phase 6: User Story 4 — Browse Full Menu Tree (Priority: P4)

**Goal**: Every leaf screen in the tree renders correctly: Clock shows live local time in the device's 12/24-hour format; About shows app info; Coming Soon and placeholders render; MENU always returns to the parent with the same row highlighted.

**Independent Test**: Navigate to every leaf in the tree; verify correct screen. Set device to pt-BR locale — menu items in Portuguese. Set device to 12-hour format — Clock shows am/pm; switch to 24-hour — Clock switches immediately.

### Android — US4

- [ ] T036 [US4] Implement `android/app/src/main/kotlin/dev/douglasneto/beatside/ui/screens/ClockScreen.kt`: `produceState` updating every second with `LocalTime.now()`; format using `DateFormat.is24HourFormat(context)` → `HH:mm:ss` or `hh:mm:ss a`; register `BroadcastReceiver` for `Intent.ACTION_TIME_CHANGED` and `Intent.ACTION_TIMEZONE_CHANGED` in a `DisposableEffect` to trigger re-read of the format preference while the screen is open (FR-024, SC-006)

- [ ] T037 [US4] Implement `android/app/src/main/kotlin/dev/douglasneto/beatside/ui/screens/AboutScreen.kt` displaying app name and author from string resources, version from `BuildConfig.VERSION_NAME` (requires `buildFeatures { buildConfig = true }` added in T001); implement `NowPlayingScreen.kt` and ensure `ComingSoonScreen.kt` renders localized "Coming soon" with MENU back hint (FR-025, FR-026)

- [ ] T038 [US4] Wire all routes in `LcdScreen.kt` `RouteScreen`: `Route.Clock → ClockScreen()`, `Route.About → AboutScreen()`, `Route.ComingSoon → ComingSoonScreen()`, `Route.NowPlaying → NowPlayingScreen()`, `Route.ShuffleSongs → ComingSoonScreen()` (FR-022–FR-026)

- [ ] T039 [US4] Populate `android/app/src/main/res/values/strings.xml` with all user-facing strings using the same key constants defined in `BeatSideMenu.kt` (T011): `beatside`, `menu_music`, `menu_playlists`, `menu_artists`, `menu_albums`, `menu_songs`, `menu_genres`, `menu_extras`, `menu_clock`, `menu_stopwatch`, `menu_games`, `menu_settings`, `menu_clicker`, `menu_haptics`, `menu_about`, `menu_shuffle_songs`, `menu_now_playing`; also add screen titles, placeholder messages (`coming_soon`, `press_menu_to_go_back`), and accessibility labels for all five wheel buttons (FR-029); verify every `stringResource(child.titleKey)` call in T017/T018 resolves without a missing-key crash

### iOS — US4 (parallel with T036–T039)

- [ ] T040 [P] [US4] Implement `ios/BeatSide/UI/Screens/ClockView.swift`: `@State` `Date` updated every second by a `Timer.publish`; `Text(date, format: .dateTime.hour().minute().second())` auto-respects locale 12/24-hour preference; observe `NSLocale.currentLocaleDidChangeNotification` via `.onReceive` to force re-render when preference changes while open (FR-024, SC-006)

- [ ] T041 [P] [US4] Implement `ios/BeatSide/UI/Screens/AboutView.swift` (app name and author from localized strings, version from `Bundle.main.infoDictionary["CFBundleShortVersionString"]`); `NowPlayingView.swift` placeholder; `ComingSoonView.swift` placeholder with localized text (FR-025, FR-026)

- [ ] T042 [P] [US4] Wire all routes in `ios/BeatSide/UI/LcdView.swift` `RouteView` switch (FR-022–FR-026)

- [ ] T043 [P] [US4] Populate `ios/BeatSide/Resources/Localizable.xcstrings` with all English strings using the same key constants as T039 (matching `BeatSideMenu.swift` `titleKey` values from T014); reference `String(localized:)` or `LocalizedStringKey` throughout all views; verify every `String(localized: child.titleKey)` resolves without a missing-key warning (FR-029)

**Checkpoint**: Every leaf screen renders. Clock updates every second and respects the system 12/24-hour preference. All menu items are drawn from string resources, not literals.

---

## Phase 7: Polish and Cross-Cutting Concerns

**Purpose**: Accessibility (FR-027/028/031), Brazilian Portuguese localization (FR-029), and CI finalization. These are tested after the core UX is confirmed working.

- [ ] T044 Implement accessibility labels and custom actions on Android: add `Modifier.semantics { contentDescription = ...; customActions = listOf(AccessibilityAction("Next item") { /* Tick(+1) */ }, AccessibilityAction("Previous item") { /* Tick(-1) */ }, AccessibilityAction("Select") { /* Press(Center) */ }, AccessibilityAction("Go back") { /* Press(Menu) */ }) }` to `ClickWheel.kt`; after each navigation action post `view.announceForAccessibility(context.getString(newNode.titleKey))` or add a live region to the selection row in `MenuListContent.kt` (FR-031)

- [ ] T045 [P] Implement accessibility labels on Android ring buttons: `contentDescription` for each zone in `ClickWheel.kt` semantics (localized "Menu button", "Next button", "Play/Pause button", "Previous button"); add `contentDescription` to each menu row in `MenuListContent.kt` including role ("submenu") and state ("On"/"Off") suffixes (FR-027, FR-028)

- [ ] T046 [P] Implement accessibility on iOS `ClickWheelView.swift`: set `isAccessibilityElement = true`, `accessibilityTraits = [.adjustable]`; override `accessibilityIncrement()` → Tick(+1), `accessibilityDecrement()` → Tick(-1); add `UIAccessibilityCustomAction` for "Select" and "Go back"; post `UIAccessibility.post(notification: .announcement, argument: String(localized: newNode.titleKey))` after any navigation action (FR-027, FR-031)

- [ ] T047 [P] Add `accessibilityLabel` to each menu row in `ios/BeatSide/UI/MenuListView.swift` with localized name, role ("submenu"), and state ("On"/"Off") (FR-028)

- [ ] T048 Add Brazilian Portuguese translations to `android/app/src/main/res/values-pt-rBR/strings.xml` for every key in `values/strings.xml` (menu item titles, screen titles, button labels, accessibility labels, placeholder messages) (FR-029, SC-005)

- [ ] T049 [P] Add Brazilian Portuguese translations to `ios/BeatSide/Resources/Localizable.xcstrings` for every key added in T043 (FR-029, SC-005)

- [ ] T050 [P] Write Compose accessibility test `android/app/src/test/kotlin/dev/douglasneto/beatside/AccessibilityTest.kt` using `@RunWith(RobolectricTestRunner::class)` and `createComposeRule()` (Robolectric 4.14.1 — no emulator required; runs in the `android-test` CI job): verify "Next item", "Previous item", "Select", "Go back" custom actions present on the wheel; verify content descriptions on ring buttons; verify menu row descriptions include role/state (SC-004, FR-027, FR-028, FR-031) **Note**: Robolectric 4.14.x supports up to API 35; annotate the test class with `@Config(sdk = [35])` (or add `app/src/test/resources/robolectric.properties` with `sdk=35`) because the app compiles against API 37 and targets API 36.

- [ ] T051 [P] Write XCUITest `ios/BeatSideUITests/AccessibilityUITests.swift` in the `BeatSideUITests` target (`bundle.ui-testing` added in T002): verify wheel's adjustable trait; verify four custom actions present; verify row accessibility labels include role and state; runs in `ios-ui-test` CI job (SC-004, FR-027, FR-028, FR-031)

- [ ] T052 Finalize `.github/workflows/ci.yml`: confirm `android-test` job runs T050 Robolectric tests via `./gradlew :app:testDebugUnitTest` (no emulator needed); confirm `ios-test` job runs unit tests; confirm `ios-ui-test` job runs `BeatSideUITests` scheme; add branch protection note in README (SC-004)

- [ ] T053 [P] Run `quickstart.md` acceptance checklist end-to-end on both platforms: verify SC-001 (≤10 gestures to any leaf), SC-002 (unit test D-1 passes), SC-003 (settings instant), SC-004 (accessibility tests green), SC-005 (pt-BR strings present), SC-006 (Clock format), SC-007 (screen-reader navigation)

**Checkpoint**: All 7 success criteria pass. CI workflow green on all three jobs. TalkBack and VoiceOver can navigate the full menu tree using wheel accessibility actions.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately.
- **Phase 2 (Foundational)**: Requires Phase 1 (T001a must pass). BLOCKS all user story work.
- **Phase 3 (US1)**: Requires Phase 2. Can begin as soon as T007 + T009 + T011 + T014 are done.
- **Phase 4 (US2)**: Requires Phase 3. Navigation wiring depends on working ClickWheel + LcdScreen.
- **Phase 5 (US3)**: Requires Phase 4 (FeedbackPlayer is wired into ClickWheel); also completes the deferred click + haptic criteria from US1 acceptance scenarios 1–3.
- **Phase 6 (US4)**: Requires Phase 4 (leaf screen routing uses existing LcdScreen switch) and Phase 2 (titleKey keys defined in T011/T014 must exist before T039/T043 populate the string files).
- **Phase 7 (Polish)**: T044–T049 require Phase 3 (ClickWheel exists); T050–T053 require Phase 6.

### User Story Dependencies

- **US1 (P1)**: Depends on Phase 2 only. Click + haptic acceptance criteria deferred to US3.
- **US2 (P2)**: Depends on US1 (needs ClickWheel + LcdScreen + BeatSideApp).
- **US3 (P3)**: Depends on US2; wires FeedbackPlayer into existing ClickWheel.
- **US4 (P4)**: Depends on US2 (leaf screen routing) and Phase 2 (titleKey constants).

### Within Each Phase

- Android and iOS tasks are independent and may proceed in parallel.
- Tests MUST be written and confirmed failing before the implementation task they cover.
- Both `[P]` pairs within a platform (e.g., T006 Android vs. T008 iOS) are fully independent.

---

## Parallel Execution Examples

### Phase 1 (T001a is sequential after T001)

```
T001 → T001a (sequential: must verify build before proceeding)
T002, T003, T004, T005 in parallel with each other (but after T001)
```

### Phase 2 (Android + iOS in parallel)

```
Agent A (Android):  T006 → T007 → T010 → T011 → T012
Agent B (iOS):      T008 → T009 → T013 → T014 → T015
```

### Phase 3 US1 (Android + iOS in parallel)

```
Agent A (Android):  T016 → T017 → T018 → T019 → T020
Agent B (iOS):      T021 → T022 → T023 → T024 → T025
```

### Phase 7 Polish (many parallel streams)

```
Agent A: T044 → T045 → T050
Agent B: T046 → T047 → T051
Agent C: T048 → T049
Agent D: T052 → T053
```

---

## Implementation Strategy

### MVP First (User Story 1 only)

1. Complete Phase 1: Setup (including T001a build verification)
2. Complete Phase 2: Foundational — WheelTracker + MenuNavigator tests and implementations
3. Complete Phase 3: US1 — Wheel UI + list display on both platforms
4. **STOP and VALIDATE**: Rotate the wheel, see the list scroll, verify clamping — both platforms
5. Continue to Phase 4 when US1 is confirmed (note: feedback arrives in Phase 5)

### Incremental Delivery

1. Phase 1 + 2 → Core types green, both CI jobs pass (empty UI)
2. Phase 3 → Rotating wheel navigates list → **MVP demonstrable** (without feedback yet)
3. Phase 4 → Open + back navigation complete
4. Phase 5 → Click + haptic feedback + Settings (US1 scenarios 1–3 now fully met)
5. Phase 6 → Full menu tree with Clock, About, all placeholders; pt-BR string keys wired
6. Phase 7 → Accessibility, pt-BR translations, CI hardened → **Ship-ready**

---

## Notes

- `[P]` tasks operate on different files and have no data dependencies within their group.
- Every pure logic task (Phases 1–2) must have its test written and confirmed failing before the implementation task begins — this is non-negotiable per Constitution III.
- `MenuNode.titleKey` is a string resource key, not a display string. Never put English text in `BeatSideMenu.kt` or `BeatSideMenu.swift`; put it in `strings.xml` / `Localizable.xcstrings` (T039/T043).
- T050 uses Robolectric (no emulator); T051 uses XCUITest in `BeatSideUITests` (`bundle.ui-testing`).
- Do not add sensitivity options, persistence, music playback, or any other out-of-scope feature.
- Commit after each task or logical group following Conventional Commits (`feat:`, `test:`, `chore:`, etc.).
- The `prototype/wheel-and-menus` branch is reference material: code may be copied from it only after confirming it satisfies the spec and passes the contract tests.
