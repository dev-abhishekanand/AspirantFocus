package com.aspirant.focus.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Reactive StateFlows for UI and Background Services
    private val _reelsBlockerFlow = MutableStateFlow(isReelsBlockerEnabled)
    val reelsBlockerFlow: StateFlow<Boolean> = _reelsBlockerFlow.asStateFlow()

    private val _shortsBlockerFlow = MutableStateFlow(isShortsBlockerEnabled)
    val shortsBlockerFlow: StateFlow<Boolean> = _shortsBlockerFlow.asStateFlow()

    private val _youtubeStudyModeFlow = MutableStateFlow(isYouTubeStudyModeEnabled)
    val youtubeStudyModeFlow: StateFlow<Boolean> = _youtubeStudyModeFlow.asStateFlow()

    private val _strictModeActiveFlow = MutableStateFlow(isStrictModeActive)
    val strictModeActiveFlow: StateFlow<Boolean> = _strictModeActiveFlow.asStateFlow()

    var isReelsBlockerEnabled: Boolean
        get() = prefs.getBoolean(KEY_REELS_BLOCKER, true)
        set(value) {
            prefs.edit().putBoolean(KEY_REELS_BLOCKER, value).apply()
            _reelsBlockerFlow.value = value
        }

    var isShortsBlockerEnabled: Boolean
        get() = prefs.getBoolean(KEY_SHORTS_BLOCKER, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SHORTS_BLOCKER, value).apply()
            _shortsBlockerFlow.value = value
        }

    var isYouTubeStudyModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_YT_STUDY_MODE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_YT_STUDY_MODE, value).apply()
            _youtubeStudyModeFlow.value = value
        }

    var isWebsiteBlockerEnabled: Boolean
        get() = prefs.getBoolean(KEY_WEBSITE_BLOCKER, true)
        set(value) = prefs.edit().putBoolean(KEY_WEBSITE_BLOCKER, value).apply()

    var isAntiUninstallEnabled: Boolean
        get() = prefs.getBoolean(KEY_ANTI_UNINSTALL, true)
        set(value) = prefs.edit().putBoolean(KEY_ANTI_UNINSTALL, value).apply()

    var isSettingsLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_SETTINGS_LOCK, true)
        set(value) = prefs.edit().putBoolean(KEY_SETTINGS_LOCK, value).apply()

    var isStrictModeActive: Boolean
        get() = prefs.getBoolean(KEY_STRICT_MODE_ACTIVE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_STRICT_MODE_ACTIVE, value).apply()
            _strictModeActiveFlow.value = value
        }

    var strictSessionEndTimeMillis: Long
        get() = prefs.getLong(KEY_STRICT_END_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_STRICT_END_TIME, value).apply()

    var pomodoroFocusMinutes: Int
        get() = prefs.getInt(KEY_POMO_FOCUS_MIN, 25)
        set(value) = prefs.edit().putInt(KEY_POMO_FOCUS_MIN, value).apply()

    var pomodoroBreakMinutes: Int
        get() = prefs.getInt(KEY_POMO_BREAK_MIN, 5)
        set(value) = prefs.edit().putInt(KEY_POMO_BREAK_MIN, value).apply()

    var lastSelectedSubject: String
        get() = prefs.getString(KEY_LAST_SUBJECT, "General Studies") ?: "General Studies"
        set(value) = prefs.edit().putString(KEY_LAST_SUBJECT, value).apply()

    companion object {
        private const val PREFS_NAME = "aspirant_focus_prefs"
        private const val KEY_REELS_BLOCKER = "key_reels_blocker"
        private const val KEY_SHORTS_BLOCKER = "key_shorts_blocker"
        private const val KEY_YT_STUDY_MODE = "key_yt_study_mode"
        private const val KEY_WEBSITE_BLOCKER = "key_website_blocker"
        private const val KEY_ANTI_UNINSTALL = "key_anti_uninstall"
        private const val KEY_SETTINGS_LOCK = "key_settings_lock"
        private const val KEY_STRICT_MODE_ACTIVE = "key_strict_mode_active"
        private const val KEY_STRICT_END_TIME = "key_strict_end_time"
        private const val KEY_POMO_FOCUS_MIN = "key_pomo_focus_min"
        private const val KEY_POMO_BREAK_MIN = "key_pomo_break_min"
        private const val KEY_LAST_SUBJECT = "key_last_subject"
    }
}
