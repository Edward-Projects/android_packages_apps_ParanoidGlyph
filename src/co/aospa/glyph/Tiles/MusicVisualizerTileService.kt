/*
 * Copyright (C) 2015 The CyanogenMod Project
 * Copyright (C) 2017 The LineageOS Project
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
package co.aospa.glyph.Tiles

import co.aospa.glyph.Constants.Constants
import co.aospa.glyph.Manager.SettingsManager

/** Quick settings tile: Glyph  */
class MusicVisualizerTileService : TileService() {
    private var mContentResolver: ContentResolver? = null
    private var mSettingObserver: SettingObserver? = null

    public override fun onCreate() {
        mContentResolver = this.getContentResolver()
        mSettingObserver = MusicVisualizerTileService.SettingObserver()
        mSettingObserver!!.register(mContentResolver)
    }

    public override fun onStartListening() {
        super.onStartListening()
        updateState()
    }

    private fun updateState() {
        if (!SettingsManager.isGlyphEnabled()) {
            getQsTile().setSubtitle(getString(R.string.glyph_accessibility_quick_settings_unavailable))
            getQsTile().setState(Tile.STATE_INACTIVE)
        } else {
            val enabled = this.enabled
            getQsTile().setSubtitle(
                if (enabled) getString(R.string.glyph_accessibility_quick_settings_on) else getString(
                    R.string.glyph_accessibility_quick_settings_off
                )
            )
            getQsTile().setState(if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE)
        }
        getQsTile().updateTile()
    }

    public override fun onClick() {
        super.onClick()
        if (SettingsManager.isGlyphEnabled()) {
            this.enabled = !this.enabled
            updateState()
        }
    }

    private var enabled: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean(Constants.GLYPH_MUSIC_VISUALIZER_ENABLE, false)
        private set(enabled) {
            val sharedPrefs: SharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this)
            sharedPrefs.edit().putBoolean(
                Constants.GLYPH_MUSIC_VISUALIZER_ENABLE,
                enabled
            ).apply()
            ServiceUtils.checkGlyphService()
        }

    public override fun onDestroy() {
        mSettingObserver!!.unregister(mContentResolver)
        super.onDestroy()
    }

    private inner class SettingObserver : ContentObserver(Handler()) {
        fun register(cr: ContentResolver) {
            cr.registerContentObserver(
                Settings.Secure.getUriFor(
                    Constants.GLYPH_ENABLE
                ), false, this
            )
        }

        fun unregister(cr: ContentResolver) {
            cr.unregisterContentObserver(this)
        }

        public override fun onChange(selfChange: Boolean, uri: Uri) {
            super.onChange(selfChange, uri)
            if (uri.equals(Settings.Secure.getUriFor(Constants.GLYPH_ENABLE))) {
                updateState()
            }
        }
    }
}
