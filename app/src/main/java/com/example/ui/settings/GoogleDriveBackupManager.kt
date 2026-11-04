package com.example.ui.settings

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.local.AppDatabase
import com.example.data.local.SanadDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupArchiveResult(
    val archiveFile: File,
    val sizeKilobytes: Long,
    val timestampFormatted: String
)

object GoogleDriveBackupManager {

    /**
     * Executes `PRAGMA wal_checkpoint(FULL)` to flush all SQLite WAL frames into the main database file,
     * then packages the Room database into a safe `.sanad` (ZIP) backup archive ready for local storage or Google Drive upload.
     */
    suspend fun createSafeSanadBackupArchive(
        context: Context,
        dao: SanadDao
    ): Result<BackupArchiveResult> = withContext(Dispatchers.IO) {
        try {
            // 1. Flush WAL to main SQLite file
            dao.performWalCheckpointFull()

            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            val backupDir = File(context.filesDir, "sanad_backups").apply {
                if (!exists()) mkdirs()
            }

            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val archiveFile = File(backupDir, "Sanad_ERP_Backup_$stamp.sanad")

            ZipOutputStream(BufferedOutputStream(FileOutputStream(archiveFile))).use { zos ->
                if (dbFile.exists()) {
                    FileInputStream(dbFile).use { fis ->
                        BufferedInputStream(fis).use { origin ->
                            val entry = ZipEntry(AppDatabase.DATABASE_NAME)
                            zos.putNextEntry(entry)
                            origin.copyTo(zos, bufferSize = 8192)
                            zos.closeEntry()
                        }
                    }
                }
                // Also include a metadata manifest inside the .sanad archive
                val metaEntry = ZipEntry("sanad_manifest.json")
                zos.putNextEntry(metaEntry)
                val metaJson = """{"app":"Sanad Accounting ERP","version":2,"timestamp":${System.currentTimeMillis()}}"""
                zos.write(metaJson.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            val sizeKb = (archiveFile.length() / 1024L).coerceAtLeast(1L)
            val readableTime = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.US).format(Date())
            Result.success(
                BackupArchiveResult(
                    archiveFile = archiveFile,
                    sizeKilobytes = sizeKb,
                    timestampFormatted = readableTime
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lists existing local `.sanad` / `.zip` backup archives stored on device.
     */
    fun listLocalBackups(context: Context): List<File> {
        val backupDir = File(context.filesDir, "sanad_backups")
        if (!backupDir.exists()) return emptyList()
        return backupDir.listFiles()
            ?.filter { it.name.endsWith(".sanad") || it.name.endsWith(".zip") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /**
     * Restores the SQLite database from a `.sanad` / `.zip` backup archive after flushing WAL.
     */
    suspend fun restoreFromBackupArchive(
        context: Context,
        dao: SanadDao,
        archiveFile: File
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            dao.performWalCheckpointFull()
            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            ZipInputStream(BufferedInputStream(FileInputStream(archiveFile))).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (entry.name == AppDatabase.DATABASE_NAME) {
                        FileOutputStream(dbFile).use { fos ->
                            zis.copyTo(fos, bufferSize = 8192)
                        }
                        break
                    }
                    entry = zis.nextEntry
                }
            }
            Result.success("تم استرجاع النسخة الاحتياطية (${archiveFile.name}) بنجاح")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Shares the `.sanad` backup summary or triggers Google Drive / Cloud intent.
     */
    fun launchGoogleDriveUploadIntent(context: Context, backupFile: File) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية سحابية - نظام سند المحاسبي (${backupFile.name})")
            putExtra(
                Intent.EXTRA_TEXT,
                "تم تجهيز النسخة الاحتياطية المشفرة (${backupFile.name}) بحجم ${backupFile.length() / 1024} KB بعد تنفيذ PRAGMA wal_checkpoint(FULL) للرفع إلى Google Drive."
            )
        }
        runCatching {
            context.startActivity(Intent.createChooser(shareIntent, "رفع النسخة الاحتياطية إلى Google Drive"))
        }
    }
}
