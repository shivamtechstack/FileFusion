package shivam.sycodes.filefusion.utility

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import shivam.sycodes.filefusion.roomdatabase.AppDatabase
import shivam.sycodes.filefusion.roomdatabase.ItemEntity
import shivam.sycodes.filefusion.service.DeleteOperationCallback
import shivam.sycodes.filefusion.service.DeleteWorker
import java.io.File
import java.util.Date

class FileOperationHelper(private val context: Context) {

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
}