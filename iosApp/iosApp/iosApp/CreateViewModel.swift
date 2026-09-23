import Foundation
import Combine
import PactPingShared

/// Observes the shared `CreateCommitmentStore.state` flow and republishes
/// it as a SwiftUI-friendly `@Published`. Forwards every setter and
/// wraps `commit()` via `IosCreateBridge`.
@MainActor
final class CreateViewModel: ObservableObject {
    @Published private(set) var state: CreateState

    private let store: CreateCommitmentStore
    private let watcher: FlowWatcher<CreateState>
    private var subscription: FlowSubscription?

    init(container: IosAppContainer) {
        self.store = container.createStore
        self.state = CreateState(
            goal: "",
            deadline: nil,
            witness: nil,
            confession: ""
        )
        self.watcher = IosFlowWatchers.shared.create(store: container.createStore)
        self.subscription = watcher.watch { [weak self] newState in
            Task { @MainActor in
                self?.state = newState
            }
        }
    }

    func setGoal(_ value: String) { store.setGoal(value: value) }
    func setDeadline(_ date: Date) { store.setDeadline(value: date.kotlinInstant) }
    func setWitness(name: String, phoneNumber: String) {
        let witness = Witness(
            id: "pc-\(phoneNumber.hashValue)",
            name: name,
            phoneNumber: phoneNumber
        )
        store.setWitness(value: witness)
    }
    func setConfession(_ value: String) { store.setConfession(value: value) }

    func commit(onResult: @escaping (Bool) -> Void) {
        IosCreateBridge.shared.commit(store: store) { result in
            Task { @MainActor in
                onResult(result.boolValue)
            }
        }
    }

    deinit {
        subscription?.cancel()
        watcher.close()
    }
}
