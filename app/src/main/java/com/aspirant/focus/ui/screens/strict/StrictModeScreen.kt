package com.aspirant.focus.ui.screens.strict

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aspirant.focus.AspirantApplication
import com.aspirant.focus.service.FocusSessionService
import com.aspirant.focus.ui.theme.*
import java.util.Locale

@Composable
fun StrictModeScreen() {
    val context = LocalContext.current
    val prefs = AspirantApplication.instance.preferencesManager
    val sessionState by FocusSessionService.sessionState.collectAsState()

    var antiUninstall by remember { mutableStateOf(prefs.isAntiUninstallEnabled) }
    var settingsLock by remember { mutableStateOf(prefs.isSettingsLockEnabled) }
    var kioskLock by remember { mutableStateOf(prefs.isKioskLockEnabled) }
    var studyWhitelist by remember { mutableStateOf(prefs.isStudyToolsWhitelisted) }

    var curfewStartHour by remember { mutableStateOf(prefs.curfewStartHour) }
    var curfewStartMin by remember { mutableStateOf(prefs.curfewStartMinute) }
    var curfewEndHour by remember { mutableStateOf(prefs.curfewEndHour) }
    var curfewEndMin by remember { mutableStateOf(prefs.curfewEndMinute) }

    fun formatTime(hour: Int, minute: Int): String {
        val h = if (hour == 0 || hour == 12) 12 else hour % 12
        val amPm = if (hour < 12) "AM" else "PM"
        return String.format(Locale.getDefault(), "%02d:%02d %s", h, minute, amPm)
    }

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

        // Screen Lock (Kiosk Mode)
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
                    Text("🔒 Screen Lock (Cannot Minimize)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Keeps Aspirant Focus pinned to the screen during strict focus. Pressing Home or App Switcher will immediately return to your study screen.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = kioskLock,
                    onCheckedChange = {
                        kioskLock = it
                        prefs.isKioskLockEnabled = it
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DangerRed,
                        checkedTrackColor = DangerRed.copy(alpha = 0.5f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Permitted Study Tools Whitelist
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
                    Text("📚 Permit Study Tools (Work Mode)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Allows essential study apps (Calculator, PDF/Docs Viewer, Zoom, Anki, Notion) during Strict Mode so you can study without interruption.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = studyWhitelist,
                    onCheckedChange = {
                        studyWhitelist = it
                        prefs.isStudyToolsWhitelisted = it
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PrimaryEmerald,
                        checkedTrackColor = PrimaryEmerald.copy(alpha = 0.5f)
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
                            "Locks all feeds and distracting apps during sleep hours to kill night doomscrolling and morning procrastination.",
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

                if (curfewEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Bedtime Start Picker Button
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    android.app.TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            curfewStartHour = hour
                                            curfewStartMin = minute
                                            prefs.curfewStartHour = hour
                                            prefs.curfewStartMinute = minute
                                        },
                                        curfewStartHour,
                                        curfewStartMin,
                                        false
                                    ).show()
                                },
                            color = SurfaceDark,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Bedtime Starts",
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatTime(curfewStartHour, curfewStartMin),
                                    fontSize = 15.sp,
                                    color = DangerRed,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Tap to change",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Morning Wakeup Picker Button
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    android.app.TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            curfewEndHour = hour
                                            curfewEndMin = minute
                                            prefs.curfewEndHour = hour
                                            prefs.curfewEndMinute = minute
                                        },
                                        curfewEndHour,
                                        curfewEndMin,
                                        false
                                    ).show()
                                },
                            color = SurfaceDark,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Morning Wakeup",
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatTime(curfewEndHour, curfewEndMin),
                                    fontSize = 15.sp,
                                    color = PrimaryEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Tap to change",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
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
