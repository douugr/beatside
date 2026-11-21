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
| Status | 📝 Specifying | 📝 Specifying |

## How this project is built

BeatSide uses **Spec-Driven Development** with [GitHub Spec Kit](https://github.com/github/spec-kit).
Every feature starts as a spec in `specs/`, then gets a plan, a task list and only then code,
for both platforms at once.

- [Constitution](.specify/memory/constitution.md): the project's non-negotiable principles
- [`specs/`](specs/): one directory per feature (spec, plan, tasks)
- [`CLAUDE.md`](CLAUDE.md): how agents work in this repository

An early prototype (never compiled) lives in the
[`prototype/wheel-and-menus`](https://github.com/douugr/beatside/tree/prototype/wheel-and-menus) branch as reference material.

## Repository layout

```
beatside/
├── android/    Android app (Gradle, Kotlin, Compose)
├── ios/        iOS app (XcodeGen, Swift, SwiftUI)
├── specs/      Feature specs, plans and tasks
└── .specify/   Spec Kit templates, scripts and constitution
```

## Trademarks

BeatSide is an independent project. It is not affiliated with, endorsed by or
sponsored by Apple, Spotify, Google or Deezer. "iPod" is a trademark of Apple Inc.

## License

[MIT](LICENSE) © 2026 Douglas Rodrigues Neto
