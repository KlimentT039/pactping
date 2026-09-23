import Foundation
import PactPingShared

// Bridge Foundation.Date <-> kotlinx.datetime.Instant. The K/Native Swift
// name is module-prefixed because Instant comes from the kotlinx-datetime
// dependency, not from our package.

extension Date {
    var kotlinInstant: Kotlinx_datetimeInstant {
        Kotlinx_datetimeInstant.companion.fromEpochMilliseconds(
            epochMilliseconds: Int64(self.timeIntervalSince1970 * 1000)
        )
    }
}

extension Kotlinx_datetimeInstant {
    var asDate: Date {
        Date(timeIntervalSince1970: TimeInterval(toEpochMilliseconds()) / 1000.0)
    }
}

// SwiftUI-friendly formatter that matches the shared `formatDeadline()` output.
enum DeadlineFormat {
    private static let formatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "EEE MMM d · h:mm a"
        return f
    }()

    static func string(from date: Date) -> String { formatter.string(from: date) }
    static func string(from instant: Kotlinx_datetimeInstant) -> String { string(from: instant.asDate) }
}

/// Single source of truth for the heads-up SMS body. Used both for the
/// preview shown on the Witness step and for the actual send launched
/// after `commit()` on the Summary step. Matches the Android
/// `R.string.create_witness_headsup_template`.
enum HeadsUpText {
    static func body(goal: String, deadlineLabel: String) -> String {
        "Heads up — I'm using Witness to hold myself to \(goal) by \(deadlineLabel). " +
        "If I miss, you'll get an honest confession from me. Nothing for you to do."
    }
}
