package com.aspirant.focus.ui.screens.analytics

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aspirant.focus.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel = viewModel()) {
    val context = LocalContext.current
    val sessions by viewModel.sessions.collectAsState()
    val usageStats by viewModel.usageStats.collectAsState()
    val hasPermission by viewModel.hasUsagePermission.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadStats()
    }

    val totalPhoneMillis = usageStats.sumOf { it.totalTimeInForegroundMillis }
    val totalStudySeconds = sessions.sumOf { it.durationSeconds }

    val phoneHours = totalPhoneMillis / (1000 * 3600)
    val phoneMins = (totalPhoneMillis / (1000 * 60)) % 60

    val studyHours = totalStudySeconds / 3600
    val studyMins = (totalStudySeconds % 3600) / 60

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(20.dp)
    ) {
        item {
            Text(
                text = "Analytics & Reports",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Track your battle against screen addiction",
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Main Ratio Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "TODAY'S COMPARISON",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${studyHours}h ${studyMins}m",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryEmerald
                            )
                            Text("Total Focused Study", fontSize = 12.sp, color = TextSecondary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${phoneHours}h ${phoneMins}m",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (phoneHours >= 6) DangerRed else WarningAmber
                            )
                            Text("Total Phone Screen Time", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Distraction Breakdown Header
            Text(
                text = "APP USAGE SINKHOLES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (!hasPermission) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = AccentBlue)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Usage Access Required",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Grant Usage Access permission so Aspirant Focus can report your app time breakdown.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Button(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                        ) {
                            Text("Grant Permission", fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // List top apps
        items(usageStats.take(6)) { stat ->
            val appMins = stat.totalTimeInForegroundMillis / (1000 * 60)
            val h = appMins / 60
            val m = appMins % 60
            val timeString = if (h > 0) "${h}h ${m}m" else "${m}m"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stat.appName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = timeString,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (stat.isDistraction) DangerRed else TextSecondary
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "RECENT STUDY SESSIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        items(sessions.take(10)) { session ->
            val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                .format(Date(session.startTimeMillis))
            val mins = session.durationSeconds / 60

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = session.subjectName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryEmerald
                        )
                        Text(
                            text = dateStr,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                    Text(
                        text = "${mins} mins",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}
