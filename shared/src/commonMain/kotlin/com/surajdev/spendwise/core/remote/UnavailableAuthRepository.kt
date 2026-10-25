package com.surajdev.spendwise.core.remote

import com.surajdev.spendwise.core.domain.model.AuthUser
import com.surajdev.spendwise.core.domain.repository.AuthError
import com.surajdev.spendwise.core.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Used when Firebase isn't configured: the app works fully offline and hides account features. */
class UnavailableAuthRepository : AuthRepository {
    override val isAvailable: Boolean = false
    override val currentUser: Flow<AuthUser?> = flowOf(null)
    override fun currentUserId(): String? = null
    override suspend fun signInWithEmail(email: String, password: String) = AuthError.Unavailable
    override suspend fun signUpWithEmail(email: String, password: String) = AuthError.Unavailable
    override suspend fun sendPasswordReset(email: String) = AuthError.Unavailable
    override suspend fun signInWithGoogle(idToken: String, accessToken: String?) = AuthError.Unavailable
    override suspend fun signInWithApple(idToken: String, rawNonce: String) = AuthError.Unavailable
    override suspend fun signOut() = Unit
}
