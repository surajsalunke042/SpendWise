package com.surajdev.spendwise.core.domain.model

/**
 * A spending category.
 *
 * @property iconKey a stable key (e.g. "restaurant") the UI maps to an icon, so it syncs across platforms.
 * @property colorArgb the category color as 0xAARRGGBB.
 * @property isArchived archived categories are hidden from pickers but keep existing expenses valid.
 */
data class Category(
    val id: String,
    val name: String,
    val iconKey: String,
    val colorArgb: Long,
    val isDefault: Boolean,
    val sortOrder: Int,
    val isArchived: Boolean = false,
)

/**
 * Built-in categories. Their IDs are fixed so that two devices seeding defaults
 * never create duplicates after syncing.
 */
object DefaultCategories {
    const val OTHER_ID = "cat_other"

    val all: List<Category> = listOf(
        Category("cat_food", "Food", "restaurant", 0xFFB07D4F, isDefault = true, sortOrder = 0),
        Category("cat_transport", "Transport", "directions_car", 0xFF56707F, isDefault = true, sortOrder = 1),
        Category("cat_shopping", "Shopping", "shopping_bag", 0xFF8B6C8F, isDefault = true, sortOrder = 2),
        Category("cat_bills", "Bills", "receipt_long", 0xFF6F7D4E, isDefault = true, sortOrder = 3),
        Category("cat_entertainment", "Entertainment", "movie", 0xFFB85C45, isDefault = true, sortOrder = 4),
        Category("cat_health", "Health", "medical_services", 0xFF4E8073, isDefault = true, sortOrder = 5),
        Category(OTHER_ID, "Other", "more_horiz", 0xFF8C857B, isDefault = true, sortOrder = 6),
    )
}
