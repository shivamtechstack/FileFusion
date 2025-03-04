package shivam.sycodes.filefusion.adapters

import android.content.Context
import android.media.ThumbnailUtils
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import shivam.sycodes.filefusion.R
import java.io.File

class RecentAdapter(var requireContext: Context, private var recentFiles: List<File>, private var onItemClick : (File) -> Unit) : RecyclerView.Adapter<RecentAdapter.RecentViewHolder>() {
    class RecentViewHolder(view: View):RecyclerView.ViewHolder(view){
        var fileName = view.findViewById<TextView>(R.id.recentFileName)!!
        var fileImage = view.findViewById<ImageView>(R.id.recentImages)!!

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentViewHolder {
        val inflate = LayoutInflater.from(parent.context).inflate(R.layout.recentrecyclerlayout,parent,false)
        return RecentViewHolder(inflate)
    }

    override fun getItemCount(): Int {
        return recentFiles.size
    }

    override fun onBindViewHolder(holder: RecentViewHolder, position: Int) {
        val file = recentFiles[position]

        holder.fileName.text = file.name

        val extension = file.extension.lowercase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)

        when {
            mimeType != null && mimeType.startsWith("image") -> {
                Glide.with(holder.fileImage.context)
                    .load(file)
                    .centerCrop()
                    .placeholder(R.drawable.imagefile)
                    .into(holder.fileImage)
            }

            mimeType != null && mimeType.startsWith("video") -> {
                    Glide.with(holder.fileImage.context)
                        .load(R.drawable.videofile)
                        .circleCrop()
                        .into(holder.fileImage)
                CoroutineScope(Dispatchers.Main).launch {
                    val videoThumbnail = withContext(Dispatchers.IO) {
                        ThumbnailUtils.createVideoThumbnail(
                            file.path, MediaStore.Images.Thumbnails.MINI_KIND
                        )
                    }
                    if (videoThumbnail != null) {
                        Glide.with(holder.fileImage.context)
                            .load(videoThumbnail)
                            .centerCrop()
                            .placeholder(R.drawable.videofile)
                            .into(holder.fileImage)
                    } else {
                        holder.fileImage.setImageResource(R.drawable.videofile)
                    }
                }
            }

            extension == "pdf" -> holder.fileImage.setImageResource(R.drawable.pdf)
            extension == "doc" -> holder.fileImage.setImageResource(R.drawable.doc)
            extension == "vcf" -> holder.fileImage.setImageResource(R.drawable.vcffile)
            extension == "pptx" -> holder.fileImage.setImageResource(R.drawable.pptxfile)
            extension == "ppt" -> holder.fileImage.setImageResource(R.drawable.ppt)
            extension == "xls" -> holder.fileImage.setImageResource(R.drawable.xls)
            extension == "txt" -> holder.fileImage.setImageResource(R.drawable.txt)
            extension == "psd" -> holder.fileImage.setImageResource(R.drawable.psd)
            extension == "iso" -> holder.fileImage.setImageResource(R.drawable.iso)
            extension == "apk" -> holder.fileImage.setImageResource(R.drawable.apk)
            extension == "docx" -> holder.fileImage.setImageResource(R.drawable.docx)
            extension in arrayOf("zip", "rar", "7z", "tar", "gz") -> holder.fileImage.setImageResource(R.drawable.zip)
            extension in arrayOf("mp3", "aac", "m4a", "wav", "flac", "amr") -> holder.fileImage.setImageResource(R.drawable.audio)
            else -> holder.fileImage.setImageResource(R.drawable.imagefile)
        }

        holder.itemView.setOnClickListener {
            onItemClick(file)
        }
    }
}