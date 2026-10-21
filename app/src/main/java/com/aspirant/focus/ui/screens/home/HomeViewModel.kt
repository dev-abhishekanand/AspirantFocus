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

    val allTopics: StateFlow<List<Topic>> = selectedExam.flatMapLatest { exam ->
        if (exam != null) {
            repository.getTopicsForExam(exam.name)
        } else {
            repository.getAllTopics()
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

    private val startOfDayMillis: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    private val todayCompletedSecondsFlow: Flow<Long> = repository.getSessionsSince(startOfDayMillis).map { sessions ->
        sessions.sumOf { it.durationSeconds }
    }

    // Live synced study time: completed sessions today + active seconds ticking right now
    val todayStudySeconds: StateFlow<Long> = combine(
        todayCompletedSecondsFlow,
        sessionState
    ) { completedSec, state ->
        val liveElapsed = if (state.isActive && !state.isBreak) {
            maxOf(0L, state.totalSeconds - state.remainingSeconds)
        } else 0L
        completedSec + liveElapsed
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _todayPhoneMillis = MutableStateFlow(0L)
    val todayPhoneMillis: StateFlow<Long> = _todayPhoneMillis.asStateFlow()

    val parkingItems: StateFlow<List<com.aspirant.focus.data.model.ParkingLotItem>> = repository.getAllParkingItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dueRevisions: StateFlow<List<com.aspirant.focus.data.model.RevisionItem>> = repository.getDueRevisions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isGrayscaleEnabled = MutableStateFlow(prefs.isGrayscaleEnabled)
    val isGrayscaleEnabled: StateFlow<Boolean> = _isGrayscaleEnabled.asStateFlow()

    val targetExamDateMillis: Long get() = prefs.targetExamDateMillis

    fun toggleGrayscale(enabled: Boolean) {
        _isGrayscaleEnabled.value = enabled
        prefs.isGrayscaleEnabled = enabled
    }

    fun parkThought(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.parkThought(text.trim())
        }
    }

    fun deleteParkingItem(item: com.aspirant.focus.data.model.ParkingLotItem) {
        viewModelScope.launch {
            repository.deleteParkingItem(item)
        }
    }

    fun completeRevision(revision: com.aspirant.focus.data.model.RevisionItem) {
        viewModelScope.launch {
            repository.completeRevision(revision)
        }
    }

    init {
        refreshStats()

        // 1. When exam changes, cascade update subjects and auto-select first subject
        viewModelScope.launch {
            subjects.collect { subjectList ->
                if (subjectList.isNotEmpty()) {
                    if (subjectList.none { it.name == _selectedSubject.value }) {
                        val firstSubject = subjectList.first().name
                        _selectedSubject.value = firstSubject
                        prefs.lastSelectedSubject = firstSubject
                    }
                } else {
                    _selectedSubject.value = ""
                }
            }
        }

        // 2. When subject or exam changes, cascade update topics and auto-select first topic
        viewModelScope.launch {
            combine(_selectedSubject, selectedExam) { subject, exam ->
                Pair(subject, exam?.name ?: "")
            }.collect { (subject, examName) ->
                loadTopics(subject, examName)
            }
        }
    }

    private fun loadTopics(subject: String, examName: String) {
        if (subject.isBlank()) {
            _topics.value = emptyList()
            _selectedTopic.value = ""
            return
        }
        viewModelScope.launch {
            repository.getTopicsForSubjectAndExam(subject, examName).collect { topicList ->
                _topics.value = topicList
                if (topicList.isNotEmpty()) {
                    if (topicList.none { it.name == _selectedTopic.value }) {
                        _selectedTopic.value = topicList.first().name
                    }
                } else {
                    _selectedTopic.value = ""
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

    fun emergencyUnlock() {
        prefs.isStrictModeActive = false
        prefs.strictSessionEndTimeMillis = 0L
        prefs.emergencyAccessEndTimeMillis = 0L
        FocusAudioGenerator.instance.stop()
        FocusSessionService.stop(getApplication())
        refreshStats()
    }

    fun grantEmergencyAccess(minutes: Int = 5) {
        prefs.emergencyAccessEndTimeMillis = System.currentTimeMillis() + minutes * 60 * 1000L
    }

    fun refreshStats() {
        viewModelScope.launch {
            _todayPhoneMillis.value = usageRepository.getTodayTotalScreenTimeMillis()
        }
    }

    override fun onCleared() {
        super.onCleared()
        FocusAudioGenerator.instance.stop()
    }
}
