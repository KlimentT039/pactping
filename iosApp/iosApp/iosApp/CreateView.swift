import SwiftUI
import Combine
import PactPingShared

enum CreateStep: Int, Hashable, CaseIterable {
    case goal, deadline, witness, confession, summary

    static let total = 5
    var index: Int { rawValue }
}

struct CreateView: View {
    @StateObject private var vm: CreateViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var path: [CreateStep] = []

    init(container: IosAppContainer) {
        _vm = StateObject(wrappedValue: CreateViewModel(container: container))
    }

    var body: some View {
        NavigationStack(path: $path) {
            GoalStep(
                vm: vm,
                onNext: { path.append(.deadline) },
                onClose: { dismiss() }
            )
            .navigationDestination(for: CreateStep.self) { step in
                switch step {
                case .goal: EmptyView()
                case .deadline:
                    DeadlineStep(vm: vm, onNext: { path.append(.witness) })
                case .witness:
                    WitnessStepView(vm: vm, onNext: { path.append(.confession) })
                case .confession:
                    ConfessionStepView(vm: vm, onNext: { path.append(.summary) })
                case .summary:
                    SummaryStep(
                        vm: vm,
                        onDone: { dismiss() },
                        onEdit: { path.removeAll() }
                    )
                }
            }
            .toolbar(.hidden, for: .navigationBar)
        }
        .interactiveDismissDisabled(false)
    }
}

// MARK: - Shared chrome

private struct StepScaffold<Content: View, CTA: View>: View {
    let stepIndex: Int
    let onBack: () -> Void
    @ViewBuilder var content: () -> Content
    @ViewBuilder var cta: () -> CTA

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                CircleIconButton(label: stepIndex == 0 ? "×" : "←", action: onBack)
                Spacer()
                StepIndicatorView(stepIndex: stepIndex)
                Spacer()
                Color.clear.frame(width: 36, height: 36)
            }
            .padding(.top, 12)

            ScrollView {
                content()
                    .padding(.top, 20)
            }

            cta()
                .padding(.top, 12)
        }
        .padding(.horizontal, 22)
        .padding(.bottom, 16)
        .background(PactPingTheme.bg.ignoresSafeArea())
        .navigationBarBackButtonHidden(true)
    }
}

private struct CircleIconButton: View {
    let label: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Circle()
                .fill(PactPingTheme.surface)
                .frame(width: 36, height: 36)
                .overlay(
                    Text(label)
                        .font(.witnessExtraBold(16))
                        .foregroundColor(PactPingTheme.ink)
                )
        }
        .buttonStyle(.plain)
    }
}

private struct StepIndicatorView: View {
    let stepIndex: Int

    var body: some View {
        HStack(spacing: 6) {
            ForEach(0..<CreateStep.total, id: \.self) { i in
                let active = i == stepIndex
                RoundedRectangle(cornerRadius: 2)
                    .fill(active ? PactPingTheme.ink : PactPingTheme.hairline)
                    .frame(width: active ? 36 : 22, height: 4)
            }
        }
    }
}

private struct StepHeader: View {
    let index: Int
    let title: String
    var subtitle: String? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("STEP \(index + 1) OF \(CreateStep.total)")
                .font(.witnessExtraBold(11))
                .tracking(2.2)
                .foregroundColor(PactPingTheme.muted)
            Text(title)
                .font(.witnessBlack(34))
                .tracking(-1)
                .lineSpacing(2)
                .foregroundColor(PactPingTheme.ink)
                .multilineTextAlignment(.leading)
            if let subtitle = subtitle {
                Text(subtitle)
                    .font(.witnessMedium(14))
                    .foregroundColor(PactPingTheme.muted)
                    .multilineTextAlignment(.leading)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct PrimaryCta: View {
    let title: String
    let enabled: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.witnessExtraBold(16))
                .foregroundColor(enabled ? PactPingTheme.onStrike : PactPingTheme.muted)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 20)
                .background(
                    RoundedRectangle(cornerRadius: 20)
                        .fill(enabled ? PactPingTheme.strike : PactPingTheme.hairline)
                )
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}

// MARK: - Step 1: Goal

private struct GoalStep: View {
    @ObservedObject var vm: CreateViewModel
    let onNext: () -> Void
    let onClose: () -> Void

