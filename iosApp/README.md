# iosApp

SwiftUI shell for Witness. Consumes the shared Kotlin Multiplatform module
(`:shared`) via an XCFramework. Mirrors the Android Compose Home screen so
the first run validates the shared persistence + presentation layer on iOS.

## What's here

```
iosApp/
├── iosApp/                       # Swift source files (drop into an Xcode project)
│   ├── iOSApp.swift              # @main App entry — owns AppEnvironment
│   ├── ContentView.swift         # NavigationStack + Route enum routing the home stack
│   ├── HomeView.swift            # SwiftUI Home — subscribes to HomeStore via FlowWatcher
│   ├── DetailView.swift          # Commitment detail w/ live countdown + Mark Done
│   ├── SuccessView.swift         # Promise-kept celebration + brag SMS
│   ├── ConfessionView.swift      # Miss flow — sends the confession via MessageComposer
│   ├── RecordView.swift          # All-time record, 8-week chart, filterable history
│   ├── CreateView.swift          # 5-step Create flow (Goal → Deadline → Witness → Confession → Summary)
│   ├── CreateViewModel.swift     # Republishes CreateCommitmentStore.state as @Published
│   ├── ContactPicker.swift       # UIViewControllerRepresentable for CNContactPickerViewController
│   ├── MessageComposer.swift     # UIViewControllerRepresentable for MFMessageComposeViewController
│   ├── InteropExtensions.swift   # Date <-> Kotlinx_datetimeInstant bridging + deadline / heads-up text helpers
│   ├── WitnessTheme.swift        # Color + font tokens mirroring the Compose theme
│   └── Info.plist                # Minimal Info.plist
└── README.md                     # this file
```

The Xcode project itself is **not** committed — generate it locally in five
clicks (instructions below) so its build settings are fresh for your Xcode
version.

## One-time setup

### 1. Build the shared XCFramework

From the repo root:

```bash
./gradlew :shared:assembleWitnessSharedXCFramework
```

That writes the framework to:

```
shared/build/XCFrameworks/release/WitnessShared.xcframework
shared/build/XCFrameworks/debug/WitnessShared.xcframework
```

Use the **debug** one while you're iterating (faster builds, better symbols);
swap to release for Archive/TestFlight.

### 2. Create the Xcode project

1. Open Xcode → **File** → **New** → **Project…**
2. Pick **iOS** → **App**. Next.
3. Product name: **iosApp**.
   Interface: **SwiftUI**.
   Language: **Swift**.
   Storage: **None**.
   Tests: optional.
4. **Save to** `witness/iosApp/` (this folder). Uncheck "Create Git
   repository". Click Create.
5. Xcode will generate `iosApp/iosApp.xcodeproj/` plus a stub
   `iosApp/iosApp/` folder with placeholder `iosAppApp.swift`,
   `ContentView.swift`, etc.

### 3. Replace stubs with the source files in this repo

In Finder, **delete** Xcode's placeholders:

- `iosApp/iosApp/iosAppApp.swift`
- `iosApp/iosApp/ContentView.swift`
- `iosApp/iosApp/Info.plist` (if Xcode generated one)

