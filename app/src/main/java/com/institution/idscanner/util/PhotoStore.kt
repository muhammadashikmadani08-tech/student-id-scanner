package com.institution.idscanner.util

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Student photos are copied into the app's own internal storage, so they
 * survive app restarts. A raw gallery content:// URI is NOT durable: the
 * system picker grant expires and the photo then disappears from profiles.
 * Stored values are absolute file paths under filesDir/photos/.
 */
object PhotoStore {
    private fun photosDir(context: Context): File =
        File(context.filesDir, "photos").also { if (!it.exists()) it.mkdirs() }

    /** Copies [uri] into internal storage and returns the saved file path. */
    fun savePhoto(context: Context, uri: Uri): String? {
        return try {
            val ext = context.contentResolver.getType(uri)?.substringAfter("/")?.takeIf { it.length in 3..4 } ?: "jpg"
            val file = File(photosDir(context), "photo_${System.currentTimeMillis()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    /** Copies a still-unresolved content URI (if any) into internal storage. */
    fun resolveStoredPath(context: Context, stored: String): String {
        if (!stored.startsWith("content://")) return stored
        return try {
            savePhoto(context, Uri.parse(stored)) ?: stored
        } catch (_: Exception) {
            stored
        }
    }

    fun deletePhoto(stored: String) {
        try {
            if (stored.startsWith("/")) {
                val file = File(stored)
                if (file.exists()) file.delete()
            }
        } catch (_: Exception) {
        }
    }
}
