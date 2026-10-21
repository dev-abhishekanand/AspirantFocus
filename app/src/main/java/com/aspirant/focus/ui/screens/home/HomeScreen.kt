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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aspirant.focus.data.model.FocusAudioType
import com.aspirant.focus.data.model.MCQBank
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

    val allTopics by viewModel.allTopics.collectAsState()
    val parkingItems by viewModel.parkingItems.collectAsState()
    val dueRevisions by viewModel.dueRevisions.collectAsState()
    val isGrayscale by viewModel.isGrayscaleEnabled.collectAsState()

    var showParkingLotDialog by remember { mutableStateOf(false) }
    var parkingInput by remember { mutableStateOf("") }
    var showMcqDialog by remember { mutableStateOf(false) }
    var mcqIndex by remember { mutableStateOf(0) }
    var mcqCorrectCount by remember { mutableStateOf(0) }

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

        // Selected Exam Tag with D-Day Countdown & Progress
        selectedExam?.let { exam ->
            val daysLeft = maxOf(0L, (viewModel.targetExamDateMillis - System.currentTimeMillis()) / (24 * 3600 * 1000L))
            val hoursLeft = maxOf(0L, ((viewModel.targetExamDateMillis - System.currentTimeMillis()) % (24 * 3600 * 1000L)) / (3600 * 1000L))
            val completedTopics = allTopics.count { it.isCompleted }
            val totalTopics = allTopics.size
            val progressFraction = if (totalTopics > 0) completedTopics.toFloat() / totalTopics.toFloat() else 0f
            val progressPercent = (progressFraction * 100).toInt()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToExams() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎯 TARGET: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryEmerald)
                            Text(exam.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Text("Change Exam ▾", fontSize = 11.sp, color = AccentBlue)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("⏳ $daysLeft Days • $hoursLeft Hours Left", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WarningAmber)
                        Text("$progressPercent% Syllabus Covered", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryEmerald)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = PrimaryEmerald,
                        trackColor = SurfaceDark
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            // Due Revisions Alert Banner
            if (dueRevisions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PurpleAesthetic.copy(alpha = 0.15f))
                        .clickable { onNavigateToExams() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🧠 ${dueRevisions.size} topics due for Spaced Revision today", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PurpleAesthetic)
                    Text("Revise ▾", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PurpleAesthetic)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
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

        // Distraction Parking Lot (Active Focus)
        if (sessionState.isActive) {
            Spacer(modifier = Modifier.height(18.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showParkingLotDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📝", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Distraction Parking Lot", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Urge to search or text? Jot it down & keep studying.", fontSize = 11.sp, color = TextMuted)
                    }
                    Button(
                        onClick = { showParkingLotDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("+ Park", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

                        Divider(modifier = Modifier.padding(vertical = 10.dp), color = Color.White.copy(alpha = 0.05f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Monochrome Ambient Filter", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Reduces vivid colors to kill visual dopamine", fontSize = 11.sp, color = TextMuted)
                            }
                            Switch(
                                checked = isGrayscale,
                                onCheckedChange = { viewModel.toggleGrayscale(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentBlue, checkedTrackColor = AccentBlue.copy(alpha = 0.5f))
                            )
                        }
                    }
                }

                // Parked Thoughts Review List (when Idle)
                if (parkingItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
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
                                Text("📝 PARKED THOUGHTS (${parkingItems.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                                TextButton(onClick = { showParkingLotDialog = true }) {
                                    Text("View All", fontSize = 11.sp, color = AccentBlue)
                                }
                            }
                            parkingItems.take(3).forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(item.text, fontSize = 13.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = { viewModel.deleteParkingItem(item) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Text("✕", color = TextMuted, fontSize = 12.sp)
                                    }
                                }
                            }
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
                    if (sessionState.isStrict) {
                        showMcqDialog = true
                    } else {
                        viewModel.stopFocusSession()
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
                    tint = if (sessionState.isStrict) WarningAmber else Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (sessionState.isStrict) "Strict Locked (Solve 3 MCQs to Unlock)" else "End Session Early",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (sessionState.isStrict) WarningAmber else Color.White
                )
            }
        }
    }

    // Distraction Parking Lot Dialog
    if (showParkingLotDialog) {
        Dialog(onDismissRequest = { showParkingLotDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("📝 Distraction Parking Lot", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Don't break your flow. Jot down whatever urge popped into your head, and attend to it after your session.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = parkingInput,
                        onValueChange = { parkingInput = it },
                        placeholder = { Text("e.g. Check cutoffs, reply to Rahul...", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showParkingLotDialog = false }) {
                            Text("Close", color = TextMuted)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (parkingInput.isNotBlank()) {
                                    viewModel.parkThought(parkingInput.trim())
                                    parkingInput = ""
                                    Toast.makeText(context, "Thought parked! Stay focused.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                        ) {
                            Text("Park Thought", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (parkingItems.isNotEmpty()) {
                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.05f))
                        Text("Previously Parked:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Spacer(modifier = Modifier.height(6.dp))
                        parkingItems.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.text, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = { viewModel.deleteParkingItem(item) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Text("✕", color = TextMuted, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Emergency MCQ Unlock Challenge Dialog
    if (showMcqDialog) {
        val currentQuestion = MCQBank.questions[mcqIndex % MCQBank.questions.size]
        Dialog(onDismissRequest = { showMcqDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎯 Emergency MCQ Gate", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WarningAmber)
                        Badge(containerColor = WarningAmber) { Text("$mcqCorrectCount / 3 Solved", color = BackgroundDark, fontWeight = FontWeight.Bold) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Strict Mode requires solving 3 exam MCQs correctly before early unlock.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "[${currentQuestion.subject}] ${currentQuestion.question}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    currentQuestion.options.forEachIndexed { optIndex, optionText ->
                        Button(
                            onClick = {
                                if (optIndex == currentQuestion.correctIndex) {
                                    val newCount = mcqCorrectCount + 1
                                    mcqCorrectCount = newCount
                                    if (newCount >= 3) {
                                        Toast.makeText(context, "Challenge Passed! Strict Mode Unlocked.", Toast.LENGTH_LONG).show()
                                        viewModel.emergencyUnlock()
                                        showMcqDialog = false
                                        mcqCorrectCount = 0
                                    } else {
                                        Toast.makeText(context, "Correct! Next question...", Toast.LENGTH_SHORT).show()
                                        mcqIndex++
                                    }
                                } else {
                                    Toast.makeText(context, "Incorrect answer! Stay in study mode.", Toast.LENGTH_LONG).show()
                                    showMcqDialog = false
                                    mcqCorrectCount = 0
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard)
                        ) {
                            Text(
                                text = "${('A' + optIndex)}. $optionText",
                                fontSize = 12.sp,
                                color = TextPrimary,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(onClick = {
                            showMcqDialog = false
                            mcqCorrectCount = 0
                        }) {
                            Text("Return to Focus Session", color = PrimaryEmerald, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

