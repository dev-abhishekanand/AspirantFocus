package com.aspirant.focus.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.aspirant.focus.AspirantApplication
import com.aspirant.focus.overlay.InterventionOverlayManager
import com.aspirant.focus.service.FocusSessionService
import kotlinx.coroutines.*

class AspirantAccessibilityService : AccessibilityService() {

    private lateinit var overlayManager: InterventionOverlayManager
    private val prefs by lazy { AspirantApplication.instance.preferencesManager }
    private val handler = Handler(Looper.getMainLooper())
    private var lastInterventionTime = 0L
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var blockedAppPackages = setOf<String>()
    private val approvedIntentionPackages = mutableMapOf<String, Long>()

    private fun isCurfewActive(): Boolean {
        if (!prefs.isCurfewEnabled) return false
        val calendar = java.util.Calendar.getInstance()
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(java.util.Calendar.MINUTE)
        val currentTotalMin = currentHour * 60 + currentMinute

        val startTotalMin = prefs.curfewStartHour * 60 + prefs.curfewStartMinute
        val endTotalMin = prefs.curfewEndHour * 60 + prefs.curfewEndMinute

        return if (startTotalMin > endTotalMin) {
            currentTotalMin >= startTotalMin || currentTotalMin < endTotalMin
        } else {
            currentTotalMin in startTotalMin until endTotalMin
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlayManager = InterventionOverlayManager(this)

        // Observe custom blocked apps from database
        val db = AspirantApplication.instance.database
        serviceScope.launch {
            db.focusDao().getAllAppRules().collect { rules ->
                blockedAppPackages = rules.filter { it.isBlockedDuringFocus && !it.blockReelsOnly }
                    .map { it.packageName }
                    .toSet()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        // 0. Curfew Guard (Bedtime & Morning Lock)
        if (isCurfewActive() &&
            packageName != applicationContext.packageName &&
            (blockedAppPackages.contains(packageName) || packageName == PKG_INSTAGRAM || packageName == PKG_YOUTUBE)
        ) {
            val curfewEndTime = String.format("%02d:%02d", prefs.curfewEndHour, prefs.curfewEndMinute)
            throttleIntervention(
                "🌙 Curfew Active",
                "Sleep & morning momentum protected. Distracting apps are locked until $curfewEndTime."
            )
            performGlobalAction(GLOBAL_ACTION_HOME)
            return
        }

        // 0.1 Dynamic App Blocking (Blocks any app added to the blocker during focus)
        if (FocusSessionService.sessionState.value.isActive &&
            packageName != applicationContext.packageName &&
            blockedAppPackages.contains(packageName)
        ) {
            throttleIntervention(
                "App Blocked During Focus!",
                "You have an active study session for ${FocusSessionService.sessionState.value.subject}. Stay locked in."
            )
            performGlobalAction(GLOBAL_ACTION_HOME)
            return
        }

        // 0.2 Intention Pause (10s breath gate outside of focus mode)
        if (!FocusSessionService.sessionState.value.isActive &&
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
                        PKG_INSTAGRAM -> "Instagram"
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

        // 1. Strict Mode & Anti-Uninstall Lockout Check
        if (prefs.isStrictModeActive) {
            checkStrictProtection(packageName, event)
        }

        // 2. Targeted Instagram Reels Blocker
        if (prefs.isReelsBlockerEnabled && packageName == PKG_INSTAGRAM) {
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

        // 4. Website & Adult Content Filtering
        if (prefs.isWebsiteBlockerEnabled && BROWSER_PACKAGES.contains(packageName)) {
            checkBrowserUrl(event)
        }
    }

    override fun onInterrupt() {
        // Required callback when accessibility is interrupted
    }

    // --- Strict Protection (Anti-Uninstall & Settings Bypass Lock) ---
    private fun checkStrictProtection(packageName: String, event: AccessibilityEvent) {
        // Prevent Uninstallation
        if (INSTALLER_PACKAGES.contains(packageName)) {
            val root = rootInActiveWindow ?: return
            if (containsTextIgnoreCase(root, "Aspirant Focus") &&
                (containsTextIgnoreCase(root, "uninstall") || containsTextIgnoreCase(root, "delete"))
            ) {
                throttleIntervention("Uninstall Blocked!", "Strict Mode is active. Complete your study session first!", isStrict = true)
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
                throttleIntervention("Settings Locked!", "Settings are locked during Strict Mode focus session.", isStrict = true)
                performGlobalAction(GLOBAL_ACTION_HOME)
                return
            }
        }
    }

    // --- Instagram Reels Interceptor ---
    private fun checkInstagramReels(event: AccessibilityEvent) {
        val root = rootInActiveWindow ?: return

        // Check view IDs and content descriptions for Reels triggers
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

        // If user is idling on the Home recommendations feed, gently steer them
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

    // --- Browser URL / Adult Content Filtering ---
    private fun checkBrowserUrl(event: AccessibilityEvent) {
        val root = rootInActiveWindow ?: return

        val urlBarNode = findUrlNode(root)
        val url = urlBarNode?.text?.toString()?.lowercase() ?: return

        val isBlocked = ADULT_AND_DISTRACTING_DOMAINS.any { domain ->
            url.contains(domain)
        }

        if (isBlocked) {
            throttleIntervention(
                "Distracting Site Blocked!",
                "This website is blocked to protect your study time and mental clarity."
            )
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    // --- Node Helpers ---
    private fun findUrlNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.className?.contains("EditText", ignoreCase = true) == true) {
            val text = node.text?.toString() ?: ""
            if (text.contains(".") || text.startsWith("http")) {
                return node
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findUrlNode(child)
            if (result != null) return result
        }
        return null
    }

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
        if (now - lastInterventionTime > 2500) {
            lastInterventionTime = now
            overlayManager.showIntervention(title, msg, isStrictLockout = isStrict)
        }
    }

    companion object {
        const val PKG_INSTAGRAM = "com.instagram.android"
        const val PKG_YOUTUBE = "com.google.android.youtube"
        const val PKG_SETTINGS = "com.android.settings"

        val INSTALLER_PACKAGES = setOf(
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
            "com.samsung.android.packageinstaller"
        )

        val BROWSER_PACKAGES = setOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.brave.browser",
            "com.opera.browser",
            "com.microsoft.emmx",
            "com.duckduckgo.mobile.android"
        )

        val ADULT_AND_DISTRACTING_DOMAINS = listOf(
            "instagram.com/reels",
            "m.youtube.com/shorts",
            "tiktok.com",
            "pornhub",
            "xvideos",
            "xnxx",
            "redtube",
            "bet365",
            "stake.com"
        )
    }
}
