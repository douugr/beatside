# BeatSide Constitution

## Core Principles

### I. Native per Platform, Parity by Spec

- The Android app MUST be written in Kotlin with Jetpack Compose, and the iOS app in Swift with
  SwiftUI. Cross-platform UI frameworks (Flutter, React Native, Kotlin Multiplatform UI) MUST NOT
  be used, and the two apps MUST NOT share code.
- What the apps share is **behavior**. Every feature spec MUST describe the behavior once, for both
  platforms, and MUST list every platform difference explicitly (for example, universal media
  control exists only on Android).
- A feature is done for a platform only when it matches its spec on that platform. A spec MAY ship
  on one platform first, but the gap MUST be recorded in the spec.

Rationale: native code gives the best feel and platform integration; specs keep two codebases
from drifting apart.

### II. The Wheel Is the Interface

- Every screen MUST be fully operable with the wheel alone: rotation, the center button and the
  four ring buttons (Menu, Next, Previous, Play/Pause).
- Screens MUST consume only `WheelEvent`s (`tick`, `press`, `longPress`), never raw touches. The
  touch-to-event translation lives in one place per platform.
- Every tick MUST produce its click sound and haptic in the same frame, unless the user turned
  them off. Wheel input MUST never be dropped or delayed by loading work: I/O runs off the main
  thread.
- The original device's interaction model (menu tree, clamped lists, Menu to go back) is the
  default. Departures from it MUST be justified in the feature spec.

Rationale: the tactile wheel is the reason the app exists; if it feels wrong, nothing else matters.

### III. Pure Logic, Tested First (NON-NEGOTIABLE)

- Input math, navigation, state reducers and data mapping MUST live in pure types with no UI
  framework imports, so they run in plain unit tests (JUnit on Android, Swift Testing on iOS).
- Tests for that logic MUST be written from the spec's acceptance scenarios **before** the
  implementation, and MUST fail first.
- Both platforms MUST implement the same test cases for shared behavior. When a test case
  changes on one platform, the spec and the other platform's test change with it.
- Floating-point tests MUST keep a margin from exact thresholds so they cannot flip on rounding.

Rationale: the hardest bugs in this app are in invisible math (angles, detents, clamping), and
two codebases need one shared definition of correct.

### IV. Official APIs and Store Rules Only

- Integrations MUST use official, documented APIs and SDKs. Unofficial or reverse-engineered APIs
  (for example, private YouTube Music endpoints) MUST NOT be used.
- Every feature MUST comply with Google Play and App Store policies. Sensitive permissions (such as
  notification access for universal media control) MUST be requested only when the feature that
  needs them is first used, with an explanation screen, and the app MUST stay usable if the user
  says no.
- Service limits MUST be designed around, not worked around. Spotify library features MUST stay
  behind a setting while the app runs under Spotify's Development Mode user cap.

Rationale: the goal is a published app, and policy violations end in removal from the stores.

### V. Privacy by Default

- The app MUST NOT include analytics, advertising or tracking SDKs.
- User data (libraries, listening state, settings) MUST stay on the device. Service tokens MUST be
  stored only in the Android Keystore-backed storage or the iOS Keychain.
- Secrets (API keys, client secrets, keystores, signing files) MUST NOT be committed. Configuration
  that needs them MUST read them from untracked local files or CI secrets.

Rationale: a music player has no reason to know anything about its user beyond their music.

### VI. Original Identity

- The name, store listings, UI copy and artwork MUST NOT use Apple trademarks ("iPod",
  "Click Wheel") or copy Apple's artwork. The look MAY evoke 2000s portable players through
  original design.
- Store listings MUST state that BeatSide is not affiliated with Apple, Spotify, Google or Deezer.

Rationale: trademark complaints are the fastest way to lose a store listing.

### VII. Small, Shippable Slices

- Each feature spec MUST be the smallest slice that delivers value on its own and can be released
  independently.
- Code MUST NOT be written for requirements that no accepted spec contains. Abstractions are
  introduced when a second real use appears, not before.

Rationale: a hobby project survives by shipping often and staying small.

## Technical Constraints

- **Android:** minimum SDK 26; target SDK as required by current Google Play policy; Gradle with a
  version catalog (`android/gradle/libs.versions.toml`); AGP built-in Kotlin.
- **iOS:** minimum iOS 17; Swift 6 language mode; the Xcode project is generated from
  `ios/project.yml` by XcodeGen and is not committed.
- **Repository:** monorepo with `android/`, `ios/` and `specs/`. Specs follow the Spec Kit layout
  (`specs/NNN-short-name/` with `spec.md`, `plan.md`, `tasks.md`).
- **Localization:** all user-facing strings MUST be externalized (Android string resources, iOS
  String Catalogs). English is the base language; Brazilian Portuguese is the first translation.
- **Accessibility:** wheel buttons and list rows MUST expose accessibility labels so TalkBack and
  VoiceOver users can navigate.

## Development Workflow

- Work follows the Spec Kit flow: `/speckit-specify` → (`/speckit-clarify`) → `/speckit-plan` →
  `/speckit-tasks` → (`/speckit-analyze`) → `/speckit-implement`.
- Each feature gets a branch named after its spec directory (for example, `001-click-wheel`) and
  is merged into `main` through a pull request.
- A pull request MUST NOT be merged unless the unit tests for every platform it touches pass and
  its tasks in `tasks.md` are checked off.
- When behavior changes, the spec changes in the same pull request. Code that disagrees with its
  spec is a bug in one of them, and the PR MUST say which.
- Commit messages follow Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`…).
- The `prototype/wheel-and-menus` branch is reference material only. Code MAY be copied from it
  only when it satisfies an accepted spec and passes that spec's tests.

## Governance

- This constitution supersedes any other practice or document in the repository. Plans produced
  by `/speckit-plan` MUST pass its Constitution Check, and every violation MUST be justified in the
  plan's Complexity Tracking table.
- Amendments are made through a pull request that edits this file, explains the reason, and bumps
  the version:
  - MAJOR: a principle is removed or redefined in an incompatible way.
  - MINOR: a principle or section is added or materially expanded.
  - PATCH: clarifications and wording fixes.
- Reviewers check every pull request against these principles. Runtime guidance for agents lives
  in `CLAUDE.md` and MUST NOT contradict this document.

**Version**: 1.0.0 | **Ratified**: 2026-11-21 | **Last Amended**: 2026-11-21
