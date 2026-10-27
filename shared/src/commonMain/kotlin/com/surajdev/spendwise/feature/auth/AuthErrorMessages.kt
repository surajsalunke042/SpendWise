package com.surajdev.spendwise.feature.auth

import com.surajdev.spendwise.core.domain.repository.AuthError

internal const val MIN_PASSWORD_LENGTH = 8

/** User-facing text for an [AuthError]. Empty for [AuthError.Cancelled], which needs no message. */
internal fun AuthError.userMessage(): String = when (this) {
    AuthError.InvalidCredentials -> "That email and password don't match."
    AuthError.EmailAlreadyInUse -> "An account with this email already exists. Try signing in."
    AuthError.WeakPassword -> "Choose a stronger password (at least $MIN_PASSWORD_LENGTH characters)."
    AuthError.InvalidEmail -> "Enter a valid email address."
    AuthError.Network -> "No internet connection. Your data is safe on this device; try again when you're online."
    AuthError.Cancelled -> ""
    AuthError.Unavailable -> "Sign-in isn't available in this build."
    AuthError.RequiresRecentLogin -> "For your security, please sign out, sign in again, and then retry."
    is AuthError.Unknown -> message ?: "Something went wrong. Please try again."
}
