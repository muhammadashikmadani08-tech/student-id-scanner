package com.institution.idscanner.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.institution.idscanner.util.PhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.institution.idscanner.AppViewModel
import com.institution.idscanner.data.Student

@Composable
fun StudentFormScreen(vm: AppViewModel, existing: Student?, onDone: () -> Unit) {
    var id by remember { mutableStateOf(existing?.studentId ?: "") }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var father by remember { mutableStateOf(existing?.fatherName ?: "") }
    var mother by remember { mutableStateOf(existing?.motherName ?: "") }
    var className by remember { mutableStateOf(existing?.className ?: "") }
    var roll by remember { mutableStateOf(existing?.roll ?: "") }
    var session by remember { mutableStateOf(existing?.session ?: "") }
    var section by remember { mutableStateOf(existing?.section ?: "") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var photoUri by remember { mutableStateOf(existing?.photoUri ?: "") }
    var error by remember { mutableStateOf("") }

    // Picked photos are copied into the app's internal storage at once, so the
    // photo stays saved even after the app is closed and reopened.
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) { PhotoStore.savePhoto(context, uri) }
                photoUri = saved ?: uri.toString()
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(if (existing == null) "Add Student" else "Edit Student", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        @Composable
        fun Field(v: String, onV: (String) -> Unit, label: String, enabled: Boolean = true) {
            OutlinedTextField(value = v, onValueChange = onV, label = { Text(label) }, enabled = enabled, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
        }
        Field(id, { id = it }, "Student ID *", enabled = existing == null)
        Field(name, { name = it }, "নাম *")
        Field(father, { father = it }, "পিতার নাম")
        Field(mother, { mother = it }, "মাতার নাম")
        Field(className, { className = it }, "শ্রেণি")
        Field(roll, { roll = it }, "রোল")
        Field(session, { session = it }, "সেশন")
        Field(section, { section = it }, "বিভাগ/শাখা")
        Field(phone, { phone = it }, "মোবাইল নম্বর")
        OutlinedButton(onClick = { photoPicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
            Text(if (photoUri.isBlank()) "ছবি বেছে নিন (ঐচ্ছিক)" else "ছবি পরিবর্তন করুন")
        }
        if (error.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(error, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                if (id.isBlank() || name.isBlank()) { error = "Student ID ও নাম অবশ্যই দিতে হবে।"; return@Button }
                val student = Student(id.trim(), name.trim(), father.trim(), mother.trim(), className.trim(), roll.trim(), session.trim(), section.trim(), phone.trim(), photoUri)
                if (photoUri.startsWith("content://")) {
                    // Copy the photo into internal storage first, then save.
                    scope.launch {
                        val saved = withContext(Dispatchers.IO) {
                            PhotoStore.resolveStoredPath(context, photoUri)
                        }
                        vm.saveStudent(student.copy(photoUri = saved))
                        onDone()
                    }
                } else {
                    vm.saveStudent(student)
                    onDone()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save করুন") }
    }
}

@Composable
fun ProfileScreen(
    vm: AppViewModel,
    studentId: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    onAttendance: () -> Unit = {}
) {
    val students by vm.students.collectAsState()
    val institutionName by vm.institutionName.collectAsState()
    val student = students.firstOrNull { it.studentId == studentId }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (student == null) {
            Text("Student পাওয়া যায়নি।", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("ফিরে যান") }
        } else {
            Text("Student Profile", style = MaterialTheme.typography.headlineMedium)
            StudentDetailCard(student, institutionName)
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onAttendance, modifier = Modifier.fillMaxWidth()) { Text("সাপ্তাহিক হাজিরা দেখুন") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Edit করুন (Admin)") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { vm.deleteStudent(student); onDelete() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Delete করুন (Admin)") }
        }
    }
}
