package com.surajdev.spendwise.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.surajdev.spendwise.app.navigation.AddExpenseRoute
import com.surajdev.spendwise.app.navigation.AuthRoute
import com.surajdev.spendwise.app.navigation.CategoriesRoute
import com.surajdev.spendwise.app.navigation.DeepLinks
import com.surajdev.spendwise.app.navigation.ExpensesRoute
import com.surajdev.spendwise.app.navigation.HomeRoute
import com.surajdev.spendwise.app.navigation.ReportRoute
import com.surajdev.spendwise.app.navigation.SettingsRoute
import com.surajdev.spendwise.core.designsystem.component.AppBackground
import com.surajdev.spendwise.core.designsystem.component.AppIcon
import com.surajdev.spendwise.core.designsystem.component.HairlineDivider
import com.surajdev.spendwise.core.designsystem.component.LocalBottomBarClearance
import com.surajdev.spendwise.core.designsystem.component.ambientShadow
import com.surajdev.spendwise.core.designsystem.icon.AppIcons
import com.surajdev.spendwise.core.designsystem.theme.SpendWiseTheme
import com.surajdev.spendwise.core.designsystem.theme.Spacing
import com.surajdev.spendwise.feature.addexpense.AddExpenseRoute as AddExpenseScreenRoute
import com.surajdev.spendwise.feature.auth.AuthRoute as AuthScreenRoute
import com.surajdev.spendwise.feature.categories.CategoriesRoute as CategoriesScreenRoute
import com.surajdev.spendwise.feature.expenselist.ExpenseListRoute
import com.surajdev.spendwise.feature.home.HomeRoute as HomeScreenRoute
import com.surajdev.spendwise.feature.report.ReportRoute as ReportScreenRoute
import com.surajdev.spendwise.feature.settings.SettingsRoute as SettingsScreenRoute
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.DrawableResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.reflect.KClass

/** Root composable shared by Android and iOS. */
@Composable
fun App() {
    val appViewModel = koinViewModel<AppViewModel>()
    val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        appViewModel.onForeground()
        onPauseOrDispose { }
    }
    val mode = themeMode ?: return
    SpendWiseTheme(mode) {
        AppNavigation()
    }
}

private data class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val label: String,
    val icon: DrawableResource,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(HomeRoute, HomeRoute::class, "Home", AppIcons.Home),
    TopLevelDestination(ExpensesRoute, ExpensesRoute::class, "Expenses", AppIcons.Expenses),
    TopLevelDestination(ReportRoute(), ReportRoute::class, "Report", AppIcons.Report),
    TopLevelDestination(SettingsRoute, SettingsRoute::class, "Settings", AppIcons.Settings),
)

private val GlassBarHeight = 64.dp

