package shivam.sycodes.filefusion.roomdatabase

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface itemDAO {
    @Insert
    suspend fun insertTrashItem(trashItem: ItemEntity)

    @Query("SELECT * FROM items_Entity WHERE trashFileName = :trashFileName LIMIT 1")
    suspend fun getTrashItemByFileName(trashFileName : String):ItemEntity

    @Query("SELECT * FROM items_Entity WHERE trashFileName= :trashFileName")
    suspend fun deleteTrashItem(trashFileName: String):ItemEntity

    @Insert
    suspend fun addBookmark(bookmark: BookmarkEntity)

    @Query("SELECT * FROM bookmark_entities")
    suspend fun getAllBookmarks(): List<BookmarkEntity>

    @Query("DELETE FROM bookmark_entities WHERE bookmarkFilePath = :filePath")
    suspend fun removeBookmark(filePath: String)


}