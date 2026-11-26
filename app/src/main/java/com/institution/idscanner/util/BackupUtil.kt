package com.institution.idscanner.util

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/** Copies the Room database file out (backup) or back in (restore).
 *  Restore requires an app restart afterwards. [dbName] is the active
 *  account's database file, so backups stay per-Gmail too. */
object BackupUtil {
    private fun dbFile(context: Context, dbName: String = "student_id_scanner.db") =
        context.getDatabasePath(dbName)

    fun backup(context: Context, out: java.io.OutputStream, dbName: String = "student_id_scanner.db") {
        FileInputStream(dbFile(context, dbName)).use { it.copyTo(out) }
    }

    fun restore(context: Context, input: File, dbName: String = "student_id_scanner.db") {
        val db = dbFile(context, dbName)
        // Close Room first is handled by the caller restarting the process;
        // here we simply overwrite the file.
        input.inputStream().use { src -> FileOutputStream(db).use { src.copyTo(it) } }
    }
}
