package com.aspirant.focus.ui.screens.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aspirant.focus.data.model.FocusAudioType
import com.aspirant.focus.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigateToPermissions: () -> Unit,
    onNavigateToExams: () -> Unit
) {
    val context = LocalContext.current
    val sessionState by viewModel.sessionState.collectAsState()
    val selectedExam by viewModel.selectedExam.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val selectedSubject by viewModel.selectedSubject.collectAsState()
    val topics by viewModel.topics.collectAsState()
    val selectedTopic by viewModel.selectedTopic.collectAsState()
    val selectedDuration by viewModel.selectedDurationMinutes.collectAsState()
    val isPomodoro by viewModel.isPomodoro.collectAsState()
    val isStrict by viewModel.isStrict.collectAsState()
    val selectedAudio by viewModel.selectedAudio.collectAsState()
    val todayStudySec by viewModel.todayStudySeconds.collectAsState()
    val todayPhoneMillis by viewModel.todayPhoneMillis.collectAsState()

    val durations = listOf(15, 25, 45, 60, 90, 120)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Aspirant Focus",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Reclaim your hours. Clear your exam.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
            TextButton(onClick = onNavigateToPermissions) {
                Text("Permissions", color = AccentBlue, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Selected Exam Tag with quick change button
        selectedExam?.let { exam ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .clickable { onNavigateToExams() }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎯 TARGET: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryEmerald)
                    Text(exam.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
                Text("Change Exam ▾", fontSize = 11.sp, color = AccentBlue)
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Reality Check Card (Phone Time vs Study Time)
        val studyHours = todayStudySec / 3600
        val studyMins = (todayStudySec % 3600) / 60
        val phoneHours = todayPhoneMillis / (1000 * 3600)
        val phoneMins = (todayPhoneMillis / (1000 * 60)) % 60

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "TODAY'S REALITY CHECK",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "${studyHours}h ${studyMins}m",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryEmerald
                        )
                        Text("Study Time", fontSize = 12.sp, color = TextSecondary)
                    }
                    Divider(
                        modifier = Modifier
                            .height(40.dp)
                            .width(1.dp),
                        color = Color.White.copy(alpha = 0.1f)
                    )
                    Column {
                        Text(
                            text = "${phoneHours}h ${phoneMins}m",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (phoneHours >= 6) DangerRed else WarningAmber
                        )
                        Text("Phone Time", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Circular Timer Display
        val remainingSec = if (sessionState.isActive) sessionState.remainingSeconds else selectedDuration * 60L
        val displayMin = remainingSec / 60
        val displaySec = remainingSec % 60
        val progress = if (sessionState.isActive && sessionState.totalSeconds > 0) {
            remainingSec.toFloat() / sessionState.totalSeconds.toFloat()
        } else 1.0f

        Box(
            modifier = Modifier
                .size(250.dp)
                .clip(CircleShape)
                .background(SurfaceDark)
                .border(6.dp, Brush.radialGradient(listOf(SurfaceCard, BackgroundDark)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = if (sessionState.isBreak) AccentBlue else PrimaryEmerald,
                strokeWidth = 10.dp,
                trackColor = SurfaceCard
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (sessionState.isActive) {
                    Text(
                        text = if (sessionState.isBreak) "REST BREAK" else sessionState.subject.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (sessionState.isBreak) AccentBlue else PrimaryEmerald,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        maxLines = 1
                    )
                }
                Text(
                    text = String.format("%02d:%02d", displayMin, displaySec),
                    fontSize = 50.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (sessionState.isStrict) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = DangerRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "STRICT MODE LOCKED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DangerRed
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Session Configuration (Shown when IDLE)
        AnimatedVisibility(visible = !sessionState.isActive) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Subject Selector
                Text(
                    text = "SELECT SUBJECT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(subjects) { subject ->
                        val isSelected = subject.name == selectedSubject
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) PrimaryEmerald else SurfaceCard)
                                .clickable { viewModel.selectSubject(subject.name) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = subject.name,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Active Topic Selector Chips
                if (topics.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "TOPIC YOU'RE STUDYING NOW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(topics) { topic ->
                            val isSelected = topic.name == selectedTopic
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) AccentBlue else SurfaceDark)
                                    .clickable { viewModel.selectTopic(topic.name) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = topic.name,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Duration Selector
                Text(
                    text = "DURATION (MINUTES)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    durations.forEach { mins ->
                        val isSelected = mins == selectedDuration
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) AccentBlue else SurfaceCard)
                                .clickable { viewModel.selectDuration(mins) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${mins}m",
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Focus Audio Sound Selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = PurpleAesthetic, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("FOCUS AUDIO SOUND", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(FocusAudioType.values()) { audio ->
                                val isSelected = audio == selectedAudio
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) PurpleAesthetic else SurfaceCard)
                                        .clickable { viewModel.selectAudio(audio) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = audio.displayName,
                                        fontSize = 11.sp,
                                        color = if (isSelected) Color.White else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Strict Mode & Pomodoro Toggles
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Strict Mode", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("No early quit. Anti-uninstall locked.", fontSize = 11.sp, color = TextMuted)
                            }
                            Switch(
                                checked = isStrict,
                                onCheckedChange = { viewModel.toggleStrict(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = DangerRed, checkedTrackColor = DangerRed.copy(alpha = 0.5f))
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 10.dp), color = Color.White.copy(alpha = 0.05f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Pomodoro Cycles", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Automated 5-minute break after session", fontSize = 11.sp, color = TextMuted)
                            }
                            Switch(
                                checked = isPomodoro,
                                onCheckedChange = { viewModel.togglePomodoro(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = PrimaryEmerald, checkedTrackColor = PrimaryEmerald.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main Action Button
        if (!sessionState.isActive) {
            Button(
                onClick = { viewModel.startFocusSession() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Focus Session", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = {
                    val stopped = viewModel.stopFocusSession()
                    if (!stopped) {
                        Toast.makeText(
                            context,
                            "Strict Mode Active: You committed to finish this study block!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (sessionState.isStrict) SurfaceCard else DangerRed
                )
            ) {
                Icon(
                    imageVector = if (sessionState.isStrict) Icons.Default.Lock else Icons.Default.Stop,
                    contentDescription = null,
                    tint = if (sessionState.isStrict) DangerRed else Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (sessionState.isStrict) "Strict Mode Active (Locked)" else "End Session Early",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (sessionState.isStrict) DangerRed else Color.White
                )
            }
        }
    }
}
