# Wheel Behavior Contract

**Feature**: 001-click-wheel-nav | **Date**: 2026-11-21

This file is the **single source of truth** for `WheelTracker` test cases. Both the Android
(JUnit / Kotlin) and iOS (Swift Testing / Swift) test suites MUST implement every case in this
table verbatim — same inputs, same expected outputs. When a test case changes here, both suites
change with it.

Conventions used in the table:

- `diameter = 200` (px or pt); `radius = 100`.
- `ringRadius = 80` — a sample distance in the ring, between the center circle (36 px) and
  the edge (100 px). Used for all tap-zone and drag tests.
- `pointAt(degrees, r=80)` — a touch point in wheel coordinates whose angle is `degrees` and
  whose distance from the wheel center is `r`. In Cartesian: `(cx + r·cos(deg), cy + r·sin(deg))`.
- Drag tests move in 1° increments from `startAngle` by `totalDegrees`.
- All floating-point assertions use a tolerance of 0.001° unless otherwise noted.
- "No ticks" means every `move` call returns empty. `up` returns `Press` **only if** movement
  also stayed within the 8 dp/pt tap slop; if movement exceeded tap slop, `up` returns empty.

---

## Group 1 — `normalize`

| ID | Input | Expected output |
|---|---|---|
| N-1 | `normalize(190)` | `−170` |
| N-2 | `normalize(−190)` | `170` |
| N-3 | `normalize(180)` | `180` |
| N-4 | `normalize(−180)` | `180` |
| N-5 | `normalize(540)` | `180` |
| N-6 | `normalize(0)` | `0` |
| N-7 | `normalize(−360)` | `0` |

---

## Group 2 — Detent counting

| ID | Start angle | Drag (°) | Expected tick sum | Rationale |
|---|---|---|---|---|
| D-1 | −90° (top) | +50° CW | +3 | 50° / 15° = 3.33; floor = 3. Use 50°, not 45°, to avoid an exact multiple. |
| D-2 | −90° | −50° CCW | −3 | Symmetric. |
| D-3 | −90° | +20° CW | +1 | 20° / 15° = 1.33; floor = 1. |
| D-4 | −90° | −20° CCW | −1 | Symmetric. |

---

## Group 3 — Boundary crossing (no burst)

| ID | Start → End (angle wrap) | Drag (°) | Expected tick sum | Rationale |
|---|---|---|---|---|
| B-1 | 170° → 190° (crosses 0°/360°) | +20° | +1 | `normalize(20°)` = 20°; one tick, no burst. |
| B-2 | −170° → −190° | −20° | −1 | Symmetric. |

---

## Group 4 — Jitter absorption

| ID | Description | Expected behaviour |
|---|---|---|
| J-1 | Finger oscillates ±10° five times without crossing a 15° boundary (start at angle 0°, r=80) | Zero ticks emitted in any `move` call; `up` returns **empty** (movement of ≈14 px exceeds the 8 dp/pt tap slop, so `movedBeyondSlop = true`). |

---

## Group 5 — Dead zone

| ID | Description | Expected behaviour |
|---|---|---|
| DZ-1 | Drag starts on the ring; next `move` point is inside dead zone (< 20 % of radius from center) | `move` returns empty; `lastAngle` anchor dropped. |
| DZ-2 | After DZ-1: `move` to ring point at 180° (re-anchor) | `move` returns empty (re-anchor; no tick emitted). |
| DZ-3 | After DZ-2: `move` to ring point at 200° | `move` returns `[Tick(+1)]` (20° from anchor). |

---

## Group 6 — Touch that starts in center

| ID | Description | Expected behaviour |
|---|---|---|
| C-1 | `down` at the wheel's exact center (distance = 0) | `down` returns `true`; `startedInCenter` = true. |
| C-2 | After C-1: `move` to ring point at 0° | `move` returns empty. |
| C-3 | After C-2: `up` (finger moved beyond slop) | `up` returns empty (not a tap because slop exceeded). |
| C-4 | `down` at center, no `move`, `up` immediately | `up` returns `[Press(Center)]`. |

