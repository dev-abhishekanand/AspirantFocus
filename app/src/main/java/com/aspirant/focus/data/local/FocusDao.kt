package com.aspirant.focus.data.local

import androidx.room.*
import com.aspirant.focus.data.model.AppBlockRule
import com.aspirant.focus.data.model.FocusSession
import com.aspirant.focus.data.model.Subject
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusDao {

    // Focus Sessions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSession): Long

    @Query("SELECT * FROM focus_sessions ORDER BY startTimeMillis DESC")
    fun getAllSessions(): Flow<List<FocusSession>>

    @Query("SELECT * FROM focus_sessions WHERE startTimeMillis >= :startTime ORDER BY startTimeMillis DESC")
    fun getSessionsSince(startTime: Long): Flow<List<FocusSession>>

    @Query("SELECT SUM(durationSeconds) FROM focus_sessions WHERE startTimeMillis >= :startTime")
    suspend fun getTotalStudySecondsSince(startTime: Long): Long?

    // Subjects
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSubject(subject: Subject)

    @Query("SELECT * FROM study_subjects")
    fun getAllSubjects(): Flow<List<Subject>>

    // App Block Rules
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppRule(rule: AppBlockRule)

    @Query("SELECT * FROM app_block_rules")
    fun getAllAppRules(): Flow<List<AppBlockRule>>

    @Query("SELECT * FROM app_block_rules WHERE packageName = :pkg LIMIT 1")
    suspend fun getRuleForPackage(pkg: String): AppBlockRule?

    @Delete
    suspend fun deleteAppRule(rule: AppBlockRule)
}
