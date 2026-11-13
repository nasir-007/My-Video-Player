package com.example.ui.library

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.AspectRatioMode
import com.example.data.model.DecoderMode
import com.example.data.model.FolderItem
import com.example.data.model.OrientationMode
import com.example.data.model.SleepTimerOption
import com.example.data.model.SubtitleStylePrefs
import com.example.data.model.VideoItem
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.SoftCyanContainer
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TheaterSurface

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onPlayVideo: (uriString: String, title: String, subtitleUri: String?, folderUri: String?) -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSleepSheet by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            viewModel.pinFolder(treeUri)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.handlePickedVideo(uri) { video ->
                onPlayVideo(video.uriString, video.title, video.externalSubtitleUri, video.folderUri)
            }
        }
    }

    val readyState = state as? LibraryUiState.Ready
    if (readyState?.activeFolder != null) {
        BackHandler {
            viewModel.closeFolder()
        }
    }

    LaunchedEffect(readyState?.statusMessage) {
        val message = readyState?.statusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearStatusMessage()
    }

    fun requestNotificationIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 700.dp

        Scaffold(
            containerColor = ObsidianBackground,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                Column {
                    if (readyState != null && readyState.nowPlaying.isActive) {
                        BackgroundMiniPlayerBar(
                            nowPlaying = readyState.nowPlaying,
                            sleepTimer = readyState.sleepTimer,
                            onResumeVideo = {
                                onPlayVideo(
                                    readyState.nowPlaying.uriString,
                                    readyState.nowPlaying.title,
                                    null,
                                    null
                                )
                            },
                            onTogglePlayPause = viewModel::toggleMiniBarPlayPause,
                            onStop = viewModel::stopBackgroundPlayback
                        )
                    }
                    if (!isWideScreen && readyState != null) {
                        NavigationBar(
                            containerColor = TheaterSurface,
                            tonalElevation = 0.dp
                        ) {
                            LibraryTab.entries.forEach { tab ->
                                val selected = readyState.activeTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = tabIcon(tab, selected),
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = ElectricCyan,
                                        selectedTextColor = ElectricCyan,
                                        indicatorColor = SoftCyanContainer,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen && readyState != null) {
                    NavigationRail(
                        containerColor = TheaterSurface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .navigationBarsPadding()
                    ) {
                        Spacer(Modifier.height(16.dp))
                        LibraryTab.entries.forEach { tab ->
                            val selected = readyState.activeTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = SoftCyanContainer,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    when (val ui = state) {
                        is LibraryUiState.Loading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = ElectricCyan)
                            }
                        }
                        is LibraryUiState.Error -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(ui.message, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        is LibraryUiState.Ready -> {
                            LibraryContent(
                                state = ui,
                                isWideScreen = isWideScreen,
                                onCycleDecoder = { viewModel.cycleDefaultDecoder(ui.playbackPrefs.defaultDecoderMode) },
                                onOpenSleepMode = { showSleepSheet = true },
                                onLinkFolder = { folderPickerLauncher.launch(null) },
                                onPickVideo = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                },
                                onSearchChange = viewModel::updateSearchQuery,
                                onOpenFolder = viewModel::openFolder,
                                onCloseFolder = viewModel::closeFolder,
                                onUnpinFolder = viewModel::unpinFolder,
                                onPlayVideoItem = { video ->
                                    onPlayVideo(
                                        video.uriString,
                                        video.title,
                                        video.externalSubtitleUri,
                                        video.folderUri
                                    )
                                },
                                onRemoveHistory = viewModel::removeHistoryItem,
                                onSelectDecoder = viewModel::setDefaultDecoder,
                                onSelectAspect = viewModel::setDefaultAspect,
                                onSelectOrientation = viewModel::setDefaultOrientation,
                                onSelectSpeed = viewModel::setDefaultSpeed,
                                onSelectSeekStep = viewModel::setSeekStepSeconds,
                                onToggleBackgroundPlay = { enabled ->
                                    if (enabled) requestNotificationIfNeeded()
                                    viewModel.setBackgroundPlayEnabled(enabled)
                                },
                                onToggleAutoResume = viewModel::setAutoResume,
                                onToggleBrightnessGesture = viewModel::setBrightnessGesture,
                                onToggleVolumeGesture = viewModel::setVolumeGesture,
                                onToggleSeekGesture = viewModel::setSeekGesture,
                                onTogglePinchZoom = viewModel::setPinchZoom,
                                onToggleDoubleTapSeek = viewModel::setDoubleTapSeek,
                                onUpdateSubtitleStyle = viewModel::setSubtitleStyle,
                                onSelectSleepTimer = viewModel::setSleepTimer,
                                onClearWatchHistory = viewModel::clearWatchHistory,
                                onResetSettings = viewModel::resetAllSettings
                            )
                        }
                    }
                }
            }
        }

        if (showSleepSheet && readyState != null) {
            SleepAndPlaybackModalSheet(
                prefs = readyState.playbackPrefs,
                sleepTimer = readyState.sleepTimer,
                onDismiss = { showSleepSheet = false },
                onSelectSleepTimer = viewModel::setSleepTimer,
                onSetCustomSleepMinutes = viewModel::setCustomSleepMinutes,
                onToggleBackgroundPlay = { enabled ->
                    if (enabled) requestNotificationIfNeeded()
                    viewModel.setBackgroundPlayEnabled(enabled)
                },
                onToggleAutoResume = viewModel::setAutoResume
            )
        }
    }
}

