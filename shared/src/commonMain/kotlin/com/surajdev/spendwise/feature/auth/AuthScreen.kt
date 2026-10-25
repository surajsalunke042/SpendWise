package com.surajdev.spendwise.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import com.surajdev.spendwise.core.designsystem.component.appTextFieldColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.surajdev.spendwise.core.designsystem.component.AppIcon
import com.surajdev.spendwise.core.designsystem.component.HairlineDivider
import com.surajdev.spendwise.core.designsystem.component.PrimaryButton
import com.surajdev.spendwise.core.designsystem.component.SecondaryButton
import com.surajdev.spendwise.core.designsystem.icon.AppIcons
import com.surajdev.spendwise.core.designsystem.theme.SpendWiseTheme
import com.surajdev.spendwise.core.designsystem.theme.Spacing
import com.surajdev.spendwise.core.platform.SocialSignInLauncher
import com.surajdev.spendwise.core.platform.rememberSocialSignInLauncher
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthRoute(onDone: () -> Unit, viewModel: AuthViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val launcher = rememberSocialSignInLauncher()
    val scope = rememberCoroutineScope()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AuthEffect.SignedIn -> onDone()
            }
        }
    }
    AuthScreen(
        state = state,
        launcher = launcher,
        onIntent = viewModel::onIntent,
        onGoogle = {
            viewModel.onIntent(AuthIntent.SocialStarted)
            scope.launch { viewModel.onIntent(AuthIntent.SocialResult(launcher.signInWithGoogle())) }
        },
        onApple = {
            viewModel.onIntent(AuthIntent.SocialStarted)
            scope.launch { viewModel.onIntent(AuthIntent.SocialResult(launcher.signInWithApple())) }
        },
        onBack = onDone,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    state: AuthUiState,
    launcher: SocialSignInLauncher,
    onIntent: (AuthIntent) -> Unit,
    onGoogle: () -> Unit,
    onApple: () -> Unit,
    onBack: () -> Unit,
) {
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val signUp = state.mode == AuthMode.SIGN_UP
    val fieldColors = appTextFieldColors()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = onBack) { AppIcon(AppIcons.Back, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(if (signUp) "Create your account" else "Welcome back", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Sign in to back up your expenses and sync them across your devices. " +
                    "You can keep using SpendWise without an account.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.sm))

            if (launcher.supportsApple) {
                // Apple's guidelines ask for the Apple option to be at least as prominent as others.
                PrimaryButton("Continue with Apple", onClick = onApple, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth())
            }
            if (launcher.supportsGoogle) {
                SecondaryButton("Continue with Google", onClick = onGoogle, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth())
            }
            if (launcher.supportsApple || launcher.supportsGoogle) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = Spacing.sm)) {
                    HairlineDivider(Modifier.weight(1f))
                    Text(
                        "  or use email  ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HairlineDivider(Modifier.weight(1f))
                }
            }

            OutlinedTextField(
                value = state.email,
                onValueChange = { onIntent(AuthIntent.EmailChanged(it)) },
                label = { Text("Email") },
                leadingIcon = { AppIcon(AppIcons.Mail, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                shape = MaterialTheme.shapes.small,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = { onIntent(AuthIntent.PasswordChanged(it)) },
                label = { Text("Password") },
                leadingIcon = { AppIcon(AppIcons.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        AppIcon(
                            if (showPassword) AppIcons.VisibilityOff else AppIcons.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onIntent(AuthIntent.Submit) }),
                supportingText = if (signUp) ({ Text("At least 8 characters") }) else null,
                shape = MaterialTheme.shapes.small,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )

            state.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            state.info?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = SpendWiseTheme.extended.success) }

            PrimaryButton(
                text = if (signUp) "Create account" else "Sign in",
                onClick = { onIntent(AuthIntent.Submit) },
                loading = state.isBusy,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { onIntent(AuthIntent.ToggleMode) }) {
                    Text(if (signUp) "I already have an account" else "Create an account")
                }
                if (!signUp) {
                    TextButton(onClick = { onIntent(AuthIntent.ForgotPassword) }) { Text("Forgot password") }
                }
            }
            Spacer(Modifier.height(Spacing.xl))
        }
    }
}
