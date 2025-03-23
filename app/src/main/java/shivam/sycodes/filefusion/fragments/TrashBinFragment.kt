package shivam.sycodes.filefusion.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import shivam.sycodes.filefusion.R
import shivam.sycodes.filefusion.adapters.TrashAdapter
import shivam.sycodes.filefusion.databinding.FragmentTrashBinBinding
import shivam.sycodes.filefusion.filehandling.FileOpener
import shivam.sycodes.filefusion.service.DeleteOperationCallback
import shivam.sycodes.filefusion.utility.FileOperationHelper
import java.io.File

class TrashBinFragment : Fragment() {

    private var _binding : FragmentTrashBinBinding?= null
    private val binding get() = _binding
    private lateinit var fileOperationHelper : FileOperationHelper
   private lateinit var trashPath : String
   private lateinit var trashAdapter: TrashAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrashBinBinding.inflate(inflater,container,false)
        fileOperationHelper = FileOperationHelper(requireContext())
        trashPath= fileOperationHelper.getTrashDir().toString()

        loadFiles(trashPath)

        binding?.trashBinDeleteAllButton?.setOnClickListener {
            showDeleteAllConfirmationDialog()
        }

        return binding!!.root
    }

    private fun showDeleteAllConfirmationDialog() {
        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Delete All Files")
            .setMessage("Are you sure you want to permanently delete all files from the trash bin?")
            .setPositiveButton("Delete") { _, _ -> deleteAllFiles() }
            .setNegativeButton("Cancel", null)
            .create()

        alertDialog.show()
    }

    private fun deleteAllFiles() {
        val directory = File(trashPath)
        if (directory.isDirectory && directory.exists()) {
            val files: Array<File> = directory.listFiles() ?: arrayOf()
            if (files.isNotEmpty()) {
                fileOperationHelper.deleteOperation(files.toList(), object : DeleteOperationCallback {
                    override fun onSuccess(deletedFiles: List<String>) {
                        Toast.makeText(requireContext(), "All files deleted successfully", Toast.LENGTH_SHORT).show()
                        loadFiles(trashPath)
                    }

                    override fun onFailure(errorMessage: String) {
                        Toast.makeText(requireContext(), "Failed to delete files", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                showToast("Trash bin is already empty")
            }
        }
    }

    private fun loadFiles(trashPath: String) {
        val directory = File(trashPath)
        if (directory.isDirectory && directory.exists()) {
            val files: Array<File> = directory.listFiles() ?: arrayOf()
            val sortedFiles = files.sortedByDescending { it.lastModified() }.toMutableList()

            if (sortedFiles.isNotEmpty()) {
                if (::trashAdapter.isInitialized) {

                    trashAdapter.files = sortedFiles.toTypedArray()
                    trashAdapter.notifyDataSetChanged()
                } else {

                    trashAdapter = TrashAdapter(requireContext(), sortedFiles.toTypedArray(),onItemClick = { selectedFile ->
                        val fileOpener = FileOpener(requireContext())
                        fileOpener.openFile(selectedFile)

                    }, onItemLongClick = { selectedFile ->
                        val trashDialog = AlertDialog.Builder(requireContext())
                        val trashDialogView = LayoutInflater.from(requireContext()).inflate(R.layout.trashdialog, null, false)
                        trashDialog.setView(trashDialogView)

                        val trashBinDelete = trashDialogView.findViewById<CardView>(R.id.trashBin_deletebutton)
                        val trashBinRestore = trashDialogView.findViewById<CardView>(R.id.trashBin_restoreButton)
                        val trashCancelButton = trashDialogView.findViewById<TextView>(R.id.trashCancelButton)
                        val trashFileNameShow = trashDialogView.findViewById<TextView>(R.id.trashFileNameShow)

                        trashFileNameShow.text= "File : ${selectedFile.name}"

                        val alertDialog = trashDialog.create()

                        trashBinDelete.setOnClickListener {
                            val position = sortedFiles.indexOf(selectedFile)
                            if (position >= 0) {
                                fileOperationHelper.deleteOperation(listOf(selectedFile), object :
                                    DeleteOperationCallback {
                                    override fun onSuccess(deletedFiles: List<String>) {
                                        Toast.makeText(requireContext(), "Files deleted successfully: ${deletedFiles.joinToString()}", Toast.LENGTH_SHORT).show()
                                    }

                                    override fun onFailure(errorMessage: String) {
                                      Toast.makeText(requireContext(), "files deletion Failed !!", Toast.LENGTH_SHORT).show()
                                    }
                                })
                                sortedFiles.removeAt(position)
                                trashAdapter.files = sortedFiles.toTypedArray()
                                trashAdapter.notifyItemRemoved(position)
                            }
                            alertDialog.dismiss()
                        }

                        trashBinRestore.setOnClickListener {
                            val position = sortedFiles.indexOf(selectedFile)
                            if (position >= 0) {
                                fileOperationHelper.isRestored(selectedFile)
                                sortedFiles.removeAt(position)
                                trashAdapter.files = sortedFiles.toTypedArray()
                                trashAdapter.notifyItemChanged(position)
                            }
                            alertDialog.dismiss()
                        }

                       trashCancelButton.setOnClickListener {
                           alertDialog.dismiss()
                       }

                        alertDialog.show()
                    })

                    binding?.trashBinRecyclerView?.layoutManager = GridLayoutManager(requireContext(), 3)
                    binding?.trashBinRecyclerView?.adapter = trashAdapter
                }
            } else {
                binding?.trashBinRecyclerView?.adapter = null
                showToast("Trash Directory is empty")
            }
        }
    }

    private fun showToast(message : String){
        Toast.makeText(requireContext(),message,Toast.LENGTH_SHORT).show()
    }
}