package shivam.sycodes.filefusion.popupmenus

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import shivam.sycodes.filefusion.R
import shivam.sycodes.filefusion.archievingAndEncryption.ZipArchieve
import shivam.sycodes.filefusion.roomdatabase.AppDatabase
import shivam.sycodes.filefusion.roomdatabase.BookmarkEntity
import shivam.sycodes.filefusion.service.VaultService
import java.io.File
import java.util.Date


class BottomPopUpMenu(private val context: Context) {

    private lateinit var zipArchieve : ZipArchieve
    fun popUpMenuBottom(
        selectedFiles: List<File>, view: View,
        hideNavigationBar: () -> Unit,
        isFromCategory: Boolean,
        category: String?,
    ){
        val bottomPopUpMenu = PopupMenu(context,view)
        zipArchieve =ZipArchieve()
        bottomPopUpMenu.menuInflater.inflate(R.menu.bottompopupmenu, bottomPopUpMenu.menu)
        bottomPopUpMenu.setForceShowIcon(true)

        for (i in 0 until bottomPopUpMenu.menu.size()){
            val menuItem = bottomPopUpMenu.menu.getItem(i)
            val drawable = menuItem.icon
            drawable?.setTint(ContextCompat.getColor(context, R.color.iconcolor))
        }
        if (isFromCategory){
            if (category == "bookmarks"){
                bottomPopUpMenu.menu.findItem(R.id.bookmark).isVisible = false
                bottomPopUpMenu.menu.findItem(R.id.removeBookmark).isVisible = true
            }
            bottomPopUpMenu.menu.findItem(R.id.archive).isVisible =false
            bottomPopUpMenu.menu.findItem(R.id.unarchive).isVisible =false
            bottomPopUpMenu.menu.findItem(R.id.decrypt).isVisible =false
            bottomPopUpMenu.menu.findItem(R.id.encrypt).isVisible =false
        }
        if (selectedFiles.any { it.isDirectory }){
            bottomPopUpMenu.menu.findItem(R.id.bookmark).isVisible = false
            bottomPopUpMenu.menu.findItem(R.id.removeBookmark).isVisible = false
            bottomPopUpMenu.menu.findItem(R.id.movetovault).isVisible = false
        }

        bottomPopUpMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.archive -> {

                    val archiveDialog = AlertDialog.Builder(context)
                    val archiveDialogView = LayoutInflater.from(context).inflate(R.layout.archieve_dialog,null,false)
                    archiveDialog.setView(archiveDialogView)

                    val dialog = archiveDialog.create()
                    val archiveFileName = archiveDialogView.findViewById<EditText>(R.id.archivedFileName)
                    val typeSpinner = archiveDialogView.findViewById<Spinner>(R.id.typeSpinnerArchive)
                    val compressionSpinner = archiveDialogView.findViewById<Spinner>(R.id.spinnerCompressionArchieve)
                    val protectWithPasswordCheckBox = archiveDialogView.findViewById<CheckBox>(R.id.protectWithPasswordCheckBox)
                    val cancelButton = archiveDialogView.findViewById<Button>(R.id.archive_cancel_button)
                    val createButton = archiveDialogView.findViewById<Button>(R.id.create_archieve_button)
                    val passwordEditText = archiveDialogView.findViewById<EditText>(R.id.archivePasswordEditText)
                    val confirmPasswordEditText = archiveDialogView.findViewById<EditText>(R.id.archiveConfirmPasswordEditText)
                    val archievePasswordlayout = archiveDialogView.findViewById<View>(R.id.archivePasswordLayout)

                    cancelButton.setOnClickListener {
                        dialog.dismiss()
                    }

                    archiveFileName.text = Editable.Factory.getInstance().newEditable(selectedFiles.first().nameWithoutExtension)

                    val type = arrayOf("Zip","7z")
                    val zipCompressionLevels = arrayOf("No Compression", "Very Fast", "Fast","Balanced", "High", "Maximum Compression")
                    val sevenZCompressionLevels = arrayOf("Fastest", "Fast", "Normal", "Maximum")

                    val typeAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, type)
                    typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    typeSpinner.adapter = typeAdapter
                    typeSpinner.setSelection(0)

                    val compressionAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, zipCompressionLevels)
                    compressionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    compressionSpinner.adapter = compressionAdapter

