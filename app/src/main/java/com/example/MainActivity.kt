package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.player.KinoXPlayerHolder
import com.example.player.PipController
import com.example.ui.library.LibraryScreen
import com.example.ui.library.LibraryViewModel
import com.example.ui.navigation.LibraryRoute
import com.example.ui.navigation.PlayerRoute
import com.example.ui.player.PlayerScreen
import com.example.ui.player.PlayerViewModel
import com.example.ui.theme.KinoXTheme

class MainActivity : ComponentActivity() {

    private var isInPipModeState by mutableStateOf(false)
    private var pendingExternalUri by mutableStateOf<Pair<String, String>?>(null)

    private var isPlayerActive: Boolean = false
    private var lastIsPlaying: Boolean = false
    private var lastVideoWidth: Int = 16
    private var lastVideoHeight: Int = 9
    private var lastSourceRect: Rect? = null
    private var activePlayerViewModel: PlayerViewModel? = null

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != PipController.ACTION_PIP_CONTROL) return
            val vm = activePlayerViewModel ?: return
            when (intent.getIntExtra(PipController.EXTRA_CONTROL_TYPE, 0)) {
                PipController.CONTROL_REWIND -> vm.seekBy(-10_000L, showHud = false)
                PipController.CONTROL_PLAY_PAUSE -> vm.togglePlayPause()
                PipController.CONTROL_FORWARD -> vm.seekBy(10_000L, showHud = false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )

        ContextCompat.registerReceiver(
            this,
            pipReceiver,
            IntentFilter(PipController.ACTION_PIP_CONTROL),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        extractExternalVideoIntent(intent)

        lifecycleScope.launch {
            KinoXPlayerHolder.sleepAutoCloseEvents.collect {
                finishAndRemoveTask()
            }
        }

        setContent {
            KinoXTheme {
                val navController = rememberNavController()
                val libraryViewModel: LibraryViewModel = viewModel()

                val externalTarget = pendingExternalUri
                LaunchedEffect(externalTarget) {
                    if (externalTarget != null) {
                        val (uriStr, name) = externalTarget
                        pendingExternalUri = null
                        navController.navigate(
                            PlayerRoute(
                                uriString = uriStr,
                                title = name
                            )
                        ) {
                            launchSingleTop = true
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = LibraryRoute
                ) {
                    composable<LibraryRoute> {
                        isPlayerActive = false
                        activePlayerViewModel = null
                        updatePipParams()

                        LibraryScreen(
                            viewModel = libraryViewModel,
                            onPlayVideo = { uriString, title, subtitleUri, folderUri ->
                                navController.navigate(
                                    PlayerRoute(
                                        uriString = uriString,
                                        title = title,
                                        subtitleUri = subtitleUri,
                                        folderUri = folderUri
                                    )
                                )
                            }
                        )
                    }

                    composable<PlayerRoute> { backStackEntry ->
                        val route = backStackEntry.toRoute<PlayerRoute>()
                        val playerViewModel: PlayerViewModel = viewModel(backStackEntry)
                        isPlayerActive = true
                        activePlayerViewModel = playerViewModel

                        PlayerScreen(
                            viewModel = playerViewModel,
                            uriString = route.uriString,
                            title = route.title,
                            subtitleUri = route.subtitleUri,
                            folderUri = route.folderUri,
                            isInPipMode = isInPipModeState,
                            onPipStateSync = { playing, width, height, rect ->
                                lastIsPlaying = playing
                                lastVideoWidth = width
                                lastVideoHeight = height
                                lastSourceRect = rect
                                updatePipParams()
                            },
                            onRequestEnterPip = { enterPipIfSupported() },
                            onNavigateBack = {
                                isPlayerActive = false
                                activePlayerViewModel = null
                                updatePipParams()
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractExternalVideoIntent(intent)
    }

    private fun extractExternalVideoIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val data: Uri = intent.data ?: return
        val title = runCatching {
            contentResolver.query(data, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) c.getString(idx) else null
                } else null
            }
        }.getOrNull() ?: data.lastPathSegment ?: "Local Video"
        pendingExternalUri = data.toString() to title
    }

    private fun updatePipParams() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !PipController.isPipSupported(this)) {
            return
        }
        val params = PipController.buildParams(
            context = this,
            isPlaying = lastIsPlaying,
            videoWidth = lastVideoWidth,
            videoHeight = lastVideoHeight,
            sourceRectHint = lastSourceRect,
            autoEnter = isPlayerActive && lastIsPlaying && !KinoXPlayerHolder.backgroundPlayEnabled
        ) ?: return
        runCatching { setPictureInPictureParams(params) }
    }

    private fun enterPipIfSupported() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !PipController.isPipSupported(this)) {
            return
        }
        val params = PipController.buildParams(
            context = this,
            isPlaying = lastIsPlaying,
            videoWidth = lastVideoWidth,
            videoHeight = lastVideoHeight,
            sourceRectHint = lastSourceRect,
            autoEnter = false
        ) ?: return
        runCatching { enterPictureInPictureMode(params) }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
            isPlayerActive &&
            lastIsPlaying &&
            !KinoXPlayerHolder.backgroundPlayEnabled
        ) {
            enterPipIfSupported()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipModeState = isInPictureInPictureMode
    }

    override fun onStop() {
        super.onStop()
        activePlayerViewModel?.persistProgress()
        val keepPlayingInBackground =
            KinoXPlayerHolder.backgroundPlayEnabled && KinoXPlayerHolder.exoPlayer?.isPlaying == true
        if (!isInPictureInPictureMode && !keepPlayingInBackground) {
            activePlayerViewModel?.pause(saveNow = true)
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(pipReceiver) }
        super.onDestroy()
    }
}
