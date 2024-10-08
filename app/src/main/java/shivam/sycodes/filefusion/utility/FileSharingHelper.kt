package shivam.sycodes.filefusion.utility

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale

class FileSharingHelper(private val context: Context) {

    fun shareFiles(files: List<File>) {
        if (files.isNotEmpty()) {
            if (files.size == 1) {
                val selectedFile = files[0]
                if (selectedFile.isDirectory) {
                    shareFolder(selectedFile)
                } else {
                    shareSingleFile(selectedFile)
                }
            } else {
                val areAllDirectories = files.all { it.isDirectory }
                if (areAllDirectories) {
                    shareMultipleFolders(files)
                } else {
                    shareMultipleFiles(files)
                }
            }
        } else {
            Toast.makeText(context, "No files or folders selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareSingleFile(file: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = getMimeType(file) ?: "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share File"))
    }

    private fun shareMultipleFiles(files: List<File>) {
        val fileUris = ArrayList<Uri>()
        files.forEach { file ->
            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            fileUris.add(uri)
        }

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Files"))
    }

    private fun shareFolder(folder: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", folder)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Folder"))
    }

    private fun shareMultipleFolders(folders: List<File>) {
        val folderUris = ArrayList<Uri>()
        folders.forEach { folder ->
            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", folder)
            folderUris.add(uri)
        }

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, folderUris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Folders"))
    }

    private fun getMimeType(file: File): String? {
        val extension = MimeTypeMap.getFileExtensionFromUrl(file.name)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase(Locale.ROOT))
    }

}