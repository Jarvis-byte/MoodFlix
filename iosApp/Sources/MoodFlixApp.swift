import SwiftUI
import Shared

@main
struct MoodFlixApp: App {
    // One container for the app's lifetime - it owns the HttpClient and the
    // Keychain-backed key store. See shared/.../di/IosAppContainer.kt.
    let container = IosAppContainer(
        tmdbApiKey: Bundle.main.object(forInfoDictionaryKey: "TMDB_API_KEY") as? String ?? ""
    )

    var body: some Scene {
        WindowGroup {
            DiscoverView(container: container)
        }
    }
}
