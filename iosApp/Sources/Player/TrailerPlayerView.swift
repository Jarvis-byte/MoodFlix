import SwiftUI
import WebKit

/// Embeds YouTube's iframe player - no embedded-video screen exists on
/// Android yet (it just opens the YouTube link externally there), so this is
/// iOS-only for now. A WKWebView loading the standard embed URL is the
/// lightweight, ToS-compliant way to get real in-app playback without
/// pulling in the full YouTube iOS Player SDK.
struct TrailerPlayerView: View {
    @Environment(\.dismiss) private var dismiss

    let youtubeKey: String
    let title: String

    var body: some View {
        NavigationStack {
            YouTubeEmbedWebView(youtubeKey: youtubeKey)
                .background(Color.black)
                .ignoresSafeArea(edges: .bottom)
                .navigationTitle(title)
                .navigationBarTitleDisplayMode(.inline)
                .toolbarBackground(Color.black, for: .navigationBar)
                .toolbarColorScheme(.dark, for: .navigationBar)
                .toolbar {
                    ToolbarItem(placement: .topBarLeading) {
                        Button(action: { dismiss() }) {
                            Image(systemName: "xmark")
                                .foregroundStyle(.white)
                        }
                    }
                }
        }
        .preferredColorScheme(.dark)
    }
}

private struct YouTubeEmbedWebView: UIViewRepresentable {
    let youtubeKey: String

    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        // Lets the video play inline in the web view instead of forcing
        // fullscreen-only playback, and allows autoplay without a user
        // gesture inside the embed (autoplay is still muted-by-default per
        // WebKit policy unless this is set).
        config.allowsInlineMediaPlayback = true
        config.mediaTypesRequiringUserActionForPlayback = []

        let webView = WKWebView(frame: .zero, configuration: config)
        webView.scrollView.isScrollEnabled = false
        webView.backgroundColor = .black
        webView.isOpaque = false
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        guard let url = URL(
            string: "https://www.youtube.com/embed/\(youtubeKey)?playsinline=1&autoplay=1&rel=0"
        ) else { return }
        if webView.url != url {
            webView.load(URLRequest(url: url))
        }
    }
}
