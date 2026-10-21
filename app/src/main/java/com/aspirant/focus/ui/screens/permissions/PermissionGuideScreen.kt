package com.aspirant.focus.ui.screens.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.aspirant.focus.ui.theme.*
import com.aspirant.focus.util.PermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionGuideScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasAccessibility by remember { mutableStateOf(PermissionHelper.isAccessibilityEnabled(context)) }
    var hasOverlay by remember { mutableStateOf(PermissionHelper.isOverlayGranted(context)) }
    var hasUsage by remember { mutableStateOf(PermissionHelper.isUsageGranted(context)) }
    var hasBattery by remember { mutableStateOf(PermissionHelper.isBatteryOptimizationIgnored(context)) }

    // Recheck permissions whenever user returns from Android Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAccessibility = PermissionHelper.isAccessibilityEnabled(context)
                hasOverlay = PermissionHelper.isOverlayGranted(context)
                hasUsage = PermissionHelper.isUsageGranted(context)
                hasBattery = PermissionHelper.isBatteryOptimizationIgnored(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup Required Permissions", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = "Aspirant Focus requires the following Android permissions to intercept feeds, block adult websites, and enforce strict study curfews:",
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Android 13/14/15 Restricted Settings 2-Step Card
            if (!hasAccessibility) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.12f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "2-Step Blocker Activation",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarningAmber
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "On Android 13, 14 & 15, activating the service requires 2 quick steps:\n\n" +
                                   "• STEP 1 (Unlock Security):\n" +
                                   "  Tap 'Step 1: App Info' -> tap the 3 dots (⋮) in top-right -> tap 'Allow restricted settings'.\n" +
                                   "  ⚠️ NOTE: If the 3 dots are already gone, Step 1 is ALREADY COMPLETED!\n\n" +
                                   "• STEP 2 (Turn Switch ON):\n" +
                                   "  Tap 'Step 2: Turn Switch ON' below -> find 'Aspirant Focus' (under Downloaded apps) -> FLIP THE SWITCH TO ON.",
                            fontSize = 12.sp,
                            color = TextPrimary,
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { PermissionHelper.openAppDetailsSettings(context) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Step 1: App Info", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { PermissionHelper.openAccessibilitySettings(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Text("Step 2: Turn Switch ON", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 1. Accessibility Service (Primary Blocker Engine)
            PermissionCard(
                title = "1. Accessibility Service (Core Engine)",
                description = "Required to detect and block Instagram, YouTube Shorts, adult sites, and enforce curfew lockdowns.",
                isGranted = hasAccessibility,
                onGrantClick = {
                    PermissionHelper.openAccessibilitySettings(context)
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Display Over Other Apps
            PermissionCard(
                title = "2. Display Over Other Apps (Overlay)",
                description = "Required to show the full-screen intervention lock screen over distracting or adult content.",
                isGranted = hasOverlay,
                onGrantClick = {
                    PermissionHelper.openOverlaySettings(context)
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Usage Access
            PermissionCard(
                title = "3. Usage Access",
                description = "Enables accurate daily screen time tracking and sinkhole reports.",
                isGranted = hasUsage,
                onGrantClick = {
                    PermissionHelper.openUsageSettings(context)
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Battery Optimization Exemption
            PermissionCard(
                title = "4. Background Battery Exemption",
                description = "Prevents Android from killing your focus timer in the background.",
                isGranted = hasBattery,
                onGrantClick = {
                    PermissionHelper.openBatterySettings(context)
                }
            )
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Icon(
                    imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (isGranted) PrimaryEmerald else DangerRed
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = description, fontSize = 12.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(12.dp))
            if (!isGranted) {
                Button(
                    onClick = onGrantClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enable in Settings", fontSize = 13.sp)
                }
            } else {
                Surface(
                    color = PrimaryEmerald.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "✓ Active & Protected",
                        color = PrimaryEmerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
