import SwiftUI

/// Mirrors ui/components/RatingSlider.kt.
struct RatingSlider: View {
    @Binding var value: Float

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Minimum rating")
                    .font(.headline)
                    .foregroundStyle(Color.moodOnBackground)
                Spacer()
                Text(String(format: "%.1f+", value))
                    .font(.headline)
                    .foregroundStyle(Color.moodPrimary)
            }
            Slider(
                value: Binding(
                    get: { Double(value) },
                    set: { value = (Float($0) * 10).rounded() / 10 }
                ),
                in: 5...9,
                step: 0.5
            )
            .tint(Color.moodPrimary)
        }
    }
}
