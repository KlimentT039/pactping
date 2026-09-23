import SwiftUI
import Combine
import PactPingShared

@main
struct iOSApp: App {
    // App-scoped container — owns the SQLite driver, repos, and stores.
    @StateObject private var app = AppEnvironment()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(app)
                .preferredColorScheme(nil) // follow system light/dark
        }
    }
}

/// Lightweight wrapper around the Kotlin `IosAppContainer` so SwiftUI views
/// can pull stores out of the environment.
@MainActor
final class AppEnvironment: ObservableObject {
    let container: IosAppContainer

    init() {
        self.container = IosAppContainer()
    }
}
