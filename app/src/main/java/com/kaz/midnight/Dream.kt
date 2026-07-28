package com.kaz.midnight

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore

@Entity(tableName = "dreams")
data class Dream(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,
    val creationDate: String,
    val lastModified: String,
    val isFavorite: Boolean = false
) {
    // adapter can see these but room wont try to save them
    @Ignore
    var tags: List<Tag> = emptyList()
}
