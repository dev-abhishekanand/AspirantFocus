package com.aspirant.focus.data.repository

import com.aspirant.focus.data.local.FocusDao
import com.aspirant.focus.data.model.AppBlockRule
import com.aspirant.focus.data.model.FocusSession
import com.aspirant.focus.data.model.Subject
import kotlinx.coroutines.flow.Flow

class FocusRepository(private val focusDao: FocusDao) {

    fun getAllSessions(): Flow<List<FocusSession>> = focusDao.getAllSessions()

    fun getSessionsSince(startTime: Long): Flow<List<FocusSession>> = focusDao.getSessionsSince(startTime)

    suspend fun getTotalStudySecondsSince(startTime: Long): Long =
        focusDao.getTotalStudySecondsSince(startTime) ?: 0L

    suspend fun saveSession(session: FocusSession): Long =
        focusDao.insertSession(session)

    fun getAllSubjects(): Flow<List<Subject>> = focusDao.getAllSubjects()

    suspend fun addSubject(subject: Subject) = focusDao.insertSubject(subject)

    fun getAllAppRules(): Flow<List<AppBlockRule>> = focusDao.getAllAppRules()

    suspend fun updateAppRule(rule: AppBlockRule) = focusDao.insertAppRule(rule)

    suspend fun getRuleForPackage(pkg: String): AppBlockRule? =
        focusDao.getRuleForPackage(pkg)
}
