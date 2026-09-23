import SwiftUI
import Combine
import PactPingShared

@MainActor
final class CommitmentDetailViewModel: ObservableObject {
    @Published var state: DetailUiState

    let store: CommitmentDetailStore
    private let watcher: FlowWatcher<DetailUiState>
    private var subscription: FlowSubscription?

    init(container: IosAppContainer, id: String) {
        self.store = container.detailStore(id: id)
        self.state = DetailUiState(
            commitment: nil,
            record: PactPingShared.Record(kept: 0, broken: 0),
            countdown: CountdownParts(days: 0, hours: 0, minutes: 0, seconds: 0),
            isLoading: true
        )
        self.watcher = IosFlowWatchers.shared.detail(store: store)
        self.subscription = watcher.watch { [weak self] newState in
            Task { @MainActor in
                self?.state = newState
            }
        }
    }

    func markKept(onDone: @escaping () -> Void) {
        IosActionBridge.shared.markKept(store: store) {
            Task { @MainActor in onDone() }
        }
    }

    deinit {
        subscription?.cancel()
        watcher.close()
    }
}

struct DetailView: View {
    @StateObject private var vm: CommitmentDetailViewModel
    let onBack: () -> Void
    let onMarkedKept: () -> Void

    init(container: IosAppContainer, id: String, onBack: @escaping () -> Void, onMarkedKept: @escaping () -> Void) {
        _vm = StateObject(wrappedValue: CommitmentDetailViewModel(container: container, id: id))
        self.onBack = onBack
        self.onMarkedKept = onMarkedKept
    }

    var body: some View {
        VStack(spacing: 0) {
            TopBar(onBack: onBack, showPill: vm.state.commitment != nil)
                .padding(.top, 12)

            if let commitment = vm.state.commitment {
                ScrollView {
                    Content(commitment: commitment, countdown: vm.state.countdown)
                        .padding(.top, 8)
                }
                Spacer(minLength: 12)
                MarkDoneButton {
                    vm.markKept(onDone: onMarkedKept)
                }
                .padding(.top, 12)
            } else if vm.state.isLoading {
                Spacer()
            } else {
                NotFound()
            }
        }
        .padding(.horizontal, 22)
        .padding(.bottom, 16)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(PactPingTheme.bg.ignoresSafeArea())
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
    }
}

// MARK: - Top bar

private struct TopBar: View {
    let onBack: () -> Void
    let showPill: Bool

    var body: some View {
        HStack {
            Button(action: onBack) {
                Circle()
                    .fill(PactPingTheme.surface)
                    .frame(width: 36, height: 36)
                    .overlay(
                        Text("←")
                            .font(.witnessExtraBold(16))
                            .foregroundColor(PactPingTheme.ink)
                    )
            }
            .buttonStyle(.plain)
            Spacer()
            if showPill {
                Text("ON THE LINE")
                    .font(.witnessExtraBold(10))
                    .tracking(1.2)
                    .foregroundColor(PactPingTheme.ink)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(Capsule().fill(PactPingTheme.surfaceAlt))
            }
            Spacer()
            Color.clear.frame(width: 36, height: 36)
        }
    }
}

// MARK: - Content