@Composable
private fun LibraryContent(
    state: LibraryUiState.Ready,
    isWideScreen: Boolean,
    onCycleDecoder: () -> Unit,
    onOpenSleepMode: () -> Unit,
    onLinkFolder: () -> Unit,
    onPickVideo: () -> Unit,
    onSearchChange: (String) -> Unit,
    onOpenFolder: (FolderItem) -> Unit,
    onCloseFolder: () -> Unit,
    onUnpinFolder: (FolderItem) -> Unit,
    onPlayVideoItem: (VideoItem) -> Unit,
    onRemoveHistory: (String) -> Unit,
    onSelectDecoder: (DecoderMode) -> Unit,
    onSelectAspect: (AspectRatioMode) -> Unit,
    onSelectOrientation: (OrientationMode) -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onSelectSeekStep: (Int) -> Unit,
    onToggleBackgroundPlay: (Boolean) -> Unit,
    onToggleAutoResume: (Boolean) -> Unit,
    onToggleBrightnessGesture: (Boolean) -> Unit,
    onToggleVolumeGesture: (Boolean) -> Unit,
    onToggleSeekGesture: (Boolean) -> Unit,
    onTogglePinchZoom: (Boolean) -> Unit,
    onToggleDoubleTapSeek: (Boolean) -> Unit,
    onUpdateSubtitleStyle: (SubtitleStylePrefs) -> Unit,
    onSelectSleepTimer: (SleepTimerOption) -> Unit,
    onClearWatchHistory: () -> Unit,
    onResetSettings: () -> Unit
) {
    val columns = if (isWideScreen) 2 else 1

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 1040.dp)
    ) {
        if (state.activeTab != LibraryTab.SETTINGS) {
            item(span = { GridItemSpan(columns) }) {
                CinemaHeroHeader(
                    defaultDecoder = state.playbackPrefs.defaultDecoderMode,
                    sleepTimer = state.sleepTimer,
                    onCycleDecoder = onCycleDecoder,
                    onOpenSleepMode = onOpenSleepMode,
                    onLinkFolder = onLinkFolder,
                    onPickVideo = onPickVideo
                )
            }

            if (state.continueWatching.isNotEmpty()) {
                item(span = { GridItemSpan(columns) }) {
                    ContinueWatchingSection(
                        items = state.continueWatching,
                        onPlayVideo = onPlayVideoItem,
                        onRemoveHistory = onRemoveHistory
                    )
                }
            }

            item(span = { GridItemSpan(columns) }) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search local folders or videos...") },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted)
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = TextMuted)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = ElevatedSlate,
                        unfocusedContainerColor = ElevatedSlate
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library_search_input")
                )
            }
        }

        when (state.activeTab) {
            LibraryTab.FOLDERS -> {
                val openFolder = state.activeFolder
                if (openFolder != null) {
                    item(span = { GridItemSpan(columns) }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = onCloseFolder,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.cd_back),
                                    tint = ElectricCyan
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = openFolder.displayName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${state.activeFolderVideos.size} videos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    if (state.isScanningFolder) {
                        item(span = { GridItemSpan(columns) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = ElectricCyan)
                            }
                        }
                    } else if (state.activeFolderVideos.isEmpty()) {
                        item(span = { GridItemSpan(columns) }) {
                            EmptyLibraryCard(
                                title = "No Video Files Found",
                                subtitle = "This folder does not contain supported MP4, MKV, WebM, AVI, or MOV files.",
                                primaryActionLabel = "Link Another Folder",
                                onPrimaryAction = onLinkFolder
                            )
                        }
                    } else {
                        items(state.activeFolderVideos, key = { it.uriString }) { video ->
                            VideoListCard(
                                video = video,
                                onClick = { onPlayVideoItem(video) }
                            )
                        }
                    }
                } else {
                    item(span = { GridItemSpan(columns) }) {
                        Text(
                            text = stringResource(R.string.section_pinned_folders),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    if (state.pinnedFolders.isEmpty()) {
                        item(span = { GridItemSpan(columns) }) {
                            EmptyLibraryCard(
                                title = stringResource(R.string.empty_library_title),
                                subtitle = stringResource(R.string.empty_library_subtitle),
                                primaryActionLabel = stringResource(R.string.action_link_folder),
                                onPrimaryAction = onLinkFolder,
                                secondaryActionLabel = stringResource(R.string.action_pick_video),
                                onSecondaryAction = onPickVideo
                            )
                        }
                    } else {
                        items(state.pinnedFolders, key = { it.treeUriString }) { folder ->
                            FolderCard(
                                folder = folder,
                                onClick = { onOpenFolder(folder) },
                                onRemove = { onUnpinFolder(folder) }
                            )
                        }
                    }
                }
            }

            LibraryTab.VIDEOS -> {
                item(span = { GridItemSpan(columns) }) {
                    Text(
                        text = stringResource(R.string.section_local_videos),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                if (state.localVideos.isEmpty()) {
                    item(span = { GridItemSpan(columns) }) {
                        EmptyLibraryCard(
                            title = "No Local Videos Yet",
                            subtitle = "Pick a video file or link a local folder to build your offline cinema library.",
                            primaryActionLabel = stringResource(R.string.action_pick_video),
                            onPrimaryAction = onPickVideo,
                            secondaryActionLabel = stringResource(R.string.action_link_folder),
                            onSecondaryAction = onLinkFolder
                        )
                    }
                } else {
                    items(state.localVideos, key = { it.uriString }) { video ->
                        VideoListCard(
                            video = video,
                            onClick = { onPlayVideoItem(video) },
                            onRemove = { onRemoveHistory(video.uriString) }
                        )
                    }
                }
            }

            LibraryTab.SETTINGS -> {
                item(span = { GridItemSpan(columns) }) {
                    Text(
                        text = "Customize KinoX",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                item(span = { GridItemSpan(columns) }) {
                    SettingsCustomizationSection(
                        prefs = state.playbackPrefs,
                        sleepTimer = state.sleepTimer,
                        onSelectDecoder = onSelectDecoder,
                        onSelectAspect = onSelectAspect,
                        onSelectOrientation = onSelectOrientation,
                        onSelectSpeed = onSelectSpeed,
                        onSelectSeekStep = onSelectSeekStep,
                        onToggleBackgroundPlay = onToggleBackgroundPlay,
                        onToggleAutoResume = onToggleAutoResume,
                        onToggleBrightnessGesture = onToggleBrightnessGesture,
                        onToggleVolumeGesture = onToggleVolumeGesture,
                        onToggleSeekGesture = onToggleSeekGesture,
                        onTogglePinchZoom = onTogglePinchZoom,
                        onToggleDoubleTapSeek = onToggleDoubleTapSeek,
                        onUpdateSubtitleStyle = onUpdateSubtitleStyle,
                        onSelectSleepTimer = onSelectSleepTimer,
                        onOpenSleepModal = onOpenSleepMode,
                        onClearWatchHistory = onClearWatchHistory,
                        onResetSettings = onResetSettings
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryCard(
    title: String,
    subtitle: String,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ElevatedSlate,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.VideoLibrary,
                contentDescription = null,
                tint = ElectricCyan,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPrimaryAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = ObsidianBackground
                    ),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(primaryActionLabel, fontWeight = FontWeight.Bold)
                }
                if (secondaryActionLabel != null && onSecondaryAction != null) {
                    OutlinedButton(
                        onClick = onSecondaryAction,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text(secondaryActionLabel, color = ElectricCyan)
                    }
                }
            }
        }
    }
}

private fun tabIcon(tab: LibraryTab, selected: Boolean) = when (tab) {
    LibraryTab.FOLDERS -> if (selected) Icons.Filled.Folder else Icons.Outlined.Folder
    LibraryTab.VIDEOS -> if (selected) Icons.Filled.Movie else Icons.Outlined.Movie
    LibraryTab.SETTINGS -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
}
