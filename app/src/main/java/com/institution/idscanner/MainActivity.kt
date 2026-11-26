package com.institution.idscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.institution.idscanner.data.AccountStore
import com.institution.idscanner.data.AppDatabase
import com.institution.idscanner.data.SettingsStore
import com.institution.idscanner.data.StudentRepository
import com.institution.idscanner.ui.AppNav
import com.institution.idscanner.ui.theme.StudentIDScannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Read the active Gmail synchronously so the correct per-account
        // database and settings are used before any UI is built.
        val accountEmail = AccountStore.readCurrentEmail(this)
        val accountStore = AccountStore(this)
        val db = AppDatabase.get(this, accountEmail)
        val repo = StudentRepository(db)
        val settings = SettingsStore(this, AccountStore.accountKey(accountEmail))
        val dbFileName = AppDatabase.databaseNameFor(accountEmail)
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AppViewModel(repo, settings, accountStore, dbFileName) as T
        }
        setContent {
            StudentIDScannerTheme {
                val vm: AppViewModel = viewModel(factory = factory)
                AppNav(vm)
            }
        }
    }
}
