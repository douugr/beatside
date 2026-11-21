# Click wheel specification

The click wheel is the heart of BeatSide. Android and iOS implement it separately
(Compose and SwiftUI), so this document is the **single source of truth**: both
`WheelTracker` implementations must follow it, and both test suites check the same cases.

## Geometry

```
            MENU
        ╭─────────╮
       ╱  ╭─────╮  ╲
  |◀◀ │   │  ●  │   │ ▶▶|
       ╲  ╰─────╯  ╱
        ╰─────────╯
           ▶❙❙
```

| Name | Value | Notes |
|---|---|---|
| `R` | half of the wheel's width | outer radius |
| `centerRatio` | `0.36` | center button radius = `0.36 × R` |
| `deadZoneRatio` | `0.20` | drag samples closer than `0.20 × R` to the center are ignored, because the angle is unstable there. The first sample after leaving the dead zone re-anchors the angle without emitting ticks |

Angles are measured with `atan2(y − cy, x − cx)` in **screen coordinates (y grows down)**,
in degrees. In these coordinates a larger angle means **clockwise**, and `−90°` is the top.

## Rotation → ticks

The wheel turns continuous rotation into discrete **ticks** (detents):

1. When a touch starts in the ring (between the center button and `R`), store its angle.
2. On every move, compute `delta = normalize(newAngle − lastAngle)`, where
   `normalize` maps any angle to the interval `(−180°, 180°]`. This prevents the
   360° jump when crossing the 0°/360° boundary.
3. Add `delta` to an accumulator. While `accumulator ≥ step`, emit `Tick(+1)` and
   subtract `step`. While `accumulator ≤ −step`, emit `Tick(−1)` and add `step`.
4. Store `newAngle` as the last angle.

`Tick(+1)` = clockwise = **down** a list / volume up / scrub forward.

| Sensitivity | `step` | Detents per turn |
|---|---|---|
| Low | 20° | 18 |
| **Normal (default)** | 15° | 24 |
| High | 10° | 36 |

## Buttons

A gesture that ends **without emitting any tick** and **without moving more than
`tapSlop` (8 dp / 8 pt)** from where it started is a **tap**:

| Where the tap started | Button |
|---|---|
| Inside the center circle | `Center` (select) |
| Ring, angle in `[−135°, −45°)` (top) | `Menu` (go back) |
| Ring, angle in `[−45°, 45°)` (right) | `Next` |
| Ring, angle in `[45°, 135°)` (bottom) | `PlayPause` |
| Ring, everything else (left) | `Previous` |

Holding still for **500 ms** (no tick, within `tapSlop`) emits `LongPress(button)`
instead, and the following release emits nothing. Uses:

- `LongPress(Center)` → contextual action (adds a song to the On-The-Go playlist, as on the original).
- `LongPress(PlayPause)` → reserved (on the original iPod it put the device to sleep).
- `LongPress(Next/Previous)` → reserved for continuous seek.

A touch that starts outside the wheel's circle is ignored entirely. A touch that
starts inside the center circle never rotates the wheel: it can only become a tap or
a long press. A gesture that moves beyond `tapSlop` without emitting a tick emits
nothing.

## Feedback

- Every `Tick` plays the **click sound** and a **light selection haptic**, even when the
  list is already at its end. That way the wheel feels like a physical detent.
- Button presses play a **stronger haptic** and no click sound.
- Both can be turned off in Settings (`Clicker` and `Haptics`).

## Lists

- Selection **clamps** at the first and last item. It does not wrap around, which matches the original.
- The list scrolls only as much as needed to keep the selected row visible.
- `Menu` pops back to the previous menu. At the root it does nothing.

## Test cases (both platforms)

1. `normalize(190) == −170`, `normalize(−190) == 170`, `normalize(180) == 180`, `normalize(−180) == 180`.
2. A slow clockwise drag of 50° with `step = 15` emits exactly three `+1` ticks
   (50°, not 45°, so floating-point error at the exact multiple cannot flip the result).
3. Crossing the boundary (from 350° to 10°, i.e. +20°) emits one `+1` tick, not a burst.
4. Turning back and forth by 10° never emits a tick (the accumulator absorbs it).
5. Samples inside the dead zone are ignored. The first sample after leaving it
   re-anchors `lastAngle` without emitting ticks, so crossing the center never
   produces a burst.
6. Tap classification follows the button table, half a degree on each side of every zone edge.
