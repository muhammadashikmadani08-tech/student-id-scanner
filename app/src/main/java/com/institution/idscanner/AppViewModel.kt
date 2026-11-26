package com.institution.idscanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.institution.idscanner.data.AccountStore
import com.institution.idscanner.data.AttendanceRecord
import com.institution.idscanner.data.SettingsStore
import com.institution.idscanner.data.Student
import com.institution.idscanner.data.StudentRepository
import com.institution.idscanner.util.PhotoStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(
    private val repo: StudentRepository,
    private val settings: SettingsStore,
    private val accountStore: AccountStore,
    val databaseFileName: String
) : ViewModel() {
    val students: StateFlow<List<Student>> = repo.allStudents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val history = repo.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val institutionName = settings.institutionName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val adminPin = settings.adminPin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "1234")

    /** The Gmail currently active ("" = none). Each Gmail has its own data. */
    val currentAccountEmail = accountStore.currentAccountEmail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    /** Attendance records for one student, oldest first. */
    fun attendanceFor(studentId: String): Flow<List<AttendanceRecord>> =
        repo.attendanceFor(studentId)

    suspend fun markAttendance(studentId: String) {
        repo.markAttendance(studentId)
    }

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query
    val searchResults: StateFlow<List<Student>> = MutableStateFlow(emptyList())

    fun setQuery(q: String) {
        _query.value = q
        viewModelScope.launch {
            repo.search(q).collect { (searchResults as MutableStateFlow).value = it }
        }
    }

    /** Returns the student (or null). On a hit, records both the scan
     *  history entry and today's attendance. */
    suspend fun lookupScanned(raw: String): Student? {
        val student = repo.findByScanned(raw)
        if (student != null) {
            repo.recordScan(student.studentId)
            repo.markAttendance(student.studentId)
        }
        return student
    }

    suspend fun getStudent(id: String) = repo.findByScanned(id)

    fun saveStudent(student: Student) = viewModelScope.launch { repo.upsert(student) }
    fun deleteStudent(student: Student) = viewModelScope.launch {
        PhotoStore.deletePhoto(student.photoUri)
        repo.delete(student)
    }
    fun importStudents(list: List<Student>) = viewModelScope.launch { repo.upsertAll(list) }
    fun deleteHistory(id: Long) = viewModelScope.launch { repo.deleteHistory(id) }
    fun clearHistory() = viewModelScope.launch { repo.clearHistory() }
    fun setInstitutionName(name: String) = viewModelScope.launch { settings.setInstitutionName(name) }
    fun setAdminPin(pin: String) = viewModelScope.launch { settings.setAdminPin(pin) }

    /** Saves the active Gmail (normalised). The caller restarts the
     *  activity so the other account's database is loaded. */
    suspend fun setAccountEmail(email: String) {
        accountStore.setCurrentAccountEmail(email)
    }
}
