package com.nyaa.aniyaa.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY addedAt DESC")
    fun observe(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY addedAt DESC")
    suspend fun getAll(): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks WHERE identity = :identity LIMIT 1")
    suspend fun getByIdentity(identity: String): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BookmarkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<BookmarkEntity>)

    @Query(
        "DELETE FROM bookmarks WHERE identity = :identity OR " +
            "(site = :site AND (torrentId = :identity OR infoHash = :identity))"
    )
    suspend fun delete(identity: String, site: String)

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAll()
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC")
    fun observe(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC")
    suspend fun getAll(): List<HistoryEntity>

    @Insert
    suspend fun insert(entity: HistoryEntity): Long

    @Query("DELETE FROM search_history WHERE site = :site AND LOWER(query) = LOWER(:query)")
    suspend fun deleteByQuery(site: String, query: String)

    @Query(
        "DELETE FROM search_history WHERE site = :site AND id NOT IN " +
            "(SELECT id FROM (SELECT id FROM search_history WHERE site = :site ORDER BY timestamp DESC LIMIT :keep))"
    )
    suspend fun trim(site: String, keep: Int)

    @Query("DELETE FROM search_history")
    suspend fun deleteAll()
}

@Dao
interface SavedSearchDao {
    @Query("SELECT * FROM saved_searches ORDER BY id DESC")
    fun observe(): Flow<List<SavedSearchEntity>>

    @Query("SELECT * FROM saved_searches ORDER BY id DESC")
    suspend fun getAll(): List<SavedSearchEntity>

    @Query("SELECT * FROM saved_searches WHERE notify = 1")
    suspend fun getNotifying(): List<SavedSearchEntity>

    @Query("SELECT * FROM saved_searches WHERE id = :id")
    suspend fun getById(id: Long): SavedSearchEntity?

    @Insert
    suspend fun insert(entity: SavedSearchEntity): Long

    @Update
    suspend fun update(entity: SavedSearchEntity)

    @Query("DELETE FROM saved_searches WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM saved_searches")
    suspend fun deleteAll()
}

@Dao
interface SentListingDao {
    @Query("SELECT identity FROM sent_listings")
    fun observe(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SentListingEntity)

    @Query("DELETE FROM sent_listings WHERE identity = :identity")
    suspend fun delete(identity: String)

    @Query("DELETE FROM sent_listings")
    suspend fun deleteAll()

    @Query(
        "DELETE FROM sent_listings WHERE identity NOT IN " +
            "(SELECT identity FROM sent_listings ORDER BY sentAt DESC LIMIT :keep)"
    )
    suspend fun trim(keep: Int)
}

@Dao
interface ViewedListingDao {
    @Query("SELECT * FROM viewed_listings ORDER BY viewedAt DESC")
    fun observe(): Flow<List<ViewedListingEntity>>

    @Query("SELECT * FROM viewed_listings ORDER BY viewedAt DESC")
    suspend fun getAll(): List<ViewedListingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ViewedListingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ViewedListingEntity>)

    @Query("DELETE FROM viewed_listings WHERE identity = :identity")
    suspend fun delete(identity: String)

    @Query(
        "DELETE FROM viewed_listings WHERE identity NOT IN " +
            "(SELECT identity FROM (SELECT identity FROM viewed_listings ORDER BY viewedAt DESC LIMIT :keep))"
    )
    suspend fun trim(keep: Int)

    @Query("DELETE FROM viewed_listings")
    suspend fun deleteAll()
}
