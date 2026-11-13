package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.KinoXDatabase
import com.example.data.local.PinnedFolderEntity
import com.example.data.local.PlayerPreferencesStore
import com.example.data.local.VideoHistoryEntity
import com.example.data.model.AspectRatioMode
import com.example.data.model.DecoderMode
import com.example.data.model.OrientationMode
import com.example.data.model.SleepTimerOption
import com.example.data.model.SubtitleStylePrefs
import com.example.data.repository.MediaRepository
import com.example.player.BatteryOptimizer
import com.example.player.KinoXPlayerHolder
import com.example.ui.library.LibraryScreen
import com.example.ui.library.LibraryViewModel
import com.example.ui.theme.KinoXTheme
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29, 36])
class KinoXUnitAndUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun alwaysOnBatteryOptimizerConfiguresLowLatencyAndThrottledPolling() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val loadControl = BatteryOptimizer.buildAlwaysOptimizedLoadControl(context)
        assertNotNull(loadControl)

        val activeInterval = BatteryOptimizer.optimalTickerIntervalMs(context, isPlaying = true)
        val pausedInterval = BatteryOptimizer.optimalTickerIntervalMs(context, isPlaying = false)
        assertTrue(pausedInterval > activeInterval)
    }

    @Test
    fun sleepTimerArmsAndTriggersAutoCloseEvent() = runBlocking {
        KinoXPlayerHolder.setSleepTimer(SleepTimerOption.MIN_15)
        assertTrue(KinoXPlayerHolder.sleepTimer.value.isActive)
        assertEquals(SleepTimerOption.MIN_15, KinoXPlayerHolder.sleepTimer.value.option)
        assertEquals(900L, KinoXPlayerHolder.sleepTimer.value.remainingSeconds)

        val closeDeferred = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(5_000L) {
                KinoXPlayerHolder.sleepAutoCloseEvents.first()
            }
        }
        KinoXPlayerHolder.triggerSleepAutoClose()
        closeDeferred.await()

        assertFalse(KinoXPlayerHolder.sleepTimer.value.isActive)
    }

    @Test
    fun roomDatabasePersistsAndClearsLocalVideosAndFolders() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, KinoXDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.kinoXDao()

        dao.upsertPinnedFolder(
            PinnedFolderEntity(treeUriString = "content://tree/movies", displayName = "Movies")
        )
        dao.upsertVideoHistory(
            VideoHistoryEntity(
                uriString = "content://media/video1.mp4",
                title = "Demo Movie.mp4",
                durationMs = 120_000L,
                lastPositionMs = 45_000L,
                decoderMode = DecoderMode.HW.name
            )
        )

        val folders = dao.observePinnedFolders().first()
        val videos = dao.observeVideoHistory().first()

        assertEquals(1, folders.size)
        assertEquals("Movies", folders.first().displayName)
        assertEquals(1, videos.size)
        assertEquals(45_000L, videos.first().lastPositionMs)

        dao.clearAllVideoHistory()
        assertEquals(0, dao.observeVideoHistory().first().size)
        db.close()
    }

    @Test
    fun playbackPreferencesCustomizeAndResetToDefaults() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, KinoXDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val prefsStore = PlayerPreferencesStore(context)
        val repository = MediaRepository(context, db.kinoXDao(), prefsStore)

        repository.updateDefaultDecoder(DecoderMode.SW)
        repository.updateDefaultAspect(AspectRatioMode.RATIO_16_9)
        repository.updateDefaultOrientation(OrientationMode.LANDSCAPE)
        repository.updateDefaultSpeed(1.5f)
        repository.updateSeekStepSeconds(30)
        repository.updateBrightnessGesture(false)
        repository.updateVolumeGesture(false)
        repository.updatePinchZoom(false)
        repository.updateSubtitleStyle(
            SubtitleStylePrefs(fontSizeSp = 26f, textColorArgb = 0xFF00E5FFL, hasOutline = false)
        )

        val updated = repository.playbackPrefs.first()
        assertEquals(DecoderMode.SW, updated.defaultDecoderMode)
        assertEquals(AspectRatioMode.RATIO_16_9, updated.defaultAspectMode)
        assertEquals(OrientationMode.LANDSCAPE, updated.defaultOrientationMode)
        assertEquals(1.5f, updated.defaultPlaybackSpeed, 0.01f)
        assertEquals(30, updated.seekStepSeconds)
        assertFalse(updated.brightnessGestureEnabled)
        assertFalse(updated.volumeGestureEnabled)
        assertFalse(updated.pinchZoomEnabled)
        assertEquals(26f, updated.subtitleStyle.fontSizeSp, 0.01f)
        assertEquals(0xFF00E5FFL, updated.subtitleStyle.textColorArgb)
        assertFalse(updated.subtitleStyle.hasOutline)

        repository.resetSettingsToDefaults()
        val reset = repository.playbackPrefs.first()
        assertEquals(DecoderMode.HW_PLUS, reset.defaultDecoderMode)
        assertEquals(AspectRatioMode.FIT, reset.defaultAspectMode)
        assertEquals(OrientationMode.AUTO, reset.defaultOrientationMode)
        assertEquals(1.0f, reset.defaultPlaybackSpeed, 0.01f)
        assertEquals(10, reset.seekStepSeconds)
        assertTrue(reset.brightnessGestureEnabled)
        assertTrue(reset.volumeGestureEnabled)
        assertTrue(reset.pinchZoomEnabled)
        assertTrue(reset.subtitleStyle.hasOutline)

        db.close()
    }

    @Test
    fun libraryScreenIsSimplifiedAndSleepModeSetsCountdown() {
        KinoXPlayerHolder.setSleepTimer(SleepTimerOption.OFF)
        val context = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = LibraryViewModel(context)

        composeTestRule.setContent {
            KinoXTheme {
                LibraryScreen(
                    viewModel = viewModel,
                    onPlayVideo = { _, _, _, _ -> }
                )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 8_000L) {
            composeTestRule.onAllNodesWithTag("link_folder_button").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("link_folder_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pick_video_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_folders").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_videos").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_settings").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_power").assertDoesNotExist()

        composeTestRule.onNodeWithTag("hero_sleep_pill").performClick()
        composeTestRule.onNodeWithText("Sleeping Time Mode").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sleep_chip_min_30").performClick()
        composeTestRule.onNodeWithTag("sleep_timer_countdown_badge").assertIsDisplayed()

        KinoXPlayerHolder.setSleepTimer(SleepTimerOption.OFF)
    }

    @Test
    fun settingsTabRendersFullCustomizationSections() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = LibraryViewModel(context)

        composeTestRule.setContent {
            KinoXTheme {
                LibraryScreen(
                    viewModel = viewModel,
                    onPlayVideo = { _, _, _, _ -> }
                )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 8_000L) {
            composeTestRule.onAllNodesWithTag("nav_tab_settings").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("nav_tab_settings").performClick()
        composeTestRule.onNodeWithText("Customize KinoX").assertIsDisplayed()
        composeTestRule.onNodeWithTag("setting_decoder_hw_plus").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("setting_aspect_ratio_16_9").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("setting_toggle_brightness_gesture").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_subtitle_preview").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("reset_settings_button").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("clear_history_button").performScrollTo().assertIsDisplayed()
    }
}
