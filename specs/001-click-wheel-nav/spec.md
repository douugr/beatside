# Feature Specification: Click Wheel and Menu Navigation

**Feature Branch**: `001-click-wheel-nav`

**Created**: 2026-11-21

**Status**: Draft

**Input**: User description: "Click wheel and menu navigation: the first usable slice of BeatSide, on Android and iOS."

## Clarifications

### Session 2026-11-21

- Q: How should a screen-reader user navigate the menu when touch gestures are intercepted? → A: The wheel component exposes four accessibility actions — "Next item", "Previous item", "Select", "Go back". Menu rows do not need to be individually focusable.
- Q: What happens when a ring button is held still for 500 ms? → A: Ring buttons produce a normal `Press(button)` on release regardless of hold duration; long-press detection applies only to the center button.
- Q: Should the Clock screen follow the device's 12/24-hour system setting or always use 24-hour? → A: Follow the device's 12/24-hour system setting, switching automatically if changed while the screen is open.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Rotate wheel to navigate a list (Priority: P1)

A user opens BeatSide and sees the root menu. They place a finger on the wheel ring and rotate
clockwise to move the highlight down the list, then counter-clockwise to move it back up.
Each detent produces an audible click and a light haptic. The highlight stops at the bottom row
and does not wrap around.

**Why this priority**: The wheel is the entire interface. If rotation does not reliably move the
selection by exactly one row per detent, the app is unusable. Everything else depends on this.

**Independent Test**: Can be fully tested by launching the app and rotating the wheel on the
root menu without pressing any button — delivers demonstrable, feel-based value on its own.

**Acceptance Scenarios**:

1. **Given** the root menu is displayed with the first row highlighted, **When** the user rotates
   the wheel clockwise by 15°, **Then** the highlight moves to the second row, one click sounds,
   and one light haptic fires.
2. **Given** the last row is highlighted, **When** the user rotates the wheel clockwise by any
   amount, **Then** the highlight stays on the last row, the click and haptic still fire (the
   wheel always feels physical), and no row wraps to the top.
3. **Given** the first row is highlighted, **When** the user rotates the wheel counter-clockwise
   by 15°, **Then** the highlight stays on the first row (clamped) with click and haptic.
4. **Given** the user moves their finger back and forth by less than 15° without crossing a detent
   boundary, **Then** no row change occurs.
5. **Given** a touch started in the ring and crosses the center dead-zone (inner 20 % of radius),
   **Then** no tick fires during the dead-zone crossing, and rotation resumes cleanly once the
   finger re-enters the ring.
6. **Given** a touch starts on the center button, **Then** no rotation ticks are emitted regardless
   of subsequent finger movement.

---

### User Story 2 - Open items and go back with the wheel buttons (Priority: P2)

Having highlighted an item with the wheel, the user presses the center button to open it. They
navigate into a submenu, then press the MENU button (top of the ring) to return one level. At the
root, pressing MENU does nothing. The system back gesture mirrors MENU on Android, except at the
root, where it leaves the app.

**Why this priority**: Navigation is the second pillar of the interaction model. Without open and
back, the user is trapped on the first screen.

**Independent Test**: Fully testable by navigating into one submenu and pressing MENU to return,
delivering a complete forward-and-back navigation loop.

**Acceptance Scenarios**:

1. **Given** the root menu is showing, **When** the user taps the center button with "Settings"
   highlighted, **Then** the Settings submenu opens with its first row highlighted.
2. **Given** the Settings submenu is showing, **When** the user taps the MENU ring button (top
   quarter of ring), **Then** the app returns to the root menu, with "Settings" still highlighted.
3. **Given** the root menu is showing, **When** the user taps the MENU ring button, **Then**
   nothing happens (root is the floor).
4. **Given** an Android device showing a submenu, **When** the user triggers the system back
   gesture, **Then** the app behaves identically to pressing MENU.
5. **Given** the user holds the center button for 500 ms without moving, **Then** a long-press
   event is detected and no select action fires on release.
