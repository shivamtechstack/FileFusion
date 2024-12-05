package shivam.sycodes.filefusion.others

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import shivam.sycodes.filefusion.R
import java.io.File

class BottomPopUpMenu(private val context: Context) {
    fun popUpMenuBottom(selectedFiles : List<File>, view:View ,hideNavigationBar:()->Unit){
        val bottomPopUpMenu = PopupMenu(context,view)
        bottomPopUpMenu.inflate(R.menu.bottompopupmenu)
        bottomPopUpMenu.setForceShowIcon(true)

        bottomPopUpMenu.setOnMenuItemClickListener { item ->
            when(item.itemId){
                R.id.archive ->{
                    Toast.makeText(context,"Archiving file",Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.copypath ->{
                    Toast.makeText(context,"Copying Path",Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.createshortcut ->{
                    Toast.makeText(context,"Creating Shortcut",Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.properties ->{
                    Toast.makeText(context,"Properties",Toast.LENGTH_SHORT).show()
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
}