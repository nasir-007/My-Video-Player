package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_history")
data class VideoHistoryEntity(
    @PrimaryKey val uriString: String,
    val title: String,
    val folderUri: String? = null,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val lastPositionMs: Long = 0L,
    val lastPlayedAt: Long = System.currentTimeMillis(),
    val decoderMode: String = "HW_PLUS",
    val externalSubtitleUri: String? = null,
    val subtitleDelayMs: Long = 0L,
    val aspectMode: String = "FIT"
)

@Entity(tableName = "pinned_folders")
data class PinnedFolderEntity(
    @PrimaryKey val treeUriString: String,
    val displayName: String,
    val addedAt: Long = System.currentTimeMillis()
)
