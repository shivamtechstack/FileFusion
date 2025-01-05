package shivam.sycodes.filefusion.utility

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import shivam.sycodes.filefusion.R
import shivam.sycodes.filefusion.roomdatabase.AppDatabase
import shivam.sycodes.filefusion.roomdatabase.ItemEntity
import shivam.sycodes.filefusion.service.DeleteOperationCallback
import shivam.sycodes.filefusion.service.DeleteWorker
import shivam.sycodes.filefusion.service.PasteService
import shivam.sycodes.filefusion.viewModel.FileOperationViewModel
import java.io.File
import java.util.Date

class FileOperationHelper(private val context: Context) {

    private val fileOperationViewModel = FileOperationViewModel()

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    fun deleteOperation(selectedFiles: List<File>,callback: DeleteOperationCallback){
        val filePaths = selectedFiles.map { it.absolutePath }.toTypedArray()

        val data = Data.Builder()
            .putStringArray(DeleteWorker.KEY_FILES_TO_DELETE, filePaths)
            .build()

        val deleteWorkRequest = OneTimeWorkRequestBuilder<DeleteWorker>()
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(deleteWorkRequest)

        WorkManager.getInstance(context).getWorkInfoByIdLiveData(deleteWorkRequest.id)
            .observeForever { workInfo ->
                if (workInfo != null && workInfo.state.isFinished) {
                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        callback.onSuccess(filePaths.toList())
                    } else {
                        callback.onFailure("Failed to delete files.")
                    }
                }
            }
    }

    fun moveToTrash(selectedFiles: List<File>): Boolean {
        val trashDir = getTrashDir()
        val trashDao = AppDatabase.getDatabase(context).itemDAO()
        if (trashDir == null || !trashDir.exists()) {
            showToast("Trash Directory Error!")
            return false
        }

        var allMovedSuccessfully = true

        selectedFiles.forEach { file ->
            val trashFile = File(trashDir, file.name)
            val isMoved = if (file.isDirectory) {
                moveFileOrDirectoryToTrash(file, trashFile)
            } else {
                file.renameTo(trashFile)
            }
            if (isMoved){
                CoroutineScope(Dispatchers.IO).launch{
                    val trashItem = ItemEntity(
                        trashFileName = file.name,
                        trashFileOriginalPath = file.absolutePath,
                        trashFileDeletionDate = Date().toString()
                    )
                    trashDao.insertTrashItem(trashItem)
                }
            }
            if (!isMoved) {
                allMovedSuccessfully = false
            }
        }
        return allMovedSuccessfully
    }

    private fun moveFileOrDirectoryToTrash(source: File, trashDir: File): Boolean {
        if (source.isDirectory) {
            trashDir.mkdirs()

            val allMovedSuccessfully = source.listFiles()?.all { child ->
                moveFileOrDirectoryToTrash(child, File(trashDir, child.name))
            } ?: true

            return if (allMovedSuccessfully) {
                source.deleteRecursively()
            } else {
                false
            }
        } else {
            return source.renameTo(trashDir)
        }
    }

    fun getTrashDir(): File? {
        val storageDir = Environment.getExternalStorageDirectory()
        val trashDir = File(storageDir, ".FileFusionTrashBin")

        if (!trashDir.exists()) {
            val isCreated = trashDir.mkdirs()
            if (!isCreated) {
                showToast("Failed to create Trash Directory!")
                return null
            }
        }
        return trashDir
    }

    @SuppressLint("SuspiciousIndentation")
    fun isRestored(file : File){
    val trashDAO = AppDatabase.getDatabase(context).itemDAO()
        CoroutineScope(Dispatchers.IO).launch {
            val trashItem = trashDAO.getTrashItemByFileName(file.name)
            if (trashItem!=null) {
                val originalFilePath = File(trashItem.trashFileOriginalPath)
                val isrestored = if (file.isDirectory) {
                    moveFileOrDirectoryToTrash(file, originalFilePath)
                } else {
                    file.renameTo(originalFilePath)
                }
                withContext(Dispatchers.Main) {
                    if (isrestored) {
                        trashDAO.deleteTrashItem(file.name)
                        showToast("File restored to original path.")
                    } else {
                        showToast("Failed to restore file.")
                    }
                } }else {
                    showToast("Original path not found!")
                }
        }
    }

    fun pasteOperation(currentPath: String?, filesToPaste: List<File>?, isCutOperation: Boolean) {

        if (filesToPaste.isNullOrEmpty()) {
            Log.e("PasteOperation", "No files to paste")
            return
        }

        currentPath?.let { destinationPath ->
            filesToPaste.forEach { file ->
                val destinationFile = File(destinationPath, file.name)

                if (destinationPath == file.absolutePath){
                    Toast.makeText(context, "Cannot paste on same location", Toast.LENGTH_SHORT).show()
                }

                if (destinationFile.exists()) {
                    Log.d("PasteOperation", "Conflict detected for file: ${file.name}")

                    val alertDialog = AlertDialog.Builder(context)
                    val dialogView = LayoutInflater.from(context).inflate(R.layout.fileconflictdialog, null)
                    alertDialog.setView(dialogView)

                    val dialog = alertDialog.create()
                    dialog.show()

                    val cancelButton = dialogView.findViewById<Button>(R.id.fileconflict_cancel_button)
                    val keepBoth = dialogView.findViewById<Button>(R.id.keepboth_button)
                    val keepNew = dialogView.findViewById<LinearLayout>(R.id.keep_new_file)
                    val keepOld = dialogView.findViewById<LinearLayout>(R.id.keep_old_file)
                    val sourceFileImage = dialogView.findViewById<ImageView>(R.id.source_file_preview)
                    val destinationFileImage = dialogView.findViewById<ImageView>(R.id.destination_file_preview)

                    val sourceFileName = dialogView.findViewById<TextView>(R.id.source_file_name)
                    val destinationFileName = dialogView.findViewById<TextView>(R.id.destination_file_name)

                    val sourceFileSize = dialogView.findViewById<TextView>(R.id.source_file_size)
                    val destinationFileSize = dialogView.findViewById<TextView>(R.id.destination_file_size)

                    sourceFileName.text = file.name
                    destinationFileName.text = destinationFile.name

                    sourceFileSize.text = convertFileSize(file.length())
                    destinationFileSize.text = convertFileSize(destinationFile.length())

                    keepBoth.setOnClickListener {
                        val newFileName = generateNewFileName(destinationPath, file.name)
                        val newFile = File(destinationPath, newFileName)
                        startPasteService(file, newFile.absolutePath, isCutOperation)
                        dialog.dismiss()
                    }

                    cancelButton.setOnClickListener {
                        dialog.dismiss()
                    }
                    keepNew.setOnClickListener {
                        startPasteService(file, destinationPath, isCutOperation)
                        destinationFile.delete()
                        dialog.dismiss()
                    }
                    keepOld.setOnClickListener {
                        fileOperationViewModel.filesToCopyorCut = null
                        dialog.dismiss()
                    }

                } else {
                    startPasteService(file, destinationPath, isCutOperation)
                }
            }
            fileOperationViewModel.filesToCopyorCut = null
        }
    }

    private fun generateNewFolderName(destinationPath: String, originalName: String): String {
        val baseName = originalName
        var newName = baseName
        var counter = 1

        val destinationDirectory = File(destinationPath)

        if (!destinationDirectory.exists()) {
            destinationDirectory.mkdirs()
        }

        while (File(destinationDirectory, newName).exists()) {
            newName = "$baseName($counter)"
            counter++
        }
        return newName
    }

    private fun generateNewFileName(destinationPath: String, originalName: String): String {
        val baseName = originalName.substringBeforeLast(".")
        val extension = originalName.substringAfterLast(".", "")
        var newName: String
        var counter = 1

        do {
            newName = if (extension.isNotEmpty()) {
                "$baseName($counter).$extension"
            } else {
                "$baseName($counter)"
            }
            counter++
        } while (File(destinationPath, newName).exists())

        return newName
    }


    private fun startPasteService(file: File, destinationPath: String, isCutOperation: Boolean) {
        val intent = Intent(context, PasteService::class.java).apply {
            putExtra("FILES_TO_PASTE", arrayOf(file.absolutePath))
            putExtra("DESTINATION_PATH", destinationPath)
            putExtra("IS_CUT_OPERATION", isCutOperation)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    private fun convertFileSize(sizeInBytes: Long): String {
        val kb = 1024
        val mb = kb * 1024
        val gb = mb * 1024

        return when {
            sizeInBytes >= gb -> String.format("%.2f GB", sizeInBytes / gb.toDouble())
            sizeInBytes >= mb -> String.format("%.2f MB", sizeInBytes / mb.toDouble())
            sizeInBytes >= kb -> String.format("%.2f KB", sizeInBytes / kb.toDouble())
            else -> "$sizeInBytes B"
        }
    }


}