package shivam.sycodes.filefusion.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import shivam.sycodes.filefusion.R
import java.io.File

class PasteWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val channelId = "paste_channel"
    private val notificationId = 1
    private lateinit var filesToPaste: List<File>
    private var destinationPath: String? = null
    private var isCutOperation = false
    private var totalCopied: Long = 0L
    private val notificationManager = NotificationManagerCompat.from(applicationContext)

    override suspend fun doWork(): Result {
        val filePaths = inputData.getStringArray("FILES_TO_PASTE")?.toList() ?: return Result.failure()
        destinationPath = inputData.getString("DESTINATION_PATH")
        isCutOperation = inputData.getBoolean("IS_CUT_OPERATION", false)
        filesToPaste = filePaths.map { File(it) }

        createNotificationChannel()
        withContext(Dispatchers.IO) {
            pasteFiles()
        }
        return Result.success()
    }

    private fun pasteFiles() {
        val destinationDirectory = File(destinationPath ?: return).apply { mkdirs() }

        val totalSize = calculateTotalSize(filesToPaste)


        filesToPaste.forEach { file ->
            val targetFile = File(destinationDirectory, file.name)

            if (isCutOperation) {
                totalCopied += moveFileOrDirectory(file, targetFile)
            } else {
                totalCopied += copyFileOrDirectory(file, targetFile, totalSize) { currentFile, copiedBytes ->
                    updateNotification(currentFile.name, currentFile.length(), copiedBytes, totalCopied, totalSize)
                }
            }
        }
        updateNotification("Completed", 0, 0, totalSize, totalSize)
        completeNotification()
    }

    @SuppressLint("MissingPermission")
    private fun updateNotification(
        currentFileName: String,
        currentFileSize: Long,
        copiedBytes: Long,
        totalCopied: Long,
        totalSize: Long,
    ) {
        val remoteViews = RemoteViews(applicationContext.packageName, R.layout.notification_layout).apply {
                setTextViewText(R.id.currentFileName, "Current file: $currentFileName")
                setTextViewText(R.id.currentFileSizeText, "Size: ${formatSize(currentFileSize)}")
                setTextViewText(R.id.totalCopied, "Copied: ${formatSize(copiedBytes)}")

                val currentFileProgress = if (currentFileSize > 0) ((copiedBytes * 100) / currentFileSize).toInt() else 100
                setProgressBar(R.id.currentFileProgress, 100, currentFileProgress, false)

            setTextViewText(R.id.allFileSizeText, "Total Size: ${formatSize(totalSize)}")
            val combinedProgress = if (totalSize > 0) ((totalCopied * 100) / totalSize).toInt() else 100
            setProgressBar(R.id.allFilesProgress, 100, combinedProgress, false)
        }

        val notificationBuilder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setOngoing(true)
            .setCustomContentView(remoteViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())

        notificationManager.notify(notificationId, notificationBuilder.build())
    }


    @SuppressLint("DefaultLocale")
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

    @SuppressLint("MissingPermission")
    private fun completeNotification() {
        val completeNotification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Paste Complete")
            .setContentText("All files pasted successfully.")
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setOngoing(false)
            .build()

        notificationManager.notify(notificationId, completeNotification)
    }

    private  fun copyFileOrDirectory(
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
                val childCopied = copyFileOrDirectory(child, File(destination, child.name), totalSize, onProgress)
                copiedBytes += childCopied
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

    private fun createNotificationChannel() {
            val channel = NotificationChannel(channelId, "Paste Operation", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Notifications for ongoing paste operations"}
            applicationContext.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
