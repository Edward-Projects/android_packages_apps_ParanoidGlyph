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
import java.io.InputStreamReader
import kotlin.math.floor

object AnimationManager {
    private const val TAG = "GlyphAnimationManager"
    private const val DEBUG = true
    private var sWakeLock: PowerManager.WakeLock? = null

    private fun acquireWakeLock(context: Context) {
        if (sWakeLock == null) {
            val pm: PowerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            sWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG)
            sWakeLock.acquire()
            if (DEBUG) Log.d(TAG, "Acquired wakelock")
        }
    }

    private fun releaseWakeLock() {
        if (sWakeLock != null) {
            sWakeLock.release()
            sWakeLock = null
            if (DEBUG) Log.d(TAG, "Released wakelock")
        }
    }

    private fun submit(runnable: Runnable): Future<*> {
        val executorService: ExecutorService = Executors.newSingleThreadExecutor()
        return executorService.submit(runnable)
    }

    private fun check(name: String?, wait: Boolean): Boolean {
        if (DEBUG) Log.d(
            TAG,
            "Playing animation | name: " + name + " | waiting: " + wait.toString()
        )

        if (StatusManager.isAllLedActive()) {
            if (DEBUG) Log.d(TAG, "All LEDs are active, exiting animation | name: " + name)
            return false
        }

        if (StatusManager.isCallLedActive()) {
            if (DEBUG) Log.d(
                TAG,
                "Call animation is currently active, exiting animation | name: " + name
            )
            return false
        }

        if (StatusManager.isAnimationActive()) {
            val start = System.currentTimeMillis()
            if (wait) {
                if (DEBUG) Log.d(TAG, "There is already an animation playing, wait | name: " + name)
                while (StatusManager.isAnimationActive()) {
                    if (System.currentTimeMillis() - start >= 2500) return false
                }
            } else {
                if (DEBUG) Log.d(
                    TAG,
                    "There is already an animation playing, exiting | name: " + name
                )
                return false
            }
        }

        return true
    }

    private fun checkInterruption(name: String?): Boolean {
        if (StatusManager.isAllLedActive()
            || (name !== "call" && StatusManager.isCallLedEnabled())
            || (name === "call" && !StatusManager.isCallLedEnabled())
        ) {
            return true
        }
        return false
    }

    fun playCsv(context: Context, name: String?) {
        playCsv(context, name, false)
    }

    fun playCsv(context: Context, name: String?, wait: Boolean) {
        submit(Runnable {
            if (!check(name, wait)) return@submit
            acquireWakeLock(context)

            StatusManager.setAnimationActive(true)
            try {
                BufferedReader(
                    InputStreamReader(
                        ResourceUtils.getAnimation(name)
                    )
                ).use { reader ->
                    var line: String?
                    while ((reader.readLine().also { line = it }) != null) {
                        if (checkInterruption("csv")) throw InterruptedException()
                        line = line!!.replace(" ", "")
                        line = if (line.endsWith(",")) line.substring(0, line.length - 1) else line
                        val pattern: Array<String?> =
                            line.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        if (ArrayUtils.contains(
                                Constants.getSupportedAnimationPatternLengths(),
                                pattern.size
                            )
                        ) {
                            updateLedFrame(pattern)
                        } else {
                            if (DEBUG) Log.d(
                                TAG,
                                "Animation line length mismatch | name: " + name + " | line: " + line
                            )
                            throw InterruptedException()
                        }
                        Thread.sleep(16, 666000)
                    }
                }
            } catch (e: Exception) {
                if (DEBUG) Log.d(
                    TAG,
                    "Exception while playing animation | name: " + name + " | exception: " + e
                )
            } finally {
                updateLedFrame(FloatArray(5))
                StatusManager.setAnimationActive(false)
                if (DEBUG) Log.d(TAG, "Done playing animation | name: " + name)
                releaseWakeLock()
            }
        })
    }

    fun playCharging(batteryLevel: Int, wait: Boolean) {
        if (!check("charging", wait)) return

        StatusManager.setAnimationActive(true)
        StatusManager.setChargingAnimationActive(true)

        var batteryArray = StatusManager.getBatteryArray()
        val amount = floor((batteryLevel / 100.0) * batteryArray.size).toInt()
        val last = StatusManager.getChargingLedLast()
        val next = amount - 1

        try {
            if (last <= next) {
                for (i in last..next) {
                    if (checkInterruption("charging")) throw InterruptedException()
                    StatusManager.setChargingLedLast(i)
                    batteryArray[i] = Constants.MAX_PATTERN_BRIGHTNESS
                    updateLedFrame(batteryArray)
                    Thread.sleep(16, 666000)
                }
            } else if (last > next) {
                for (i in last downTo next + 1) {
                    if (checkInterruption("charging")) throw InterruptedException()
                    StatusManager.setChargingLedLast(i)
                    batteryArray[i] = 0
                    updateLedFrame(batteryArray)
                    Thread.sleep(16, 666000)
                }
            }
        } catch (e: InterruptedException) {
            if (DEBUG) Log.d(TAG, "Exception while playing animation, interrupted | name: charging")
            if (!StatusManager.isAllLedActive()) {
                StatusManager.setChargingLedLast(0)
                batteryArray =
                    IntArray(ResourceUtils.getInteger("glyph_settings_battery_levels_num"))
                updateLedFrame(batteryArray)
            }
        } finally {
            StatusManager.setAnimationActive(false)
            StatusManager.setBatteryArray(batteryArray)
            if (DEBUG) Log.d(TAG, "Done playing animation | name: charging")
        }
    }

    fun dismissCharging() {
        val emptyArray = IntArray(ResourceUtils.getInteger("glyph_settings_battery_levels_num"))
        val batteryArray = StatusManager.getBatteryArray()

        if (emptyArray.contentEquals(batteryArray)) return

        if (!check("Dismiss charging", false)) return

        StatusManager.setAnimationActive(true)

        try {
            if (checkInterruption("Dismiss charging")) throw InterruptedException()
            for (i in batteryArray.indices.reversed()) {
                if (checkInterruption("Dismiss charging")) throw InterruptedException()
                if (batteryArray[i] != 0) {
                    StatusManager.setChargingLedLast(i)
                    batteryArray[i] = 0
                    updateLedFrame(batteryArray)
                    Thread.sleep(16, 666000)
                }
            }
        } catch (e: InterruptedException) {
            if (DEBUG) Log.d(
                TAG,
                "Exception while playing animation, interrupted | name: Dismiss charging"
            )
            if (!StatusManager.isAllLedActive()) updateLedFrame(IntArray(batteryArray.size))
        } finally {
            StatusManager.setChargingLedLast(0)
            StatusManager.setChargingAnimationActive(false)
            StatusManager.setAnimationActive(false)
            if (DEBUG) Log.d(TAG, "Done playing animation | name: Dismiss charging")
        }
    }

    fun playVolume(context: Context, volumeLevel: Int, wait: Boolean) {
        if (!check("volume", wait)) return

        acquireWakeLock(context)

        StatusManager.setAnimationActive(true)
        StatusManager.setVolumeAnimationActive(true)

        var volumeArray = StatusManager.getVolumeArray()
        if (volumeArray == null) {
            if (DEBUG) Log.d(TAG, "Volume array is null, cannot play animation")
            return
        }

        val amount = Math.round((volumeLevel / 100.0) * volumeArray.size).toInt()
        val last = StatusManager.getVolumeLedLast()
        val next = amount - 1

        try {
            if (last <= next) {
                for (i in last..next) {
                    if (checkInterruption("volume")) throw InterruptedException()
                    StatusManager.setVolumeLedLast(i)
                    volumeArray[i] = Constants.MAX_PATTERN_BRIGHTNESS
                    updateLedFrame(volumeArray)
                    Thread.sleep(16, 666000)
                }
            } else if (last > next) {
                for (i in last downTo next + 1) {
                    if (checkInterruption("volume")) throw InterruptedException()
                    StatusManager.setVolumeLedLast(i)
                    volumeArray[i] = 0
                    updateLedFrame(volumeArray)
                    Thread.sleep(16, 666000)
                }
            }
        } catch (e: InterruptedException) {
            if (DEBUG) Log.d(TAG, "Exception while playing animation, interrupted | name: volume")
            if (!StatusManager.isAllLedActive()) {
                StatusManager.setVolumeLedLast(0)
                volumeArray = IntArray(ResourceUtils.getInteger("glyph_settings_volume_levels_num"))
                updateLedFrame(volumeArray)
            }
        } finally {
            StatusManager.setAnimationActive(false)
            StatusManager.setVolumeArray(volumeArray)
            if (DEBUG) Log.d(TAG, "Done playing animation | name: volume")
            releaseWakeLock()
        }
    }

    fun dismissVolume(context: Context) {
        val emptyArray = IntArray(ResourceUtils.getInteger("glyph_settings_volume_levels_num"))
        val volumeArray = StatusManager.getVolumeArray()

        if (emptyArray.contentEquals(volumeArray)) return

        if (!check("Dismiss volume", false)) return

        acquireWakeLock(context)

        StatusManager.setAnimationActive(true)

        try {
            if (checkInterruption("Dismiss volume")) throw InterruptedException()
            for (i in volumeArray.indices.reversed()) {
                if (volumeArray[i] != 0) {
                    if (checkInterruption("Dismiss volume")) throw InterruptedException()
                    StatusManager.setVolumeLedLast(i)
                    volumeArray[i] = 0
                    updateLedFrame(volumeArray)
                    Thread.sleep(16, 666000)
                }
            }
        } catch (e: InterruptedException) {
            if (DEBUG) Log.d(
                TAG,
                "Exception while playing animation, interrupted | name: Dismiss volume"
            )
            if (!StatusManager.isAllLedActive()) updateLedFrame(IntArray(volumeArray.size))
        } finally {
            StatusManager.setVolumeLedLast(0)
            StatusManager.setVolumeAnimationActive(false)
            StatusManager.setAnimationActive(false)
            if (DEBUG) Log.d(TAG, "Done playing animation | name: Dismiss volume")
            releaseWakeLock()
        }
    }

    fun playCall(name: String?) {
        StatusManager.setCallLedEnabled(true)

        if (!check("call: " + name, true)) return

        StatusManager.setCallLedActive(true)

        while (StatusManager.isCallLedEnabled()) {
            try {
                BufferedReader(
                    InputStreamReader(
                        ResourceUtils.getCallAnimation(name)
                    )
                ).use { reader ->
                    var line: String?
                    while ((reader.readLine().also { line = it }) != null) {
                        if (checkInterruption("call")) throw InterruptedException()
                        line = line!!.replace(" ", "")
                        line = if (line.endsWith(",")) line.substring(0, line.length - 1) else line
                        val pattern: Array<String?> =
                            line.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        if (ArrayUtils.contains(
                                Constants.getSupportedAnimationPatternLengths(),
                                pattern.size
                            )
                        ) {
                            updateLedFrame(pattern)
                        } else {
                            if (DEBUG) Log.d(
                                TAG,
                                "Animation line length mismatch | name: " + name + " | line: " + line
                            )
                            throw InterruptedException()
                        }
                        Thread.sleep(16, 666000)
                    }
                }
            } catch (e: Exception) {
                if (DEBUG) Log.d(
                    TAG,
                    "Exception while playing animation | name: " + name + " | exception: " + e
                )
            } finally {
                if (StatusManager.isAllLedActive()) {
                    if (DEBUG) Log.d(TAG, "All LED active, pause playing animation | name: " + name)
                    while (StatusManager.isAllLedActive()) {
                    }
                }
            }
        }
    }

    fun stopCall() {
        if (DEBUG) Log.d(TAG, "Disabling Call Animation")
        StatusManager.setCallLedEnabled(false)
        updateLedFrame(FloatArray(5))
        StatusManager.setCallLedActive(false)
        if (DEBUG) Log.d(TAG, "Done playing Call Animation")
    }

    fun playEssential() {
        if (DEBUG) Log.d(TAG, "Playing Essential Animation")
        val led: Int = ResourceUtils.getInteger("glyph_settings_notifs_essential_led")
        if (!StatusManager.isEssentialLedActive()) {
            submit(Runnable {
                if (!check("essential", true)) return@submit
                StatusManager.setAnimationActive(true)

                try {
                    if (checkInterruption("essential")) throw InterruptedException()
                    val steps = intArrayOf(12, 24, 36, 48, 60)
                    for (i in steps) {
                        if (checkInterruption("essential")) throw InterruptedException()
                        updateLedSingle(led, Constants.MAX_PATTERN_BRIGHTNESS / 100 * i)
                        Thread.sleep(16, 666000)
                    }
                } catch (e: InterruptedException) {
                }
                StatusManager.setAnimationActive(false)
                StatusManager.setEssentialLedActive(true)
                if (DEBUG) Log.d(TAG, "Done playing animation | name: essential")
            })
        } else {
            updateLedSingle(led, Constants.MAX_PATTERN_BRIGHTNESS / 100 * 60)
            return
        }
    }

    fun stopEssential() {
        if (DEBUG) Log.d(TAG, "Disabling Essential Animation")
        StatusManager.setEssentialLedActive(false)
        if (!StatusManager.isAnimationActive() && !StatusManager.isAllLedActive()) {
            val led: Int = ResourceUtils.getInteger("glyph_settings_notifs_essential_led")
            updateLedSingle(led, 0)
        }
    }

    fun playMusic(name: String) {
        val maxPatternBrightness = Constants.MAX_PATTERN_BRIGHTNESS.toFloat()
        val pattern = FloatArray(5)

        when (name) {
            "low" -> pattern[4] = maxPatternBrightness
            "mid_low" -> pattern[3] = maxPatternBrightness
            "mid" -> pattern[2] = maxPatternBrightness
            "mid_high" -> pattern[0] = maxPatternBrightness
            "high" -> pattern[1] = maxPatternBrightness
            else -> {
                if (DEBUG) Log.d(TAG, "Name doesn't match any zone, returning | name: " + name)
                return
            }
        }

        try {
            if (StatusManager.isGlyphIdle()) {
                updateLedFrame(pattern)
                Thread.sleep(106)
            }
        } catch (e: Exception) {
            if (DEBUG) Log.d(
                TAG,
                "Exception while playing animation | name: music: " + name + " | exception: " + e
            )
        } finally {
            if (StatusManager.isGlyphIdle()) {
                updateLedFrame(FloatArray(5))
                if (DEBUG) Log.d(TAG, "Done playing animation | name: " + name)
            }
        }
    }

    private fun updateLedFrame(pattern: Array<String?>) {
        AnimationManager.updateLedFrame(
            Arrays.stream<String?>(pattern)
                .mapToInt { s: String? -> s!!.toInt() }
                .toArray())
    }

    fun updateLedFrame(pattern: IntArray) {
        val floatPattern = FloatArray(pattern.size)
        for (i in pattern.indices) {
            floatPattern[i] = pattern[i].toFloat()
        }
        updateLedFrame(floatPattern)
    }

    private fun updateLedFrame(pattern: FloatArray) {
        //if (DEBUG) Log.d(TAG, "Updating pattern: " + pattern);
        val maxPatternBrightness = Constants.MAX_PATTERN_BRIGHTNESS.toFloat()
        val currentBrightness = Constants.getBrightness().toFloat()
        val essentialLed: Int = ResourceUtils.getInteger("glyph_settings_notifs_essential_led")

        if (StatusManager.isEssentialLedActive()) {
            if (pattern.size == 5) { // Phone (1) pattern
                if (pattern[1] < (maxPatternBrightness / 100 * 60)) {
                    pattern[1] = maxPatternBrightness / 100 * 60
                }
            } else if (pattern.size == 33) { // Phone (2) pattern
                if (pattern[2] < (maxPatternBrightness / 100 * 60)) {
                    pattern[2] = maxPatternBrightness / 100 * 60
                }
            }
        }

        for (i in pattern.indices) {
            pattern[i] = pattern[i] / maxPatternBrightness * currentBrightness
        }
        FileUtils.writeFrameLed(pattern)
    }

    private fun updateLedSingle(led: Int, brightness: String) {
        updateLedSingle(led, brightness.toFloat())
    }

    private fun updateLedSingle(led: Int, brightness: Int) {
        updateLedSingle(led, brightness.toFloat())
    }

    private fun updateLedSingle(led: Int, brightness: Float) {
        //if (DEBUG) Log.d(TAG, "Updating led | led: " + led + " | brightness: " + brightness);
        var brightness = brightness
        val maxPatternBrightness = Constants.MAX_PATTERN_BRIGHTNESS.toFloat()
        val currentBrightness = Constants.getBrightness().toFloat()
        val essentialLed: Int = ResourceUtils.getInteger("glyph_settings_notifs_essential_led")

        if (StatusManager.isEssentialLedActive()
            && led == essentialLed && brightness < (maxPatternBrightness / 100 * 60)
        ) {
            brightness = maxPatternBrightness / 100 * 60
        }

        brightness = brightness / maxPatternBrightness * currentBrightness

        FileUtils.writeSingleLed(led, brightness)
    }

    fun singleLedBlink(context: Context, zone: Int, brightness: Int, durationMs: Int) {
        if (zone < 0 || zone >= ResourceUtils.getInteger("glyph_settings_led_count")) {
            if (DEBUG) Log.e(TAG, "Invalid LED zone: " + zone)
            return
        }

        if (DEBUG) Log.d(
            TAG,
            "Blinking zone " + zone + " at brightness " + brightness + " for " + durationMs + "ms"
        )

        acquireWakeLock(context)

        submit(Runnable {
            try {
                updateLedSingle(zone, brightness)

                Thread.sleep(durationMs.toLong())

                if (!StatusManager.isEssentialLedActive() ||
                    zone != ResourceUtils.getInteger("glyph_settings_notifs_essential_led")
                ) {
                    updateLedSingle(zone, 0)
                }
            } catch (e: InterruptedException) {
                if (DEBUG) Log.e(TAG, "Interrupted while blinking LED zone " + zone, e)
            } finally {
                releaseWakeLock()
            }
        })
    }

    fun playGlyphFrame(context: Context, zones: IntArray?, brightness: Int, durationMs: Int) {
        if (zones == null || zones.size == 0) {
            if (DEBUG) Log.e(TAG, "Invalid zones array")
            return
        }

        if (DEBUG) Log.d(
            TAG,
            "Playing frame with " + zones.size + " zones at brightness " + brightness + " for " + durationMs + "ms"
        )

        acquireWakeLock(context)

        submit(Runnable {
            try {
                for (zone in zones) {
                    if (zone >= 0 && zone < ResourceUtils.getInteger("glyph_settings_led_count")) {
                        updateLedSingle(zone, brightness)
                    }
                }

                Thread.sleep(durationMs.toLong())

                val essentialLed: Int =
                    ResourceUtils.getInteger("glyph_settings_notifs_essential_led")
                for (zone in zones) {
                    if (zone >= 0 && zone < ResourceUtils.getInteger("glyph_settings_led_count")) {
                        if (!StatusManager.isEssentialLedActive() || zone != essentialLed) {
                            updateLedSingle(zone, 0)
                        }
                    }
                }
            } catch (e: InterruptedException) {
                if (DEBUG) Log.e(TAG, "Interrupted while playing Glyph frame", e)
            } finally {
                releaseWakeLock()
            }
        })
    }

    fun stopAll() {
        if (DEBUG) Log.d(TAG, "Stopping all LED animations")

        if (StatusManager.isCallLedEnabled()) {
            stopCall()
        }

        val ledCount: Int = ResourceUtils.getInteger("glyph_settings_led_count")
        val essentialLed: Int = ResourceUtils.getInteger("glyph_settings_notifs_essential_led")

        for (i in 0..<ledCount) {
            if (!StatusManager.isEssentialLedActive() || i != essentialLed) {
                updateLedSingle(i, 0)
            }
        }

        StatusManager.setAnimationActive(false)
        StatusManager.setCallLedActive(false)

        if (DEBUG) Log.d(TAG, "All LED animations stopped")
    }

    fun canPlayGlyphComposer(): Boolean {
        if (StatusManager.isAllLedActive()) {
            if (DEBUG) Log.d(TAG, "Cannot play Glyph Composer: All LEDs active")
            return false
        }

        if (StatusManager.isCallLedActive()) {
            if (DEBUG) Log.d(TAG, "Cannot play Glyph Composer: Call animation active")
            return false
        }

        return true
    }
}
