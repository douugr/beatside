import SwiftUI

/// App entry point. Portrait-only is locked in project.yml (UISupportedInterfaceOrientations).
@main
struct BeatSideApp: App {
    var body: some Scene {
        WindowGroup {
            BeatSideView()
        }
    }
}
