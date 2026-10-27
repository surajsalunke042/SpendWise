package com.surajdev.spendwise.app.di

import androidx.room3.RoomDatabase
import com.surajdev.spendwise.app.AppViewModel
import com.surajdev.spendwise.core.common.IdGenerator
import com.surajdev.spendwise.core.common.SystemTimeZoneProvider
import com.surajdev.spendwise.core.common.TimeZoneProvider
import com.surajdev.spendwise.core.common.UuidIdGenerator
import com.surajdev.spendwise.core.data.local.AppPreferences
import com.surajdev.spendwise.core.data.local.createPreferencesDataStore
import com.surajdev.spendwise.core.data.repository.CategoryRepositoryImpl
import com.surajdev.spendwise.core.data.repository.ExpenseRepositoryImpl
import com.surajdev.spendwise.core.data.repository.SettingsRepositoryImpl
import com.surajdev.spendwise.core.data.sync.SyncEngine
import com.surajdev.spendwise.core.data.sync.SyncRepositoryImpl
import com.surajdev.spendwise.core.database.SpendWiseDatabase
import com.surajdev.spendwise.core.database.buildSpendWiseDatabase
import com.surajdev.spendwise.core.domain.repository.CategoryRepository
import com.surajdev.spendwise.core.domain.repository.ExpenseRepository
import com.surajdev.spendwise.core.domain.repository.SettingsRepository
import com.surajdev.spendwise.core.domain.repository.SyncRepository
import com.surajdev.spendwise.core.domain.usecase.ObserveDailyReportUseCase
import com.surajdev.spendwise.core.domain.usecase.ObserveDayTotalUseCase
import com.surajdev.spendwise.core.domain.usecase.ObserveExpenseSectionsUseCase
import com.surajdev.spendwise.core.domain.usecase.SaveCategoryUseCase
import com.surajdev.spendwise.core.domain.usecase.SaveExpenseUseCase
import com.surajdev.spendwise.feature.addexpense.AddExpenseViewModel
import com.surajdev.spendwise.feature.auth.AuthViewModel
import com.surajdev.spendwise.feature.categories.CategoriesViewModel
import com.surajdev.spendwise.feature.expenselist.ExpenseListViewModel
import com.surajdev.spendwise.feature.home.HomeViewModel
import com.surajdev.spendwise.feature.report.ReportViewModel
import com.surajdev.spendwise.feature.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.time.Clock

/** Where each platform keeps its files. */
class PlatformPaths(val preferencesFile: String)

/** Long-lived scope for app-wide background work such as sync. */
class AppScope : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Default)

/**
 * Provided by each platform:
 * `RoomDatabase.Builder<SpendWiseDatabase>`, [PlatformPaths], `ReminderScheduler`,
 * `BackgroundSyncScheduler`, `AuthRepository` and `SyncRemoteDataSource`.
 */
expect val platformModule: Module

val dataModule = module {
    single<Clock> { Clock.System }
    single<IdGenerator> { UuidIdGenerator() }
    single<TimeZoneProvider> { SystemTimeZoneProvider() }
    single { AppScope() }

    single { get<RoomDatabase.Builder<SpendWiseDatabase>>().buildSpendWiseDatabase() }
    single { AppPreferences(createPreferencesDataStore(get<PlatformPaths>().preferencesFile), get()) }

    single<ExpenseRepository> { ExpenseRepositoryImpl(get<SpendWiseDatabase>().expenseDao(), get(), get()) }
    single<CategoryRepository> { CategoryRepositoryImpl(get<SpendWiseDatabase>().categoryDao(), get(), get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get(), get()) }

    single { SyncEngine(get(), get(), get(), get()) }
    single { SyncRepositoryImpl(get(), get(), get(), get(), get(), get(), get(), get<AppScope>()) } bind SyncRepository::class
}

val domainModule = module {
    factory { SaveExpenseUseCase(get(), get(), get(), get(), get(), get()) }
    factory { SaveCategoryUseCase(get(), get()) }
    factory { ObserveExpenseSectionsUseCase(get(), get(), get()) }
    factory { ObserveDailyReportUseCase(get(), get(), get()) }
    factory { ObserveDayTotalUseCase(get(), get()) }
}

val viewModelModule = module {
    viewModelOf(::AppViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::AddExpenseViewModel)
    viewModelOf(::ExpenseListViewModel)
    viewModelOf(::ReportViewModel)
    viewModelOf(::CategoriesViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::AuthViewModel)
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication = startKoin {
    appDeclaration()
    modules(platformModule, dataModule, domainModule, viewModelModule)
}
