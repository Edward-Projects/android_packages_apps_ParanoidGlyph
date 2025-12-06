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

import co.aospa.glyph.Utils.ResourceUtils.getInteger

object StatusManager {
    private const val TAG = "GlyphStatusManager"
    private const val DEBUG = true

    var isAllLedActive: Boolean = false
        private set
    var isAnimationActive: Boolean = false
    var isChargingAnimationActive: Boolean = false
    var isVolumeAnimationActive: Boolean = false
    var isCallLedActive: Boolean = false
    var isEssentialLedActive: Boolean = false
    var chargingLedLast: Int = 0
    var batteryArray: IntArray? = IntArray(getInteger("glyph_settings_battery_levels_num"))
    var volumeLedLast: Int = 0
    var volumeArray: IntArray? = IntArray(getInteger("glyph_settings_volume_levels_num"))

    var isCallLedEnabled: Boolean = false

    fun setAllLedsActive(status: Boolean) {
        isAllLedActive = status
    }

    val isGlyphIdle: Boolean
        get() {
            if (isAllLedActive || isCallLedActive || isAnimationActive
                || isChargingAnimationActive || isVolumeAnimationActive || isCallLedEnabled
            ) {
                return false
            } else {
                return true
            }
        }
}
