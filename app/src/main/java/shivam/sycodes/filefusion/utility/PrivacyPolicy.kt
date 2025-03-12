package shivam.sycodes.filefusion.utility

import android.annotation.SuppressLint
import android.content.Context
import android.text.method.ScrollingMovementMethod
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

class PrivacyPolicy(private val context: Context) {

    @SuppressLint("SetTextI18n")
    fun showPolicyDialog() {
        val textView = TextView(context).apply {
            text = """
                Privacy Policy for FileFusion
                Effective Date: 12-03-2025

                Welcome to FileFusion! Your privacy is important to us, and we want to ensure you have a clear understanding of how our app handles data and the features it provides.

                1. No Data Collection or Tracking
                FileFusion is designed with privacy in mind. We do not collect, store, or share any personal data. The app does not track your activity, and it does not send any information to external servers or third parties. Everything you do within the app remains entirely on your device.

                2. Secure Vault Feature
                FileFusion includes a Secure Vault feature to help you organize and manage your files. However, please note:
                The vault is not encrypted and functions as a basic file storage solution.
                If you uninstall the app or clear its data, the vault’s contents will be permanently deleted and cannot be recovered.
                We recommend that you regularly back up important files to an external location for safekeeping.
                
                3. Experimental File Encryption Feature
                FileFusion offers an experimental file encryption feature, which is still under development. While we aim to improve it over time, there may be instances where encrypted files cannot be recovered due to unexpected behavior.

                Users should test this feature on non-critical files first before using it on important data.
                We encourage users to keep additional backups of any encrypted files elsewhere, as this feature is still in its early stages.
                
                4. Data Responsibility and Best Practices
                While FileFusion provides features to help manage and secure your files, users are responsible for:
                Backing up important data to external storage or cloud services.
                Understanding that clearing app data or uninstalling FileFusion will delete vault contents.
                Using the experimental encryption feature cautiously to avoid accidental data loss.
                
                5. Security and Future Improvements
                We are committed to enhancing FileFusion’s security and reliability. Future updates may introduce improved encryption and additional safety measures. Please ensure you keep the app updated to benefit from the latest improvements.

                6. Changes to This Privacy Policy
                This Privacy Policy may be updated as FileFusion evolves. Any updates will be reflected within the app or on our official page. Continued use of the app after updates indicates acceptance of the revised policy.
            """.trimIndent()
            setPadding(40, 20, 40, 20)
            textSize = 14f
            movementMethod = ScrollingMovementMethod()
        }

        val scrollView = ScrollView(context).apply {
            addView(textView)
        }

        AlertDialog.Builder(context)
            .setTitle("Privacy Policy")
            .setView(scrollView)
            .setPositiveButton("Ok",null)
            .setCancelable(false)
            .show()
    }
}