package com.surajdev.spendwise.app

import androidx.room3.RoomDatabase
import androidx.room3.Room
import com.surajdev.spendwise.app.di.PlatformPaths
import com.surajdev.spendwise.app.di.dataModule
import com.surajdev.spendwise.app.di.domainModule
import com.surajdev.spendwise.app.di.platformModule
import com.surajdev.spendwise.app.di.viewModelModule
import com.surajdev.spendwise.core.database.SpendWiseDatabase
import com.surajdev.spendwise.core.domain.repository.SyncRepository
import com.surajdev.spendwise.feature.addexpense.AddExpenseArgs
import com.surajdev.spendwise.feature.addexpense.AddExpenseViewModel
import com.surajdev.spendwise.feature.auth.AuthViewModel
import com.surajdev.spendwise.feature.categories.CategoriesViewModel
import com.surajdev.spendwise.feature.expenselist.ExpenseListViewModel
import com.surajdev.spendwise.feature.home.HomeViewModel
import com.surajdev.spendwise.feature.report.ReportArgs
import com.surajdev.spendwise.feature.report.ReportViewModel
import com.surajdev.spendwise.feature.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull

/** Catches DI wiring mistakes (which Koin would otherwise only report at runtime, on a device). */
@OptIn(ExperimentalCoroutinesApi::class)
class DependencyGraphTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun everyScreenCanBeCreated() {
        val dir = Files.createTempDirectory("spendwise-di").toFile()
        val testOverrides = module {
            single<RoomDatabase.Builder<SpendWiseDatabase>> { Room.inMemoryDatabaseBuilder<SpendWiseDatabase>() }
            single { PlatformPaths(File(dir, "prefs.preferences_pb").absolutePath) }
        }
        val koin = startKoin {
            allowOverride(true)
            modules(platformModule, dataModule, domainModule, viewModelModule, testOverrides)
        }.koin

        assertNotNull(koin.get<SyncRepository>())
        assertNotNull(koin.get<AppViewModel>())
        assertNotNull(koin.get<HomeViewModel>())
        assertNotNull(koin.get<ExpenseListViewModel>())
        assertNotNull(koin.get<CategoriesViewModel>())
        assertNotNull(koin.get<SettingsViewModel>())
        assertNotNull(koin.get<AuthViewModel>())
        assertNotNull(koin.get<AddExpenseViewModel> { parametersOf(AddExpenseArgs(null)) })
        assertNotNull(koin.get<ReportViewModel> { parametersOf(ReportArgs(null)) })
    }
}
