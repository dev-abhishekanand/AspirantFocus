package com.aspirant.focus.ui.screens.exams

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aspirant.focus.AspirantApplication
import com.aspirant.focus.data.model.ExamEntity
import com.aspirant.focus.data.model.Subject
import com.aspirant.focus.data.model.Topic
import com.aspirant.focus.data.repository.FocusRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ExamsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as AspirantApplication).database
    private val repository = FocusRepository(db.focusDao())

    val allExams: StateFlow<List<ExamEntity>> = repository.getAllExams()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedExam: StateFlow<ExamEntity?> = repository.getSelectedExam()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()

    private val _topicsMap = MutableStateFlow<Map<String, List<Topic>>>(emptyMap())
    val topicsMap: StateFlow<Map<String, List<Topic>>> = _topicsMap.asStateFlow()

    init {
        viewModelScope.launch {
            selectedExam.filterNotNull().collect { exam ->
                loadSubjectsForExam(exam.name)
            }
        }
    }

    fun selectExam(examName: String) {
        viewModelScope.launch {
            repository.selectExam(examName)
            loadSubjectsForExam(examName)
        }
    }

    private fun loadSubjectsForExam(examName: String) {
        viewModelScope.launch {
            repository.getSubjectsForExam(examName).collect { subjectList ->
                _subjects.value = subjectList
                subjectList.forEach { subj ->
                    loadTopicsForSubject(subj.name)
                }
            }
        }
    }

    private fun loadTopicsForSubject(subjectName: String) {
        val currentExam = selectedExam.value?.name ?: ""
        viewModelScope.launch {
            repository.getTopicsForSubjectAndExam(subjectName, currentExam).collect { topics ->
                val current = _topicsMap.value.toMutableMap()
                current[subjectName] = topics
                _topicsMap.value = current
            }
        }
    }

    fun addCustomExam(name: String, description: String) {
        viewModelScope.launch {
            repository.addExam(ExamEntity(name = name, description = description, isSelected = true))
            selectExam(name)
        }
    }

    fun deleteExam(examName: String) {
        viewModelScope.launch {
            repository.deleteExam(examName)
            val remaining = allExams.value.filter { it.name != examName }
            if (remaining.isNotEmpty()) {
                selectExam(remaining.first().name)
            }
        }
    }

    fun addCustomSubject(subjectName: String) {
        val currentExam = selectedExam.value?.name ?: "General Study"
        viewModelScope.launch {
            repository.addSubject(
                Subject(examName = currentExam, name = subjectName, colorHex = "#3B82F6")
            )
            loadSubjectsForExam(currentExam)
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
            val current = _topicsMap.value.toMutableMap()
            current.remove(subject.name)
            _topicsMap.value = current
            selectedExam.value?.name?.let { loadSubjectsForExam(it) }
        }
    }

    fun addTopic(subjectName: String, topicName: String) {
        val currentExam = selectedExam.value?.name ?: ""
        viewModelScope.launch {
            repository.addTopic(Topic(examName = currentExam, subjectName = subjectName, name = topicName))
            loadTopicsForSubject(subjectName)
        }
    }

    val dueRevisions: StateFlow<List<com.aspirant.focus.data.model.RevisionItem>> = repository.getDueRevisions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleTopicCompleted(topic: Topic) {
        viewModelScope.launch {
            val willBeCompleted = !topic.isCompleted
            repository.updateTopic(topic.copy(isCompleted = willBeCompleted))
            if (willBeCompleted) {
                repository.scheduleRevisionsForTopic(topic)
            }
            loadTopicsForSubject(topic.subjectName)
        }
    }

    fun completeRevision(revision: com.aspirant.focus.data.model.RevisionItem) {
        viewModelScope.launch {
            repository.completeRevision(revision)
        }
    }

    fun deleteTopic(topic: Topic) {
        viewModelScope.launch {
            repository.deleteTopic(topic)
            loadTopicsForSubject(topic.subjectName)
        }
    }
}
