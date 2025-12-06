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

import android.content.BroadcastReceiver
import co.aospa.glyph.Constants.Constants
import co.aospa.glyph.Manager.SettingsManager
import co.aospa.glyph.Utils.FileUtils

class TorchTileService : TileService() {
    private val mUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        public override fun onReceive(context: Context?, intent: Intent?) {
            updateState()
        }
    }

    public override fun onCreate() {
        super.onCreate()
        val filter: IntentFilter = IntentFilter(ACTION_UPDATE_TILE)
        registerReceiver(mUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    public override fun onDestroy() {
        try {
            unregisterReceiver(mUpdateReceiver)
        } catch (e: Exception) {
        }
        super.onDestroy()
    }

    public override fun onStartListening() {
        super.onStartListening()
        updateState()
    }

    private fun updateState() {
        if (Constants.CONTEXT == null) {
            Constants.CONTEXT = getApplicationContext()
        }

        val glyphEnabled = SettingsManager.isGlyphEnabledIgnoreSchedule()

        if (!glyphEnabled) {
            getQsTile().setState(Tile.STATE_UNAVAILABLE)
            getQsTile().setSubtitle(getString(R.string.glyph_accessibility_quick_settings_disabled))
            getQsTile().updateTile()
            return
        }

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
        if (!SettingsManager.isGlyphEnabledIgnoreSchedule()) {
            return
        }
        this.enabled = !this.enabled
        updateState()
    }

    private var enabled: Boolean
        get() = StatusManager.isAllLedActive()
        private set(enabled) {
            StatusManager.setAllLedsActive(enabled)
            FileUtils.writeAllLed(if (enabled) Constants.getMaxBrightness() else 0)
            if (StatusManager.isEssentialLedActive() && !enabled) FileUtils.writeSingleLed(
                ResourceUtils.getInteger("glyph_settings_notifs_essential_led"),
                (Constants.getMaxBrightness() / 100 * 7).toFloat()
            )
        }

    companion object {
        private const val TAG = "GlyphTorchTile"

        private const val ACTION_UPDATE_TILE = "co.aospa.glyph.UPDATE_TORCH_TILE"

        fun requestTileUpdate(context: Context) {
            val intent: Intent = Intent(ACTION_UPDATE_TILE)
            context.sendBroadcast(intent)
        }
    }
}