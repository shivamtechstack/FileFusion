package shivam.sycodes.filefusion.archievingAndEncryption

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import shivam.sycodes.filefusion.R
import java.io.File

class EncryptionDialog {
    @SuppressLint("SuspiciousIndentation")
    fun encryptionDialog(context: Context, selectedFiles: List<File>, requestNotification: () -> Unit) {

        val dialog = AlertDialog.Builder(context)
        val dialogView = LayoutInflater.from(context).inflate(R.layout.encryptiodialog, null)
        dialog.setView(dialogView)

        val dialogBuilder = dialog.create()

        val fileNames = dialogView.findViewById<TextView>(R.id.encrypt_selectedFiles_name)
        val password = dialogView.findViewById<EditText>(R.id.EncryptionPasswordEditText)
        val confirmPassword = dialogView.findViewById<EditText>(R.id.EncryptionConfirmPasswordEditText)
        val typeSpinner = dialogView.findViewById<Spinner>(R.id.typeSpinnerEncryption)
        val encryptButton = dialogView.findViewById<Button>(R.id.encrypt_button)
        val cancelButton = dialogView.findViewById<Button>(R.id.encrypt_cancel_button)
        val errorMessage = dialogView.findViewById<TextView>(R.id.error_message_encryptdialog)

        fileNames.text = selectedFiles.joinToString(", ") { it.name }

        cancelButton.setOnClickListener {
            dialogBuilder.dismiss()
        }

        val encryptionTypes = arrayOf("AES(Advanced Encryption Standard)")

        val typeAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, encryptionTypes)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = typeAdapter
        typeSpinner.setSelection(0)


        encryptButton.setOnClickListener {
            val selectedType = typeSpinner.selectedItem.toString()
            val pass = password.text.toString().trim()
            val confirmPass = confirmPassword.text.toString().trim()

                    if(pass.isEmpty() || confirmPass.isEmpty()){
                        errorMessage.text = "Password cannot be empty"
                    }else{
                        if(pass != confirmPass){
                            errorMessage.text = "Passwords do not match"
                        }else{
                            errorMessage.text = ""
                            val filePassword = password.text.toString()
                            when(selectedType){
                                "AES(Advanced Encryption Standard)" -> {
                                    requestNotification()
                                    val intent = Intent(context,AESEncryptionServices::class.java).apply {
                                        putExtra("selectedFiles",ArrayList(selectedFiles))
                                        putExtra("password",filePassword)
                                    }
                                    ContextCompat.startForegroundService(context,intent)
                                    dialogBuilder.dismiss()
                                }
                            }
                        }
                    }
            }
        dialogBuilder.show()
    }
}