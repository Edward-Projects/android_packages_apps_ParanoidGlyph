/*
 * Copyright (C) 2015 The CyanogenMod Project
 *               2017-2018 The LineageOS Project
 *               2020-2024 Paranoid Android
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
package co.aospa.glyph.Services

import android.app.Service
import co.aospa.glyph.Manager.SettingsManager
import java.util.function.Consumer

class FlipToGlyphService : Service() {
    private var isFlipped = false
    private var ringerMode = 0

    private var mAudioManager: AudioManager? = null
    private var mFlipToGlyphSensor: FlipToGlyphSensor? = null
    private var mContext: Context? = null

    public override fun onCreate() {
        if (DEBUG) Log.d(TAG, "Creating service")

        mContext = this
        mFlipToGlyphSensor =
            FlipToGlyphSensor(this, Consumer { flipped: Boolean? -> this.onFlip(flipped!!) })
        mAudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    public override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "Starting service")
        mFlipToGlyphSensor.enable()
        return START_STICKY
    }

    public override fun onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying service")
        mFlipToGlyphSensor.disable()
        super.onDestroy()
    }

    public override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun onFlip(flipped: Boolean) {
        if (flipped == isFlipped) return
        if (DEBUG) Log.d(TAG, "Flipped: " + flipped)
        if (flipped) {
            AnimationManager.playCsv(mContext, "flip")
            ringerMode = mAudioManager.getRingerModeInternal()
            val preferredMode = SettingsManager.getFlipRingerMode()
            if (DEBUG) Log.d(TAG, "Preferred ringer mode: " + preferredMode)

            if (preferredMode != -1) {
                if (DEBUG) Log.d(TAG, "Setting ringer mode to: " + preferredMode)
                mAudioManager.setRingerModeInternal(preferredMode)
            } else {
                if (DEBUG) Log.d(TAG, "Following system ringer mode: " + ringerMode)
            }
        } else {
            val preferredMode = SettingsManager.getFlipRingerMode()
            if (preferredMode != -1) {
                mAudioManager.setRingerModeInternal(ringerMode)
            }
        }
        isFlipped = flipped
    }

    companion object {
        private const val TAG = "FlipToGlyphService"
        private const val DEBUG = true
    }
}