package com.aspirant.focus.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aspirant.focus.AspirantApplication
import com.aspirant.focus.data.model.Subject
import com.aspirant.focus.data.repository.FocusRepository
import com.aspirant.focus.data.repository.UsageRepository
import com.aspirant.focus.service.FocusSessionService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as AspirantApplication).database
    private val repository = FocusRepository(db.focusDao())
    private val usageRepository = UsageRepository(application)
    private val prefs = (application as AspirantApplication).preferencesManager

    val sessionState = FocusSessionService.sessionState

    val subjects: StateFlow<List<Subject>> = repository.getAllSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSubject = MutableStateFlow(prefs.lastSelectedSubject)
    val selectedSubject: StateFlow<String> = _selectedSubject.asStateFlow()

    private val _selectedDurationMinutes = MutableStateFlow(prefs.pomodoroFocusMinutes)
    val selectedDurationMinutes: StateFlow<Int> = _selectedDurationMinutes.asStateFlow()

    private val _isPomodoro = MutableStateFlow(true)
    val isPomodoro: StateFlow<Boolean> = _isPomodoro.asStateFlow()

    private val _isStrict = MutableStateFlow(false)
    val isStrict: StateFlow<Boolean> = _isStrict.asStateFlow()

    private val _todayStudySeconds = MutableStateFlow(0L)
    val todayStudySeconds: StateFlow<Long> = _todayStudySeconds.asStateFlow()

    private val _todayPhoneMillis = MutableStateFlow(0L)
    val todayPhoneMillis: StateFlow<Long> = _todayPhoneMillis.asStateFlow()

    init {
        refreshStats()
    }

    fun selectSubject(subject: String) {
        _selectedSubject.value = subject
        prefs.lastSelectedSubject = subject
    }

    fun selectDuration(minutes: Int) {
        _selectedDurationMinutes.value = minutes
    }

    fun togglePomodoro(enabled: Boolean) {
        _isPomodoro.value = enabled
    }

    fun toggleStrict(enabled: Boolean) {
        _isStrict.value = enabled
    }

    fun startFocusSession() {
        val durationSec = _selectedDurationMinutes.value * 60L
        FocusSessionService.start(
            context = getApplication(),
            subject = _selectedSubject.value,
            durationSec = durationSec,
            isStrict = _isStrict.value,
            isPomodoro = _isPomodoro.value
        )
    }

    fun stopFocusSession(): Boolean {
        if (sessionState.value.isStrict) {
            return false // Cannot stop in strict mode
        }
        FocusSessionService.stop(getApplication())
        refreshStats()
        return true
    }

    fun refreshStats() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            _todayStudySeconds.value = repository.getTotalStudySecondsSince(calendar.timeInMillis)
            _todayPhoneMillis.value = usageRepository.getTodayTotalScreenTimeMillis()
        }
    }
}
