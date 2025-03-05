package shivam.sycodes.filefusion.archievingAndEncryption

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import shivam.sycodes.filefusion.R
import shivam.sycodes.filefusion.utility.PermissionHelper
import java.io.File
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

    private fun encryptFile(file: File, password: String, destinationFolder: File, notificationId: Int) {
        val fileSize = file.length()
        var bytesCopied = 0L
        var lastUpdateTime = System.currentTimeMillis()

        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        val secretKey = generateKey(password, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        val ivSpec = IvParameterSpec(iv)

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)

        val encryptedFileName = resolveUniqueFileName(destinationFolder, file.name + ".enc")
        val encryptedFile = File(destinationFolder, encryptedFileName)

        FileOutputStream(encryptedFile).use { fileOut ->
            fileOut.write(salt)
            fileOut.write(iv)
            CipherOutputStream(fileOut, cipher).use { cipherOut ->
                file.inputStream().use { input ->

                    val buffer = ByteArray(65536)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        cipherOut.write(buffer, 0, bytesRead)
                        bytesCopied += bytesRead

                        val progress = ((bytesCopied * 100) / fileSize).toInt()
                        val currentTime = System.currentTimeMillis()

                        if (currentTime - lastUpdateTime > 100 || progress % 5 == 0) {
                            updateProgressNotification(notificationId, file.name, progress)
                            lastUpdateTime = currentTime
                        }
                    }
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
        val encryptedFolderName = resolveUniqueFileName(File(directory.parent!!), directory.name + ".enc")
        val encryptedFolder = File(directory.parent, encryptedFolderName)
        if (!encryptedFolder.exists()) encryptedFolder.mkdirs()

        directory.walkTopDown().forEach { file ->
            if (file.isFile) {
                encryptFile(file, password, encryptedFolder, NOTIFICATION_ID)
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

        val initialNotification = NotificationCompat.Builder(this, PermissionHelper.CHANNEL_ID)
            .setContentTitle("Initializing Encryption")
            .setContentText("Preparing files...")
            .setSmallIcon(R.drawable.baseline_content_copy_24)
            .setProgress(100, 0, true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .build()

        startForeground(NOTIFICATION_ID, initialNotification)

        CoroutineScope(Dispatchers.IO).launch {

            try {
                selectedFiles.forEach { file ->
                    if (file.isDirectory) {
                        encryptDirectory(file, password.toString())
                    } else {
                        val parentFolder = File(file.parent ?: "")
                      encryptFile(file,password.toString(),parentFolder, NOTIFICATION_ID)
                    }
                }
                showCompletionNotification()
                withContext(Dispatchers.IO){
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

    private fun resolveUniqueFileName(parentFolder: File, fileName: String): String {
        val baseName = fileName.substringBeforeLast(".")
        val extension = fileName.substringAfterLast(".", "")
        var uniqueName = fileName
        var counter = 1

        while (File(parentFolder, uniqueName).exists()) {
            val timestamp = System.currentTimeMillis()
            uniqueName = if (extension.isNotEmpty()) {
                "$baseName-$timestamp.$extension"
            } else {
                "$baseName-$timestamp"
            }
        }
        return uniqueName
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @SuppressLint("MissingPermission")
    private fun updateProgressNotification(notificationId: Int, fileName: String, progress: Int) {
        val notificationManager = NotificationManagerCompat.from(this)

        val notification = NotificationCompat.Builder(this, PermissionHelper.CHANNEL_ID)
            .setContentTitle("Encrypting: $fileName")
            .setContentText("Progress: $progress%")
            .setSmallIcon(R.drawable.baseline_content_copy_24)
            .setProgress(100, progress, false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    @SuppressLint("MissingPermission")
    private fun showCompletionNotification() {
        val notificationManager = NotificationManagerCompat.from(this)

        notificationManager.cancel(NOTIFICATION_ID)

        val notification = NotificationCompat.Builder(this, PermissionHelper.CHANNEL_ID)
            .setContentTitle("Encryption Complete")
            .setContentText("All files encrypted successfully.")
            .setSmallIcon(R.drawable.baseline_done_all_24)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
        CoroutineScope(Dispatchers.Main).launch {
            kotlinx.coroutines.delay(3000)
            notificationManager.cancel(NOTIFICATION_ID)
        }
    }
}
