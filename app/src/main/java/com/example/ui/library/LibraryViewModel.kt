package com.example.ui.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.KinoXDatabase
import com.example.data.local.PlayerPreferencesStore
import com.example.data.model.AspectRatioMode
import com.example.data.model.DecoderMode
import com.example.data.model.FolderItem
import com.example.data.model.OrientationMode
import com.example.data.model.PlaybackPrefs
import com.example.data.model.SleepTimerOption
import com.example.data.model.SubtitleStylePrefs
import com.example.data.model.VideoItem
import com.example.data.repository.MediaRepository
import com.example.player.KinoXPlayerHolder
import com.example.player.NowPlayingSnapshot
import com.example.player.SleepTimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LibraryTab(val label: String) {
    FOLDERS("Folders"),
    VIDEOS("Videos"),
    SETTINGS("Settings")
}

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data class Ready(
        val activeTab: LibraryTab = LibraryTab.FOLDERS,
        val searchQuery: String = "",
        val continueWatching: List<VideoItem> = emptyList(),
        val localVideos: List<VideoItem> = emptyList(),
        val pinnedFolders: List<FolderItem> = emptyList(),
        val activeFolder: FolderItem? = null,
        val activeFolderVideos: List<VideoItem> = emptyList(),
        val isScanningFolder: Boolean = false,
        val playbackPrefs: PlaybackPrefs = PlaybackPrefs(),
        val nowPlaying: NowPlayingSnapshot = NowPlayingSnapshot(),
        val sleepTimer: SleepTimerState = SleepTimerState(),
        val statusMessage: String? = null
    ) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}

private data class LibraryControlState(
    val activeTab: LibraryTab = LibraryTab.FOLDERS,
    val searchQuery: String = "",
    val activeFolder: FolderItem? = null,
    val activeFolderVideos: List<VideoItem> = emptyList(),
    val isScanningFolder: Boolean = false,
    val folderCounts: Map<String, Int> = emptyMap(),
    val statusMessage: String? = null
)

