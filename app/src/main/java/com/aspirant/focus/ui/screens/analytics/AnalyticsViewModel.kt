package com.aspirant.focus.ui.screens.analytics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aspirant.focus.AspirantApplication
import com.aspirant.focus.data.model.DailyComparison
import com.aspirant.focus.data.model.FocusSession
import com.aspirant.focus.data.model.SubjectStudyStat
import com.aspirant.focus.data.model.UsageStatItem
import com.aspirant.focus.data.repository.FocusRepository
import com.aspirant.focus.data.repository.UsageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

    private val _dailyComparisons = MutableStateFlow<List<DailyComparison>>(emptyList())
    val dailyComparisons: StateFlow<List<DailyComparison>> = _dailyComparisons.asStateFlow()

    private val _subjectBreakdown = MutableStateFlow<List<SubjectStudyStat>>(emptyList())
    val subjectBreakdown: StateFlow<List<SubjectStudyStat>> = _subjectBreakdown.asStateFlow()

    init {
        viewModelScope.launch {
            sessions.collect { sessionList ->
                computeStats(sessionList)
            }
        }
    }

    fun loadStats() {
        val hasPerm = usageRepository.hasUsageStatsPermission()
        _hasUsagePermission.value = hasPerm
        if (hasPerm) {
            _usageStats.value = usageRepository.getTodayUsageStats()
        }
        computeStats(sessions.value)
    }

    private fun computeStats(sessionList: List<FocusSession>) {
        viewModelScope.launch(Dispatchers.Default) {
            // 1. Calculate 7-Day Comparison
            val comparisons = mutableListOf<DailyComparison>()
            val hasPerm = usageRepository.hasUsageStatsPermission()

            for (i in 6 downTo 0) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.DAY_OF_YEAR, -i)
                }
                val startMillis = cal.timeInMillis
                val calEnd = (cal.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    add(Calendar.MILLISECOND, -1)
                }
                val endMillis = calEnd.timeInMillis

                val dayLabel = if (i == 0) "Today" else SimpleDateFormat("EEE", Locale.getDefault()).format(Date(startMillis))

                val dayStudySecs = sessionList
                    .filter { it.startTimeMillis in startMillis..endMillis }
                    .sumOf { it.durationSeconds }
                val studyHours = dayStudySecs / 3600f

                val screenMillis = if (hasPerm) usageRepository.getDailyScreenTimeMillis(startMillis, endMillis) else 0L
                val screenHours = screenMillis / (1000f * 3600f)

                comparisons.add(
                    DailyComparison(
                        dayLabel = dayLabel,
                        studyHours = studyHours,
                        screenHours = screenHours,
                        isWin = studyHours >= screenHours && studyHours > 0f
                    )
                )
            }
            _dailyComparisons.value = comparisons

            // 2. Subject-wise breakdown
            val totalSeconds = sessionList.sumOf { it.durationSeconds }
            if (totalSeconds > 0) {
                val breakdown = sessionList.groupBy { it.subjectName }
                    .map { (subject, list) ->
                        val subSeconds = list.sumOf { it.durationSeconds }
                        SubjectStudyStat(
                            subjectName = subject,
                            totalSeconds = subSeconds,
                            percentage = (subSeconds.toFloat() / totalSeconds.toFloat()) * 100f
                        )
                    }
                    .sortedByDescending { it.totalSeconds }
                _subjectBreakdown.value = breakdown
            } else {
                _subjectBreakdown.value = emptyList()
            }
        }
    }
}
