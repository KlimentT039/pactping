import SwiftUI
import Combine
import PactPingShared

/// Top-level routes pushed onto the home navigation stack. The Create flow
/// is presented as a sheet from `HomeView` itself; only pushed screens live
/// here.
enum Route: Hashable {
    case detail(id: String)
    case confession(id: String)
    case success(id: String)
    case record
}

@MainActor
final class NavRouter: ObservableObject {
    @Published var path: [Route] = []

    func showDetail(id: String) { path.append(.detail(id: id)) }
    func showConfession(id: String) { path.append(.confession(id: id)) }
    func showRecord() { path.append(.record) }

    func popToRoot() { path.removeAll() }

    /// Pop everything else and push Success. Mirrors Android's
    /// `popBackStack(Home, inclusive=false); navigate(success/id)`.
    func swapToSuccess(id: String) {
        path = [.success(id: id)]
    }
}

struct ContentView: View {
    @EnvironmentObject private var app: AppEnvironment
    @StateObject private var router = NavRouter()

    var body: some View {
        NavigationStack(path: $router.path) {
            HomeView(
                container: app.container,
                onCommitmentTap: { id, isOverdue in
                    if isOverdue {
                        router.showConfession(id: id)
                    } else {
                        router.showDetail(id: id)
                    }
                },
                onRecordTap: { router.showRecord() }
            )
            .navigationDestination(for: Route.self) { route in
                switch route {
                case .detail(let id):
                    DetailView(
                        container: app.container,
                        id: id,
                        onBack: { router.popToRoot() },
                        onMarkedKept: { router.swapToSuccess(id: id) }
                    )
                case .confession(let id):
                    ConfessionView(
                        container: app.container,
                        id: id,
                        onLater: { router.popToRoot() },
                        onSent: { router.popToRoot() }
                    )
                case .success(let id):
                    SuccessView(
                        container: app.container,
                        id: id,
                        onClose: { router.popToRoot() }
                    )
                case .record:
                    RecordView(
                        container: app.container,
                        onBack: { router.popToRoot() }
                    )
                }
            }
        }
    }
}
