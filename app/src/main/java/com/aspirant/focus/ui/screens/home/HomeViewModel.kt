package com.aspirant.focus.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aspirant.focus.AspirantApplication
import com.aspirant.focus.data.model.FocusAudioType
import com.aspirant.focus.data.model.Subject
import com.aspirant.focus.data.model.Topic
import com.aspirant.focus.data.repository.FocusRepository
import com.aspirant.focus.data.repository.UsageRepository
import com.aspirant.focus.service.FocusAudioGenerator
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

    val selectedExam = repository.getSelectedExam()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val subjects: StateFlow<List<Subject>> = selectedExam.flatMapLatest { exam ->
        if (exam != null) {
            repository.getSubjectsForExam(exam.name)
        } else {
            repository.getAllSubjects()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSubject = MutableStateFlow(prefs.lastSelectedSubject)
    val selectedSubject: StateFlow<String> = _selectedSubject.asStateFlow()

    private val _topics = MutableStateFlow<List<Topic>>(emptyList())
    val topics: StateFlow<List<Topic>> = _topics.asStateFlow()

    private val _selectedTopic = MutableStateFlow("")
    val selectedTopic: StateFlow<String> = _selectedTopic.asStateFlow()

    private val _selectedDurationMinutes = MutableStateFlow(prefs.pomodoroFocusMinutes)
    val selectedDurationMinutes: StateFlow<Int> = _selectedDurationMinutes.asStateFlow()

    private val _isPomodoro = MutableStateFlow(true)
    val isPomodoro: StateFlow<Boolean> = _isPomodoro.asStateFlow()

    private val _isStrict = MutableStateFlow(false)
    val isStrict: StateFlow<Boolean> = _isStrict.asStateFlow()

    private val _selectedAudio = MutableStateFlow(FocusAudioType.NONE)
    val selectedAudio: StateFlow<FocusAudioType> = _selectedAudio.asStateFlow()

    private val _todayStudySeconds = MutableStateFlow(0L)
    val todayStudySeconds: StateFlow<Long> = _todayStudySeconds.asStateFlow()

    private val _todayPhoneMillis = MutableStateFlow(0L)
    val todayPhoneMillis: StateFlow<Long> = _todayPhoneMillis.asStateFlow()

    init {
        refreshStats()
        viewModelScope.launch {
            _selectedSubject.collect { subject ->
                loadTopics(subject)
            }
        }
    }

    private fun loadTopics(subject: String) {
        viewModelScope.launch {
            repository.getTopicsForSubject(subject).collect { topicList ->
                _topics.value = topicList
                if (topicList.isNotEmpty() && _selectedTopic.value.isEmpty()) {
                    _selectedTopic.value = topicList.first().name
                }
            }
        }
    }

    fun selectSubject(subject: String) {
        _selectedSubject.value = subject
        _selectedTopic.value = ""
        prefs.lastSelectedSubject = subject
    }

    fun selectTopic(topic: String) {
        _selectedTopic.value = topic
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

    fun selectAudio(audio: FocusAudioType) {
        _selectedAudio.value = audio
        if (sessionState.value.isActive) {
            FocusAudioGenerator.instance.play(audio)
        }
    }

    fun startFocusSession() {
        val durationSec = _selectedDurationMinutes.value * 60L
        val subjectTag = if (_selectedTopic.value.isNotBlank()) {
            "${_selectedSubject.value} • ${_selectedTopic.value}"
        } else {
            _selectedSubject.value
        }

        if (_selectedAudio.value != FocusAudioType.NONE) {
            FocusAudioGenerator.instance.play(_selectedAudio.value)
        }

        FocusSessionService.start(
            context = getApplication(),
            subject = subjectTag,
            durationSec = durationSec,
            isStrict = _isStrict.value,
            isPomodoro = _isPomodoro.value
        )
    }

    fun stopFocusSession(): Boolean {
        if (sessionState.value.isStrict) {
            return false // Cannot stop in strict mode
        }
        FocusAudioGenerator.instance.stop()
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

    override fun onCleared() {
        super.onCleared()
        FocusAudioGenerator.instance.stop()
    }
}
