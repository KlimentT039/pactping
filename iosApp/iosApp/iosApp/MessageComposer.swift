import SwiftUI
import UIKit
import MessageUI

/// SwiftUI wrapper around `MFMessageComposeViewController`. Symmetric to
/// Android's `rememberSmsSender` — we open the user's messaging app with
/// the recipient + body pre-filled and let them tap send. Witness never
/// auto-sends; the witness sees a real message from a real person.
///
/// Callers should gate presentation behind `MFMessageComposeViewController.canSendText()`.
struct MessageComposer: UIViewControllerRepresentable {
    let recipient: String
    let body: String
    let onComplete: (MessageComposeResult) -> Void

    func makeUIViewController(context: Context) -> MFMessageComposeViewController {
        let vc = MFMessageComposeViewController()
        vc.recipients = [recipient]
        vc.body = body
        vc.messageComposeDelegate = context.coordinator
        return vc
    }

    func updateUIViewController(_ uiViewController: MFMessageComposeViewController, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator(onComplete: onComplete) }

    final class Coordinator: NSObject, MFMessageComposeViewControllerDelegate {
        let onComplete: (MessageComposeResult) -> Void

        init(onComplete: @escaping (MessageComposeResult) -> Void) {
            self.onComplete = onComplete
        }

        func messageComposeViewController(_ controller: MFMessageComposeViewController,
                                          didFinishWith result: MessageComposeResult) {
            controller.dismiss(animated: true) {
                self.onComplete(result)
            }
        }
    }
}

/// Reusable payload for `.sheet(item:)` so any screen can present the
/// composer without inventing its own Identifiable wrapper.
struct OutboundSms: Identifiable {
    let id = UUID()
    let recipient: String
    let body: String
}

/// Payload for the "couldn't open Messages, copied instead" alert, presented
/// via `.sheet(item:)`-style `.alert(item:)` on any screen.
struct SmsCopied: Identifiable {
    let id = UUID()
    let recipientName: String
}

/// The outcome of trying to reach a witness: either the native composer
/// should be shown, or texting isn't available (simulator / SIM-less iPad)
/// and we've fallen back to the clipboard.
enum SmsAttempt {
    case compose(OutboundSms)
    case copied(SmsCopied)
}

enum MessageSend {
    /// Decide how to deliver `body` to `recipient`. When the device can send
    /// texts we hand back an `OutboundSms` for the composer; otherwise we copy
    /// the text to the clipboard and hand back a `SmsCopied` alert payload, so
    /// the user still gets the message instead of a silent no-op.
    static func attempt(recipient: String, name: String, body: String) -> SmsAttempt {
        if MFMessageComposeViewController.canSendText() {
            return .compose(OutboundSms(recipient: recipient, body: body))
        }
        UIPasteboard.general.string = body
        return .copied(SmsCopied(recipientName: name))
    }
}

extension View {
    /// Standard alert shown when texting is unavailable and the message was
    /// copied to the clipboard instead. `onAcknowledge` runs when the user
    /// taps OK, so callers can advance the flow exactly as a real send would.
    func smsCopiedAlert(_ item: Binding<SmsCopied?>, onAcknowledge: @escaping () -> Void) -> some View {
        alert(
            "Message copied",
            isPresented: Binding(get: { item.wrappedValue != nil },
                                 set: { if !$0 { item.wrappedValue = nil } }),
            presenting: item.wrappedValue
        ) { _ in
            Button("OK") {
                item.wrappedValue = nil
                onAcknowledge()
            }
        } message: { payload in
            Text("This device can't send texts, so the message to \(payload.recipientName) was copied to your clipboard. Paste it into Messages (or any app) to send it.")
        }
    }
}
