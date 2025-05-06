package shivam.sycodes.filefusion.archievingAndEncryption

import android.content.Context
import android.content.Intent
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import shivam.sycodes.filefusion.R
import java.io.File
import java.io.Serializable

class ArchiveDialog {

   fun zipCode(
       context: Context,
       selectedFiles: List<File>,
       requestNotification: () -> Unit,
       hideNavigationBar: () -> Unit
   ) {

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
//        val passwordEditText = archiveDialogView.findViewById<EditText>(R.id.archivePasswordEditText)
//        val confirmPasswordEditText = archiveDialogView.findViewById<EditText>(R.id.archiveConfirmPasswordEditText)
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
                        archievePasswordlayout.visibility = View.GONE
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
            requestNotification()
            val selectedType = typeSpinner.selectedItem.toString()
            val selectedCompression = compressionSpinner.selectedItem.toString()
            val archiveFilename = archiveFileName.text.toString() + ".zip"

            when(selectedType){
                "Zip" -> {
                    val zipIntent = Intent(context, ZipArchive::class.java).apply {
                        putExtra("files", listOf(selectedFiles) as Serializable)
                        putExtra("archive_name", archiveFilename)
                        putExtra("compression_level", selectedCompression)
                        putExtra("Action", "zip")
                    }
                    ContextCompat.startForegroundService(context,zipIntent)
                    dialog.dismiss()
                    hideNavigationBar()
                }
                "7z" -> {
                    val sevenZipIntent = Intent(context, SevenZipArchive::class.java).apply {
                        putExtra("files", listOf(selectedFiles) as Serializable)
                        putExtra("archive_name", archiveFilename)
                        putExtra("compression_level", selectedCompression)
                        putExtra("Action", "7zip")
                    }
                    ContextCompat.startForegroundService(context,sevenZipIntent)
                    dialog.dismiss()
                    hideNavigationBar()
                }
            }
        }
        dialog.show()
    }
}