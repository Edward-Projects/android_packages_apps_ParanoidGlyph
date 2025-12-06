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
import kotlin.math.sqrt

class ChargingService : Service() {
    private var thread: HandlerThread? = null
    private var mThreadHandler: Handler? = null

    private var mBatteryManager: BatteryManager? = null
    private var mSensorManager: SensorManager? = null

    private var mPowerManager: PowerManager? = null

    private var mAccelerometerSensor: Sensor? = null
    private val dismissCharging: Runnable = object : Runnable {
        override fun run() {
            AnimationManager.dismissCharging()
        }
    }

    public override fun onCreate() {
        if (DEBUG) Log.d(TAG, "Creating service")

        // Add a handler thread
        thread = HandlerThread("ChargingService")
        thread.start()
        val looper: Looper? = thread.getLooper()
        mThreadHandler = Handler(looper)

        mBatteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mPowerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

        mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val powerMonitor: IntentFilter = IntentFilter()
        powerMonitor.addAction(Intent.ACTION_POWER_CONNECTED)
        powerMonitor.addAction(Intent.ACTION_POWER_DISCONNECTED)
        registerReceiver(mPowerMonitor, powerMonitor)
    }

    public override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "Starting service")
        return START_STICKY
    }

    public override fun onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying service")
        this.unregisterReceiver(mPowerMonitor)
        onPowerDisconnected()
        thread.quit()
        super.onDestroy()
    }

    public override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private val batteryLevel: Int
        get() = mBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

    private fun onPowerConnected() {
        if (DEBUG) Log.d(TAG, "Power connected")
        if (DEBUG) Log.d(TAG, "Battery level: " + this.batteryLevel)
        playChargingAnimation(true)
        mSensorManager.registerListener(
            mSensorEventListener,
            mAccelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    private fun onPowerDisconnected() {
        if (DEBUG) Log.d(TAG, "Power disconnected")
        mSensorManager.unregisterListener(mSensorEventListener)
    }

    private fun playChargingAnimation(wait: Boolean) {
        if (mThreadHandler.hasCallbacks(dismissCharging)) mThreadHandler.removeCallbacks(
            dismissCharging
        )
        mThreadHandler.post({
            AnimationManager.playCharging(this.batteryLevel, wait)
        })
        mThreadHandler.postDelayed(dismissCharging, 1190)
    }

    private val mPowerMonitor: BroadcastReceiver = object : BroadcastReceiver() {
        public override fun onReceive(context: Context?, intent: Intent) {
            if (intent.getAction().equals(Intent.ACTION_POWER_CONNECTED)) {
                onPowerConnected()
            } else if (intent.getAction().equals(Intent.ACTION_POWER_DISCONNECTED)) {
                onPowerDisconnected()
            }
        }
    }

    private val mSensorEventListener: SensorEventListener = object : SensorEventListener() {
        public override fun onSensorChanged(event: SensorEvent) {
            val x: Float = event.values[0]
            val y: Float = event.values[1]
            val z: Float = event.values[2]
            val acceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

            if (acceleration > ACCELEROMETER_THRESHOLD && z <= ZFACEDOWN_THRESHOLD && !mPowerManager.isInteractive()) {
                playChargingAnimation(false)
            }
        }

        public override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }
    }

    companion object {
        private const val TAG = "GlyphChargingService"
        private const val DEBUG = true

        private const val ACCELEROMETER_THRESHOLD = 10.0f
        private val ZFACEDOWN_THRESHOLD = -5.0f
    }
}
