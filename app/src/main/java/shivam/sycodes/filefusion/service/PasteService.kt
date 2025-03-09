package shivam.sycodes.filefusion.service

import android.Manifest
import android.R
import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class PasteService : Service() {

    private val notificationId = 3
    private lateinit var fileToPaste: File
    private var destinationPath: String? = null
    private var isCutOperation = false
    private val binder = LocalBinder()
    private var intentData: Intent? = null
    private var totalBytes: Long = 0
    private var copiedBytes: Long = 0

    inner class LocalBinder : Binder() {
        fun getService(): PasteService = this@PasteService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        this.intentData = intent

        val filePath = intent?.getStringExtra("FILE_TO_PASTE") ?: return START_NOT_STICKY
        destinationPath = intent.getStringExtra("DESTINATION_PATH")
        isCutOperation = intent.getBooleanExtra("IS_CUT_OPERATION", false)
        fileToPaste = File(filePath)

        startForeground(notificationId, createInitialNotification())

        CoroutineScope(Dispatchers.IO).launch {
            pasteFile()
            stopSelf()
        }
        return START_STICKY
    }

    private suspend fun pasteFile() {
        val destinationDirectory = File(destinationPath ?: return).apply { mkdirs() }
        val targetFile = File(destinationDirectory, fileToPaste.name)

        totalBytes = getTotalBytes(fileToPaste)
        copiedBytes = 0

        if (isCutOperation) {
            moveFileOrDirectory(fileToPaste, targetFile)
        } else {
            copyFileOrDirectory(fileToPaste, targetFile)
        }
        completeNotification()
    }

    private fun getTotalBytes(file: File): Long {
        return if (file.isDirectory) {
            file.walkBottomUp().sumOf { it.length() }
        } else {
            file.length()
        }
    }

    private fun createInitialNotification(): Notification {
        return NotificationCompat.Builder(this, "fileoperation")
            .setContentTitle("Pasting File")
            .setContentText("Pasting ${fileToPaste.name} to $destinationPath")
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setOngoing(true)
            .setProgress(100, 0, false)
            .build()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun updateNotificationProgress(progress: Int) {
        val notification = NotificationCompat.Builder(this, "fileoperation")
            .setContentTitle("Pasting File")
            .setContentText("Pasting ${fileToPaste.name} to $destinationPath")
            .setSmallIcon(R.drawable.ic_menu_save)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, false)
            .build()
        NotificationManagerCompat.from(this).notify(notificationId, notification)
    }

    @SuppressLint("MissingPermission")
    private fun completeNotification() {
        val notification = NotificationCompat.Builder(this, "fileoperation")
            .setContentTitle("Paste Complete")
            .setContentText("${fileToPaste.name} pasted successfully.")
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setOngoing(false)
            .setProgress(0, 0, false)
            .build()
        NotificationManagerCompat.from(this).notify(notificationId, notification)

        val intent = Intent("shivam.sycodes.filefusion.PASTE_COMPLETE")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

        CoroutineScope(Dispatchers.Main).launch {
            kotlinx.coroutines.delay(5000)
            NotificationManagerCompat.from(this@PasteService).cancel(notificationId)
        }
    }

    @SuppressLint("MissingPermission")
    private fun copyFileOrDirectory(source: File, destination: File) {
        val buffer = ByteArray(1024 * 128)

        if (source.isDirectory) {
            destination.mkdirs()
            source.listFiles()?.forEach { child ->
                copyFileOrDirectory(child, File(destination, child.name))
            }
        } else {
            source.inputStream().use { input ->
                destination.outputStream().use { output ->
                    var bytes = input.read(buffer)
                    while (bytes >= 0) {
                        output.write(buffer, 0, bytes)
                        copiedBytes += bytes
                        val progress = ((copiedBytes * 100) / totalBytes).toInt()
                        updateNotificationProgress(progress)
                        bytes = input.read(buffer)
                    }
                }
            }
        }
    }

    private fun moveFileOrDirectory(source: File, destination: File) {
        if (source.isDirectory) {
            destination.mkdirs()
            source.listFiles()?.forEach { child ->
                moveFileOrDirectory(child, File(destination, child.name))
            }
            source.deleteRecursively()
        } else {
            source.renameTo(destination)
        }
    }
}

