package com.example.data.repository

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.example.data.local.KinoXDao
import com.example.data.local.PinnedFolderEntity
import com.example.data.local.PlayerPreferencesStore
import com.example.data.local.VideoHistoryEntity
import com.example.data.model.AspectRatioMode
import com.example.data.model.DecoderMode
import com.example.data.model.FolderItem
import com.example.data.model.PlaybackPrefs
import com.example.data.model.PowerProfile
import com.example.data.model.SubtitleStylePrefs
import com.example.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MediaRepository(
    private val context: Context,
    private val dao: KinoXDao,
    private val prefsStore: PlayerPreferencesStore
) {
    val playbackPrefs: Flow<PlaybackPrefs> = prefsStore.playbackPrefs

    val videoHistory: Flow<List<VideoItem>> = dao.observeVideoHistory().map { list ->
        list.map { it.toModel() }
    }

    val pinnedFolders: Flow<List<FolderItem>> = dao.observePinnedFolders().map { list ->
        list.map {
            FolderItem(
                treeUriString = it.treeUriString,
                displayName = it.displayName,
                addedAt = it.addedAt
            )
        }
    }

    suspend fun updateDefaultDecoder(mode: DecoderMode) = prefsStore.updateDefaultDecoder(mode)

    suspend fun updatePowerProfile(profile: PowerProfile) = prefsStore.updatePowerProfile(profile)

    suspend fun updateBackgroundPlayEnabled(enabled: Boolean) = prefsStore.updateBackgroundPlayEnabled(enabled)

    suspend fun updateAutoEcoOnBatterySaver(enabled: Boolean) = prefsStore.updateAutoEcoOnBatterySaver(enabled)

    suspend fun updateAutoResume(enabled: Boolean) = prefsStore.updateAutoResume(enabled)

    suspend fun updateSubtitleStyle(style: SubtitleStylePrefs) = prefsStore.updateSubtitleStyle(style)

    suspend fun getVideoHistoryByUri(uriString: String): VideoItem? =
        dao.getVideoHistory(uriString)?.toModel()

    suspend fun saveVideoProgress(
        uriString: String,
        title: String,
        folderUri: String?,
        durationMs: Long,
        lastPositionMs: Long,
        decoderMode: DecoderMode,
        externalSubtitleUri: String?,
        subtitleDelayMs: Long,
        aspectMode: AspectRatioMode,
        width: Int = 0,
        height: Int = 0,
        sizeBytes: Long = 0L
    ) {
        val existing = dao.getVideoHistory(uriString)
        dao.upsertVideoHistory(
            VideoHistoryEntity(
                uriString = uriString,
                title = title.ifBlank { existing?.title ?: "Video" },
                folderUri = folderUri ?: existing?.folderUri,
                durationMs = if (durationMs > 0L) durationMs else (existing?.durationMs ?: 0L),
                sizeBytes = if (sizeBytes > 0L) sizeBytes else (existing?.sizeBytes ?: 0L),
                width = if (width > 0) width else (existing?.width ?: 0),
                height = if (height > 0) height else (existing?.height ?: 0),
                lastPositionMs = lastPositionMs.coerceAtLeast(0L),
                lastPlayedAt = System.currentTimeMillis(),
                decoderMode = decoderMode.name,
                externalSubtitleUri = externalSubtitleUri ?: existing?.externalSubtitleUri,
                subtitleDelayMs = subtitleDelayMs,
                aspectMode = aspectMode.name
            )
        )
    }

    suspend fun removeHistoryItem(uriString: String) {
        dao.deleteVideoHistory(uriString)
    }

    suspend fun pinFolder(treeUri: Uri): FolderItem = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
        }
        val doc = DocumentFile.fromTreeUri(context, treeUri)
        val name = doc?.name ?: treeUri.lastPathSegment?.substringAfterLast(':') ?: "Video Folder"
        val entity = PinnedFolderEntity(
            treeUriString = treeUri.toString(),
            displayName = name
        )
        dao.upsertPinnedFolder(entity)
        val count = countVideosInDocumentTree(doc)
        FolderItem(
            treeUriString = entity.treeUriString,
            displayName = entity.displayName,
            videoCount = count,
            addedAt = entity.addedAt
        )
    }

    suspend fun unpinFolder(treeUriString: String) {
        dao.deletePinnedFolder(treeUriString)
    }

    suspend fun scanFolderVideos(treeUriString: String): List<VideoItem> = withContext(Dispatchers.IO) {
        val treeUri = Uri.parse(treeUriString)
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val files = collectFilesRecursively(rootDoc, maxDepth = 2)
        val subtitleFilesByBaseName = files
            .filter { file ->
                val name = file.name?.lowercase() ?: ""
                name.endsWith(".srt") || name.endsWith(".vtt")
            }
            .associateBy { file ->
                file.name?.substringBeforeLast('.')?.lowercase().orEmpty()
            }

        val existingMap = mutableMapOf<String, VideoHistoryEntity>()
        files.forEach { file ->
            val uriStr = file.uri.toString()
            dao.getVideoHistory(uriStr)?.let { existingMap[uriStr] = it }
        }

        files.filter { isVideoFile(it) }
            .map { doc ->
                val uriStr = doc.uri.toString()
                val title = doc.name ?: "Video"
                val baseName = title.substringBeforeLast('.').lowercase()
                val sidecarSub = subtitleFilesByBaseName[baseName]?.uri?.toString()
                val existing = existingMap[uriStr]
                val meta = if (existing == null || existing.durationMs <= 0L) {
                    probeLocalVideoMetadata(doc.uri)
                } else {
                    LocalVideoMeta(existing.durationMs, existing.width, existing.height)
                }
                VideoItem(
                    uriString = uriStr,
                    title = title,
                    folderUri = treeUriString,
                    durationMs = meta.durationMs,
                    sizeBytes = doc.length(),
                    width = meta.width,
                    height = meta.height,
                    lastPositionMs = existing?.lastPositionMs ?: 0L,
                    lastPlayedAt = existing?.lastPlayedAt ?: doc.lastModified(),
                    decoderMode = DecoderMode.fromName(existing?.decoderMode),
                    externalSubtitleUri = existing?.externalSubtitleUri ?: sidecarSub,
                    subtitleDelayMs = existing?.subtitleDelayMs ?: 0L,
                    aspectMode = AspectRatioMode.fromName(existing?.aspectMode)
                )
            }
            .sortedBy { it.title.lowercase() }
    }

    suspend fun countFolderVideos(treeUriString: String): Int = withContext(Dispatchers.IO) {
        val doc = DocumentFile.fromTreeUri(context, Uri.parse(treeUriString))
        countVideosInDocumentTree(doc)
    }

    suspend fun registerPickedVideo(uri: Uri): VideoItem = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
        }
        val uriString = uri.toString()
        val existing = dao.getVideoHistory(uriString)
        val displayName = queryDisplayName(context.contentResolver, uri)
            ?: existing?.title
            ?: uri.lastPathSegment?.substringAfterLast('/')
            ?: "Local Video"
        val sizeBytes = querySizeBytes(context.contentResolver, uri)
        val meta = probeLocalVideoMetadata(uri)

        val item = VideoItem(
            uriString = uriString,
            title = displayName,
            folderUri = existing?.folderUri,
            durationMs = if (meta.durationMs > 0L) meta.durationMs else (existing?.durationMs ?: 0L),
            sizeBytes = if (sizeBytes > 0L) sizeBytes else (existing?.sizeBytes ?: 0L),
            width = if (meta.width > 0) meta.width else (existing?.width ?: 0),
            height = if (meta.height > 0) meta.height else (existing?.height ?: 0),
            lastPositionMs = existing?.lastPositionMs ?: 0L,
            lastPlayedAt = System.currentTimeMillis(),
            decoderMode = DecoderMode.fromName(existing?.decoderMode),
            externalSubtitleUri = existing?.externalSubtitleUri,
            subtitleDelayMs = existing?.subtitleDelayMs ?: 0L,
            aspectMode = AspectRatioMode.fromName(existing?.aspectMode)
        )
        dao.upsertVideoHistory(
            VideoHistoryEntity(
                uriString = item.uriString,
                title = item.title,
                folderUri = item.folderUri,
                durationMs = item.durationMs,
                sizeBytes = item.sizeBytes,
                width = item.width,
                height = item.height,
                lastPositionMs = item.lastPositionMs,
                lastPlayedAt = item.lastPlayedAt,
                decoderMode = item.decoderMode.name,
                externalSubtitleUri = item.externalSubtitleUri,
                subtitleDelayMs = item.subtitleDelayMs,
                aspectMode = item.aspectMode.name
            )
        )
        item
    }

    private fun countVideosInDocumentTree(rootDoc: DocumentFile?): Int {
        if (rootDoc == null || !rootDoc.exists()) return 0
        return collectFilesRecursively(rootDoc, maxDepth = 2).count { isVideoFile(it) }
    }

    private fun collectFilesRecursively(dir: DocumentFile, maxDepth: Int, depth: Int = 0): List<DocumentFile> {
        if (depth > maxDepth) return emptyList()
        val result = mutableListOf<DocumentFile>()
        val children = runCatching { dir.listFiles() }.getOrDefault(emptyArray())
        for (child in children) {
            if (child.isDirectory) {
                result += collectFilesRecursively(child, maxDepth, depth + 1)
            } else if (child.isFile) {
                result += child
            }
        }
        return result
    }

    private fun isVideoFile(doc: DocumentFile): Boolean {
        val mime = doc.type?.lowercase().orEmpty()
        if (mime.startsWith("video/")) return true
        val name = doc.name?.lowercase().orEmpty()
        return VIDEO_EXTENSIONS.any { name.endsWith(it) }
    }

    private data class LocalVideoMeta(val durationMs: Long, val width: Int, val height: Int)

    private fun probeLocalVideoMetadata(uri: Uri): LocalVideoMeta {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            LocalVideoMeta(duration, width, height)
        } catch (_: Exception) {
            LocalVideoMeta(0L, 0, 0)
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
        return runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        }.getOrNull()
    }

    private fun querySizeBytes(resolver: ContentResolver, uri: Uri): Long {
        return runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (idx >= 0) cursor.getLong(idx) else 0L
                } else 0L
            } ?: 0L
        }.getOrDefault(0L)
    }

    private fun VideoHistoryEntity.toModel() = VideoItem(
        uriString = uriString,
        title = title,
        folderUri = folderUri,
        durationMs = durationMs,
        sizeBytes = sizeBytes,
        width = width,
        height = height,
        lastPositionMs = lastPositionMs,
        lastPlayedAt = lastPlayedAt,
        decoderMode = DecoderMode.fromName(decoderMode),
        externalSubtitleUri = externalSubtitleUri,
        subtitleDelayMs = subtitleDelayMs,
        aspectMode = AspectRatioMode.fromName(aspectMode)
    )

    companion object {
        private val VIDEO_EXTENSIONS = listOf(
            ".mp4", ".mkv", ".webm", ".avi", ".mov", ".m4v", ".ts", ".3gp", ".flv", ".wmv"
        )
    }
}
