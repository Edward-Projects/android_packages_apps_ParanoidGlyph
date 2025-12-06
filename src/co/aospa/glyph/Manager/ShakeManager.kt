/*
 * Copyright (C) 2024-2025 LunarisAOSP
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

object ShakeManager {
    private const val TAG = "GlyphShakeManager"
    private const val DEBUG = false

    /**
     * Start the shake detector service if enabled in settings
     * @param context Application context
     */
    fun startShakeService(context: Context?) {
        if (context == null) {
            Log.e(TAG, "Context is null, cannot start service")
            return
        }


        // IMPORTANT: Use isGlyphEnabledIgnoreSchedule() so shake-to-torch works during schedule
        if (isShakeEnabled(context) && SettingsManager.isGlyphEnabledIgnoreSchedule()) {
            try {
                val serviceIntent: Intent = Intent(context, ShakeDetectorService::class.java)
                context.startService(serviceIntent)
                if (DEBUG) Log.d(TAG, "Shake service start requested")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start shake service", e)
            }
        } else {
            if (DEBUG) Log.d(TAG, "Shake or Glyph disabled, not starting service")
        }
    }

    /**
     * Stop the shake detector service
     * @param context Application context
     */
    fun stopShakeService(context: Context?) {
        if (context == null) {
            Log.e(TAG, "Context is null, cannot stop service")
            return
        }

        try {
            val serviceIntent: Intent = Intent(context, ShakeDetectorService::class.java)
            context.stopService(serviceIntent)
            if (DEBUG) Log.d(TAG, "Shake service stop requested")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop shake service", e)
        }
    }

    /**
     * Restart the shake detector service (useful when settings change)
     * @param context Application context
     */
    fun restartShakeService(context: Context?) {
        if (DEBUG) Log.d(TAG, "Restarting shake service")
        stopShakeService(context)

        try {
            Thread.sleep(100)
        } catch (e: InterruptedException) {
        }

        startShakeService(context)
    }

    /**
     * Check if shake gesture is enabled in preferences
     * @param context Application context
     * @return true if shake is enabled, false otherwise
     */
    fun isShakeEnabled(context: Context?): Boolean {
        if (context == null) {
            return false
        }

        try {
            val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return prefs.getBoolean(Constants.GLYPH_SHAKE_TORCH_ENABLE, false)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read shake preference", e)
            return false
        }
    }

    /**
     * Enable or disable shake gesture
     * @param context Application context
     * @param enabled true to enable, false to disable
     */
    fun setShakeEnabled(context: Context?, enabled: Boolean) {
        if (context == null) {
            return
        }

        try {
            val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            prefs.edit().putBoolean(Constants.GLYPH_SHAKE_TORCH_ENABLE, enabled).apply()

            if (enabled) {
                startShakeService(context)
            } else {
                stopShakeService(context)
            }

            if (DEBUG) Log.d(TAG, "Shake gesture " + (if (enabled) "enabled" else "disabled"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set shake preference", e)
        }
    }
}