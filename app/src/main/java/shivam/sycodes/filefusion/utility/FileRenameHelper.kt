package shivam.sycodes.filefusion.utility

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputEditText
import shivam.sycodes.filefusion.R
import java.io.File

class FileRenameHelper(private val context : Context) {

    fun renameFiles(
        selectedFiles: List<File>,
        onFilesRenamed: () -> Unit,
        hideNavigationBars: () -> Unit
    ) {
        val numberOfFiles = selectedFiles.size

        if (numberOfFiles == 1) {
            renameSingleFile(selectedFiles[0], onFilesRenamed, hideNavigationBars)
        } else if (numberOfFiles > 1) {
            renameMultipleFiles(selectedFiles, onFilesRenamed, hideNavigationBars)
        }
    }

    private fun renameSingleFile(file: File, onFilesRenamed: () -> Unit, hideNavigationBars: () -> Unit) {
        val alertDialog = AlertDialog.Builder(context)
        val renameView = LayoutInflater.from(context).inflate(R.layout.rename_single_file, null, false)
        alertDialog.setView(renameView)

        val renameBox = renameView.findViewById<TextInputEditText>(R.id.rename_single_edittext)
        val cancelButton = renameView.findViewById<Button>(R.id.rename_single_cancel_button)
        val okButton = renameView.findViewById<Button>(R.id.rename_single_ok_button)

        renameBox.setText(file.name)

        val dialog = alertDialog.create()

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        okButton.setOnClickListener {
            val newName = renameBox.text.toString().trim()
            if (newName.isNotEmpty()) {
                val newFile = File(file.parent, newName)
                if (newFile.exists()) {
                    Toast.makeText(context, "File with name $newName already exists", Toast.LENGTH_SHORT).show()
                } else {
                    if (file.renameTo(newFile)) {
                        Toast.makeText(context, "Renamed successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Rename failed", Toast.LENGTH_SHORT).show()
                    }
                }
                onFilesRenamed()
            }
            dialog.dismiss()
            hideNavigationBars()
        }

        dialog.show()
    }

    private fun renameMultipleFiles(selectedFiles: List<File>, onFilesRenamed: () -> Unit, hideNavigationBars: () -> Unit) {
        val alertDialog = AlertDialog.Builder(context)
        val renameView = LayoutInflater.from(context).inflate(R.layout.rename_single_file, null, false)
        alertDialog.setView(renameView)

        val renameBox = renameView.findViewById<TextInputEditText>(R.id.rename_single_edittext)
        val cancelButton = renameView.findViewById<Button>(R.id.rename_single_cancel_button)
        val okButton = renameView.findViewById<Button>(R.id.rename_single_ok_button)

        val dialog = alertDialog.create()

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        okButton.setOnClickListener {
            val newBaseName = renameBox.text.toString().trim()
            if (newBaseName.isNotEmpty()) {
                selectedFiles.forEachIndexed { index, file ->
                    val extension = if (file.isFile && newBaseName.contains('.')) {
                        ""
                    } else if (file.isFile) {
                        ".${file.extension}"
                    } else {
                        ""
                    }
                    val newName = if (newBaseName.contains('.')) {
                        val baseNameWithoutExtension = newBaseName.substringBeforeLast('.')
                        val providedExtension = newBaseName.substringAfterLast('.')
                        "$baseNameWithoutExtension(${index + 1}).$providedExtension"
                    } else {
                        "$newBaseName(${index + 1})$extension"
                    }

                    val newFile = File(file.parent, newName)
                    if (newFile.exists()) {
                        Toast.makeText(context, "File with name $newName already exists", Toast.LENGTH_SHORT).show()
                    } else {
                        if (file.renameTo(newFile)) {
                            Toast.makeText(context, "${file.name} renamed to $newName", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Rename failed for ${file.name}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                onFilesRenamed()
            }
            dialog.dismiss()
            hideNavigationBars()
        }

        dialog.show()
    }

}