package shivam.sycodes.filefusion.viewModel

import androidx.lifecycle.ViewModel
import java.io.File

class FileOperationViewModel:ViewModel() {
    var filesToCopyorCut : List<File>? = null
    var isCutOperation : Boolean = false
}