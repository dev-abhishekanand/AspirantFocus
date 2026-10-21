package com.aspirant.focus.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aspirant.focus.AspirantApplication
import com.aspirant.focus.service.FocusSessionService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            com.aspirant.focus.service.BlockerProtectionService.start(context)
            val prefs = AspirantApplication.instance.preferencesManager
            if (prefs.isStrictModeActive) {
                val now = System.currentTimeMillis()
                val remainingSec = (prefs.strictSessionEndTimeMillis - now) / 1000
                if (remainingSec > 10) {
                    // Resume locked strict session
                    FocusSessionService.start(
                        context = context,
                        subject = prefs.lastSelectedSubject,
                        durationSec = remainingSec,
                        isStrict = true,
                        isPomodoro = false
                    )
                } else {
                    prefs.isStrictModeActive = false
                    prefs.strictSessionEndTimeMillis = 0L
                }
            }
        }
    }
}
