package com.surajdev.spendwise.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.surajdev.spendwise.core.common.Outcome
import com.surajdev.spendwise.core.designsystem.theme.CategoryPalette
import com.surajdev.spendwise.core.domain.model.Category
import com.surajdev.spendwise.core.domain.model.DefaultCategories
import com.surajdev.spendwise.core.domain.repository.CategoryRepository
import com.surajdev.spendwise.core.domain.usecase.CategoryDraft
import com.surajdev.spendwise.core.domain.usecase.CategoryValidationError
import com.surajdev.spendwise.core.domain.usecase.SaveCategoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The category being created or edited in the editor sheet. */
data class CategoryEditor(
    val id: String? = null,
    val name: String = "",
    val iconKey: String = "restaurant",
    val colorArgb: Long = CategoryPalette.first(),
    val error: String? = null,
    val canArchive: Boolean = false,
    val isArchived: Boolean = false,
)

data class CategoriesUiState(
    val active: List<Category> = emptyList(),
    val archived: List<Category> = emptyList(),
    val editor: CategoryEditor? = null,
)

sealed interface CategoriesIntent {
    data object New : CategoriesIntent
    data class Edit(val category: Category) : CategoriesIntent
    data class NameChanged(val name: String) : CategoriesIntent
    data class IconSelected(val key: String) : CategoriesIntent
    data class ColorSelected(val argb: Long) : CategoriesIntent
    data object Save : CategoriesIntent
    data object DismissEditor : CategoriesIntent
    data object ToggleArchive : CategoriesIntent
    data class Restore(val id: String) : CategoriesIntent
}

class CategoriesViewModel(
    private val categories: CategoryRepository,
    private val saveCategory: SaveCategoryUseCase,
) : ViewModel() {

    private val editor = MutableStateFlow<CategoryEditor?>(null)

    val state: StateFlow<CategoriesUiState> = combine(
        categories.observeCategories(includeArchived = true),
        editor,
    ) { all, currentEditor ->
        CategoriesUiState(
            active = all.filterNot { it.isArchived },
            archived = all.filter { it.isArchived },
            editor = currentEditor,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    fun onIntent(intent: CategoriesIntent) {
        when (intent) {
            CategoriesIntent.New -> {
                val usedColors = state.value.active.map { it.colorArgb }.toSet()
                editor.value = CategoryEditor(colorArgb = CategoryPalette.firstOrNull { it !in usedColors } ?: CategoryPalette.first())
            }
            is CategoriesIntent.Edit -> editor.value = CategoryEditor(
                id = intent.category.id,
                name = intent.category.name,
                iconKey = intent.category.iconKey,
                colorArgb = intent.category.colorArgb,
                // "Other" is the fallback for expenses whose category is missing, so it always stays.
                canArchive = intent.category.id != DefaultCategories.OTHER_ID,
                isArchived = intent.category.isArchived,
            )
            is CategoriesIntent.NameChanged -> editor.update { it?.copy(name = intent.name, error = null) }
            is CategoriesIntent.IconSelected -> editor.update { it?.copy(iconKey = intent.key) }
            is CategoriesIntent.ColorSelected -> editor.update { it?.copy(colorArgb = intent.argb) }
            CategoriesIntent.DismissEditor -> editor.value = null
            CategoriesIntent.Save -> save()
            CategoriesIntent.ToggleArchive -> {
                val current = editor.value ?: return
                val id = current.id ?: return
                viewModelScope.launch {
                    categories.setArchived(id, !current.isArchived)
                    editor.value = null
                }
            }
            is CategoriesIntent.Restore -> viewModelScope.launch { categories.setArchived(intent.id, false) }
        }
    }

    private fun save() {
        val current = editor.value ?: return
        viewModelScope.launch {
            val result = saveCategory(CategoryDraft(current.id, current.name, current.iconKey, current.colorArgb))
            when (result) {
                is Outcome.Success -> editor.value = null
                is Outcome.Failure -> editor.update {
                    it?.copy(
                        error = when (result.error) {
                            CategoryValidationError.NAME_MISSING -> "Give the category a name"
                            CategoryValidationError.NAME_TOO_LONG -> "Keep it under ${SaveCategoryUseCase.MAX_NAME_LENGTH} characters"
                            CategoryValidationError.NAME_TAKEN -> "You already have a category with this name"
                            CategoryValidationError.NOT_FOUND -> "This category no longer exists"
                        },
                    )
                }
            }
        }
    }
}
