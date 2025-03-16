package shivam.sycodes.filefusion

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import shivam.sycodes.filefusion.databinding.ActivityAppSettingsBinding
import shivam.sycodes.filefusion.fragments.PasswordAuthentication
import shivam.sycodes.filefusion.utility.AboutUs
import shivam.sycodes.filefusion.utility.PrivacyPolicy

class AppSettings : AppCompatActivity() {
    private lateinit var binding: ActivityAppSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAppSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                binding.fragmentContainerView1.visibility = View.GONE
            }
        }
        binding.vaultPasswordChangeButton.setOnClickListener {
            binding.fragmentContainerView1.visibility = View.VISIBLE
            val fragment = PasswordAuthentication.newInstance("change")
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView1, fragment)
                .addToBackStack(null)
                .commit()
        }
        binding.privacyPolicyButton.setOnClickListener {
            PrivacyPolicy(this).showPolicyDialog()
        }
        binding.aboutButton.setOnClickListener {
            AboutUs(this).showAboutUsDialog()
        }
    }

    override fun onResume() {
        binding.fragmentContainerView1.visibility = View.GONE
        super.onResume()
    }
}