package com.surajdev.spendwise.feature.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.surajdev.spendwise.core.designsystem.component.AmountText
import com.surajdev.spendwise.core.designsystem.component.CategoryBadge
import com.surajdev.spendwise.core.designsystem.theme.Spacing
import com.surajdev.spendwise.core.domain.model.ExpenseItem
import com.surajdev.spendwise.core.domain.model.MoneyFormatter

/** One expense: category badge, note (or category), details and amount. */
@Composable
fun ExpenseRow(
    item: ExpenseItem,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val expense = item.expense
    val title = expense.note.ifBlank { item.category.name }
    val details = buildList {
        if (expense.note.isNotBlank()) add(item.category.name)
        add(Formats.time(expense.localDateTime.time))
        expense.paymentMethod?.let { add(it.label) }
    }.joinToString(" · ")
    val description = "$title, ${MoneyFormatter.format(expense.amount)}, $details"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        CategoryBadge(item.category.iconKey, item.category.colorArgb)
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        AmountText(expense.amount, style = MaterialTheme.typography.titleMedium)
    }
}
