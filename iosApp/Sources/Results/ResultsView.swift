import SwiftUI
import Shared

struct ResultsView: View {
    @StateObject private var viewModel: ResultsViewModel
    private let container: IosAppContainer
    private let query: MoodQuery

    init(container: IosAppContainer, query: MoodQuery) {
        self.container = container
        _viewModel = StateObject(wrappedValue: ResultsViewModel(container: container))
        self.query = query
    }

    var body: some View {
        Group {
            switch viewModel.phase {
            case .askingAi:
                statusView("Asking the AI for picks...")
            case .aiResponded(let count, let by):
                statusView("\(by) suggested \(count) titles - looking them up on TMDB...")
            case .enriched(let movies, let by):
                movieList(movies, caption: "Curated by \(by)")
            case .awaitingTmdbFallback:
                statusView("AI picks unavailable")
            case .fallback(let movies):
                movieList(movies, caption: "AI unavailable - showing popular TMDB picks")
            case .failed(let message):
                statusView("Couldn't find anything: \(message)")
            }
        }
        .background(Color.moodBackground)
        .navigationTitle("Results")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { viewModel.run(query: query) }
        .alert(
            "AI picks unavailable",
            isPresented: Binding(
                get: {
                    if case .awaitingTmdbFallback = viewModel.phase { return true }
                    return false
                },
                set: { _ in }
            )
        ) {
            Button("Use TMDB picks") { viewModel.confirmTmdbFallback() }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("We couldn't reach any connected AI provider. Would you like popular, well-rated picks from TMDB instead?")
        }
    }

    private func statusView(_ message: String) -> some View {
        VStack(spacing: 12) {
            ProgressView()
            Text(message)
                .foregroundStyle(Color.moodOnSurfaceVariant)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func movieList(_ movies: [Movie], caption: String) -> some View {
        List {
            Section {
                ForEach(movies, id: \.tmdbId) { movie in
                    NavigationLink {
                        DetailView(container: container, tmdbId: movie.tmdbId, mediaType: movie.mediaType)
                    } label: {
                        MovieRow(movie: movie)
                    }
                }
            } header: {
                Text(caption)
            }
        }
        .listStyle(.plain)
    }
}

private struct MovieRow: View {
    let movie: Movie

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            AsyncImage(url: movie.posterUrl.flatMap { URL(string: $0) }) { image in
                image.resizable().aspectRatio(contentMode: .fill)
            } placeholder: {
                Color.moodSurfaceVariant
            }
            .frame(width: 56, height: 84)
            .clipShape(RoundedRectangle(cornerRadius: 8))

            VStack(alignment: .leading, spacing: 4) {
                Text("\(movie.title) (\(movie.year))")
                    .font(.headline)
                    .foregroundStyle(Color.moodOnBackground)
                Text(String(format: "\u{2605} %.1f", movie.rating))
                    .font(.caption)
                    .foregroundStyle(Color.moodPrimary)
                if !movie.moodReason.isEmpty {
                    Text(movie.moodReason)
                        .font(.caption)
                        .foregroundStyle(Color.moodOnSurfaceVariant)
                        .lineLimit(2)
                }
            }
        }
        .padding(.vertical, 4)
    }
}
