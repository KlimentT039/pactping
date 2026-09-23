import SwiftUI
import Combine
import PactPingShared

@MainActor
final class RecordViewModel: ObservableObject {
    @Published var state: RecordUiState

    private let store: RecordStore
    private let watcher: FlowWatcher<RecordUiState>
    private var subscription: FlowSubscription?

    init(container: IosAppContainer) {
        self.store = container.recordStore
        self.state = RecordUiState(
            record: PactPingShared.Record(kept: 0, broken: 0),
            weeks: [],
            history: [],
            streak: 0,
            filter: HistoryFilter.all,
            isLoading: true
        )
        self.watcher = IosFlowWatchers.shared.record(store: store)
        self.subscription = watcher.watch { [weak self] newState in
            Task { @MainActor in
                self?.state = newState
            }
        }
    }

    func setFilter(_ filter: HistoryFilter) {
        store.setFilter(f: filter)
    }

    deinit {
        subscription?.cancel()
        watcher.close()
    }
}

struct RecordView: View {
    @StateObject private var vm: RecordViewModel
    let onBack: () -> Void

    init(container: IosAppContainer, onBack: @escaping () -> Void) {
        _vm = StateObject(wrappedValue: RecordViewModel(container: container))
        self.onBack = onBack
    }

    var body: some View {
        VStack(spacing: 0) {
            TopBar(onBack: onBack)
                .padding(.top, 12)
            Spacer().frame(height: 20)
            if vm.state.hasHistory {
                ScrollView {
                    LazyVStack(spacing: 20) {
                        HeroNumbers(record: vm.state.record)
                        WeeksChart(state: vm.state)
                        FilterSegments(filter: vm.state.filter) { vm.setFilter($0) }
                        ForEach(vm.state.history, id: \.id) { entry in
                            HistoryRow(entry: entry)
                        }
                        Spacer().frame(height: 8)
                    }
                    .padding(.top, 0)
                }
            } else {
                EmptyHistory()
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
            Text("RECORD")
                .font(.witnessExtraBold(11))
                .tracking(2.4)
                .foregroundColor(PactPingTheme.muted)
            Spacer()
            Color.clear.frame(width: 36, height: 36)
        }
    }
}

// MARK: - Sections

private struct HeroNumbers: View {
    let record: PactPingShared.Record

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("ALL-TIME")
                .font(.witnessExtraBold(11))
                .tracking(2.2)
                .foregroundColor(PactPingTheme.muted)
            HStack(alignment: .bottom) {
                VStack(alignment: .leading, spacing: 0) {
                    Text("\(record.kept)")
                        .font(.witnessBlack(56))
                        .tracking(-2)
                        .foregroundColor(PactPingTheme.kept)
                    Text("KEPT")
                        .font(.witnessExtraBold(11))
                        .tracking(2.2)
                        .foregroundColor(PactPingTheme.kept)
                }
                Spacer()
                Text("/")
                    .font(.witnessBlack(32))
                    .tracking(-1.5)
                    .foregroundColor(PactPingTheme.muted)
                    .padding(.bottom, 14)
                Spacer()
                VStack(alignment: .trailing, spacing: 0) {
                    Text("\(record.broken)")
                        .font(.witnessBlack(56))
                        .tracking(-2)
                        .foregroundColor(PactPingTheme.miss)
                    Text("BROKEN")
                        .font(.witnessExtraBold(11))
                        .tracking(2.2)
                        .foregroundColor(PactPingTheme.miss)
                }
            }
            KeptRatioBar(ratio: Double(record.keptRatio))
        }
    }
}

private struct KeptRatioBar: View {
    let ratio: Double

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                RoundedRectangle(cornerRadius: 4).fill(PactPingTheme.miss)
                RoundedRectangle(cornerRadius: 4)
                    .fill(PactPingTheme.kept)
                    .frame(width: geo.size.width * CGFloat(max(0, min(1, ratio))))
            }
        }
        .frame(height: 6)
    }
}

private struct WeeksChart: View {
    let state: RecordUiState

    var body: some View {
        VStack(spacing: 8) {
            HStack {
                Text("LAST 8 WEEKS")
                    .font(.witnessExtraBold(11))
                    .tracking(2.2)
                    .foregroundColor(PactPingTheme.muted)
                Spacer()
                if state.streak > 0 {
                    Text("+\(state.streak) streak")
                        .font(.witnessExtraBold(12))
                        .foregroundColor(PactPingTheme.kept)
                }
            }
            BarChart(weeks: state.weeks)
                .frame(height: 110)
            HStack {
                ForEach(1...8, id: \.self) { i in
                    Text("W\(i)")
                        .font(.witnessExtraBold(10))
                        .tracking(1)
                        .foregroundColor(PactPingTheme.muted)
                        .frame(maxWidth: .infinity)
                }
            }
        }
    }
}

