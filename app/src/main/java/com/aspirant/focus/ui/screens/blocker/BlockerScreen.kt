package com.aspirant.focus.ui.screens.blocker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aspirant.focus.ui.theme.*

@Composable
fun BlockerScreen(viewModel: BlockerViewModel = viewModel()) {
    val isReelsEnabled by viewModel.isReelsBlockerEnabled.collectAsState()
    val isShortsEnabled by viewModel.isShortsBlockerEnabled.collectAsState()
    val isYtStudyEnabled by viewModel.isYouTubeStudyModeEnabled.collectAsState()
    val isWebsiteEnabled by viewModel.isWebsiteBlocker.collectAsState()
    val appRules by viewModel.appRules.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(20.dp)
    ) {
        item {
            Text(
                text = "Distraction Blocker",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Targeted controls to stop doomscrolling",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Signature Feature Header
            Text(
                text = "TARGETED FEED BLOCKERS (PRO)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryEmerald,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Instagram Reels Blocker Card
            BlockerToggleCard(
                title = "Instagram Reels Blocker",
                subtitle = "Blocks the Reels viewer & tab instantly. Keep DMs and search available.",
                isChecked = isReelsEnabled,
                onCheckedChange = { viewModel.setReelsBlocker(it) },
                accentColor = PurpleAesthetic
            )

            Spacer(modifier = Modifier.height(12.dp))

            // YouTube Shorts Blocker Card
            BlockerToggleCard(
                title = "YouTube Shorts Blocker",
                subtitle = "Blocks the Shorts shelf & vertical video player. Keep lectures accessible.",
                isChecked = isShortsEnabled,
                onCheckedChange = { viewModel.setShortsBlocker(it) },
                accentColor = DangerRed
            )

            Spacer(modifier = Modifier.height(12.dp))

            // YouTube Study Mode
            BlockerToggleCard(
                title = "YouTube Study Mode",
                subtitle = "Disables homepage recommendation algorithm to prevent rabbit holes.",
                isChecked = isYtStudyEnabled,
                onCheckedChange = { viewModel.setYouTubeStudyMode(it) },
                accentColor = WarningAmber
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Website & Adult Content Filter
            BlockerToggleCard(
                title = "Website & Adult Content Blocker",
                subtitle = "Blocks adult domains, gambling, and web short-form feeds across browsers.",
                isChecked = isWebsiteEnabled,
                onCheckedChange = { viewModel.setWebsiteBlocker(it) },
                accentColor = AccentBlue
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "APPS TO BLOCK DURING FOCUS SESSIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(appRules) { rule ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = rule.appName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (rule.blockReelsOnly) "Reels & Shorts filtered" else "Entire app blocked during session",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = rule.isBlockedDuringFocus,
                        onCheckedChange = { viewModel.toggleAppBlock(rule) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PrimaryEmerald,
                            checkedTrackColor = PrimaryEmerald.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun BlockerToggleCard(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color
) {
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
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accentColor,
                    checkedTrackColor = accentColor.copy(alpha = 0.5f)
                )
            )
        }
    }
}
