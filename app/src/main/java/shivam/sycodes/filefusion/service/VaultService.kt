package shivam.sycodes.filefusion.service
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import shivam.sycodes.filefusion.R
import shivam.sycodes.filefusion.roomdatabase.AppDatabase
import shivam.sycodes.filefusion.roomdatabase.VaultEntity
import java.io.File

class VaultService : Service() {

    companion object {
        const val CHANNEL_ID = "VaultServiceChannel"
        const val NOTIFICATION_ID = 1
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("SuspiciousIndentation")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        val action = intent?.action
        val selectedFiles = intent?.getSerializableExtra("selectedFiles") as? List<File> ?: emptyList()

        val notification = when (action) {
            "MOVE_OUT_OF_VAULT" -> buildNotification("Decrypting files and moving out of vault...")
            "MOVE_TO_VAULT" -> buildNotification("Encrypting files and moving to vault...")
            else -> buildNotification("Processing files...")
        }
        startForeground(NOTIFICATION_ID, notification)

        serviceScope.launch {
            when (action) {
                "MOVE_TO_VAULT" -> {
                    for (file in selectedFiles) {
                        moveToVault(applicationContext, file)
                    }
                }
                "MOVE_OUT_OF_VAULT" -> {
                    for (file in selectedFiles) {
                        val database = AppDatabase.getDatabase(applicationContext).itemDAO()
                        val vaultItem = database.getVaultItemByPath(file.absolutePath)
                        val originalPath = vaultItem?.originalPath
                            moveOutOfVault(applicationContext, file, originalPath.toString())
                    }
                }
            }
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun getVaultDir(context: Context): File {
        val vaultDir = File(context.filesDir, "vault")
        vaultDir.setWritable(true)
        if (!vaultDir.exists()) {
            vaultDir.mkdirs()
        }
        return vaultDir
    }

    private suspend fun moveToVault(context: Context, file: File) {
        val vaultFile = File(getVaultDir(context), file.name)

        file.inputStream().use { inputStream ->
            vaultFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        saveVaultFileRecord(file.name, file.absolutePath, vaultFile.absolutePath)
        file.delete()
    }

    private suspend fun moveOutOfVault(context: Context, vaultFile: File, originalPath: String) {
        val originalFile = File(originalPath)
        val parentFolder = originalFile.parentFile

        if (parentFolder != null && !parentFolder.exists()) {
            if (!parentFolder.mkdirs()) {
                Log.e("VaultService", "Failed to create parent directory: ${parentFolder.absolutePath}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to create parent directory", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }

        if (parentFolder?.canWrite() != true) {
            Log.e("VaultService", "Original file path is not writable!")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Original file path is not writable", Toast.LENGTH_SHORT).show()
            }
            return
        }

        try {
            vaultFile.inputStream().use { inputStream ->
                originalFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            MediaScannerConnection.scanFile(context, arrayOf(originalFile.absolutePath), null, null)

            val database = AppDatabase.getDatabase(context).itemDAO()
            val vaultItem = database.getVaultItemByPath(vaultFile.absolutePath)

            vaultFile.delete()
            vaultItem?.let {
                database.deleteVaultItem(it)
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "File restored to original location: ${originalFile.absolutePath}",
                    Toast.LENGTH_LONG
                ).show()
            }
            val intent = Intent("shivam.sycodes.filefusion.MOVE_OUT_OF_VAULT_COMPLETE")
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e("VaultService", "Error while restoring file: ${e.localizedMessage}")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "An error occurred while restoring the file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun saveVaultFileRecord(fileName: String, originalPath: String, vaultPath: String) {
        val database = AppDatabase.getDatabase(applicationContext).itemDAO()
        val vaultItem = VaultEntity(
            fileName = fileName,
            originalPath = originalPath,
            vaultPath = vaultPath,
            transferDate = System.currentTimeMillis()
        )
        database.addVaultItem(vaultItem)
    }

    private fun buildNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Vault Service")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.baseline_content_copy_24)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Vault Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

}

