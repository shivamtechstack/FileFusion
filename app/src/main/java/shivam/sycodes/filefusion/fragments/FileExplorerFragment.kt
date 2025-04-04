package shivam.sycodes.filefusion.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import shivam.sycodes.filefusion.AppSettings
import shivam.sycodes.filefusion.R
import shivam.sycodes.filefusion.utility.FileSharingHelper
import shivam.sycodes.filefusion.adapters.FileAdapter
import shivam.sycodes.filefusion.databinding.FragmentFileExplorerBinding
import shivam.sycodes.filefusion.filehandling.FileOpener
import shivam.sycodes.filefusion.popupmenus.BottomPopUpMenu
import shivam.sycodes.filefusion.roomdatabase.AppDatabase
import shivam.sycodes.filefusion.service.DeleteOperationCallback
import shivam.sycodes.filefusion.utility.CreateFileAndFolder
import shivam.sycodes.filefusion.utility.FileOperationHelper
import shivam.sycodes.filefusion.utility.FileRenameHelper
import shivam.sycodes.filefusion.utility.PathDisplayHelper
import shivam.sycodes.filefusion.utility.PermissionHelper
import shivam.sycodes.filefusion.utility.PreferencesHelper
import shivam.sycodes.filefusion.viewModel.FileOperationViewModel
import java.io.File

class FileExplorerFragment : Fragment() {

    private var _binding: FragmentFileExplorerBinding?= null
    private val binding get() = _binding!!
    private var currentPath : String? = null
    private lateinit var fileAdapter:FileAdapter
    private lateinit var absolutePath :String
    private lateinit var fileSharingHelper: FileSharingHelper
    private lateinit var fileRenameHelper : FileRenameHelper
    private lateinit var fileOperationViewModel: FileOperationViewModel
    private lateinit var fileOperationHelper: FileOperationHelper
    private lateinit var bottomPopUpMenu: BottomPopUpMenu
    private lateinit var preferencesHelper: PreferencesHelper
    private lateinit var fileOpener : FileOpener
    private lateinit var currentSortOption : Pair<String?, Boolean>
    private lateinit var createFileFolderClass : CreateFileAndFolder
    private lateinit var permissionHelper: PermissionHelper
    private var isFabOpen = false
    private lateinit var pathDisplayHelper : PathDisplayHelper
    private var isFromCategory: Boolean = false

