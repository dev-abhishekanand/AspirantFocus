package com.aspirant.focus.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectName: String,
    val topicName: String = "",
    val durationSeconds: Long,
    val startTimeMillis: Long,
    val endTimeMillis: Long = System.currentTimeMillis(),
    val isPomodoro: Boolean = false,
    val isStrict: Boolean = false,
    val wasCompleted: Boolean = true
)

@Entity(tableName = "target_exams")
data class ExamEntity(
    @PrimaryKey
    val name: String,
    val description: String,
    val isSelected: Boolean = false
)

@Entity(tableName = "study_subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val examName: String,
    val name: String,
    val colorHex: String = "#3B82F6",
    val targetDailyMinutes: Int = 120
)

@Entity(tableName = "study_topics")
data class Topic(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val examName: String = "",
    val subjectName: String,
    val name: String,
    val isCompleted: Boolean = false
)

@Entity(tableName = "app_block_rules")
data class AppBlockRule(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isBlockedDuringFocus: Boolean = true,
    val blockReelsOnly: Boolean = false,
    val isWhitelistedForStudy: Boolean = false,
    val dailyLimitMinutes: Int = 0 // 0 means no limit unless in focus
)

data class UsageStatItem(
    val packageName: String,
    val appName: String,
    val totalTimeInForegroundMillis: Long,
    val isDistraction: Boolean = false
)

enum class FocusAudioType(val displayName: String) {
    NONE("Off"),
    BROWN_NOISE("Brown Noise (Deep Focus)"),
    WHITE_NOISE("White Noise (Calm)"),
    BINAURAL_ALPHA("Alpha Waves (Flow State - 10Hz)"),
    BINAURAL_BETA("Beta Waves (Intense Study - 20Hz)")
}

@Entity(tableName = "revisions")
data class RevisionItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val topicId: Long,
    val topicName: String,
    val subjectName: String,
    val stage: Int, // 1: Day 1, 2: Day 3, 3: Day 7, 4: Day 30
    val scheduledDateMillis: Long,
    val isCompleted: Boolean = false
)

@Entity(tableName = "parking_lot")
data class ParkingLotItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isReviewed: Boolean = false
)

data class MCQQuestion(
    val id: Int,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val subject: String
)

data class DailyComparison(
    val dayLabel: String,
    val studyHours: Float,
    val screenHours: Float,
    val isWin: Boolean
)

data class SubjectStudyStat(
    val subjectName: String,
    val totalSeconds: Long,
    val percentage: Float
)

