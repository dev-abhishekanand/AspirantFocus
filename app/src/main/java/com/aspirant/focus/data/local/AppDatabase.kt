package com.aspirant.focus.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aspirant.focus.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [FocusSession::class, ExamEntity::class, Subject::class, Topic::class, AppBlockRule::class],
    version = 2,
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
                .fallbackToDestructiveMigration()
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
                // Pre-populate Exams
                val exams = listOf(
                    ExamEntity("JEE (Main & Advanced)", "Engineering entrance preparation", isSelected = true),
                    ExamEntity("NEET", "Medical entrance preparation", isSelected = false),
                    ExamEntity("UPSC CSE", "Civil Services Examination", isSelected = false),
                    ExamEntity("CA (Chartered Accountancy)", "ICAI CA Foundation & Inter", isSelected = false),
                    ExamEntity("GATE / Software Engg", "CS & Technical exams", isSelected = false),
                    ExamEntity("General Study", "Custom study preparation", isSelected = false)
                )
                exams.forEach { dao.insertExam(it) }

                // Pre-populate Subjects & Topics for JEE
                populateSubjectAndTopics(dao, "JEE (Main & Advanced)", "Physics", "#F59E0B", listOf(
                    "Kinematics & Newton's Laws", "Work, Power & Energy", "Rotational Motion",
                    "Thermodynamics", "Electrostatics", "Current Electricity", "Ray & Wave Optics", "Modern Physics"
                ))
                populateSubjectAndTopics(dao, "JEE (Main & Advanced)", "Chemistry", "#8B5CF6", listOf(
                    "Chemical Bonding", "Thermodynamics & Equilibrium", "General Organic Chemistry",
                    "Hydrocarbons", "Coordination Compounds", "Electrochemistry"
                ))
                populateSubjectAndTopics(dao, "JEE (Main & Advanced)", "Mathematics", "#10B981", listOf(
                    "Differential Calculus", "Integral Calculus", "Coordinate Geometry",
                    "Vectors & 3D", "Matrices & Determinants", "Probability"
                ))

                // NEET
                populateSubjectAndTopics(dao, "NEET", "Biology", "#10B981", listOf(
                    "Human Physiology", "Genetics & Evolution", "Cell Structure & Function",
                    "Plant Physiology", "Ecology & Environment", "Biotechnology"
                ))
                populateSubjectAndTopics(dao, "NEET", "Chemistry", "#8B5CF6", listOf(
                    "Organic Chemistry Reaction Mechanisms", "Chemical Bonding", "Equilibrium", "Coordination Compounds"
                ))
                populateSubjectAndTopics(dao, "NEET", "Physics", "#F59E0B", listOf(
                    "Mechanics", "Thermodynamics", "Optics", "Electrostatics & Magnetism"
                ))

                // UPSC
                populateSubjectAndTopics(dao, "UPSC CSE", "Polity & Constitution", "#3B82F6", listOf(
                    "Fundamental Rights & DPSP", "Parliament & State Legislatures", "Judiciary",
                    "Panchayati Raj", "Constitutional Bodies"
                ))
                populateSubjectAndTopics(dao, "UPSC CSE", "History & Culture", "#F59E0B", listOf(
                    "Modern Indian History (1857-1947)", "Freedom Struggle", "Ancient & Medieval India", "Art & Architecture"
                ))
                populateSubjectAndTopics(dao, "UPSC CSE", "Economy", "#10B981", listOf(
                    "Fiscal & Monetary Policy", "Inflation & Banking", "External Sector & Trade", "Agriculture & Subsidies"
                ))
                populateSubjectAndTopics(dao, "UPSC CSE", "Geography & Environment", "#06B6D4", listOf(
                    "Physical Geography", "Indian Rivers & Climate", "Biodiversity & Climate Change", "National Parks & Sanctuaries"
                ))

                // CA
                populateSubjectAndTopics(dao, "CA (Chartered Accountancy)", "Accounting & Law", "#3B82F6", listOf(
                    "Financial Statements", "Corporate & Other Laws", "Partnership Accounts", "Company Accounts"
                ))
                populateSubjectAndTopics(dao, "CA (Chartered Accountancy)", "Taxation & Audit", "#EF4444", listOf(
                    "Income Tax Assessment", "GST Basics & Returns", "Auditing Standards", "Internal Controls"
                ))

                // GATE / Software
                populateSubjectAndTopics(dao, "GATE / Software Engg", "Data Structures & Algos", "#EC4899", listOf(
                    "Arrays & Linked Lists", "Trees & Graphs", "Dynamic Programming", "Sorting & Searching"
                ))
                populateSubjectAndTopics(dao, "GATE / Software Engg", "Core CS Systems", "#6366F1", listOf(
                    "Operating Systems (Process & Memory)", "DBMS (SQL & Normalization)", "Computer Networks (TCP/IP)"
                ))

                // Default Distraction Apps
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

            private suspend fun populateSubjectAndTopics(
                dao: FocusDao,
                exam: String,
                subjectName: String,
                color: String,
                topics: List<String>
            ) {
                dao.insertSubject(Subject(examName = exam, name = subjectName, colorHex = color))
                topics.forEach { topicName ->
                    dao.insertTopic(Topic(subjectName = subjectName, name = topicName))
                }
            }
        }
    }
}
