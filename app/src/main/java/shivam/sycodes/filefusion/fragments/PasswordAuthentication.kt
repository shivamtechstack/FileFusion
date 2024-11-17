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
import java.util.ArrayList

class PasswordAuthentication : Fragment() {
    private var _binding : FragmentPasswordAuthenticationBinding ? = null
    private val binding get() = _binding!!
    private lateinit var preferencesHelper : PreferencesHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferencesHelper = PreferencesHelper(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPasswordAuthenticationBinding.inflate(inflater,container,false)

        binding.authenticatePassword.setOnPatternListener(object : PatternLockView.OnPatternListener{
            override fun onComplete(ids: ArrayList<Int>): Boolean {
                val enteredpassword = ids.joinToString(",")
                val savedPassword = preferencesHelper.getPassword()

                if (savedPassword == enteredpassword){
                    fragmentManager!!.beginTransaction().replace(R.id.fragmentContainerView,VaultFragment()).commit()
                }else{
                   return true
                }

             return true
            }
        })


        return binding.root
    }
}