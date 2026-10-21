package com.aspirant.focus.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.aspirant.focus.AspirantApplication
import com.aspirant.focus.MainActivity
import com.aspirant.focus.R
import com.aspirant.focus.data.model.FocusSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FocusSessionService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null
    private val prefs by lazy { AspirantApplication.instance.preferencesManager }
    private val repository by lazy {
        val db = AspirantApplication.instance.database
        com.aspirant.focus.data.repository.FocusRepository(db.focusDao())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val subject = intent.getStringExtra(EXTRA_SUBJECT) ?: "General Studies"
                val durationSec = intent.getLongExtra(EXTRA_DURATION_SEC, 25 * 60L)
                val isStrict = intent.getBooleanExtra(EXTRA_IS_STRICT, false)
                val isPomodoro = intent.getBooleanExtra(EXTRA_IS_POMODORO, false)
                startSession(subject, durationSec, isStrict, isPomodoro)
            }
            ACTION_STOP -> {
                if (_sessionState.value.isStrict) {
                    // Cannot stop in strict mode
                    return START_STICKY
                }
                stopSession(completed = false)
            }
        }
        return START_STICKY
    }

    private fun startSession(subject: String, durationSec: Long, isStrict: Boolean, isPomodoro: Boolean) {
        timerJob?.cancel()

        val startTime = System.currentTimeMillis()
        val endTime = startTime + (durationSec * 1000)

        _sessionState.value = SessionState(
            isActive = true,
            subject = subject,
            remainingSeconds = durationSec,
            totalSeconds = durationSec,
            isStrict = isStrict,
            isPomodoro = isPomodoro,
            isBreak = false
        )

        if (isStrict) {
            prefs.isStrictModeActive = true
            prefs.strictSessionEndTimeMillis = endTime
        }

        startForeground(NOTIFICATION_ID, buildNotification())

        timerJob = serviceScope.launch {
            while (_sessionState.value.remainingSeconds > 0) {
                delay(1000)
                val newRemaining = _sessionState.value.remainingSeconds - 1
                _sessionState.value = _sessionState.value.copy(remainingSeconds = newRemaining)
                updateNotification()
            }

            // Session completed!
            onSessionComplete(subject, durationSec, startTime, isStrict, isPomodoro)
        }
    }

    private suspend fun onSessionComplete(
        subject: String,
        durationSec: Long,
        startTime: Long,
        isStrict: Boolean,
        isPomodoro: Boolean
    ) {
        // Save to Database
        repository.saveSession(
            FocusSession(
                subjectName = subject,
                durationSeconds = durationSec,
                startTimeMillis = startTime,
                endTimeMillis = System.currentTimeMillis(),
                isPomodoro = isPomodoro,
                isStrict = isStrict,
                wasCompleted = true
            )
        )

        if (isStrict) {
            prefs.isStrictModeActive = false
            prefs.strictSessionEndTimeMillis = 0L
        }

        // If Pomodoro, trigger break
        if (isPomodoro && !_sessionState.value.isBreak) {
            val breakSec = prefs.pomodoroBreakMinutes * 60L
            _sessionState.value = _sessionState.value.copy(
                remainingSeconds = breakSec,
                totalSeconds = breakSec,
                isBreak = true
            )
            updateNotification()

            timerJob = serviceScope.launch {
                while (_sessionState.value.remainingSeconds > 0) {
                    delay(1000)
                    val newRemaining = _sessionState.value.remainingSeconds - 1
                    _sessionState.value = _sessionState.value.copy(remainingSeconds = newRemaining)
                    updateNotification()
                }
                stopSession(completed = true)
            }
        } else {
            stopSession(completed = true)
        }
    }

    private fun stopSession(completed: Boolean) {
        timerJob?.cancel()
        _sessionState.value = SessionState()
        prefs.isStrictModeActive = false
        prefs.strictSessionEndTimeMillis = 0L
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val state = _sessionState.value
        val minutes = state.remainingSeconds / 60
        val seconds = state.remainingSeconds % 60
        val timeString = String.format("%02d:%02d", minutes, seconds)

        val title = if (state.isBreak) "☕ Rest Break" else "🎯 Focus: ${state.subject}"
        val content = if (state.isStrict) "$timeString remaining • 🔒 Strict Mode Locked" else "$timeString remaining"

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, AspirantApplication.CHANNEL_FOCUS_TIMER)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    data class SessionState(
        val isActive: Boolean = false,
        val subject: String = "",
        val remainingSeconds: Long = 0,
        val totalSeconds: Long = 0,
        val isStrict: Boolean = false,
        val isPomodoro: Boolean = false,
        val isBreak: Boolean = false
    )

    companion object {
        const val NOTIFICATION_ID = 2001

        const val ACTION_START = "com.aspirant.focus.action.START"
        const val ACTION_STOP = "com.aspirant.focus.action.STOP"

        const val EXTRA_SUBJECT = "extra_subject"
        const val EXTRA_DURATION_SEC = "extra_duration_sec"
        const val EXTRA_IS_STRICT = "extra_is_strict"
        const val EXTRA_IS_POMODORO = "extra_is_pomodoro"

        private val _sessionState = MutableStateFlow(SessionState())
        val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

        fun start(context: Context, subject: String, durationSec: Long, isStrict: Boolean, isPomodoro: Boolean) {
            val intent = Intent(context, FocusSessionService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SUBJECT, subject)
                putExtra(EXTRA_DURATION_SEC, durationSec)
                putExtra(EXTRA_IS_STRICT, isStrict)
                putExtra(EXTRA_IS_POMODORO, isPomodoro)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FocusSessionService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
