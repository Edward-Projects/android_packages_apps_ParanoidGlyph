/*
 * Copyright (C) 2015 The CyanogenMod Project
 *               2017-2019 The LineageOS Project
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
package co.aospa.glyph

import android.content.BroadcastReceiver
import co.aospa.glyph.Constants.Constants

class BootCompletedReceiver : BroadcastReceiver() {
    public override fun onReceive(context: Context, intent: Intent?) {
        if (DEBUG) Log.d(TAG, "Received boot completed intent")
        Constants.CONTEXT = context.getApplicationContext()

        if (GlyphScheduleManager.isScheduleEnabled(context)) {
            GlyphScheduleManager.setupScheduleAlarms(context)
            if (DEBUG) Log.d(TAG, "Schedule alarms restored on boot")
        }

        ServiceUtils.checkGlyphService()

        if (ShakeManager.isShakeEnabled(context)) {
            ShakeManager.startShakeService(context)
            if (DEBUG) Log.d(TAG, "Shake service started on boot")
        }
    }

    companion object {
        private const val DEBUG = true
        private const val TAG = "ParanoidGlyph"
    }
}