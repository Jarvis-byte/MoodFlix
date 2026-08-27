import Foundation
import Shared

/// Mirrors ui/discover/DiscoverViewModel.kt.
@MainActor
final class DiscoverViewModel: ObservableObject {
    @Published var selectedMood: Mood?
    @Published var selectedGenre: Genre = Genre.entries.first!
    @Published var minRating: Float = 7.0
    @Published var freeText: String = ""
    @Published var availableProviders: [OttProvider] = []
    @Published var selectedProviderIds: Set<Int32> = []
    @Published var mediaFilter: MediaTypeFilter = .both
    @Published var hasAnyProvider = false

    var canSearch: Bool { selectedMood != nil }

    private let container: IosAppContainer

    init(container: IosAppContainer) {
        self.container = container
        Task { await loadOttProviders() }
        Task { await observeConnectedProviders() }
    }

    private func loadOttProviders() async {
        do {
            let region = try await FlowBridgeKt.firstForSwift(container.userPrefs.watchCountry) as? String ?? "US"
            let result = try await container.getOttProvidersUseCase.invoke(region: region)
            if let success = result as? AppResultSuccess<AnyObject> {
                availableProviders = success.data as? [OttProvider] ?? []
            }
        } catch {
            // Non-fatal - Discover still works with an empty provider list.
        }
    }

    private func observeConnectedProviders() async {
        do {
            let flow = container.observeConnectedProvidersUseCase.invoke()
            try await FlowBridgeKt.collectForSwift(flow) { [weak self] value in
                let providers = value as? [ConnectedProvider] ?? []
                self?.hasAnyProvider = providers.contains { $0.hasKey }
            }
        } catch {
            // Stream ended (e.g. view torn down) - nothing to do.
        }
    }

    func toggleProvider(_ id: Int32) {
        if selectedProviderIds.contains(id) {
            selectedProviderIds.remove(id)
        } else {
            selectedProviderIds.insert(id)
        }
    }

    func clearProviders() {
        selectedProviderIds.removeAll()
    }

    @discardableResult
    func surpriseMood() -> Mood {
        let mood = Mood.entries.randomElement()!
        selectedMood = mood
        return mood
    }

    func buildQuery() -> MoodQuery? {
        guard let mood = selectedMood else { return nil }
        return MoodQuery(
            mood: mood,
            genre: selectedGenre,
            minRating: minRating,
            freeText: freeText,
            excludeTitles: [],
            selectedProviderIds: selectedProviderIds.map { KotlinInt(int: $0) },
            mediaFilter: mediaFilter,
            page: 1
        )
    }
}
