/*
 * Copyright (C) 2024 Neoteric OS
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

class AutoBrightnessService : Service() {
    private var mSensorManager: SensorManager? = null
    private var mLightSensor: Sensor? = null
    public override fun onCreate() {
        if (DEBUG) Log.d(TAG, "Creating service")

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Get light sensor type
        val sensorName: String = ResourceUtils.getString("glyph_light_sensor")
        val sensors: MutableList<Sensor> = mSensorManager.getSensorList(Sensor.TYPE_ALL)
        for (sensor in sensors) {
            if (sensorName == sensor.getStringType()) {
                sensorType = sensor.getType()
                break
            }
        }

        mLightSensor = mSensorManager.getDefaultSensor(sensorType)
        mSensorManager.registerListener(
            mSensorEventListener,
            mLightSensor, SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    public override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "Starting service")
        return START_STICKY
    }

    public override fun onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying service")
        mSensorManager.unregisterListener(mSensorEventListener)
        super.onDestroy()
    }

    public override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private val mSensorEventListener: SensorEventListener = object : SensorEventListener() {
        public override fun onSensorChanged(event: SensorEvent) {
            val lux = event.values[0] as Int
            var lux_index = 0
            val brightnessValue: Int

            for (i in 1..<AutoBrightnessLux.size) {
                if (lux < AutoBrightnessLux[i]) {
                    break
                } else if (lux >= AutoBrightnessLux[i]) {
                    lux_index = i
                }
            }

            brightnessValue = BrightnessValues[lux_index]

            if (brightnessValue != Constants.getBrightness()) {
                if (DEBUG) {
                    val led_lux: Int = AutoBrightnessLux[lux_index]
                    Log.d(
                        TAG, "Brightness changed: " + "RealLux: " + lux +
                                " | BrightnessLux: " + led_lux + " | BrightnessValue: " + brightnessValue
                    )
                }
                Constants.setBrightness(brightnessValue)
                if (StatusManager.isEssentialLedActive()) AnimationManager.playEssential()
            }
        }

        public override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }
    }

    companion object {
        private const val TAG = "GlyphAutoBrightnessService"
        private const val DEBUG = true

        private var sensorType = 0
        private val AutoBrightnessLux: IntArray =
            ResourceUtils.getIntArray("glyph_auto_brightness_levels")
        private val BrightnessValues: IntArray = Constants.getBrightnessLevels()
    }
}
