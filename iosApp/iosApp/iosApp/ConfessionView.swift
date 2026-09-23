import SwiftUI
import Combine
import PactPingShared

@MainActor
final class ConfessionViewModel: ObservableObject {
    /// Optional because the Kotlin `ConfessionUiState` initial value uses
    /// `kotlin.time.Duration`, which is awkward to construct from Swift.
    /// Initial `nil` is the "loading" state; the watcher fills it.
    @Published var state: ConfessionUiState?

    let store: ConfessionStore
    private let watcher: FlowWatcher<ConfessionUiState>
    private var subscription: FlowSubscription?

    init(container: IosAppContainer, id: String) {
        self.store = container.confessionStore(id: id)
        self.state = nil
        self.watcher = IosFlowWatchers.shared.confession(store: store)
        self.subscription = watcher.watch { [weak self] newState in
            Task { @MainActor in
                self?.state = newState
            }
        }
    }

    var commitment: Commitment? { state?.commitment }
    var isReady: Bool { state?.isReady ?? false }

    func confirmMiss(onDone: @escaping () -> Void) {
        IosActionBridge.shared.confirmMiss(store: store) {
            Task { @MainActor in onDone() }
        }
    }

    deinit {
        subscription?.cancel()
        watcher.close()
    }
}

struct ConfessionView: View {
    @StateObject private var vm: ConfessionViewModel
    let onLater: () -> Void
    let onSent: () -> Void
    @State private var pendingSms: OutboundSms?
    @State private var smsCopied: SmsCopied?

    init(container: IosAppContainer, id: String, onLater: @escaping () -> Void, onSent: @escaping () -> Void) {
        _vm = StateObject(wrappedValue: ConfessionViewModel(container: container, id: id))
        self.onLater = onLater
        self.onSent = onSent
    }

    var body: some View {
        let commitment = vm.commitment

        VStack(spacing: 0) {
            TopBar(onLater: onLater)
                .padding(.top, 12)
            Spacer().frame(height: 20)
            Group {
                if let c = commitment {
                    ScrollView {
                        Content(commitment: c, elapsed: elapsedSince(deadline: c.deadline))
                            .padding(.top, 0)
                    }
                } else {
                    Spacer()
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            Spacer().frame(height: 12)

            SendButton(enabled: vm.isReady) {
                guard let c = commitment else { return }
                vm.confirmMiss {
                    let firstName = c.witness.name.split(separator: " ").first.map(String.init) ?? c.witness.name
                    switch MessageSend.attempt(recipient: c.witness.phoneNumber, name: firstName, body: c.confession) {
                    case .compose(let sms):
                        pendingSms = sms
                    case .copied(let alert):
                        smsCopied = alert
                    }
                }
            }
            Spacer().frame(height: 4)
            Button(action: onLater) {
                Text("I'll deal with this later")
                    .font(.witnessBold(14))
                    .foregroundColor(PactPingTheme.muted)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 22)
        .padding(.bottom, 16)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(PactPingTheme.bg.ignoresSafeArea())
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
        .sheet(item: $pendingSms, onDismiss: { onSent() }) { sms in
            MessageComposer(recipient: sms.recipient, body: sms.body) { _ in
                pendingSms = nil
            }
            .ignoresSafeArea()
        }
        .smsCopiedAlert($smsCopied, onAcknowledge: onSent)
    }

    private func elapsedSince(deadline: Kotlinx_datetimeInstant) -> TimeInterval {
        let elapsed = Date().timeIntervalSince(deadline.asDate)
        return max(0, elapsed)
    }
}

// MARK: - Pieces

private struct TopBar: View {
    let onLater: () -> Void

    var body: some View {
        HStack {
            Button(action: onLater) {
                Circle()
                    .fill(PactPingTheme.surface)
                    .frame(width: 36, height: 36)
                    .overlay(
                        Text("×")
                            .font(.witnessExtraBold(18))
                            .foregroundColor(PactPingTheme.ink)
                    )
            }
            .buttonStyle(.plain)
            Spacer()
            Text("DEADLINE BLOWN")
                .font(.witnessExtraBold(10))
                .tracking(1.2)
                .foregroundColor(PactPingTheme.miss)
                .padding(.horizontal, 10)
                .padding(.vertical, 5)
                .background(Capsule().fill(PactPingTheme.missSoft))
            Spacer()
            Color.clear.frame(width: 36, height: 36)
        }
    }
}

private struct Content: View {
    let commitment: Commitment
    let elapsed: TimeInterval

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            VStack(alignment: .leading, spacing: 8) {
                Text("YOU MISSED.")
                    .font(.witnessBlack(11))
                    .tracking(2.4)
                    .foregroundColor(PactPingTheme.miss)
                Text("Time\nto own it.")
                    .font(.witnessBlack(40))
                    .tracking(-1.2)
                    .foregroundColor(PactPingTheme.ink)
                Text("\(DeadlineFormat.string(from: commitment.deadline)) came and went \(elapsedLabel(elapsed)) ago.")
                    .font(.witnessMedium(14))
                    .foregroundColor(PactPingTheme.muted)
            }

            VStack(alignment: .leading, spacing: 10) {
                HStack(spacing: 10) {
                    Circle()
                        .fill(witnessAvatarColor(commitment.witness.initials))
                        .frame(width: 28, height: 28)
                        .overlay(
                            Text(commitment.witness.initials)
                                .font(.witnessExtraBold(11))
                                .foregroundColor(.white)
                        )
                    Text("To \(commitment.witness.name) · SMS")
                        .font(.witnessBold(12))
                        .foregroundColor(PactPingTheme.ink)
                }
                Text(commitment.confession)
                    .font(.witnessMedium(14))
                    .foregroundColor(.white)
                    .padding(.horizontal, 18)
                    .padding(.vertical, 16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(
                        UnevenRoundedRectangle(
                            cornerRadii: .init(topLeading: 22, bottomLeading: 22, bottomTrailing: 6, topTrailing: 22),
                            style: .continuous
                        )
                        .fill(PactPingTheme.miss)
                    )
                Text("Goes from your phone, as you. We don't edit a word.")
                    .font(.witnessMedium(12))
                    .foregroundColor(PactPingTheme.muted)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func elapsedLabel(_ d: TimeInterval) -> String {
        let minutes = max(Int(d / 60), 1)
        let hours = Int(d / 3600)
        let days = Int(d / 86_400)
        if days >= 1 { return "\(days) days" }
        if hours >= 1 { return "\(hours) hours" }
        return "\(minutes) minutes"
    }
}

private struct SendButton: View {
    let enabled: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text("Send the truth")
                .font(.witnessBlack(17))
                .tracking(0.4)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 24)
                .background(
                    RoundedRectangle(cornerRadius: 22)
                        .fill(enabled ? PactPingTheme.miss : PactPingTheme.hairline)
                )
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}
