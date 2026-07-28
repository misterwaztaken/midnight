package com.kaz.midnight

import androidx.room.Entity

@Entity(primaryKeys = ["dreamId", "tagId"])
data class DreamTagCrossRef(
    val dreamId: Int,
    val tagId: Int
)
