package shivam.sycodes.filefusion.utility

import android.content.Context
import android.os.Environment
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date

class FileOperationHelper(private val context: Context) {

    private fun copyFileorDirectory(source: File, destination: File){
        if (source.isDirectory) {
            destination.mkdirs()
            source.listFiles()?.forEach { child ->
                copyFileorDirectory(child, File(destination, child.name))
            }
        }else{
                source.copyTo(destination, overwrite = false)
            }
    }

    private fun moveFileorDirectory(source: File, destination: File){
        if (source.isDirectory){
            destination.mkdirs()
            source.listFiles()?.forEach { child ->
                moveFileorDirectory(child,File(destination, child.name))
            }
            source.deleteRecursively()
        }else {
            source.renameTo(destination)
        }
    }

    fun pasteFiles(filesToPaste : List<File>?,
                   destinationPath: String?,
                   isCutOperation: Boolean,
                   onComplete: () -> Unit){

        if ( filesToPaste == null || destinationPath == null){
            showToast("No files or destination path available")
            return
        }
        val destinationDirectory = File(destinationPath)

        filesToPaste.forEach { file ->
            val targetFile = File(destinationDirectory, file.name)
            if (targetFile.exists()){
                showToast("File or folder with name ${file.name} already exists in the destination")
            }else{
                if (isCutOperation){
                    moveFileorDirectory(file, targetFile)
                }else{
                    copyFileorDirectory(file, targetFile)
                }
            }
        }
        if (isCutOperation){
            filesToPaste.forEach { file -> file.deleteRecursively() }
        }
        onComplete()
        showToast("Paste operation complete")
    }
    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun deleteDirectory(directory: File): Boolean {
        return try{
            if (directory.isDirectory) directory.listFiles()?.forEach { file ->
                if (file.isDirectory){
                    deleteDirectory(file)
                }else{
                    file.delete()
                }
            }
            directory.delete()
        }catch (e : Exception){
            e.printStackTrace()
            false
        }
    }

    fun deleteOperation(selectedFiles: List<File>){

        selectedFiles.forEach { file: File ->
            if (file.isDirectory) {
                if (deleteDirectory(file)) {
                    showToast("Directory Deleted Successfully")
                } else {
                    showToast("Failed to delete directory")
                }
            }else{
                    if (file.delete()) {
                        showToast("File deleted successfully")
                    } else {
                        showToast("Failed to delete file")
                    }
            }
        }
    }
    fun moveToTrash(selectedFiles: List<File>): Boolean {
        val trashDir = getTrashDir()
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

}