package com.kaz.midnight

import androidx.room.*

@Dao
interface DreamDao {

    // dream stuff
    @Query("SELECT * FROM dreams ORDER BY id DESC")
    fun getAll(): List<Dream>

    @Query("SELECT * FROM dreams WHERE id = :id")
    fun getById(id: Int): Dream?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(dream: Dream): Long

    @Update
    fun update(dream: Dream)

    @Delete
    fun delete(dream: Dream)

    @Query("DELETE FROM dreams WHERE id = :id")
    fun deleteById(id: Int)

    // search and filter
    @Query("""
        SELECT * FROM dreams 
        WHERE content LIKE :query 
        AND (:favOnly = 0 OR isFavorite = 1) 
        ORDER BY id DESC
    """)
    fun getFilteredDreams(query: String, favOnly: Int): List<Dream>

    // tags
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertTag(tag: Tag): Long

    @Update
    fun updateTag(tag: Tag)

    @Delete
    fun deleteTag(tag: Tag)

    @Query("SELECT * FROM tags")
    fun getAllTags(): List<Tag>

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    fun getTagByName(name: String): Tag?

    // linking dreams and tags
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDreamTagCrossRef(crossRef: DreamTagCrossRef)

    @Query("DELETE FROM DreamTagCrossRef WHERE dreamId = :dreamId")
    fun deleteTagsForDream(dreamId: Int)

    @Transaction
    @Query("SELECT * FROM tags JOIN DreamTagCrossRef ON tags.id = DreamTagCrossRef.tagId WHERE dreamId = :dreamId")
    fun getTagsForDream(dreamId: Int): List<Tag>

    @Transaction
    @Query("SELECT * FROM dreams WHERE id = :dreamId")
    fun getDreamWithTags(dreamId: Int): DreamWithTags

    @Transaction
    @Query("""
        SELECT * FROM dreams 
        INNER JOIN DreamTagCrossRef ON dreams.id = DreamTagCrossRef.dreamId 
        WHERE DreamTagCrossRef.tagId IN (:tagIds)
    """)
    fun getDreamsByTags(tagIds: List<Int>): List<Dream>

    @Transaction
    @Query("""
    SELECT DISTINCT dreams.* FROM dreams 
    INNER JOIN DreamTagCrossRef ON dreams.id = DreamTagCrossRef.dreamId 
    INNER JOIN tags ON DreamTagCrossRef.tagId = tags.id
    WHERE (tags.name IN (:tagNames) OR :ignoreTags = 1)
    AND (:favOnly = 0 OR isFavorite = 1)
""")
    fun getFilteredDreams(tagNames: List<String>, ignoreTags: Int, favOnly: Int): List<Dream>

    @Query("SELECT * FROM dreams")
    fun getAllSync(): List<Dream>

    @Query("SELECT * FROM DreamTagCrossRef")
    fun getAllCrossRefs(): List<DreamTagCrossRef>

    @Query("SELECT * FROM dreams WHERE content = :content AND creationDate = :date LIMIT 1")
    fun findDuplicate(content: String, date: String): Dream?
}
