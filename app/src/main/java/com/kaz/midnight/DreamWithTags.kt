package com.kaz.midnight

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class DreamWithTags(
    @Embedded val dream: Dream,
    @Relation(
        parentColumn = "id",       // dream table id
        entityColumn = "id",       // tag table id
        associateBy = Junction(
            value = DreamTagCrossRef::class,
            parentColumn = "dreamId",
            entityColumn = "tagId"
        )
    )
    val tags: List<Tag>
)
