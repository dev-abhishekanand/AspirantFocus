package com.aspirant.focus.ui.screens.strict

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aspirant.focus.AspirantApplication
import com.aspirant.focus.service.FocusSessionService
import com.aspirant.focus.ui.theme.*

@Composable
fun StrictModeScreen() {
    val prefs = AspirantApplication.instance.preferencesManager
    val sessionState by FocusSessionService.sessionState.collectAsState()

    var antiUninstall by remember { mutableStateOf(prefs.isAntiUninstallEnabled) }
    var settingsLock by remember { mutableStateOf(prefs.isSettingsLockEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "Strict Mode & Protection",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Zero loopholes when willpower runs out",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Live Strict Status Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (sessionState.isStrict) DangerRed.copy(alpha = 0.15f) else SurfaceCard
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (sessionState.isStrict) Icons.Default.Lock else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (sessionState.isStrict) DangerRed else PrimaryEmerald,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = if (sessionState.isStrict) "Strict Session Active" else "Protection Ready",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (sessionState.isStrict) DangerRed else TextPrimary
                    )
                    Text(
                        text = if (sessionState.isStrict)
                            "All bypasses and settings are locked until the timer ends."
                        else
                            "Strict Mode will lock down your phone when you next hit Start.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "PROTECTION LAYERS (PRO)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Anti-Uninstall Protection
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Anti-Uninstall Guard", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Blocks attempting to delete or uninstall Aspirant Focus during active study sessions.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = antiUninstall,
                    onCheckedChange = {
                        antiUninstall = it
                        prefs.isAntiUninstallEnabled = it
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DangerRed,
                        checkedTrackColor = DangerRed.copy(alpha = 0.5f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Settings Lockout
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Settings Lockout", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Blocks opening Phone Settings to force stop or disable accessibility permissions.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = settingsLock,
                    onCheckedChange = {
                        settingsLock = it
                        prefs.isSettingsLockEnabled = it
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DangerRed,
                        checkedTrackColor = DangerRed.copy(alpha = 0.5f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Curfew & Bedtime Guard
        var curfewEnabled by remember { mutableStateOf(prefs.isCurfewEnabled) }
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🌙 Bedtime & Morning Curfew Guard", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Locks all feeds and distracting apps during sleep hours (11:00 PM – 06:30 AM) to kill night doomscrolling and morning procrastination.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = curfewEnabled,
                        onCheckedChange = {
                            curfewEnabled = it
                            prefs.isCurfewEnabled = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PrimaryEmerald,
                            checkedTrackColor = PrimaryEmerald.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Intention Breath Pause
        var intentionPause by remember { mutableStateOf(prefs.isIntentionPauseEnabled) }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("🌿 Intention Pause (10-Second Breath)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Forces a 10-second deep breath pause before opening distracting apps outside focus to break subconscious muscle memory.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = intentionPause,
                    onCheckedChange = {
                        intentionPause = it
                        prefs.isIntentionPauseEnabled = it
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AccentBlue,
                        checkedTrackColor = AccentBlue.copy(alpha = 0.5f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Emergency MCQ Unlock Challenge
        var emergencyMcq by remember { mutableStateOf(prefs.isEmergencyMcqEnabled) }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("🎯 Emergency Friction: MCQ Gate", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Attempting to quit strict sessions early requires solving tough exam MCQs correctly before early unlock is allowed.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = emergencyMcq,
                    onCheckedChange = {
                        emergencyMcq = it
                        prefs.isEmergencyMcqEnabled = it
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = WarningAmber,
                        checkedTrackColor = WarningAmber.copy(alpha = 0.5f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Accountability Partner Alert
        var accountabilityEnabled by remember { mutableStateOf(prefs.isAccountabilityEnabled) }
        var partnerName by remember { mutableStateOf(prefs.accountabilityName) }
        var partnerPhone by remember { mutableStateOf(prefs.accountabilityPhone) }

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
                    Column(modifier = Modifier.weight(1f)) {
                        Text("👥 Accountability Partner Alert", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Logs uninstallation or early exit attempts directly to a trusted study partner or parent.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = accountabilityEnabled,
                        onCheckedChange = {
                            accountabilityEnabled = it
                            prefs.isAccountabilityEnabled = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PurpleAesthetic,
                            checkedTrackColor = PurpleAesthetic.copy(alpha = 0.5f)
                        )
                    )
                }

                if (accountabilityEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = partnerName,
                        onValueChange = {
                            partnerName = it
                            prefs.accountabilityName = it
                        },
                        label = { Text("Partner's Name") },
                        placeholder = { Text("e.g. Papa, Bhai, Study Partner") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = partnerPhone,
                        onValueChange = {
                            partnerPhone = it
                            prefs.accountabilityPhone = it
                        },
                        label = { Text("Partner's Phone / Contact") },
                        placeholder = { Text("+91 9876543210") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Reboot Persistence Note
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Reboot Persistence",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "If you restart your phone to bypass Strict Mode, Aspirant Focus automatically resumes the locked countdown upon boot.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
