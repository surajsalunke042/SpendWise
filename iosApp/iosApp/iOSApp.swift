import FirebaseCore
import GoogleSignIn
import Shared
import SwiftUI

@main
struct iOSApp: App {
    init() {
        // Firebase is optional: without GoogleService-Info.plist the app runs offline-only.
        if Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil {
            FirebaseApp.configure()
        }
        IosEntryKt.startSpendWise(nativeSignIn: NativeSignIn())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    _ = GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}

/// Hosts the shared Compose Multiplatform UI.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        IosEntryKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
