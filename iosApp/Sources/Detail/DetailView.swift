import SwiftUI
import Shared

/// Mirrors ui/detail/DetailScreen.kt.
struct DetailView: View {
    @StateObject private var viewModel: DetailViewModel
    @Environment(\.openURL) private var openURL
    @State private var playingTrailer: Trailer?
    @State private var showTrailerPlayer = false

    init(container: IosAppContainer, tmdbId: Int32, mediaType: MediaType) {
        _viewModel = StateObject(
            wrappedValue: DetailViewModel(container: container, tmdbId: tmdbId, mediaType: mediaType)
        )
    }

    var body: some View {
        Group {
            if viewModel.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let errorMessage = viewModel.errorMessage {
                VStack(spacing: 8) {
                    Text(errorMessage)
                        .foregroundStyle(.red)
                        .multilineTextAlignment(.center)
                    Button("Try again") { viewModel.load() }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .padding(32)
            } else if let movie = viewModel.movie {
                ScrollView {
                    content(for: movie)
                        .padding(.horizontal, 20)
                }
            }
        }
        .background(Color.moodBackground)
        .fullScreenCover(isPresented: $showTrailerPlayer) {
            if let trailer = playingTrailer {
                TrailerPlayerView(youtubeKey: trailer.youtubeKey, title: trailer.name)
            }
        }
        .navigationBarTitleDisplayMode(.inline)
    }

    @ViewBuilder
    private func content(for movie: Movie) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            backdrop(for: movie)
                .padding(.top, 18)

            Text(movie.title)
                .font(.largeTitle.bold())
                .foregroundStyle(Color.moodOnBackground)
                .padding(.top, 18)

            Text(metaLine(for: movie))
                .font(.subheadline)
                .foregroundStyle(Color.moodOnSurfaceVariant)
                .padding(.top, 6)

            Text(movie.overview)
                .font(.body)
                .foregroundStyle(Color.moodOnBackground)
                .padding(.top, 16)

            providerSection("Stream", providers: movie.watchProviders.filter { $0.type == .stream })
            providerSection("Rent", providers: movie.watchProviders.filter { $0.type == .rent })
            providerSection("Buy", providers: movie.watchProviders.filter { $0.type == .buy })

            if let link = movie.justWatchLink, let url = URL(string: link) {
                Button("See all watch options") { openURL(url) }
                    .buttonStyle(.borderedProminent)
                    .tint(Color.moodPrimary)
                    .padding(.top, 12)
            }

            Spacer(minLength: 40)
        }
    }

    private func backdrop(for movie: Movie) -> some View {
        let imageUrlString = movie.trailer?.thumbnailUrl ?? movie.backdropUrl
        return ZStack {
            AsyncImage(url: imageUrlString.flatMap { URL(string: $0) }) { image in
                image.resizable().aspectRatio(contentMode: .fill)
            } placeholder: {
                Color.moodSurfaceVariant
            }
            .aspectRatio(16.0 / 9.0, contentMode: .fill)
            .clipShape(RoundedRectangle(cornerRadius: 18))
            .clipped()

            if let trailer = movie.trailer {
                Button {
                    playingTrailer = trailer
                    showTrailerPlayer = true
                } label: {
                    Image(systemName: "play.fill")
                        .font(.title2)
                        .foregroundStyle(Color.moodOnPrimary)
                        .frame(width: 58, height: 58)
                        .background(Circle().fill(Color.moodPrimary))
                }
            }
        }
        .frame(maxWidth: .infinity)
        .aspectRatio(16.0 / 9.0, contentMode: .fit)
    }

    private func metaLine(for movie: Movie) -> String {
        var parts = [String(format: "%.1f", movie.rating), movie.year]

        if movie.mediaType == .series {
            if let seasons = movie.seasonCount {
                let count = seasons.intValue
                parts.append("\(count) season\(count == 1 ? "" : "s")")
            }
            if let episodes = movie.episodeCount {
                parts.append("\(episodes.intValue) episodes")
            }
            if let runtime = movie.runtimeMinutes {
                parts.append("~\(runtime.intValue) min/ep")
            }
        } else if let runtime = movie.runtimeMinutes {
            parts.append("\(runtime.intValue) min")
        }

        if !movie.genres.isEmpty {
            parts.append(movie.genres.joined(separator: ", "))
        }

        return parts.joined(separator: " \u{00B7} ")
    }

    @ViewBuilder
    private func providerSection(_ label: String, providers: [WatchProvider]) -> some View {
        if !providers.isEmpty {
            VStack(alignment: .leading, spacing: 8) {
                Text(label)
                    .font(.headline)
                    .foregroundStyle(Color.moodOnBackground)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(providers, id: \.providerId) { provider in
                            AsyncImage(url: provider.logoUrl.flatMap { URL(string: $0) }) { image in
                                image.resizable().aspectRatio(contentMode: .fill)
                            } placeholder: {
                                Color.moodSurfaceVariant
                            }
                            .frame(width: 46, height: 46)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                    }
                }
            }
            .padding(.top, 24)
        }
    }
}
