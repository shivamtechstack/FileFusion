package shivam.sycodes.filefusion.archievingAndEncryption

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import shivam.sycodes.filefusion.utility.PermissionHelper.Companion.CHANNEL_ID
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipArchive : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val filesToZip = intent?.getSerializableExtra("files") as? ArrayList<File> ?: arrayListOf()

        val archiveName: String = intent?.getStringExtra("archive_name") ?: "archive.zip"
        val compressionLevel: String = intent?.getStringExtra("compression_level") ?: ""
        val action: String = intent?.getStringExtra("Action") ?: ""

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Zipping Files")
            .setContentText("Compressing files into $archiveName")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)

        val compression = when(compressionLevel){
            "No Compression" -> 0
            "Very Fast" -> 1
            "Fast" -> 3
            "Balanced" -> 5
            "High" -> 7
            "Maximum Compression" -> 9
            else -> 0
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    "zip" -> {
                    }
                    "unzip" -> {
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

}