@Composable
private fun AppNavigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = topLevelDestinations.any { top -> destination?.hasRoute(top.routeClass) == true }
    val vivid = SpendWiseTheme.extended.vivid
    val hazeState = rememberHazeState()

    LaunchedEffect(navController) {
        DeepLinks.events.collect { target ->
            when (target) {
                DeepLinks.Target.TODAY_REPORT -> navController.navigateTopLevel(ReportRoute())
            }
            DeepLinks.consumed()
        }
    }

    // Tab screens draw content under the navigation bar and pad their lists by this much.
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val clearance = when {
        !showBottomBar -> 0.dp
        vivid -> GlassBarHeight + 12.dp + navInset + Spacing.sm
        else -> 81.dp + navInset
    }
    val isSelected: (TopLevelDestination) -> Boolean = { top -> destination?.hierarchy?.any { it.hasRoute(top.routeClass) } == true }

    Box(Modifier.fillMaxSize()) {
        // Everything inside the haze source can be blurred by the glass navigation bar.
        Box(Modifier.fillMaxSize().hazeSource(hazeState)) {
            AppBackground(Modifier.matchParentSize())
            CompositionLocalProvider(LocalBottomBarClearance provides clearance) {
                NavHost(navController = navController, startDestination = HomeRoute) {
                    composable<HomeRoute> {
                        TabContent {
                            HomeScreenRoute(
                                onAddExpense = { navController.navigate(AddExpenseRoute()) },
                                onOpenExpense = { navController.navigate(AddExpenseRoute(it)) },
                                onOpenReport = { navController.navigateTopLevel(ReportRoute()) },
                                onOpenSettings = { navController.navigateTopLevel(SettingsRoute) },
                            )
                        }
                    }
                    composable<ExpensesRoute> {
                        TabContent {
                            ExpenseListRoute(
                                onAddExpense = { navController.navigate(AddExpenseRoute()) },
                                onOpenExpense = { navController.navigate(AddExpenseRoute(it)) },
                            )
                        }
                    }
                    composable<ReportRoute> { entry ->
                        val route = entry.toRoute<ReportRoute>()
                        TabContent {
                            ReportScreenRoute(
                                initialDate = route.epochDay?.let { LocalDate.fromEpochDays(it) },
                                onOpenExpense = { navController.navigate(AddExpenseRoute(it)) },
                            )
                        }
                    }
                    composable<SettingsRoute> {
                        TabContent {
                            SettingsScreenRoute(
                                onOpenCategories = { navController.navigate(CategoriesRoute) },
                                onSignIn = { navController.navigate(AuthRoute) },
                            )
                        }
                    }
                    composable<AddExpenseRoute> { entry ->
                        val route = entry.toRoute<AddExpenseRoute>()
                        AddExpenseScreenRoute(
                            expenseId = route.expenseId,
                            onClose = { navController.popBackStack() },
                            onManageCategories = { navController.navigate(CategoriesRoute) },
                        )
                    }
                    composable<CategoriesRoute> {
                        CategoriesScreenRoute(onBack = { navController.popBackStack() })
                    }
                    composable<AuthRoute> {
                        AuthScreenRoute(onDone = { navController.popBackStack() })
                    }
                }
            }
        }

        if (showBottomBar) {
            val modifier = Modifier.align(Alignment.BottomCenter)
            if (vivid) {
                GlassNavBar(hazeState, isSelected, onSelect = { navController.navigateTopLevel(it.route) }, modifier)
            } else {
                DockedNavBar(isSelected, onSelect = { navController.navigateTopLevel(it.route) }, modifier)
            }
        }
    }
}

/** Tab screens extend under the navigation bar, so they skip the system navigation inset. */
@Composable
private fun TabContent(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().consumeWindowInsets(WindowInsets.navigationBars)) { content() }
}

/**
 * Light theme: a floating frosted-glass pill that blurs the content scrolling beneath it.
 * The selected tab expands into a gradient capsule with its label.
 */
@Composable
private fun GlassNavBar(
    hazeState: HazeState,
    isSelected: (TopLevelDestination) -> Boolean,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extended = SpendWiseTheme.extended
    val shape = CircleShape
    Row(
        modifier = modifier
            .navigationBarsPadding()
            .padding(start = Spacing.screen, end = Spacing.screen, bottom = 12.dp)
            .fillMaxWidth()
            .height(GlassBarHeight)
            .ambientShadow(shape, Color(0x2E6C5CE7), radius = 28.dp, offsetY = 10.dp)
            .clip(shape)
            .hazeBlur(
                input = HazeInput.Sources(hazeState),
                style = HazeBlurStyle {
                    blurRadius(24.dp)
                    noiseFactor(0.05f)
                    colorEffects(listOf(HazeColorEffect.tint(Color.White.copy(alpha = 0.48f))))
                },
            )
            .border(1.dp, extended.cardBorder, shape)
            .padding(6.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        topLevelDestinations.forEach { top ->
            val selected = isSelected(top)
            val weight by animateFloatAsState(if (selected) 1.7f else 1f, label = "tab-weight")
            Box(
                modifier = Modifier
                    .weight(weight)
                    .fillMaxHeight()
                    .clip(shape)
                    .then(if (selected) Modifier.background(extended.primaryBrush) else Modifier)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(top) }),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(
                        top.icon,
                        contentDescription = top.label,
                        tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 22.dp,
                    )
                    AnimatedVisibility(selected, enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) {
                        Text(
                            top.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            maxLines = 1,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Dark theme: the classic docked bar with a hairline top border. */
@Composable
private fun DockedNavBar(
    isSelected: (TopLevelDestination) -> Boolean,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        HairlineDivider()
        NavigationBar(containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
            topLevelDestinations.forEach { top ->
                val selected = isSelected(top)
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelect(top) },
                    icon = { AppIcon(top.icon, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant) },
                    label = { Text(top.label, style = MaterialTheme.typography.labelMedium) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

/** Switches tabs keeping a single copy of each tab on the back stack. */
private fun NavHostController.navigateTopLevel(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
