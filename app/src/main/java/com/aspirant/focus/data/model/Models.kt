package com.aspirant.focus.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectName: String,
    val durationSeconds: Long,
    val startTimeMillis: Long,
    val endTimeMillis: Long = System.currentTimeMillis(),
    val isPomodoro: Boolean = false,
    val isStrict: Boolean = false,
    val wasCompleted: Boolean = true
)

@Entity(tableName = "study_subjects")
data class Subject(
    @PrimaryKey
    val name: String,
    val colorHex: String = "#3B82F6",
    val targetDailyMinutes: Int = 120
)

@Entity(tableName = "app_block_rules")
data class AppBlockRule(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isBlockedDuringFocus: Boolean = true,
    val blockReelsOnly: Boolean = false,
    val dailyLimitMinutes: Int = 0 // 0 means no limit unless in focus
)

data class UsageStatItem(
    val packageName: String,
    val appName: String,
    val totalTimeInForegroundMillis: Long,
    val isDistraction: Boolean = false
)
