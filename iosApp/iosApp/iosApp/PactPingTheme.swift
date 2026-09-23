import SwiftUI

/// Mirror of the Compose `PactPingSemantic` tokens. Adapts automatically to
/// light/dark via `Color(uiColor:)` on iOS 15+; we use literal hex pairs
/// here so the SwiftUI shell renders the same palette as Android.
enum PactPingTheme {

    // Neutrals
    static let ink = adaptive(light: 0x0B0B0F, dark: 0xF5F1EA)
    static let inkSoft = adaptive(light: 0x2B2B33, dark: 0xC8C6C0)
    static let muted = adaptive(light: 0x7A7A82, dark: 0x7B7B82)
    static let bg = adaptive(light: 0xEFE9DE, dark: 0x0B0B0F)
    static let surface = adaptive(light: 0xF7F2E8, dark: 0x15151B)
    static let surfaceAlt = adaptive(light: 0xFFFFFF, dark: 0x1D1D24)
    static let hairline = adaptive(light: 0x0B0B0F, dark: 0xF5F1EA, alphaLight: 0.08, alphaDark: 0.08)

    // Semantic
    static let strike = adaptive(light: 0xFF4D1F, dark: 0xFF6B3D)
    static let onStrike = adaptive(light: 0xFFFFFF, dark: 0x0B0B0F)
    static let kept = adaptive(light: 0x1E8C4A, dark: 0x25C26A)
    static let miss = adaptive(light: 0xD11A2A, dark: 0xF2515E)
    static let keptSoft = adaptive(light: 0xD9F1E2, dark: 0x25C26A, alphaLight: 1.0, alphaDark: 0.14)
    static let missSoft = adaptive(light: 0xFBE0E1, dark: 0xF2515E, alphaLight: 1.0, alphaDark: 0.14)

    private static func adaptive(
        light: Int,
        dark: Int,
        alphaLight: Double = 1.0,
        alphaDark: Double = 1.0
    ) -> Color {
        Color(uiColor: UIColor { trait in
            let hex = trait.userInterfaceStyle == .dark ? dark : light
            let alpha = trait.userInterfaceStyle == .dark ? alphaDark : alphaLight
            return UIColor(
                red: CGFloat((hex >> 16) & 0xFF) / 255.0,
                green: CGFloat((hex >> 8) & 0xFF) / 255.0,
                blue: CGFloat(hex & 0xFF) / 255.0,
                alpha: CGFloat(alpha)
            )
        })
    }
}

extension Font {
    static func witnessBlack(_ size: CGFloat) -> Font {
        .system(size: size, weight: .black, design: .default)
    }
    static func witnessExtraBold(_ size: CGFloat) -> Font {
        .system(size: size, weight: .heavy, design: .default)
    }
    static func witnessBold(_ size: CGFloat) -> Font {
        .system(size: size, weight: .bold, design: .default)
    }
    static func witnessMedium(_ size: CGFloat) -> Font {
        .system(size: size, weight: .medium, design: .default)
    }
}
