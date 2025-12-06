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
package co.aospa.glyph.Utils

import android.content.Context
import co.aospa.glyph.Constants.Constants
import co.aospa.glyph.Manager.SettingsManager

object ServiceUtils {
    private const val TAG = "GlyphServiceUtils"
    private const val DEBUG = true

    private val context: Context = Constants.CONTEXT

    private val CHARGING_LEVELS = ResourceUtils.getInteger("glyph_settings_battery_levels_num")
    private val glyphChargingMeterAvailable = CHARGING_LEVELS > 0

    private val POWERSHARE_ACTIVE: String =
        ResourceUtils.getString("glyph_settings_paths_powershare_active_absolute")
    private val POWERSHARE_ENABLED: String =
        ResourceUtils.getString("glyph_settings_paths_powershare_enabled_absolute")
    private val glyphPowershareAvailable =
        !POWERSHARE_ACTIVE.isBlank() && !POWERSHARE_ENABLED.isBlank()

    val isNotificationServiceEnabled: Boolean
        get() {
            val pkgName: String? = context.getPackageName()
            val flat: String? = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_NOTIFICATION_LISTENERS
            )
            if (flat != null) {
                val names: Array<String?> =
                    flat.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (name in names) {
                    val cn: ComponentName? = ComponentName.unflattenFromString(name)
                    if (cn != null && TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true
                    }
                }
            }
            return false
        }

    private fun startCallReceiverService() {
        if (DEBUG) Log.d(TAG, "Starting Glyph call receiver service")
        context.startServiceAsUser(
            Intent(context, CallReceiverService::class.java),
            UserHandle.CURRENT
        )
    }

    private fun stopCallReceiverService() {
        if (DEBUG) Log.d(TAG, "Stopping Glyph call receiver service")
        context.stopServiceAsUser(
            Intent(context, CallReceiverService::class.java),
            UserHandle.CURRENT
        )
    }

    private fun startChargingService() {
        if (DEBUG) Log.d(TAG, "Starting Glyph charging service")
        if (!glyphChargingMeterAvailable) return
        context.startServiceAsUser(
            Intent(context, ChargingService::class.java),
            UserHandle.CURRENT
        )
    }

    private fun stopChargingService() {
        if (DEBUG) Log.d(TAG, "Stopping Glyph charging service")
        context.stopServiceAsUser(
            Intent(context, ChargingService::class.java),
            UserHandle.CURRENT
        )
    }

    private fun startFlipToGlyphService() {
        if (DEBUG) Log.d(TAG, "Starting Flip to Glyph service")
        context.startServiceAsUser(
            Intent(context, FlipToGlyphService::class.java),
            UserHandle.CURRENT
        )
    }

    private fun stopFlipToGlyphService() {
        if (DEBUG) Log.d(TAG, "Stopping Flip to Glyph service")
        context.stopServiceAsUser(
            Intent(context, FlipToGlyphService::class.java),
            UserHandle.CURRENT
        )
    }

    fun startMusicVisualizerService() {
        if (DEBUG) Log.d(TAG, "Starting Music Visualizer service")
        context.startServiceAsUser(
            Intent(context, MusicVisualizerService::class.java),
            UserHandle.CURRENT
        )
    }

    internal fun stopMusicVisualizerService() {
        if (DEBUG) Log.d(TAG, "Stopping Music Visualizer service")
        context.stopServiceAsUser(
            Intent(context, MusicVisualizerService::class.java),
            UserHandle.CURRENT
        )
    }

    private fun startPowershareService() {
        if (DEBUG) Log.d(TAG, "Starting Glyph powershare service")
        if (!glyphPowershareAvailable) return
        context.startServiceAsUser(
            Intent(context, PowershareService::class.java),
            UserHandle.CURRENT
        )
    }

    private fun stopPowershareService() {
        if (DEBUG) Log.d(TAG, "Stopping Glyph powershare service")
        context.stopServiceAsUser(
            Intent(context, PowershareService::class.java),
            UserHandle.CURRENT
        )
    }

    fun startVolumeLevelService() {
        if (DEBUG) Log.d(TAG, "Starting Volume Level service")
        context.startServiceAsUser(
            Intent(context, VolumeLevelService::class.java),
            UserHandle.CURRENT
        )
    }

    internal fun stopVolumeLevelService() {
        if (DEBUG) Log.d(TAG, "Stopping Volume Listener service")
        context.stopServiceAsUser(
            Intent(context, VolumeLevelService::class.java),
            UserHandle.CURRENT
        )
    }

    private fun startAutoBrightnessService() {
        if (DEBUG) Log.d(TAG, "Starting Auto Brightness service")
        context.startServiceAsUser(
            Intent(context, AutoBrightnessService::class.java),
            UserHandle.CURRENT
        )
    }

    private fun stopAutoBrightnessService() {
        if (DEBUG) Log.d(TAG, "Stopping Auto Brightness service")
        context.stopServiceAsUser(
            Intent(context, AutoBrightnessService::class.java),
            UserHandle.CURRENT
        )
    }

    fun startThirdPartyService() {
        if (DEBUG) Log.d(TAG, "Starting ThirdParty service")
        context.startServiceAsUser(
            Intent(context, ThirdPartyService::class.java),
            UserHandle.CURRENT
        )
    }

    internal fun stopThirdPartyService() {
        if (DEBUG) Log.d(TAG, "Stopping ThirdParty service")
        context.stopServiceAsUser(
            Intent(context, ThirdPartyService::class.java),
            UserHandle.CURRENT
        )
    }

    fun checkGlyphService() {
        if (SettingsManager.getGlyphBrightness() != Constants.getBrightness()) {
            Constants.setBrightness(SettingsManager.getGlyphBrightness())
            startThirdPartyService()
            if (StatusManager.isEssentialLedActive()) AnimationManager.playEssential()
        }

        val glyphEnabled = SettingsManager.isGlyphEnabled()

        val glyphBaseEnabled = SettingsManager.isGlyphEnabledIgnoreSchedule()

        if (glyphEnabled) {
            if (SettingsManager.isGlyphChargingEnabled()) {
                startChargingService()
            } else {
                stopChargingService()
            }
            if (SettingsManager.isGlyphPowershareEnabled()) {
                startPowershareService()
            } else {
                stopPowershareService()
            }
            if (SettingsManager.isGlyphCallEnabled()) {
                startCallReceiverService()
            } else {
                stopCallReceiverService()
            }
            if (SettingsManager.isGlyphFlipEnabled()) {
                startFlipToGlyphService()
            } else {
                stopFlipToGlyphService()
            }
            if (SettingsManager.isGlyphMusicVisualizerEnabled()) {
                startMusicVisualizerService()
            } else {
                stopMusicVisualizerService()
            }
            if (SettingsManager.isGlyphVolumeLevelEnabled()) {
                startVolumeLevelService()
            } else {
                stopVolumeLevelService()
            }
            if (SettingsManager.isGlyphAutoBrightnessEnabled()) {
                startAutoBrightnessService()
            } else {
                stopAutoBrightnessService()
            }
        } else {
            stopChargingService()
            stopPowershareService()
            stopCallReceiverService()
            stopFlipToGlyphService()
            stopMusicVisualizerService()
            stopVolumeLevelService()
            stopAutoBrightnessService()
        }

        if (glyphBaseEnabled && ShakeManager.isShakeEnabled(context)) {
            ShakeManager.startShakeService(context)
        } else {
            ShakeManager.stopShakeService(context)
        }

        if (glyphBaseEnabled) {
            startThirdPartyService()
        } else {
            stopThirdPartyService()
        }
    }
}