6. **Given** a screen reader is active and the wheel is focused, **When** the user invokes the
   "Next item" or "Previous item" accessibility action, **Then** the selection moves by one row
   (clamped at first/last) exactly as a wheel tick would.
7. **Given** a screen reader is active and the wheel is focused, **When** the user invokes the
   "Select" accessibility action, **Then** the highlighted item opens exactly as a center-button
   tap would.
8. **Given** a screen reader is active and the wheel is focused, **When** the user invokes the
   "Go back" accessibility action, **Then** the app goes up one level exactly as a MENU tap would.
9. **Given** an Android device showing the root menu, **When** the user triggers the system back
   gesture, **Then** the app leaves the root and the system handles back (the app is closed or
   sent to the background), unlike MENU, which does nothing at the root.

---

### User Story 3 - Hear and feel wheel feedback; configure it in Settings (Priority: P3)

The user explores the full menu tree and experiences click sounds and haptics on every detent and
stronger haptics on button presses. They navigate to Settings and toggle Clicker or Haptics off;
the change takes effect on the very next detent without restarting the app.

**Why this priority**: Feedback completes the tactile identity of the wheel. Settings are needed
to satisfy users in quiet or accessibility contexts.

**Independent Test**: Testable by navigating Settings → Clicker → Off and immediately rotating
the wheel — no click should sound.

**Acceptance Scenarios**:

1. **Given** Clicker is On and Haptics is On (defaults), **When** the user rotates the wheel one
   detent, **Then** a short click plays and a light haptic fires.
2. **Given** Clicker is On, **When** the user taps a ring button, **Then** a medium haptic fires
   and no click plays.
3. **Given** the user navigates to Settings and taps "Clicker" to toggle it Off, **When** the user
   immediately rotates the wheel one detent, **Then** no click sound plays (haptic still fires if
   Haptics is On).
4. **Given** Haptics is Off, **When** the user rotates the wheel, **Then** no haptic fires
   regardless of Clicker state.

---

### User Story 4 - Browse the full menu tree (Priority: P4)

