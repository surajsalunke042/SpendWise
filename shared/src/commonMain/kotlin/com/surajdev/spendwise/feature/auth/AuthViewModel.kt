package com.surajdev.spendwise.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.surajdev.spendwise.core.domain.repository.AuthError
import com.surajdev.spendwise.core.domain.repository.AuthRepository
import com.surajdev.spendwise.core.platform.SocialSignInResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode { SIGN_IN, SIGN_UP }

data class AuthUiState(
    val mode: AuthMode = AuthMode.SIGN_IN,
    val email: String = "",
    val password: String = "",
    val isBusy: Boolean = false,
    val error: String? = null,
    val info: String? = null,
)

sealed interface AuthIntent {
    data class EmailChanged(val value: String) : AuthIntent
    data class PasswordChanged(val value: String) : AuthIntent
    data object ToggleMode : AuthIntent
    data object Submit : AuthIntent
    data object ForgotPassword : AuthIntent
    /** Result from the platform sign-in sheet (Google or Apple). */
    data class SocialResult(val result: SocialSignInResult) : AuthIntent
    data object SocialStarted : AuthIntent
}

sealed interface AuthEffect {
    data object SignedIn : AuthEffect
}

class AuthViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val _effects = Channel<AuthEffect>(Channel.BUFFERED)
    val effects: Flow<AuthEffect> = _effects.receiveAsFlow()

    fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.EmailChanged -> _state.update { it.copy(email = intent.value, error = null, info = null) }
            is AuthIntent.PasswordChanged -> _state.update { it.copy(password = intent.value, error = null) }
            AuthIntent.ToggleMode -> _state.update {
                it.copy(mode = if (it.mode == AuthMode.SIGN_IN) AuthMode.SIGN_UP else AuthMode.SIGN_IN, error = null, info = null)
            }
            AuthIntent.Submit -> submit()
            AuthIntent.ForgotPassword -> forgotPassword()
            AuthIntent.SocialStarted -> _state.update { it.copy(isBusy = true, error = null, info = null) }
            is AuthIntent.SocialResult -> handleSocial(intent.result)
        }
    }

    private fun submit() {
        val current = _state.value
        val email = current.email.trim()
        when {
            !email.contains('@') || !email.contains('.') -> return _state.update { it.copy(error = "Enter a valid email address") }
            current.mode == AuthMode.SIGN_UP && current.password.length < MIN_PASSWORD ->
                return _state.update { it.copy(error = "Use at least $MIN_PASSWORD characters for your password") }
            current.password.isEmpty() -> return _state.update { it.copy(error = "Enter your password") }
        }
        attempt {
            if (current.mode == AuthMode.SIGN_IN) auth.signInWithEmail(email, current.password)
            else auth.signUpWithEmail(email, current.password)
        }
    }

    private fun forgotPassword() {
        val email = _state.value.email.trim()
        if (!email.contains('@')) {
            _state.update { it.copy(error = "Enter your email above, then tap \"Forgot password\" again") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, error = null) }
            val error = auth.sendPasswordReset(email)
            _state.update {
                if (error == null) it.copy(isBusy = false, info = "We've emailed you a link to reset your password.")
                else it.copy(isBusy = false, error = error.message())
            }
        }
    }

    private fun handleSocial(result: SocialSignInResult) {
        when (result) {
            is SocialSignInResult.Google -> attempt { auth.signInWithGoogle(result.idToken, result.accessToken) }
            is SocialSignInResult.Apple -> attempt { auth.signInWithApple(result.idToken, result.rawNonce) }
            SocialSignInResult.Cancelled -> _state.update { it.copy(isBusy = false) }
            is SocialSignInResult.Failed -> _state.update { it.copy(isBusy = false, error = result.message) }
        }
    }

    private fun attempt(block: suspend () -> AuthError?) {
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, error = null, info = null) }
            val error = block()
            if (error == null) {
                _effects.send(AuthEffect.SignedIn)
            } else {
                _state.update { it.copy(isBusy = false, error = if (error == AuthError.Cancelled) null else error.message()) }
            }
        }
    }

    private fun AuthError.message(): String = when (this) {
        AuthError.InvalidCredentials -> "That email and password don't match."
        AuthError.EmailAlreadyInUse -> "An account with this email already exists. Try signing in."
        AuthError.WeakPassword -> "Choose a stronger password (at least $MIN_PASSWORD characters)."
        AuthError.InvalidEmail -> "Enter a valid email address."
        AuthError.Network -> "No internet connection. Your data is safe on this device; try again when you're online."
        AuthError.Cancelled -> ""
        AuthError.Unavailable -> "Sign-in isn't available in this build."
        is AuthError.Unknown -> message ?: "Something went wrong. Please try again."
    }

    private companion object {
        const val MIN_PASSWORD = 8
    }
}
