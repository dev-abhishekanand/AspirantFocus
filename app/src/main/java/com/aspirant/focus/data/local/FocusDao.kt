package com.aspirant.focus.data.local

import androidx.room.*
import com.aspirant.focus.data.model.*
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

    // Exams
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity)

    @Query("SELECT * FROM target_exams")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM target_exams WHERE isSelected = 1 LIMIT 1")
    fun getSelectedExam(): Flow<ExamEntity?>

    @Query("UPDATE target_exams SET isSelected = CASE WHEN name = :examName THEN 1 ELSE 0 END")
    suspend fun selectExam(examName: String)

    // Subjects
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject)

    @Delete
    suspend fun deleteSubject(subject: Subject)

    @Query("SELECT * FROM study_subjects WHERE examName = :examName")
    fun getSubjectsForExam(examName: String): Flow<List<Subject>>

    @Query("SELECT * FROM study_subjects")
    fun getAllSubjects(): Flow<List<Subject>>

    // Topics
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: Topic)

    @Update
    suspend fun updateTopic(topic: Topic)

    @Delete
    suspend fun deleteTopic(topic: Topic)

    @Query("SELECT * FROM study_topics WHERE subjectName = :subjectName")
    fun getTopicsForSubject(subjectName: String): Flow<List<Topic>>

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