private struct Content: View {
    let commitment: Commitment
    let countdown: CountdownParts

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            VStack(alignment: .leading, spacing: 6) {
                Text("COUNTDOWN")
                    .font(.witnessExtraBold(11))
                    .tracking(2.2)
                    .foregroundColor(PactPingTheme.strike)
                HeroCountdown(parts: countdown)
            }
            VStack(alignment: .leading, spacing: 4) {
                Text(commitment.goal)
                    .font(.witnessExtraBold(22))
                    .tracking(-0.3)
                    .foregroundColor(PactPingTheme.ink)
                Text("due \(DeadlineFormat.string(from: commitment.deadline))")
                    .font(.witnessMedium(12))
                    .foregroundColor(PactPingTheme.muted)
            }
            WitnessCard(name: commitment.witness.name, initials: commitment.witness.initials)
            ConfessionPreview(
                firstName: commitment.witness.name.split(separator: " ").first.map(String.init) ?? "they",
                confession: commitment.confession
            )
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct HeroCountdown: View {
    let parts: CountdownParts

    var body: some View {
        let d = Int(truncatingIfNeeded: parts.days)
        let h = Int(truncatingIfNeeded: parts.hours)
        let m = Int(truncatingIfNeeded: parts.minutes)
        let s = Int(truncatingIfNeeded: parts.seconds)

        VStack(alignment: .leading, spacing: 2) {
            HStack(alignment: .bottom, spacing: 0) {
                if d > 0 {
                    HeroNum("\(d)"); HeroUnit("d")
                    Spacer().frame(width: 16)
                    HeroNum("\(h)"); HeroUnit("h")
                } else if h > 0 {
                    HeroNum("\(h)"); HeroUnit("h")
                    Spacer().frame(width: 16)
                    HeroNum(String(format: "%02d", m)); HeroUnit("m")
                } else {
                    HeroNum("\(m)"); HeroUnit("m")
                }
            }
            Text(subText(d: d, h: h, m: m, s: s))
                .font(.witnessBlack(30))
                .tracking(-1)
                .foregroundColor(PactPingTheme.muted)
        }
    }

    private func subText(d: Int, h: Int, m: Int, s: Int) -> String {
        if d > 0 || h > 0 {
            return "\(String(format: "%02d", m))m  \(String(format: "%02d", s))s"
        }
        return "\(String(format: "%02d", s))s"
    }
}

private struct HeroNum: View {
    let text: String
    init(_ text: String) { self.text = text }
    var body: some View {
        Text(text)
            .font(.witnessBlack(72))
            .tracking(-3)
            .foregroundColor(PactPingTheme.ink)
    }
}

private struct HeroUnit: View {
    let text: String
    init(_ text: String) { self.text = text }
    var body: some View {
        Text(text)
            .font(.witnessBold(28))
            .foregroundColor(PactPingTheme.muted)
            .padding(.leading, 4)
            .padding(.bottom, 12)
    }
}

private struct WitnessCard: View {
    let name: String
    let initials: String

    var body: some View {
        HStack {
            HStack(spacing: 12) {
                Circle()
                    .fill(avatarColor(initials))
                    .frame(width: 36, height: 36)
                    .overlay(
                        Text(initials)
                            .font(.witnessExtraBold(13))
                            .foregroundColor(.white)
                    )
                VStack(alignment: .leading, spacing: 0) {
                    Text(name)
                        .font(.witnessBold(15))
                        .foregroundColor(PactPingTheme.ink)
                    Text("witness · notified")
                        .font(.witnessMedium(12))
                        .foregroundColor(PactPingTheme.muted)
                }
            }
            Spacer()
            Text("EYES ON")
                .font(.witnessExtraBold(11))
                .tracking(1.6)
                .foregroundColor(PactPingTheme.strike)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(
            RoundedRectangle(cornerRadius: 22)
                .fill(PactPingTheme.surface)
                .overlay(
                    RoundedRectangle(cornerRadius: 22)
                        .stroke(PactPingTheme.hairline, lineWidth: 1)
                )
        )
    }
}

private struct ConfessionPreview: View {
    let firstName: String
    let confession: String

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("IF YOU MISS, \(firstName.uppercased()) GETS")
                .font(.witnessExtraBold(11))
                .tracking(2)
                .foregroundColor(PactPingTheme.muted)
            Text(confession)
                .font(.witnessMedium(14))
                .foregroundColor(PactPingTheme.bg)
                .padding(.horizontal, 18)
                .padding(.vertical, 16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(
                    UnevenRoundedRectangle(
                        cornerRadii: .init(topLeading: 22, bottomLeading: 6, bottomTrailing: 22, topTrailing: 22),
                        style: .continuous
                    )
                    .fill(PactPingTheme.ink)
                )
        }
    }
}

private struct MarkDoneButton: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text("✓  Mark this done")
                .font(.witnessExtraBold(16))
                .foregroundColor(PactPingTheme.onStrike)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 20)
                .background(RoundedRectangle(cornerRadius: 20).fill(PactPingTheme.strike))
        }
        .buttonStyle(.plain)
    }
}

private struct NotFound: View {
    var body: some View {
        VStack(spacing: 6) {
            Spacer()
            Text("This one's gone.")
                .font(.witnessBlack(28))
                .foregroundColor(PactPingTheme.ink)
            Text("It was either deleted or never existed.")
                .font(.witnessMedium(14))
                .foregroundColor(PactPingTheme.muted)
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Shared helpers

func witnessAvatarColor(_ seed: String) -> Color {
    let palette: [Color] = [
        Color(red: 0.247, green: 0.424, blue: 1.0),
        Color(red: 0.878, green: 0.271, blue: 0.482),
        Color(red: 0.133, green: 0.627, blue: 0.420),
        Color(red: 0.702, green: 0.435, blue: 0.0),
        Color(red: 0.431, green: 0.337, blue: 0.812),
    ]
    let hash = abs(seed.hashValue)
    return palette[hash % palette.count]
}

private func avatarColor(_ seed: String) -> Color { witnessAvatarColor(seed) }
