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
import com.example.data.model.DecoderMode
import com.example.data.model.PlaybackPrefs
import com.example.data.model.PowerProfile
import com.example.data.model.SubtitleStylePrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.kinoXPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "kinox_prefs")

class PlayerPreferencesStore(private val context: Context) {

    private object Keys {
        val DEFAULT_DECODER = stringPreferencesKey("default_decoder")
        val POWER_PROFILE = stringPreferencesKey("power_profile")
        val BACKGROUND_PLAY_ENABLED = booleanPreferencesKey("background_play_enabled")
        val AUTO_ECO_ON_SAVER = booleanPreferencesKey("auto_eco_on_saver")
        val SEEK_STEP_SECONDS = intPreferencesKey("seek_step_seconds")
        val AUTO_RESUME = booleanPreferencesKey("auto_resume")
        val SUB_FONT_SIZE = floatPreferencesKey("sub_font_size")
        val SUB_TEXT_COLOR = longPreferencesKey("sub_text_color")
        val SUB_BG_OPACITY = floatPreferencesKey("sub_bg_opacity")
        val SUB_OUTLINE = booleanPreferencesKey("sub_outline")
        val SUB_BOTTOM_MARGIN = floatPreferencesKey("sub_bottom_margin")
    }

    val playbackPrefs: Flow<PlaybackPrefs> = context.kinoXPrefsDataStore.data.map { prefs ->
        PlaybackPrefs(
            defaultDecoderMode = DecoderMode.fromName(prefs[Keys.DEFAULT_DECODER]),
            powerProfile = PowerProfile.fromName(prefs[Keys.POWER_PROFILE]),
            backgroundPlayEnabled = prefs[Keys.BACKGROUND_PLAY_ENABLED] ?: true,
            autoEcoOnBatterySaver = prefs[Keys.AUTO_ECO_ON_SAVER] ?: true,
            seekStepSeconds = prefs[Keys.SEEK_STEP_SECONDS] ?: 10,
            autoResume = prefs[Keys.AUTO_RESUME] ?: true,
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

    suspend fun updatePowerProfile(profile: PowerProfile) {
        context.kinoXPrefsDataStore.edit {
            it[Keys.POWER_PROFILE] = profile.name
            if (profile == PowerProfile.ECO) {
                it[Keys.DEFAULT_DECODER] = DecoderMode.HW.name
            }
        }
    }

    suspend fun updateBackgroundPlayEnabled(enabled: Boolean) {
        context.kinoXPrefsDataStore.edit { it[Keys.BACKGROUND_PLAY_ENABLED] = enabled }
    }

    suspend fun updateAutoEcoOnBatterySaver(enabled: Boolean) {
        context.kinoXPrefsDataStore.edit { it[Keys.AUTO_ECO_ON_SAVER] = enabled }
    }

    suspend fun updateAutoResume(enabled: Boolean) {
        context.kinoXPrefsDataStore.edit { it[Keys.AUTO_RESUME] = enabled }
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
}
