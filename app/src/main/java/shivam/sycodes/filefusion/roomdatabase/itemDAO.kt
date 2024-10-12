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
}