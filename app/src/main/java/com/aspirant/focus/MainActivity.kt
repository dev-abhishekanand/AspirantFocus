package com.aspirant.focus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.aspirant.focus.ui.screens.analytics.AnalyticsScreen
import com.aspirant.focus.ui.screens.blocker.BlockerScreen
import com.aspirant.focus.ui.screens.exams.ExamsScreen
import com.aspirant.focus.ui.screens.home.HomeScreen
import com.aspirant.focus.ui.screens.permissions.PermissionGuideScreen
import com.aspirant.focus.ui.screens.strict.StrictModeScreen
import com.aspirant.focus.ui.theme.*
import com.aspirant.focus.util.ActiveBlockNotice
import com.aspirant.focus.util.PermissionHelper

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.aspirant.focus.service.BlockerProtectionService.start(this)
        setContent {
            AspirantFocusTheme {
                MainAppNavHost()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Focus : Screen("focus", "Focus", Icons.Default.Timer)
    object Blocker : Screen("blocker", "Blocker", Icons.Default.Block)
    object Exams : Screen("exams", "Syllabus", Icons.Default.School)
    object Strict : Screen("strict", "Strict", Icons.Default.Lock)
    object Analytics : Screen("analytics", "Analytics", Icons.Default.BarChart)
    object Permissions : Screen("permissions", "Permissions", Icons.Default.Lock)
}

@Composable
fun MainAppNavHost() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val blockNotice by ActiveBlockNotice.notice.collectAsState()

    var isAccessibilityActive by remember { mutableStateOf(PermissionHelper.isAccessibilityEnabled(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityActive = PermissionHelper.isAccessibilityEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val bottomNavItems = listOf(
        Screen.Focus,
        Screen.Blocker,
        Screen.Exams,
        Screen.Strict,
        Screen.Analytics
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        topBar = {
            // Persistent warning banner if Accessibility Service is not active
            if (!isAccessibilityActive && currentRoute != Screen.Permissions.route) {
                Surface(
                    color = DangerRed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate(Screen.Permissions.route) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Blocker Engine Disabled! Tap to activate permissions.",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Enable →",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        },
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
                    },
                    onNavigateToExams = {
                        navController.navigate(Screen.Exams.route)
                    }
                )
            }
            composable(Screen.Blocker.route) {
                BlockerScreen()
            }
            composable(Screen.Exams.route) {
                ExamsScreen()
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

    // Full-screen Intervention Modal when an app, adult site, or curfew is blocked
    blockNotice?.let { notice ->
        Dialog(
            onDismissRequest = { ActiveBlockNotice.dismiss() },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val emoji = when (notice.reason) {
                        "CURFEW" -> "🌙"
                        "ADULT_CONTENT" -> "🛑"
                        "INCOGNITO" -> "🔒"
                        "VPN_BLOCKED" -> "🛡️"
                        "PLAY_STORE_RESTRICTED" -> "⛔"
                        else -> "⚠️"
                    }
                    Text(text = emoji, fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = notice.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DangerRed,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = notice.message,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { ActiveBlockNotice.dismiss() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Stay Focused (Return to Aspirant Focus)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
