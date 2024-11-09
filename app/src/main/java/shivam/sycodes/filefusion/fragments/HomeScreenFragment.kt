package shivam.sycodes.filefusion.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.os.Environment.DIRECTORY_DOCUMENTS
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.StatFs
import android.os.storage.StorageManager
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.commit
import shivam.sycodes.filefusion.R
import shivam.sycodes.filefusion.databinding.FragmentHomeScreenBinding
import java.io.File


class HomeScreenFragment : Fragment() {

    private var _binding : FragmentHomeScreenBinding ? = null
    private val binding get() = _binding!!

    private val internalStoragePath = Environment.getExternalStorageDirectory().absolutePath
    private val downloadsFolderPath = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).absolutePath
    private val documentsFolderPath = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS).absolutePath
    private val storageManager by lazy { requireContext().getSystemService(Context.STORAGE_SERVICE) as StorageManager }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
       _binding = FragmentHomeScreenBinding.inflate(inflater,container,false)

        updateStorageDetails(internalStoragePath,binding.storageProgressBar,binding.storagePercentage,binding.storageAvailable)

        val sdCardPath = getExternalStoragePaths()
        val usbPath = getUsbDevicePaths()

        if (sdCardPath == null){
            binding.ExternalStorageCardview.visibility = View.GONE
        }else{
            binding.ExternalStorageCardview.visibility = View.VISIBLE
        }

        if (usbPath == null){
            binding.usbStorageCardview.visibility = View.GONE
        }else{
            binding.usbStorageCardview.visibility = View.VISIBLE
        }

        binding.InternalStorageCardview.setOnClickListener {
           fragment(internalStoragePath,null)
        }
        binding.ExternalStorageCardview.setOnClickListener {
            sdCardPath?.let { path ->
                fragment(path, null)
                updateStorageDetails(sdCardPath,binding.sdcardstorageProgressBar,binding.sdcardprogressPercentage,binding.sdcardstorageAvailable)
            } ?: run {
                Toast.makeText(requireContext(),"No SD Card Detected.",Toast.LENGTH_SHORT).show()
            }
        }
        binding.usbStorageCardview.setOnClickListener {
            usbPath?.let { path ->
                fragment(path,null)
                updateStorageDetails(usbPath,binding.usbstorageProgressBar,binding.usbprogressPercentage,binding.usbstorageAvailable)
            }?: run{
                Toast.makeText(requireContext(),"No USB Storage Detected.",Toast.LENGTH_SHORT).show()
            }
        }
        binding.recentButton.setOnClickListener {
            fragment(null,"recent")
        }
        binding.photosButton.setOnClickListener {
            fragment(null, "photos")
        }
        binding.musicButton.setOnClickListener {
            fragment(null,"music")
        }
        binding.videosButton.setOnClickListener {
            fragment(null,"videos")
        }
        binding.apksButton.setOnClickListener {
            fragment(null,"apks")
        }
        binding.documentsButton.setOnClickListener {
            fragment(documentsFolderPath,null)
        }
        binding.downloadsButton.setOnClickListener {
            fragment(downloadsFolderPath,null)
        }
        binding.archieveButton.setOnClickListener {
            fragment(null, "archives")
        }
        binding.trashBinCardView.setOnClickListener{
           fragmentManager?.beginTransaction()?.replace(R.id.fragmentContainerView,TrashBinFragment())?.addToBackStack(null)?.commit()
        }
        binding.bookmarkCardView.setOnClickListener {
            fragment(null,"bookmarks")
        }
        binding.vaultBinCardView.setOnClickListener {
            fragmentManager?.beginTransaction()?.replace(R.id.fragmentContainerView,VaultFragment())?.addToBackStack(null)?.commit()
        }
        return binding.root
    }
    private fun fragment(path : String?, category: String?){
        val fragmentStacking = FileExplorerFragment.newInstance(path,category)
        parentFragmentManager.commit {
            replace(R.id.fragmentContainerView,fragmentStacking).addToBackStack(null)
        }
    }
    private fun getExternalStoragePaths(): String? {
        val storageVolumes = storageManager.storageVolumes
        for (volume in storageVolumes) {
            if (volume.isRemovable && volume.state == Environment.MEDIA_MOUNTED) {
                val sdCardPath = volume.directory?.absolutePath
                if (sdCardPath != null) {
                    return sdCardPath
                }
            }
        }
        return null
    }

    private fun getUsbDevicePaths(): String? {
        val storageVolumes = storageManager.storageVolumes
        for (volume in storageVolumes) {
            if (!volume.isRemovable && !volume.isPrimary && volume.state == Environment.MEDIA_MOUNTED) {
                val usbPath = volume.directory?.absolutePath
                if (usbPath != null) {
                    return usbPath
                }
            }
        }
        return null
    }
    @SuppressLint("SetTextI18n")
    private fun updateStorageDetails(
        path: String,
        progressBar: ProgressBar,
        percentageView: TextView,
        availableTextView: TextView
    ) {
        val stat = StatFs(path)
        val totalBytes = stat.totalBytes
        val availableBytes = stat.availableBytes
        val usedBytes = totalBytes - availableBytes
        val usedPercentage = (usedBytes.toDouble() / totalBytes.toDouble()) * 100

        progressBar.progress = usedPercentage.toInt()
        percentageView.text = "${usedPercentage.toInt()}%"

        val totalSize = formatSize(totalBytes)
        val availableSize = formatSize(availableBytes)
        availableTextView.text = "$availableSize | $totalSize available"
    }

    private fun formatSize(size: Long): String {
        val kb = 1024
        val mb = kb * 1024
        val gb = mb * 1024
        return when {
            size >= gb -> "${size / gb} GB"
            size >= mb -> "${size / mb} MB"
            size >= kb -> "${size / kb} KB"
            else -> "$size B"
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}