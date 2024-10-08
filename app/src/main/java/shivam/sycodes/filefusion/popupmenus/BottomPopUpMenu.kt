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
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import shivam.sycodes.filefusion.R
import java.io.File
import java.util.Date


class BottomPopUpMenu(private val context: Context) {
    fun popUpMenuBottom(
        selectedFiles: List<File>, view: View,
        hideNavigationBar: () -> Unit,
        isFromCategory: Boolean,
    ){
        val bottomPopUpMenu = PopupMenu(context,view)
        bottomPopUpMenu.menuInflater.inflate(R.menu.bottompopupmenu, bottomPopUpMenu.menu)
        bottomPopUpMenu.setForceShowIcon(true)

        for (i in 0 until bottomPopUpMenu.menu.size()){
            val menuItem = bottomPopUpMenu.menu.getItem(i)
            val drawable = menuItem.icon
            drawable?.setTint(ContextCompat.getColor(context, R.color.iconcolor))
        }
        if (isFromCategory){
            bottomPopUpMenu.menu.findItem(R.id.archive).isVisible =false
            bottomPopUpMenu.menu.findItem(R.id.unarchive).isVisible =false
            bottomPopUpMenu.menu.findItem(R.id.decrypt).isVisible =false
            bottomPopUpMenu.menu.findItem(R.id.encrypt).isVisible =false
        }

        bottomPopUpMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.archive -> {
                    Toast.makeText(context, "Archiving file", Toast.LENGTH_SHORT).show()
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
                else -> {
                    Toast.makeText(context, "No selection", Toast.LENGTH_SHORT).show()
                    true
                }
            }
        }
        bottomPopUpMenu.show()
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
                    .setIcon(Icon.createWithResource(context, R.drawable.folder))  // Use your shortcut icon
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