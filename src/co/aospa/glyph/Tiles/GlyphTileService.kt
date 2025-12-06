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

import android.service.quicksettings.Tile
import co.aospa.glyph.Manager.SettingsManager

/** Quick settings tile: Glyph  */
class GlyphTileService : TileService() {
    public override fun onStartListening() {
        super.onStartListening()
        updateState()
    }

    private fun updateState() {
        val enabled = this.enabled
        getQsTile().setSubtitle(
            if (enabled) getString(R.string.glyph_accessibility_quick_settings_on) else getString(
                R.string.glyph_accessibility_quick_settings_off
            )
        )
        getQsTile().setState(if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE)
        getQsTile().updateTile()
    }

    public override fun onClick() {
        super.onClick()
        this.enabled = !this.enabled
        updateState()
    }

    private var enabled: Boolean
        get() = SettingsManager.isGlyphEnabled()
        private set(enabled) {
            SettingsManager.enableGlyph(enabled)
            ServiceUtils.checkGlyphService()
        }
}
