package shivam.sycodes.filefusion.utility

import android.content.Context

class PreferencesHelper(context: Context) {
    private val sharedPreferencesforFileView = context.getSharedPreferences("FileViewPreferences",Context.MODE_PRIVATE)

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
}