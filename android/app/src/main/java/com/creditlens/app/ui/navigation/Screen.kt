package com.creditlens.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object NewAssessment : Screen("assessment", "Assess", Icons.Default.Calculate)
    object History : Screen("history", "History", Icons.Default.History)
    object ModelInfo : Screen("model_info", "Model", Icons.Default.Analytics)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object About : Screen("about", "About", Icons.Default.Info)
}

// Fixed four-item bottom navigation
val bottomNavScreens = listOf(
    Screen.Dashboard,
    Screen.NewAssessment,
    Screen.History,
    Screen.ModelInfo
)

// Complete navigation drawer items
val drawerScreens = listOf(
    Screen.Dashboard,
    Screen.NewAssessment,
    Screen.History,
    Screen.ModelInfo,
    Screen.Settings,
    Screen.About
)
