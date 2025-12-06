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
package co.aospa.glyph.Sensors

import android.content.Context
import java.time.Duration
import java.util.function.Consumer
import kotlin.math.abs

class FlipToGlyphSensor(context: Context, @NonNull onFlip: Consumer<Boolean?>?) :
    SensorEventListener {
    private var isFlipped = false
    private val mOnFlip: Consumer<Boolean?>

    private val mSensorManager: SensorManager
    private val mSensorAccelerometer: Sensor?
    private val mContext: Context

    private val mTimeThreshold: Duration = Duration.ofMillis(1000L)
    private val mAccelerationThreshold = 0.2f
    private val mZAccelerationThreshold = -9.5f
    private val mZAccelerationThresholdLenient = mZAccelerationThreshold + 1.0f
    private var mPrevAcceleration = 0f
    private var mPrevAccelerationTime: Long = 0
    private var mZAccelerationIsFaceDown = false
    private var mZAccelerationFaceDownTime = 0L

    private val mCurrentXYAcceleration = ExponentialMovingAverage(MOVING_AVERAGE_WEIGHT)
    private val mCurrentZAcceleration = ExponentialMovingAverage(MOVING_AVERAGE_WEIGHT)

    init {
        mContext = context
        mOnFlip = Objects.requireNonNull<Consumer<Boolean?>>(onFlip)
        mSensorManager = mContext.getSystemService(SensorManager::class.java)
        mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER, false)
    }

    public override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.getType() !== Sensor.TYPE_ACCELEROMETER) return

        val x: Float = event.values[0]
        val y: Float = event.values[1]
        mCurrentXYAcceleration.updateMovingAverage(x * x + y * y)
        mCurrentZAcceleration.updateMovingAverage(event.values[2])

        val curTime: Long = event.timestamp
        if (abs(mCurrentXYAcceleration.mMovingAverage - mPrevAcceleration) > mAccelerationThreshold) {
            mPrevAcceleration = mCurrentXYAcceleration.mMovingAverage
            mPrevAccelerationTime = curTime
        }
        val moving = curTime - mPrevAccelerationTime <= mTimeThreshold.toNanos()

        val zAccelerationThreshold =
            if (isFlipped) mZAccelerationThresholdLenient else mZAccelerationThreshold
        val isCurrentlyFaceDown =
            mCurrentZAcceleration.mMovingAverage < zAccelerationThreshold
        val isFaceDownForPeriod = isCurrentlyFaceDown
                && mZAccelerationIsFaceDown
                && curTime - mZAccelerationFaceDownTime > mTimeThreshold.toNanos()
        if (isCurrentlyFaceDown && !mZAccelerationIsFaceDown) {
            mZAccelerationFaceDownTime = curTime
            mZAccelerationIsFaceDown = true
        } else if (!isCurrentlyFaceDown) {
            mZAccelerationIsFaceDown = false
        }

        if (!moving && isFaceDownForPeriod && !isFlipped) {
            onFlip(true)
        } else if (!isFaceDownForPeriod && isFlipped) {
            onFlip(false)
        }
    }

    public override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun onFlip(flipped: Boolean) {
        if (DEBUG) Log.d(TAG, "Flipped: " + flipped)
        mOnFlip.accept(flipped)
        isFlipped = flipped
    }

    fun enable() {
        if (DEBUG) Log.d(TAG, "Enabling Sensor")
        mSensorManager.registerListener(
            this, mSensorAccelerometer,
            SensorManager.SENSOR_DELAY_NORMAL,
            mContext.getResources().getInteger(
                com.android.internal.R.integer.config_flipToScreenOffMaxLatencyMicros
            )
        )
    }

    fun disable() {
        if (DEBUG) Log.d(TAG, "Disabling Sensor")
        onFlip(false)
        mSensorManager.unregisterListener(this, mSensorAccelerometer)
    }

    private inner class ExponentialMovingAverage @JvmOverloads constructor(
        private val mAlpha: Float,
        private val mInitialAverage: Float = 0.0f
    ) {
        private var mMovingAverage: Float

        init {
            this.mMovingAverage = mInitialAverage
        }

        fun updateMovingAverage(newValue: Float) {
            mMovingAverage = newValue + mAlpha * (mMovingAverage - newValue)
        }

        fun reset() {
            mMovingAverage = this.mInitialAverage
        }
    }

    companion object {
        private const val DEBUG = true
        private const val TAG = "FlipToGlyphSensor"

        private const val MOVING_AVERAGE_WEIGHT = 0.5f
    }
}