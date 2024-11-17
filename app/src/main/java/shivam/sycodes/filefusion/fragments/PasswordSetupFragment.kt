package shivam.sycodes.filefusion.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.itsxtt.patternlock.PatternLockView
import shivam.sycodes.filefusion.R
import shivam.sycodes.filefusion.databinding.FragmentPasswordSetupFragementBinding
import shivam.sycodes.filefusion.utility.PreferencesHelper


class PasswordSetupFragment : Fragment() {

    private var _binding: FragmentPasswordSetupFragementBinding?= null
    private val binding get() = _binding!!

    private lateinit var preferencesHelper : PreferencesHelper

    private var firstPattern: String? = null
    private var isConfirmingPattern = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferencesHelper = PreferencesHelper(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPasswordSetupFragementBinding.inflate(inflater,container,false)

        binding.patternLockView.setOnPatternListener(object : PatternLockView.OnPatternListener {

            override fun onComplete(ids: ArrayList<Int>): Boolean {
                val enteredPattern = ids.joinToString(",")
                if (!isConfirmingPattern){
                    firstPattern = enteredPattern
                    isConfirmingPattern = true
                    Toast.makeText(requireContext(),"Confirm your pattern",Toast.LENGTH_SHORT).show()
                    return true
                }else{
                    if (enteredPattern == firstPattern) {
                        preferencesHelper.savePassword(enteredPattern)
                        Toast.makeText(context, "Pattern set successfully!", Toast.LENGTH_SHORT).show()
                        fragmentManager!!.beginTransaction().replace(R.id.fragmentContainerView,VaultFragment()).commit()
                    } else {
                        Toast.makeText(context, "Patterns do not match. Try again.", Toast.LENGTH_SHORT).show()
                        isConfirmingPattern = false
                        firstPattern = null
                    }
                }

                return true
            }
        })
        return binding.root
    }
}