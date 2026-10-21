package com.aspirant.focus.ui.screens.permissions

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aspirant.focus.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionGuideScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var hasAccessibility by remember { mutableStateOf(checkAccessibility(context)) }
    var hasOverlay by remember { mutableStateOf(checkOverlay(context)) }
    var hasUsage by remember { mutableStateOf(checkUsage(context)) }
    var hasBattery by remember { mutableStateOf(checkBattery(context)) }

    // Recheck on resume
    DisposableEffect(Unit) {
        onDispose { }
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
                text = "To block Instagram Reels, YouTube Shorts, and enforce Strict Mode, Aspirant Focus needs the following system permissions:",
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Accessibility Service
            PermissionCard(
                title = "1. Accessibility Service",
                description = "Crucial for detecting and blocking Reels, Shorts, and stopping bypass attempts.",
                isGranted = hasAccessibility,
                onGrantClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Display Over Other Apps
            PermissionCard(
                title = "2. Display Over Other Apps",
                description = "Enables the mindful pause & block screen overlay over distracting apps.",
                isGranted = hasOverlay,
                onGrantClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Usage Access
            PermissionCard(
                title = "3. Usage Access",
                description = "Enables accurate daily screen time and distraction breakdown reports.",
                isGranted = hasUsage,
                onGrantClick = {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Battery Optimization Exemption
            PermissionCard(
                title = "4. Background Battery Exemption",
                description = "Prevents Android from killing your focus timer in the background.",
                isGranted = hasBattery,
                onGrantClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
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
            }
        }
    }
}

private fun checkAccessibility(context: Context): Boolean {
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabledServices.contains(context.packageName)
}

private fun checkOverlay(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else true
}

private fun checkUsage(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun checkBattery(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        pm.isIgnoringBatteryOptimizations(context.packageName)
    } else true
}
