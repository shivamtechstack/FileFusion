package shivam.sycodes.filefusion.roomdatabase

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items_Entity")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id : Long = 0,
    val trashFileName : String,
    val trashFileOriginalPath :  String,
    val trashFileDeletionDate : String
)

@Entity(tableName = "bookmark_entities")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id : Long = 0,
    val bookmarkFilePath : String,
    val bookmarkFileName :  String
)
