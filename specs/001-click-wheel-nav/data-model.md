# Data Model: Click Wheel and Menu Navigation

**Feature**: 001-click-wheel-nav | **Date**: 2026-11-21

This document describes the core types shared by both platforms at the behavioral level.
Platform-specific representations (Kotlin `sealed interface` vs. Swift `enum`) are implementation
details; the logical shape is identical.

---

## WheelButton

The five physical controls on the wheel.

| Value | Zone (screen coordinates, y grows down) | Notes |
|---|---|---|
| `Center` | Distance from center ≤ 36 % of outer radius | The select button |
| `Menu` | Ring, angle ∈ [−135°, −45°) | Top of the ring |
| `Next` | Ring, angle ∈ [−45°, 45°) | Right of the ring |
| `PlayPause` | Ring, angle ∈ [45°, 135°) | Bottom of the ring |
| `Previous` | Ring, everything else | Left of the ring |

Angles are computed as `atan2(dy, dx)` in screen coordinates where −90° is the top.
Every angle belongs to exactly one zone (half-open intervals, no gaps).

---

## WheelEvent

The only currency screens accept from the wheel. No screen ever sees a raw touch.

| Variant | Fields | Meaning |
|---|---|---|
| `Tick` | `direction: Int` (+1 or −1) | One detent of rotation. +1 = clockwise = down a list. |
| `Press` | `button: WheelButton` | A short tap on a button, or a ring button released after any hold. |
| `LongPress` | `button: WheelButton` | Only ever `Center`; held ≥ 500 ms without rotation or slop. |

**Invariant**: a `LongPress(Center)` suppresses the subsequent `Press(Center)`. No other
`LongPress` variant is ever emitted.

---

## WheelTracker (behavioral contract)

Stateful gesture interpreter. One instance per active touch sequence.

| Input | Output | Side-effects |
|---|---|---|
| `down(x, y, diameter)` → `Bool` | `false` if outside wheel | Resets accumulator, angle anchor, flags |
| `move(x, y)` → `[WheelEvent]` | Zero or more `Tick`s | Updates accumulator |
| `longPressTimeout()` → `[WheelEvent]` | At most one `LongPress(Center)`, or empty | Sets longPressFired |
| `up()` → `[WheelEvent]` | At most one `Press(button)`, or empty | Clears active flag |
| `cancel()` | — | Clears active flag; discards gesture |

**Geometry constants**:

| Constant | Value | Meaning |
|---|---|---|
| `CENTER_RATIO` | 0.36 | Center button radius = 0.36 × outer radius |
| `DEAD_ZONE_RATIO` | 0.20 | Samples closer than 0.20 × outer radius ignored |
| `STEP_DEGREES` | 15.0 | Degrees per detent at Normal sensitivity |
| `TAP_SLOP` | 8 dp / 8 pt | Maximum movement for a gesture to count as a tap |
| `LONG_PRESS_MS` | 500 | Milliseconds of stillness before long-press fires |

---

## MenuNode

One node in the navigation tree. The whole app is a tree of these.

| Variant | Fields | Rendered as |
|---|---|---|
| `Submenu` | `titleKey: String`, `children: [MenuNode]` | A scrollable list |
| `Screen` | `titleKey: String`, `route: Route` | A leaf screen (driven by route) |
| `Toggle` | `titleKey: String`, `key: SettingKey` | A row showing "On" / "Off" |

**`titleKey`** is a locale-neutral string resource key (e.g. `"menu_music"`). It is defined in
the pure core and resolved by the UI layer: `stringResource(node.titleKey)` on Android,
`String(localized: node.titleKey)` on iOS. This keeps the core framework-free while satisfying
FR-029 (all user-facing text externalized and available in English and Brazilian Portuguese).

`Toggle` nodes are never pushed onto the navigation stack; selecting them emits a
`ToggleSetting` effect instead.

---

## Route

Identifies which leaf screen to render when a `Screen` node is entered.

| Value | Screen rendered |
|---|---|
| `ComingSoon` | Placeholder: "Coming soon" + "Press MENU to go back." |
| `NowPlaying` | Placeholder for future music control |
| `ShuffleSongs` | Placeholder |
| `Clock` | Live local time, device 12/24-hour format, updates every second |
| `About` | App name, version, author |

