package com.aspirant.focus.data.repository

import com.aspirant.focus.data.local.FocusDao
import com.aspirant.focus.data.model.*
import kotlinx.coroutines.flow.Flow

class FocusRepository(private val focusDao: FocusDao) {

    // Focus Sessions
    fun getAllSessions(): Flow<List<FocusSession>> = focusDao.getAllSessions()

    fun getSessionsSince(startTime: Long): Flow<List<FocusSession>> = focusDao.getSessionsSince(startTime)

    suspend fun getTotalStudySecondsSince(startTime: Long): Long =
        focusDao.getTotalStudySecondsSince(startTime) ?: 0L

    suspend fun saveSession(session: FocusSession): Long =
        focusDao.insertSession(session)

    // Exams
    fun getAllExams(): Flow<List<ExamEntity>> = focusDao.getAllExams()

    fun getSelectedExam(): Flow<ExamEntity?> = focusDao.getSelectedExam()

    suspend fun selectExam(examName: String) = focusDao.selectExam(examName)

    suspend fun addExam(exam: ExamEntity) = focusDao.insertExam(exam)

    // Subjects
    fun getAllSubjects(): Flow<List<Subject>> = focusDao.getAllSubjects()

    fun getSubjectsForExam(examName: String): Flow<List<Subject>> = focusDao.getSubjectsForExam(examName)

    suspend fun addSubject(subject: Subject) = focusDao.insertSubject(subject)

    suspend fun deleteSubject(subject: Subject) = focusDao.deleteSubject(subject)

    // Topics
    fun getTopicsForSubject(subjectName: String): Flow<List<Topic>> = focusDao.getTopicsForSubject(subjectName)

    suspend fun addTopic(topic: Topic) = focusDao.insertTopic(topic)

    suspend fun updateTopic(topic: Topic) = focusDao.updateTopic(topic)

    suspend fun deleteTopic(topic: Topic) = focusDao.deleteTopic(topic)

    // App Block Rules
    fun getAllAppRules(): Flow<List<AppBlockRule>> = focusDao.getAllAppRules()

    suspend fun updateAppRule(rule: AppBlockRule) = focusDao.insertAppRule(rule)

    suspend fun getRuleForPackage(pkg: String): AppBlockRule? =
        focusDao.getRuleForPackage(pkg)

    suspend fun deleteAppRule(rule: AppBlockRule) =
        focusDao.deleteAppRule(rule)
}