private struct BarChart: View {
    let weeks: [WeekBucket]

    var body: some View {
        GeometryReader { geo in
            Canvas { ctx, size in
                let n = max(weeks.count, 1)
                let gap: CGFloat = 6
                let colWidth = (size.width - gap * CGFloat(n - 1)) / CGFloat(n)
                let cornerRadius: CGFloat = 4
                let gapBetween: CGFloat = 2
                let maxTotal = max(weeks.map { Int($0.total) }.max() ?? 1, 1)

                for (i, w) in weeks.enumerated() {
                    let total = Int(w.total)
                    guard total > 0 else { continue }
                    let x = CGFloat(i) * (colWidth + gap)
                    let totalHeight = CGFloat(total) / CGFloat(maxTotal) * size.height
                    let keptHeight = CGFloat(w.kept) / CGFloat(total) * totalHeight
                    let missHeight = CGFloat(w.missed) / CGFloat(total) * totalHeight

                    if keptHeight > 0 {
                        let rect = CGRect(x: x, y: size.height - keptHeight, width: colWidth, height: keptHeight)
                        ctx.fill(Path(roundedRect: rect, cornerRadius: cornerRadius),
                                 with: .color(PactPingTheme.kept))
                    }
                    if missHeight > 0 {
                        let topY = size.height - keptHeight - missHeight - (keptHeight > 0 ? gapBetween : 0)
                        let rect = CGRect(x: x, y: topY, width: colWidth, height: missHeight)
                        ctx.fill(Path(roundedRect: rect, cornerRadius: cornerRadius),
                                 with: .color(PactPingTheme.miss))
                    }
                }
            }
            .frame(width: geo.size.width, height: geo.size.height)
        }
    }
}

private struct FilterSegments: View {
    let filter: HistoryFilter
    let onChange: (HistoryFilter) -> Void

    var body: some View {
        HStack(spacing: 4) {
            Segment(title: "All", active: filter == .all) { onChange(.all) }
            Segment(title: "Kept", active: filter == .kept) { onChange(.kept) }
            Segment(title: "Broken", active: filter == .broken) { onChange(.broken) }
        }
        .padding(4)
        .background(RoundedRectangle(cornerRadius: 14).fill(PactPingTheme.surfaceAlt))
    }
}

private struct Segment: View {
    let title: String
    let active: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.witnessBold(12))
                .foregroundColor(active ? PactPingTheme.bg : PactPingTheme.muted)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
                .background(
                    RoundedRectangle(cornerRadius: 10)
                        .fill(active ? PactPingTheme.ink : Color.clear)
                )
        }
        .buttonStyle(.plain)
    }
}

private struct HistoryRow: View {
    let entry: HistoryEntry

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 12) {
                Circle()
                    .fill(entry.isKept ? PactPingTheme.kept : PactPingTheme.miss)
                    .frame(width: 8, height: 8)
                Text(entry.goal)
                    .font(.witnessBold(14))
                    .foregroundColor(PactPingTheme.ink)
                    .frame(maxWidth: .infinity, alignment: .leading)
                let date = HistoryDate.string(from: entry.resolvedAt)
                Text(entry.wasLate ? "\(date) · late" : date)
                    .font(.witnessBold(12))
                    .foregroundColor(PactPingTheme.muted)
            }
            .padding(.vertical, 14)
            Rectangle()
                .fill(PactPingTheme.hairline)
                .frame(height: 1)
        }
    }
}

private struct EmptyHistory: View {
    var body: some View {
        VStack(spacing: 8) {
            Spacer()
            Text("No history yet.")
                .font(.witnessBlack(28))
                .tracking(-0.8)
                .foregroundColor(PactPingTheme.ink)
            Text("When you start hitting (or missing) deadlines, they'll land here.")
                .font(.witnessMedium(14))
                .foregroundColor(PactPingTheme.muted)
                .multilineTextAlignment(.center)
            Spacer()
        }
        .padding(.horizontal, 12)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private enum HistoryDate {
    private static let formatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "EEE MMM d"
        return f
    }()

    static func string(from instant: Kotlinx_datetimeInstant) -> String {
        formatter.string(from: instant.asDate)
    }
}