private data class PlayerRuntimeOverlay(
    val nowPlaying: NowPlayingSnapshot,
    val sleepTimer: SleepTimerState
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val database = KinoXDatabase.getInstance(application)
    private val prefsStore = PlayerPreferencesStore(application)
    val repository = MediaRepository(application, database.kinoXDao(), prefsStore)

    private val controlState = MutableStateFlow(LibraryControlState())

    private val runtimeOverlay = combine(
        KinoXPlayerHolder.nowPlaying,
        KinoXPlayerHolder.sleepTimer
    ) { nowPlaying, sleepTimer ->
        PlayerRuntimeOverlay(nowPlaying = nowPlaying, sleepTimer = sleepTimer)
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        repository.videoHistory,
        repository.pinnedFolders,
        repository.playbackPrefs,
        runtimeOverlay,
        controlState
    ) { history, folders, prefs, runtime, controls ->
        KinoXPlayerHolder.backgroundPlayEnabled = prefs.backgroundPlayEnabled

        val query = controls.searchQuery.trim().lowercase()
        val continueWatching = history
            .filter { it.lastPositionMs > 0L }
            .filter { query.isEmpty() || it.title.lowercase().contains(query) }
            .take(10)

        val localVideos = history
            .filter { query.isEmpty() || it.title.lowercase().contains(query) }

        val enrichedFolders = folders
            .map { f ->
                f.copy(videoCount = controls.folderCounts[f.treeUriString] ?: f.videoCount)
            }
            .filter { query.isEmpty() || it.displayName.lowercase().contains(query) }

        val filteredFolderVideos = controls.activeFolderVideos
            .filter { query.isEmpty() || it.title.lowercase().contains(query) }

        LibraryUiState.Ready(
            activeTab = controls.activeTab,
            searchQuery = controls.searchQuery,
            continueWatching = continueWatching,
            localVideos = localVideos,
            pinnedFolders = enrichedFolders,
            activeFolder = controls.activeFolder,
            activeFolderVideos = filteredFolderVideos,
            isScanningFolder = controls.isScanningFolder,
            playbackPrefs = prefs,
            nowPlaying = runtime.nowPlaying,
            sleepTimer = runtime.sleepTimer,
            statusMessage = controls.statusMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState.Loading
    )

    init {
        refreshFolderCounts()
    }

    fun selectTab(tab: LibraryTab) {
        controlState.update { it.copy(activeTab = tab) }
    }

    fun updateSearchQuery(query: String) {
        controlState.update { it.copy(searchQuery = query) }
    }

    fun clearStatusMessage() {
        controlState.update { it.copy(statusMessage = null) }
    }

    fun cycleDefaultDecoder(current: DecoderMode) {
        viewModelScope.launch {
            repository.updateDefaultDecoder(current.next())
        }
    }

    fun setDefaultDecoder(mode: DecoderMode) {
        viewModelScope.launch {
            repository.updateDefaultDecoder(mode)
        }
    }

    fun setDefaultAspect(mode: AspectRatioMode) {
        viewModelScope.launch {
            repository.updateDefaultAspect(mode)
        }
    }

    fun setDefaultOrientation(mode: OrientationMode) {
        viewModelScope.launch {
            repository.updateDefaultOrientation(mode)
        }
    }

    fun setDefaultSpeed(speed: Float) {
        viewModelScope.launch {
            repository.updateDefaultSpeed(speed)
        }
    }

    fun setSeekStepSeconds(seconds: Int) {
        viewModelScope.launch {
            repository.updateSeekStepSeconds(seconds)
        }
    }

    fun setBackgroundPlayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateBackgroundPlayEnabled(enabled)
            KinoXPlayerHolder.backgroundPlayEnabled = enabled
        }
    }

    fun setAutoResume(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAutoResume(enabled)
        }
    }

    fun setBrightnessGesture(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateBrightnessGesture(enabled)
        }
    }

    fun setVolumeGesture(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateVolumeGesture(enabled)
        }
    }

    fun setSeekGesture(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSeekGesture(enabled)
        }
    }

    fun setPinchZoom(enabled: Boolean) {
        viewModelScope.launch {
            repository.updatePinchZoom(enabled)
        }
    }

    fun setDoubleTapSeek(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateDoubleTapSeek(enabled)
        }
    }

    fun setSubtitleStyle(style: SubtitleStylePrefs) {
        viewModelScope.launch {
            repository.updateSubtitleStyle(style)
        }
    }

    fun resetAllSettings() {
        viewModelScope.launch {
            repository.resetSettingsToDefaults()
            KinoXPlayerHolder.backgroundPlayEnabled = true
            controlState.update { it.copy(statusMessage = "Settings reset to defaults") }
        }
    }

    fun clearWatchHistory() {
        viewModelScope.launch {
            repository.clearWatchHistory()
            controlState.update { it.copy(statusMessage = "Watch history cleared") }
        }
    }

    fun setSleepTimer(option: SleepTimerOption) {
        KinoXPlayerHolder.setSleepTimer(option)
        val msg = when (option) {
            SleepTimerOption.OFF -> "Sleep Mode turned off"
            SleepTimerOption.END_OF_VIDEO -> "Sleep Mode: App will auto-close when video ends"
            else -> "Sleep Mode: App will auto-close in ${option.label}"
        }
        controlState.update { it.copy(statusMessage = msg) }
    }

    fun setCustomSleepMinutes(minutes: Int) {
        KinoXPlayerHolder.setCustomSleepMinutes(minutes)
        val msg = if (minutes <= 0) {
            "Sleep Mode turned off"
        } else {
            "Sleep Mode: App will auto-close in $minutes min"
        }
        controlState.update { it.copy(statusMessage = msg) }
    }

    fun toggleMiniBarPlayPause() {
        KinoXPlayerHolder.togglePlayPauseFromMiniBar()
    }

    fun stopBackgroundPlayback() {
        KinoXPlayerHolder.stopAndClearSession()
    }

    fun pinFolder(treeUri: Uri) {
        viewModelScope.launch {
            runCatching {
                val folder = repository.pinFolder(treeUri)
                controlState.update { state ->
                    state.copy(
                        activeTab = LibraryTab.FOLDERS,
                        folderCounts = state.folderCounts + (folder.treeUriString to folder.videoCount),
                        statusMessage = "Linked \"${folder.displayName}\" (${folder.videoCount} videos)"
                    )
                }
                openFolder(folder)
            }.onFailure { err ->
                controlState.update {
                    it.copy(statusMessage = err.message ?: "Unable to access selected folder")
                }
            }
        }
    }

    fun unpinFolder(folder: FolderItem) {
        viewModelScope.launch {
            repository.unpinFolder(folder.treeUriString)
            controlState.update { state ->
                val resetActive = state.activeFolder?.treeUriString == folder.treeUriString
                state.copy(
                    activeFolder = if (resetActive) null else state.activeFolder,
                    activeFolderVideos = if (resetActive) emptyList() else state.activeFolderVideos
                )
            }
        }
    }

    fun openFolder(folder: FolderItem) {
        viewModelScope.launch {
            controlState.update { it.copy(activeFolder = folder, isScanningFolder = true) }
            val videos = repository.scanFolderVideos(folder.treeUriString)
            controlState.update { state ->
                state.copy(
                    activeFolder = folder.copy(videoCount = videos.size),
                    activeFolderVideos = videos,
                    isScanningFolder = false,
                    folderCounts = state.folderCounts + (folder.treeUriString to videos.size)
                )
            }
        }
    }

    fun closeFolder() {
        controlState.update {
            it.copy(activeFolder = null, activeFolderVideos = emptyList(), isScanningFolder = false)
        }
    }

    fun handlePickedVideo(uri: Uri, onReadyToPlay: (VideoItem) -> Unit) {
        viewModelScope.launch {
            runCatching {
                repository.registerPickedVideo(uri)
            }.onSuccess { item ->
                onReadyToPlay(item)
            }.onFailure { err ->
                controlState.update {
                    it.copy(statusMessage = err.message ?: "Could not open selected video")
                }
            }
        }
    }

    fun removeHistoryItem(uriString: String) {
        viewModelScope.launch {
            repository.removeHistoryItem(uriString)
        }
    }

    private fun refreshFolderCounts() {
        viewModelScope.launch {
            repository.pinnedFolders.collect { folders ->
                val counts = mutableMapOf<String, Int>()
                folders.forEach { folder ->
                    counts[folder.treeUriString] = repository.countFolderVideos(folder.treeUriString)
                }
                controlState.update { it.copy(folderCounts = counts) }
            }
        }
    }
}
