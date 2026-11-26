package com.institution.idscanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Student::class, ScanHistory::class, AttendanceRecord::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun historyDao(): ScanHistoryDao
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        @Volatile private var INSTANCE_NAME: String? = null

        /** Adds the attendance table. Existing students/history are untouched. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `attendance` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`studentId` TEXT NOT NULL, `date` TEXT NOT NULL, " +
                        "`scannedAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "`index_attendance_studentId_date` ON `attendance` (`studentId`, `date`)"
                )
            }
        }

        /** Database file for an account: the shared file when no Gmail is set,
         *  otherwise a separate file per Gmail (hashed, so no email in the name). */
        fun databaseNameFor(accountEmail: String): String {
            val key = AccountStore.accountKey(accountEmail)
            return if (key == "default") "student_id_scanner.db"
            else "student_id_scanner_$key.db"
        }

        fun get(context: Context, accountEmail: String = ""): AppDatabase {
            val name = databaseNameFor(accountEmail)
            val cached = INSTANCE
            if (cached != null && INSTANCE_NAME == name) return cached
            return synchronized(this) {
                val again = INSTANCE
                if (again != null && INSTANCE_NAME == name) {
                    again
                } else {
                    // Account switched: close the old database before opening
                    // the other account's file.
                    try { INSTANCE?.close() } catch (_: Exception) { }
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        name
                    )
                        .addMigrations(MIGRATION_1_2)
                        .addCallback(SeedCallback())
                        .build()
                        .also { INSTANCE = it; INSTANCE_NAME = name }
                }
            }
        }

        /** Seeds 5 demo students on first creation of each database file, so
         *  scanning can be tried immediately. Each Gmail account's new
         *  database gets its own copy. Delete them from Admin > Students,
         *  or replace them with a CSV import. */
        private class SeedCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    // Seed through raw SQL: Room is not ready inside onCreate.
                    val demo = listOf(
                        arrayOf("STD-1001", "আব্দুল্লাহ আল মামুন", "মোঃ আব্দুল করিম", "ফাতেমা বেগম", "দশম", "01", "2025-26", "ক", "01700000001"),
                        arrayOf("STD-1002", "ফারজানা আক্তার", "মোঃ ফজলুল হক", "শাহনাজ বেগম", "নবম", "05", "2025-26", "খ", "01700000002"),
                        arrayOf("STD-1003", "মোঃ রাকিব হাসান", "মোঃ আনোয়ার হোসেন", "রোকেয়া বেগম", "অষ্টম", "12", "2025-26", "ক", "01700000003"),
                        arrayOf("STD-1004", "নুসরাত জাহান", "মোঃ নজরুল ইসলাম", "মরিয়ম বেগম", "সপ্তম", "03", "2025-26", "গ", ""),
                        arrayOf("STD-1005", "তানভীর আহমেদ", "মোঃ তাজুল ইসলাম", "আমিনা খাতুন", "ষষ্ঠ", "07", "2025-26", "ক", "01700000005")
                    )
                    demo.forEach {
                        db.execSQL(
                            "INSERT INTO students (studentId, name, fatherName, motherName, className, roll, session, section, phone, photoUri) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, '')",
                            it
                        )
                    }
                }
            }
        }
    }
}
