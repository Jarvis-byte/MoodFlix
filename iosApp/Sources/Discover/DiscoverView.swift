import SwiftUI
import Shared

/// Mirrors ui/discover/DiscoverScreen.kt.
struct DiscoverView: View {
    @StateObject private var viewModel: DiscoverViewModel
    @State private var query: MoodQuery?
    @State private var showResults = false
    @State private var showSettings = false

    private let container: IosAppContainer

    init(container: IosAppContainer) {
        self.container = container
        _viewModel = StateObject(wrappedValue: DiscoverViewModel(container: container))
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 28) {
                    header

                    section("Looking for") {
                        HStack(spacing: 8) {
                            ForEach(MediaTypeFilter.entries, id: \.self) { filter in
                                MoodChip(
                                    label: filter.label,
                                    selected: viewModel.mediaFilter == filter,
                                    action: { viewModel.mediaFilter = filter }
                                )
                                .frame(maxWidth: .infinity)
                            }
                        }
                    }

                    section("Mood") {
                        moodGrid
                    }

                    section("Genre") {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                ForEach(Genre.entries, id: \.self) { genre in
                                    MoodChip(
                                        label: genre.label,
                                        selected: viewModel.selectedGenre == genre,
                                        action: { viewModel.selectedGenre = genre }
                                    )
                                }
                            }
                        }
                    }

                    if !viewModel.availableProviders.isEmpty {
                        section("Where do you watch?") {
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 8) {
                                    MoodChip(
                                        label: "All platforms",
                                        selected: viewModel.selectedProviderIds.isEmpty,
                                        action: { viewModel.clearProviders() }
                                    )
                                    ForEach(viewModel.availableProviders, id: \.id) { provider in
                                        ProviderChip(
                                            name: provider.name,
                                            logoUrl: provider.logoUrl,
                                            selected: viewModel.selectedProviderIds.contains(provider.id),
                                            action: { viewModel.toggleProvider(provider.id) }
                                        )
                                    }
                                }
                            }
                            if !viewModel.selectedProviderIds.isEmpty {
                                Text("Only showing titles on \(viewModel.selectedProviderIds.count) selected platform(s)")
                                    .font(.caption)
                                    .foregroundStyle(Color.moodOnSurfaceVariant)
                            }
                        }
                    }

                    RatingSlider(value: $viewModel.minRating)

                    TextField(
                        "Anything else? (optional)",
                        text: $viewModel.freeText,
                        prompt: Text("no subtitles, under 2 hours, nothing depressing")
                    )
                    .textFieldStyle(.plain)
                    .padding(14)
                    .background(RoundedRectangle(cornerRadius: 16).fill(Color.moodSurfaceVariant))
                    .lineLimit(3)

                    actionButtons

                    if !viewModel.hasAnyProvider {
                        connectProviderHint
                    }
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 8)
            }
            .background(Color.moodBackground)
            .navigationDestination(isPresented: $showResults) {
                if let query {
                    ResultsView(container: container, query: query)
                }
            }
            .sheet(isPresented: $showSettings) {
                SettingsView(container: container)
            }
            .toolbar {
                ToolbarItem(placement: .principal) {
                    Text("MoodFlix")
                        .font(.title2.bold())
                        .foregroundStyle(Color.moodPrimary)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button(action: { showSettings = true }) {
                        Image(systemName: "gearshape")
                            .foregroundStyle(Color.moodOnBackground)
                    }
                }
            }
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Aaj kya dekhna hai?")
                .font(.largeTitle.bold())
                .foregroundStyle(Color.moodOnBackground)
            Text("Pick a mood. The rest is on us.")
                .foregroundStyle(Color.moodOnSurfaceVariant)
        }
    }

    private var moodGrid: some View {
        let rows = stride(from: 0, to: Mood.entries.count, by: 2).map { i in
            Array(Mood.entries[i..<min(i + 2, Mood.entries.count)])
        }
        return VStack(spacing: 8) {
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                HStack(spacing: 8) {
                    ForEach(row, id: \.self) { mood in
                        MoodChip(
                            label: mood.label,
                            selected: viewModel.selectedMood == mood,
                            action: { viewModel.selectedMood = mood }
                        )
                        .frame(maxWidth: .infinity)
                    }
                    if row.count == 1 { Spacer() }
                }
            }
        }
    }

    private var actionButtons: some View {
        HStack(spacing: 10) {
            Button {
                query = viewModel.buildQuery()
                showResults = query != nil
            } label: {
                Text("Find me something")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .frame(height: 54)
                    .foregroundStyle(Color.moodOnPrimary)
                    .background(RoundedRectangle(cornerRadius: 16).fill(Color.moodPrimary))
            }
            .disabled(!viewModel.canSearch)
            .opacity(viewModel.canSearch ? 1 : 0.5)

            Button {
                viewModel.surpriseMood()
                query = viewModel.buildQuery()
                showResults = query != nil
            } label: {
                Image(systemName: "dice")
                    .frame(width: 54, height: 54)
                    .foregroundStyle(Color.moodOnBackground)
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.moodOutline, lineWidth: 1))
            }
        }
    }

    private var connectProviderHint: some View {
        VStack(spacing: 4) {
            Text("No AI provider connected yet")
                .font(.headline)
                .foregroundStyle(Color.moodOnBackground)
            Text("You can still search - MoodFlix will show popular picks from TMDB instead of AI-curated ones.")
                .font(.subheadline)
                .foregroundStyle(Color.moodOnSurfaceVariant)
                .multilineTextAlignment(.center)
            Button("Connect a provider") { showSettings = true }
                .padding(.top, 6)
        }
        .frame(maxWidth: .infinity)
        .multilineTextAlignment(.center)
        .padding(.vertical, 12)
    }

    @ViewBuilder
    private func section<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title.uppercased())
                .font(.subheadline.weight(.bold))
                .foregroundStyle(Color.moodPrimary)
            content()
        }
    }
}
