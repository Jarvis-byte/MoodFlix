import SwiftUI
import Shared

/// Mirrors ui/settings/SettingsScreen.kt.
struct SettingsView: View {
    @StateObject private var viewModel: SettingsViewModel
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL

    init(container: IosAppContainer) {
        _viewModel = StateObject(wrappedValue: SettingsViewModel(container: container))
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Your keys stay on this phone. They are encrypted in the Keychain and never sent to any MoodFlix server, because there isn't one.")
                        Text("Connect more than one and MoodFlix falls back automatically when a free quota runs out.")
                    }
                    .font(.subheadline)
                    .foregroundStyle(Color.moodOnSurfaceVariant)
                    .padding(.bottom, 8)

                    ForEach(viewModel.providers, id: \.type) { provider in
                        ProviderCard(
                            provider: provider,
                            isEditing: viewModel.editingProvider == provider.type,
                            draftKey: $viewModel.draftKey,
                            isFirst: provider.order == 0,
                            onStartEditing: { viewModel.startEditing(provider.type) },
                            onSave: { viewModel.saveKey() },
                            onCancel: { viewModel.cancelEditing() },
                            onRemove: { viewModel.removeKey(provider.type) },
                            onMoveUp: { viewModel.moveUp(provider.type) },
                            onGetKey: { openURL(URL(string: provider.type.keyConsoleUrl)!) }
                        )
                    }
                }
                .padding(20)
            }
            .background(Color.moodBackground)
            .navigationTitle("AI providers")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(action: { dismiss() }) {
                        Image(systemName: "chevron.left")
                            .foregroundStyle(Color.moodOnBackground)
                    }
                }
            }
            .alert(
                viewModel.message ?? "",
                isPresented: Binding(
                    get: { viewModel.message != nil },
                    set: { if !$0 { viewModel.dismissMessage() } }
                )
            ) {
                Button("OK") { viewModel.dismissMessage() }
            }
        }
    }
}

private struct ProviderCard: View {
    let provider: ConnectedProvider
    let isEditing: Bool
    @Binding var draftKey: String
    let isFirst: Bool
    let onStartEditing: () -> Void
    let onSave: () -> Void
    let onCancel: () -> Void
    let onRemove: () -> Void
    let onMoveUp: () -> Void
    let onGetKey: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .center) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(provider.type.displayName)
                        .font(.headline)
                        .foregroundStyle(Color.moodOnBackground)
                    Text(statusText)
                        .font(.caption)
                        .foregroundStyle(provider.hasKey ? Color.moodPrimary : Color.moodOnSurfaceVariant)
                }
                Spacer()
                if provider.hasKey {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(Color.moodPrimary)
                    if !isFirst {
                        Button(action: onMoveUp) {
                            Image(systemName: "chevron.up")
                        }
                        .padding(.leading, 4)
                    }
                }
            }

            if isEditing {
                SecureField(provider.type.keyPrefixHint, text: $draftKey)
                    .textFieldStyle(.plain)
                    .padding(12)
                    .background(RoundedRectangle(cornerRadius: 14).fill(Color.moodSurfaceVariant))
                    .padding(.top, 12)

                HStack(spacing: 8) {
                    Button("Save", action: onSave)
                        .buttonStyle(.borderedProminent)
                        .tint(Color.moodPrimary)
                        .disabled(draftKey.trimmingCharacters(in: .whitespaces).isEmpty)
                    Button("Cancel", action: onCancel)
                    Spacer()
                    Button(action: onGetKey) {
                        HStack(spacing: 4) {
                            Text("Get a key")
                            Image(systemName: "arrow.up.right.square")
                        }
                    }
                }
                .font(.subheadline)
                .padding(.top, 10)
            } else {
                HStack(spacing: 16) {
                    Button(provider.hasKey ? "Replace key" : "Connect", action: onStartEditing)
                    if provider.hasKey {
                        Button("Remove", role: .destructive, action: onRemove)
                    } else {
                        Button("Get a free key", action: onGetKey)
                    }
                }
                .font(.subheadline)
                .padding(.top, 8)
            }
        }
        .padding(16)
        .background(RoundedRectangle(cornerRadius: 18).fill(Color.moodSurface))
    }

    private var statusText: String {
        guard provider.hasKey else { return "Not connected" }
        return isFirst ? "Connected · tried first" : "Connected · fallback \(provider.order + 1)"
    }
}
