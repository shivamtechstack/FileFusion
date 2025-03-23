package shivam.sycodes.filefusion.utility

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

class PreferencesHelper(context: Context) {
    private val sharedPreferencesforFileView = context.getSharedPreferences("FileViewPreferences",Context.MODE_PRIVATE)
    private val sharedPreferencesForPassword = context.getSharedPreferences("PasswordPreferences",Context.MODE_PRIVATE)
    private val themeSharedPreferences = context.getSharedPreferences("themeSharedPreferences", Context.MODE_PRIVATE )

    fun saveSortOptions(sortBy : String, isAscending : Boolean){
        sharedPreferencesforFileView.edit().apply{
            putString("sortBy", sortBy)
            putBoolean("isAscending", isAscending)
            apply()
        }
    }

    fun getSortOptions(): Pair<String?, Boolean> {
        val sortBy = sharedPreferencesforFileView.getString("sortBy", "name")
        val isAscending = sharedPreferencesforFileView.getBoolean("isAscending", true)
        return Pair(sortBy, isAscending)
    }

    fun saveViewMode(isGridView: Boolean){
        sharedPreferencesforFileView.edit().apply {
            putBoolean("isGridView",isGridView)
            apply()
        }
    }

    fun isGridView():Boolean{
        return sharedPreferencesforFileView.getBoolean("isGridView", false)
    }

    fun hiddenFiles(isHiddenchecked:Boolean){
        sharedPreferencesforFileView.edit().apply {
            putBoolean("isFilesHidden",isHiddenchecked)
            apply()
        }
    }
    fun isFilesHidden(): Boolean{
        return sharedPreferencesforFileView.getBoolean("isFilesHidden",false)
    }

    fun savePassword(password: String){
        sharedPreferencesForPassword.edit().apply {
            putString("password",password)
            apply()
        }
    }
    fun getPassword(): String? {
        return sharedPreferencesForPassword.getString("password", null)
    }

    fun saveThemePreferences(themeMode: Int) {
        themeSharedPreferences.edit().apply {
            putInt("theme_mode", themeMode)
            apply()
        }
    }

    fun getThemePreferences(): Int {
        return themeSharedPreferences.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

}