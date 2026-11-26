package com.institution.idscanner.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.Flow

class StudentRepository(private val db: AppDatabase) {
    private val studentDao = db.studentDao()
    private val historyDao = db.historyDao()
    private val attendanceDao = db.attendanceDao()

    val allStudents: Flow<List<Student>> = studentDao.getAll()
    val history: Flow<List<ScanHistory>> = historyDao.getAll()

    fun search(query: String): Flow<List<Student>> = studentDao.search(query)

    fun attendanceFor(studentId: String): Flow<List<AttendanceRecord>> =
        attendanceDao.getForStudent(studentId)

    /** Normalises a scanned value (trims spaces, upper-cases) before lookup. */
    suspend fun findByScanned(raw: String): Student? {
        val id = raw.trim().uppercase()
        return studentDao.getById(id)
            ?: studentDao.getById(raw.trim())
    }

    suspend fun recordScan(studentId: String) {
        historyDao.insert(ScanHistory(studentId = studentId))
    }

    /** Records today's attendance (Asia/Dhaka). Repeat scans the same day
     *  are ignored by the unique index, so a student is marked once a day. */
    suspend fun markAttendance(studentId: String, nowMillis: Long = System.currentTimeMillis()) {
        attendanceDao.insertIgnore(
            AttendanceRecord(studentId = studentId, date = dateString(nowMillis), scannedAt = nowMillis)
        )
    }

    companion object {
        private val DHAKA: TimeZone = TimeZone.getTimeZone("Asia/Dhaka")

        /** "yyyy-MM-dd" for [millis] in Asia/Dhaka. */
        fun dateString(millis: Long = System.currentTimeMillis()): String {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            fmt.timeZone = DHAKA
            return fmt.format(Date(millis))
        }
    }

    suspend fun upsert(student: Student) = studentDao.upsert(student)
    suspend fun upsertAll(students: List<Student>) = studentDao.upsertAll(students)
    suspend fun delete(student: Student) = studentDao.delete(student)
    suspend fun deleteHistory(id: Long) = historyDao.deleteById(id)
    suspend fun clearHistory() = historyDao.clearAll()
}
