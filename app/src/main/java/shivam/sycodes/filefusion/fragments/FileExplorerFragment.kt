package shivam.sycodes.filefusion.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.CheckBox
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import shivam.sycodes.filefusion.R
import shivam.sycodes.filefusion.utility.FileSharingHelper
import shivam.sycodes.filefusion.adapters.FileAdapter
import shivam.sycodes.filefusion.databinding.FragmentFileExplorerBinding
import shivam.sycodes.filefusion.popupmenus.BottomPopUpMenu
import shivam.sycodes.filefusion.roomdatabase.AppDatabase
import shivam.sycodes.filefusion.service.DeleteOperationCallback
import shivam.sycodes.filefusion.service.PasteWorker
import shivam.sycodes.filefusion.utility.CreateFileAndFolder
import shivam.sycodes.filefusion.utility.FileOperationHelper
import shivam.sycodes.filefusion.utility.FileRenameHelper
import shivam.sycodes.filefusion.utility.PathDisplayHelper
import shivam.sycodes.filefusion.utility.PermissionHelper
import shivam.sycodes.filefusion.utility.PreferencesHelper
import shivam.sycodes.filefusion.viewModel.FileOperationViewModel
import java.io.File
import java.util.Locale

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
    private lateinit var currentSortOption : Pair<String?, Boolean>
    private lateinit var createFileFolderClass : CreateFileAndFolder
    private lateinit var permissionHelper: PermissionHelper
    private var isFabOpen = false
    private lateinit var pathDisplayHelper : PathDisplayHelper
    private var isFromCategory: Boolean = false
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
        bottomPopUpMenu= BottomPopUpMenu(requireContext())
        createFileFolderClass = CreateFileAndFolder(requireContext())


        requireActivity().onBackPressedDispatcher.addCallback(this,object : OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })
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
        popUpMenuTop()

        return binding.root
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
        getFilesWithCondition(directory, fileList) { it.lastModified() >= recentTimeLimit }

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
            fileAdapter = FileAdapter(requireContext(), filesList, onItemClick = { openFile(it) }, onItemLongClick = { bottomNavigation() })
            binding.RecyclerViewFileExplorer.adapter = fileAdapter
        } else {
            fileAdapter = FileAdapter(requireContext(), emptyList(), onItemClick = { openFile(it) }, onItemLongClick = { bottomNavigation() })
            binding.RecyclerViewFileExplorer.adapter = null
            Toast.makeText(requireContext(), "No files found in this category", Toast.LENGTH_SHORT).show()
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
                            openFile(selectedFile)
                        }
                    }, onItemLongClick = { _ -> bottomNavigation() })
                    binding.RecyclerViewFileExplorer.adapter = fileAdapter
                } else {
                    fileAdapter = FileAdapter(requireContext(), emptyList(), onItemClick = {_-> }, onItemLongClick = {_->})
                    binding.RecyclerViewFileExplorer.adapter = null
                    Toast.makeText(requireContext(), "No files found in this category", Toast.LENGTH_SHORT).show()
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
                    R.id.setting_button_toolbar ->{
                        Toast.makeText(requireContext(), "Settings Button click", Toast.LENGTH_SHORT).show()
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
            fileOperationViewModel.filesToCopyorCut = selectedFilesCopy
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
            val permanentDeletecheckBox=deleteDialogView.findViewById<CheckBox>(R.id.delete_permanently_checkbox)
            val cancelDeleteButton = deleteDialogView.findViewById<Button>(R.id.delete_operation_cancel_button)
            val movetoTrashButton = deleteDialogView.findViewById<Button>(R.id.moveto_trash_button)

            filesizeTextview.text=fileAdapter.getSelectedFiles().size.toString()

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
                    permissionHelper = PermissionHelper(requireContext())
                    if (!permissionHelper.isNotificationPermissionGranted()){
                        permissionHelper.requestNotificationPermission(requestNotificationPermissionLauncher)
                    }
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
                    isFromCategory,
                    category)
        }
    }
    private fun showPasteLayout() {
        binding.pastelayout.visibility = View.VISIBLE
        hideNavigationBars()
        loadFiles(currentPath)
        setupPasteOperation()
    }
    private fun setupPasteOperation() {
        binding.pasteButton.setOnClickListener {
            val filesToPaste = fileOperationViewModel.filesToCopyorCut
            val filePaths = filesToPaste!!.map { it.absolutePath }
            val isCutOperation = fileOperationViewModel.isCutOperation
            permissionHelper = PermissionHelper(requireContext())
            if(!permissionHelper.isNotificationPermissionGranted()){
                permissionHelper.requestNotificationPermission(requestNotificationPermissionLauncher)
            }

            val inputData = Data.Builder()
                .putStringArray("FILES_TO_PASTE", filePaths.toTypedArray())
                .putString("DESTINATION_PATH", currentPath)
                .putBoolean("IS_CUT_OPERATION", isCutOperation)
                .build()

            val pasteWorkRequest = OneTimeWorkRequestBuilder<PasteWorker>()
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(requireContext()).enqueue(pasteWorkRequest)

            WorkManager.getInstance(requireContext()).getWorkInfoByIdLiveData(pasteWorkRequest.id)
                .observe(viewLifecycleOwner, Observer { workInfo ->
                    if (workInfo != null && workInfo.state == WorkInfo.State.SUCCEEDED) {
                        Toast.makeText(requireContext(), "Paste operation completed successfully!", Toast.LENGTH_SHORT).show()
                        loadFiles(currentPath)
                    } else if (workInfo != null && workInfo.state == WorkInfo.State.FAILED) {
                        Toast.makeText(requireContext(), "Paste operation failed.", Toast.LENGTH_SHORT).show()
                        loadFiles(currentPath)
                    }
                })
            fileOperationViewModel.filesToCopyorCut = null
            binding.pastelayout.visibility = View.GONE

        }
        binding.newfolderWithpaste.setOnClickListener {
            createFileFolderClass.createNewFolder(currentPath,::loadFiles)
        }
    }
    private fun openFile(file: File) {
        val uri: Uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
        val mimeType: String? = getMimeType(file)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, mimeType)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "No application found to open this file", Toast.LENGTH_SHORT).show()
        }
    }
    private fun getMimeType(file: File): String? {
        val extension = MimeTypeMap.getFileExtensionFromUrl(file.name)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase(Locale.ROOT))
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
        if(fileAdapter.isMultiSelectedMode){
            fileAdapter.clearSelection()
            hideNavigationBars()
        }else if(isFabOpen){
            binding.fabContainer.visibility = View.GONE
            binding.fabContainer.animate().scaleY(0f).scaleX(0f).setDuration(200).withEndAction {
                binding.fabContainer.visibility = View.GONE
            }.start()
            binding.floatingActionButton.setImageResource(R.drawable.plus_24)
            isFabOpen = false
        }
        else {
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
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}