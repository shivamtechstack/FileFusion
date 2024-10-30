package shivam.sycodes.filefusion.splashscreen

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import shivam.sycodes.filefusion.MainActivity
import shivam.sycodes.filefusion.R
@SuppressLint("CustomSplashScreen")
class SplashScreen : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (!isAllFileAccessGranted()) {
            showDialogBox()
        } else {
            navigateToMainActivity()
        }
    }

    private fun showDialogBox() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Storage Access Required")
            .setMessage(
                "This app requires access to all files on your device to provide core functionality, " +
                        "such as viewing, organizing, and managing files across different folders. Without this permission, " +
                        "the app won't be able to show or modify files stored on your device." +
                        "\n\n Canceling this dialog will close the app."
            )
            .setPositiveButton("Grant") { _, _ ->
                try {
                    requestAllFilesAccess()
                } catch (e: Exception) {
                    Toast.makeText(this, "Unable to open settings. Please try again.", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
                setTextColor(resources.getColor(R.color.lightred, theme))
                textSize = 16f
                isAllCaps = false
            }
        }
        dialog.show()
    }

    private fun isAllFileAccessGranted(): Boolean {
        return Environment.isExternalStorageManager()
    }

    @Suppress("DEPRECATION")
    private fun requestAllFilesAccess() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_CODE_ALL_FILES_ACCESS)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            startActivityForResult(intent, REQUEST_CODE_ALL_FILES_ACCESS)
        }
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ALL_FILES_ACCESS) {
            if (isAllFileAccessGranted()) {
                navigateToMainActivity()
            } else {
                Toast.makeText(this, "Permission is required to access all files.", Toast.LENGTH_SHORT).show()
                showDialogBox()
            }
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    companion object {
        private const val REQUEST_CODE_ALL_FILES_ACCESS = 1001
    }
}
