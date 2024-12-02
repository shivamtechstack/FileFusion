package shivam.sycodes.filefusion.archievingAndEncryption

import android.app.Notification
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
import shivam.sycodes.filefusion.utility.PermissionHelper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class AESDecryptionService : Service() {
    companion object {
        const val NOTIFICATION_ID = 2
        const val LOCK_FILE_NAME = ".folder_lock_info"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val selectedFiles = intent?.getSerializableExtra("selectedFiles") as? List<File> ?: emptyList()
        val password = intent?.getStringExtra("password")
        val notification = buildNotification("Decrypting files")
        startForeground(NOTIFICATION_ID, notification)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                selectedFiles.forEach { file ->
                    if (file.isDirectory) {
                        decryptDirectory(file, password.toString())
                    } else {
                        val parentFolder = File(file.parent ?: "")
                        decryptFile(file, password.toString(),parentFolder)
                    }
                }
                stopForeground(true)
                stopSelf()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    e.printStackTrace()
                    Toast.makeText(
                        this@AESDecryptionService,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun decryptFile(file: File, password: String, destinationFolder: File) {
        FileInputStream(file).use { fileIn ->
            val salt = ByteArray(16)
            if (fileIn.read(salt) != 16) {
                throw IllegalArgumentException("Invalid file format: Salt missing")
            }

            val iv = ByteArray(16)
            if (fileIn.read(iv) != 16) {
                throw IllegalArgumentException("Invalid file format: IV missing")
            }

            val secretKey = generateKey(password, salt)
            val ivSpec = IvParameterSpec(iv)

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)

            val decryptedFile = File(destinationFolder, file.nameWithoutExtension)

            CipherInputStream(fileIn, cipher).use { cipherIn ->
                FileOutputStream(decryptedFile).use { output ->
                    cipherIn.copyTo(output)
                }
            }

            if (decryptedFile.exists() && decryptedFile.length() > 0) {
                Log.d("AESDecryptionService", "Decryption successful: ${decryptedFile.absolutePath}")
            } else {
                Log.e("AESDecryptionService", "Decryption failed: No output file generated")
            }
        }
    }

    private fun generateKey(password: String, salt: ByteArray): SecretKey {
        val keySpec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
        val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val secretKey = keyFactory.generateSecret(keySpec)
        return SecretKeySpec(secretKey.encoded, "AES")
    }


    private fun decryptDirectory(directory: File, password: String) {
        val lockFile = File(directory, LOCK_FILE_NAME)
        if (!lockFile.exists()) {
            throw Exception("Lock file not found. Directory is not encrypted.")
        }

        val hashedPassword = hashPassword(password)
        val lockFileContent = lockFile.readText()
        if (!lockFileContent.contains(hashedPassword)) {
            throw Exception("Incorrect password.")
        }

        val decryptedFolder = File(directory.parent, directory.nameWithoutExtension)
        if (!decryptedFolder.exists()) decryptedFolder.mkdirs()

        directory.walkTopDown().forEach { file ->
            if (file.isFile && file.name != LOCK_FILE_NAME) {
                decryptFile(file, password, decryptedFolder)
            }
        }

        Log.d("AESDecryptionService", "Decrypted folder: ${decryptedFolder.absolutePath}")
    }


    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun buildNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, PermissionHelper.CHANNEL_ID)
            .setContentTitle("Decryption Service")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.baseline_content_copy_24)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
