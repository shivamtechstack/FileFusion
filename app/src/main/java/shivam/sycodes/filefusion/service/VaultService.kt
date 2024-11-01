package shivam.sycodes.filefusion.service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import shivam.sycodes.filefusion.R
import shivam.sycodes.filefusion.encryption.VaultEncryptionKeyManager
import shivam.sycodes.filefusion.roomdatabase.AppDatabase
import shivam.sycodes.filefusion.roomdatabase.VaultEntity
import java.io.File
import java.io.FileOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey

class VaultService : Service() {

    companion object {
        const val CHANNEL_ID = "VaultServiceChannel"
        const val NOTIFICATION_ID = 1
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        createNotificationChannel()

        val notification = buildNotification("Encrypting files and moving to vault...")
        startForeground(NOTIFICATION_ID, notification)

        val selectedFiles = intent?.getSerializableExtra("selectedFiles") as? List<File> ?: emptyList()

        if (selectedFiles.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                moveFilesToVault(selectedFiles)
                stopSelf()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private suspend fun moveFilesToVault(selectedFiles: List<File>) {
        val vaultDir = getVaultDir(applicationContext)
        val secretKey = VaultEncryptionKeyManager.getKey()

        selectedFiles.forEach { file ->
            if (file.isDirectory) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Directory cannot be moved", Toast.LENGTH_SHORT).show()
                }
            } else {
                val encryptedFile = File(vaultDir, "${file.name}.enc")
                encryptAndMoveFile(file, encryptedFile, secretKey)

                saveVaultFileRecord(file.absolutePath, encryptedFile.absolutePath)
            }
        }
    }

    private fun encryptAndMoveFile(file: File, encryptedFile: File, secretKey: SecretKey) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv

        FileOutputStream(encryptedFile).use { fos ->
            fos.write(iv)
            CipherOutputStream(fos, cipher).use { cos ->
                file.inputStream().use { it.copyTo(cos) }
            }
        }
        file.delete()
    }

    private fun getVaultDir(context: Context): File {
        val vaultDir = File(context.filesDir, "vault")
        if (!vaultDir.exists()) {
            vaultDir.mkdirs()
            File(vaultDir, ".nomedia").createNewFile()
        }
        return vaultDir
    }
    private suspend fun saveVaultFileRecord(originalPath: String, vaultPath: String) {
       val vaultDao = AppDatabase.getDatabase(applicationContext).itemDAO()
        val trashItem = VaultEntity(
            fileName = File(originalPath).name,
            originalPath = originalPath,
            vaultPath = vaultPath,
            transferDate = System.currentTimeMillis()
        )
        vaultDao.addVaultItem(trashItem)
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

    override fun onBind(intent: Intent?): IBinder? = null
}
