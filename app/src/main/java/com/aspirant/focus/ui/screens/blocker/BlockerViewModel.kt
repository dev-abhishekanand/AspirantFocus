package com.aspirant.focus.ui.screens.blocker

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aspirant.focus.AspirantApplication
import com.aspirant.focus.data.model.AppBlockRule
import com.aspirant.focus.data.repository.FocusRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class InstalledAppItem(
    val packageName: String,
    val appName: String
)

class BlockerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as AspirantApplication).database
    private val repository = FocusRepository(db.focusDao())
    private val prefs = (application as AspirantApplication).preferencesManager
    private val pm = application.packageManager

    val appRules: StateFlow<List<AppBlockRule>> = repository.getAllAppRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isReelsBlockerEnabled = prefs.reelsBlockerFlow
    val isShortsBlockerEnabled = prefs.shortsBlockerFlow
    val isYouTubeStudyModeEnabled = prefs.youtubeStudyModeFlow

    private val _isWebsiteBlocker = MutableStateFlow(prefs.isWebsiteBlockerEnabled)
    val isWebsiteBlocker: StateFlow<Boolean> = _isWebsiteBlocker.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppItem>> = _installedApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val availableAppsToAdd: StateFlow<List<InstalledAppItem>> = combine(
        _installedApps,
        appRules,
        _searchQuery
    ) { installed, blocked, query ->
        val blockedPackages = blocked.map { it.packageName }.toSet()
        installed
            .filter { !blockedPackages.contains(it.packageName) }
            .filter {
                query.isEmpty() ||
                it.appName.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadInstalledApps()
    }

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            val apps = resolveInfos.mapNotNull { info ->
                val pkg = info.activityInfo.packageName
                if (pkg == getApplication<Application>().packageName) {
                    null
                } else {
                    val label = info.loadLabel(pm).toString()
                    InstalledAppItem(packageName = pkg, appName = label)
                }
            }.distinctBy { it.packageName }
             .sortedBy { it.appName.lowercase() }

            _installedApps.value = apps
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addApp(item: InstalledAppItem) {
        viewModelScope.launch {
            repository.updateAppRule(
                AppBlockRule(
                    packageName = item.packageName,
                    appName = item.appName,
                    isBlockedDuringFocus = true,
                    blockReelsOnly = false
                )
            )
        }
    }

    fun removeApp(rule: AppBlockRule) {
        viewModelScope.launch {
            repository.deleteAppRule(rule)
        }
    }

    fun setReelsBlocker(enabled: Boolean) {
        prefs.isReelsBlockerEnabled = enabled
    }

    fun setShortsBlocker(enabled: Boolean) {
        prefs.isShortsBlockerEnabled = enabled
    }

    fun setYouTubeStudyMode(enabled: Boolean) {
        prefs.isYouTubeStudyModeEnabled = enabled
    }

    fun setWebsiteBlocker(enabled: Boolean) {
        prefs.isWebsiteBlockerEnabled = enabled
        _isWebsiteBlocker.value = enabled
    }

    fun toggleAppBlock(rule: AppBlockRule) {
        viewModelScope.launch {
            repository.updateAppRule(rule.copy(isBlockedDuringFocus = !rule.isBlockedDuringFocus))
        }
    }
}
