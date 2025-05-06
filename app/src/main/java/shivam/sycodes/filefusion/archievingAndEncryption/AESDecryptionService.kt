package shivam.sycodes.filefusion.archievingAndEncryption

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
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
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.coroutines.cancellation.CancellationException

class AESDecryptionService : Service() {
    private var isDecryptionCancelled = false
    companion object {
        const val NOTIFICATION_ID = 2
        const val LOCK_FILE_NAME = ".folder_lock_info"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent?.action == "ACTION_CANCEL_DECRYPTION") {
            isDecryptionCancelled = true
            val notificationManager = NotificationManagerCompat.from(this)
            notificationManager.cancel(NOTIFICATION_ID)
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }

        val selectedFiles = intent?.getSerializableExtra("selectedFiles") as? List<File> ?: emptyList()
        val password = intent?.getStringExtra("password")

        val initialNotification = NotificationCompat.Builder(this, PermissionHelper.CHANNEL_ID)
            .setContentTitle("Initializing decryption")
            .setContentText("Preparing files...")
            .setSmallIcon(R.drawable.baseline_lock_open_24)
            .setProgress(100, 0, true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .build()

        startForeground(NOTIFICATION_ID, initialNotification)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                selectedFiles.forEach { file ->
                    if (isDecryptionCancelled) return@launch

                    if (file.isDirectory) {
                        decryptDirectory(file, password.toString())
                    } else {
                        val parentFolder = File(file.parent ?: "")
                        decryptFile(file, password.toString(),parentFolder, NOTIFICATION_ID)
                    }
                }
                if (!isDecryptionCancelled) showCompletionNotification()

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
    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
    }

//    private fun decryptFile(file: File, password: String, destinationFolder: File, notificationId: Int) {
//        val fileSize = file.length()
//        var bytesCopied = 0L
//        var lastUpdateTime = System.currentTimeMillis()
//
//        FileInputStream(file).use { fileIn ->
//            val salt = ByteArray(16)
//            if (fileIn.read(salt) != 16) {
//                throw IllegalArgumentException("Invalid file format: Salt missing")
//            }
//
//            val iv = ByteArray(16)
//            if (fileIn.read(iv) != 16) {
//                throw IllegalArgumentException("Invalid file format: IV missing")
//            }
//
//            val secretKey = generateKey(password, salt)
//            val ivSpec = IvParameterSpec(iv)
//
//            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
//            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
//
//            val encryptedFileName = resolveUniqueFileName(destinationFolder, file.nameWithoutExtension)
//            val decryptedFile = File(destinationFolder, encryptedFileName)
//
//            FileOutputStream(decryptedFile).use { output ->
//
//                if (isDecryptionCancelled){
//                    decryptedFile.delete()
//                    return
//                }
//
//                val inputBuffer = ByteArray(65536)
//                val outputBuffer = ByteArray(65536)
//
//                while (true) {
//
//                    if (isDecryptionCancelled){
//                        decryptedFile.delete()
//                        return
//                    }
//
//                    val bytesRead = fileIn.read(inputBuffer)
//                    if (bytesRead == -1) break
//
//                    val decryptedBytes = cipher.update(inputBuffer, 0, bytesRead, outputBuffer)
//                    output.write(outputBuffer, 0, decryptedBytes)
//
//                    bytesCopied += bytesRead
//                    val progress = ((bytesCopied * 100) / fileSize).toInt()
//                    val currentTime = System.currentTimeMillis()
//
//                    if (currentTime - lastUpdateTime > 100 || progress % 5 == 0) {
//                        updateProgressNotification(notificationId, file.name, progress)
//                        lastUpdateTime = currentTime
//                    }
//                }
//
//                val finalBytes = cipher.doFinal()
//                if (finalBytes.isNotEmpty()) {
//                    output.write(finalBytes)
//                }
//            }
//
//            if (decryptedFile.exists() && decryptedFile.length() > 0) {
//                Log.d("AESDecryptionService", "Decryption successful: ${decryptedFile.absolutePath}")
//            } else {
//                Log.e("AESDecryptionService", "Decryption failed: No output file generated")
//            }
//        }
//    }

