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
        case fallback(movies: [Movie], reason: String)
        case failed(message: String)
    }

    @Published var phase: Phase = .askingAi

    private let container: IosAppContainer

    init(container: IosAppContainer) {
        self.container = container
    }

    func run(query: MoodQuery) {
        phase = .askingAi
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

    private func apply(_ value: Any?) {
        if value is RecommendationStateAskingAi {
            phase = .askingAi
        } else if let s = value as? RecommendationStateAiResponded {
            phase = .aiResponded(count: Int(s.titleCount), by: s.answeredBy)
        } else if let s = value as? RecommendationStateEnriched {
            phase = .enriched(movies: s.movies, by: s.answeredBy)
        } else if let s = value as? RecommendationStateFallbackToTmdb {
            phase = .fallback(movies: s.movies, reason: s.reason.message)
        } else if let s = value as? RecommendationStateFailed {
            phase = .failed(message: s.error.message)
        }
    }
}
