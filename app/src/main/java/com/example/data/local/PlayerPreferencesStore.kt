package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AspectRatioMode
import com.example.data.model.DecoderMode
import com.example.data.model.OrientationMode
import com.example.data.model.PlaybackPrefs
import com.example.data.model.SubtitleStylePrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.kinoXPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "kinox_prefs")

class PlayerPreferencesStore(private val context: Context) {

    private object Keys {
        val DEFAULT_DECODER = stringPreferencesKey("default_decoder")
        val DEFAULT_ASPECT = stringPreferencesKey("default_aspect")
        val DEFAULT_ORIENTATION = stringPreferencesKey("default_orientation")
        val DEFAULT_SPEED = floatPreferencesKey("default_speed")
        val BACKGROUND_PLAY_ENABLED = booleanPreferencesKey("background_play_enabled")
        val SEEK_STEP_SECONDS = intPreferencesKey("seek_step_seconds")
        val AUTO_RESUME = booleanPreferencesKey("auto_resume")
        val GESTURE_BRIGHTNESS = booleanPreferencesKey("gesture_brightness")
        val GESTURE_VOLUME = booleanPreferencesKey("gesture_volume")
        val GESTURE_SEEK = booleanPreferencesKey("gesture_seek")
        val GESTURE_PINCH_ZOOM = booleanPreferencesKey("gesture_pinch_zoom")
        val GESTURE_DOUBLE_TAP = booleanPreferencesKey("gesture_double_tap")
        val SUB_FONT_SIZE = floatPreferencesKey("sub_font_size")
        val SUB_TEXT_COLOR = longPreferencesKey("sub_text_color")
        val SUB_BG_OPACITY = floatPreferencesKey("sub_bg_opacity")
        val SUB_OUTLINE = booleanPreferencesKey("sub_outline")
        val SUB_BOTTOM_MARGIN = floatPreferencesKey("sub_bottom_margin")
    }

    val playbackPrefs: Flow<PlaybackPrefs> = context.kinoXPrefsDataStore.data.map { prefs ->
        val savedOrientation = prefs[Keys.DEFAULT_ORIENTATION]
        PlaybackPrefs(
            defaultDecoderMode = DecoderMode.fromName(prefs[Keys.DEFAULT_DECODER]),
            defaultAspectMode = AspectRatioMode.fromName(prefs[Keys.DEFAULT_ASPECT]),
            defaultOrientationMode = OrientationMode.entries.firstOrNull { it.name == savedOrientation }
                ?: OrientationMode.AUTO,
            defaultPlaybackSpeed = (prefs[Keys.DEFAULT_SPEED] ?: 1.0f).coerceIn(0.25f, 3.0f),
            backgroundPlayEnabled = prefs[Keys.BACKGROUND_PLAY_ENABLED] ?: true,
            seekStepSeconds = (prefs[Keys.SEEK_STEP_SECONDS] ?: 10).coerceIn(5, 60),
            autoResume = prefs[Keys.AUTO_RESUME] ?: true,
            brightnessGestureEnabled = prefs[Keys.GESTURE_BRIGHTNESS] ?: true,
            volumeGestureEnabled = prefs[Keys.GESTURE_VOLUME] ?: true,
            seekGestureEnabled = prefs[Keys.GESTURE_SEEK] ?: true,
            pinchZoomEnabled = prefs[Keys.GESTURE_PINCH_ZOOM] ?: true,
            doubleTapSeekEnabled = prefs[Keys.GESTURE_DOUBLE_TAP] ?: true,
            subtitleStyle = SubtitleStylePrefs(
                fontSizeSp = prefs[Keys.SUB_FONT_SIZE] ?: 20f,
                textColorArgb = prefs[Keys.SUB_TEXT_COLOR] ?: 0xFFFFFFFFL,
                backgroundOpacity = prefs[Keys.SUB_BG_OPACITY] ?: 0.55f,
                hasOutline = prefs[Keys.SUB_OUTLINE] ?: true,
                bottomMarginPercent = prefs[Keys.SUB_BOTTOM_MARGIN] ?: 9f
            )
        )
    }

    suspend fun updateDefaultDecoder(mode: DecoderMode) {
        context.kinoXPrefsDataStore.edit { it[Keys.DEFAULT_DECODER] = mode.name }
    }

    suspend fun updateDefaultAspect(mode: AspectRatioMode) {
        context.kinoXPrefsDataStore.edit { it[Keys.DEFAULT_ASPECT] = mode.name }
    }

    suspend fun updateDefaultOrientation(mode: OrientationMode) {
        context.kinoXPrefsDataStore.edit { it[Keys.DEFAULT_ORIENTATION] = mode.name }
    }

    suspend fun updateDefaultSpeed(speed: Float) {
        context.kinoXPrefsDataStore.edit { it[Keys.DEFAULT_SPEED] = speed.coerceIn(0.25f, 3.0f) }
    }

    suspend fun updateSeekStepSeconds(seconds: Int) {
        context.kinoXPrefsDataStore.edit { it[Keys.SEEK_STEP_SECONDS] = seconds.coerceIn(5, 60) }
    }

    suspend fun updateBackgroundPlayEnabled(enabled: Boolean) {
        context.kinoXPrefsDataStore.edit { it[Keys.BACKGROUND_PLAY_ENABLED] = enabled }
    }

    suspend fun updateAutoResume(enabled: Boolean) {
        context.kinoXPrefsDataStore.edit { it[Keys.AUTO_RESUME] = enabled }
    }

    suspend fun updateBrightnessGesture(enabled: Boolean) {
        context.kinoXPrefsDataStore.edit { it[Keys.GESTURE_BRIGHTNESS] = enabled }
    }

    suspend fun updateVolumeGesture(enabled: Boolean) {
        context.kinoXPrefsDataStore.edit { it[Keys.GESTURE_VOLUME] = enabled }
    }

    suspend fun updateSeekGesture(enabled: Boolean) {
        context.kinoXPrefsDataStore.edit { it[Keys.GESTURE_SEEK] = enabled }
    }

    suspend fun updatePinchZoom(enabled: Boolean) {
        context.kinoXPrefsDataStore.edit { it[Keys.GESTURE_PINCH_ZOOM] = enabled }
    }

    suspend fun updateDoubleTapSeek(enabled: Boolean) {
        context.kinoXPrefsDataStore.edit { it[Keys.GESTURE_DOUBLE_TAP] = enabled }
    }

    suspend fun updateSubtitleStyle(style: SubtitleStylePrefs) {
        context.kinoXPrefsDataStore.edit { prefs ->
            prefs[Keys.SUB_FONT_SIZE] = style.fontSizeSp
            prefs[Keys.SUB_TEXT_COLOR] = style.textColorArgb
            prefs[Keys.SUB_BG_OPACITY] = style.backgroundOpacity
            prefs[Keys.SUB_OUTLINE] = style.hasOutline
            prefs[Keys.SUB_BOTTOM_MARGIN] = style.bottomMarginPercent
        }
    }

    suspend fun resetAllToDefaults() {
        context.kinoXPrefsDataStore.edit { it.clear() }
    }
}
