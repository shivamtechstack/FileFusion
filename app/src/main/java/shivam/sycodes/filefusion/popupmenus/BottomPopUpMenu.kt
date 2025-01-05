package shivam.sycodes.filefusion.popupmenus

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import shivam.sycodes.filefusion.R
import shivam.sycodes.filefusion.archievingAndEncryption.ArchiveDialog
import shivam.sycodes.filefusion.archievingAndEncryption.DecryptionDialog
import shivam.sycodes.filefusion.archievingAndEncryption.EncryptionDialog
import shivam.sycodes.filefusion.fragments.PasswordAuthentication
import shivam.sycodes.filefusion.fragments.PasswordSetupFragment
import shivam.sycodes.filefusion.roomdatabase.AppDatabase
import shivam.sycodes.filefusion.roomdatabase.BookmarkEntity
import shivam.sycodes.filefusion.service.VaultService
import shivam.sycodes.filefusion.utility.PreferencesHelper
import shivam.sycodes.filefusion.viewModel.PasswordAuthCallBack
import java.io.File
import java.util.Date

class BottomPopUpMenu(private val context: Context) {

    fun popUpMenuBottom(
        selectedFiles: List<File>, view: View,
        hideNavigationBar: () -> Unit,
        requestNotification: () -> Unit,
        isFromCategory: Boolean,
        category: String?,
    ){
        val preferencesHelper = PreferencesHelper(context)
        val bottomPopUpMenu = PopupMenu(context,view)
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
            bottomPopUpMenu.menu.findItem(R.id.encrypt).isVisible =false
        }
        if (selectedFiles.any { it.isDirectory }){
            bottomPopUpMenu.menu.findItem(R.id.bookmark).isVisible = false
            bottomPopUpMenu.menu.findItem(R.id.removeBookmark).isVisible = false
            bottomPopUpMenu.menu.findItem(R.id.movetovault).isVisible = false
        }
        if (selectedFiles.size <= 1){
            val file = selectedFiles[0]
            if (file.extension == "enc"){
                bottomPopUpMenu.menu.findItem(R.id.decrypt).isVisible = true
                bottomPopUpMenu.menu.findItem(R.id.encrypt).isVisible = false
            }
        }

        bottomPopUpMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.archive -> {

                   ArchiveDialog().zipCode(context,selectedFiles,requestNotification,hideNavigationBar)
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
                R.id.encrypt ->{
                    if (selectedFiles.isNotEmpty()){
                        EncryptionDialog().encryptionDialog(context,selectedFiles,requestNotification)
                        hideNavigationBar()
                    }else{
                        Toast.makeText(context, "No files selected to encrypt", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                R.id.decrypt -> {
                    if (selectedFiles.isNotEmpty()){
                        DecryptionDialog().decryptionDialog(context,selectedFiles,requestNotification)
                        hideNavigationBar()
                    }else{
                        Toast.makeText(context, "No files selected to decrypt", Toast.LENGTH_SHORT).show()
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
                       if(preferencesHelper.getPassword()!=null){
                           val fragment = PasswordAuthentication.newInstance("moveFile")
                           fragment.setCallback(object : PasswordAuthCallBack {
                               override fun onAuthenticationSuccess() {
                                   val intent = Intent(context, VaultService::class.java).apply {
                                       putExtra("selectedFiles",ArrayList(selectedFiles))
                                       action = "MOVE_TO_VAULT"
                                   }
                                   ContextCompat.startForegroundService(context,intent)
                               }
                           })
                           (context as AppCompatActivity).supportFragmentManager.beginTransaction()
                               .replace(R.id.fragmentContainerView, fragment)
                               .addToBackStack(null)
                               .commit()

                       }else if (preferencesHelper.getPassword() == null){

                           val fragment = PasswordSetupFragment.newInstance("moveFile")
                           fragment.setCallback(object : PasswordAuthCallBack {
                               override fun onAuthenticationSuccess() {
                                   val intent = Intent(context, VaultService::class.java).apply {
                                       putExtra("selectedFiles",ArrayList(selectedFiles))
                                       action = "MOVE_TO_VAULT"
                                   }
                                   ContextCompat.startForegroundService(context,intent)
                               }
                           })
                           (context as AppCompatActivity).supportFragmentManager.beginTransaction()
                               .replace(R.id.fragmentContainerView, fragment)
                               .addToBackStack(null)
                               .commit()
                       }else{
                           Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT).show()
                       }
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
               // extension == "zip" -> zipArchieve.unZipFile(file,outputZipFilePath)

                else -> Toast.makeText(context,"Unsupported Archive Format!!",Toast.LENGTH_SHORT).show()
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