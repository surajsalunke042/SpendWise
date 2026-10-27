# SpendWise: development guide

Setup, build, release and troubleshooting notes for working on SpendWise. For an overview of the app, see the [README](../README.md).

## Contents

1. [Run on Android](#run-on-android)
2. [Firebase setup](#firebase-setup)
3. [Run on iOS (on a Mac)](#run-on-ios-on-a-mac)
4. [About Sign in with Apple](#about-sign-in-with-apple)
5. [Tests](#tests)
6. [Database migrations](#database-migrations)
7. [App icon and store graphics](#app-icon-and-store-graphics)
8. [Releasing](#releasing)
9. [Splitting into modules later](#splitting-into-modules-later)
10. [Phase 2: automatic transactions](#phase-2-automatic-transactions)
11. [Troubleshooting](#troubleshooting)

---

## Run on Android
Requirements: Android Studio (Otter 3 feature drop or newer, for AGP 9) with its bundled JDK.

1. Open the `SpendWise` folder in Android Studio and let Gradle sync.
2. Select the **androidApp** run configuration and a device/emulator, then **Run**.

Or from a terminal:

```bash
./gradlew :androidApp:installDebug
```

Without Firebase configured, the app runs in **offline mode** (Settings shows "Offline mode"). Everything except sign-in and sync works.

---

## Firebase setup

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
- Apple requires apps with account creation to also let users **delete their account** in-app (Guideline 5.1.1(v)). SpendWise has this in Settings → Account & sync.

---

## Tests

```bash
./gradlew :shared:jvmTest           # all shared tests, on Windows/macOS/Linux (~30 s)
./gradlew :shared:iosSimulatorArm64Test   # same common tests on the iOS simulator (Mac only)
```

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

## App icon and store graphics

Every icon is generated from one geometry by `tools/app-icon/AppIconGenerator.java`: the Android adaptive icon (with a monochrome layer for themed icons), legacy launcher PNGs, the iOS 1024 px icon, the Play Store 512 px icon and the 1024×500 Play feature graphic.

To change the design, edit the constants at the top of the generator and run, from the project root (JDK 17+):

```bash
java tools/app-icon/AppIconGenerator.java
```

---

## Releasing

### Versioning
Before every store upload, bump the version: Android `versionCode` (must always increase) and `versionName` in `androidApp/build.gradle.kts`; iOS `MARKETING_VERSION` and `CURRENT_PROJECT_VERSION` in `iosApp/project.yml`.

### Google Play
1. Release signing reads `keystore.properties` from the project root (git-ignored):
   ```properties
   storeFile=C:/path/to/your-upload-key.jks
   storePassword=...
   keyAlias=...
   keyPassword=...
   ```
   Keep the upload keystore outside the project and back it up.
2. Build the bundle: `./gradlew :androidApp:bundleRelease` → `androidApp/build/outputs/bundle/release/androidApp-release.aab`.
3. Firebase needs every signing fingerprint for Google sign-in: debug, upload key (`./gradlew :androidApp:signingReport`) and the **Play App Signing** key (Play Console → Test and release → App integrity).
4. Security rules live in `firebase/firestore.rules`. Publish changes with `firebase deploy --only firestore:rules` or paste them into the console.
5. The privacy policy and account-deletion pages are in `docs/` (published with GitHub Pages); their URLs are in `core/common/AppLinks.kt`.

### Apple App Store
- Apple Developer Program membership; App ID with **Sign in with Apple** enabled.
- In Xcode: **Any iOS Device** → **Product → Archive** → **Distribute App → App Store Connect**, then test through **TestFlight**.
- App Privacy: data linked to the user is email and user-entered financial info, not used for tracking. Provide a demo account in the review notes.
- The export-compliance question is pre-answered (`ITSAppUsesNonExemptEncryption = NO`).

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
| Settings says "Offline mode" | `google-services.json` (Android) or `GoogleService-Info.plist` (iOS) is missing; see [Firebase setup](#firebase-setup). |
| Google sign-in: "No Google account found" / error 10 | Add your SHA-1 to Firebase and download a fresh `google-services.json`. |
| Google sign-in on iOS opens but never returns | `GOOGLE_REVERSED_CLIENT_ID` in `Config.xcconfig` doesn't match the plist; run `xcodegen generate` again. |
| Xcode: "No such module 'Shared'" | Build once (⌘B); the Gradle build phase creates the framework. Check that `JAVA_HOME` points to JDK 17+. |
| Xcode: Firebase linker errors | Firebase iOS must be 11.x for GitLive 2.7.0 (set in `project.yml`). In Xcode use **File → Packages → Reset Package Caches**. |
| Gradle: "iOS targets are disabled" on Windows | Expected. iOS compiles only on macOS. |
| No reminder notification | Allow notifications for SpendWise. On Android the reminder can arrive a few minutes late (battery saving). |
