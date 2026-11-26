package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.GanControlScreen
import com.example.ui.screens.GanDashboardScreen
import com.example.ui.screens.GanHistoryScreen
import com.example.ui.screens.GanSettingsScreen
import com.example.ui.theme.IrigasiKuTheme
import com.example.ui.viewmodel.GanViewModel

object AppRoutes {
    const val ROUTE_DASHBOARD = "dashboard"
    const val ROUTE_CONTROL = "control"
    const val ROUTE_HISTORY = "history"
    const val ROUTE_SETTINGS = "settings"
}

private data class NavTab(
    val route: String,
    val label: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val navTabs = listOf(
    NavTab(
        route = AppRoutes.ROUTE_DASHBOARD,
        label = "Monitor",
        title = "GAN Smart Irrigation",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        testTag = "nav_dashboard"
    ),
    NavTab(
        route = AppRoutes.ROUTE_CONTROL,
        label = "Kontrol",
        title = "Control Pump",
        selectedIcon = Icons.Filled.Tune,
        unselectedIcon = Icons.Outlined.Tune,
        testTag = "nav_control"
    ),
    NavTab(
        route = AppRoutes.ROUTE_HISTORY,
        label = "Histori",
        title = "Histori & Kirim Data",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "nav_history"
    ),
    NavTab(
        route = AppRoutes.ROUTE_SETTINGS,
        label = "API",
        title = "Pengaturan API",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_settings"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IrigasiKuApp(
    viewModel: GanViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppRoutes.ROUTE_DASHBOARD
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSnackbar()
        }
    }

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val activeTab = navTabs.firstOrNull { it.route == currentRoute } ?: navTabs.first()

    IrigasiKuTheme(dynamicColor = uiState.config.useDynamicColor) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 600.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets.statusBars,
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = activeTab.title,
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                },
                bottomBar = {
                    if (!isWideScreen) {
                        NavigationBar(
                            modifier = Modifier.navigationBarsPadding(),
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            navTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isWideScreen) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            navTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = AppRoutes.ROUTE_DASHBOARD,
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 840.dp)
                        ) {
                            composable(AppRoutes.ROUTE_DASHBOARD) {
                                GanDashboardScreen(
                                    uiState = uiState,
                                    onRefreshAll = { viewModel.refreshAllData() }
                                )
                            }

                            composable(AppRoutes.ROUTE_CONTROL) {
                                GanControlScreen(
                                    uiState = uiState,
                                    onUpdateDraft = { viewModel.updateLocalControlDraft(it) },
                                    onSaveControl = { viewModel.saveControlToServer() },
                                    onRefreshControl = { viewModel.loadControl(forceOverwrite = true) }
                                )
                            }

                            composable(AppRoutes.ROUTE_HISTORY) {
                                GanHistoryScreen(
                                    uiState = uiState,
                                    onRefreshHistory = { viewModel.loadHistory() },
                                    onSubmitSensorData = { viewModel.submitSensorData(it) }
                                )
                            }

                            composable(AppRoutes.ROUTE_SETTINGS) {
                                GanSettingsScreen(
                                    uiState = uiState,
                                    onSaveApiConfig = { url, dev ->
                                        viewModel.saveApiConnectionConfig(url, dev)
                                    },
                                    onResetDefaultConfig = { viewModel.resetApiConnectionToConfigPhp() },
                                    onTestApiConnection = { url, dev ->
                                        viewModel.testApiConnection(url, dev)
                                    },
                                    onRefreshWeather = { viewModel.refreshWeather() },
                                    onSelectLocationPreset = { viewModel.selectLocationPreset(it) },
                                    onUseGpsLocation = { viewModel.useDeviceGpsLocation() },
                                    onToggleDynamicColor = { viewModel.setDynamicColor(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
