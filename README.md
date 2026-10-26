# Witness

**Accountability that actually bites.** Set a goal with a deadline, name a witness from your contacts, and commit to a confession if you miss it. Hit the goal and you log the win. Miss it, and the app hands you a one-tap message to send your witness — no wriggling out.

> Working title — the current builds ship under the name **PactPing**, which is what you'll see in the screenshots. The point of the app is that a real person is watching, and you never need them to install anything.

<p align="center">
  <img src="docs/screenshots/home.png" width="240" alt="Home screen with record card and active commitments">
  <img src="docs/screenshots/detail.png" width="240" alt="Commitment detail with live countdown">
  <img src="docs/screenshots/confession.png" width="240" alt="Missed deadline confession screen">
</p>

![Kotlin](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?logo=kotlin&logoColor=white)
![Platforms](https://img.shields.io/badge/Platforms-Android%20%7C%20iOS-3DDC84)
![UI](https://img.shields.io/badge/UI-Compose%20%2B%20SwiftUI-blue)
![License](https://img.shields.io/badge/License-MIT-green)

---

## What it is

Most habit and accountability apps run on the honor system: you tap "done" and nobody is the wiser. Witness puts a real person and a real consequence behind a specific deadline. You declare a one-off commitment, pick a witness from your phone's contacts, and write the confession that gets sent if you fall short. The witness is notified up front, so someone is genuinely expecting you to follow through — but they only ever receive a normal text message, so there is no social network to join and nothing for them to download.

The interesting engineering story is that the entire product runs on **one shared Kotlin Multiplatform core with fully native UIs** — Jetpack Compose on Android and SwiftUI on iOS — and no backend at all. State, notifications, and the confession flow are handled on-device.

## Screenshots

All screenshots are from the Android app (Jetpack Compose) running on an emulator. Raw images live in [`docs/screenshots/`](docs/screenshots).

### Home, detail, and record

| Home | Commitment detail | Record |
| :---: | :---: | :---: |
| <img src="docs/screenshots/home.png" width="240" alt="Home"> | <img src="docs/screenshots/detail.png" width="240" alt="Commitment detail"> | <img src="docs/screenshots/record.png" width="240" alt="Record"> |
| Kept vs. broken at a glance, active promises sorted by deadline. | Live countdown, the witness, and the exact confession on the line. | All-time tally, last 8 weeks, and filterable history. |

### Making a promise

A five-step flow: goal → deadline → witness → confession → summary.

| 1. Goal | 2. Deadline | 3. Witness |
| :---: | :---: | :---: |
| <img src="docs/screenshots/create-goal.png" width="240" alt="Create: goal"> | <img src="docs/screenshots/create-deadline.png" width="240" alt="Create: deadline"> | <img src="docs/screenshots/create-witness.png" width="240" alt="Create: witness"> |

| 4. Confession | 5. Summary |
| :---: | :---: |
| <img src="docs/screenshots/create-confession.png" width="240" alt="Create: confession"> | <img src="docs/screenshots/create-summary.png" width="240" alt="Create: summary"> |

The witness is picked with the system contact picker, and "Lock it in" opens the SMS app with the heads-up text pre-filled. You always tap Send yourself.

### Outcomes

| Promise kept | Deadline blown |
| :---: | :---: |
| <img src="docs/screenshots/promise-kept.png" width="240" alt="Promise kept"> | <img src="docs/screenshots/confession.png" width="240" alt="Confession"> |
| Logs the win and offers to brag to your witness. | Hands you the confession, ready to send as-is. |

### Dark theme

| Home | Record |
| :---: | :---: |
| <img src="docs/screenshots/home-dark.png" width="240" alt="Home, dark theme"> | <img src="docs/screenshots/record-dark.png" width="240" alt="Record, dark theme"> |

<!-- TODO: App Store and Google Play links once published -->

## Features

- Create a one-off commitment with a goal and a hard deadline.
- Pick a witness from your contacts; they get a heads-up text when named.
- Write a custom confession that is sent to the witness if you miss.
- Deadline reminders via native local notifications.
- Live countdown on every active promise.
- A "send them the win" message when you keep a promise.
- A running record of kept versus broken promises, with an 8-week chart.
- Light and dark themes.
- Fully offline. No account, no server, no data leaving the device.

## Why I built it

I wanted a focused project that demonstrates production Kotlin Multiplatform the way real teams ship it: shared business logic, native UI on each platform, and clean handling of the genuinely hard cross-platform problems. The accountability concept is deliberately small in scope but exercises everything that matters — persistence, scheduling, platform interop, and a Kotlin-to-Swift bridge that a native iOS UI can consume comfortably.

## Architecture

A single shared module holds the domain, data, and presentation logic. Each platform owns its UI and talks to the shared layer through a thin, idiomatic bridge.

```mermaid
graph TD
    subgraph Android
        A[Jetpack Compose UI] --> VM
    end
    subgraph iOS
        B[SwiftUI Views] --> OBS[ObservableObject wrappers]
        OBS --> VM
    end
    subgraph Shared KMP Module
        VM[Presentation: Stores + StateFlow]
        VM --> REPO[Domain: Repository interfaces]
        REPO --> DECO[Notification decorator]
        DECO --> IMPL[Data: SQLDelight repository]
        IMPL --> DB[(SQLDelight)]
        VM --> PLAT[expect/actual platform APIs]
    end
    PLAT -. Android actual .-> ANDP[AlarmManager / Notifications / Intents]
    PLAT -. iOS actual .-> IOSP[UNUserNotificationCenter / MessageUI / Contacts]
```

The presentation layer exposes immutable UI state as Kotlin `Flow` / `StateFlow` and actions as plain functions. On Android, Compose collects the flow directly. On iOS, a small `FlowWatcher` in `iosMain` turns each flow into a callback subscription, and a SwiftUI `ObservableObject` wraps each shared store and republishes its state as `@Published`. That wrapper is the whole bridge between Kotlin and SwiftUI. [SKIE](https://skie.touchlab.co/) is on the roadmap to replace the callbacks with Swift `async` / `AsyncSequence`.

Deadline scheduling is wired in as a repository decorator (`NotifyingCommitmentRepository`) rather than being called from every store, so the presentation layer never has to think about alarms or notifications.

The platform-specific work — scheduling deadline reminders, sending the confession, and reading contacts — is isolated behind `expect`/`actual` declarations, with Android implementations using `AlarmManager`, notifications, and intents, and iOS implementations using `UNUserNotificationCenter`, `MessageUI`, and the Contacts framework.

## Tech stack

| Concern | Choice |
| --- | --- |
| Shared language | Kotlin Multiplatform |
| Android UI | Jetpack Compose |
| iOS UI | SwiftUI |
| Kotlin ↔ Swift interop | XCFramework + `FlowWatcher` callbacks (SKIE planned) |
| Async | Coroutines + Flow / StateFlow |
| Persistence | SQLDelight |
| Dependency injection | Manual (composition roots per platform) |
| Dates | kotlinx-datetime |
| Architecture | Layered domain / data / presentation with StateFlow-driven stores |
| Tests | kotlin.test, JUnit |

## Project structure

```
witness/
├── shared/                       # Kotlin Multiplatform core
│   └── src/
│       ├── commonMain/kotlin/
│       │   ├── domain/           # entities (Commitment, Witness, Record) + repository interfaces
│       │   ├── data/             # SQLDelight + in-memory repository impls
│       │   ├── notification/     # DeadlineNotifier + repository decorator that schedules alarms
│       │   └── presentation/     # Stores exposing StateFlow / Flow of UI state
│       ├── commonMain/sqldelight/# .sq schema and typed queries
│       ├── androidMain/kotlin/   # Android actuals (AlarmManager, SQLite driver)
│       ├── iosMain/kotlin/       # iOS actuals + IosAppContainer composition root
│       └── commonTest/kotlin/    # shared unit tests
├── androidApp/                   # Jetpack Compose application (screens + platform integrations)
├── iosApp/                       # SwiftUI application (Xcode project)
├── design/                       # UI mockups
├── docs/screenshots/             # README screenshots
├── build.gradle.kts
└── settings.gradle.kts
```

## Getting started

### Requirements

- Android Studio (latest stable) with the Kotlin Multiplatform plugin
- Xcode (latest stable) for the iOS app
- JDK 17+

### Android

1. Open the project root in Android Studio.
2. Select the `androidApp` run configuration.
3. Run on an emulator or device.

### iOS

1. Build the shared framework: `./gradlew :shared:assemblePactPingSharedXCFramework`.
2. Open `iosApp/iosApp/iosApp.xcodeproj` in Xcode.
3. Select a simulator and run.

See [`iosApp/README.md`](iosApp/README.md) for the full Xcode setup.

## Key technical decisions

**Shared logic, native UI.** Compose Multiplatform would have shared the UI too, but native UI per platform is how many production KMP codebases actually operate, especially when KMP is adopted into existing native apps. It keeps each platform's UX best-in-class and demonstrates the harder skill: clean Kotlin-to-Swift interop rather than the everything-shared shortcut.

**A thin interop layer, SKIE next.** Raw Kotlin suspend functions and flows are awkward to consume from Swift. For now a small `FlowWatcher` turns flows into callbacks that SwiftUI wrappers republish. The next step is SKIE, which makes the generated API feel native — `async` functions, `AsyncSequence` flows, and real Swift enums for sealed classes.

**SQLDelight over a simpler key-value store.** Commitments, witnesses, and history are relational and queried, so type-safe SQL shared across both platforms is the right fit and avoids hand-rolled serialization.

**No backend.** Reaching the witness through native messaging and keeping all data on-device removes server cost, sync complexity, and the cold-start problem a social app would otherwise face. It also makes a clean privacy story: nothing leaves the phone.

## Roadmap

**Product**
- [ ] Escalating stakes for repeat misses
- [x] "Brag" message to the witness on success, not just confession on failure
- [ ] Optional photo proof when you complete a goal
- [ ] Verified check-ins via Health Connect / HealthKit for fitness goals
- [ ] Widgets and Live Activity / Dynamic Island support

**Engineering**
- [ ] Adopt SKIE for Swift `async` / `AsyncSequence` interop
- [ ] Introduce Koin as a shared DI container (replaces the two hand-wired composition roots)
- [ ] Add a domain use-case layer between stores and repositories
- [ ] Convert store outputs to `StateFlow` via `stateIn` and hoist the countdown ticker
- [ ] Split the larger screen files (Create, Home, Record) into per-component files
- [ ] Extract a small design-system module for the reused Compose primitives
- [ ] Wire up GitHub Actions CI plus detekt / ktlint