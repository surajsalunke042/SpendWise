import AuthenticationServices
import CryptoKit
import FirebaseCore
import GoogleSignIn
import Shared
import UIKit

/// Platform sign-in sheets for the shared Kotlin code (see NativeSignInProvider in IosPlatform.kt).
/// Results go back to Kotlin, which signs in to Firebase with the returned tokens.
final class NativeSignIn: NSObject, NativeSignInProvider {

    var supportsGoogle: Bool { FirebaseApp.app()?.options.clientID != nil }

    // MARK: Google

    func signInWithGoogle(completion: @escaping (NativeSignInResult) -> Void) {
        guard let clientID = FirebaseApp.app()?.options.clientID else {
            completion(.failure("Google sign-in isn't configured."))
            return
        }
        guard let presenter = Self.topViewController() else {
            completion(.failure("Couldn't open Google sign-in."))
            return
        }
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
        GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { result, error in
            if let error = error as NSError? {
                if error.code == GIDSignInError.canceled.rawValue {
                    completion(.cancelled())
                } else {
                    completion(.failure(error.localizedDescription))
                }
                return
            }
            guard let user = result?.user, let idToken = user.idToken?.tokenString else {
                completion(.failure("Google didn't return an ID token."))
                return
            }
            completion(NativeSignInResult(
                idToken: idToken,
                secondaryToken: user.accessToken.tokenString,
                errorMessage: nil,
                cancelled: false
            ))
        }
    }

    // MARK: Apple

    private var currentNonce: String?
    private var appleCompletion: ((NativeSignInResult) -> Void)?

    func signInWithApple(completion: @escaping (NativeSignInResult) -> Void) {
        let nonce = Self.randomNonce()
        currentNonce = nonce
        appleCompletion = completion

        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.fullName, .email]
        request.nonce = Self.sha256(nonce)

        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self
        controller.performRequests()
    }

    private func finishApple(_ result: NativeSignInResult) {
        appleCompletion?(result)
        appleCompletion = nil
        currentNonce = nil
    }

    // MARK: Helpers

    private static func topViewController() -> UIViewController? {
        let scene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }
        var top = scene?.windows.first { $0.isKeyWindow }?.rootViewController
        while let presented = top?.presentedViewController {
            top = presented
        }
        return top
    }

    private static func randomNonce(length: Int = 32) -> String {
        let charset = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
        var bytes = [UInt8](repeating: 0, count: length)
        let status = SecRandomCopyBytes(kSecRandomDefault, length, &bytes)
        precondition(status == errSecSuccess, "Unable to generate nonce")
        return String(bytes.map { charset[Int($0) % charset.count] })
    }

    private static func sha256(_ input: String) -> String {
        SHA256.hash(data: Data(input.utf8)).map { String(format: "%02x", $0) }.joined()
    }
}

extension NativeSignIn: ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding {

    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        Self.topViewController()?.view.window ?? ASPresentationAnchor()
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        guard
            let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
            let tokenData = credential.identityToken,
            let idToken = String(data: tokenData, encoding: .utf8),
            let nonce = currentNonce
        else {
            finishApple(.failure("Apple didn't return a valid token."))
            return
        }
        finishApple(NativeSignInResult(idToken: idToken, secondaryToken: nonce, errorMessage: nil, cancelled: false))
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        if (error as? ASAuthorizationError)?.code == .canceled {
            finishApple(.cancelled())
        } else {
            finishApple(.failure(error.localizedDescription))
        }
    }
}

extension NativeSignInResult {
    static func failure(_ message: String) -> NativeSignInResult {
        NativeSignInResult(idToken: nil, secondaryToken: nil, errorMessage: message, cancelled: false)
    }

    static func cancelled() -> NativeSignInResult {
        NativeSignInResult(idToken: nil, secondaryToken: nil, errorMessage: nil, cancelled: true)
    }
}