    private fun decryptFile(file: File, password: String, destinationFolder: File, notificationId : Int): Boolean {
        var decryptedFile: File? = null
        return try {
            val fileSize = file.length()
            var bytesCopied = 0L
            var lastUpdateTime = System.currentTimeMillis()

            FileInputStream(file).use { fileIn ->
                val salt = ByteArray(16)
                if (fileIn.read(salt) != 16) throw IllegalArgumentException("Salt missing")

                val iv = ByteArray(16)
                if (fileIn.read(iv) != 16) throw IllegalArgumentException("IV missing")

                val secretKey = generateKey(password, salt)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                    init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
                }

                val uniqueName = resolveUniqueFileName(destinationFolder, file.nameWithoutExtension)
                decryptedFile = File(destinationFolder, uniqueName)

                FileOutputStream(decryptedFile!!).use { output ->
                    val inputBuffer = ByteArray(65536)
                    val outputBuffer = ByteArray(65536)

                    while (true) {
                        if (isDecryptionCancelled) throw CancellationException()

                        val bytesRead = fileIn.read(inputBuffer)
                        if (bytesRead == -1) break

                        val decryptedLen = cipher.update(inputBuffer, 0, bytesRead, outputBuffer)
                        output.write(outputBuffer, 0, decryptedLen)

                        bytesCopied += bytesRead
                        val progress = ((bytesCopied * 100) / fileSize).toInt()
                        val currentTime = System.currentTimeMillis()

                        if (currentTime - lastUpdateTime > 100 || progress % 5 == 0) {
                        updateProgressNotification(notificationId, file.name, progress)
                       lastUpdateTime = currentTime
                   }
                    }

                    val finalBytes = cipher.doFinal()
                    if (finalBytes.isNotEmpty()) {
                        output.write(finalBytes)
                    }
                }
            }

            Log.d("AESDecryptionService", "Decrypted: ${'$'}{decryptedFile!!.absolutePath}")
            true
        } catch (e: AEADBadTagException) {
            decryptedFile?.delete()
            notifyError("Incorrect password for file: ${file.name}")
            false
        } catch (e: Exception) {
            decryptedFile?.delete()
            notifyError("Failed to decrypt ${'$'}{file.name}: ${'$'}{e.message}")
            false
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
        val encryptedFolderName = resolveUniqueFileName(File(directory.parent!!), directory.nameWithoutExtension)

        val decryptedFolder = File(directory.parent, encryptedFolderName)
        if (!decryptedFolder.exists()) decryptedFolder.mkdirs()

        directory.walkTopDown().forEach { file ->

            if (isDecryptionCancelled){
                decryptedFolder.deleteRecursively()
                return
            }

            if (file.isFile && file.name != LOCK_FILE_NAME) {
                decryptFile(file, password, decryptedFolder, NOTIFICATION_ID)
            }
        }

        Log.d("AESDecryptionService", "Decrypted folder: ${decryptedFolder.absolutePath}")
    }


    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun resolveUniqueFileName(parentFolder: File, fileName: String): String {
        val baseName = fileName.substringBeforeLast(".")
        val extension = fileName.substringAfterLast(".", "")
        var uniqueName = fileName

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

    @SuppressLint("MissingPermission")
    private fun updateProgressNotification(notificationId: Int, fileName: String, progress: Int) {

        if (isDecryptionCancelled) {
            val notificationManager = NotificationManagerCompat.from(this)
            notificationManager.cancel(notificationId)
            return
        }

        val cancelDecryptionIntent = Intent(applicationContext, AESDecryptionService::class.java).apply {
            action = "ACTION_CANCEL_DECRYPTION"
        }

        val cancelPendingIntent = PendingIntent.getService(
            applicationContext, 0, cancelDecryptionIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notificationManager = NotificationManagerCompat.from(this)

        val notification = NotificationCompat.Builder(this, PermissionHelper.CHANNEL_ID)
            .setContentTitle("Decrypting: $fileName")
            .setContentText("Progress: $progress%")
            .setSmallIcon(R.drawable.baseline_lock_open_24)
            .setProgress(100, progress, false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.baseline_clear_24,"Cancel",cancelPendingIntent)
            .setOnlyAlertOnce(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    @SuppressLint("MissingPermission")
    private fun notifyError(message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(this@AESDecryptionService, message, Toast.LENGTH_LONG).show()
        }
        NotificationCompat.Builder(this, PermissionHelper.CHANNEL_ID)
            .setContentTitle("Decryption Error")
            .setContentText(message)
            .setSmallIcon(R.drawable.baseline_clear_24)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build().also {
                NotificationManagerCompat.from(this).notify(NOTIFICATION_ID + 1, it)
            }
    }

    @SuppressLint("MissingPermission")
    private fun showCompletionNotification() {
        val notificationManager = NotificationManagerCompat.from(this)

        notificationManager.cancel(AESEncryptionServices.NOTIFICATION_ID)

        val notification = NotificationCompat.Builder(this, PermissionHelper.CHANNEL_ID)
            .setContentTitle("Decryption Complete")
            .setContentText("All files decrypted successfully.")
            .setSmallIcon(R.drawable.baseline_done_all_24)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(AESEncryptionServices.NOTIFICATION_ID, notification)

        val intent = Intent("shivam.sycodes.filefusion.DECRYPTION_COMPLETE")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

        CoroutineScope(Dispatchers.Main).launch {
            kotlinx.coroutines.delay(3000)
            notificationManager.cancel(AESEncryptionServices.NOTIFICATION_ID)
        }
    }
}
