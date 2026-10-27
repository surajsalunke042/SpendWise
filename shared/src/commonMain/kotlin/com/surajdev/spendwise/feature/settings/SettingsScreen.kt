package com.surajdev.spendwise.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.surajdev.spendwise.core.common.AppLinks
import com.surajdev.spendwise.core.designsystem.component.AppIcon
import com.surajdev.spendwise.core.designsystem.component.LocalBottomBarClearance
import com.surajdev.spendwise.core.designsystem.component.SurfaceCard
import com.surajdev.spendwise.core.designsystem.component.HairlineDivider
import com.surajdev.spendwise.core.designsystem.component.PrimaryButton
import com.surajdev.spendwise.core.designsystem.component.SecondaryButton
import com.surajdev.spendwise.core.designsystem.component.SectionLabel
import com.surajdev.spendwise.core.designsystem.icon.AppIcons
import com.surajdev.spendwise.core.designsystem.theme.SpendWiseTheme
import com.surajdev.spendwise.core.designsystem.theme.Spacing
import com.surajdev.spendwise.core.domain.model.SyncStatus
import com.surajdev.spendwise.core.domain.model.ThemeMode
import com.surajdev.spendwise.core.platform.platformName
import com.surajdev.spendwise.core.platform.rememberNotificationPermissionRequester
import com.surajdev.spendwise.core.platform.rememberSocialSignInLauncher
import com.surajdev.spendwise.feature.common.Formats
import com.surajdev.spendwise.feature.common.TimePickerSheet
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.DrawableResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsRoute(
    onOpenCategories: () -> Unit,
    onSignIn: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    val requestPermission = rememberNotificationPermissionRequester { granted ->
        permissionDenied = !granted
        if (granted) viewModel.onIntent(SettingsIntent.ReminderToggled(true))
    }
    val launcher = rememberSocialSignInLauncher()
    val scope = rememberCoroutineScope()
    val reauthMethod = state.user?.let { reauthMethodFor(it, launcher) } ?: ReauthMethod.NONE
    SettingsScreen(
        state = state,
        permissionDenied = permissionDenied,
        reauthMethod = reauthMethod,
        onIntent = viewModel::onIntent,
        onEnableReminder = requestPermission,
        onOpenCategories = onOpenCategories,
        onSignIn = onSignIn,
        onConfirmDelete = { password ->
            when (reauthMethod) {
                ReauthMethod.PASSWORD -> viewModel.onIntent(SettingsIntent.DeleteAccountWithPassword(password))
                ReauthMethod.GOOGLE, ReauthMethod.APPLE -> {
                    viewModel.onIntent(SettingsIntent.DeleteAccountSocialStarted)
                    scope.launch {
                        val result = if (reauthMethod == ReauthMethod.GOOGLE) launcher.signInWithGoogle() else launcher.signInWithApple()
                        viewModel.onIntent(SettingsIntent.DeleteAccountWithSocial(result))
                    }
                }
                ReauthMethod.NONE -> viewModel.onIntent(SettingsIntent.DeleteAccountWithoutReauth)
            }
        },
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    permissionDenied: Boolean,
    reauthMethod: ReauthMethod,
    onIntent: (SettingsIntent) -> Unit,
    onEnableReminder: () -> Unit,
    onOpenCategories: () -> Unit,
    onSignIn: () -> Unit,
    onConfirmDelete: (password: String) -> Unit,
) {
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.screen, end = Spacing.screen, top = Spacing.lg, bottom = LocalBottomBarClearance.current + Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)

            SectionLabel("Account & sync", Modifier.padding(top = Spacing.md))
            AccountCard(
                state,
                onSignIn = onSignIn,
                onSyncNow = { onIntent(SettingsIntent.SyncNow) },
                onSignOut = { confirmSignOut = true },
                onDeleteAccount = { confirmDelete = true },
            )

            SectionLabel("Preferences", Modifier.padding(top = Spacing.md))
            SurfaceCard(modifier = Modifier.fillMaxWidth(), contentPadding = 0.dp) {
                SettingRow(
                    icon = AppIcons.Notifications,
                    title = "Daily reminder",
                    subtitle = if (state.settings.reminderEnabled) {
                        "Every day at ${Formats.time(state.settings.reminderTime)}"
                    } else if (permissionDenied) {
                        "Notifications are blocked. Allow them in system settings."
                    } else {
                        "Off"
                    },
                    trailing = {
                        Switch(
                            checked = state.settings.reminderEnabled,
                            onCheckedChange = { on -> if (on) onEnableReminder() else onIntent(SettingsIntent.ReminderToggled(false)) },
                            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
                        )
                    },
                )
                if (state.settings.reminderEnabled) {
                    HairlineDivider(Modifier.padding(start = 56.dp))
                    SettingRow(
                        icon = AppIcons.Time,
                        title = "Reminder time",
                        subtitle = Formats.time(state.settings.reminderTime),
                        onClick = { showTimePicker = true },
                    )
                }
                HairlineDivider(Modifier.padding(start = 56.dp))
                SettingRow(
                    icon = AppIcons.Category,
                    title = "Categories",
                    subtitle = "Add, edit or archive categories",
                    onClick = onOpenCategories,
                    trailing = { AppIcon(AppIcons.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                )
                HairlineDivider(Modifier.padding(start = 56.dp))
                Column(Modifier.padding(Spacing.lg)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(AppIcons.Contrast, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(Spacing.lg))
                        Text("Appearance", style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(Modifier.height(Spacing.md))
                    ThemeSelector(state.settings.themeMode) { onIntent(SettingsIntent.ThemeChanged(it)) }
                }
            }

            SectionLabel("About", Modifier.padding(top = Spacing.md))
            SurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Text("SpendWise", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Your data is stored on this device first and works fully offline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "Running on $platformName. Fonts: Fraunces and Inter (SIL Open Font License). " +
                        "Icons: Material Symbols (Apache 2.0).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = { uriHandler.openUri(AppLinks.PRIVACY_POLICY) },
                    contentPadding = PaddingValues(0.dp),
                ) { Text("Privacy policy") }
            }
        }
    }

    if (showTimePicker) {
        TimePickerSheet(
            initial = state.settings.reminderTime,
            onDismiss = { showTimePicker = false },
            onConfirm = {
                onIntent(SettingsIntent.ReminderTimeChanged(it))
                showTimePicker = false
            },
        )
    }
    if (state.deleteAccount.completed) {
        AccountDeletedDialog(onDismiss = {
            confirmDelete = false
            onIntent(SettingsIntent.DeleteAccountDismissed)
        })
    } else if (confirmDelete && state.user != null) {
        DeleteAccountDialog(
            state = state.deleteAccount,
            method = reauthMethod,
            onConfirm = onConfirmDelete,
            onDismiss = {
                confirmDelete = false
                onIntent(SettingsIntent.DeleteAccountDismissed)
            },
        )
    }
    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = {
                Text(
                    "Your expenses are safely stored in your account. You can keep a copy on this device, " +
                        "or remove it so nobody else using this phone can see it.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    onIntent(SettingsIntent.SignOut(keepLocalData = false))
                }) { Text("Remove from device", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    onIntent(SettingsIntent.SignOut(keepLocalData = true))
                }) { Text("Keep on device") }
            },
        )
    }
}

