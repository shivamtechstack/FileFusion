package shivam.sycodes.filefusion.utility

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

class PermissionHelper(private val context: Context) {

    fun isNotificationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestNotificationPermission(launcher: ActivityResultLauncher<String>) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}