# Architecture

BeatSide is a monorepo with two **fully native** apps. There is no shared code
between them. What they share is **behavior**, described in the specs under `docs/`.

```
beatside/
├── android/   Kotlin + Jetpack Compose (Gradle)
├── ios/       Swift + SwiftUI (XcodeGen → Xcode project)
└── docs/      Shared specs: wheel, menus, features, roadmap
```

## Platform differences that shape the design

| Capability | Android | iOS |
|---|---|---|
| Replace the home screen (launcher) | ✅ `HOME` intent filter | ❌ not allowed |
| Read and control **any** app's playback (Spotify, YouTube Music, Deezer…) | ✅ `MediaSessionManager` + notification listener | ❌ apps cannot see each other's playback |
| Local music files | ✅ MediaStore + Media3/ExoPlayer | ✅ only songs in the user's library or imported files |
| Apple Music | ❌ | ✅ MusicKit |
| Spotify library and playback | ✅ Web API + App Remote SDK | ✅ Web API + iOS SDK |
| Deezer | Library via REST API; playback hands off to the Deezer app | Same |
| YouTube Music | No official API: controlled through the universal Now Playing only | Not supported |

In practice, the Android app is a **launcher with universal media control**, and the
iOS app is a **music player** with a click-wheel interface.

## Layers (both platforms)

```
┌───────────────────────────────────────────────┐
│ UI: LCD screen (menus, Now Playing, Extras)   │
├───────────────────────────────────────────────┤
│ Wheel: WheelTracker → WheelEvent              │  docs/WHEEL.md
├───────────────────────────────────────────────┤
│ Menu engine: MenuNode tree + MenuNavigator    │
├───────────────────────────────────────────────┤
│ Playback                                      │
│  ├─ NowPlayingSource (universal, Android)     │
│  └─ MusicSource: Local · Spotify · Deezer ·   │
│                  Apple Music (iOS)            │
└───────────────────────────────────────────────┘
```

### Wheel

`WheelTracker` is pure logic, with no UI framework imports, and it is unit-tested.
It turns touch samples into `WheelEvent`s:

- `Tick(+1 / −1)`
- `Press(button)` and `LongPress(button)`, where `button` is one of `Center`, `Menu`,
  `Next`, `Previous`, `PlayPause`

The `ClickWheel` view only draws the wheel, forwards touches to the tracker, and plays
feedback. Screens never handle raw gestures, only `WheelEvent`s.

### Menu engine

The original iPod is essentially a tree of lists, so the menu engine is declarative:

- `MenuNode` is either a `Submenu` (title + children), a `Screen` (a custom view, such
  as Now Playing), or an `Action` (a toggle in Settings, for example).
- `MenuNavigator` holds a stack of `(node, selectedIndex)`. It reacts to `WheelEvent`s:
  ticks move the selection (clamped), `Center` enters the selected item, `Menu` pops
  the stack.

Adding a feature means adding nodes to the tree, not copying scroll logic.

### Playback sources

- **`NowPlayingSource` (Android only):** lists the active media sessions with
  `MediaSessionManager.getActiveSessions` once the user enables BeatSide as a
  notification listener. This gives metadata, artwork and transport controls for any
  player app, with no API keys.
- **`MusicSource`:** one implementation per service. Each one browses a library
  (playlists, artists, albums, songs) and can start playback.
  - `LocalSource`: MediaStore (Android) / media library (iOS). Plays in-app.
  - `SpotifySource`: Web API for the library, App Remote / iOS SDK to play.
  - `DeezerSource`: REST API for the library. Opens the Deezer app to play, and
    the universal Now Playing takes over (Android).
  - `AppleMusicSource` (iOS): MusicKit.

### Known external constraints (checked November 2026)

- **Spotify:** apps in Development Mode require Spotify Premium and are limited to
  5 users per app. Extended quota is reserved for large, established apps. So, for
  the public Play Store release, Spotify library browsing ships disabled, behind a
  Settings toggle. The universal Now Playing still controls Spotify for everyone on
  Android.
- **Deezer:** the native mobile SDKs are deprecated; only the REST API remains.
- **YouTube Music:** no official API. Unofficial clients break often and violate the
  terms of service, so BeatSide only controls it through the universal Now Playing.

## Naming and trademarks

"iPod" and "Click Wheel" are Apple trademarks. BeatSide uses neither in its name, in
the store listing or in the UI copy; the store listing describes the app as a "retro
wheel player". The wheel artwork is original, not a copy of Apple's.
