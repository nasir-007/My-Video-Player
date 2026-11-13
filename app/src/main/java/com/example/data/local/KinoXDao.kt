package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KinoXDao {
    @Query("SELECT * FROM video_history ORDER BY lastPlayedAt DESC")
    fun observeVideoHistory(): Flow<List<VideoHistoryEntity>>

    @Query("SELECT * FROM video_history WHERE uriString = :uriString LIMIT 1")
    suspend fun getVideoHistory(uriString: String): VideoHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVideoHistory(entity: VideoHistoryEntity)

    @Query("DELETE FROM video_history WHERE uriString = :uriString")
    suspend fun deleteVideoHistory(uriString: String)

    @Query("DELETE FROM video_history")
    suspend fun clearAllVideoHistory()

    @Query("SELECT * FROM pinned_folders ORDER BY addedAt DESC")
    fun observePinnedFolders(): Flow<List<PinnedFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPinnedFolder(entity: PinnedFolderEntity)

    @Query("DELETE FROM pinned_folders WHERE treeUriString = :treeUriString")
    suspend fun deletePinnedFolder(treeUriString: String)
}
