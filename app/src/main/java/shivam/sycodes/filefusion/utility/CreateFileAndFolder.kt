package shivam.sycodes.filefusion.utility

import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import shivam.sycodes.filefusion.R
import java.io.File
import kotlin.reflect.KFunction1

class CreateFileAndFolder(private val context : Context) {

    fun createNewFile(
        currentPath: String?,
        loadFiles: (String?) -> Unit
    ){
        val fileDialog = AlertDialog.Builder(context)
        val fileDialogView = LayoutInflater.from(context).inflate(R.layout.newfilefolderdialog,null,false)
        fileDialog.setView(fileDialogView)
        fileDialog.setTitle("New File")
        val fileName = fileDialogView.findViewById<EditText>(R.id.newFileFolder_edittext)

        fileDialog.setPositiveButton("Create"){ dialog, _ ->
            val filename = fileName.text.toString().trim()
            if (filename.isNotEmpty() && currentPath != null){
                val newFile = File(currentPath, filename)
                try{
                    if (!newFile.exists()) {
                        if (newFile.createNewFile()) {
                            showToast("File created successfully")
                        } else {
                            showToast("Failed to create file!")
                        }
                    }else{
                        showToast("File already exists")
                    }
                }catch (e : Exception){
                    showToast("Error : $e")
                }
            }else{
                showToast("Filename cannot be empty")
            }
            loadFiles(currentPath)
            dialog.dismiss()
        }
        fileDialog.setNegativeButton("Cancel"){dialog,_ ->
            dialog.dismiss()
        }
        fileDialog.show()
    }

    fun createNewFolder(
        currentPath: String?,
        loadFiles: (String?) -> Unit
    ){
        val folderDialog = AlertDialog.Builder(context)
        val folderDialogView = LayoutInflater.from(context).inflate(R.layout.newfilefolderdialog,null,false)
        folderDialog.setView(folderDialogView)
        folderDialog.setTitle("New Folder")
        val folderName = folderDialogView.findViewById<EditText>(R.id.newFileFolder_edittext)

        folderDialog.setPositiveButton("Create") { dialog, _ ->
            val foldername = folderName.text.toString().trim()
            if (foldername.isNotEmpty() && currentPath != null) {
                val newFolder = File(currentPath, foldername)
                if (!newFolder.exists()) {
                    if (newFolder.mkdir()) {
                        showToast("Folder created successfully")
                    } else {
                        showToast("Failed to create folder")
                    }
                } else {
                    showToast("Folder already exists")
                }
            } else {
                showToast("Folder name cannot be empty")
            }
            loadFiles(currentPath)
            dialog.dismiss()
        }
        folderDialog.setNegativeButton("cancel"){dialog,_ ->
            dialog.dismiss()
        }
        folderDialog.show()
    }

    private fun showToast(message : String){
        Toast.makeText(context,message, Toast.LENGTH_SHORT).show()
    }
}