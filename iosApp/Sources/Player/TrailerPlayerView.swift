import SwiftUI
import SafariServices

/// Plays the YouTube trailer in-app as a modal sheet.
///
/// This used to be a custom WKWebView embed of YouTube's iframe player, but
/// WKWebView-hosted video reliably renders as a solid black rectangle in the
/// iOS Simulator (a known WebKit/Simulator compositing limitation - it does
/// work on a real device, but that's cold comfort while developing against
/// the Simulator). SFSafariViewController uses the real Safari rendering
/// process instead of an embedded WKWebView, which doesn't have that bug, so
/// playback is reliable in both the Simulator and on-device at the cost of
/// showing Safari's own chrome (address bar) rather than fully custom UI.
struct TrailerPlayerView: View {
    let youtubeKey: String
    let title: String

    var body: some View {
        if let url = URL(string: "https://www.youtube.com/watch?v=\(youtubeKey)") {
            SafariView(url: url)
                .ignoresSafeArea()
        }
    }
}

private struct SafariView: UIViewControllerRepresentable {
    let url: URL

    func makeUIViewController(context: Context) -> SFSafariViewController {
        let config = SFSafariViewController.Configuration()
        config.entersReaderIfAvailable = false
        return SFSafariViewController(url: url, configuration: config)
    }

    func updateUIViewController(_ controller: SFSafariViewController, context: Context) {}
}
