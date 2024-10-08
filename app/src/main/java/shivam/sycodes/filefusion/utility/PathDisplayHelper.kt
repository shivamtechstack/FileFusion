package shivam.sycodes.filefusion.utility

import android.widget.LinearLayout
import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.TextView
import android.view.LayoutInflater
import android.widget.Toast
import androidx.core.content.ContextCompat
import shivam.sycodes.filefusion.R

class PathDisplayHelper(private val context: Context) {
    fun setupPathLayout(pathLayout: LinearLayout, currentPath: String?) {
        pathLayout.removeAllViews()

        if (currentPath != null) {
            val pathSegments = currentPath.split("/")

            pathSegments.forEachIndexed { index, segment ->
                val textView = LayoutInflater.from(pathLayout.context).inflate(R.layout.path_item, pathLayout, false) as TextView
                textView.text = segment

                val fullPath = pathSegments.take(index + 1).joinToString("/")

                textView.setOnClickListener {
                    copyPathToClipboard(fullPath)
                }

                pathLayout.addView(textView)

                if (index != pathSegments.size - 1) {
                    val separatorView = TextView(pathLayout.context).apply {
                        text = " > "
                        setPadding(8, 0, 8, 0)
                        textSize = 16f
                        setTextColor(ContextCompat.getColor(pathLayout.context, android.R.color.darker_gray))
                    }
                    pathLayout.addView(separatorView)
                }
            }
        }
    }

    private fun copyPathToClipboard(fullPath: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("file_path", fullPath)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(context, "Path copied to clipboard: $fullPath", Toast.LENGTH_SHORT).show()
    }
}