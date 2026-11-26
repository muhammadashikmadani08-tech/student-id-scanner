package com.institution.idscanner.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.institution.idscanner.AppViewModel
import com.institution.idscanner.util.BackupUtil
import com.institution.idscanner.util.CsvUtil
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AdminScreen(
    vm: AppViewModel,
    onStudents: () -> Unit,
    onAdd: () -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pin by vm.adminPin.collectAsState()
    var entered by remember { mutableStateOf("") }
    var unlocked by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val students by vm.students.collectAsState()

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) scope.launch {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { vm.importStudents(CsvUtil.parse(it)) }
                message = "CSV import সম্পন্ন হয়েছে।"
            }.onFailure { message = "Import ব্যর্থ: ${it.message}" }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri != null) scope.launch {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(CsvUtil.toCsv(students).toByteArray(Charsets.UTF_8))
                }
                message = "CSV export সম্পন্ন হয়েছে।"
            }.onFailure { message = "Export ব্যর্থ: ${it.message}" }
        }
    }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri: Uri? ->
        if (uri != null) scope.launch {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { BackupUtil.backup(context, it, vm.databaseFileName) }
                message = "Backup সম্পন্ন হয়েছে।"
            }.onFailure { message = "Backup ব্যর্থ: ${it.message}" }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) scope.launch {
            runCatching {
                val tmp = File(context.cacheDir, "restore.db")
                context.contentResolver.openInputStream(uri)?.use { src -> tmp.outputStream().use { src.copyTo(it) } }
                BackupUtil.restore(context, tmp, vm.databaseFileName)
                message = "Restore সম্পন্ন — অ্যাপ বন্ধ করে আবার খুলুন।"
            }.onFailure { message = "Restore ব্যর্থ: ${it.message}" }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Admin Section", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        if (!unlocked) {
            Text("Admin PIN দিন (ডিফল্ট: 1234)। Settings থেকে PIN বদলানো যায়।")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = entered, onValueChange = { entered = it },
                label = { Text("PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { unlocked = entered == pin; if (!unlocked) message = "ভুল PIN।" },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Unlock করুন") }
            if (message.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(message) }
        } else {
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("Add Student") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onStudents, modifier = Modifier.fillMaxWidth()) { Text("Students দেখুন / Edit / Delete") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*")) }, modifier = Modifier.fillMaxWidth()) {
                Text("CSV/Excel (CSV) থেকে Import করুন")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { exportLauncher.launch("students.csv") }, modifier = Modifier.fillMaxWidth()) {
                Text("CSV-এ Export করুন")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { backupLauncher.launch("student_backup.db") }, modifier = Modifier.fillMaxWidth()) {
                Text("Database Backup নিন")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { restoreLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
                Text("Database Restore করুন")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) {
                Text("প্রতিষ্ঠানের নাম ও PIN পরিবর্তন করুন")
            }
            if (message.isNotBlank()) { Spacer(Modifier.height(10.dp)); Text(message) }
            Spacer(Modifier.height(10.dp))
            Text("Excel ফাইল সরাসরি নয় — Excel-এ File > Save As > CSV UTF-8 করে import করুন।", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel, onDone: () -> Unit) {
    val institutionName by vm.institutionName.collectAsState()
    val pin by vm.adminPin.collectAsState()
    val currentEmail by vm.currentAccountEmail.collectAsState()
    var name by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var first by remember { mutableStateOf(true) }
    if (first && institutionName.isNotBlank()) { name = institutionName; first = false }

    // Gmail account section: each Gmail keeps its own separate local data.
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var emailInput by remember { mutableStateOf("") }
    var emailFirst by remember { mutableStateOf(true) }
    if (emailFirst && currentEmail.isNotBlank()) { emailInput = currentEmail; emailFirst = false }
    var emailError by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text("প্রতিষ্ঠানের নাম (Home Screen-এ দেখাবে; Logo বদলাতে res/drawable-এর আইকন বদলান — README দেখুন)।")
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("প্রতিষ্ঠানের নাম") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Text("বর্তমান Admin PIN: $pin")
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = newPin, onValueChange = { newPin = it }, label = { Text("নতুন PIN (4 সংখ্যা)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = {
                if (name.isNotBlank()) vm.setInstitutionName(name.trim())
                if (newPin.length >= 4) vm.setAdminPin(newPin.trim())
                onDone()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save করুন") }
        Spacer(Modifier.height(20.dp))
        Text("Gmail অ্যাকাউন্ট", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            if (currentEmail.isBlank()) "কোনো Gmail দেওয়া হয়নি"
            else "বর্তমান Gmail: $currentEmail"
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = emailInput, onValueChange = { emailInput = it; emailError = "" },
            label = { Text("Gmail লিখুন") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        if (emailError.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(emailError, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                val normalised = com.institution.idscanner.data.AccountStore.normaliseEmail(emailInput)
                if (!com.institution.idscanner.data.AccountStore.isValidEmail(normalised)) {
                    emailError = "সঠিক Gmail লিখুন (যেমন name@gmail.com)।"
                    return@OutlinedButton
                }
                scope.launch {
                    vm.setAccountEmail(normalised)
                    // Restart so the other account's database and settings load.
                    val activity = context as? android.app.Activity
                    activity?.let {
                        val intent = it.intent
                        it.finish()
                        it.startActivity(intent)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("অ্যাকাউন্ট বদলান") }
        Spacer(Modifier.height(8.dp))
        Text("তথ্য ফোনেই আলাদাভাবে থাকবে; Gmail-এ আপলোড হবে না।", style = MaterialTheme.typography.bodyMedium)
    }
}