    var body: some View {
        StepScaffold(stepIndex: 0, onBack: onClose) {
            VStack(alignment: .leading, spacing: 28) {
                StepHeader(index: 0, title: "What's the\npromise?")
                VStack(alignment: .leading, spacing: 14) {
                    TextField(
                        "Finish marketing deck",
                        text: Binding(
                            get: { vm.state.goal },
                            set: { vm.setGoal($0) }
                        )
                    )
                    .font(.witnessExtraBold(26))
                    .foregroundColor(PactPingTheme.ink)
                    .textInputAutocapitalization(.sentences)
                    Rectangle().fill(PactPingTheme.ink).frame(height: 2)
                    Text("Be specific. Future you isn't allowed to weasel. \"Get fit\" doesn't count.")
                        .font(.witnessMedium(14))
                        .foregroundColor(PactPingTheme.muted)
                }
            }
        } cta: {
            PrimaryCta(title: "Next  →", enabled: vm.state.hasGoal, action: onNext)
        }
    }
}

// MARK: - Step 2: Deadline

private struct DeadlineStep: View {
    @ObservedObject var vm: CreateViewModel
    let onNext: () -> Void
    @State private var localDate: Date = Date().addingTimeInterval(60 * 60 * 24) // tomorrow

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        StepScaffold(stepIndex: 1, onBack: { dismiss() }) {
            VStack(alignment: .leading, spacing: 28) {
                StepHeader(index: 1, title: "When's the line\nin the sand?")

                VStack(alignment: .leading, spacing: 16) {
                    DatePicker(
                        "Deadline",
                        selection: Binding(
                            get: { localDate },
                            set: { newDate in
                                localDate = newDate
                                vm.setDeadline(newDate)
                            }
                        ),
                        in: Date()...,
                        displayedComponents: [.date, .hourAndMinute]
                    )
                    .datePickerStyle(.graphical)
                    .accentColor(PactPingTheme.strike)
                    .padding(16)
                    .background(
                        RoundedRectangle(cornerRadius: 22)
                            .fill(PactPingTheme.surface)
                            .overlay(
                                RoundedRectangle(cornerRadius: 22)
                                    .stroke(PactPingTheme.hairline, lineWidth: 1)
                            )
                    )

                    Text("Once set, it's locked. No extensions, no take-backs.")
                        .font(.witnessMedium(14))
                        .foregroundColor(PactPingTheme.muted)
                }
            }
        } cta: {
            PrimaryCta(title: "Next  →", enabled: vm.state.hasDeadline, action: onNext)
        }
        .onAppear {
            if let existing = vm.state.deadline {
                localDate = existing.asDate
            } else {
                vm.setDeadline(localDate)
            }
        }
    }
}

// MARK: - Step 3: Witness

private struct WitnessStepView: View {
    @ObservedObject var vm: CreateViewModel
    let onNext: () -> Void
    @State private var showPicker = false
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        StepScaffold(stepIndex: 2, onBack: { dismiss() }) {
            VStack(alignment: .leading, spacing: 20) {
                StepHeader(
                    index: 2,
                    title: "Who's\nwatching?",
                    subtitle: "They get a heads-up text now and the confession later — only if you miss."
                )

                if let witness = vm.state.witness {
                    SelectedWitnessCard(witness: witness)
                    Button("Pick a different one") { showPicker = true }
                        .font(.witnessBold(13))
                        .foregroundColor(PactPingTheme.muted)
                        .frame(maxWidth: .infinity)
                    HeadsUpPreview(goal: vm.state.goal, deadline: vm.state.deadline)
                } else {
                    PickContactCard { showPicker = true }
                }
            }
        } cta: {
            PrimaryCta(title: "Next  →", enabled: vm.state.hasWitness, action: onNext)
        }
        .sheet(isPresented: $showPicker) {
            ContactPicker { name, phone in
                vm.setWitness(name: name, phoneNumber: phone)
                showPicker = false
            }
            .ignoresSafeArea()
        }
    }
}

private struct PickContactCard: View {
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 14) {
                Circle()
                    .fill(PactPingTheme.ink)
                    .frame(width: 44, height: 44)
                    .overlay(
                        Text("＋")
                            .font(.witnessBlack(22))
                            .foregroundColor(PactPingTheme.bg)
                    )
                VStack(alignment: .leading, spacing: 2) {
                    Text("Pick a contact")
                        .font(.witnessExtraBold(17))
                        .foregroundColor(PactPingTheme.ink)
                    Text("We'll only use their name and number.")
                        .font(.witnessMedium(13))
                        .foregroundColor(PactPingTheme.muted)
                }
                Spacer()
            }
            .padding(EdgeInsets(top: 22, leading: 20, bottom: 22, trailing: 20))
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 22)
                    .fill(PactPingTheme.surface)
                    .overlay(
                        RoundedRectangle(cornerRadius: 22)
                            .stroke(PactPingTheme.hairline, lineWidth: 1)
                    )
            )
        }
        .buttonStyle(.plain)
    }
}

