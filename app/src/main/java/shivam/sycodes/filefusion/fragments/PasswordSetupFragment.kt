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
    private var action:String? = null

    companion object {
        private const val ARG_ACTION = "action"
        @JvmStatic
        fun newInstance(action: String): PasswordSetupFragment {
            return PasswordSetupFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ACTION, action)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            action = it.getString(ARG_ACTION)

        }
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
                    binding.createpatterntext.text= "Confirm New Pattern"
                    return true
                }else{
                    if (enteredPattern == firstPattern) {
                        if (action == "change"){
                            preferencesHelper.savePassword(enteredPattern)
                            Toast.makeText(context, "Pattern changed successfully!", Toast.LENGTH_SHORT).show()
                            requireActivity().finish()
                        }else {
                            preferencesHelper.savePassword(enteredPattern)
                            Toast.makeText(context, "Pattern set successfully!", Toast.LENGTH_SHORT)
                                .show()
                            fragmentManager!!.beginTransaction()
                                .replace(R.id.fragmentContainerView, VaultFragment()).commit()
                        }
                    } else {
                       binding.patternConfirmationText.text = "Patterns do not match. Try again."
                        isConfirmingPattern = false
                        firstPattern = null
                    }
                }

                return true
            }
        })
        binding.setuppasswordback.setOnClickListener {
            super.getActivity()?.onBackPressed()
        }
        return binding.root
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}