# BeatSide

**A retro wheel music player for Android and iOS.** Spin a wheel, feel every detent,
and browse your music the way it worked in the 2000s, now with streaming services
on top.

> *Beat* + *side*: the B-side of how you listen to music, always beside you.

| | Android | iOS |
|---|---|---|
| UI | Jetpack Compose | SwiftUI |
| Language | Kotlin | Swift |
| Role | Launcher with universal media control | Music player |
| Status | 🚧 Foundation | 🚧 Foundation |

## Features (planned)

- A wheel you can actually feel: a click sound and a haptic detent on every step
- Classic menu tree: Music, Now Playing, Extras, Settings
- **Android:** control whatever is playing (Spotify, YouTube Music, Deezer, any player)
  from one Now Playing screen, and use BeatSide as your home screen
- Local library with playlists, shuffle, On-The-Go playlist and Cover Flow
- Spotify and Deezer libraries; Apple Music on iOS
- Extras: clock, stopwatch, notes, and a couple of small games

See [the roadmap](docs/ROADMAP.md) for what's done and what's next.

## Repository layout

```
beatside/
├── android/   Android app (Gradle, Kotlin, Compose)
├── ios/       iOS app (XcodeGen, Swift, SwiftUI)
└── docs/      Shared specs
```

The two apps share **no code**, only behavior. The specs in `docs/` are the source of
truth for both:

- [Architecture](docs/ARCHITECTURE.md): layers, platform differences and service constraints
- [Click wheel spec](docs/WHEEL.md): geometry, rotation math, buttons and feedback
- [Roadmap](docs/ROADMAP.md)

## Getting started

### Android

Requirements: Android Studio (latest stable), JDK 17+.

```bash
cd android
./gradlew :app:installDebug   # or open the android/ folder in Android Studio
./gradlew :app:testDebugUnitTest
```

### iOS

Requirements: Xcode 16+ and [XcodeGen](https://github.com/yonaskolb/XcodeGen)
(`brew install xcodegen`). The Xcode project is generated, not committed.

```bash
cd ios
xcodegen generate
open BeatSide.xcodeproj
```

Set your team under *Signing & Capabilities* before running on a device.

## Trademarks

BeatSide is an independent project. It is not affiliated with, endorsed by or
sponsored by Apple, Spotify, Google or Deezer. "iPod" is a trademark of Apple Inc.

## License

[MIT](LICENSE) © 2026 Douglas Rodrigues Neto
