package com.aspirant.focus.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aspirant.focus.data.model.AppBlockRule
import com.aspirant.focus.data.model.FocusSession
import com.aspirant.focus.data.model.Subject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [FocusSession::class, Subject::class, AppBlockRule::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun focusDao(): FocusDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aspirant_focus_database"
                )
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDefaultData(database.focusDao())
                    }
                }
            }

            suspend fun populateDefaultData(dao: FocusDao) {
                // Pre-populate core aspirant subjects
                val defaultSubjects = listOf(
                    Subject("General Studies", "#3B82F6", 120),
                    Subject("Mathematics", "#10B981", 120),
                    Subject("Physics", "#F59E0B", 90),
                    Subject("Chemistry", "#8B5CF6", 90),
                    Subject("Coding & CS", "#EC4899", 180),
                    Subject("Revision", "#6366F1", 60)
                )
                defaultSubjects.forEach { dao.insertSubject(it) }

                // Pre-populate major distraction apps with reels/shorts triggers
                val defaultAppRules = listOf(
                    AppBlockRule("com.instagram.android", "Instagram", isBlockedDuringFocus = true, blockReelsOnly = true),
                    AppBlockRule("com.google.android.youtube", "YouTube", isBlockedDuringFocus = true, blockReelsOnly = true),
                    AppBlockRule("com.snapchat.android", "Snapchat", isBlockedDuringFocus = true, blockReelsOnly = false),
                    AppBlockRule("com.twitter.android", "X (Twitter)", isBlockedDuringFocus = true, blockReelsOnly = false),
                    AppBlockRule("com.facebook.katana", "Facebook", isBlockedDuringFocus = true, blockReelsOnly = true),
                    AppBlockRule("com.reddit.frontpage", "Reddit", isBlockedDuringFocus = true, blockReelsOnly = false)
                )
                defaultAppRules.forEach { dao.insertAppRule(it) }
            }
        }
    }
}