The user can navigate the entire menu tree described in this slice: Root → Music (sub-items open a
"Coming soon" screen), Extras → Clock (shows live time), Extras → Stopwatch / Games ("Coming
soon"), Settings → About (app info), Shuffle Songs and Now Playing (placeholder screens).

**Why this priority**: The tree validates that the navigation infrastructure handles multiple levels
and varied leaf-screen types.

**Independent Test**: Testable by navigating to every leaf in the tree and verifying the correct
screen appears.

**Acceptance Scenarios**:

1. **Given** the root menu, **When** the user opens Music → Playlists, **Then** a "Coming soon"
   screen appears with a MENU button to go back.
2. **Given** the Extras submenu, **When** the user opens Clock, **Then** the current local time
   is displayed in the format matching the device's 12/24-hour system setting, and updates every
   second.
3. **Given** the Settings submenu, **When** the user opens About, **Then** the app name, version
   number and author are shown.
4. **Given** any "Coming soon" or placeholder screen, **When** the user presses MENU, **Then**
   the app returns to the parent menu with the same row highlighted.

---

### Edge Cases

- What happens when the wheel ring receives a tap at a zone boundary? → Boundaries are defined
  by half-open intervals: [−135°, −45°) = MENU, [−45°, 45°) = Next, [45°, 135°) = Play/Pause,
  everything else = Previous. Each angle belongs to exactly one zone so no ambiguity exists at
  the boundary.
- How does the app handle a drag that starts in the ring, moves into the dead zone, and exits on
  the opposite side of the wheel? → The dead zone absorbs the crossing; rotation resumes cleanly
  after the first sample outside the dead zone re-anchors the angle.
- What if the device has no haptic actuator? → Haptic calls are silently no-ops; the toggle
  remains visible but effectively does nothing.
- What if the screen is very small (e.g. a small phone) and the menu list has many rows? →
  The list scrolls to keep the highlighted row fully visible; no row is ever clipped away.

## Requirements *(mandatory)*

### Functional Requirements

**Screen layout**

- **FR-001**: The app MUST display a portrait-only layout consisting of an LCD area at the top
  (title bar + scrollable content region) and a round wheel below it.
- **FR-002**: The LCD area MUST show a title bar with the current screen's name and a content
  region showing the menu list or leaf screen.

**Wheel rotation**

- **FR-003**: One detent MUST equal exactly 15° of finger travel around the ring (24 detents per
  full revolution).
- **FR-004**: The wheel tracker MUST use angle normalization that maps any delta to (−180°, 180°]
  so that crossing the 0°/360° boundary never produces a burst of ticks.
- **FR-005**: The tracker MUST use an accumulator: a tick is emitted only when the accumulated
  angle reaches ±15°; leftover angle carries forward.
- **FR-006**: Samples whose touch point is within 20 % of the wheel's outer radius from the center
  MUST be treated as dead-zone and ignored; the first sample after leaving the dead zone
  re-anchors the last angle without emitting a tick.
- **FR-007**: A touch that begins on the center button MUST NOT emit rotation ticks.

**Wheel buttons (taps and long press)**

- **FR-008**: A gesture that ends without emitting any tick and without moving more than 8 dp /
  8 pt from its start MUST be classified as a tap. If the touch started on the ring, the tap
  triggers the button zone under the start point (see FR-009). If the touch started on the center
  button (within 36 % of the wheel's outer radius), the tap triggers a `Press(Center)` event.
- **FR-009**: Button zones on the ring MUST use the following half-open angle intervals, measured
  in screen coordinates where y grows downward and −90° is the top of the wheel:
  [−135°, −45°) = MENU; [−45°, 45°) = Next; [45°, 135°) = Play/Pause;
  [135°, 180°] ∪ (−180°, −135°) = Previous. Every angle belongs to exactly one zone.
- **FR-010**: A touch on the center button (within 36 % of the wheel's outer radius) held still
  for 500 ms MUST emit a `LongPress(Center)` event and MUST NOT also emit a `Press(Center)` event
  on release. Ring buttons (MENU, Next, Play/Pause, Previous) do NOT trigger long-press detection;
  a ring button held for any duration fires a normal `Press(button)` on release.
- **FR-011**: Next, Previous and Play/Pause taps MUST be detected and emitted as events but have
  no effect on navigation in this slice.

**Navigation**

- **FR-012**: The center button tap (select) MUST open the highlighted menu item.
- **FR-013**: The MENU button tap MUST navigate one level up in the menu tree; at the root it
  MUST do nothing.
- **FR-014**: On Android, the system back gesture MUST behave identically to the MENU button
  while not at the root; at the root it leaves the app.
- **FR-015**: The list selection MUST clamp at the first and last rows (no wrap-around).
- **FR-016**: The content region MUST scroll the minimum amount necessary to keep the highlighted
  row fully visible after every tick.
- **FR-017**: Rows that navigate to a submenu or screen MUST display a ">" marker; Settings
  toggle rows MUST display "On" or "Off" reflecting current state.

**Feedback**

- **FR-018**: Every tick MUST play a short click sound AND fire a light haptic, unless the user
  has disabled them individually.
  On iOS the click sound follows the Ring/Silent switch (silent mode mutes the click; haptics
  still fire).
- **FR-019**: Every button press MUST fire a medium haptic and MUST NOT play a click sound.
- **FR-020**: The Settings screen MUST provide Clicker (on/off) and Haptics (on/off) toggles.
- **FR-021**: Toggling Clicker or Haptics MUST take effect on the very next tick or button press
  without any app restart.

**Menu tree**

- **FR-022**: The root menu MUST contain exactly: Music, Extras, Settings, Shuffle Songs, Now
  Playing (in this order).
- **FR-023**: The Music submenu MUST contain: Playlists, Artists, Albums, Songs, Genres; each
  MUST open a "Coming soon" placeholder screen.
- **FR-024**: The Extras submenu MUST contain: Clock, Stopwatch, Games. Clock MUST display the
  current local time formatted according to the device's 12/24-hour system setting, updating every
  second, and MUST switch format automatically if the system setting changes while the screen is
  open. Stopwatch and Games MUST open "Coming soon" screens.
- **FR-025**: The Settings submenu MUST contain: Clicker toggle, Haptics toggle, About. About
  MUST display the app name, version, and author.
- **FR-026**: Shuffle Songs and Now Playing MUST open placeholder screens.

**Accessibility**

- **FR-027**: Every wheel ring button zone (MENU, Next, Play/Pause, Previous) and the center
  button MUST have an accessibility label for TalkBack (Android) and VoiceOver (iOS).
- **FR-028**: Every menu row MUST have an accessibility label describing its name and, where
  applicable, its state ("On" / "Off") or role ("submenu").
- **FR-031**: When a screen reader (TalkBack on Android, VoiceOver on iOS) is active, the wheel
  component MUST expose four platform accessibility actions: "Next item" (equivalent to one
  clockwise tick moving the selection down), "Previous item" (equivalent to one counter-clockwise
  tick moving the selection up), "Select" (equivalent to `Press(Center)`), and "Go back"
  (equivalent to `Press(Menu)`). Menu rows MUST NOT require individual accessibility focus
  traversal; the wheel's actions are the sole navigation mechanism for screen-reader users.

**Localization**

- **FR-029**: All user-facing strings (menu items, screen titles, accessibility labels,
  placeholder text) MUST be externalized and available in English and Brazilian Portuguese.

**Platform constraints**

- **FR-030**: Settings state (Clicker on/off, Haptics on/off) MUST persist only for the duration
  of the app session; persistence across launches is out of scope.

### Key Entities

- **WheelEvent**: A discrete event emitted by the wheel tracker — `Tick(direction)`,
  `Press(button)`, or `LongPress(button)`. Screens consume only WheelEvents, never raw touches.
- **Button**: One of `Center`, `Menu`, `Next`, `PlayPause`, `Previous`.
- **MenuItem**: A node in the menu tree with a title, an optional state (for toggles), a
  child-indicator flag, and an action (open screen, toggle, no-op).
- **NavigationStack**: The ordered stack of screens the user has descended through; MENU pops it.
- **WheelSettings**: The in-session flags `clickerEnabled` and `hapticsEnabled`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can reach any leaf screen in the menu tree within 10 wheel rotations and
  button presses from launch.
- **SC-002**: Every clockwise 15° of rotation produces exactly one downward row movement; no
  burst of ticks occurs when crossing any point of the circle, verifiable by automated unit test.
- **SC-003**: Toggling a Setting takes effect within one subsequent interaction (the very next
  tick or press) with no perceptible delay.
- **SC-004**: All wheel buttons and menu rows pass automated accessibility inspection (content
  descriptions / accessibility labels present) on both platforms.
- **SC-005**: The app correctly displays all menu text in both English and Brazilian Portuguese
  with no missing or placeholder string keys.
- **SC-006**: The Clock screen shows the correct local time formatted according to the device's
  12/24-hour system setting, updates every second, and switches format automatically when the
  system setting changes — verified manually over a 5-second interval on both format modes.
- **SC-007**: With a screen reader active, a user can navigate to any leaf screen and return to
  root using only the wheel's accessibility actions, verified by automated accessibility test on
  both platforms.

## Assumptions

- Settings (Clicker, Haptics) are not persisted between app launches; default is both On.
- The wheel outer radius is half of its rendered width; exact pixel dimensions are a design
  decision for the plan phase.
- Wheel sensitivity is fixed at the Normal preset (15° / detent); sensitivity options are out of
  scope for this slice.
- The app starts at the root menu on every launch; no session restore is required.
- Playing, pausing or controlling media is entirely out of scope; buttons related to playback are
  detected but have no effect.
- Localization covers English and Brazilian Portuguese only; no other languages are required.
- No Apple trademarks ("iPod", "Click Wheel") appear anywhere in the UI or assets.
- The prototype branch (`prototype/wheel-and-menus`) may be referenced and partially reused
  provided the copied code satisfies this spec and passes its tests.