---

## SettingKey

Identifies a boolean setting toggled from the Settings submenu.

| Value | Default | Meaning |
|---|---|---|
| `Clicker` | `true` | Play the click WAV on every tick |
| `Haptics` | `true` | Fire haptic feedback on ticks and presses |

---

## WheelSettings

In-session state; not persisted across launches (FR-030).

| Field | Type | Default |
|---|---|---|
| `clickerEnabled` | Boolean | `true` |
| `hapticsEnabled` | Boolean | `true` |

**Mutation**: toggling is the only operation. When `SettingKey.Clicker` is toggled,
`clickerEnabled` flips; similarly for `Haptics`. The change takes effect on the next event.

---

## MenuFrame

One entry on the navigation stack.

| Field | Type | Notes |
|---|---|---|
| `node` | `MenuNode` | The screen currently shown |
| `selectedIndex` | Int | The highlighted row index; 0 for leaf screens |

---

## MenuState

The full navigation stack.

| Field | Type | Notes |
|---|---|---|
| `stack` | `[MenuFrame]` | Never empty; root is always at index 0 |
| `current` | `MenuFrame` (derived) | `stack.last()` |
| `canGoBack` | Bool (derived) | `stack.count > 1` |

---

## MenuEffect

A side effect the navigator cannot handle on its own.

| Variant | Fields | Meaning |
|---|---|---|
| `ToggleSetting` | `key: SettingKey` | Flip a setting; handled by the UI layer |
| `Unhandled` | `event: WheelEvent` | An event that has no meaning in the current context (e.g. PlayPause in a menu) |

---

## MenuNavigator (behavioral contract)

Pure reducer. No I/O, no side effects.

```
reduce(state: MenuState, event: WheelEvent) → (MenuState, MenuEffect?)
```

| Event | Condition | New State | Effect |
|---|---|---|---|
| `Tick(d)` | Current node is a `Submenu` | `selectedIndex` clamped to [0, children.count − 1] | — |
| `Tick(d)` | Current node is a `Screen` | Unchanged | `Unhandled(Tick(d))` |
| `Press(Center)` | Current is `Submenu`, selected child is `Submenu` or `Screen` | Push child as new frame | — |
| `Press(Center)` | Current is `Submenu`, selected child is `Toggle` | Unchanged | `ToggleSetting(key)` |
| `Press(Menu)` | `canGoBack` | Pop stack | — |
| `Press(Menu)` | At root | Unchanged | — |
| `Press(Next/Prev/PP)` | Any | Unchanged | `Unhandled(press)` |
| `LongPress(Center)` | Any | Unchanged | `Unhandled(longPress)` |

---

## BeatSideMenu (static tree for this slice)

`titleKey` values are string resource keys defined in `BeatSideMenu` and resolved by the UI layer.

```
Root  titleKey="app_name"
├── Submenu  titleKey="menu_music"
│   ├── Screen  titleKey="menu_playlists"   → ComingSoon
│   ├── Screen  titleKey="menu_artists"     → ComingSoon
│   ├── Screen  titleKey="menu_albums"      → ComingSoon
│   ├── Screen  titleKey="menu_songs"       → ComingSoon
│   └── Screen  titleKey="menu_genres"      → ComingSoon
├── Submenu  titleKey="menu_extras"
│   ├── Screen  titleKey="menu_clock"       → Clock
│   ├── Screen  titleKey="menu_stopwatch"   → ComingSoon
│   └── Screen  titleKey="menu_games"       → ComingSoon
├── Submenu  titleKey="menu_settings"
│   ├── Toggle  titleKey="menu_clicker"     → SettingKey.Clicker
│   ├── Toggle  titleKey="menu_haptics"     → SettingKey.Haptics
│   └── Screen  titleKey="menu_about"       → About
├── Screen   titleKey="menu_shuffle_songs"  → ShuffleSongs (placeholder)
└── Screen   titleKey="menu_now_playing"    → NowPlaying (placeholder)
```
