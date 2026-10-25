package com.surajdev.spendwise.core.domain.usecase

import com.surajdev.spendwise.core.common.IdGenerator
import com.surajdev.spendwise.core.common.Outcome
import com.surajdev.spendwise.core.domain.model.Category
import com.surajdev.spendwise.core.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.first

data class CategoryDraft(
    val id: String? = null,
    val name: String,
    val iconKey: String,
    val colorArgb: Long,
)

enum class CategoryValidationError { NAME_MISSING, NAME_TOO_LONG, NAME_TAKEN, NOT_FOUND }

class SaveCategoryUseCase(
    private val categories: CategoryRepository,
    private val idGenerator: IdGenerator,
) {
    suspend operator fun invoke(draft: CategoryDraft): Outcome<Category, CategoryValidationError> {
        val name = draft.name.trim()
        if (name.isEmpty()) return Outcome.Failure(CategoryValidationError.NAME_MISSING)
        if (name.length > MAX_NAME_LENGTH) return Outcome.Failure(CategoryValidationError.NAME_TOO_LONG)

        val all = categories.observeCategories(includeArchived = true).first()
        val clash = all.any { it.id != draft.id && !it.isArchived && it.name.equals(name, ignoreCase = true) }
        if (clash) return Outcome.Failure(CategoryValidationError.NAME_TAKEN)

        val existing = draft.id?.let { id -> all.firstOrNull { it.id == id } ?: return Outcome.Failure(CategoryValidationError.NOT_FOUND) }
        val category = existing?.copy(name = name, iconKey = draft.iconKey, colorArgb = draft.colorArgb)
            ?: Category(
                id = idGenerator.newId(),
                name = name,
                iconKey = draft.iconKey,
                colorArgb = draft.colorArgb,
                isDefault = false,
                sortOrder = (all.maxOfOrNull { it.sortOrder } ?: 0) + 1,
            )
        categories.save(category)
        return Outcome.Success(category)
    }

    companion object {
        const val MAX_NAME_LENGTH = 30
    }
}