private struct SelectedWitnessCard: View {
    let witness: Witness

    var body: some View {
        HStack(spacing: 14) {
            Circle()
                .fill(avatarColor(witness.initials))
                .frame(width: 44, height: 44)
                .overlay(
                    Text(witness.initials)
                        .font(.witnessExtraBold(15))
                        .foregroundColor(.white)
                )
            VStack(alignment: .leading, spacing: 2) {
                Text(witness.name)
                    .font(.witnessExtraBold(17))
                    .foregroundColor(PactPingTheme.ink)
                Text(witness.phoneNumber)
                    .font(.witnessMedium(13))
                    .foregroundColor(PactPingTheme.muted)
            }
            Spacer()
            Circle()
                .fill(PactPingTheme.ink)
                .frame(width: 26, height: 26)
                .overlay(
                    Text("✓")
                        .font(.witnessBlack(14))
                        .foregroundColor(PactPingTheme.bg)
                )
        }
        .padding(EdgeInsets(top: 18, leading: 20, bottom: 18, trailing: 20))
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

private struct HeadsUpPreview: View {
    let goal: String
    let deadline: Kotlinx_datetimeInstant?

    var body: some View {
        guard !goal.isEmpty, let deadline = deadline else {
            return AnyView(EmptyView())
        }
        let body = HeadsUpText.body(
            goal: goal,
            deadlineLabel: DeadlineFormat.string(from: deadline)
        )
        return AnyView(
            VStack(alignment: .leading, spacing: 8) {
                Text("HEADS-UP THEY'LL GET")
                    .font(.witnessExtraBold(10))
                    .tracking(2)
                    .foregroundColor(PactPingTheme.muted)
                Text(body)
                    .font(.witnessMedium(14))
                    .foregroundColor(PactPingTheme.ink)
                    .padding(EdgeInsets(top: 16, leading: 18, bottom: 16, trailing: 18))
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(
                        RoundedRectangle(cornerRadius: 22)
                            .fill(PactPingTheme.surface)
                            .overlay(
                                RoundedRectangle(cornerRadius: 22)
                                    .stroke(PactPingTheme.hairline, lineWidth: 1)
                            )
                    )
            }
        )
    }
}

// MARK: - Step 4: Confession

private struct ConfessionStepView: View {
    @ObservedObject var vm: CreateViewModel
    let onNext: () -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        let firstName = vm.state.witness?.name.split(separator: " ").first.map(String.init) ?? "your witness"

        return StepScaffold(stepIndex: 3, onBack: { dismiss() }) {
            VStack(alignment: .leading, spacing: 20) {
                StepHeader(
                    index: 3,
                    title: "The\nconfession.",
                    subtitle: "If you miss, this goes to \(firstName), verbatim. Make it honest."
                )

                ZStack(alignment: .topLeading) {
                    if vm.state.confession.isEmpty {
                        Text("Hey \(firstName) — I told you I'd ___ by ___. I didn't. No good excuse. Sorry for the noise.")
                            .font(.witnessMedium(15))
                            .foregroundColor(PactPingTheme.muted)
                            .padding(18)
                    }
                    TextEditor(text: Binding(
                        get: { vm.state.confession },
                        set: { if $0.count <= 280 { vm.setConfession($0) } }
                    ))
                    .font(.witnessMedium(15))
                    .foregroundColor(PactPingTheme.ink)
                    .scrollContentBackground(.hidden)
                    .padding(13)
                    .frame(minHeight: 150)
                }
                .background(
                    RoundedRectangle(cornerRadius: 18)
                        .fill(PactPingTheme.surface)
                        .overlay(
                            RoundedRectangle(cornerRadius: 18)
                                .stroke(PactPingTheme.hairline, lineWidth: 1)
                        )
                )

                HStack {
                    Text("Drafted by you · editable until deadline")
                        .font(.witnessMedium(12))
                        .foregroundColor(PactPingTheme.muted)
                    Spacer()
                    Text("\(vm.state.confession.count) / 280")
                        .font(.witnessExtraBold(12))
                        .foregroundColor(vm.state.hasConfession ? PactPingTheme.ink : PactPingTheme.muted)
                }
            }
        } cta: {
            PrimaryCta(title: "Next  →", enabled: vm.state.hasConfession, action: onNext)
        }
    }
}

// MARK: - Step 5: Summary

private struct SummaryStep: View {
    @ObservedObject var vm: CreateViewModel
    /// Called once the entire create flow is done — after commit succeeds
    /// and the heads-up SMS composer (if any) has been dismissed.
    let onDone: () -> Void
    let onEdit: () -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var pendingSms: OutboundSms?
    @State private var smsCopied: SmsCopied?

