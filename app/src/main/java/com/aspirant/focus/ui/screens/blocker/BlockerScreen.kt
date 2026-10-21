package com.aspirant.focus.ui.screens.blocker

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aspirant.focus.data.model.AppBlockRule
import com.aspirant.focus.ui.theme.*

@Composable
fun BlockerScreen(viewModel: BlockerViewModel = viewModel()) {
    val isReelsEnabled by viewModel.isReelsBlockerEnabled.collectAsState()
    val isShortsEnabled by viewModel.isShortsBlockerEnabled.collectAsState()
    val isYtStudyEnabled by viewModel.isYouTubeStudyModeEnabled.collectAsState()
    val isWebsiteEnabled by viewModel.isWebsiteBlocker.collectAsState()
    val appRules by viewModel.appRules.collectAsState()
    val availableApps by viewModel.availableAppsToAdd.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

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

            Spacer(modifier = Modifier.height(16.dp))

            // 24/7 System-Wide Adult DNS Shield Card
            SystemWideDnsShieldCard()

            Spacer(modifier = Modifier.height(28.dp))

            // Section Header with + Add App Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "APPS TO BLOCK DURING FOCUS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Button(
                    onClick = {
                        viewModel.updateSearchQuery("")
                        viewModel.loadInstalledApps()
                        showAddDialog = true
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add App", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // List of Blocked Apps
        items(appRules, key = { it.packageName }) { rule ->
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

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = rule.isBlockedDuringFocus,
                            onCheckedChange = { viewModel.toggleAppBlock(rule) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PrimaryEmerald,
                                checkedTrackColor = PrimaryEmerald.copy(alpha = 0.5f)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.removeApp(rule) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Add App Dialog Modal
    if (showAddDialog) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Add App to Blocker",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        IconButton(onClick = { showAddDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search Input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Search installed apps...", fontSize = 13.sp, color = TextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryEmerald,
                            unfocusedBorderColor = SurfaceCard,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // App Selection List
                    if (availableApps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No more apps to add",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(availableApps) { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addApp(app)
                                            showAddDialog = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = app.appName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = app.packageName,
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add",
                                        tint = PrimaryEmerald
                                    )
                                }
                                Divider(color = Color.White.copy(alpha = 0.04f))
                            }
                        }
                    }
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

@Composable
fun SystemWideDnsShieldCard() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var copiedDns by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryEmerald.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🛡️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "System-Wide Adult DNS Shield",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryEmerald.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "24/7 ZERO LOOPHOLE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryEmerald,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Permanently blocks adult websites, private/incognito tabs, and bypasses at the Android network layer across all browsers (including Brave) and apps.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // CleanBrowsing DNS option (Recommended)
            DnsProviderRow(
                name = "CleanBrowsing Family (Recommended)",
                hostname = "adult-filter-dns.cleanbrowsing.org",
                details = "Blocks porn, adult sites & enforces SafeSearch 24/7",
                isCopied = copiedDns == "adult-filter-dns.cleanbrowsing.org",
                onCopy = {
                    clipboardManager.setText(AnnotatedString("adult-filter-dns.cleanbrowsing.org"))
                    copiedDns = "adult-filter-dns.cleanbrowsing.org"
                    Toast.makeText(context, "CleanBrowsing DNS copied! Ready to paste.", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Cloudflare 1.1.1.3 Family option
            DnsProviderRow(
                name = "Cloudflare 1.1.1.3 Family",
                hostname = "family.cloudflare-dns.com",
                details = "Blocks malware & adult content network-wide",
                isCopied = copiedDns == "family.cloudflare-dns.com",
                onCopy = {
                    clipboardManager.setText(AnnotatedString("family.cloudflare-dns.com"))
                    copiedDns = "family.cloudflare-dns.com"
                    Toast.makeText(context, "Cloudflare DNS copied! Ready to paste.", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Button to open Android Private DNS settings
            Button(
                onClick = {
                    val intents = listOf(
                        Intent("android.settings.NETWORK_OPERATOR_SETTINGS"),
                        Intent(Settings.ACTION_WIRELESS_SETTINGS),
                        Intent(Settings.ACTION_SETTINGS)
                    )
                    var opened = false
                    for (intent in intents) {
                        try {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                            opened = true
                            break
                        } catch (_: Exception) {}
                    }
                    if (!opened) {
                        Toast.makeText(context, "Please open Android Settings -> Network & internet -> Private DNS", Toast.LENGTH_LONG).show()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Open Android DNS Settings",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Setup 3-Step Guide
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundDark.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = "HOW TO ACTIVATE (30 SECONDS):",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "1. Tap 'Copy' on CleanBrowsing hostname above.\n" +
                           "2. Tap 'Open Android DNS Settings' -> Private DNS -> Select 'Private DNS provider hostname'.\n" +
                           "3. Paste hostname & Save. Then lock Settings in Strict Mode so it cannot be disabled!",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
fun DnsProviderRow(
    name: String,
    hostname: String,
    details: String,
    isCopied: Boolean,
    onCopy: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = BackgroundDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = hostname,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryEmerald
                )
                Text(
                    text = details,
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onCopy,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isCopied) PrimaryEmerald.copy(alpha = 0.2f) else Color.Transparent,
                    contentColor = if (isCopied) PrimaryEmerald else TextPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isCopied) PrimaryEmerald else SurfaceCard
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(
                    imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isCopied) "Copied!" else "Copy",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

