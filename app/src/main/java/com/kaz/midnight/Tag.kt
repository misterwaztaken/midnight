package com.kaz.midnight

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val colorHex: String = DEFAULT_HEX
) {
    companion object {
        const val DEFAULT_HEX = "#BB86FC"
        const val AUTO_HEX = "#533483"
    }
}


