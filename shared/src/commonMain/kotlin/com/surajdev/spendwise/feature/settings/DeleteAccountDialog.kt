package com.surajdev.spendwise.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.surajdev.spendwise.core.designsystem.component.AppIcon
import com.surajdev.spendwise.core.designsystem.component.appTextFieldColors
import com.surajdev.spendwise.core.designsystem.icon.AppIcons
import com.surajdev.spendwise.core.designsystem.theme.Spacing
import com.surajdev.spendwise.core.domain.model.AuthProvider
import com.surajdev.spendwise.core.domain.model.AuthUser
import com.surajdev.spendwise.core.platform.SocialSignInLauncher

/** How the user proves it's them before the account is deleted. */
enum class ReauthMethod { PASSWORD, GOOGLE, APPLE, NONE }

fun reauthMethodFor(user: AuthUser, launcher: SocialSignInLauncher): ReauthMethod = when {
    AuthProvider.EMAIL in user.providers -> ReauthMethod.PASSWORD
    AuthProvider.GOOGLE in user.providers && launcher.supportsGoogle -> ReauthMethod.GOOGLE
    AuthProvider.APPLE in user.providers && launcher.supportsApple -> ReauthMethod.APPLE
    // e.g. an Apple account on Android. Firebase allows this only shortly after sign-in.
    else -> ReauthMethod.NONE
}

@Composable
fun DeleteAccountDialog(
    state: DeleteAccountState,
    method: ReauthMethod,
    onConfirm: (password: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!state.isBusy) onDismiss() },
        title = { Text("Delete account?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Text(
                    "This permanently deletes your account and all your expenses and categories, " +
                        "from the cloud and from this device. This can't be undone.",
                )
                when (method) {
                    ReauthMethod.PASSWORD -> OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { AppIcon(AppIcons.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        singleLine = true,
                        enabled = !state.isBusy,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        shape = MaterialTheme.shapes.small,
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ReauthMethod.GOOGLE -> Text("To confirm it's you, choose your Google account again.", style = MaterialTheme.typography.bodySmall)
                    ReauthMethod.APPLE -> Text("To confirm it's you, sign in with Apple again.", style = MaterialTheme.typography.bodySmall)
                    ReauthMethod.NONE -> Unit
                }
                state.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(password) }, enabled = !state.isBusy) {
                if (state.isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.error)
                } else {
                    val label = when (method) {
                        ReauthMethod.GOOGLE -> "Confirm with Google"
                        ReauthMethod.APPLE -> "Confirm with Apple"
                        else -> "Delete forever"
                    }
                    Text(label, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isBusy) { Text("Cancel") }
        },
    )
}

@Composable
fun AccountDeletedDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Account deleted") },
        text = { Text("Your account and all its data have been deleted. You can keep using SpendWise offline.") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
    )
}
