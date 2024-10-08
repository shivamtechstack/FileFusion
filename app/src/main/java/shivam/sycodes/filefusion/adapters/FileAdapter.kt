package shivam.sycodes.filefusion.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.provider.MediaStore
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import shivam.sycodes.filefusion.R
import shivam.sycodes.filefusion.utility.PreferencesHelper
import java.io.File
import java.text.DecimalFormat
import java.util.Date
import kotlin.math.log10
import kotlin.math.pow

class FileAdapter(val context: Context,private var files : List<File>,
    private var onItemClick : (File) -> Unit,
    private val onItemLongClick : (File) -> Unit) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {
        //this is main adapter

        private val preferencesHelper :PreferencesHelper = PreferencesHelper(context)

        private val selectedFiles = mutableSetOf<File>()
        var isMultiSelectedMode = false

        inner class FileViewHolder(view: View): RecyclerView.ViewHolder(view){
            private val filename : TextView? = view.findViewById(R.id.directory_name)
            private val fileImage : ImageView? =view.findViewById(R.id.directory_image)
            private val checkBox : CheckBox?= view.findViewById(R.id.checkbox)
            private val fileSize :TextView? =view.findViewById(R.id.filesize)
            private val fileDate : TextView?=view.findViewById(R.id.fileDate)

            init {
                view.setOnClickListener {
                    val selectedFile = files[adapterPosition]
                    if (isMultiSelectedMode){
                        toggleSelection(selectedFile)
                    }else{
                        onItemClick(selectedFile)
                    }
                }
                view.setOnLongClickListener {
                    val selectedFile = files[adapterPosition]
                    if(!isMultiSelectedMode){
                        isMultiSelectedMode = true
                        toggleSelection(selectedFile)
                    }
                    onItemLongClick(selectedFile)
                    true
                }
            }
            @SuppressLint("SetTextI18n")
            fun bind(file: File) {
                filename?.text = file.name
                if (file.isDirectory) {
                    fileImage?.setImageResource(R.drawable.folder)
                    val itemCount = file.listFiles()?.size ?: 0
                    fileSize?.text = "$itemCount items"
                } else {
                    val extension = file.extension.lowercase()
                    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                    when {
                        mimeType != null && mimeType.startsWith("image") -> {
                            fileImage?.let {
                                Glide.with(it.context)
                                    .load(file)
                                    .placeholder(R.drawable.imagefile)
                                    .into(it)
                            }
                        }
                        mimeType != null && mimeType.startsWith("video") -> {
                            val videoThumbnail: Bitmap? = ThumbnailUtils.createVideoThumbnail(
                                file.path, MediaStore.Images.Thumbnails.MINI_KIND
                            )
                            if (videoThumbnail != null) {
                                fileImage?.setImageBitmap(videoThumbnail)
                            } else {
                                fileImage?.setImageResource(R.drawable.videofile)
                            }
                        }
                        extension == "pdf" -> fileImage?.setImageResource(R.drawable.pdf)
                        extension == "doc" -> fileImage?.setImageResource(R.drawable.doc)
                        extension == "ppt" -> fileImage?.setImageResource(R.drawable.ppt)
                        extension == "xls" -> fileImage?.setImageResource(R.drawable.xls)
                        extension == "txt" -> fileImage?.setImageResource(R.drawable.txt)
                        extension == "psd" -> fileImage?.setImageResource(R.drawable.psd)
                        extension == "iso" -> fileImage?.setImageResource(R.drawable.iso)
                        extension == "apk" -> fileImage?.setImageResource(R.drawable.apk)
                        extension == "docx" -> fileImage?.setImageResource(R.drawable.docx)
                        extension in arrayOf("zip", "rar", "7z", "tar", "gz") -> fileImage?.setImageResource(R.drawable.zip)
                        extension in arrayOf("mp3", "aac", "m4a", "wav", "flac", "amr") -> fileImage?.setImageResource(R.drawable.audio)
                        else -> fileImage?.setImageResource(R.drawable.document)
                    }
                    val filesize = formatFileSize(file.length())
                    val lastModified = formatDate(file.lastModified())
                    fileDate?.visibility = View.VISIBLE
                    fileSize?.text = filesize
                    fileDate?.text = lastModified
                }
                checkBox?.isChecked = selectedFiles.contains(file)
                checkBox?.visibility = if (isMultiSelectedMode) View.VISIBLE else View.GONE
                if (isMultiSelectedMode){
                    fileSize?.visibility = View.GONE
                    fileDate?.visibility= View.GONE
                }
            }
        }
    @SuppressLint("NotifyDataSetChanged")
    private fun toggleSelection(file: File){
        if (selectedFiles.contains(file)){
            selectedFiles.remove(file)
        }else{
            selectedFiles.add(file)
        }
        notifyItemChanged(files.indexOf(file))
    }
    fun getSelectedFiles() : List<File> = selectedFiles.toList()

    @SuppressLint("NotifyDataSetChanged")
    fun clearSelection(){
        selectedFiles.clear()
        isMultiSelectedMode = false
        notifyDataSetChanged()
    }
    @SuppressLint("NotifyDataSetChanged")
    fun selectAll() {
        selectedFiles.clear()
        selectedFiles.addAll(files)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val gridViewtrue=preferencesHelper.isGridView()
        if (gridViewtrue){
            val gridinflator = LayoutInflater.from(parent.context).inflate(R.layout.gridf_flile_layout,parent,false)
            return FileViewHolder(gridinflator)
        }else {
            val inflater =
                LayoutInflater.from(parent.context).inflate(R.layout.file_layout, parent, false)
            return FileViewHolder(inflater)
        }
    }

    override fun getItemCount(): Int {
        return files.size
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(files[position])
    }

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
    }

    private fun formatDate(timestamp: Long): String {
        val date = Date(timestamp)
        return DateFormat.format("MMM dd, yyyy • hh:mm a", date).toString()
    }
}