import Foundation
import Shared

/// A minimal Results screen - proves the AI -> TMDB recommendation pipeline
/// round-trips for real over the shared module. Not the polished card UI
/// from ui/results/ResultsScreen.kt; that's a follow-up.
@MainActor
final class ResultsViewModel: ObservableObject {
    enum Phase {
        case askingAi
        case aiResponded(count: Int, by: String)
        case enriched(movies: [Movie], by: String)
        /// Every connected AI provider failed - ask the user before falling back to TMDB.
        case awaitingTmdbFallback(reason: String)
        case fallback(movies: [Movie])
        case failed(message: String)
    }

    @Published var phase: Phase = .askingAi

    private let container: IosAppContainer
    private var pendingQuery: MoodQuery?

    init(container: IosAppContainer) {
        self.container = container
    }

    func run(query: MoodQuery) {
        phase = .askingAi
        pendingQuery = query
        Task {
            do {
                let flow = container.getRecommendationsUseCase.invoke(query: query)
                try await FlowBridgeKt.collectForSwift(flow) { [weak self] value in
                    self?.apply(value)
                }
            } catch {
                self.phase = .failed(message: error.localizedDescription)
            }
        }
    }

    /// User agreed, from the fallback prompt, to switch to plain TMDB picks.
    func confirmTmdbFallback() {
        guard let query = pendingQuery else { return }
        Task {
            do {
                let movies = try await container.getRecommendationsUseCase.fallbackToTmdb(query: query)
                self.phase = .fallback(movies: movies)
            } catch {
                self.phase = .failed(message: error.localizedDescription)
            }
        }
    }

    private func apply(_ value: Any?) {
        if value is RecommendationStateAskingAi {
            phase = .askingAi
        } else if let s = value as? RecommendationStateAiResponded {
            phase = .aiResponded(count: Int(s.titleCount), by: s.answeredBy)
        } else if let s = value as? RecommendationStateEnriched {
            phase = .enriched(movies: s.movies, by: s.answeredBy)
        } else if let s = value as? RecommendationStateAiFailed {
            phase = .awaitingTmdbFallback(reason: s.reason.message)
        } else if let s = value as? RecommendationStateFailed {
            phase = .failed(message: s.error.message)
        }
    }
}