    private val pasteCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (isAdded) {
                Toast.makeText(requireContext(), "Paste complete", Toast.LENGTH_SHORT).show()
                loadFiles(currentPath)
            }
        }
    }
    private val encryptionCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (isAdded) {
                Toast.makeText(requireContext(), "Encryption complete", Toast.LENGTH_SHORT).show()
                loadFiles(currentPath)
            }
        }
    }
    private val decryptionCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (isAdded) {
                Toast.makeText(requireContext(), "Decryption complete", Toast.LENGTH_SHORT).show()
                loadFiles(currentPath)
            }
        }
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(requireContext(), "Notification permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    companion object{
        private const val ARG_PATH = "path"
        private const val ARG_CATEGORY="category"

        fun newInstance(path : String?,category: String?):FileExplorerFragment{
            val fragment = FileExplorerFragment()
            val args = Bundle()
            args.putString(ARG_PATH, path)
            args.putString(ARG_CATEGORY, category)

            fragment.arguments = args
            return fragment
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferencesHelper = PreferencesHelper(requireContext())
        fileOperationViewModel = ViewModelProvider(requireActivity())[FileOperationViewModel::class.java]
        currentSortOption = preferencesHelper.getSortOptions()
        pathDisplayHelper = PathDisplayHelper(requireContext())
        fileSharingHelper=FileSharingHelper(requireContext())
        fileRenameHelper= FileRenameHelper(requireContext())
        fileOperationHelper= FileOperationHelper(requireContext())
        bottomPopUpMenu= BottomPopUpMenu(this,requireContext())
        createFileFolderClass = CreateFileAndFolder(requireContext())
        fileOpener = FileOpener(requireContext())
        permissionHelper = PermissionHelper(requireContext())


        requireActivity().onBackPressedDispatcher.addCallback(this,object : OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            pasteCompleteReceiver,
            IntentFilter("shivam.sycodes.filefusion.PASTE_COMPLETE")
        )
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(encryptionCompleteReceiver,
            IntentFilter("shivam.sycodes.filefusion.ENCRYPTION_COMPLETE"))
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(decryptionCompleteReceiver,
            IntentFilter("shivam.sycodes.filefusion.DECRYPTION_COMPLETE"))

    }
    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(pasteCompleteReceiver)
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(encryptionCompleteReceiver)
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(decryptionCompleteReceiver)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFileExplorerBinding.inflate(inflater,container,false)

        arguments?.let {
            absolutePath = it.getString(ARG_PATH).toString()
            currentPath = it.getString(ARG_PATH)
        }
        pathDisplayHelper.setupPathLayout(binding.pathContainer,currentPath)

        binding.RecyclerViewFileExplorer.layoutManager= LinearLayoutManager(context)
        val isGridView=preferencesHelper.isGridView()
        setupRecyclerView(isGridView)

        if (fileOperationViewModel.filesToCopyorCut != null) {
            binding.pastelayout.visibility = View.VISIBLE
            setupPasteOperation()
        } else {
            binding.pastelayout.visibility = View.GONE
        }


        val category = arguments?.getString(ARG_CATEGORY)
        when(category){
            "recent" -> loadFilesFromStorage(this::getRecentFiles)
            "photos" -> loadFilesFromMediaStore(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            "videos" -> loadFilesFromMediaStore(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            "music" -> loadFilesFromMediaStore(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
            "apks" -> loadApkFiles()
            "archives" -> loadFilesFromStorage(this::getArchiveFiles)
            "bookmarks"-> loadBookmarks()
            else -> loadFiles(currentPath)
        }

        binding.pasteClear.setOnClickListener {
            binding.pastelayout.visibility = View.GONE
            fileOperationViewModel.filesToCopyorCut = null
        }
        if (isFromCategory){
            binding.floatingActionButton.visibility = View.GONE
        }
        binding.floatingActionButton.setOnClickListener {
            toogleFabMenu()
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { performGlobalSearch(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    loadFiles(currentPath)
                } else {
                    performGlobalSearch(newText)
                }
                return true
            }
        })

        popUpMenuTop()


        return binding.root
    }

    private fun performGlobalSearch(query: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val allFiles = mutableListOf<File>()
            searchFiles(Environment.getExternalStorageDirectory(), allFiles, query)

            withContext(Dispatchers.Main) {
                displayFilesInRecyclerView(allFiles)
            }
        }
    }

    private fun searchFiles(directory: File, resultList: MutableList<File>, query: String) {
        if (directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                if (file.name.contains(query, ignoreCase = true)) {
                    resultList.add(file)
                }
                if (file.isDirectory) {
                    searchFiles(file, resultList, query)
                }
            }
        }
    }

    private fun loadBookmarks(){
        isFromCategory = true
        CoroutineScope(Dispatchers.IO).launch {
            val bookmarkDAO = AppDatabase.getDatabase(requireContext()).itemDAO()
            val bookmarkEntities = bookmarkDAO.getAllBookmarks()

            val bookmarkFiles = bookmarkEntities.map { bookmark ->
                File(bookmark.bookmarkFilePath)
            }.filter { it.exists() }
            withContext(Dispatchers.Main){
                displayFilesInRecyclerView(bookmarkFiles)
            }
        }
    }
    private fun loadFilesFromStorage(getFilesMethod: (File, MutableList<File>) -> Unit) {
        isFromCategory = true
        val fileList = mutableListOf<File>()
        getFilesMethod(Environment.getExternalStorageDirectory(), fileList)
        fileList.sortByDescending { it.lastModified() }

        displayFilesInRecyclerView(fileList)
    }

    private fun loadFilesFromMediaStore(uri: Uri) {
        isFromCategory = true
        val fileList = mutableListOf<File>()
        val projection = arrayOf(MediaStore.MediaColumns.DATA)

        val cursor = requireContext().contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            val dataIndex = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            while (it.moveToNext()) {
                val filePath = it.getString(dataIndex)
                val file = File(filePath)
                if (file.exists()) fileList.add(file)
            }
        }
        fileList.sortByDescending { it.lastModified() }

        displayFilesInRecyclerView(fileList)
    }

    private fun loadApkFiles() {
        isFromCategory = true
        val fileList = mutableListOf<File>()
        val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
        val selectionArgs = arrayOf("application/vnd.android.package-archive")

        val cursor = requireContext().contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection, selection, selectionArgs, null
        )
        cursor?.use {
            val dataIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            while (it.moveToNext()) {
                val filePath = it.getString(dataIndex)
                val file = File(filePath)
                if (file.exists()) fileList.add(file)
            }
        }
        fileList.sortByDescending { it.lastModified() }

        displayFilesInRecyclerView(fileList)
    }

    private fun getRecentFiles(directory: File, fileList: MutableList<File>) {
        val oneWeekInMillis = 7 * 24 * 60 * 60 * 1000
        val recentTimeLimit = System.currentTimeMillis() - oneWeekInMillis
        getFilesWithCondition(directory, fileList) { !it.isHidden && it.lastModified() >= recentTimeLimit }
        fileList.sortByDescending { it.lastModified() }
    }

    private fun getArchiveFiles(directory: File, fileList: MutableList<File>) {
        val archiveExtensions = listOf("zip", "rar", "7z", "tar", "gz", "bz2")
        getFilesWithCondition(directory, fileList) { archiveExtensions.contains(it.extension.lowercase()) }
    }

    private fun getFilesWithCondition(directory: File, fileList: MutableList<File>, condition: (File) -> Boolean) {
        if (directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    getFilesWithCondition(file, fileList, condition)
                } else if (condition(file)) {
                    fileList.add(file)
                }
            }
        }
    }

    private fun displayFilesInRecyclerView(filesList: List<File>) {
        if (filesList.isNotEmpty()) {
            fileAdapter = FileAdapter(requireContext(), filesList,
                onItemClick = { file ->
                    if (file.isDirectory) {
                        currentPath = file.absolutePath
                        binding.searchView.setQuery("",false)
                        loadFiles(currentPath)
                    } else {
                        fileOpener.openFile(file)
                    } },
                onItemLongClick = { bottomNavigation() })
            showRecyclerView()
            binding.RecyclerViewFileExplorer.adapter = fileAdapter
        } else {
            binding.RecyclerViewFileExplorer.adapter = null
          emptyRecyclerViewLoad()
        }
    }

    private fun setupRecyclerView(isGridView: Boolean) {
        binding.RecyclerViewFileExplorer.layoutManager = if (isGridView) {
            GridLayoutManager(context, 3)
        } else {
            LinearLayoutManager(context)
        }
    }

    private fun loadFiles(path: String?) {
        pathDisplayHelper.setupPathLayout(binding.pathContainer,path)
        path?.let {
            val directory = File(path)
            if (directory.exists() && directory.isDirectory){
                val files : Array<File> = directory.listFiles()?: arrayOf()
                val shouldShowHiddenFiles = preferencesHelper.isFilesHidden()
                val visibleFiles = if (shouldShowHiddenFiles) {
                    files.toList()
                } else {
                    files.filterNot { it.isHidden }
                }

                val sortedFiles = sortFiles(visibleFiles)

                if (sortedFiles.isNotEmpty()) {
                    fileAdapter = FileAdapter(requireContext(),sortedFiles, onItemClick = { selectedFile ->
                        if (selectedFile.isDirectory) {
                            currentPath = selectedFile.absolutePath
                            loadFiles(currentPath)
                        } else {
                            fileOpener.openFile(selectedFile)
                        }
                    }, onItemLongClick = { _ -> bottomNavigation() })
                    showRecyclerView()
                    binding.RecyclerViewFileExplorer.adapter = fileAdapter
                } else {
                    fileAdapter = FileAdapter(requireContext(), emptyList(), onItemClick = {_-> }, onItemLongClick = {_->})
                    binding.RecyclerViewFileExplorer.adapter = null
                    emptyRecyclerViewLoad()
                }
            }
        }
    }

    private fun sortFiles(files : List<File>): List<File>{
        val folders = files.filter { it.isDirectory }
        val filesOnly = files.filter { !it.isDirectory }

        val sortedFolders = when (currentSortOption.first) {
            "name" -> if (currentSortOption.second) folders.sortedBy { it.name } else folders.sortedByDescending { it.name }
            "size" -> if (currentSortOption.second) folders.sortedBy { it.length() } else folders.sortedByDescending { it.length() }
            "last_modified" -> if (currentSortOption.second) folders.sortedBy { it.lastModified() } else folders.sortedByDescending { it.lastModified() }
            "type" -> if (currentSortOption.second) folders.sortedBy { it.extension } else folders.sortedByDescending { it.extension }
            else -> folders
        }

        val sortedFiles = when (currentSortOption.first) {
            "name" -> if (currentSortOption.second) filesOnly.sortedBy { it.name } else filesOnly.sortedByDescending { it.name }
            "size" -> if (currentSortOption.second) filesOnly.sortedBy { it.length() } else filesOnly.sortedByDescending { it.length() }
            "last_modified" -> if (currentSortOption.second) filesOnly.sortedBy { it.lastModified() } else filesOnly.sortedByDescending { it.lastModified() }
            "type" -> if (currentSortOption.second) filesOnly.sortedBy { it.extension } else filesOnly.sortedByDescending { it.extension }
            else -> filesOnly
        }

        return sortedFolders + sortedFiles
    }
    private fun popUpMenuTop() {
        binding.toolbarPopupMenu.setOnClickListener { view ->
            val toolbarPopupMenu = PopupMenu(requireContext(), view)
            toolbarPopupMenu.menuInflater.inflate(R.menu.toolbarmenu,toolbarPopupMenu.menu)
            toolbarPopupMenu.setForceShowIcon(true)
            for (i in 0 until toolbarPopupMenu.menu.size()){
                val menuItem = toolbarPopupMenu.menu.getItem(i)
                val drawable = menuItem.icon
                drawable?.setTint(ContextCompat.getColor(requireContext(), R.color.iconcolor))
            }
            val isGridView = preferencesHelper.isGridView()
            val isHiddenFiles = preferencesHelper.isFilesHidden()

            if (isFromCategory){
                toolbarPopupMenu.menu.findItem(R.id.sortby_dialogbox).isVisible = false
                toolbarPopupMenu.menu.findItem(R.id.hidden_file_button).isVisible = false
            }
            toolbarPopupMenu.menu.findItem(R.id.list_layout_mode).isChecked = !isGridView
            toolbarPopupMenu.menu.findItem(R.id.gridView_layout_mode).isChecked = isGridView
            toolbarPopupMenu.menu.findItem(R.id.hidden_file_button).isChecked = isHiddenFiles

            toolbarPopupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.sortby_dialogbox -> {
                        showSortDialogBox()
                        true
                    }

                    R.id.hidden_file_button ->{
                        val newHiddenState = !preferencesHelper.isFilesHidden()
                        preferencesHelper.hiddenFiles(newHiddenState)
                        menuItem.isChecked = newHiddenState
                        loadFiles(currentPath)
                        true
                    }
                    R.id.trashBin_button_toolbar ->{
                        fragmentManager?.beginTransaction()?.replace(R.id.fragmentContainerView,TrashBinFragment())?.addToBackStack(null)?.commit()
                        true
                    }
                    R.id.setting_button_toolbar ->{
                        val intent=Intent(this@FileExplorerFragment.requireContext(),AppSettings::class.java)
                        startActivity(intent)
                        true
                    }
                    R.id.list_layout_mode ->{
                        if (!menuItem.isChecked) {
                            preferencesHelper.saveViewMode(false)
                            setupRecyclerView(false)
                            menuItem.isChecked = true
                            toolbarPopupMenu.menu.findItem(R.id.gridView_layout_mode).isChecked = false
                            loadFiles(currentPath)
                        }
                        true
                    }
                    R.id.gridView_layout_mode ->{
                        if (!menuItem.isChecked) {
                            preferencesHelper.saveViewMode(true)
                            setupRecyclerView(true)
                            menuItem.isChecked = true
                            toolbarPopupMenu.menu.findItem(R.id.list_layout_mode).isChecked = false
                            loadFiles(currentPath)
                        }
                        true
                    }
                    else -> false
                }
            }
            toolbarPopupMenu.show()
        }
    }
    private fun bottomNavigation() {
        binding.toolbar.visibility = View.GONE
        binding.floatingActionButton.visibility =View.GONE
        binding.pathContainer.visibility = View.GONE
        binding.bottomNavigation.visibility = View.VISIBLE
        binding.TopNavigation.visibility = View.VISIBLE

        fileAdapter.setOnSelectionChangeListener(object : FileAdapter.OnSelectionChangeListener {
            override fun onSelectionChanged(selectedFileCount: Int) {
                binding.numberOfSelectedFilesTextView.text = "$selectedFileCount selected"
            }
        })

        binding.clearSelectionButton.setOnClickListener {
            fileAdapter.clearSelection()
            hideNavigationBars()
            loadFiles(currentPath)
        }
        binding.selectAllButton.setOnClickListener {
            fileAdapter.selectAll()
        }
        binding.cutButton.setOnClickListener {
            val selectedFilesCut = fileAdapter.getSelectedFiles()
            fileOperationViewModel.filesToCopyorCut = selectedFilesCut
            fileOperationViewModel.isCutOperation = true
            showPasteLayout()
        }
        binding.copyButton.setOnClickListener {
            val selectedFilesCopy = fileAdapter.getSelectedFiles()
            Log.d("FileSelection", "Copy selected ${selectedFilesCopy.size} file(s): ${selectedFilesCopy.joinToString { it.name }}")
            fileOperationViewModel.filesToCopyorCut = selectedFilesCopy
            Log.d("FileSelection", "Global list now has ${fileOperationViewModel.filesToCopyorCut?.size} file(s)")
            fileOperationViewModel.isCutOperation = false
            showPasteLayout()
        }
        binding.shareButton.setOnClickListener {
            val selectedFilesSharing = fileAdapter.getSelectedFiles()
            fileSharingHelper.shareFiles(selectedFilesSharing)
        }
        binding.renameButton.setOnClickListener {
            val selectedFilesRename = fileAdapter.getSelectedFiles()
            fileRenameHelper.renameFiles(
                selectedFilesRename,
                {loadFiles(currentPath)},
                ::hideNavigationBars
            )
        }
        binding.deleteButton.setOnClickListener {
            val deleteAlertDialog = AlertDialog.Builder(requireContext())
            val deleteDialogView = LayoutInflater.from(requireContext()).inflate(R.layout.deletedialog,null,false)
            deleteAlertDialog.setView(deleteDialogView)

            val filesizeTextview= deleteDialogView.findViewById<TextView>(R.id.deleting_file_size)
            val filesNamesTextview= deleteDialogView.findViewById<TextView>(R.id.deleting_file_names)
            val permanentDeletecheckBox=deleteDialogView.findViewById<CheckBox>(R.id.delete_permanently_checkbox)
            val cancelDeleteButton = deleteDialogView.findViewById<Button>(R.id.delete_operation_cancel_button)
            val movetoTrashButton = deleteDialogView.findViewById<Button>(R.id.moveto_trash_button)

            filesizeTextview.text="No. of file/Folder: ${fileAdapter.getSelectedFiles().size.toString()}"
            filesNamesTextview.text = "Name : ${fileAdapter.getSelectedFiles().joinToString { it.name }}"

            val deletedialog = deleteAlertDialog.create()
            val selectedFiles = fileAdapter.getSelectedFiles()

            val isExternalStorage = selectedFiles.any { filePath ->
                filePath.startsWith("/storage/") && !filePath.startsWith("/storage/emulated/0/")
            }

            if (isExternalStorage) {
                // If the file is from SD card or USB, force permanent delete
                permanentDeletecheckBox.isChecked = true
                permanentDeletecheckBox.isEnabled = false
                movetoTrashButton.text = "Delete"
            } else {
                permanentDeletecheckBox.isEnabled = true
            }

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
                permissionHelper.ensureNotificationSetup(requestNotificationPermissionLauncher)
                if (permanentDeletecheckBox.isChecked){
                    val selectedFilesDelete = fileAdapter.getSelectedFiles()
                    fileOperationHelper.deleteOperation(selectedFilesDelete, object :
                        DeleteOperationCallback {
                        override fun onSuccess(deletedFiles: List<String>) {

                            loadFiles(currentPath)
                            Toast.makeText(requireContext(), "Files deleted successfully: ${deletedFiles.joinToString()}", Toast.LENGTH_SHORT).show()
                        }

                        override fun onFailure(errorMessage: String) {
                            loadFiles(currentPath)
                        }
                    })
                    deletedialog.dismiss()
                    fileAdapter.clearSelection()
                    hideNavigationBars()
                }else{
                   val selectedFilesForTrash = fileAdapter.getSelectedFiles()
                    val trashMoveSuccess = fileOperationHelper.moveToTrash(selectedFilesForTrash)
                    if (trashMoveSuccess){
                        fileAdapter.clearSelection()
                        loadFiles(currentPath)
                    }
                    fileAdapter.clearSelection()
                    hideNavigationBars()
                    loadFiles(currentPath)
                    deletedialog.dismiss()
                }
            }
            deletedialog.show()
        }
        binding.moreOptionsButton.setOnClickListener {
            val category = arguments?.getString(ARG_CATEGORY)
                bottomPopUpMenu.popUpMenuBottom(
                    fileAdapter.getSelectedFiles(),
                    view = binding.moreOptionsButton,
                    ::hideNavigationBars,
                    ::requestNotification,
                    isFromCategory,
                    category)
        }
    }
    private fun showPasteLayout() {
        binding.pastelayout.visibility = View.VISIBLE
        hideNavigationBars()
        setupPasteOperation()
        loadFiles(currentPath)
    }
    private fun setupPasteOperation() {
        binding.pasteButton.setOnClickListener {
            val filesToPaste = fileOperationViewModel.filesToCopyorCut
            val isCutOperation = fileOperationViewModel.isCutOperation

            permissionHelper.ensureNotificationSetup(requestNotificationPermissionLauncher)

            fileOperationHelper.pasteOperation(currentPath,filesToPaste,isCutOperation)

            binding.pastelayout.visibility = View.GONE
            fileOperationViewModel.filesToCopyorCut = null

        }
        binding.newfolderWithpaste.setOnClickListener {
            createFileFolderClass.createNewFolder(currentPath,::loadFiles)
        }
    }

    private fun showSortDialogBox() {
        val alertDialog = AlertDialog.Builder(requireContext())
        val sortDialogView = LayoutInflater.from(requireContext()).inflate(R.layout.sortby_dialog_box,null,false)
        alertDialog.setView(sortDialogView)
        alertDialog.setTitle("Sort by")

        val sortGroup = sortDialogView.findViewById<RadioGroup>(R.id.sort_group_button)
        val ascendingDescendingGroup = sortDialogView.findViewById<RadioGroup>(R.id.ascending_descending_group)

        when(currentSortOption.first){
            "name" -> sortGroup.check(R.id.name_sort)
            "size" -> sortGroup.check(R.id.size_sort)
            "last_modified" -> sortGroup.check(R.id.last_modified_sort)
            "type" -> sortGroup.check(R.id.type_sort)
        }
        if (currentSortOption.second){
            ascendingDescendingGroup.check(R.id.ascending_sort)
        }else{
            ascendingDescendingGroup.check(R.id.descending_sort)
        }

        val dialog =alertDialog.create()

        sortDialogView.findViewById<Button>(R.id.sort_dialog_ok_button).setOnClickListener {
            val selectedSortOption = when(sortGroup.checkedRadioButtonId){
                R.id.name_sort -> "name"
                R.id.size_sort -> "size"
                R.id.last_modified_sort -> "last_modified"
                R.id.type_sort -> "type"
                else -> "name"
            }
            val isAscending = ascendingDescendingGroup.checkedRadioButtonId == R.id.ascending_sort
            preferencesHelper.saveSortOptions(selectedSortOption,isAscending)
            currentSortOption = Pair(selectedSortOption,isAscending)
            loadFiles(currentPath)
            dialog.dismiss()
        }
        sortDialogView.findViewById<Button>(R.id.sort_dialog_cancel_button).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun toogleFabMenu(){
        if (isFabOpen){
            fapButtonVisibility()
        }else{
            binding.fabContainer.visibility = View.VISIBLE
            binding.fabContainer.animate().scaleY(1f).scaleX(1f).setDuration(200).start()
            binding.floatingActionButton.setImageResource(R.drawable.baseline_clear_24)
            isFabOpen = true
        }
        binding.fabActionNewFile.setOnClickListener {
            createFileFolderClass.createNewFile(
                currentPath,
                ::loadFiles
            )
           fapButtonVisibility()
        }
        binding.fabActionNewFolder.setOnClickListener {
            createFileFolderClass.createNewFolder(currentPath,::loadFiles)
           fapButtonVisibility()
        }
    }
    private fun fapButtonVisibility() {
        binding.fabContainer.visibility = View.GONE
        binding.fabContainer.animate().scaleY(0f).scaleX(0f).setDuration(200).withEndAction {
            binding.fabContainer.visibility = View.GONE
        }.start()
        binding.floatingActionButton.setImageResource(R.drawable.plus_24)
        isFabOpen = false
    }
    private fun handleBackPress() {
        if (::fileAdapter.isInitialized && fileAdapter.isMultiSelectedMode) {
            fileAdapter.clearSelection()
            hideNavigationBars()
            loadFiles(currentPath)
        } else if (isFabOpen) {
            binding.fabContainer.animate()
                .scaleY(0f)
                .scaleX(0f)
                .setDuration(200)
                .withEndAction {
                    binding.fabContainer.visibility = View.GONE
                }
                .start()
            binding.floatingActionButton.setImageResource(R.drawable.plus_24)
            isFabOpen = false
        } else {
            if (isFromCategory) {
                parentFragmentManager.popBackStack()
            } else if (currentPath != absolutePath) {
                val parentFile = currentPath?.let { File(it).parentFile }
                if (parentFile != null && parentFile.exists()) {
                    currentPath = parentFile.absolutePath
                    loadFiles(currentPath)
                } else {
                    requireActivity().onBackPressed()
                }
            } else {
                parentFragmentManager.popBackStack()
            }
        }
    }


    private fun hideNavigationBars(){
        binding.bottomNavigation.visibility=View.GONE
        binding.TopNavigation.visibility=View.GONE
        binding.toolbar.visibility = View.VISIBLE
        if (isFromCategory){
            binding.floatingActionButton.visibility = View.GONE
            binding.pathContainer.visibility = View.GONE
        }else {
            binding.floatingActionButton.visibility = View.VISIBLE
            binding.pathContainer.visibility = View.VISIBLE
        }
        loadFiles(currentPath)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun emptyRecyclerViewLoad(){
        binding.RecyclerViewFileExplorer.visibility = View.GONE
        binding.emptyRecyclerViewImage.visibility = View.VISIBLE
    }
    private fun showRecyclerView(){
        binding.RecyclerViewFileExplorer.visibility = View.VISIBLE
        binding.emptyRecyclerViewImage.visibility = View.GONE
    }
    fun requestNotification(){
        permissionHelper = PermissionHelper(requireContext())
        permissionHelper.ensureNotificationSetup(requestNotificationPermissionLauncher)
    }

}