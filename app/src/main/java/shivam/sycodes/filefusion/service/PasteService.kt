package shivam.sycodes.filefusion.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class PasteService : Service() {

    private val notificationId = 1
    private lateinit var filesToPaste: List<File>
    private var destinationPath: String? = null
    private var isCutOperation = false
    private var totalCopied: Long = 0L
    private var totalSize: Long = 0L
    private val binder = LocalBinder()
    private var intentData: Intent? = null

    inner class LocalBinder : Binder() {
        fun getService(): PasteService = this@PasteService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        this.intentData = intent
        val filePaths =
            intent?.getStringArrayExtra("FILES_TO_PASTE")?.toList() ?: return START_NOT_STICKY
        destinationPath = intent.getStringExtra("DESTINATION_PATH")
        isCutOperation = intent.getBooleanExtra("IS_CUT_OPERATION", false)
        filesToPaste = filePaths.map { File(it) }

        startForeground(notificationId, createNotification("Preparing paste operation...", 0, 0))

        CoroutineScope(Dispatchers.IO).launch {
            pasteFiles()
            stopSelf()
        }
        return START_STICKY
    }

    private suspend fun pasteFiles() {
        val destinationDirectory = File(destinationPath ?: return).apply { mkdirs() }
        totalSize = calculateTotalSize(filesToPaste)

        filesToPaste.forEach { file ->
            val targetFile = File(destinationDirectory, file.name)
            if (isCutOperation) {
                totalCopied += moveFileOrDirectory(file, targetFile)
            } else {
                totalCopied += copyFileOrDirectory(
                    file,
                    targetFile,
                    totalSize
                ) { currentFile, copiedBytes ->
                    updateNotification(
                        currentFile.name,
                        currentFile.length(),
                        copiedBytes,
                        totalCopied,
                        totalSize
                    )
                }
            }
        }
        completeNotification()
    }

    private fun createNotification(message: String, progress: Int, max: Int): Notification {
        return NotificationCompat.Builder(this, "fileoperation")
            .setContentTitle("Paste Operation")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setProgress(max, progress, false)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun updateNotification(
        currentFileName: String,
        currentFileSize: Long,
        copiedBytes: Long,
        totalCopied: Long,
        totalSize: Long
    ) {
        val notification = NotificationCompat.Builder(this, "fileoperation")
            .setContentTitle("Pasting: $currentFileName")
            .setContentText("Progress: ${formatSize(totalCopied)} / ${formatSize(totalSize)}")
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setProgress(100, ((totalCopied.toDouble() / totalSize) * 100).toInt(), false)
            .build()
        NotificationManagerCompat.from(this).notify(notificationId, notification)
    }

    @SuppressLint("MissingPermission")
    private fun completeNotification() {
        val notification = NotificationCompat.Builder(this, "fileoperation")
            .setContentTitle("Paste Complete")
            .setContentText("All files pasted successfully.")
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setOngoing(false)
            .build()
        NotificationManagerCompat.from(this).notify(notificationId, notification)
    }

    private fun formatSize(bytes: Long): String {
        val kb = 1024L
        val mb = kb * 1024
        val gb = mb * 1024

        return when {
            bytes >= gb -> String.format("%.2f GB", bytes / gb.toFloat())
            bytes >= mb -> String.format("%.2f MB", bytes / mb.toFloat())
            bytes >= kb -> String.format("%.2f KB", bytes / kb.toFloat())
            else -> "$bytes Bytes"
        }
    }

    private fun calculateTotalSize(files: List<File>): Long {
        return files.sumOf { file ->
            if (file.isDirectory) {
                file.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            } else {
                file.length()
            }
        }
    }

    private fun copyFileOrDirectory(
        source: File,
        destination: File,
        totalSize: Long,
        onProgress: (File, Long) -> Unit
    ): Long {
        var copiedBytes = 0L
        val buffer = ByteArray(1024 * 128)

        if (source.isDirectory) {
            destination.mkdirs()
            source.listFiles()?.forEach { child ->
                copiedBytes += copyFileOrDirectory(child, File(destination, child.name), totalSize, onProgress)
            }
        } else {
            source.inputStream().use { input ->
                destination.outputStream().use { output ->
                    var bytes = input.read(buffer)
                    while (bytes >= 0) {
                        output.write(buffer, 0, bytes)
                        copiedBytes += bytes
                        onProgress(source, copiedBytes)
                        bytes = input.read(buffer)
                    }
                }
            }
        }
        return copiedBytes
    }

    private fun moveFileOrDirectory(source: File, destination: File): Long {
        val size = source.length()
        if (source.isDirectory) {
            destination.mkdirs()
            source.listFiles()?.forEach { child ->
                moveFileOrDirectory(child, File(destination, child.name))
            }
            source.deleteRecursively()
        } else {
            source.renameTo(destination)
        }
        return size
    }
}

