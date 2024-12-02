package shivam.sycodes.filefusion.archievingAndEncryption

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import shivam.sycodes.filefusion.R
import java.io.File

class DecryptionDialog {
    @SuppressLint("SetTextI18n")
    fun decryptionDialog(
        context: Context,
        selectedFiles: List<File>,
        requestNotification: () -> Unit
    ) {
        val dialog = AlertDialog.Builder(context)
        val dialogView = LayoutInflater.from(context).inflate(R.layout.decryptiondialog, null)
        dialog.setView(dialogView)

        val dialogBuilder = dialog.create()

        val fileNames = dialogView.findViewById<TextView>(R.id.decrypt_selectedFiles_name)
        val password = dialogView.findViewById<EditText>(R.id.decryptionPasswordEditText)
        val decryptButton = dialogView.findViewById<Button>(R.id.decrypt_button)
        val cancelButton = dialogView.findViewById<Button>(R.id.decrypt_cancel_button)
        val errorMessage = dialogView.findViewById<TextView>(R.id.error_message_decryptdialog)

        fileNames.text = selectedFiles.joinToString(", ") { it.name }

        cancelButton.setOnClickListener {
            dialogBuilder.dismiss()
        }

        decryptButton.setOnClickListener {
            if (password.text.toString().isEmpty()) {
                errorMessage.text = "Password cannot be empty"
            }else{
                    errorMessage.text = ""
                    val filePassword = password.text.toString()
                    requestNotification()
                    val intent = Intent(context,AESDecryptionService::class.java).apply {
                        putExtra("selectedFiles",ArrayList(selectedFiles))
                        putExtra("password",filePassword)
                    }
                    ContextCompat.startForegroundService(context,intent)
                    dialogBuilder.dismiss()
        }
    }
        dialogBuilder.show()
}
}