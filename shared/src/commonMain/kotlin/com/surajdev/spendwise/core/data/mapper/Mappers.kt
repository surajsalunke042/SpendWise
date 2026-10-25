package com.surajdev.spendwise.core.data.mapper

import com.surajdev.spendwise.core.database.entity.CategoryEntity
import com.surajdev.spendwise.core.database.entity.ExpenseEntity
import com.surajdev.spendwise.core.database.entity.SyncState
import com.surajdev.spendwise.core.domain.model.Category
import com.surajdev.spendwise.core.domain.model.Currency
import com.surajdev.spendwise.core.domain.model.Expense
import com.surajdev.spendwise.core.domain.model.ExpenseSource
import com.surajdev.spendwise.core.domain.model.Money
import com.surajdev.spendwise.core.domain.model.PaymentMethod
import com.surajdev.spendwise.core.remote.CategoryRecord
import com.surajdev.spendwise.core.remote.ExpenseRecord
import kotlin.time.Instant

fun ExpenseEntity.toDomain(): Expense = Expense(
    id = id,
    amount = Money(amountMinor, Currency.fromCode(currency)),
    categoryId = categoryId,
    note = note,
    occurredAt = Instant.fromEpochMilliseconds(occurredAt),
    timeZoneId = timeZoneId,
    paymentMethod = PaymentMethod.fromNameOrNull(paymentMethod),
    source = ExpenseSource.fromName(source),
    externalRef = externalRef,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
)

fun Expense.toEntity(deviceId: String): ExpenseEntity = ExpenseEntity(
    id = id,
    amountMinor = amount.minorUnits,
    currency = amount.currency.code,
    categoryId = categoryId,
    note = note,
    occurredAt = occurredAt.toEpochMilliseconds(),
    timeZoneId = timeZoneId,
    paymentMethod = paymentMethod?.name,
    source = source.name,
    externalRef = externalRef,
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt.toEpochMilliseconds(),
    deletedAt = null,
    syncState = SyncState.PENDING,
    lastModifiedBy = deviceId,
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    iconKey = iconKey,
    colorArgb = colorArgb,
    isDefault = isDefault,
    sortOrder = sortOrder,
    isArchived = archived,
)

fun Category.toEntity(createdAt: Long, updatedAt: Long, syncState: Int, deviceId: String): CategoryEntity =
    CategoryEntity(
        id = id,
        name = name,
        iconKey = iconKey,
        colorArgb = colorArgb,
        isDefault = isDefault,
        sortOrder = sortOrder,
        archived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = null,
        syncState = syncState,
        lastModifiedBy = deviceId,
    )

fun ExpenseEntity.toRecord() = ExpenseRecord(
    id = id,
    amountMinor = amountMinor,
    currency = currency,
    categoryId = categoryId,
    note = note,
    occurredAt = occurredAt,
    timeZoneId = timeZoneId,
    paymentMethod = paymentMethod,
    source = source,
    externalRef = externalRef,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    lastModifiedBy = lastModifiedBy,
)

fun ExpenseRecord.toEntity(syncState: Int = SyncState.SYNCED) = ExpenseEntity(
    id = id,
    amountMinor = amountMinor,
    currency = currency,
    categoryId = categoryId,
    note = note,
    occurredAt = occurredAt,
    timeZoneId = timeZoneId,
    paymentMethod = paymentMethod,
    source = source,
    externalRef = externalRef,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncState = syncState,
    lastModifiedBy = lastModifiedBy,
)

fun CategoryEntity.toRecord() = CategoryRecord(
    id = id,
    name = name,
    iconKey = iconKey,
    colorArgb = colorArgb,
    isDefault = isDefault,
    sortOrder = sortOrder,
    archived = archived,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    lastModifiedBy = lastModifiedBy,
)

fun CategoryRecord.toEntity(syncState: Int = SyncState.SYNCED) = CategoryEntity(
    id = id,
    name = name,
    iconKey = iconKey,
    colorArgb = colorArgb,
    isDefault = isDefault,
    sortOrder = sortOrder,
    archived = archived,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncState = syncState,
    lastModifiedBy = lastModifiedBy,
)
