package com.aspirant.focus.ui.screens.analytics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aspirant.focus.AspirantApplication
import com.aspirant.focus.data.model.FocusSession
import com.aspirant.focus.data.model.UsageStatItem
import com.aspirant.focus.data.repository.FocusRepository
import com.aspirant.focus.data.repository.UsageRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as AspirantApplication).database
    private val repository = FocusRepository(db.focusDao())
    private val usageRepository = UsageRepository(application)

    val sessions: StateFlow<List<FocusSession>> = repository.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _usageStats = MutableStateFlow<List<UsageStatItem>>(emptyList())
    val usageStats: StateFlow<List<UsageStatItem>> = _usageStats.asStateFlow()

    private val _hasUsagePermission = MutableStateFlow(false)
    val hasUsagePermission: StateFlow<Boolean> = _hasUsagePermission.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        val hasPerm = usageRepository.hasUsageStatsPermission()
        _hasUsagePermission.value = hasPerm
        if (hasPerm) {
            _usageStats.value = usageRepository.getTodayUsageStats()
        }
    }
}