---

## Group 7 — Touch outside the wheel

| ID | Input | Expected output |
|---|---|---|
| O-1 | `down` at distance > radius from center | `down` returns `false`; tracker stays inactive. |

---

## Group 8 — Tap zone classification

All cases: `down` at the given point, `up` immediately (no move). Expected: `up` returns
`[Press(button)]`.

**Note**: Cases T-6, T-9, T-12, T-15 (exact boundary angles via `cos`/`sin`) are intentionally
omitted — floating-point trigonometry cannot guarantee an exact boundary value across JVM and
Swift runtimes (Constitution III: keep margin from exact thresholds). The ±0.5° cases below
(T-7/T-8, T-10/T-11, T-13/T-14, T-16/T-17) provide full boundary coverage with margin.

| ID | Touch point | Expected button |
|---|---|---|
| T-1 | Center of wheel (distance = 0) | `Center` |
| T-2 | `pointAt(−90°)` (top ring) | `Menu` |
| T-3 | `pointAt(0°)` (right ring) | `Next` |
| T-4 | `pointAt(90°)` (bottom ring) | `PlayPause` |
| T-5 | `pointAt(180°)` (left ring) | `Previous` |
| T-7 | `pointAt(−134.5°)` | `Menu` (0.5° inside Menu zone) |
| T-8 | `pointAt(−135.5°)` | `Previous` (0.5° past the Menu/Previous boundary) |
| T-10 | `pointAt(−44.5°)` | `Next` (0.5° inside Next zone) |
| T-11 | `pointAt(−45.5°)` | `Menu` (0.5° past the Next/Menu boundary) |
| T-13 | `pointAt(44.5°)` | `Next` (0.5° inside Next zone, bottom side) |
| T-14 | `pointAt(45.5°)` | `PlayPause` (0.5° past the Next/PlayPause boundary) |
| T-16 | `pointAt(134.5°)` | `PlayPause` (0.5° inside PlayPause zone) |
| T-17 | `pointAt(135.5°)` | `Previous` (0.5° past the PlayPause/Previous boundary) |

---

## Group 9 — Long press (Center only)

| ID | Description | Expected behaviour |
|---|---|---|
| LP-1 | `down` at center, no move; call `longPressTimeout` after 500 ms | `longPressTimeout` returns `[LongPress(Center)]`; subsequent `up` returns empty. |
| LP-2 | `down` at ring point (any button); no move; call `longPressTimeout` | `longPressTimeout` returns empty; `up` returns `[Press(button)]`. |
| LP-3 | `down` at center; drag beyond tap slop; call `longPressTimeout` | `longPressTimeout` returns empty (slop exceeded). |

---

## Group 10 — Ring button hold (not long press)

| ID | Description | Expected behaviour |
|---|---|---|
| RH-1 | `down` on MENU ring point, hold for any duration, call `longPressTimeout`, then `up` | `longPressTimeout` returns empty; `up` returns `[Press(Menu)]`. |
| RH-2 | Same for Next, PlayPause, Previous | Same result per button. |

---

## Notes for implementors

1. Keep a margin from exact threshold multiples. Test 50°, not 45°; test 20°, not exactly 15°.
2. `normalize` is a pure function; test it in isolation before any gesture test.
3. The dead-zone tests (Group 5) depend on precise point placement; use `hypot(dx, dy) < 0.20 × radius`
   to place the dead-zone sample.
4. Exact zone boundary angles (−135°, −45°, 45°, 135°) are tested only with ±0.5° margin
   (T-7/T-8, T-10/T-11, T-13/T-14, T-16/T-17) to avoid floating-point trig instability.
5. Tests MUST be written and confirmed failing before the implementation they cover is written
   (Constitution III).

**Total cases: 40** (N-1–N-7, D-1–D-4, B-1–B-2, J-1, DZ-1–DZ-3, C-1–C-4, O-1,
T-1–T-5 + T-7/T-8 + T-10/T-11 + T-13/T-14 + T-16/T-17, LP-1–LP-3, RH-1–RH-2)
