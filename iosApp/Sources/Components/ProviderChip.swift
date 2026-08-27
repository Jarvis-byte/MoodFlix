import SwiftUI

/// Mirrors ui/components/ProviderChip.kt.
struct ProviderChip: View {
    let name: String
    let logoUrl: String?
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                if let logoUrl, let url = URL(string: logoUrl) {
                    AsyncImage(url: url) { image in
                        image.resizable().aspectRatio(contentMode: .fill)
                    } placeholder: {
                        Color.moodOutline
                    }
                    .frame(width: 18, height: 18)
                    .clipShape(Circle())
                }
                Text(name)
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(selected ? Color.moodOnPrimary : Color.moodOnBackground)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(
                Capsule().fill(selected ? Color.moodPrimary : Color.moodSurfaceVariant)
            )
            .overlay(
                Capsule().stroke(Color.moodOutline, lineWidth: selected ? 0 : 1)
            )
        }
        .buttonStyle(.plain)
    }
}
