package com.aspirant.focus.data.repository

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import com.aspirant.focus.data.model.UsageStatItem
import java.util.Calendar

class UsageRepository(private val context: Context) {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    private val knownDistractions = setOf(
        "com.instagram.android",
        "com.google.android.youtube",
        "com.snapchat.android",
        "com.twitter.android",
        "com.facebook.katana",
        "com.reddit.frontpage",
        "com.zhiliaoapp.musically", // TikTok
        "com.netflix.mediaclient",
        "com.amazon.avod.thirdpartyclient"
    )

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getTodayUsageStats(): List<UsageStatItem> {
        if (!hasUsageStatsPermission() || usageStatsManager == null) {
            return emptyList()
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: return emptyList()

        val pm = context.packageManager
        val list = mutableListOf<UsageStatItem>()

        // Aggregate per package
        val aggregated = mutableMapOf<String, Long>()
        for (usage in stats) {
            if (usage.totalTimeInForeground > 10_000) { // filter out < 10 seconds
                aggregated[usage.packageName] = (aggregated[usage.packageName] ?: 0L) + usage.totalTimeInForeground
            }
        }

        for ((pkg, duration) in aggregated) {
            val appName = try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                pkg
            }

            list.add(
                UsageStatItem(
                    packageName = pkg,
                    appName = appName,
                    totalTimeInForegroundMillis = duration,
                    isDistraction = knownDistractions.contains(pkg)
                )
            )
        }

        return list.sortedByDescending { it.totalTimeInForegroundMillis }
    }

    fun getTodayTotalScreenTimeMillis(): Long {
        return getTodayUsageStats().sumOf { it.totalTimeInForegroundMillis }
    }

    fun getTodayDistractionTimeMillis(): Long {
        return getTodayUsageStats()
            .filter { it.isDistraction }
            .sumOf { it.totalTimeInForegroundMillis }
    }
}
