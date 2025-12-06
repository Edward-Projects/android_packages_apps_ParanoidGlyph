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
package co.aospa.glyph.Constants

import android.content.Context

object Constants {
    private const val TAG = "GlyphConstants"
    private const val DEBUG = true

    var CONTEXT: Context? = null
    const val MAX_PATTERN_BRIGHTNESS: Int = 4095

    var device: String? = null
        get() {
            if (field == null) field = ResourceUtils.getString("glyph_settings_device")

            return field
        }
        private set

    private var brightness = -1
    private var brightnessMax = -1
    var brightnessLevels: IntArray? = null
        get() {
            if (field == null) field = ResourceUtils.getIntArray("glyph_settings_brightness_levels")

            return field
        }
        private set
    var supportedAnimationPatternLengths: IntArray? = null
        get() {
            if (field == null) field =
                ResourceUtils.getIntArray("glyph_settings_animations_supported_pattern_lengths")

            return field
        }
        private set

    const val GLYPH_ENABLE: String = "glyph_enable"
    const val GLYPH_FLIP_ENABLE: String = "glyph_settings_flip_toggle"
    const val GLYPH_BRIGHTNESS: String = "glyph_settings_brightness"
    const val GLYPH_CHARGING_CATEGORY: String = "glyph_settings_charging"
    const val GLYPH_CHARGING_LEVEL_ENABLE: String = "glyph_settings_charging_level"
    const val GLYPH_CHARGING_POWERSHARE_ENABLE: String = "glyph_settings_charging_powershare"
    const val GLYPH_CALL_CATEGORY: String = "glyph_settings_call"
    const val GLYPH_CALL_ENABLE: String = "glyph_settings_call_toggle"
    const val GLYPH_CALL_SUB_PREVIEW: String = "glyph_settings_call_sub_preview"
    const val GLYPH_CALL_SUB_ANIMATIONS: String = "glyph_settings_call_sub_animations"
    const val GLYPH_CALL_SUB_ENABLE: String = "glyph_settings_call_sub_toggle"
    const val GLYPH_MUSIC_VISUALIZER_ENABLE: String = "glyph_settings_music_visualizer_toggle"
    const val GLYPH_NOTIFS_ENABLE: String = "glyph_settings_notifs_toggle"
    const val GLYPH_NOTIFS_SUB_PREVIEW: String = "glyph_settings_notifs_sub_preview"
    const val GLYPH_NOTIFS_SUB_ANIMATIONS: String = "glyph_settings_notifs_sub_animations"
    const val GLYPH_NOTIFS_SUB_ESSENTIAL: String = "glyph_settings_notifs_sub_essential"
    const val GLYPH_NOTIFS_SUB_CATEGORY: String = "glyph_settings_notifs_sub"
    const val GLYPH_NOTIFS_SUB_ENABLE: String = "glyph_settings_notifs_sub_toggle"
    const val GLYPH_VOLUME_LEVEL_ENABLE: String = "glyph_settings_volume_level_toggle"
    const val GLYPH_AUTO_BRIGHTNESS_ENABLE: String = "glyph_settings_auto_brightness_toggle"
    const val GLYPH_SHAKE_TORCH_ENABLE: String = "glyph_settings_shake_torch_toggle"
    const val GLYPH_SHAKE_SENSITIVITY: String = "glyph_settings_shake_sensitivity"
    const val GLYPH_FLIP_RINGER_MODE: String = "glyph_settings_flip_ringer_mode"
    const val GLYPH_COMPOSER_ENABLE: String = "glyph_settings_composer_enable"
    const val GLYPH_COMPOSER_FALLBACK: String = "glyph_settings_composer_fallback"
    const val GLYPH_COMPOSER_PREVIEW: String = "glyph_settings_composer_preview"
    const val GLYPH_SCHEDULE: String = "glyph_settings_schedule"

    val APPS_TO_IGNORE: Array<String?> = arrayOf<String?>(
        "android",
        "com.android.traceur",  //"com.google.android.dialer",
        "com.google.android.setupwizard",
        "dev.kdrag0n.dyntheme.privileged.sys"
    )
    val NOTIFS_TO_IGNORE: Array<String?> = arrayOf<String?>(
        "com.google.android.dialer:phone_incoming_call",
        "com.google.android.dialer:phone_ongoing_call",
        "com.android.systemui:BAT"
    )

    fun setBrightness(b: Int): Boolean {
        if (b > ResourceUtils.getInteger("glyph_settings_brightness_max")) return false

        brightness = b
        return true
    }

    fun getBrightness(): Int {
        if (brightness == -1) brightness = ResourceUtils.getInteger("glyph_settings_brightness_max")

        return brightness
    }

    val maxBrightness: Int
        get() {
            if (brightnessMax == -1) brightnessMax =
                ResourceUtils.getInteger("glyph_settings_brightness_max")

            return brightnessMax
        }
}