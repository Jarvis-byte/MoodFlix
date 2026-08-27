import SwiftUI

/// Mirrors app/src/main/java/com/arka/moodflix/ui/theme/Color.kt and
/// Theme.kt - the same cinema-dark palette, adapted to SwiftUI's
/// light/dark color(light:dark:) pattern instead of Compose's two explicit
/// ColorSchemes.
extension Color {
    static let ink900 = Color(red: 0x0B / 255, green: 0x0B / 255, blue: 0x10 / 255)
    static let ink800 = Color(red: 0x13 / 255, green: 0x13 / 255, blue: 0x1B / 255)
    static let ink700 = Color(red: 0x1C / 255, green: 0x1C / 255, blue: 0x27 / 255)
    static let ink600 = Color(red: 0x27 / 255, green: 0x27 / 255, blue: 0x33 / 255)

    static let amber400 = Color(red: 0xF5 / 255, green: 0xB9 / 255, blue: 0x42 / 255)
    static let amber700 = Color(red: 0x8A / 255, green: 0x5A / 255, blue: 0x00 / 255)

    static let violet400 = Color(red: 0x9B / 255, green: 0x8C / 255, blue: 0xFF / 255)
    static let violet700 = Color(red: 0x4B / 255, green: 0x3F / 255, blue: 0xA8 / 255)

    static let cream100 = Color(red: 0xF4 / 255, green: 0xF1 / 255, blue: 0xEA / 255)
    static let cream200 = Color(red: 0xDE / 255, green: 0xDA / 255, blue: 0xD0 / 255)
    static let mutedText = Color(red: 0x9A / 255, green: 0x97 / 255, blue: 0xA6 / 255)

    // Semantic roles - each one swaps automatically with the system
    // light/dark appearance, same intent as Theme.kt's DarkColors/LightColors.
    static var moodBackground: Color { Color(light: .cream100, dark: .ink900) }
    static var moodSurface: Color { Color(light: .white, dark: .ink800) }
    static var moodSurfaceVariant: Color { Color(light: .cream200, dark: .ink700) }
    static var moodPrimary: Color { Color(light: .amber700, dark: .amber400) }
    static var moodOnPrimary: Color { Color(light: .cream100, dark: .ink900) }
    static var moodOnBackground: Color { Color(light: .ink900, dark: .cream100) }
    static var moodOnSurfaceVariant: Color { Color(light: .ink700, dark: .mutedText) }
    static var moodOutline: Color { Color(light: .cream200, dark: .ink600) }

    private init(light: Color, dark: Color) {
        self = Color(uiColor: UIColor { traits in
            traits.userInterfaceStyle == .dark ? UIColor(dark) : UIColor(light)
        })
    }
}
