package com.institution.idscanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class Student(
    @PrimaryKey val studentId: String,
    val name: String,
    val fatherName: String = "",
    val motherName: String = "",
    val className: String = "",
    val roll: String = "",
    val session: String = "",
    val section: String = "",
    val phone: String = "",
    /** Content URI string of a picked photo, or empty. */
    val photoUri: String = ""
)

@Entity(tableName = "scan_history")
data class ScanHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: String,
    val scannedAt: Long = System.currentTimeMillis()
)

/**
 * One attendance record per student per day. [date] is "yyyy-MM-dd" in the
 * Asia/Dhaka timezone. The unique index on (studentId, date) plus
 * INSERT OR IGNORE keeps it to one record per student per day, so repeat
 * scans on the same day never create duplicates.
 */
@Entity(
    tableName = "attendance",
    indices = [androidx.room.Index(value = ["studentId", "date"], unique = true)]
)
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: String,
    val date: String,
    val scannedAt: Long = System.currentTimeMillis()
)
