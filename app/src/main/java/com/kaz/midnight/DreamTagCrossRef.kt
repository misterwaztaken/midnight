package com.kaz.midnight

import androidx.room.Entity
import androidx.room.Index

@Entity(primaryKeys = ["dreamId", "tagId"], indices = [Index("tagId")])
data class DreamTagCrossRef(
    val dreamId: Int,
    val tagId: Int
)