    var body: some View {
        let witness = vm.state.witness
        let firstName = witness?.name.split(separator: " ").first.map(String.init) ?? "they"

        return StepScaffold(stepIndex: 4, onBack: { dismiss() }) {
            VStack(alignment: .leading, spacing: 20) {
                StepHeader(index: 4, title: "Here's\nthe deal.")

                VStack(spacing: 0) {
                    SummaryRow(label: "GOAL", value: vm.state.goal.isEmpty ? "—" : vm.state.goal)
                    Divider().background(PactPingTheme.hairline)
                    SummaryRow(
                        label: "DUE",
                        value: vm.state.deadline.map(DeadlineFormat.string) ?? "—"
                    )
                    Divider().background(PactPingTheme.hairline)
                    SummaryRow(
                        label: "WITNESS",
                        value: witness?.name ?? "—",
                        sub: witness?.phoneNumber
                    )
                }
                .padding(.horizontal, 18)
                .background(
                    RoundedRectangle(cornerRadius: 22)
                        .fill(PactPingTheme.surface)
                        .overlay(
                            RoundedRectangle(cornerRadius: 22)
                                .stroke(PactPingTheme.hairline, lineWidth: 1)
                        )
                )

                VStack(alignment: .leading, spacing: 8) {
                    Text("IF YOU MISS, \(firstName.uppercased()) GETS")
                        .font(.witnessExtraBold(11))
                        .tracking(2)
                        .foregroundColor(PactPingTheme.muted)
                    Text(vm.state.confession.isEmpty ? "—" : vm.state.confession)
                        .font(.witnessMedium(14))
                        .foregroundColor(PactPingTheme.bg)
                        .padding(EdgeInsets(top: 16, leading: 18, bottom: 16, trailing: 18))
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
        } cta: {
            VStack(spacing: 4) {
                PrimaryCta(title: "Lock it in", enabled: vm.state.canCommit) {
                    commitAndMaybeSend()
                }
                Button("Go back and edit", action: onEdit)
                    .font(.witnessBold(13))
                    .foregroundColor(PactPingTheme.muted)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
            }
        }
        .sheet(item: $pendingSms, onDismiss: { onDone() }) { sms in
            MessageComposer(recipient: sms.recipient, body: sms.body) { _ in
                // The composer dismisses itself on completion; clearing the
                // binding triggers our onDismiss → onDone.
                pendingSms = nil
            }
            .ignoresSafeArea()
        }
        .smsCopiedAlert($smsCopied, onAcknowledge: onDone)
    }

    /// Snapshot the create-state, commit, then launch the heads-up composer.
    /// `commit()` resets the store so we capture the values first.
    private func commitAndMaybeSend() {
        let goal = vm.state.goal
        let deadline = vm.state.deadline
        let witness = vm.state.witness

        vm.commit { success in
            guard success else { return }
            guard let w = witness, let d = deadline else {
                onDone()
                return
            }
            let body = HeadsUpText.body(
                goal: goal,
                deadlineLabel: DeadlineFormat.string(from: d)
            )
            let firstName = w.name.split(separator: " ").first.map(String.init) ?? w.name
            switch MessageSend.attempt(recipient: w.phoneNumber, name: firstName, body: body) {
            case .compose(let sms):
                pendingSms = sms
            case .copied(let alert):
                // No SMS available (simulator, no SIM) — copy to clipboard
                // and tell the user instead of silently closing.
                smsCopied = alert
            }
        }
    }
}

private struct SummaryRow: View {
    let label: String
    let value: String
    var sub: String? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.witnessExtraBold(10))
                .tracking(2)
                .foregroundColor(PactPingTheme.muted)
            Text(value)
                .font(.witnessBold(16))
                .foregroundColor(PactPingTheme.ink)
            if let sub = sub {
                Text(sub)
                    .font(.witnessMedium(13))
                    .foregroundColor(PactPingTheme.muted)
            }
        }
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

// MARK: - Helpers

private func avatarColor(_ seed: String) -> Color {
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
