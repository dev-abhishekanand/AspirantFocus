package com.aspirant.focus.ui.screens.blocker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aspirant.focus.AspirantApplication
import com.aspirant.focus.data.model.AppBlockRule
import com.aspirant.focus.data.repository.FocusRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BlockerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as AspirantApplication).database
    private val repository = FocusRepository(db.focusDao())
    private val prefs = (application as AspirantApplication).preferencesManager

    val appRules: StateFlow<List<AppBlockRule>> = repository.getAllAppRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isReelsBlockerEnabled = prefs.reelsBlockerFlow
    val isShortsBlockerEnabled = prefs.shortsBlockerFlow
    val isYouTubeStudyModeEnabled = prefs.youtubeStudyModeFlow

    private val _isWebsiteBlocker = MutableStateFlow(prefs.isWebsiteBlockerEnabled)
    val isWebsiteBlocker: StateFlow<Boolean> = _isWebsiteBlocker.asStateFlow()

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
