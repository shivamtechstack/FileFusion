package shivam.sycodes.filefusion.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import shivam.sycodes.filefusion.R
import shivam.sycodes.filefusion.adapters.FileAdapter
import shivam.sycodes.filefusion.databinding.FragmentVaultBinding
import shivam.sycodes.filefusion.filehandling.FileOpener
import shivam.sycodes.filefusion.roomdatabase.AppDatabase
import shivam.sycodes.filefusion.service.DeleteOperationCallback
import shivam.sycodes.filefusion.service.VaultService
import shivam.sycodes.filefusion.utility.FileOperationHelper
import shivam.sycodes.filefusion.utility.PermissionHelper
import java.io.File

class VaultFragment : Fragment(){

    private var _binding : FragmentVaultBinding?= null
    private val binding get() = _binding
    private var vaultDir: File? = null
    private lateinit var vaultAdapter : FileAdapter
    private lateinit var permissionHelper : PermissionHelper
    private lateinit var fileOperationHelper : FileOperationHelper
    private lateinit var fileOpener : FileOpener
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(requireContext(), "Notification permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fileOperationHelper = FileOperationHelper(requireContext())
        permissionHelper = PermissionHelper(requireContext())
        fileOpener = FileOpener(requireContext())
        initializeVaultDirectory()
    }

    private fun initializeVaultDirectory() {
        vaultDir = File(requireContext().filesDir, "vault")
        vaultDir!!.setWritable(true)
        if (!vaultDir!!.exists()) {
            vaultDir!!.mkdirs()
            File(vaultDir, ".nomedia").createNewFile()
        }
        if (!vaultDir!!.canWrite()) {
            Toast.makeText(requireContext(), "Vault directory is not writable", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentVaultBinding.inflate(inflater,container,false)
        loadFiles(vaultDir)

        return binding?.root
    }

   fun loadFiles(vaultDir: File?) {
        val files = vaultDir?.listFiles() ?: arrayOf()
        val sortedFiles = files.sortedByDescending { it.lastModified() }.toMutableList()
        if (sortedFiles.isNotEmpty()){

            vaultAdapter = FileAdapter(requireContext(),sortedFiles,
                onItemClick ={ file ->
                    fileOpener.openFile(file)

            } , onItemLongClick = { selectedFiles ->
                popUpMenu()

            } )
            binding?.vaultRecyclerView?.layoutManager= LinearLayoutManager(requireContext())
            binding?.vaultRecyclerView?.adapter = vaultAdapter

        }else{
            binding?.vaultRecyclerView?.adapter = null
            Toast.makeText(requireContext(),"No files found",Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun popUpMenu() {
        binding?.VaultTopPopUpNavigation?.visibility = View.VISIBLE
        binding?.vaultBottomPopUpNavigation?.visibility = View.VISIBLE

        binding?.vaultClearFileButton?.setOnClickListener {
            vaultAdapter.clearSelection()
            hideOptionMenu()
            loadFiles(vaultDir)
        }
        vaultAdapter.setOnSelectionChangeListener(object : FileAdapter.OnSelectionChangeListener {
            @SuppressLint("SetTextI18n")
            override fun onSelectionChanged(selectedFileCount: Int) {
                binding?.numberOfSelectedFilesVault?.text = "$selectedFileCount File selected"
            }
        })

        binding?.vaultSelectAllButton?.setOnClickListener {
            vaultAdapter.selectAll()
        }

        binding?.vaultFileDelete?.setOnClickListener {
            val deleteAlertDialog = AlertDialog.Builder(requireContext())
            val deleteDialogView = LayoutInflater.from(requireContext()).inflate(R.layout.deletedialog,null,false)
            deleteAlertDialog.setView(deleteDialogView)

            val filesizeTextview= deleteDialogView.findViewById<TextView>(R.id.deleting_file_size)
            val permanentDeletecheckBox=deleteDialogView.findViewById<CheckBox>(R.id.delete_permanently_checkbox)
            val cancelDeleteButton = deleteDialogView.findViewById<Button>(R.id.delete_operation_cancel_button)
            val movetoTrashButton = deleteDialogView.findViewById<Button>(R.id.moveto_trash_button)
            val permanentDeleteLayout = deleteDialogView.findViewById<View>(R.id.delete_permanently_checkbox_layout)

            filesizeTextview.text=vaultAdapter.getSelectedFiles().size.toString()
            permanentDeletecheckBox.isChecked = true
            permanentDeleteLayout.visibility = View.GONE
            val deletedialog = deleteAlertDialog.create()

            permanentDeletecheckBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    movetoTrashButton.text = "Delete"
                } else {
                    movetoTrashButton.text = "Move to Trash"
                }
            }

            cancelDeleteButton.setOnClickListener {
                deletedialog.dismiss()
            }
            movetoTrashButton.setOnClickListener {
                if (permanentDeletecheckBox.isChecked){
                    if (!permissionHelper.isNotificationPermissionGranted()){
                        permissionHelper.requestNotificationPermission(requestNotificationPermissionLauncher)
                    }
                    val selectedFilesDelete = vaultAdapter.getSelectedFiles()
                    fileOperationHelper.deleteOperation(selectedFilesDelete, object :
                        DeleteOperationCallback {
                        override fun onSuccess(deletedFiles: List<String>) {
                            loadFiles(vaultDir)
                            Toast.makeText(requireContext(), "File deleted successfully: ${deletedFiles.joinToString()}", Toast.LENGTH_SHORT).show()

                            val vaultDao = AppDatabase.getDatabase(requireContext()).itemDAO()
                            CoroutineScope(Dispatchers.IO).launch {
                                deletedFiles.forEach { filePath ->
                                    val vaultItem = vaultDao.getVaultItemByPath(filePath)
                                    if (vaultItem != null) {
                                        vaultDao.deleteVaultItem(vaultItem)
                                    }
                                }
                            }
                        }
                        override fun onFailure(errorMessage: String) {
                            loadFiles(vaultDir)
                        }
                    })
                    deletedialog.dismiss()
                    vaultAdapter.clearSelection()
                    hideOptionMenu()
                }else{
                    deletedialog.dismiss()
                }
            }
            deletedialog.show()
        }
        binding?.MoveOutOfVault?.setOnClickListener {
            if (!permissionHelper.isNotificationPermissionGranted()) {
                permissionHelper.requestNotificationPermission(requestNotificationPermissionLauncher)
                return@setOnClickListener
            }

            val selectedFilesMoveOut = vaultAdapter.getSelectedFiles()
            if (selectedFilesMoveOut.isNotEmpty()) {
                val intent = Intent(requireContext(), VaultService::class.java).apply {
                    putExtra("selectedFiles", ArrayList(selectedFilesMoveOut))
                    action = "MOVE_OUT_OF_VAULT"
                }
                requireContext().startForegroundService(intent)
                vaultAdapter.clearSelection()
            } else {
                Toast.makeText(requireContext(), "No files selected", Toast.LENGTH_SHORT).show()
            }
            hideOptionMenu()
        }


        binding?.vaultFileInfo?.setOnClickListener {

        }

    }

    private fun hideOptionMenu(){
        binding?.VaultTopPopUpNavigation?.visibility = View.GONE
        binding?.vaultBottomPopUpNavigation?.visibility = View.GONE
    }

    override fun onDestroy() {
        _binding = null

        super.onDestroy()
    }

}