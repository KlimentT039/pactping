import SwiftUI
import ContactsUI

/// SwiftUI wrapper around `CNContactPickerViewController` that returns the
/// (name, phone-number) pair the user selected. The picker is sandboxed:
/// the system grants temporary read of the picked contact only, so we don't
/// need NSContactsUsageDescription or runtime permission flow.
struct ContactPicker: UIViewControllerRepresentable {
    let onPicked: (_ name: String, _ phoneNumber: String) -> Void

    func makeUIViewController(context: Context) -> CNContactPickerViewController {
        let picker = CNContactPickerViewController()
        picker.displayedPropertyKeys = [CNContactPhoneNumbersKey]
        // Force the user to choose ONE phone number rather than picking a
        // whole contact (which might have multiples). Setting the contact
        // predicate to false stops the picker from returning the whole
        // contact on tap (which would fire the unimplemented
        // `didSelect contact:` callback and silently pick nothing); instead
        // it always drills into the contact so a phone number can be picked.
        picker.predicateForSelectionOfContact = NSPredicate(value: false)
        picker.predicateForSelectionOfProperty = NSPredicate(format: "key == 'phoneNumbers'")
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: CNContactPickerViewController, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator(onPicked: onPicked) }

    final class Coordinator: NSObject, CNContactPickerDelegate {
        let onPicked: (String, String) -> Void

        init(onPicked: @escaping (String, String) -> Void) {
            self.onPicked = onPicked
        }

        func contactPicker(_ picker: CNContactPickerViewController,
                           didSelect contactProperty: CNContactProperty) {
            guard let phoneNumber = contactProperty.value as? CNPhoneNumber else { return }
            let contact = contactProperty.contact
            let formatted = CNContactFormatter.string(from: contact, style: .fullName)
            let fallback = "\(contact.givenName) \(contact.familyName)"
                .trimmingCharacters(in: .whitespaces)
            let name = (formatted?.isEmpty == false ? formatted : nil)
                ?? (fallback.isEmpty ? phoneNumber.stringValue : fallback)
            onPicked(name, phoneNumber.stringValue)
        }
    }
}
