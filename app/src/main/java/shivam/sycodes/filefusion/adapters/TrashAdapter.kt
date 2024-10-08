package shivam.sycodes.filefusion.adapters

import android.content.Context
import android.graphics.Bitmap
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
import shivam.sycodes.filefusion.R
import java.io.File

class TrashAdapter(val context: Context, var files: Array<File>, private var onItemClick: (File)-> Unit): RecyclerView.Adapter<TrashAdapter.TrashViewHolder>(){
    inner class TrashViewHolder(view : View):RecyclerView.ViewHolder(view){
        var fileName = view.findViewById<TextView>(R.id.directory_name)!!
        var fileImage = view.findViewById<ImageView>(R.id.directory_image)!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrashViewHolder {
        val inflater = LayoutInflater.from(parent.context).inflate(R.layout.gridf_flile_layout,parent,false)
        return TrashViewHolder(inflater)
    }

    override fun getItemCount(): Int {
        return files.size
    }

    override fun onBindViewHolder(holder: TrashViewHolder, position: Int) {
        holder.fileName.text = files[position].name
        if (files[position].isDirectory){
            holder.fileImage.setImageResource(R.drawable.folder)
        }else{
            val extension = files[position].extension.lowercase()
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            holder.fileImage.apply {
                when {
                    mimeType != null && mimeType.startsWith("image") -> {
                        holder.fileImage.let {
                            Glide.with(it.context)
                                .load(files[position])
                                .placeholder(R.drawable.imagefile)
                                .into(it)
                        }
                    }

                    mimeType != null && mimeType.startsWith("video") -> {
                        val videoThumbnail: Bitmap? = ThumbnailUtils.createVideoThumbnail(
                            files[position].path, MediaStore.Images.Thumbnails.MINI_KIND
                        )
                        if (videoThumbnail != null) {
                            holder.fileImage.setImageBitmap(videoThumbnail)
                        } else {
                            holder.fileImage.setImageResource(R.drawable.videofile)
                        }
                    }

                    extension == "pdf" -> holder.fileImage.setImageResource(R.drawable.pdf)
                    extension == "doc" -> holder.fileImage.setImageResource(R.drawable.doc)
                    extension == "ppt" -> holder.fileImage.setImageResource(R.drawable.ppt)
                    extension == "xls" -> holder.fileImage.setImageResource(R.drawable.xls)
                    extension == "txt" -> holder.fileImage.setImageResource(R.drawable.txt)
                    extension == "psd" -> holder.fileImage.setImageResource(R.drawable.psd)
                    extension == "iso" -> holder.fileImage.setImageResource(R.drawable.iso)
                    extension == "apk" -> holder.fileImage.setImageResource(R.drawable.apk)
                    extension == "docx" -> holder.fileImage.setImageResource(R.drawable.docx)
                    extension in arrayOf(
                        "zip",
                        "rar",
                        "7z",
                        "tar",
                        "gz"
                    ) -> holder.fileImage.setImageResource(R.drawable.zip)

                    extension in arrayOf(
                        "mp3",
                        "aac",
                        "m4a",
                        "wav",
                        "flac",
                        "amr"
                    ) -> holder.fileImage.setImageResource(R.drawable.audio)

                    else -> holder.fileImage.setImageResource(R.drawable.document)
                }
            }
        }
    }
}