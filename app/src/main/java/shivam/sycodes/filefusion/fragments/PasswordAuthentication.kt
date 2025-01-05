package shivam.sycodes.filefusion.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.itsxtt.patternlock.PatternLockView
import shivam.sycodes.filefusion.R
import shivam.sycodes.filefusion.databinding.FragmentPasswordAuthenticationBinding
import shivam.sycodes.filefusion.utility.PreferencesHelper
import shivam.sycodes.filefusion.viewModel.PasswordAuthCallBack
import java.util.ArrayList

class PasswordAuthentication : Fragment() {
    private var _binding : FragmentPasswordAuthenticationBinding ? = null
    private val binding get() = _binding!!
    private lateinit var preferencesHelper : PreferencesHelper
    private var action :String? = null
    private var callback: PasswordAuthCallBack? = null

    companion object {
        private const val ARG_ACTION = "action"

        @JvmStatic
        fun newInstance(action: String): PasswordAuthentication {
            return PasswordAuthentication().apply {
                arguments = Bundle().apply {
                    putString(ARG_ACTION, action)
                }
            }
        }
    }

    fun setCallback(callback: PasswordAuthCallBack) {
        this.callback = callback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferencesHelper = PreferencesHelper(requireContext())
        arguments?.let {
            action = it.getString(ARG_ACTION)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPasswordAuthenticationBinding.inflate(inflater,container,false)
        if (action == "change"){
            binding.drawpatterntext.text = "Draw Old Pattern"
        }

        binding.authenticatePassword.setOnPatternListener(object : PatternLockView.OnPatternListener{
            override fun onComplete(ids: ArrayList<Int>): Boolean {
                val enteredpassword = ids.joinToString(",")
                val savedPassword = preferencesHelper.getPassword()

                if (savedPassword == enteredpassword){
                    if (action == "change"){
                        val fragment=PasswordSetupFragment.newInstance("change")
                        fragmentManager!!.beginTransaction().replace(R.id.fragmentContainerView1,fragment).commit()
                    }else if (action == "moveFile"){
                        callback?.onAuthenticationSuccess()
                        parentFragmentManager.popBackStack()
                    }
                    else {
                        val fragment = VaultFragment()
                        clearBackStack()
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainerView, fragment)
                            .addToBackStack(null)
                            .commit()
                    }
                }else{
                    binding.wrongpatterntext.text = "Wrong Pattern, Try Again"
                   return true
                }

             return true
            }
        })
        binding.authpasswordback.setOnClickListener {
            super.getActivity()?.onBackPressed()
        }

        return binding.root
    }
    fun clearBackStack() {
        val backStackCount = parentFragmentManager.backStackEntryCount
        for (i in 0 until backStackCount) {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}