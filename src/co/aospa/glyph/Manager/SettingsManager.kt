/*
 * Copyright (C) 2022-2024 Paranoid Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package co.aospa.glyph.Manager

import android.content.Context
import co.aospa.glyph.Constants.Constants
import co.aospa.glyph.Utils.FileUtils

object SettingsManager {
    private const val TAG = "GlyphSettingsManager"
    private const val DEBUG = true

    private val context: Context = Constants.CONTEXT

    fun enableGlyph(enable: Boolean): Boolean {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putBoolean(Constants.GLYPH_ENABLE, enable).apply()

        return Settings.Secure.putInt(
            context.getContentResolver(),
            Constants.GLYPH_ENABLE, if (enable) 1 else 0
        )
    }

    val isGlyphEnabled: Boolean
        get() {
            val baseEnabled = (Settings.Secure.getInt(
                context.getContentResolver(),
                Constants.GLYPH_ENABLE, 1
            ) !== 0
                    || PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(Constants.GLYPH_ENABLE, false))

            if (GlyphScheduleManager.isScheduleEnabled(context) &&
                GlyphScheduleManager.isScheduleCurrentlyActive(context)
            ) {
                return false
            }

            return baseEnabled
        }

    val isGlyphEnabledIgnoreSchedule: Boolean
        get() = (Settings.Secure.getInt(
            context.getContentResolver(),
            Constants.GLYPH_ENABLE, 1
        ) !== 0
                || PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(Constants.GLYPH_ENABLE, false))

    val isGlyphFlipEnabled: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(
                Constants.GLYPH_FLIP_ENABLE,
                false
            ) && isGlyphEnabled

    val glyphBrightness: Int
        get() {
            val levels = Constants.getBrightnessLevels()
            val brightnessSetting: Int =
                glyphBrightnessSetting
            return levels[brightnessSetting - 1]
        }

    val glyphBrightnessSetting: Int
        get() {
            var d = 3
            if (FileUtils.readLine("/mnt/vendor/persist/color") === "white") d =
                2
            return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(Constants.GLYPH_BRIGHTNESS, d)
        }

    val isGlyphChargingEnabled: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(
                Constants.GLYPH_CHARGING_LEVEL_ENABLE,
                false
            ) && isGlyphEnabled

    val isGlyphPowershareEnabled: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(
                Constants.GLYPH_CHARGING_POWERSHARE_ENABLE,
                false
            ) && isGlyphEnabled

    val isGlyphCallEnabled: Boolean
        get() = Settings.Secure.getInt(
            context.getContentResolver(),
            Constants.GLYPH_CALL_ENABLE, 1
        ) !== 0 && isGlyphEnabled

    fun setGlyphCallEnabled(enable: Boolean): Boolean {
        return Settings.Secure.putInt(
            context.getContentResolver(),
            Constants.GLYPH_CALL_ENABLE, if (enable) 1 else 0
        )
    }

    val glyphCallAnimation: String
        get() = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(
                Constants.GLYPH_CALL_SUB_ANIMATIONS,
                ResourceUtils.getString("glyph_settings_call_animations_default")
            )

    val isGlyphMusicVisualizerEnabled: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(
                Constants.GLYPH_MUSIC_VISUALIZER_ENABLE,
                false
            ) && isGlyphEnabled

    val isGlyphVolumeLevelEnabled: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(
                Constants.GLYPH_VOLUME_LEVEL_ENABLE,
                false
            ) && isGlyphEnabled

    val isGlyphNotifsEnabled: Boolean
        get() = Settings.Secure.getInt(
            context.getContentResolver(),
            Constants.GLYPH_NOTIFS_ENABLE, 1
        ) !== 0 && isGlyphEnabled

    fun setGlyphNotifsEnabled(enable: Boolean): Boolean {
        return Settings.Secure.putInt(
            context.getContentResolver(),
            Constants.GLYPH_NOTIFS_ENABLE, if (enable) 1 else 0
        )
    }

    val glyphNotifsAnimation: String
        get() = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(
                Constants.GLYPH_NOTIFS_SUB_ANIMATIONS,
                ResourceUtils.getString("glyph_settings_notifs_animations_default")
            )

    fun isGlyphNotifsAppEnabled(app: String?): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(app, true) && isGlyphNotifsEnabled
    }

    fun isGlyphNotifsAppEssential(app: String?): Boolean {
        val selectedValues: MutableSet<String?> =
            PreferenceManager.getDefaultSharedPreferences(context)
                .getStringSet(Constants.GLYPH_NOTIFS_SUB_ESSENTIAL, HashSet<String?>())
        return selectedValues.contains(app) && isGlyphNotifsEnabled
    }

    val isGlyphAutoBrightnessEnabled: Boolean
        get() = !ResourceUtils.getString("glyph_light_sensor")
            .isBlank() && PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(Constants.GLYPH_AUTO_BRIGHTNESS_ENABLE, false)
                && isGlyphEnabled

    val flipRingerMode: Int
        get() = Settings.Secure.getInt(
            Constants.CONTEXT.getContentResolver(),
            Constants.GLYPH_FLIP_RINGER_MODE,
            AudioManager.RINGER_MODE_VIBRATE
        )

    var isGlyphComposerEnabled: Boolean
        get() = Settings.Secure.getInt(
            Constants.CONTEXT.getContentResolver(),
            Constants.GLYPH_COMPOSER_ENABLE, 1
        ) === 1
        set(enabled) {
            Settings.Secure.putInt(
                Constants.CONTEXT.getContentResolver(),
                Constants.GLYPH_COMPOSER_ENABLE, if (enabled) 1 else 0
            )
        }

    fun useComposerFallback(): Boolean {
        return Settings.Secure.getInt(
            Constants.CONTEXT.getContentResolver(),
            Constants.GLYPH_COMPOSER_FALLBACK, 1
        ) === 1
    }
}
