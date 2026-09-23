import SwiftUI
import Combine
import PactPingShared

struct SuccessView: View {
    @StateObject private var vm: CommitmentDetailViewModel
    let onClose: () -> Void
    @State private var pendingSms: OutboundSms?
    @State private var smsCopied: SmsCopied?

    init(container: IosAppContainer, id: String, onClose: @escaping () -> Void) {
        _vm = StateObject(wrappedValue: CommitmentDetailViewModel(container: container, id: id))
        self.onClose = onClose
    }

    var body: some View {
        let commitment = vm.state.commitment

        VStack(spacing: 0) {
            TopBar(onClose: onClose)
                .padding(.top, 12)

            Spacer()

            VStack(spacing: 22) {
                GreenBurst()
                VStack(spacing: 6) {
                    Text("LOGGED TO RECORD")
                        .font(.witnessExtraBold(11))
                        .tracking(2.4)
                        .foregroundColor(PactPingTheme.kept)
                    Text("Promise kept.")
                        .font(.witnessBlack(36))
                        .tracking(-1)
                        .foregroundColor(PactPingTheme.ink)
                    if let c = commitment {
                        Spacer().frame(height: 2)
                        Text(c.goal)
                            .font(.witnessMedium(14))
                            .foregroundColor(PactPingTheme.muted)
                        Text(spareLabel(commitment: c))
                            .font(.witnessBold(14))
                            .foregroundColor(PactPingTheme.ink)
                    }
                }
                RecordSummaryCard(record: vm.state.record)
            }
            .padding(.horizontal, 4)

            Spacer()

            if let c = commitment {
                BragButton(witnessName: c.witness.name) {
                    let firstName = c.witness.name.split(separator: " ").first.map(String.init) ?? c.witness.name
                    let body = "Hey \(firstName) — I told you I'd \(c.goal). Done. You can stop watching."
                    switch MessageSend.attempt(recipient: c.witness.phoneNumber, name: firstName, body: body) {
                    case .compose(let sms):
                        pendingSms = sms
                    case .copied(let alert):
                        smsCopied = alert
                    }
                }
                Spacer().frame(height: 4)
            }
            Button(action: onClose) {
                Text("Just close this")
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
        .sheet(item: $pendingSms, onDismiss: { onClose() }) { sms in
            MessageComposer(recipient: sms.recipient, body: sms.body) { _ in
                pendingSms = nil
            }
            .ignoresSafeArea()
        }
        .smsCopiedAlert($smsCopied, onAcknowledge: onClose)
    }

    private func spareLabel(commitment: Commitment) -> String {
        guard let resolved = commitment.resolvedAt else { return "Done just in time." }
        let spare = commitment.deadline.asDate.timeIntervalSince(resolved.asDate)
        if spare <= 0 { return "Done just in time." }
        let minutes = Int(spare / 60)
        let hours = Int(spare / 3600)
        let days = Int(spare / 86_400)
        if days >= 1 { return "Done with \(days)d to spare." }
        if hours >= 1 { return "Done with \(hours)h to spare." }
        if minutes >= 1 { return "Done with \(minutes)m to spare." }
        return "Done just in time."
    }
}

// MARK: - Pieces

private struct TopBar: View {
    let onClose: () -> Void

    var body: some View {
        HStack {
            Button(action: onClose) {
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
            Text("PROMISE KEPT")
                .font(.witnessExtraBold(10))
                .tracking(1.2)
                .foregroundColor(PactPingTheme.kept)
                .padding(.horizontal, 10)
                .padding(.vertical, 5)
                .background(Capsule().fill(PactPingTheme.keptSoft))
            Spacer()
            Color.clear.frame(width: 36, height: 36)
        }
    }
}

private struct GreenBurst: View {
    var body: some View {
        ZStack {
            Circle()
                .fill(PactPingTheme.keptSoft)
                .frame(width: 120, height: 120)
            Circle()
                .fill(PactPingTheme.kept)
                .frame(width: 96, height: 96)
            Text("✓")
                .font(.witnessBlack(56))
                .foregroundColor(.white)
        }
    }
}

private struct RecordSummaryCard: View {
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
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 4).fill(PactPingTheme.miss)
                    RoundedRectangle(cornerRadius: 4)
                        .fill(PactPingTheme.kept)
                        .frame(width: geo.size.width * CGFloat(max(0, min(1, Double(record.keptRatio)))))
                }
            }
            .frame(height: 6)
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
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

private struct BragButton: View {
    let witnessName: String
    let action: () -> Void

    var body: some View {
        let firstName = witnessName.split(separator: " ").first.map(String.init) ?? witnessName
        Button(action: action) {
            Text("Send \(firstName) the win")
                .font(.witnessExtraBold(16))
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 20)
                .background(RoundedRectangle(cornerRadius: 20).fill(PactPingTheme.kept))
        }
        .buttonStyle(.plain)
    }
}