                    typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            when (type[position]) {
                                "Zip" -> {

                                    val zipAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, zipCompressionLevels)
                                    zipAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                    compressionSpinner.adapter = zipAdapter
                                }
                                "7z" -> {

                                    val sevenZAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, sevenZCompressionLevels)
                                    sevenZAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                    compressionSpinner.adapter = sevenZAdapter
                                }
                            }
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {
                            compressionSpinner.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, zipCompressionLevels)
                        }
                    }
                    protectWithPasswordCheckBox.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked){
                            archievePasswordlayout.visibility = View.VISIBLE
                        }
                    }

                    createButton.setOnClickListener {
                        val selectedType = typeSpinner.selectedItem.toString()
                        val selectedCompression = compressionSpinner.selectedItem.toString()

                        when(selectedType){
                            "Zip" -> {
                                if (selectedFiles.isNotEmpty()) {
                                    val parentDir = selectedFiles.first().parentFile
                                    if (parentDir != null) {
                                        val outputZipFileName = archiveFileName.text.toString() + ".zip"
                                        val outputZipFilePath = File(parentDir, outputZipFileName).absolutePath

                                        if (protectWithPasswordCheckBox.isChecked){
                                            val password = passwordEditText.text.toString()
                                            val confirmPassword = confirmPasswordEditText.text.toString()
                                            if(password.isEmpty() || confirmPassword.isEmpty()){
                                                Toast.makeText(context, "Password cannot be empty", Toast.LENGTH_SHORT).show()
                                                return@setOnClickListener
                                            }
                                            if (password != confirmPassword){
                                                Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                                                return@setOnClickListener
                                            }
                                            else{
                                                zipArchieve.zipFileorFolder(selectedFiles, outputZipFilePath,selectedCompression,password)
                                            }
                                        }else {

                                            zipArchieve.zipFileorFolder(
                                                selectedFiles,
                                                outputZipFilePath,
                                                selectedCompression
                                            )
                                        }
                                        Toast.makeText(context, "Files zipped to: $outputZipFilePath", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Cannot determine parent directory", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }

                    dialog.show()
                    true
                }
                R.id.unarchive ->{
                    if (selectedFiles.isNotEmpty()) {
                        val parentDir = selectedFiles.first().parentFile
                        if (parentDir != null) {
                            val outputZipFileName = "archived_${System.currentTimeMillis()}.zip"
                            val outputZipFilePath = File(parentDir, outputZipFileName).absolutePath
                            determineFileType(selectedFiles, outputZipFilePath)
                        }
                    }else{
                        Toast.makeText(context,"No File/Folder Selected",Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                R.id.copypath -> {
                    if (selectedFiles.isNotEmpty()) {
                        copyPathsToClipboard(selectedFiles)
                    }
                    true
                }
                R.id.createshortcut -> {
                    if (selectedFiles.isNotEmpty()) {
                        createShortcuts(selectedFiles)
                    }
                    true
                }
                R.id.properties -> {
                    if (selectedFiles.isNotEmpty()) {
                        showFileProperties(selectedFiles)
                    }
                    true
                }
                R.id.bookmark ->{
                    val bookmarkDao = AppDatabase.getDatabase(context).itemDAO()

                    CoroutineScope(Dispatchers.IO).launch{
                        selectedFiles.forEach { file ->
                            val bookmarkItem = BookmarkEntity(
                                bookmarkFileName = file.name.toString(),
                                bookmarkFilePath = file.absolutePath
                            )
                            try {
                                bookmarkDao.addBookmark(bookmarkItem)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "${file.name} bookmarked successfully", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Failed to bookmark ${file.name}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    true
                }
                R.id.movetovault -> {
                   if (selectedFiles.isNotEmpty()){
                       val intent = Intent(context, VaultService::class.java).apply {
                           putExtra("selectedFiles",ArrayList(selectedFiles))
                           action = "MOVE_TO_VAULT"
                       }
                       ContextCompat.startForegroundService(context,intent)
                   }else{
                       Toast.makeText(context, "No files selected to move to vault", Toast.LENGTH_SHORT).show()
                   }
                   true
                }
                R.id.removeBookmark -> {

                    val bookmarkDao = AppDatabase.getDatabase(context).itemDAO()
                    CoroutineScope(Dispatchers.IO).launch {
                        selectedFiles.forEach { file ->
                            bookmarkDao.removeBookmark(file.absolutePath)
                        }
                        try {
                            withContext(Dispatchers.Main){
                                Toast.makeText(context, "Bookmarks removed successfully", Toast.LENGTH_SHORT).show()
                            }
                        }catch (e: Exception){
                            withContext(Dispatchers.Main){
                                Toast.makeText(context, "Failed to remove bookmarks", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    true
                }
                else -> {
                    Toast.makeText(context, "No selection", Toast.LENGTH_SHORT).show()
                    true
                }
            }
        }
        bottomPopUpMenu.show()
    }

    private fun determineFileType(selectedFiles: List<File>, outputZipFilePath: String) {
        selectedFiles.forEach { file ->
            val extension = file.extension.lowercase()
            when{
                extension == "zip" -> zipArchieve.unZipFile(file,outputZipFilePath)

                else -> Toast.makeText(context,"Unsupported Archieve Format!!",Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun copyPathsToClipboard(files: List<File>) {
        val paths = files.joinToString("\n") { it.absolutePath }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("File Paths", paths)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Path copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun createShortcuts(files: List<File>) {
        val shortcutManager = context.getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager

        if (shortcutManager.isRequestPinShortcutSupported) {
            files.forEach { file ->
                val shortcutIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                }

                val shortcutInfo = ShortcutInfo.Builder(context, file.name)
                    .setShortLabel(file.name)
                    .setLongLabel("Open ${file.name}")
                    .setIcon(Icon.createWithResource(context, R.drawable.folder))
                    .setIntent(shortcutIntent)
                    .build()

                shortcutManager.requestPinShortcut(shortcutInfo, null)
            }
        } else {
            Toast.makeText(context, "Shortcut creation is not supported", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFileProperties(files: List<File>) {
        val properties = StringBuilder()
        files.forEach { file ->
            properties.append("Name: ${file.name}\n")
            properties.append("Path: ${file.absolutePath}\n")
            properties.append("Size: ${if (file.isDirectory) "Directory" else "${file.length()} bytes"}\n")
            properties.append("Last Modified: ${Date(file.lastModified())}\n")
            properties.append("\n")
        }
        AlertDialog.Builder(context)
            .setTitle("File Properties")
            .setMessage(properties.toString())
            .setPositiveButton("OK", null)
            .show()
    }

}