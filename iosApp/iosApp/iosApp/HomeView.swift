import SwiftUI
import Combine
import PactPingShared

/// Observes the shared Kotlin `HomeStore` via `FlowWatcher` and republishes
/// its state as a SwiftUI-friendly `@Published`.
@MainActor
final class HomeViewModel: ObservableObject {
    @Published var state: HomeUiState

    private let homeWatcher: FlowWatcher<HomeUiState>
    private var homeSubscription: FlowSubscription?

    init(container: IosAppContainer) {
        self.state = HomeUiState(
            record: PactPingShared.Record(kept: 0, broken: 0),
            active: [],
            isLoading: true
        )
        self.homeWatcher = IosFlowWatchers.shared.home(store: container.homeStore)
        self.homeSubscription = homeWatcher.watch { [weak self] newState in
            Task { @MainActor in
                self?.state = newState
            }
        }
    }

    deinit {
        homeSubscription?.cancel()
        homeWatcher.close()
    }
}

struct HomeView: View {
    @StateObject private var viewModel: HomeViewModel
    @State private var showCreate = false
    private let container: IosAppContainer
    private let onCommitmentTap: (String, Bool) -> Void
    private let onRecordTap: () -> Void

    init(
        container: IosAppContainer,
        onCommitmentTap: @escaping (String, Bool) -> Void = { _, _ in },
        onRecordTap: @escaping () -> Void = {}
    ) {
        self.container = container
        self.onCommitmentTap = onCommitmentTap
        self.onRecordTap = onRecordTap
        _viewModel = StateObject(wrappedValue: HomeViewModel(container: container))
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            PactPingTheme.bg.ignoresSafeArea()

            if viewModel.state.isEmpty {
                EmptyHome()
            } else {
                ActiveHome(
                    state: viewModel.state,
                    onCommitmentTap: onCommitmentTap,
                    onRecordTap: onRecordTap
                )
            }

            MakePromiseButton(action: { showCreate = true })
                .padding(.horizontal, 22)
                .padding(.bottom, 16)
        }
        .fullScreenCover(isPresented: $showCreate) {
            CreateView(container: container)
        }
    }
}

// MARK: - Sections

private struct ActiveHome: View {
    let state: HomeUiState
    let onCommitmentTap: (String, Bool) -> Void
    let onRecordTap: () -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 18) {
                TopBar()
                Button(action: onRecordTap) {
                    RecordCard(record: state.record)
                }
                .buttonStyle(.plain)

                HStack {
                    Text("ACTIVE · \(state.active.count)")
                        .font(.witnessExtraBold(11))
                        .tracking(2.2)
                        .foregroundColor(PactPingTheme.muted)
                    Spacer()
                    Text("Soonest first")
                        .font(.witnessBold(12))
                        .foregroundColor(PactPingTheme.muted)
                }

                ForEach(state.active, id: \.id) { item in
                    Button(action: { onCommitmentTap(item.id, item.isOverdue) }) {
                        CommitmentCard(item: item)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 22)
            .padding(.top, 12)
            .padding(.bottom, 120)
        }
    }
}

