import AVFoundation
import UIKit

/// The click, played through `AVAudioPlayer` in an `.ambient` audio session: it mixes with music
/// in other apps (never pausing or ducking it) and is muted by the Ring/Silent switch (FR-018).
/// A small pool of players lets fast detents overlap instead of cutting each other off.
@MainActor
final class AudioClickSound: ClickSound {
    private var players: [AVAudioPlayer] = []
    private var next = 0

    init(poolSize: Int = 4) {
        try? AVAudioSession.sharedInstance().setCategory(.ambient)
        guard let url = Bundle.main.url(forResource: "click", withExtension: "wav") else { return }
        players = (0..<poolSize).compactMap { _ in try? AVAudioPlayer(contentsOf: url) }
        players.forEach { $0.prepareToPlay() }
    }

    func play() {
        guard !players.isEmpty else { return }
        let player = players[next]
        next = (next + 1) % players.count
        player.currentTime = 0
        player.play()
    }
}

/// Light selection haptic per tick, medium impact per press. Generators are prepared ahead of use
/// and re-prepared after each fire, so ticks during a rotation fire in the same frame.
@MainActor
final class GeneratorHaptics: HapticOutput {
    private let selection = UISelectionFeedbackGenerator()
    private let impact = UIImpactFeedbackGenerator(style: .medium)

    func prepare() {
        selection.prepare()
        impact.prepare()
    }

    func tick() {
        selection.selectionChanged()
        selection.prepare()
    }

    func press() {
        impact.impactOccurred()
        impact.prepare()
    }
}
