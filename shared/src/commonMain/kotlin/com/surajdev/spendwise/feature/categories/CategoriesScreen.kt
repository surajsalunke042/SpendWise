package com.surajdev.spendwise.feature.categories

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import com.surajdev.spendwise.core.designsystem.component.appTextFieldColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.surajdev.spendwise.core.designsystem.component.AppIcon
import com.surajdev.spendwise.core.designsystem.component.CategoryBadge
import com.surajdev.spendwise.core.designsystem.component.SurfaceCard
import com.surajdev.spendwise.core.designsystem.component.HairlineDivider
import com.surajdev.spendwise.core.designsystem.component.PrimaryButton
import com.surajdev.spendwise.core.designsystem.component.SectionLabel
import com.surajdev.spendwise.core.designsystem.icon.AppIcons
import com.surajdev.spendwise.core.designsystem.theme.CategoryPalette
import com.surajdev.spendwise.core.designsystem.theme.SpendWiseTheme
import com.surajdev.spendwise.core.designsystem.theme.Spacing
import com.surajdev.spendwise.core.designsystem.theme.categoryColor
import com.surajdev.spendwise.core.domain.model.Category
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CategoriesRoute(onBack: () -> Unit, viewModel: CategoriesViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CategoriesScreen(state, viewModel::onIntent, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(state: CategoriesUiState, onIntent: (CategoriesIntent) -> Unit, onBack: () -> Unit) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Categories", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { AppIcon(AppIcons.Back, contentDescription = "Back") } },
                actions = {
                    TextButton(onClick = { onIntent(CategoriesIntent.New) }) {
                        AppIcon(AppIcons.Add, contentDescription = null, size = 18.dp, tint = MaterialTheme.colorScheme.primary)
                        Text(" New", color = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                SurfaceCard(contentPadding = 0.dp, modifier = Modifier.fillMaxWidth()) {
                    state.active.forEachIndexed { index, category ->
                        CategoryListRow(category, onClick = { onIntent(CategoriesIntent.Edit(category)) })
                        if (index < state.active.lastIndex) HairlineDivider(Modifier.padding(start = 68.dp))
                    }
                }
            }
            if (state.archived.isNotEmpty()) {
                item { SectionLabel("Archived", Modifier.padding(top = Spacing.md)) }
                item {
                    Text(
                        "Archived categories are hidden when adding expenses. Past expenses keep them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(state.archived, key = { it.id }) { category ->
                    SurfaceCard(contentPadding = 0.dp, modifier = Modifier.fillMaxWidth()) {
                        CategoryListRow(category, onClick = { onIntent(CategoriesIntent.Edit(category)) }) {
                            TextButton(onClick = { onIntent(CategoriesIntent.Restore(category.id)) }) { Text("Restore") }
                        }
                    }
                }
            }
        }
    }

    state.editor?.let { editor ->
        CategoryEditorSheet(editor, onIntent)
    }
}

@Composable
private fun CategoryListRow(category: Category, onClick: () -> Unit, trailing: @Composable () -> Unit = {
    AppIcon(AppIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
}) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(category.iconKey, category.colorArgb)
        Spacer(Modifier.size(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(category.name, style = MaterialTheme.typography.bodyLarge)
            if (category.isDefault) {
                Text("Built-in", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditorSheet(editor: CategoryEditor, onIntent: (CategoriesIntent) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { onIntent(CategoriesIntent.DismissEditor) },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen)
                .navigationBarsPadding(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryBadge(editor.iconKey, editor.colorArgb, size = 48.dp)
                Spacer(Modifier.size(Spacing.md))
                Text(
                    if (editor.id == null) "New category" else "Edit category",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            Spacer(Modifier.height(Spacing.lg))
            OutlinedTextField(
                value = editor.name,
                onValueChange = { onIntent(CategoriesIntent.NameChanged(it)) },
                label = { Text("Name") },
                singleLine = true,
                isError = editor.error != null,
                supportingText = editor.error?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                shape = MaterialTheme.shapes.small,
                colors = appTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.md))
            SectionLabel("Color")
            Spacer(Modifier.height(Spacing.sm))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                CategoryPalette.forEach { argb ->
                    ColorSwatch(argb, selected = argb == editor.colorArgb) { onIntent(CategoriesIntent.ColorSelected(argb)) }
                }
            }
            Spacer(Modifier.height(Spacing.lg))
            SectionLabel("Icon")
            Spacer(Modifier.height(Spacing.sm))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                AppIcons.categoryIcons.keys.forEach { key ->
                    IconOption(key, editor.colorArgb, selected = key == editor.iconKey) { onIntent(CategoriesIntent.IconSelected(key)) }
                }
            }
            Spacer(Modifier.height(Spacing.xl))
            PrimaryButton(
                text = if (editor.id == null) "Create category" else "Save",
                onClick = { onIntent(CategoriesIntent.Save) },
                modifier = Modifier.fillMaxWidth(),
            )
            if (editor.canArchive) {
                TextButton(
                    onClick = { onIntent(CategoriesIntent.ToggleArchive) },
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                ) {
                    Text(
                        if (editor.isArchived) "Restore category" else "Archive category",
                        color = if (editor.isArchived) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun ColorSwatch(argb: Long, selected: Boolean, onClick: () -> Unit) {
    val color = categoryColor(argb, SpendWiseTheme.extended.isDark)
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .border(BorderStroke(if (selected) 2.dp else 0.dp, if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent), CircleShape)
            .padding(4.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected; contentDescription = "Color" },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) AppIcon(AppIcons.Check, contentDescription = null, tint = Color.White, size = 18.dp)
    }
}

@Composable
private fun IconOption(key: String, colorArgb: Long, selected: Boolean, onClick: () -> Unit) {
    val color = categoryColor(colorArgb, SpendWiseTheme.extended.isDark)
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) color.copy(alpha = 0.16f) else Color.Transparent)
            .border(1.dp, if (selected) color else SpendWiseTheme.extended.hairline, MaterialTheme.shapes.small)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected; contentDescription = key.replace('_', ' ') },
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(AppIcons.forKey(key), contentDescription = null, tint = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant, size = 22.dp)
    }
}
