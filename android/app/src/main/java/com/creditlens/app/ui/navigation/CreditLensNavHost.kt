package com.creditlens.app.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.creditlens.app.data.repository.CreditLensRepository
import com.creditlens.app.ui.screens.*
import com.creditlens.app.ui.theme.*
import com.creditlens.app.ui.viewmodel.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditLensNavHost(
    repository: CreditLensRepository
) {
    val navController = rememberNavController()
    val factory = CreditLensViewModelFactory(repository)

    val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
    val assessmentViewModel: AssessmentViewModel = viewModel(factory = factory)
    val historyViewModel: HistoryViewModel = viewModel(factory = factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)

    val dashboardState by dashboardViewModel.uiState.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = CardBackground,
                drawerContentColor = PrimaryText,
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(SelectedNav, RoundedCornerShape(10.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "CreditLens",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = PrimaryText
                        )
                        Text(
                            text = "Credit Risk Engine",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = BorderColor)
                Spacer(modifier = Modifier.height(12.dp))

                drawerScreens.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                )
                            )
                        },
                        selected = isSelected,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = SelectedNav,
                            selectedIconColor = PrimaryBlue,
                            selectedTextColor = PrimaryBlue,
                            unselectedContainerColor = Color.Transparent,
                            unselectedIconColor = SecondaryText,
                            unselectedTextColor = PrimaryText
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider(color = BorderColor)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CreditLens v1.0.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = if (dashboardState.isBackendOnline) "API Connected" else "API Offline",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (dashboardState.isBackendOnline) SuccessGreen else StatusRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    ) {
        Scaffold(
            containerColor = MainBackground,
            topBar = {
                // Compact Top App Bar with brand, connection status and hamburger menu
                Surface(
                    color = CardBackground,
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(SelectedNav, RoundedCornerShape(8.dp))
                                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = "Brand Icon",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "CreditLens",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = PrimaryText
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Compact connection status badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SecondarySurface,
                                border = BorderStroke(1.dp, BorderColor),
                                modifier = Modifier.clickable { dashboardViewModel.checkHealth() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(
                                                if (dashboardState.isBackendOnline) SuccessGreen else StatusRed,
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (dashboardState.isBackendOnline) "Online" else "Offline",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = if (dashboardState.isBackendOnline) SuccessGreen else StatusRed
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Navigation menu",
                                    tint = PrimaryText,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Surface(
                    color = CardBackground,
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    NavigationBar(
                        containerColor = CardBackground,
                        tonalElevation = 0.dp
                    ) {
                        bottomNavScreens.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrimaryBlue,
                                    selectedTextColor = PrimaryBlue,
                                    indicatorColor = SelectedNav,
                                    unselectedIconColor = SecondaryText,
                                    unselectedTextColor = SecondaryText
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        viewModel = dashboardViewModel,
                        onNavigateToNewAssessment = {
                            navController.navigate(Screen.NewAssessment.route)
                        },
                        onNavigateToHistory = {
                            navController.navigate(Screen.History.route)
                        },
                        onNavigateToModelInfo = {
                            navController.navigate(Screen.ModelInfo.route)
                        }
                    )
                }
                composable(Screen.NewAssessment.route) {
                    NewAssessmentScreen(viewModel = assessmentViewModel)
                }
                composable(Screen.History.route) {
                    HistoryScreen(viewModel = historyViewModel)
                }
                composable(Screen.ModelInfo.route) {
                    ModelInfoScreen()
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(viewModel = settingsViewModel)
                }
                composable(Screen.About.route) {
                    AboutScreen()
                }
            }
        }
    }
}
