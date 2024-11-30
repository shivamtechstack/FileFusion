package shivam.sycodes.filefusion.archievingAndEncryption

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import shivam.sycodes.filefusion.R
import shivam.sycodes.filefusion.service.VaultService
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class AESEncryptionServices : Service() {
    companion object {
        const val NOTIFICATION_ID = 1
    }

    private fun encryptFile(file: File, password: String) {
        val secretKey = generateKey(password)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        val ivSpec = IvParameterSpec(iv)

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)

        val inputStream = FileInputStream(file)
        val outputFile = File(file.parent, file.name + ".enc")
        val outputStream = FileOutputStream(outputFile)

        outputStream.use {
            it.write(iv)
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                val encrypted = cipher.update(buffer, 0, bytesRead)
                if (encrypted != null) {
                    it.write(encrypted)
                }
            }
            val finalBytes = cipher.doFinal()
            if (finalBytes != null) {
                it.write(finalBytes)
            }
        }

        inputStream.close()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val selectedFiles = intent?.getSerializableExtra("selectedFiles") as? List<File> ?: emptyList()
        val password = intent?.getStringExtra("password")
        val notification = buildNotification("Encrypting files")
        startForeground(NOTIFICATION_ID, notification)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (selectedFiles.size == 1) {
                    val file = selectedFiles.first()
                    encryptFile(file, password.toString())
                } else {
                    TODO()
                }
                stopForeground(true)
                stopSelf()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    e.printStackTrace()
                    Toast.makeText(this@AESEncryptionServices, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

        }

        return START_NOT_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
    private fun generateKey(password: String): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        val secureRandom = SecureRandom.getInstance("SHA1PRNG")
        secureRandom.setSeed(password.toByteArray())
        keyGenerator.init(256, secureRandom)
        return keyGenerator.generateKey()
    }
    private fun buildNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, VaultService.CHANNEL_ID)
            .setContentTitle("Encryption Service")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.baseline_content_copy_24)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            VaultService.CHANNEL_ID,
            "Encrption Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)
    }
}