In Xcode's Project Navigator, drag in every `.swift` file from
`iosApp/iosApp/` (it's easiest to drag the whole folder):

- `iOSApp.swift`, `ContentView.swift`
- `HomeView.swift`, `DetailView.swift`, `SuccessView.swift`,
  `ConfessionView.swift`, `RecordView.swift`,
  `CreateView.swift`, `CreateViewModel.swift`
- `ContactPicker.swift`, `MessageComposer.swift`,
  `InteropExtensions.swift`, `WitnessTheme.swift`
- `Info.plist` (in target settings → General → "Custom iOS Target
  Properties" or set `INFOPLIST_FILE` to point at the file)

When prompted, choose **Copy items if needed: off** (so they stay tracked in
the repo) and add to the `iosApp` target.

### 4. Add the XCFramework to the project

1. Select the `iosApp` project in the navigator → target `iosApp` → **General**.
2. Scroll to **Frameworks, Libraries, and Embedded Content**.
3. Click `+` → **Add Other** → **Add Files…**
4. Browse to
   `shared/build/XCFrameworks/debug/WitnessShared.xcframework` and add it.
5. In the same panel, set the framework's **Embed** column to
   **Embed & Sign**.

### 5. Tweak build settings

- **Deployment Target**: iOS 16.0 (the `Color(uiColor:)` initializer needs
  iOS 15+, `Task @MainActor` patterns are smoother on 16+).
- **Build Settings → Other Linker Flags**: add `-ObjC` (Kotlin/Native
  frameworks register Obj-C runtime classes that need it).
- **Bundle identifier**: `com.klt.witness` to match the Android app, or any
  reverse-DNS you control.
- **Linked frameworks**: `MessageUI.framework` (for SMS composer) and
  `ContactsUI.framework` (for the contact picker) — both are weakly linked
  by Xcode automatically when the corresponding Swift `import` appears, but
  if you see "Undefined symbols" link errors, add them explicitly via
  Project → Target → General → Frameworks, Libraries, and Embedded Content.

### 6. Run

`Product → Run` (Cmd-R) targeting an iOS Simulator. You should see the
WITNESS wordmark, an empty record card (Kept 0 / Broken 0), and the orange
**`＋ Make a promise`** button at the bottom. The empty state renders because
the SQLite DB starts empty on first launch — exactly like Android.

## What works in this shell

- **App container** (`IosAppContainer` in shared) wires SQLDelight driver
  → `WitnessDatabase` → `CommitmentRepository` + `HomeStore` +
  `CreateCommitmentStore`.
- **Home screen** in SwiftUI subscribes to the shared `HomeStore.state` via
  `IosFlowWatchers.shared.home(store:)` → `FlowWatcher<HomeUiState>` →
  `.watch { ... }`. Renders record card, active list, and empty state with
  the same tokens as Compose.
- **Create flow** — `＋ Make a promise` presents a full-screen cover with a
  `NavigationStack`-driven 5-step wizard:
  - **Goal**: SwiftUI `TextField` bound to the store's `goal`.
  - **Deadline**: native `DatePicker` with `.graphical` style; writes back
    as `Kotlinx_datetimeInstant` via `Date.kotlinInstant`.
  - **Witness**: `CNContactPickerViewController` (no contacts permission
    required — the system picker grants per-pick scoped access). Picks one
    phone number when a contact has multiple. Renders heads-up preview.
  - **Confession**: SwiftUI `TextEditor` with 280-char cap + character
    counter.
  - **Summary**: Recap card + ink confession bubble. **Lock it in** calls
    `IosCreateBridge.shared.commit(store:onResult:)`; on success the
    `MFMessageComposeViewController` is presented with the heads-up SMS to
    the chosen witness pre-filled. When the user finishes (send, cancel,
    or no-SMS device), the cover dismisses and the new commitment shows on
    Home. **No `SEND_SMS`-equivalent permission needed** — the user always
    taps Send themselves in the composer.
- **Live updates**: when the underlying SQLite changes (via shared writes),
  every watcher emits and the SwiftUI views re-render.

## What's stubbed

- **Local notifications** for deadline reminders. The Android side has
  `AlarmManager` + Notifications wired into the architecture diagram but
  not yet built; iOS will need `UNUserNotificationCenter` once that lands.

## Updating the shared code

Whenever you change Kotlin code in `:shared`, re-run:

```bash
./gradlew :shared:assembleWitnessSharedXCFramework
```

Xcode will pick up the rebuilt framework on the next build (Cmd-B / Cmd-R).
For a tighter dev loop, switch to the **CocoaPods integration** (out of
scope for this shell — uses the `kotlin.native.cocoapods` Gradle plugin)
or migrate to **Swift Package Manager** support (KMP 2.x adds this; needs a
small `Package.swift` generator config).

## Why no SKIE yet

[SKIE](https://skie.touchlab.co/) rewrites the generated framework to
expose Kotlin `Flow` as Swift `AsyncSequence` and Kotlin `suspend fun` as
Swift `async`. That's a nicer API than the `FlowWatcher` callbacks we use
today. We can add it later — it's a single Gradle plugin alias and a
configuration block, plus a version compatible with our current Kotlin
(`2.1.20`). Skipping for the first shell to avoid plugin-version churn.
