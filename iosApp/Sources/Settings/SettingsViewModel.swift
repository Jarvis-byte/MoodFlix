import Foundation
import Shared

/// Mirrors ui/settings/SettingsViewModel.kt.
@MainActor
final class SettingsViewModel: ObservableObject {
    @Published var editingProvider: AiProviderType?
    @Published var draftKey = ""
    @Published var message: String?
    @Published var providers: [ConnectedProvider] = []

    private let keyRepository: AiKeyRepository

    init(container: IosAppContainer) {
        self.keyRepository = container.aiKeyRepository
        Task { await observeProviders() }
    }

    private func observeProviders() async {
        do {
            let flow = keyRepository.connectedProviders
            try await FlowBridgeKt.collectForSwift(flow) { [weak self] value in
                self?.providers = value as? [ConnectedProvider] ?? []
            }
        } catch {
            // Stream ended (e.g. view torn down) - nothing to do.
        }
    }

    func startEditing(_ type: AiProviderType) {
        editingProvider = type
        draftKey = ""
    }

    func cancelEditing() {
        editingProvider = nil
        draftKey = ""
    }

    func saveKey() {
        guard let type = editingProvider else { return }
        let key = draftKey.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !key.isEmpty else { return }

        Task {
            do {
                try await keyRepository.saveKey(type: type, key: key)
                editingProvider = nil
                draftKey = ""
                message = "\(type.displayName) connected"
            } catch {
                message = "Couldn't save that key: \(error.localizedDescription)"
            }
        }
    }

    func removeKey(_ type: AiProviderType) {
        Task {
            do {
                try await keyRepository.removeKey(type: type)
                message = "\(type.displayName) removed"
            } catch {
                message = "Couldn't remove that key: \(error.localizedDescription)"
            }
        }
    }

    func moveUp(_ type: AiProviderType) {
        Task {
            var current = providers.map { $0.type }
            guard let index = current.firstIndex(where: { $0 == type }), index > 0 else { return }
            current.swapAt(index, index - 1)
            do {
                try await keyRepository.setFallbackOrder(order: current)
            } catch {
                // Order is cosmetic - not worth surfacing a message for this.
            }
        }
    }

    func dismissMessage() {
        message = nil
    }
}
