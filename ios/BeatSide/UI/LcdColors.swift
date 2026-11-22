import SwiftUI

/// Retro LCD palette shared by the screen views. Mirrors Android `LcdColors.kt`.
enum LcdColors {
    static let background = Color(red: 0.949, green: 0.957, blue: 0.941)
    static let text = Color(red: 0.106, green: 0.106, blue: 0.106)
    static let border = Color(red: 0.333, green: 0.349, blue: 0.361)
    static let titleBar = LinearGradient(
        colors: [Color(white: 0.992), Color(red: 0.851, green: 0.863, blue: 0.875)],
        startPoint: .top, endPoint: .bottom)
    static let selection = LinearGradient(
        colors: [Color(red: 0.427, green: 0.690, blue: 0.949), Color(red: 0.169, green: 0.451, blue: 0.788)],
        startPoint: .top, endPoint: .bottom)
    static let selectedText = Color.white
}
