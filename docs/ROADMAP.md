# Roadmap

Each phase ships on **both** platforms unless marked otherwise.

## Phase 0: Foundation ✅ (this commit)

- [x] Monorepo with `.gitignore`, license, specs
- [x] `WheelTracker` (pure logic) with unit tests, following `docs/WHEEL.md`
- [x] `ClickWheel` view: rotation, buttons, click sound, haptics
- [x] Menu engine (`MenuNode` + `MenuNavigator`) with unit tests
- [x] LCD screen: title bar, selectable list, placeholder screens
- [ ] First run on a real device and build check (Android Studio / Xcode)

## Phase 1: Universal Now Playing (Android)

- [ ] Notification listener service + onboarding screen asking for access
- [ ] `NowPlayingSource` on top of `MediaSessionManager`
- [ ] Now Playing screen: artwork, title, artist, album, progress bar, track `n of m`
- [ ] Wheel modes in Now Playing: volume → scrubbing → rating (center click cycles them)
- [ ] Transport: play/pause, next, previous from the wheel buttons
- [ ] Launcher mode: `HOME` intent filter and an app list under Extras

## Phase 2: Local library

- [ ] `LocalSource`: Media3/ExoPlayer (Android), media library or imported files (iOS)
- [ ] Music menu: Playlists, Artists, Albums, Songs, Genres, Composers
- [ ] Shuffle Songs, Repeat, playback queue
- [ ] On-The-Go playlist (long press on center)
- [ ] Cover Flow

## Phase 3: Streaming services

- [ ] Spotify: login, playlists, saved albums and songs; play via App Remote / iOS SDK
- [ ] Deezer: login, playlists, favorites; hand off playback to the Deezer app
- [ ] Apple Music via MusicKit (iOS)
- [ ] Source switcher in the Music menu

## Phase 4: Extras and settings

- [ ] Clock, Alarms, Stopwatch, Notes
- [ ] Games: Brick, Music Quiz
- [ ] Settings: Shuffle, Repeat, EQ, Sound Check, Clicker, Haptics, wheel sensitivity, Backlight timer
- [ ] Themes: classic white, black, color variants
- [ ] Hold switch (locks the wheel)

## Phase 5: Release

- [ ] App icon and visual identity
- [ ] Play Store listing (name: "BeatSide: Retro Wheel Player")
- [ ] Privacy policy (notification access is a sensitive permission and must be justified)
- [ ] App Store release (iOS)