private struct EmptyHome: View {
    var body: some View {
        VStack(spacing: 0) {
            TopBar()
                .padding(.horizontal, 22)
                .padding(.top, 12)
            Spacer(minLength: 0)
            VStack(spacing: 28) {
                Circle()
                    .fill(PactPingTheme.surface)
                    .overlay(
                        Circle().stroke(PactPingTheme.hairline, lineWidth: 1)
                    )
                    .frame(width: 120, height: 120)
                    .overlay(
                        EyeGlyph()
                            .stroke(PactPingTheme.ink, lineWidth: 3.5)
                            .frame(width: 64, height: 64)
                    )
                VStack(spacing: 10) {
                    Text("Nothing\non the line.")
                        .font(.witnessBlack(32))
                        .tracking(-1)
                        .lineSpacing(2)
                        .foregroundColor(PactPingTheme.ink)
                        .multilineTextAlignment(.center)
                    Text("Pick something you keep dodging. Name someone who'd notice. We'll handle the rest.")
                        .font(.witnessMedium(14))
                        .foregroundColor(PactPingTheme.muted)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: 260)
                }
            }
            Spacer(minLength: 0)
            // Reserve space so the FAB doesn't overlap the description.
            Color.clear.frame(height: 96)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct TopBar: View {
    var body: some View {
        HStack {
            HStack(spacing: 0) {
                Text("WITNESS")
                    .font(.witnessBlack(16))
                    .tracking(5)
                    .foregroundColor(PactPingTheme.ink)
                Text(".")
                    .font(.witnessBlack(16))
                    .foregroundColor(PactPingTheme.strike)
            }
            Spacer()
            Circle()
                .fill(PactPingTheme.surface)
                .frame(width: 36, height: 36)
                .overlay(
                    Text("⋯").font(.witnessExtraBold(18)).foregroundColor(PactPingTheme.ink)
                )
        }
    }
}

private struct RecordCard: View {
    let record: PactPingShared.Record

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .bottom) {
                VStack(alignment: .leading, spacing: 0) {
                    Text("KEPT")
                        .font(.witnessExtraBold(11))
                        .tracking(2.2)
                        .foregroundColor(PactPingTheme.muted)
                    Text("\(record.kept)")
                        .font(.witnessBlack(28))
                        .tracking(-1)
                        .foregroundColor(PactPingTheme.kept)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 0) {
                    Text("BROKEN")
                        .font(.witnessExtraBold(11))
                        .tracking(2.2)
                        .foregroundColor(PactPingTheme.muted)
                    Text("\(record.broken)")
                        .font(.witnessBlack(28))
                        .tracking(-1)
                        .foregroundColor(PactPingTheme.miss)
                }
            }
            KeptRatioBar(ratio: Double(record.keptRatio))
            Text(record.total == 0
                ? "No record yet"
                : "\(Int(record.keptRatio * 100))% of promises kept")
                .font(.witnessMedium(12))
                .foregroundColor(PactPingTheme.muted)
        }
        .padding(EdgeInsets(top: 16, leading: 18, bottom: 16, trailing: 18))
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

private struct KeptRatioBar: View {
    let ratio: Double

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                RoundedRectangle(cornerRadius: 4)
                    .fill(PactPingTheme.miss)
                RoundedRectangle(cornerRadius: 4)
                    .fill(PactPingTheme.kept)
                    .frame(width: geo.size.width * CGFloat(max(0, min(1, ratio))))
            }
        }
        .frame(height: 6)
    }
}

private struct CommitmentCard: View {
    let item: CommitmentItem

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top) {
                Text(item.goal)
                    .font(.witnessExtraBold(18))
                    .tracking(-0.3)
                    .foregroundColor(PactPingTheme.ink)
                Spacer()
                StatusPill(isOverdue: item.isOverdue, isUrgent: item.isUrgent)
            }
            countdownText(item: item)
                .font(.witnessBlack(44))
                .tracking(-2)
                .foregroundColor(item.isUrgent || item.isOverdue ? PactPingTheme.miss : PactPingTheme.ink)
            HStack(spacing: 10) {
                Circle()
                    .fill(avatarColor(seed: item.witnessInitials))
                    .frame(width: 28, height: 28)
                    .overlay(
                        Text(item.witnessInitials)
                            .font(.witnessExtraBold(11))
                            .foregroundColor(.white)
                    )
                Text("\(item.witnessName) watching")
                    .font(.witnessBold(12))
                    .foregroundColor(PactPingTheme.muted)
            }
        }
        .padding(EdgeInsets(top: 18, leading: 20, bottom: 18, trailing: 20))
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 24)
                .fill(PactPingTheme.surface)
                .overlay(
                    RoundedRectangle(cornerRadius: 24)
                        .stroke(PactPingTheme.hairline, lineWidth: 1)
                )
        )
    }

    private func countdownText(item: CommitmentItem) -> Text {
        let parts = item.countdown
        if parts.days > 0 {
            return Text("\(parts.days)d \(parts.hours)h")
        }
        if parts.hours > 0 {
            return Text("\(parts.hours)h \(String(format: "%02d", parts.minutes))m")
        }
        return Text("\(parts.minutes)m")
    }
}

private struct StatusPill: View {
    let isOverdue: Bool
    let isUrgent: Bool

