package com.aspirant.focus.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.aspirant.focus.AspirantApplication
import com.aspirant.focus.MainActivity
import com.aspirant.focus.overlay.InterventionOverlayManager
import com.aspirant.focus.service.FocusSessionService
import com.aspirant.focus.util.ActiveBlockNotice
import kotlinx.coroutines.*
import java.util.Locale

class AspirantAccessibilityService : AccessibilityService() {

    private lateinit var overlayManager: InterventionOverlayManager
    private val prefs by lazy { AspirantApplication.instance.preferencesManager }
    private val handler = Handler(Looper.getMainLooper())
    private var lastInterventionTime = 0L
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var blockedAppPackages = setOf<String>()
    private var whitelistedAppPackages = setOf<String>()
    private val approvedIntentionPackages = mutableMapOf<String, Long>()

    fun isCurfewActive(): Boolean {
        if (!prefs.isCurfewEnabled) return false
        val cal = java.util.Calendar.getInstance()
        val curMin = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        val startMin = prefs.curfewStartHour * 60 + prefs.curfewStartMinute
        val endMin = prefs.curfewEndHour * 60 + prefs.curfewEndMinute

        return if (startMin > endMin) {
            curMin >= startMin || curMin < endMin
        } else {
            curMin in startMin until endMin
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlayManager = InterventionOverlayManager(this)
        com.aspirant.focus.service.BlockerProtectionService.start(this)

        val db = AspirantApplication.instance.database
        serviceScope.launch {
            db.focusDao().getAllAppRules().collect { rules ->
                blockedAppPackages = rules.filter { it.isBlockedDuringFocus && !it.blockReelsOnly }
                    .map { it.packageName }
                    .toSet()
                whitelistedAppPackages = rules.filter { it.isWhitelistedForStudy }
                    .map { it.packageName }
                    .toSet()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        try {
            // Check & Auto-clear expired strict mode
            if (prefs.isStrictModeActive) {
                val now = System.currentTimeMillis()
                val isSessionActive = FocusSessionService.sessionState.value.isActive
                if (!isSessionActive && (prefs.strictSessionEndTimeMillis == 0L || now >= prefs.strictSessionEndTimeMillis)) {
                    prefs.isStrictModeActive = false
                }
            }

            // 0. Curfew Guard (Bedtime & Morning Lock)
            // Hard-locks all social media, entertainment, distracting apps, and user-blocked apps
            if (isCurfewActive() &&
                packageName != applicationContext.packageName &&
                packageName != "com.android.systemui" &&
                (DEFAULT_DISTRACTING_PACKAGES.contains(packageName) ||
                 blockedAppPackages.contains(packageName) ||
                 packageName == PKG_INSTAGRAM ||
                 packageName == PKG_INSTAGRAM_LITE ||
                 packageName == PKG_YOUTUBE)
            ) {
                val curfewEndTime = String.format(Locale.getDefault(), "%02d:%02d", prefs.curfewEndHour, prefs.curfewEndMinute)
                val title = "🌙 Bedtime Curfew Active"
                val msg = "Sleep & morning momentum protected. Distracting apps are locked until $curfewEndTime."

                ActiveBlockNotice.show(title, msg, "CURFEW")
                reForegroundAspirantFocus("CURFEW")
                performGlobalAction(GLOBAL_ACTION_HOME)
                return
            }

            // 0.05 YouTube Web Curfew Check (Blocks youtube.com in browsers during bedtime curfew)
            if (isCurfewActive() && isBrowserPackage(packageName)) {
                val root = rootInActiveWindow
                if (root != null) {
                    val isYouTubeWeb = containsNodeMatching(root) { node ->
                        val text = node.text?.toString()?.lowercase() ?: ""
                        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
                        text.contains("youtube.com") || desc.contains("youtube.com") ||
                        text.contains("m.youtube.com") || desc.contains("m.youtube.com") ||
                        text.contains("youtu.be") || desc.contains("youtu.be")
                    }
                    if (isYouTubeWeb) {
                        val curfewEndTime = String.format(Locale.getDefault(), "%02d:%02d", prefs.curfewEndHour, prefs.curfewEndMinute)
                        val title = "🌙 YouTube Web Blocked"
                        val msg = "YouTube is locked until $curfewEndTime to protect your sleep."
                        ActiveBlockNotice.show(title, msg, "CURFEW")
                        reForegroundAspirantFocus("CURFEW")
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        return
                    }
                }
            }

        // 0.1 Strict Mode Kiosk Lockout (Cannot minimize app, cannot switch to unauthorized apps)
        val isEmergencyAccess = System.currentTimeMillis() < prefs.emergencyAccessEndTimeMillis
        if (prefs.isStrictModeActive && prefs.isKioskLockEnabled && !isEmergencyAccess) {
            val isAspirantApp = packageName == applicationContext.packageName
            val isSystemUi = packageName == "com.android.systemui"
            val isWhitelistedTool = prefs.isStudyToolsWhitelisted &&
                    (WHITELISTED_STUDY_TOOLS.contains(packageName) || whitelistedAppPackages.contains(packageName))

            if (!isAspirantApp && !isSystemUi && !isWhitelistedTool) {
                val title = "🔒 Focus Locked"
                val msg = "Strict Mode is active. Aspirant Focus is pinned until timer ends or Emergency Access is used."
                ActiveBlockNotice.show(title, msg, "STRICT_KIOSK")
                reForegroundAspirantFocus("STRICT_KIOSK")
                performGlobalAction(GLOBAL_ACTION_HOME)
                return
            }
        }

        // 0.2 Dynamic App Blocking (Blocks any app added to the blocker during focus)
        if (FocusSessionService.sessionState.value.isActive &&
            packageName != applicationContext.packageName &&
            packageName != "com.android.systemui" &&
            blockedAppPackages.contains(packageName)
        ) {
            val title = "App Blocked During Focus!"
            val msg = "Active study session for ${FocusSessionService.sessionState.value.subject}. Stay locked in."
            ActiveBlockNotice.show(title, msg, "APP_BLOCKED")
            reForegroundAspirantFocus("APP_BLOCKED")
            performGlobalAction(GLOBAL_ACTION_HOME)
            return
        }

        // 0.3 Intention Pause (10s breath gate outside of focus mode)
        if (!FocusSessionService.sessionState.value.isActive &&
            !isCurfewActive() &&
            prefs.isIntentionPauseEnabled &&
            packageName != applicationContext.packageName &&
            blockedAppPackages.contains(packageName)
        ) {
            val lastApproved = approvedIntentionPackages[packageName] ?: 0L
            if (System.currentTimeMillis() - lastApproved > 5 * 60 * 1000) {
                val now = System.currentTimeMillis()
                if (now - lastInterventionTime > 3000) {
                    lastInterventionTime = now
                    val appName = when (packageName) {
                        PKG_INSTAGRAM, PKG_INSTAGRAM_LITE -> "Instagram"
                        PKG_YOUTUBE -> "YouTube"
                        else -> "Distracting App"
                    }
                    overlayManager.showIntentionPause(
                        appName = appName,
                        onProceed = {
                            approvedIntentionPackages[packageName] = System.currentTimeMillis()
                        },
                        onCancel = {
                            performGlobalAction(GLOBAL_ACTION_HOME)
                        }
                    )
                }
            }
        }

        // 1. Strict Mode Anti-Uninstall & Settings Protection Check
        if (prefs.isStrictModeActive) {
            checkStrictProtection(packageName, event)
        }

        // 2. Targeted Instagram Reels Blocker
        if (prefs.isReelsBlockerEnabled && (packageName == PKG_INSTAGRAM || packageName == PKG_INSTAGRAM_LITE)) {
            checkInstagramReels(event)
        }

        // 3. Targeted YouTube Shorts Blocker & Study Mode
        if (packageName == PKG_YOUTUBE) {
            if (prefs.isShortsBlockerEnabled) {
                checkYouTubeShorts(event)
            }
            if (prefs.isYouTubeStudyModeEnabled) {
                checkYouTubeStudyMode(event)
            }
        }

        // 4. Website & Adult Content Filtering (Across all mobile browsers)
        if (prefs.isWebsiteBlockerEnabled && isBrowserPackage(packageName)) {
            checkBrowserSecurityAndContent(packageName)
        }

        // 5. Telegram Adult Channel & Content Filtering (Leaves legitimate study channels fully accessible)
        if (prefs.isWebsiteBlockerEnabled && isTelegramPackage(packageName)) {
            checkTelegramAdultContent()
        }

        // 6. VPN & Proxy Loophole Interceptor (Preserves System-Wide DNS Filter)
        if (prefs.isWebsiteBlockerEnabled && isVpnPackage(packageName)) {
            val title = "🛡️ VPN Bypass Blocked"
            val msg = "VPN and proxy apps are disabled to ensure unbreakable DNS adult filtering."
            ActiveBlockNotice.show(title, msg, "VPN_BLOCKED")
            reForegroundAspirantFocus("VPN_BLOCKED")
            performGlobalAction(GLOBAL_ACTION_HOME)
            performGlobalAction(GLOBAL_ACTION_BACK)
            return
        }

        // 7. Google Play Store Search Gatekeeper (Blocks searching for alternative browsers, VPNs, or explicit keywords)
        if (prefs.isWebsiteBlockerEnabled && packageName == PKG_PLAY_STORE) {
            checkPlayStoreSecurity()
        }

        // 8. In-App Webview Guardian (Scans Custom Tabs and embedded webviews in 3rd party apps)
        if (prefs.isWebsiteBlockerEnabled && !isBrowserPackage(packageName) && !isTelegramPackage(packageName) &&
            packageName != applicationContext.packageName && packageName != "com.android.systemui") {
            checkInAppWebviewSecurity()
        }
    } catch (t: Throwable) {
        // Prevent service from crashing on transient node recycling or OS window changes
    }
}

    override fun onInterrupt() {}

    private fun reForegroundAspirantFocus(reason: String) {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("BLOCK_REASON", reason)
            }
            startActivity(intent)
        } catch (e: Exception) {
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    // --- Strict Protection (Anti-Uninstall & Settings Bypass Lock) ---
    private fun checkStrictProtection(packageName: String, event: AccessibilityEvent) {
        // Prevent Uninstallation
        if (INSTALLER_PACKAGES.contains(packageName)) {
            val root = rootInActiveWindow ?: return
            if (containsTextIgnoreCase(root, "Aspirant Focus") &&
                (containsTextIgnoreCase(root, "uninstall") || containsTextIgnoreCase(root, "delete"))
            ) {
                ActiveBlockNotice.show("Uninstall Blocked!", "Strict Mode is active. Complete your study session first!", "STRICT_PROTECTION")
                reForegroundAspirantFocus("STRICT_PROTECTION")
                performGlobalAction(GLOBAL_ACTION_HOME)
                return
            }
        }

        // Prevent Settings Tampering (Force stop / disable accessibility)
        if (packageName == PKG_SETTINGS && prefs.isSettingsLockEnabled) {
            val root = rootInActiveWindow ?: return
            if (containsTextIgnoreCase(root, "Aspirant Focus") ||
                containsTextIgnoreCase(root, "Accessibility") ||
                containsTextIgnoreCase(root, "Force stop")
            ) {
                ActiveBlockNotice.show("Settings Locked!", "Settings are locked during Strict Mode focus session.", "SETTINGS_LOCKED")
                reForegroundAspirantFocus("SETTINGS_LOCKED")
                performGlobalAction(GLOBAL_ACTION_HOME)
                return
            }
        }
    }

    // --- Instagram Reels Interceptor ---
    private fun checkInstagramReels(event: AccessibilityEvent) {
        val root = rootInActiveWindow ?: return

        val isReelsViewing = containsNodeMatching(root) { node ->
            val id = node.viewIdResourceName?.lowercase() ?: ""
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            val text = node.text?.toString()?.lowercase() ?: ""

            id.contains("reel_viewer") ||
            id.contains("clips_video_container") ||
            (id.contains("reels_tab") && node.isSelected) ||
            desc.contains("reel by") ||
            (text == "reels" && node.isSelected)
        }

        if (isReelsViewing) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            handler.post {
                android.widget.Toast.makeText(
                    applicationContext,
                    "🎯 Reels blocked. Focus on your goals!",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // --- YouTube Shorts Interceptor ---
    private fun checkYouTubeShorts(event: AccessibilityEvent) {
        val root = rootInActiveWindow ?: return

        val isShorts = containsNodeMatching(root) { node ->
            val id = node.viewIdResourceName?.lowercase() ?: ""
            id.contains("shorts_player") ||
            id.contains("reel_recycler") ||
            id.contains("shorts_video_player") ||
            id.contains("shorts_container") ||
            id.contains("reel_watch_fragment") ||
            (id.contains("pivot_bar_shorts") && node.isSelected)
        }

        if (isShorts) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            handler.post {
                android.widget.Toast.makeText(
                    applicationContext,
                    "🎯 Shorts blocked. Stick to educational videos!",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // --- YouTube Study Mode ---
    private fun checkYouTubeStudyMode(event: AccessibilityEvent) {
        val root = rootInActiveWindow ?: return

        val isHomeFeed = containsNodeMatching(root) { node ->
            val id = node.viewIdResourceName?.lowercase() ?: ""
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            (id.contains("pivot_bar") && desc.contains("home")) || id.contains("feed_swipe_refresh")
        }

        if (isHomeFeed) {
            handler.post {
                android.widget.Toast.makeText(
                    applicationContext,
                    "YouTube Study Mode: Search directly for lectures",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // --- Browser Security & Adult Content Filtering ---
    private fun checkBrowserSecurityAndContent(packageName: String) {
        val root = rootInActiveWindow ?: return

        // 1. Incognito & Private Tab Destroyer (24/7 Zero Loophole)
        // Eliminates Incognito/Private tabs across Brave, Chrome, Firefox, Samsung, Edge, Opera, etc.
        val isIncognito = containsNodeMatching(root) { node ->
            val text = node.text?.toString()?.lowercase() ?: ""
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            val id = node.viewIdResourceName?.lowercase() ?: ""

            // Strict Multi-word Private / Incognito phrases ONLY (Zero false positives on normal words like "history", "tutorial", "doctor")
            text.contains("new incognito tab") || desc.contains("new incognito tab") ||
            text.contains("close incognito tab") || desc.contains("close incognito tab") ||
            text.contains("incognito tab") || desc.contains("incognito tab") ||
            text.contains("incognito mode") || desc.contains("incognito mode") ||
            text.contains("gone incognito") || desc.contains("gone incognito") ||
            id.contains("incognito_icon") || id.contains("incognito_splash") ||

            text.contains("new private tab") || desc.contains("new private tab") ||
            text.contains("close private tab") || desc.contains("close private tab") ||
            text.contains("private tab") || desc.contains("private tab") ||
            text.contains("private tabs") || desc.contains("private tabs") ||
            text.contains("private mode") || desc.contains("private mode") ||
            text.contains("private browsing") || desc.contains("private browsing") ||
            text.contains("switch to private") || desc.contains("switch to private") ||

            text.contains("private with tor") || desc.contains("private with tor") ||
            text.contains("new tor tab") || desc.contains("new tor tab") ||
            text.contains("close tor tab") || desc.contains("close tor tab") ||
            text.contains("tor tab") || desc.contains("tor tab") ||

            text.contains("secret mode") || desc.contains("secret mode") ||
            text.contains("inprivate tab") || desc.contains("inprivate tab") ||
            text.contains("inprivate browsing") || desc.contains("inprivate browsing") ||

            (id.contains("tab_switcher_private_tabs_button") && node.isSelected) ||
            id.contains("new_tab_menu_private") ||
            id.endsWith(":id/incognito_tab")
        }

        if (isIncognito) {
            val title = "🔒 Private Browsing Blocked"
            val msg = "Private & Incognito browsing is strictly disabled 24/7 to prevent adult content loopholes."
            ActiveBlockNotice.show(title, msg, "INCOGNITO")
            reForegroundAspirantFocus("INCOGNITO")
            performGlobalAction(GLOBAL_ACTION_HOME)
            performGlobalAction(GLOBAL_ACTION_BACK)
            return
        }

        // 2. Adult Content & Explicit Keyword Filter (Scans URL bar, search queries, page text)
        val isAdultContent = containsNodeMatching(root) { node ->
            val text = node.text?.toString()?.lowercase() ?: ""
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""

            // 1. Direct match on unambiguous adult terms/domains
            UNAMBIGUOUS_ADULT_KEYWORDS.any { keyword ->
                text.contains(keyword) || desc.contains(keyword)
            } ||
            // 2. Delimited match for terms that may appear as substrings in legitimate words
            DELIMITED_ADULT_KEYWORDS.any { keyword ->
                text.split(" ", "/", ".", "-", "?", "&", "=", "_", "@", "+", ":", ";", "#", ",", "(", ")").any { word ->
                    word == keyword
                } || desc.split(" ", "/", ".", "-", "?", "&", "=", "_", "@", "+", ":", ";", "#", ",", "(", ")").any { word ->
                    word == keyword
                }
            }
        }

        if (isAdultContent) {
            val title = "🛑 Adult Content Blocked"
            val msg = "Access to adult, pornographic, or gambling content is strictly blocked 24/7."
            ActiveBlockNotice.show(title, msg, "ADULT_CONTENT")
            reForegroundAspirantFocus("ADULT_CONTENT")
            performGlobalAction(GLOBAL_ACTION_HOME)
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    private fun isBrowserPackage(packageName: String): Boolean {
        if (BROWSER_PACKAGES.contains(packageName)) return true
        val lower = packageName.lowercase()
        return lower.contains("browser") || lower.contains("chrome") || lower.contains("firefox") || lower.contains("opera") || lower.contains("brave")
    }

    private fun isTelegramPackage(packageName: String): Boolean {
        return TELEGRAM_PACKAGES.contains(packageName) || packageName.lowercase().contains("telegram") || packageName.lowercase().contains("challegram")
    }

    private fun checkTelegramAdultContent() {
        val root = rootInActiveWindow ?: return

        val isAdultDetected = containsNodeMatching(root) { node ->
            val text = node.text?.toString()?.lowercase() ?: ""
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""

            // 1. Direct match on unambiguous adult keywords & channel indicators
            TELEGRAM_UNAMBIGUOUS_KEYWORDS.any { keyword ->
                text.contains(keyword) || desc.contains(keyword)
            } ||
            // 2. Delimited match for short tags & keywords (e.g. "18+", "mms", "leaks", "bhabhi", "anal", "escort")
            TELEGRAM_DELIMITED_KEYWORDS.any { keyword ->
                text.split(" ", "/", ".", "-", "?", "&", "=", "_", "@", "+", ":", ";", "#", ",", "(", ")", "[", "]").any { word ->
                    word == keyword
                } || desc.split(" ", "/", ".", "-", "?", "&", "=", "_", "@", "+", ":", ";", "#", ",", "(", ")", "[", "]").any { word ->
                    word == keyword
                }
            }
        }

        if (isAdultDetected) {
            val title = "🛑 Telegram Adult Content Blocked"
            val msg = "Adult channels, groups, and explicit searches in Telegram are strictly blocked 24/7."
            ActiveBlockNotice.show(title, msg, "ADULT_CONTENT")
            reForegroundAspirantFocus("ADULT_CONTENT")
            performGlobalAction(GLOBAL_ACTION_HOME)
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    private fun isVpnPackage(packageName: String): Boolean {
        if (VPN_PACKAGES.contains(packageName)) return true
        val lower = packageName.lowercase()
        return (lower.contains("vpn") || lower.contains("proxy") || lower.contains("wireguard") || lower.contains("openvpn")) &&
                !lower.contains("aspirant")
    }

    private fun checkPlayStoreSecurity() {
        val root = rootInActiveWindow ?: return

        val isProhibitedSearch = containsNodeMatching(root) { node ->
            val text = node.text?.toString()?.lowercase() ?: ""
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""

            // Searches for bypass tools or private browsers
            PLAY_STORE_RESTRICTED_KEYWORDS.any { keyword ->
                text.contains(keyword) || desc.contains(keyword)
            } ||
            // Or searching explicit adult queries in Play Store
            UNAMBIGUOUS_ADULT_KEYWORDS.any { keyword ->
                text.contains(keyword) || desc.contains(keyword)
            }
        }

        if (isProhibitedSearch) {
            val title = "🛑 Play Store Search Blocked"
            val msg = "Searching for VPNs, alternative private browsers, or explicit terms is blocked."
            ActiveBlockNotice.show(title, msg, "PLAY_STORE_RESTRICTED")
            reForegroundAspirantFocus("PLAY_STORE_RESTRICTED")
            performGlobalAction(GLOBAL_ACTION_HOME)
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    private fun checkInAppWebviewSecurity() {
        val root = rootInActiveWindow ?: return

        // Check if current screen contains a webview or custom tab
        val hasWebview = containsNodeMatching(root) { node ->
            val className = node.className?.toString() ?: ""
            val id = node.viewIdResourceName?.lowercase() ?: ""
            className.contains("WebView", ignoreCase = true) ||
            id.contains("custom_tab") ||
            id.contains("url_bar") ||
            id.contains("location_bar")
        }

        if (!hasWebview) return

        // If a webview is present, scan for adult content / explicit domains
        val isAdultDetected = containsNodeMatching(root) { node ->
            val text = node.text?.toString()?.lowercase() ?: ""
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""

            UNAMBIGUOUS_ADULT_KEYWORDS.any { keyword ->
                text.contains(keyword) || desc.contains(keyword)
            } ||
            DELIMITED_ADULT_KEYWORDS.any { keyword ->
                text.split(" ", "/", ".", "-", "?", "&", "=", "_", "@", "+", ":", ";", "#", ",", "(", ")").any { word ->
                    word == keyword
                } || desc.split(" ", "/", ".", "-", "?", "&", "=", "_", "@", "+", ":", ";", "#", ",", "(", ")").any { word ->
                    word == keyword
                }
            }
        }

        if (isAdultDetected) {
            val title = "🛑 Adult Webview Blocked"
            val msg = "Adult content loaded inside this application has been strictly blocked."
            ActiveBlockNotice.show(title, msg, "ADULT_CONTENT")
            reForegroundAspirantFocus("ADULT_CONTENT")
            performGlobalAction(GLOBAL_ACTION_HOME)
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    // --- Node Helpers ---
    private fun containsNodeMatching(
        node: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): Boolean {
        try {
            if (predicate(node)) return true
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                if (containsNodeMatching(child, predicate)) {
                    return true
                }
            }
        } catch (_: Throwable) {
            // Protect against recycled nodes / transient view changes
        }
        return false
    }

    private fun containsTextIgnoreCase(node: AccessibilityNodeInfo, target: String): Boolean {
        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()
        if (text?.contains(target, ignoreCase = true) == true || desc?.contains(target, ignoreCase = true) == true) {
            return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (containsTextIgnoreCase(child, target)) {
                return true
            }
        }
        return false
    }

    private fun throttleIntervention(title: String, msg: String, isStrict: Boolean = false) {
        // Disabled: No window overlays to prevent freeze traps.
    }

    companion object {
        const val PKG_INSTAGRAM = "com.instagram.android"
        const val PKG_INSTAGRAM_LITE = "com.instagram.lite"
        const val PKG_YOUTUBE = "com.google.android.youtube"
        const val PKG_SETTINGS = "com.android.settings"

        val DEFAULT_DISTRACTING_PACKAGES = setOf(
            "com.instagram.android",
            "com.instagram.lite",
            "com.google.android.youtube",
            "com.google.android.youtube.tv",
            "com.snapchat.android",
            "com.twitter.android",
            "com.twitter.android.lite",
            "com.facebook.katana",
            "com.facebook.lite",
            "com.facebook.orca",
            "com.reddit.frontpage",
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "com.netflix.mediaclient",
            "com.amazon.avod.thirdpartyclient",
            "in.startv.hotstar",
            "tv.twitch.android.app",
            "com.pinterest",
            "com.discord",
            "org.telegram.messenger",
            "org.telegram.messenger.web",
            "org.telegram.messenger.beta",
            "org.telegram.plus",
            "org.telegram.BETA",
            "org.thunderdog.challegram",
            "nekox.messenger",
            "org.forkclient.messenger"
        )

        val TELEGRAM_PACKAGES = setOf(
            "org.telegram.messenger",
            "org.telegram.messenger.web",
            "org.telegram.messenger.beta",
            "org.telegram.plus",
            "org.telegram.BETA",
            "org.thunderdog.challegram",
            "nekox.messenger",
            "org.forkclient.messenger"
        )

        const val PKG_PLAY_STORE = "com.android.vending"

        val INSTALLER_PACKAGES = setOf(
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
            "com.samsung.android.packageinstaller"
        )

        val VPN_PACKAGES = setOf(
            "com.cloudflare.onedotonedotonedotone",
            "free.vpn.unblock.proxy.turbovpn",
            "free.vpn.unblock.proxy.turbovpn.lite",
            "com.quarkos.turbovpn",
            "ch.protonvpn.android",
            "com.nordvpn.android",
            "com.expressvpn.vpn",
            "com.surfshark.vpnclient.android",
            "com.windscribe.vpn",
            "hotspotshield.android.vpn",
            "com.fast.free.unblock.secure.vpn",
            "org.torproject.torbrowser",
            "org.torproject.android",
            "com.psiphon3.subscription",
            "com.psiphon3",
            "com.tunnelbear.android",
            "com.cyberghostro.vpn",
            "com.vpnhub.android",
            "com.purevpn.android",
            "com.securevpn.free",
            "com.freevpn.android",
            "com.wireguard.android",
            "de.blinkt.openvpn",
            "net.openvpn.openvpn"
        )

        val PLAY_STORE_RESTRICTED_KEYWORDS = listOf(
            "vpn", "proxy", "tor browser", "private browser", "incognito browser",
            "secret browser", "hide ip", "turbovpn", "protonvpn", "nordvpn",
            "supervpn", "psiphon", "orbot", "unblock proxy"
        )

        val BROWSER_PACKAGES = setOf(
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.canary",
            "com.chrome.dev",
            "org.mozilla.firefox",
            "org.mozilla.firefox_beta",
            "org.mozilla.focus",
            "org.mozilla.fenix",
            "com.brave.browser",
            "com.brave.browser_beta",
            "com.brave.browser_nightly",
            "com.brave.browser_dev",
            "com.opera.browser",
            "com.opera.mini.native",
            "com.opera.touch",
            "com.opera.gx",
            "com.microsoft.emmx",
            "com.duckduckgo.mobile.android",
            "com.sec.android.app.sbrowser",
            "com.sec.android.app.sbrowser.beta",
            "com.mi.globalbrowser",
            "com.android.browser",
            "com.vivo.browser",
            "com.heytap.browser",
            "com.coloros.browser",
            "com.UCMobile.intl",
            "com.uc.browser.en",
            "com.vivaldi.browser",
            "com.kiwibrowser.browser",
            "org.torproject.torbrowser"
        )

        // 1. Unambiguous Adult Terms & Major Tubes (Zero benign occurrence in English language)
        val UNAMBIGUOUS_ADULT_KEYWORDS = listOf(
            // Top Adult Sites & Tubes
            "pornhub", "xvideos", "xnxx", "xhamster", "redtube", "youporn", "brazzers",
            "spankbang", "chaturbate", "stripchat", "onlyfans", "erome", "hentai",
            "beeg", "eporner", "tnaflix", "tube8", "drtuber", "nuvid", "pornhat",
            "hqporner", "daftsex", "motherless", "txxx", "rule34", "gelbooru",
            "danbooru", "e621", "camsoda", "bongacams", "myfreecams", "livejasmin",
            "manyvids", "fansly", "adultfriendfinder", "cam4", "camwhores",
            "fapello", "faphero", "coomer", "kemono", "javhd", "javlibrary",
            "missav", "jable", "avgle", "spankwire", "heavy-r", "kaotic",

            // Indian & Regional Explicit Search Terms & Domains
            "desiporn", "desisex", "indianporn", "bhabhiporn", "malluporn", "auntyporn",
            "choot", "antichut", "fulldesi", "savita bhabhi", "desi mms", "desi leak",
            "antarvasna", "sex stories", "indian sex", "desi sex", "sex story",
            "kamababa", "chudai", "sex video", "sexy video",

            // Explicit Adult Roots (Cannot appear inside standard educational words)
            "porn", "xxx", "nsfw", "camgirl", "camgirls", "blowjob", "creampie",
            "gangbang", "threesome", "shemale", "ladyboy", "masturbat", "orgasm",
            "vibrator", "dildo", "playboy", "penthouse", "fetish", "erotic",
            "nude", "nudes", "naked", "uncensored"
        )

        // 2. Delimited Adult & Gambling Keywords (Checked with word-boundary splitting to avoid false positives)
        val DELIMITED_ADULT_KEYWORDS = listOf(
            "sex", "sexy", "anal", "boobs", "pussy", "milf", "escort", "escorts", "fap",
            "bdsm", "incest", "leaks", "leak", "mms", "scandal", "bhabhi", "chudai",
            // Gambling & Betting
            "bet365", "1xbet", "stake.com", "parimatch", "betway", "dafabet",
            "melbet", "22bet", "casino", "roulette", "blackjack", "gambling"
        )

        // 3. Telegram-Specific Unambiguous Adult Channel & Content Identifiers
        val TELEGRAM_UNAMBIGUOUS_KEYWORDS = UNAMBIGUOUS_ADULT_KEYWORDS + listOf(
            "adultchannel", "adultgroup", "terabox 18+", "mega 18+", "mega leak",
            "sex video", "hot video", "sexy video", "viral mms", "desiporn", "desi mms",
            "sex stories", "indian sex", "desi sex"
        )

        // 4. Telegram-Specific Delimited Identifiers (Tags, Channel Names, Searches)
        val TELEGRAM_DELIMITED_KEYWORDS = listOf(
            "18+", "+18", "nsfw", "mms", "leak", "leaks", "bhabhi", "scandal", "sex", "sexy",
            "anal", "boobs", "pussy", "milf", "escort", "fap", "bdsm", "choot", "nude", "nudes", "chudai"
        )

        val WHITELISTED_STUDY_TOOLS = setOf(
            "com.google.android.calculator",
            "com.android.calculator2",
            "com.sec.android.app.popupcalculator",
            "com.miui.calculator",
            "com.coloros.calculator",
            "com.adobe.reader",
            "com.google.android.apps.pdfviewer",
            "com.google.android.apps.docs",
            "us.zoom.videomeetings",
            "com.google.android.apps.meetings",
            "org.anki4android",
            "com.notion.id"
        )
    }
}
