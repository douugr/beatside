# Prototype branch

This branch holds the first prototype of BeatSide, written **before** the project
adopted Spec-Driven Development (GitHub Spec Kit). It is kept as a reference only:
`main` re-implements every feature from its spec under `specs/`.

What is here:

- `docs/`: early drafts of the architecture, the click wheel spec and the roadmap.
  These are inputs for `/speckit.specify`.
- `android/`: Gradle project with the `WheelTracker`, `ClickWheel`, the menu engine
  (`MenuNode` + `MenuNavigator`), the LCD screen and unit tests. **Never compiled:**
  it was written in an environment without the Android SDK.
- `ios/`: `project.yml` (XcodeGen) and the Swift `WheelTracker` only. Incomplete.
