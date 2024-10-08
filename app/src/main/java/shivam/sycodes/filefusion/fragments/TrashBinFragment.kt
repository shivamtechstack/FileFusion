package shivam.sycodes.filefusion.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import shivam.sycodes.filefusion.adapters.TrashAdapter
import shivam.sycodes.filefusion.databinding.FragmentTrashBinBinding
import shivam.sycodes.filefusion.utility.FileOperationHelper
import java.io.File

class TrashBinFragment : Fragment() {

    private var _binding : FragmentTrashBinBinding?= null
    private val binding get() = _binding
    private lateinit var fileOperationHelper : FileOperationHelper
   private lateinit var trashPath : String
   private lateinit var trashAdapter: TrashAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrashBinBinding.inflate(inflater,container,false)
        fileOperationHelper = FileOperationHelper(requireContext())
        trashPath= fileOperationHelper.getTrashDir().toString()
        loadFiles(trashPath)
        return binding!!.root
    }
    private fun loadFiles(trashPath : String){
        trashPath.let {
            val directory = File(trashPath)
            if (directory.isDirectory && directory.exists()){
                val files : Array<File> = directory.listFiles()?: arrayOf()
                files.sortedByDescending { it.lastModified() }
                if (files.isNotEmpty()){
                trashAdapter = TrashAdapter(requireContext(),files, onItemClick ={ selectedFile ->
                }
                )
                    binding?.trashBinRecyclerView?.layoutManager = GridLayoutManager(requireContext(),3)
                    binding?.trashBinRecyclerView?.adapter = trashAdapter
                }else{
                    binding?.trashBinRecyclerView?.adapter = null
                    showToast("Trash Directory is empty")
                }

            }
        }
    }
    private fun showToast(message : String){
        Toast.makeText(requireContext(),message,Toast.LENGTH_SHORT).show()
    }
}