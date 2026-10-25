package com.surajdev.spendwise.core.remote.firebase

import com.surajdev.spendwise.core.domain.model.AuthProvider
import com.surajdev.spendwise.core.domain.model.AuthUser
import com.surajdev.spendwise.core.domain.repository.AuthError
import com.surajdev.spendwise.core.domain.repository.AuthRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseNetworkException
import dev.gitlive.firebase.FirebaseTooManyRequestsException
import dev.gitlive.firebase.auth.FirebaseAuthEmailException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidUserException
import dev.gitlive.firebase.auth.FirebaseAuthUserCollisionException
import dev.gitlive.firebase.auth.FirebaseAuthWeakPasswordException
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.OAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirebaseAuthRepository : AuthRepository {

    private val auth by lazy { Firebase.auth }

    override val isAvailable: Boolean = true

    override val currentUser: Flow<AuthUser?> get() = auth.authStateChanged.map { it?.toAuthUser() }

    override fun currentUserId(): String? = auth.currentUser?.uid

    override suspend fun signInWithEmail(email: String, password: String) = attempt {
        auth.signInWithEmailAndPassword(email.trim(), password)
    }

    override suspend fun signUpWithEmail(email: String, password: String) = attempt {
        auth.createUserWithEmailAndPassword(email.trim(), password)
    }

    override suspend fun sendPasswordReset(email: String) = attempt {
        auth.sendPasswordResetEmail(email.trim())
    }

    override suspend fun signInWithGoogle(idToken: String, accessToken: String?) = attempt {
        auth.signInWithCredential(GoogleAuthProvider.credential(idToken, accessToken))
    }

    override suspend fun signInWithApple(idToken: String, rawNonce: String) = attempt {
        auth.signInWithCredential(OAuthProvider.credential(providerId = "apple.com", idToken = idToken, rawNonce = rawNonce))
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    /** Runs a Firebase call and maps failures to [AuthError]; returns null on success. */
    private suspend fun attempt(block: suspend () -> Unit): AuthError? = try {
        block()
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: FirebaseAuthWeakPasswordException) {
        AuthError.WeakPassword
    } catch (e: FirebaseAuthUserCollisionException) {
        AuthError.EmailAlreadyInUse
    } catch (e: FirebaseAuthEmailException) {
        AuthError.InvalidEmail
    } catch (e: FirebaseAuthInvalidUserException) {
        AuthError.InvalidCredentials
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        if (e.message.orEmpty().contains("email", ignoreCase = true) &&
            e.message.orEmpty().contains("badly formatted", ignoreCase = true)
        ) AuthError.InvalidEmail else AuthError.InvalidCredentials
    } catch (e: FirebaseNetworkException) {
        AuthError.Network
    } catch (e: FirebaseTooManyRequestsException) {
        AuthError.Unknown("Too many attempts. Please wait a moment and try again.")
    } catch (e: Exception) {
        AuthError.Unknown(e.message)
    }

    private fun FirebaseUser.toAuthUser() = AuthUser(
        uid = uid,
        email = email,
        displayName = displayName,
        providers = providerData.map { info ->
            when (info.providerId) {
                "google.com" -> AuthProvider.GOOGLE
                "apple.com" -> AuthProvider.APPLE
                "password" -> AuthProvider.EMAIL
                else -> AuthProvider.OTHER
            }
        }.toSet(),
    )
}
