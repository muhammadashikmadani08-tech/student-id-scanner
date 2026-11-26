package com.institution.idscanner.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAll(): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE studentId = :id LIMIT 1")
    suspend fun getById(id: String): Student?

    @Query(
        "SELECT * FROM students WHERE studentId LIKE '%' || :q || '%' " +
            "OR name LIKE '%' || :q || '%' OR className LIKE '%' || :q || '%' " +
            "OR roll LIKE '%' || :q || '%' ORDER BY name ASC"
    )
    fun search(q: String): Flow<List<Student>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(student: Student)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(students: List<Student>)

    @Update
    suspend fun update(student: Student)

    @Delete
    suspend fun delete(student: Student)

    @Query("SELECT COUNT(*) FROM students")
    suspend fun count(): Int
}

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY scannedAt DESC")
    fun getAll(): Flow<List<ScanHistory>>

    @Insert
    suspend fun insert(entry: ScanHistory)

    @Query("DELETE FROM scan_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM scan_history")
    suspend fun clearAll()
}

@Dao
interface AttendanceDao {
    /** Inserts only if no record for this student+date exists yet. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(record: AttendanceRecord)

    @Query("SELECT * FROM attendance WHERE studentId = :studentId ORDER BY date ASC")
    fun getForStudent(studentId: String): Flow<List<AttendanceRecord>>
}
