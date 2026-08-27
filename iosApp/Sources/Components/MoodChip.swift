import SwiftUI

/// Mirrors ui/components/MoodChip.kt - a pill toggle used for mood, genre,
/// and media-type filter selection.
struct MoodChip: View {
    let label: String
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(selected ? Color.moodOnPrimary : Color.moodOnBackground)
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(
                    Capsule().fill(selected ? Color.moodPrimary : Color.moodSurfaceVariant)
                )
                .overlay(
                    Capsule().stroke(Color.moodOutline, lineWidth: selected ? 0 : 1)
                )
                .scaleEffect(selected ? 1.04 : 1.0)
        }
        .buttonStyle(.plain)
        .animation(.easeOut(duration: 0.15), value: selected)
    }
}
