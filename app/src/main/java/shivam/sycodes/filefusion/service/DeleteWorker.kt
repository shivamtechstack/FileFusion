package shivam.sycodes.filefusion.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import shivam.sycodes.filefusion.utility.PermissionHelper
import java.io.File

class DeleteWorker(private val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    private val permissionHelper: PermissionHelper = PermissionHelper(context)
    override fun doWork(): Result {
        val filesToDelete = inputData.getStringArray(KEY_FILES_TO_DELETE)?.toList() ?: return Result.failure()

        val name= "File Deletion"
        val descriptionText = "Deleting selected items"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name ,importance).apply {
            description = descriptionText
        }
        val notificationManager : NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        var success = true
        filesToDelete.forEach {
            val file = File(it)
            val deleted = if (file.isDirectory) deleteDirectory(file) else file.delete()
            updateNotification(file.name, deleted)
            if (!deleted){
                success = false
            }

        }
        return if (success) Result.success() else Result.failure()
    }
    private fun deleteDirectory(directory: File): Boolean {
        return try {
            directory.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    deleteDirectory(file)
                } else {
                    file.delete()
                }
            }
            directory.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    @SuppressLint("MissingPermission")
    private fun updateNotification(fileName: String, success: Boolean) {

        if(!permissionHelper.isNotificationPermissionGranted()){
            return
        }else {
            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_delete)
                .setContentTitle(if (success) "Deleted" else "Failed to delete")
                .setContentText("File: $fileName")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(fileName.hashCode(), notification)
        }
    }

    companion object {
        const val KEY_FILES_TO_DELETE = "KEY_FILES_TO_DELETE"
        private const val NOTIFICATION_CHANNEL_ID = "file_deletion_channel"
    }
}