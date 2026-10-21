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
            throttleIntervention(title, msg, isStrict = true)
            return
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
                throttleIntervention(title, msg, isStrict = true)
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
            throttleIntervention(title, msg)
            return
        }

        // 0.3 Intention Pause (10s breath gate outside of focus mode)
        if (!FocusSessionService.sessionState.value.isActive &&
            !isCurfewActive() &&
            prefs.isIntentionPauseEnabled &&
            packageName != applicationContext.packageName &&
            (blockedAppPackages.contains(packageName) || packageName == PKG_INSTAGRAM || packageName == PKG_YOUTUBE)
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
                throttleIntervention("Uninstall Blocked!", "Strict Mode is active. Complete your study session first!", isStrict = true)
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
                throttleIntervention("Settings Locked!", "Settings are locked during Strict Mode focus session.", isStrict = true)
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
            id.contains("reels_tab") ||
            (desc.contains("reel by") || desc.contains("reels tab") || text == "reels")
        }

        if (isReelsViewing) {
            throttleIntervention(
                "Instagram Reels Blocked!",
                "15-second loops destroy your attention span. Focus on your goals."
            )
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    // --- YouTube Shorts Interceptor ---
    private fun checkYouTubeShorts(event: AccessibilityEvent) {
        val root = rootInActiveWindow ?: return

        val isShorts = containsNodeMatching(root) { node ->
            val id = node.viewIdResourceName?.lowercase() ?: ""
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            val text = node.text?.toString()?.lowercase() ?: ""

            id.contains("shorts_player") ||
            id.contains("reel_recycler") ||
            id.contains("pivot_bar_shorts") ||
            desc.contains("shorts") ||
            text.equals("shorts", ignoreCase = true)
        }

        if (isShorts) {
            throttleIntervention(
                "YouTube Shorts Blocked!",
                "Keep YouTube strictly for lectures, not endless short-form loops."
            )
            performGlobalAction(GLOBAL_ACTION_BACK)
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
            throttleIntervention(
                "YouTube Study Mode Active",
                "Search only for educational lectures. Homepage recommendations are disabled."
            )
        }
    }

    // --- Browser Security & Adult Content Filtering ---
    private fun checkBrowserSecurityAndContent(packageName: String) {
        val root = rootInActiveWindow ?: return

        // 1. Incognito / Private Browsing Blocker
        // Blocks opening or using Incognito/Private tabs across Chrome, Samsung Internet, Firefox, Edge, etc.
        val isIncognito = containsNodeMatching(root) { node ->
            val text = node.text?.toString()?.lowercase() ?: ""
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            val id = node.viewIdResourceName?.lowercase() ?: ""

            text.contains("incognito") || desc.contains("incognito") || id.contains("incognito") ||
            text.contains("secret mode") || desc.contains("secret mode") ||
            text.contains("private tab") || desc.contains("private tab") ||
            text.contains("inprivate") || desc.contains("inprivate") ||
            text.contains("private browsing") || desc.contains("private browsing") ||
            desc.contains("new incognito tab") || text.contains("close incognito tabs")
        }

        if (isIncognito) {
            val title = "🔒 Incognito / Private Mode Blocked"
            val msg = "Private browsing is strictly disabled to prevent adult content loopholes."
            ActiveBlockNotice.show(title, msg, "INCOGNITO")
            reForegroundAspirantFocus("INCOGNITO")
            performGlobalAction(GLOBAL_ACTION_HOME)
            performGlobalAction(GLOBAL_ACTION_BACK)
            throttleIntervention(title, msg, isStrict = true)
            return
        }

        // 2. Adult Content & Explicit Keyword Filter (Scans URL bar, search queries, page text)
        val isAdultContent = containsNodeMatching(root) { node ->
            val text = node.text?.toString()?.lowercase() ?: ""
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            val id = node.viewIdResourceName?.lowercase() ?: ""

            val isUrlBar = id.contains("url") || id.contains("location") || id.contains("address") || id.contains("search") || id.contains("omnibox")

            ADULT_KEYWORDS_AND_DOMAINS.any { keyword ->
                if (isUrlBar) {
                    text.contains(keyword) || desc.contains(keyword)
                } else {
                    // Check words separated by spaces or punctuation
                    text.split(" ", "/", ".", "-", "?", "&", "=").any { word ->
                        word == keyword || (keyword.length >= 4 && word.contains(keyword))
                    }
                }
            }
        }

        if (isAdultContent) {
            val title = "🛑 Adult Content Blocked"
            val msg = "Access to adult, pornographic, or gambling content is strictly blocked on this device."
            ActiveBlockNotice.show(title, msg, "ADULT_CONTENT")
            reForegroundAspirantFocus("ADULT_CONTENT")
            performGlobalAction(GLOBAL_ACTION_HOME)
            performGlobalAction(GLOBAL_ACTION_BACK)
            throttleIntervention(title, msg, isStrict = true)
        }
    }

    private fun isBrowserPackage(packageName: String): Boolean {
        if (BROWSER_PACKAGES.contains(packageName)) return true
        val lower = packageName.lowercase()
        return lower.contains("browser") || lower.contains("chrome") || lower.contains("firefox") || lower.contains("opera")
    }

    // --- Node Helpers ---
    private fun containsNodeMatching(
        node: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): Boolean {
        if (predicate(node)) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (containsNodeMatching(child, predicate)) {
                return true
            }
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
        val now = System.currentTimeMillis()
        if (now - lastInterventionTime > 1500) {
            lastInterventionTime = now
            overlayManager.showIntervention(title, msg, isStrictLockout = isStrict)
        }
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
            "com.discord"
        )

        val INSTALLER_PACKAGES = setOf(
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
            "com.samsung.android.packageinstaller"
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
            "com.opera.browser",
            "com.opera.mini.native",
            "com.opera.touch",
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

        val ADULT_KEYWORDS_AND_DOMAINS = listOf(
            // Top Adult Sites & Tubes
            "pornhub", "xvideos", "xnxx", "xhamster", "redtube", "youporn", "brazzers",
            "spankbang", "chaturbate", "stripchat", "onlyfans", "erome", "hentai",
            "beeg", "eporner", "tnaflix", "tube8", "drtuber", "nuvid", "pornhat",
            "hqporner", "daftsex", "motherless", "txxx", "rule34", "gelbooru",
            "danbooru", "e621", "camsoda", "bongacams", "myfreecams", "livejasmin",
            "manyvids", "fansly", "adultfriendfinder", "cam4", "camwhores",
            "fapello", "faphero", "coomer", "kemono", "javhd", "javlibrary",
            "missav", "jable", "avgle", "spankwire", "heavy-r", "kaotic",

            // Explicit Adult Keywords
            "porn", "xxx", "nude", "nudes", "naked", "nsfw", "erotic",
            "fetish", "camgirl", "camgirls", "escort", "escorts",
            "boobs", "pussy", "milf", "anal", "blowjob", "creampie",
            "hardcore", "softcore", "playboy", "penthouse", "fap",
            "masturbat", "orgasm", "dildo", "vibrator", "bdsm",
            "gangbang", "threesome", "shemale", "ladyboy", "incest",

            // Gambling & Betting
            "bet365", "1xbet", "stake.com", "parimatch", "betway", "dafabet",
            "melbet", "22bet", "lottery", "casino", "roulette", "blackjack"
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
