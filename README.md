# SpendWise

A calm, private expense tracker for Android and iOS, built with Kotlin Multiplatform and Compose Multiplatform.

- Log expenses in a few taps: amount, category, note, date/time, payment method.
- Daily report with category donut, hourly spending, top categories, largest expense, and comparisons with the previous day and your 30-day average.
- Works fully offline. Optional sign-in (Google, Apple on iPhone, email) backs up and syncs your data through Firebase.
- Optional end-of-day reminder (9:30 PM by default).
- Indian Rupee with Indian digit grouping (₹1,23,456).
- Two looks: **Aurora** (light: aurora mesh background, frosted-glass cards, gradient hero cards, floating glass navigation) and **Quiet Luxury** (dark: warm charcoal, champagne and brass).

---

## Contents

1. [Tech stack](#tech-stack)
2. [Project structure](#project-structure)
3. [Run on Android (Windows)](#run-on-android-windows)
4. [Firebase setup (optional)](#firebase-setup-optional)
5. [Run on iOS (on a Mac)](#run-on-ios-on-a-mac)
6. [About Sign in with Apple](#about-sign-in-with-apple)
7. [Tests](#tests)
8. [How sync works](#how-sync-works)
9. [Database migrations](#database-migrations)
10. [Release checklist](#release-checklist)
11. [Splitting into modules later](#splitting-into-modules-later)
12. [Phase 2: automatic transactions](#phase-2-automatic-transactions)
13. [Troubleshooting](#troubleshooting)

---

## Tech stack

| Area | Library | Version |
|---|---|---|
| Language / build | Kotlin Multiplatform, AGP, Gradle | Kotlin 2.4.21, AGP 9.4.1, Gradle 9.6 |
| UI | Compose Multiplatform (Material 3) | 1.12.2 (material3 1.9.0) |
| Database | Room 3 (KMP) + bundled SQLite | 3.0.3 |
| Settings | DataStore Preferences (KMP) | 1.3.0 |
| DI | Koin | 4.2.2 |
| Navigation / ViewModel | JetBrains builds of AndroidX Navigation and Lifecycle | 2.10.0 / 2.11.0 |
| Backend | Firebase Auth + Cloud Firestore via [GitLive firebase-kotlin-sdk](https://github.com/GitLiveApp/firebase-kotlin-sdk) | 2.7.0 |
| Background work (Android) | WorkManager | 2.12.0 |
| Charts | Custom Compose Canvas (donut, bar) | — |
| Glass blur | [Haze](https://github.com/chrisbanes/haze) (`haze`, `haze-blur`), Android + iOS | 2.0.1 |
| Dates | kotlinx-datetime + `kotlin.time` | 0.8.0 |

Notes:
- **Firebase has no official Kotlin Multiplatform SDK.** GitLive wraps the official Android and iOS SDKs. All Firebase code is behind two interfaces (`AuthRepository`, `SyncRemoteDataSource`), so it can be replaced without touching the rest of the app.
- **Ktor is not used** yet: Firebase does its own networking. It will come in with Phase 2 if bank APIs or a parsing service are added.
- **Fonts:** Fraunces and Inter (SIL Open Font License, see `third_party_licenses/`). **Icons:** Material Symbols Rounded (Apache 2.0).

---

## Project structure

Two Gradle modules plus the iOS app:

```
SpendWise/
├── androidApp/            Android entry point: Application, MainActivity, manifest, launcher icons
├── shared/                All shared code (Kotlin Multiplatform library)
│   ├── src/commonMain/    Code for every platform (almost everything lives here)
│   ├── src/mobileMain/    Android + iOS only: Firebase (GitLive)
│   ├── src/androidMain/   Android-only: Room location, WorkManager reminders/sync, Google sign-in
│   ├── src/iosMain/       iOS-only: Room location, local notifications, Swift sign-in bridge
│   ├── src/jvmMain/       JVM target used only to run tests quickly on Windows
│   ├── src/commonTest/    Unit tests (domain, use cases, conflict resolution)
│   ├── src/jvmTest/       Database and sync tests (real Room, in memory)
│   └── schemas/           Room schema history (commit these)
├── iosApp/                iOS app shell (SwiftUI host, sign-in bridge), generated with XcodeGen
└── firebase/              Firestore security rules and emulator config
```

Inside `shared/src/commonMain/kotlin/com/surajdev/spendwise/`:

```
app/                 App() root, navigation, DI modules, startup
core/
  common/            Outcome type, ID generator, time zone provider
  domain/            Models, repository interfaces, use cases, daily report calculator (pure Kotlin)
  data/              Repository implementations, preferences, sync engine, conflict resolver
  database/          Room entities, DAOs, database
  remote/            Remote data source contract (Firebase implementation is in mobileMain)
  designsystem/      Quiet Luxury theme, components, charts, icons
  platform/          expect/actual UI hooks (sign-in sheets, notification permission, system bars)
feature/
  home/  addexpense/  expenselist/  report/  categories/  settings/  auth/  common/
```

**Rules that keep it clean:**
- `core/domain` never imports Room, Firebase, Compose or platform APIs.
- Features depend on `core/domain` and `core/designsystem`, not on each other (`feature/common` holds shared UI bits).
- Screens follow one pattern: a `ViewModel` exposes `state: StateFlow<UiState>` and `onIntent(Intent)`; one-off events go through an `effects` channel; the `XScreen(state, onIntent)` composable is stateless, and `XRoute` connects it to the ViewModel.

---

## Run on Android (Windows)

Requirements: Android Studio (Otter 3 feature drop or newer, for AGP 9) with its bundled JDK.

1. Open the `SpendWise` folder in Android Studio and let Gradle sync.
2. Select the **androidApp** run configuration and a device/emulator, then **Run**.

Or from a terminal:

```bash
./gradlew :androidApp:installDebug
```

Without Firebase configured, the app runs in **offline mode** (Settings shows "Offline mode"). Everything except sign-in and sync works.

---

## Firebase setup (optional)

Do this once when you want sign-in and sync.

### 1. Create the project
1. Go to the [Firebase console](https://console.firebase.google.com), **Add project** (Analytics is not needed).
2. **Build → Authentication → Get started**, then enable these sign-in providers:
   - **Email/Password**
   - **Google**
   - **Apple** (for iOS; see [About Sign in with Apple](#about-sign-in-with-apple))
3. **Build → Firestore Database → Create database**. Choose a location near your users (e.g. `asia-south1`, Mumbai). Start in **production mode**.
4. Publish the security rules: copy `firebase/firestore.rules` into **Firestore → Rules → Publish**. Or, with the Firebase CLI: `cd firebase && firebase deploy --only firestore:rules`.

### 2. Android
1. In **Project settings → Your apps**, add an **Android app** with package name `com.surajdev.spendwise`.
2. Add your **SHA-1 fingerprints** (Google sign-in won't work without them):
   ```bash
   ./gradlew :androidApp:signingReport
   ```
   Add the debug SHA-1 now. Add your release key's SHA-1, and later the **Play App Signing** SHA-1 (Play Console → Test and release → App integrity).
3. Download `google-services.json` and place it at `androidApp/google-services.json`.
4. Rebuild. The Google Services plugin is applied automatically when this file exists, and Settings will show **Sign in**.

### 3. iOS
1. In **Project settings**, add an **iOS app** with bundle ID `com.surajdev.spendwise`.
2. Download `GoogleService-Info.plist` and place it at `iosApp/iosApp/GoogleService-Info.plist`.
3. Open that plist, copy the `REVERSED_CLIENT_ID` value into `iosApp/Configuration/Config.xcconfig` as `GOOGLE_REVERSED_CLIENT_ID`.

Both config files are git-ignored. They aren't secret (security comes from the Firestore rules), but it's tidier to keep them out of public repos.

---

## Run on iOS (on a Mac)

iOS apps can only be built on macOS. The shared code is written for both platforms; the Mac is needed to build and run.

**One-time setup on the Mac:**
1. Install **Xcode** (from the App Store) and open it once to install components.
2. Install a JDK 17+ (e.g. `brew install --cask temurin`) and **XcodeGen**: `brew install xcodegen`.
3. Copy or clone the project to the Mac.
4. Put your Apple Developer **Team ID** in `iosApp/Configuration/Config.xcconfig` (`TEAM_ID = ABCDE12345`). For the simulator you can leave it empty.

**Build and run:**
```bash
cd iosApp
xcodegen generate        # creates iosApp.xcodeproj from project.yml
open iosApp.xcodeproj
```
In Xcode, pick an iPhone simulator and press **Run**. The first build takes a while: Xcode downloads Firebase and GoogleSignIn through Swift Package Manager, and a build step runs Gradle to compile the Kotlin framework (`./gradlew :shared:embedAndSignAppleFrameworkForXcode`).

Run `xcodegen generate` again whenever `project.yml` changes or you add files to `iosApp/iosApp/`, such as `GoogleService-Info.plist`.

**Quick check without Xcode UI** (compiles the shared Kotlin code for iOS and runs the shared tests on the simulator):
```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 :shared:iosSimulatorArm64Test
```

**If you hit an iOS error,** copy the full error (from Xcode's Report navigator, or the Gradle output) and send it over.

---

## About Sign in with Apple

**The rule:** Apple's App Review Guideline **4.8 (Login Services)** says that an app using a third-party or social login, such as **Sign in with Google**, must also offer an equivalent privacy-focused login option. In practice that means **Sign in with Apple**. Apps that only use their own account system (just email/password) are exempt, but SpendWise offers Google sign-in, so the rule applies.

**What goes wrong without it:** App Review **rejects the build** (a common rejection reason), and you have to add it and resubmit, which costs days.

**What SpendWise does:** Sign in with Apple is already built in on iOS (`iosApp/iosApp/NativeSignIn.swift`) and shown first, as Apple's guidelines prefer. It isn't shown on Android, where it isn't required.

**What you need to do before testing it:**
1. A paid **Apple Developer Program** membership (USD 99/year; you need it to publish anyway).
2. In [developer.apple.com](https://developer.apple.com/account) → **Identifiers**, open the App ID `com.surajdev.spendwise` and enable **Sign in with Apple**. The entitlement file is already in the project.
3. In Firebase **Authentication → Sign-in method → Apple**, enable the provider. For native iOS sign-in, no Services ID or private key is required.

**Things to know:**
- Apple lets users **hide their email**; Firebase then receives a relay address like `abc123@privaterelay.appleid.com`. That's fine: SpendWise identifies users by Firebase UID, not by email.
- Apple sends the user's name **only on the very first sign-in**. SpendWise doesn't depend on the name.
- Sign in with Apple doesn't work in the iOS Simulator unless you're signed in to an Apple ID in the simulator's Settings. Test on a real iPhone if it fails.
- Apple requires apps with account creation to also let users **delete their account** in-app (Guideline 5.1.1(v)). Add a "Delete account" option before the first App Store submission (see the checklist below).

---

## Tests

```bash
./gradlew :shared:jvmTest           # all shared tests, on Windows/macOS/Linux (~30 s)
./gradlew :shared:iosSimulatorArm64Test   # same common tests on the iOS simulator (Mac only)
```

What's covered:
- **Money**: parsing user input, Indian digit grouping, compact labels.
- **Daily report**: totals, category shares, hourly buckets, time zones, 30-day average (zero-spend days counted), previous-day comparison, 7-day strip.
- **Use cases**: expense and category validation, edits keep identity, grouping by day, live updates.
- **Room**: search (note and category name), time windows, soft delete and restore, default category seeding.
- **Sync**: push/pull, new device pulls everything, first sign-in uploads local data, last edit wins on both devices, deletions propagate, failed pushes retry, accounts stay separate, tombstone cleanup.
- **Conflict resolution**: two devices always choose the same winner.
- **DI graph**: every ViewModel can be created, which catches Koin wiring mistakes before they crash on a device.

The daily report uses a 30-day average that counts zero-spend days, but only from the day of your first expense. That way a new user's average isn't diluted by days before they installed the app.

---

## How sync works

- **Room is the source of truth.** The UI only reads the local database, so everything works offline and feels instant.
- Every change is stamped (`updatedAt`, device ID) and marked **pending**. Deletes are soft (tombstones) so they can sync.
- **When it syncs:** about 2 seconds after a change, right after sign-in, when the app comes to the foreground, every 15 minutes while open, and every 6 hours in the background on Android (WorkManager, when online).
- **Push, then pull.** Pull uses a server-timestamp cursor, so differences between device clocks can't cause missed changes.
- **Conflicts:** last write wins per record; on an exact tie a delete beats an edit, then the device ID decides. Every device reaches the same result.
- **Firestore layout:** `users/{uid}/expenses/{id}` and `users/{uid}/categories/{id}`. Rules allow each user to access only their own documents.
- **Signing in for the first time** uploads everything already on the device. **Signing out** offers to keep or remove the local copy.

---

## Database migrations

The schema is exported to `shared/schemas/` on every build. **Commit these files.**

When you change an entity:
1. Bump `SpendWiseDatabase.VERSION`.
2. For simple changes (new nullable column, new table), add an auto-migration:
   `@Database(..., autoMigrations = [AutoMigration(from = 1, to = 2)])`.
3. For anything else, write a `Migration(1, 2)` and add it to `SpendWiseDatabase.MIGRATIONS`.
4. Build once so `schemas/.../2.json` is generated, and commit it.

Never edit an old schema JSON, and never ship a version bump without a migration: users would lose data.

---

## Release checklist

### Before either store
- [ ] Set up Firebase (above) and test sign-in and sync on two devices.
- [ ] Add an in-app **Delete account** option (required by Apple 5.1.1(v); Google Play also requires an account-deletion path plus a web link).
- [ ] Write a **privacy policy** (what's stored: expenses, categories, email; where: Firebase; how to delete) and host it at a public URL.
- [ ] Bump the version: Android `versionCode` + `versionName` in `androidApp/build.gradle.kts`; iOS `MARKETING_VERSION` + `CURRENT_PROJECT_VERSION` in `iosApp/project.yml`.
- [ ] Replace the default launcher icon with the final app icon.

### Google Play
- [ ] Create an upload key (once) and **back it up**:
  ```bash
  keytool -genkeypair -v -keystore spendwise-upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
  ```
- [ ] Create `keystore.properties` in the project root (git-ignored):
  ```properties
  storeFile=spendwise-upload.jks
  storePassword=...
  keyAlias=upload
  keyPassword=...
  ```
- [ ] Build the bundle: `./gradlew :androidApp:bundleRelease` → `androidApp/build/outputs/bundle/release/androidApp-release.aab`.
- [ ] In Play Console: create the app, enable **Play App Signing**, upload the `.aab` to **Internal testing** first.
- [ ] Add the Play App Signing **SHA-1** to Firebase (otherwise Google sign-in fails in the Play build).
- [ ] Complete **Data safety** (collects: email; financial info entered by the user; encrypted in transit; user can request deletion), **Content rating**, **Target audience**, and the store listing (screenshots, short and full description).
- [ ] New personal developer accounts must run a **closed test with at least 12 testers for 14 days** before applying for production.

### Apple App Store
- [ ] Apple Developer Program membership; App ID with **Sign in with Apple** enabled.
- [ ] In App Store Connect, create the app (bundle ID `com.surajdev.spendwise`).
- [ ] In Xcode: select **Any iOS Device**, then **Product → Archive** → **Distribute App → App Store Connect**.
- [ ] Test through **TestFlight** first.
- [ ] Fill in **App Privacy** (data linked to the user: email, financial info; not used for tracking), age rating, screenshots (6.9" and 6.5" iPhone), description, support URL and privacy policy URL.
- [ ] Provide a **demo account** (email/password) in the review notes so the reviewer can see sync features.
- [ ] The export-compliance question is pre-answered (`ITSAppUsesNonExemptEncryption = NO`).

---

## Splitting into modules later

Everything lives in one `shared` module today, organized so it can be split mechanically when the app grows (for faster builds or stricter boundaries):

1. Create a new KMP library module, e.g. `:core:domain`, with the same targets (copy `shared/build.gradle.kts`, keep only the dependencies it needs).
2. Move the package folder (`core/domain/`) into it. Package names stay the same, so imports don't change.
3. Add `implementation(project(":core:domain"))` to the modules that use it.
4. Split from the inside out: `core:domain` → `core:database` → `core:data` → `core:designsystem` → one module per `feature/*`. Keep `shared` as the "umbrella" that holds `app/` (navigation, DI) and exports the iOS framework.

This works because features only talk to each other through navigation routes, and `domain` doesn't depend on anything else.

---

## Phase 2: automatic transactions

Not implemented yet. The groundwork is in place:
- Each expense has `source` (MANUAL, SMS, NOTIFICATION, EMAIL, BANK_API, IMPORT) and `externalRef` (e.g. a UPI reference) for de-duplication, so no schema change is needed.
- New sources should implement a `TransactionSource → parser → categorizer → de-duplicator` pipeline that ends in `ExpenseRepository.save`. The rest of the app doesn't change.

**Store policy reality check:**
- **Google Play** only allows SMS-reading permissions for apps that are the default SMS app (or a short list of exceptions that doesn't include expense trackers). It also forbids getting SMS data through workarounds.
- **iOS** offers no way to read SMS or other apps' notifications.
- Realistic options are notification reading from UPI/banking apps (Android, policy-sensitive), email parsing, and India's **Account Aggregator** framework, which is regulated and needs registration.

---

## Troubleshooting

| Problem | Fix |
|---|---|
| Settings says "Offline mode" | `google-services.json` (Android) or `GoogleService-Info.plist` (iOS) is missing; see Firebase setup. |
| Google sign-in: "No Google account found" / error 10 | Add your SHA-1 to Firebase and download a fresh `google-services.json`. |
| Google sign-in on iOS opens but never returns | `GOOGLE_REVERSED_CLIENT_ID` in `Config.xcconfig` doesn't match the plist; run `xcodegen generate` again. |
| Xcode: "No such module 'Shared'" | Build once (⌘B); the Gradle build phase creates the framework. Check that `JAVA_HOME` points to JDK 17+. |
| Xcode: Firebase linker errors | Firebase iOS must be 11.x for GitLive 2.7.0 (set in `project.yml`). In Xcode use **File → Packages → Reset Package Caches**. |
| Gradle: "iOS targets are disabled" on Windows | Expected. iOS compiles only on macOS. |
| No reminder notification | Allow notifications for SpendWise. On Android the reminder can arrive a few minutes late (battery saving). |
