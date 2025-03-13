package shivam.sycodes.filefusion.fragments

import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import shivam.sycodes.filefusion.R
import shivam.sycodes.filefusion.databinding.FragmentFileInfoBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileInfoFragment : Fragment() {
    private var _binding: FragmentFileInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFileInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getStringArrayList("selected_file_paths")?.let { filePaths ->
            displayFileInfo(filePaths.map { File(it) })
        }
        binding.fileInfoBackArrow.setOnClickListener {
            fragmentManager?.popBackStack()
        }
    }

    private fun displayFileInfo(files: List<File>) {
        if (files.isEmpty()) return

        val firstFile = files[0]
        val fileNames = mutableListOf<String>()
        var totalSize: Long = 0
        var totalFiles = 0
        var totalFolders = 0
        var lastModifiedTime: Long = 0
        val parentDirectories = mutableSetOf<String>()
        val fileTypes = mutableSetOf<String>()

        files.forEach { file ->
            fileNames.add(file.name)
            totalSize += if (file.isDirectory) getDirectorySize(file) else file.length()
            lastModifiedTime = maxOf(lastModifiedTime, file.lastModified())

            if (file.isDirectory) {
                totalFolders++
                totalFiles += countFilesInDirectory(file)
            } else {
                totalFiles++
                fileTypes.add(file.extension.ifEmpty { "Unknown" })
            }

            parentDirectories.add(file.parent ?: "Unknown")
        }

        // Convert size to readable format
        val readableSize = formatFileSize(totalSize)
        val lastModifiedDate = formatDate(lastModifiedTime)
        val fileTypeString = if (fileTypes.isNotEmpty()) fileTypes.joinToString(", ") else "Unknown"
        val parentPath = parentDirectories.joinToString(", ")


        binding.apply {
            fileInfoFirstFileName.text = fileNames[0]
            fileinfoName.text = if (files.size == 1) fileNames[0] else fileNames.joinToString(", ")
            fileinfoSize.text = readableSize
            fileinfoType.text = fileTypeString
            fileinfoFileLocation.text = parentPath
            fileinfoLastModified.text = lastModifiedDate
            fileinfoCount.text = if (files.size == 1 && files[0].isDirectory)
                "Contains: $totalFiles files, $totalFolders folders"
            else
                "Total Files: $totalFiles, Total Folders: $totalFolders"


        showThumbnail(firstFile)
        }
    }

    private fun showThumbnail(file: File) {
        when {
            isImage(file) -> {
                Glide.with(this)
                    .load(file)
                    .into(binding.fileInfoImageView)
            }

            isVideo(file) -> {
                val thumbnail: Bitmap? = ThumbnailUtils.createVideoThumbnail(
                    file.absolutePath,
                    MediaStore.Images.Thumbnails.MINI_KIND
                )
                if (thumbnail != null) {
                    binding.fileInfoImageView.setImageBitmap(thumbnail)
                } else {
                    binding.fileInfoImageView.setImageResource(R.drawable.img)
                }
            }

            isPdf(file) -> {
                binding.fileInfoImageView.setImageResource(R.drawable.img)
            }

            else -> {
                binding.fileInfoImageView.setImageResource(R.drawable.img)
            }
        }
    }

    private fun isImage(file: File): Boolean {
        val imageExtensions = listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
        return imageExtensions.contains(file.extension.lowercase())
    }

    private fun isVideo(file: File): Boolean {
        val videoExtensions = listOf("mp4", "mkv", "avi", "mov", "flv", "wmv")
        return videoExtensions.contains(file.extension.lowercase())
    }

    private fun isPdf(file: File): Boolean {
        return file.extension.lowercase() == "pdf"
    }

    private fun getDirectorySize(directory: File): Long {
        var size: Long = 0
        directory.listFiles()?.forEach { file ->
            size += if (file.isDirectory) getDirectorySize(file) else file.length()
        }
        return size
    }

    private fun countFilesInDirectory(directory: File): Int {
        var count = 0
        directory.listFiles()?.forEach { file ->
            count += if (file.isDirectory) countFilesInDirectory(file) else 1
        }
        return count
    }

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.2f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}