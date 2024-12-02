package shivam.sycodes.filefusion.archievingAndEncryption

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import shivam.sycodes.filefusion.R
import shivam.sycodes.filefusion.utility.PermissionHelper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class AESEncryptionServices : Service() {
    companion object {
        const val NOTIFICATION_ID = 1
        const val LOCK_FILE_NAME = ".folder_lock_info"
    }

    private fun encryptFile(file: File, password: String, destinationFolder: File) {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        val secretKey = generateKey(password, salt)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        val ivSpec = IvParameterSpec(iv)

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)

        val encryptedFile = File(destinationFolder, file.name + ".enc")
        FileOutputStream(encryptedFile).use { fileOut ->
            fileOut.write(salt)
            fileOut.write(iv)
            CipherOutputStream(fileOut, cipher).use { cipherOut ->
                file.inputStream().use { input ->
                    input.copyTo(cipherOut)
                }
            }
        }
    }
    private fun generateKey(password: String, salt: ByteArray): SecretKey {
        val keySpec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
        val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val secretKey = keyFactory.generateSecret(keySpec)
        return SecretKeySpec(secretKey.encoded, "AES")
    }

    private fun encryptDirectory(directory: File, password: String) {
        val encryptedFolder = File(directory.parent, directory.name + ".enc")
        if (!encryptedFolder.exists()) encryptedFolder.mkdirs()

        directory.walkTopDown().forEach { file ->
            if (file.isFile) {
                encryptFile(file, password, encryptedFolder)
            }
        }

        val lockFile = File(encryptedFolder, LOCK_FILE_NAME)
        val hashedPassword = hashPassword(password)
        lockFile.writeText("Folder locked with AES\nPassword hash: $hashedPassword")
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val selectedFiles = intent?.getSerializableExtra("selectedFiles") as? List<File> ?: emptyList()
        val password = intent?.getStringExtra("password")
        val notification = buildNotification("Encrypting files")
        startForeground(NOTIFICATION_ID, notification)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                selectedFiles.forEach { file ->
                    if (file.isDirectory) {
                        encryptDirectory(file, password.toString())
                    } else {
                        val parentFolder = File(file.parent ?: "")
                      encryptFile(file,password.toString(),parentFolder)
                    }
                }
                stopForeground(true)
                stopSelf()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    e.printStackTrace()
                    Toast.makeText(
                        this@AESEncryptionServices,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun buildNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, PermissionHelper.CHANNEL_ID)
            .setContentTitle("Encryption Service")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.baseline_content_copy_24)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
