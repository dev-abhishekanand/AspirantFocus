package com.aspirant.focus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.aspirant.focus.ui.screens.analytics.AnalyticsScreen
import com.aspirant.focus.ui.screens.blocker.BlockerScreen
import com.aspirant.focus.ui.screens.home.HomeScreen
import com.aspirant.focus.ui.screens.permissions.PermissionGuideScreen
import com.aspirant.focus.ui.screens.strict.StrictModeScreen
import com.aspirant.focus.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AspirantFocusTheme {
                MainAppNavHost()
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Focus : Screen("focus", "Focus", Icons.Default.Timer)
    object Blocker : Screen("blocker", "Blocker", Icons.Default.Block)
    object Strict : Screen("strict", "Strict", Icons.Default.Lock)
    object Analytics : Screen("analytics", "Analytics", Icons.Default.BarChart)
    object Permissions : Screen("permissions", "Permissions", Icons.Default.Lock)
}

@Composable
fun MainAppNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        Screen.Focus,
        Screen.Blocker,
        Screen.Strict,
        Screen.Analytics
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        bottomBar = {
            if (currentRoute != Screen.Permissions.route) {
                NavigationBar(
                    containerColor = SurfaceDark,
                    contentColor = TextPrimary
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = isSelected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryEmerald,
                                selectedTextColor = PrimaryEmerald,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted,
                                indicatorColor = SurfaceCard
                            ),
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Focus.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Focus.route) {
                HomeScreen(
                    onNavigateToPermissions = {
                        navController.navigate(Screen.Permissions.route)
                    }
                )
            }
            composable(Screen.Blocker.route) {
                BlockerScreen()
            }
            composable(Screen.Strict.route) {
                StrictModeScreen()
            }
            composable(Screen.Analytics.route) {
                AnalyticsScreen()
            }
            composable(Screen.Permissions.route) {
                PermissionGuideScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
