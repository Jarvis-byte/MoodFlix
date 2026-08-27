import Foundation
import Shared

/// Mirrors ui/detail/DetailViewModel.kt.
@MainActor
final class DetailViewModel: ObservableObject {
    @Published var isLoading = true
    @Published var movie: Movie?
    @Published var errorMessage: String?

    private let repository: MovieRepository
    private let tmdbId: Int32
    private let mediaType: MediaType

    init(container: IosAppContainer, tmdbId: Int32, mediaType: MediaType) {
        self.repository = container.movieRepository
        self.tmdbId = tmdbId
        self.mediaType = mediaType
        load()
    }

    func load() {
        isLoading = true
        errorMessage = nil
        Task {
            do {
                let result = try await repository.getMovieDetail(tmdbId: tmdbId, mediaType: mediaType)
                if let success = result as? AppResultSuccess<AnyObject> {
                    movie = success.data as? Movie
                    isLoading = false
                } else if let failure = result as? AppResultFailure {
                    errorMessage = failure.error.message
                    isLoading = false
                }
            } catch {
                errorMessage = error.localizedDescription
                isLoading = false
            }
        }
    }
}
