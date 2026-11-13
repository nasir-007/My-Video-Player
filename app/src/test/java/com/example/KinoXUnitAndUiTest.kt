package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.KinoXDatabase
import com.example.data.local.PinnedFolderEntity
import com.example.data.local.VideoHistoryEntity
import com.example.data.model.DecoderMode
import com.example.data.model.PowerProfile
import com.example.player.BatteryOptimizer
import com.example.ui.library.LibraryScreen
import com.example.ui.library.LibraryViewModel
import com.example.ui.theme.KinoXTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KinoXUnitAndUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun powerProfilesHaveBatteryOptimizedBuffersAndPolling() {
        assertTrue(PowerProfile.ECO.minBufferMs < PowerProfile.BALANCED.minBufferMs)
        assertTrue(PowerProfile.BALANCED.minBufferMs < PowerProfile.CINEMA.minBufferMs)
        assertTrue(PowerProfile.ECO.tickerIntervalMs > PowerProfile.BALANCED.tickerIntervalMs)
        assertEquals(DecoderMode.HW, PowerProfile.ECO.preferredDecoder)

        val ecoLoadControl = BatteryOptimizer.buildLoadControl(PowerProfile.ECO)
        assertNotNull(ecoLoadControl)
        val snapshot = BatteryOptimizer.readBatterySnapshot(ApplicationProvider.getApplicationContext())
        assertTrue(snapshot.batteryPercent in 0..100)
    }

    @Test
    fun roomDatabasePersistsLocalVideosAndFolders() = runBlocking {
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
        db.close()
    }

    @Test
    fun libraryScreenDisplaysLocalSourcesAndPowerTabWithoutLinkSection() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = LibraryViewModel(context)

        composeTestRule.setContent {
            KinoXTheme {
                LibraryScreen(
                    viewModel = viewModel,
                    onPlayVideo = { _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("link_folder_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pick_video_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_power").performClick()
        composeTestRule.onNodeWithText("Battery & Doze Guard").assertIsDisplayed()
        composeTestRule.onNodeWithTag("power_profile_eco").performClick()
    }
}
