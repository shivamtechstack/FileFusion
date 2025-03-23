package shivam.sycodes.filefusion

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import shivam.sycodes.filefusion.fragments.HomeScreenFragment
import shivam.sycodes.filefusion.utility.PreferencesHelper

class MainActivity : AppCompatActivity() {

    private lateinit var preferencesHelper: PreferencesHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        preferencesHelper = PreferencesHelper(this)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val savedTheme = preferencesHelper.getThemePreferences()
        AppCompatDelegate.setDefaultNightMode(savedTheme)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView,HomeScreenFragment())
            .commit()
    }
}