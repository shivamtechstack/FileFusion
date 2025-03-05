package shivam.sycodes.filefusion.utility

import android.content.Context
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

class AboutUs(private val context: Context) {

    fun showAboutUsDialog() {
        val aboutText = """
            Welcome to FileFusion, a powerful and secure file management and encryption app designed to keep your data safe and organized.

            About the Developer:
            Hey! I'm Shivam, the creator of FileFusion. My goal is to build innovative and efficient applications that enhance digital security and file management.

            Get in Touch:
            📧 Developer Contact: devshivamyadav1604@gmail.com
            📧 Personal Contact: shivam16yadav16@gmail.com
            
            🔗 GitHub Repository: https://github.com/shivamtechstack/FileFusion
            🔗 LinkedIn: http://www.linkedin.com/in/shivamyadav1604
        """.trimIndent()

        val textView = TextView(context)
        textView.text = aboutText
        textView.setPadding(40, 20, 40, 20)
        textView.autoLinkMask = Linkify.EMAIL_ADDRESSES or Linkify.WEB_URLS
        textView.movementMethod = LinkMovementMethod.getInstance()

        val builder = AlertDialog.Builder(context)
        builder.setTitle("About")
            .setView(textView)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}