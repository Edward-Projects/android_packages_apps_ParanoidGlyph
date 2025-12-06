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
package co.aospa.glyph.Services

import android.app.Service
import co.aospa.glyph.Constants.Constants
import co.aospa.glyph.Manager.SettingsManager
import co.aospa.glyph.Utils.FileUtils
import kotlin.math.sqrt

class ShakeDetectorService : Service(), SensorEventListener {
    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null
    private var mSharedPrefs: SharedPreferences? = null
    private var mWakeLock: PowerManager.WakeLock? = null
    private var mVibrator: Vibrator? = null

    private var mLastShakeTime: Long = 0
    private var mShakeCount = 0
    private var mCurrentThreshold = DEFAULT_SENSITIVITY.toFloat()

    public override fun onCreate() {
        super.onCreate()

        if (DEBUG) Log.d(TAG, "Service created")

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        mVibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?

        val powerManager: PowerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + ":ShakeWakeLock")

        loadSensitivity()

        registerSensorListener()
    }

    public override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "Service started")

        loadSensitivity()

        return START_STICKY
    }

    public override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun loadSensitivity() {
        val sensitivity: Int =
            mSharedPrefs.getInt(Constants.GLYPH_SHAKE_SENSITIVITY, DEFAULT_SENSITIVITY)
        mCurrentThreshold = sensitivity.toFloat()
        if (DEBUG) Log.d(TAG, "Shake sensitivity loaded: " + mCurrentThreshold)
    }

    private fun registerSensorListener() {
        if (mAccelerometer != null && this.isShakeEnabled && SettingsManager.isGlyphEnabled()) {
            mSensorManager.registerListener(
                this, mAccelerometer,
                SensorManager.SENSOR_DELAY_GAME
            )
            if (DEBUG) Log.d(
                TAG,
                "Accelerometer listener registered with threshold: " + mCurrentThreshold
            )
        } else {
            if (DEBUG) Log.d(TAG, "Shake disabled or Glyph disabled, listener not registered")
        }
    }

    private fun unregisterSensorListener() {
        mSensorManager.unregisterListener(this)
        if (DEBUG) Log.d(TAG, "Accelerometer listener unregistered")
    }

    private val isShakeEnabled: Boolean
        get() = mSharedPrefs.getBoolean(
            Constants.GLYPH_SHAKE_TORCH_ENABLE,
            false
        )

    public override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.getType() !== Sensor.TYPE_ACCELEROMETER) {
            return
        }

        // Allow shake even during schedule (for torch)
        if (!this.isShakeEnabled || !SettingsManager.isGlyphEnabledIgnoreSchedule()) {
            return
        }

        val x: Float = event.values[0]
        val y: Float = event.values[1]
        val z: Float = event.values[2]

        val acceleration: Float =
            sqrt((x * x + y * y + z * z).toDouble()).toFloat() - SensorManager.GRAVITY_EARTH

        if (acceleration > mCurrentThreshold) {
            val currentTime: Long = SystemClock.elapsedRealtime()

            if (currentTime - mLastShakeTime < SHAKE_TIME_WINDOW) {
                mShakeCount++
                if (DEBUG) Log.d(
                    TAG,
                    "Shake detected, count: " + mShakeCount + ", accel: " + acceleration
                )

                if (mShakeCount >= SHAKE_COUNT_THRESHOLD) {
                    onShakeDetected()
                    mShakeCount = 0
                }
            } else {
                mShakeCount = 1
                if (DEBUG) Log.d(TAG, "First shake detected, accel: " + acceleration)
            }

            mLastShakeTime = currentTime
        }
    }

    public override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    private fun onShakeDetected() {
        if (DEBUG) Log.d(TAG, "Shake gesture triggered, toggling torch")

        if (!SettingsManager.isGlyphEnabledIgnoreSchedule()) {
            if (DEBUG) Log.d(TAG, "Glyph completely disabled, ignoring shake")
            return
        }

        performHapticFeedback()

        if (mWakeLock != null && !mWakeLock.isHeld()) {
            mWakeLock.acquire(3000)
        }

        try {
            val currentState: Boolean = StatusManager.isAllLedActive()
            val newState = !currentState

            StatusManager.setAllLedsActive(newState)
            FileUtils.writeAllLed(if (newState) Constants.getMaxBrightness() else 0)

            if (StatusManager.isEssentialLedActive() && !newState) {
                FileUtils.writeSingleLed(
                    ResourceUtils.getInteger("glyph_settings_notifs_essential_led"),
                    (Constants.getMaxBrightness() / 100 * 7).toFloat()
                )
            }

            showToastNotification(newState)

            if (DEBUG) Log.d(TAG, "Torch toggled to: " + (if (newState) "ON" else "OFF"))
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling torch", e)
        } finally {
            if (mWakeLock != null && mWakeLock.isHeld()) {
                mWakeLock.release()
            }
        }
    }

    private fun performHapticFeedback() {
        if (mVibrator != null && mVibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mVibrator.vibrate(
                    VibrationEffect.createOneShot(
                        50,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                mVibrator.vibrate(50)
            }
            if (DEBUG) Log.d(TAG, "Haptic feedback triggered")
        }
    }

    private fun showToastNotification(torchOn: Boolean) {
        val handler: android.os.Handler = Handler(getMainLooper())
        handler.post({
            val message = if (torchOn) "Glyph Torch ON" else "Glyph Torch OFF"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            if (DEBUG) Log.d(TAG, "Toast shown: " + message)
        })
    }

    public override fun onDestroy() {
        if (DEBUG) Log.d(TAG, "Service destroyed")

        unregisterSensorListener()

        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release()
        }

        super.onDestroy()
    }

    companion object {
        private const val TAG = "GlyphShakeDetector"
        private const val DEBUG = false

        private const val SHAKE_TIME_WINDOW = 500
        private const val SHAKE_COUNT_THRESHOLD = 2

        private const val DEFAULT_SENSITIVITY = 35
    }
}