@Composable
private fun AccountCard(
    state: SettingsUiState,
    onSignIn: () -> Unit,
    onSyncNow: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
) {
    SurfaceCard(modifier = Modifier.fillMaxWidth()) {
        when {
            !state.authAvailable -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(AppIcons.CloudOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(Spacing.md))
                    Column {
                        Text("Offline mode", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Cloud sync isn't set up in this build. Everything stays on this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            state.user == null -> {
                Text("Back up and sync", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Sign in to keep your expenses safe and see them on all your devices. It's optional.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.md))
                PrimaryButton("Sign in", onClick = onSignIn, modifier = Modifier.fillMaxWidth(), icon = AppIcons.Person)
            }
            else -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(AppIcons.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(Spacing.md))
                    Column(Modifier.weight(1f)) {
                        Text(state.user.displayName ?: state.user.email ?: "Signed in", style = MaterialTheme.typography.titleSmall)
                        state.user.email?.takeIf { state.user.displayName != null }?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.md))
                SyncLine(state.syncStatus)
                Spacer(Modifier.height(Spacing.md))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    SecondaryButton("Sync now", onClick = onSyncNow, icon = AppIcons.Sync, modifier = Modifier.weight(1f), enabled = state.syncStatus != SyncStatus.Syncing)
                    SecondaryButton("Sign out", onClick = onSignOut, icon = AppIcons.Logout, modifier = Modifier.weight(1f))
                }
                TextButton(onClick = onDeleteAccount, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Delete account", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun SyncLine(status: SyncStatus) {
    val extended = SpendWiseTheme.extended
    val (icon, text, color) = when (status) {
        SyncStatus.Syncing -> Triple(AppIcons.Sync, "Syncing…", MaterialTheme.colorScheme.onSurfaceVariant)
        is SyncStatus.Failed -> Triple(AppIcons.CloudOff, "Couldn't sync right now. We'll retry automatically.", MaterialTheme.colorScheme.error)
        is SyncStatus.Idle -> when {
            status.pendingChanges > 0 -> Triple(AppIcons.Sync, "${status.pendingChanges} changes waiting to sync", MaterialTheme.colorScheme.onSurfaceVariant)
            status.lastSyncedAt != null -> {
                val local = status.lastSyncedAt.toLocalDateTime(TimeZone.currentSystemDefault())
                Triple(AppIcons.CloudDone, "All changes synced · ${Formats.time(local.time)}", extended.success)
            }
            else -> Triple(AppIcons.CloudDone, "Ready to sync", MaterialTheme.colorScheme.onSurfaceVariant)
        }
        else -> return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppIcon(icon, null, tint = color, size = 18.dp)
        Spacer(Modifier.width(Spacing.sm))
        Text(text, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@Composable
private fun SettingRow(
    icon: DrawableResource,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(Spacing.lg))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

@Composable
private fun ThemeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark")
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (mode, label) ->
            SegmentedButton(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    inactiveContainerColor = MaterialTheme.colorScheme.surface,
                    activeBorderColor = MaterialTheme.colorScheme.outline,
                    inactiveBorderColor = SpendWiseTheme.extended.hairline,
                ),
                label = { Text(label) },
            )
        }
    }
}