    var body: some View {
        let (bg, fg, label): (Color, Color, String) = {
            if isOverdue {
                return (PactPingTheme.missSoft, PactPingTheme.miss, "DEADLINE BLOWN")
            } else if isUrgent {
                return (PactPingTheme.surfaceAlt, PactPingTheme.strike, "DUE SOON")
            } else {
                return (PactPingTheme.surfaceAlt, PactPingTheme.ink, "ON THE LINE")
            }
        }()
        return Text(label)
            .font(.witnessExtraBold(10))
            .tracking(1.2)
            .foregroundColor(fg)
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .background(Capsule().fill(bg))
    }
}

private struct MakePromiseButton: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text("＋  Make a promise")
                .font(.witnessExtraBold(15))
                .tracking(0.2)
                .foregroundColor(PactPingTheme.onStrike)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 18)
                .background(RoundedRectangle(cornerRadius: 20).fill(PactPingTheme.strike))
        }
        .buttonStyle(.plain)
        .shadow(color: Color(red: 1, green: 0.3, blue: 0.12).opacity(0.35), radius: 14, x: 0, y: 12)
    }
}

// MARK: - Eye glyph

private struct EyeGlyph: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        let w = rect.width, h = rect.height
        path.move(to: CGPoint(x: 0, y: h / 2))
        path.addQuadCurve(to: CGPoint(x: w, y: h / 2),
                          control: CGPoint(x: w / 2, y: -h * 0.15))
        path.addQuadCurve(to: CGPoint(x: 0, y: h / 2),
                          control: CGPoint(x: w / 2, y: h * 1.15))
        path.closeSubpath()
        // pupil
        let r = w * 0.16
        path.addEllipse(in: CGRect(x: w / 2 - r, y: h / 2 - r, width: r * 2, height: r * 2))
        return path
    }
}

// MARK: - Helpers

private func avatarColor(seed: String) -> Color {
    let palette: [Color] = [
        Color(red: 0.247, green: 0.424, blue: 1.0),    // blue
        Color(red: 0.878, green: 0.271, blue: 0.482),  // pink
        Color(red: 0.133, green: 0.627, blue: 0.420),  // green
        Color(red: 0.702, green: 0.435, blue: 0.0),    // amber
        Color(red: 0.431, green: 0.337, blue: 0.812),  // violet
    ]
    let hash = abs(seed.hashValue)
    return palette[hash % palette.count]
}

// MARK: - Previews

#Preview("Home · Empty") {
    ZStack(alignment: .bottom) {
        PactPingTheme.bg.ignoresSafeArea()
        EmptyHome()
        MakePromiseButton(action: {})
            .padding(.horizontal, 22)
            .padding(.bottom, 16)
    }
}

#Preview("Home · Active") {
    let record = PactPingShared.Record(kept: 13, broken: 3)
    let state = HomeUiState(
        record: record,
        active: [
            CommitmentItem(
                id: "c1",
                goal: "Finish marketing deck",
                witnessName: "Mike Chen",
                witnessInitials: "MC",
                countdown: CountdownParts(days: 2, hours: 14, minutes: 22, seconds: 0),
                isUrgent: false,
                isOverdue: false
            ),
            CommitmentItem(
                id: "c2",
                goal: "Pay back Alex ($180)",
                witnessName: "Alex Rivera",
                witnessInitials: "AR",
                countdown: CountdownParts(days: 0, hours: 0, minutes: 47, seconds: 0),
                isUrgent: true,
                isOverdue: false
            ),
            CommitmentItem(
                id: "c3",
                goal: "Submit Q1 review",
                witnessName: "Sarah Park",
                witnessInitials: "SP",
                countdown: CountdownParts(days: 0, hours: 1, minutes: 12, seconds: 0),
                isUrgent: false,
                isOverdue: true
            ),
        ],
        isLoading: false
    )
    return ZStack(alignment: .bottom) {
        PactPingTheme.bg.ignoresSafeArea()
        ActiveHome(state: state, onCommitmentTap: { _, _ in }, onRecordTap: {})
        MakePromiseButton(action: {})
            .padding(.horizontal, 22)
            .padding(.bottom, 16)
    }
}
