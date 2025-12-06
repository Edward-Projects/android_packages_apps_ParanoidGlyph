/*
 * Copyright (C) 2022 By yours truly, Daniel Jacob Chittoor
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
import kotlin.math.abs
import kotlin.math.sqrt

class MusicVisualizerService : Service() {
    private var mAudioManager: AudioManager? = null
    private var thread: HandlerThread? = null
    private var mHandler: Handler? = null
    private var mVisualizer: Visualizer? = null
    private var bufferSize = 0
    private val isRecording = false

    private var mRunningSoundAvg: DoubleArray? // Total sound energy in one second  (0=low, 1=mid low, 2=mid, 3=mid high, 4=high)
    private var mCurrentAvgEnergyOneSec: DoubleArray? // Average sound energy in one second (0=low, 1=mid low, 2=mid, 3=mid high, 4=high)
    private var mNumberOfSamplesInOneSec = 0 // Number of samples in one second
    private var mSystemTimeStartSec: Long = 0 // System time at the start of a one second interval

    public override fun onCreate() {
        if (DEBUG) Log.d(TAG, "Creating service")

        // Run visualizer on a handler thread
        thread = HandlerThread("MusicVisualizerService")
        thread.start()
        val looper: Looper? = thread.getLooper()
        mHandler = Handler(looper)

        // Get audio service
        mAudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Create a visualizer with the audio session ID (0) which takes the entire output mix
        mVisualizer = Visualizer(0)

        // Set the capture size to the maximum available
        bufferSize = Visualizer.getCaptureSizeRange()[1]
        mVisualizer.setCaptureSize(bufferSize)

        mHandler.post({
            // Set data capture listener for visualizer
            mVisualizer.setDataCaptureListener(
                object : OnDataCaptureListener() {
                    public override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                    }

                    public override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray,
                        samplingRate: Int
                    ) {
                        if (mAudioManager.isMusicActive() && StatusManager.isGlyphIdle()) {
                            if (DEBUG) Log.d(TAG, "Music is active")
                            processAudioFFT(fft, samplingRate)
                        }
                    }
                }, Visualizer.getMaxCaptureRate() / 2, false, true
            )

            // Enable visualizer
            mVisualizer.setEnabled(true)

            // Initialize instance variables
            mRunningSoundAvg = DoubleArray(5)
            mCurrentAvgEnergyOneSec = DoubleArray(5)
            mCurrentAvgEnergyOneSec!![0] = -1.0
            mCurrentAvgEnergyOneSec!![1] = -1.0
            mCurrentAvgEnergyOneSec!![2] = -1.0
            mCurrentAvgEnergyOneSec!![3] = -1.0
            mCurrentAvgEnergyOneSec!![4] = -1.0

            // Set the start time for the current one second interval
            mSystemTimeStartSec = System.currentTimeMillis()
        })
    }

    public override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "Starting service")
        return START_STICKY
    }

    public override fun onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying service")
        mVisualizer.setEnabled(false)
        mVisualizer.release()
        thread.quit()
        super.onDestroy()
    }

    public override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun processAudioFFT(audioBytes: ByteArray, samplingRate: Int) {
        // The first byte is the DC component of the FFT result (real only)
        var energySum = abs(audioBytes[0].toInt())

        // Calculate the average instantaneous energy of the low frequency band
        var k = 2
        val captureSize = audioBytes.size / 2.0
        val sampleRate = samplingRate / 2000
        var nextFrequency = (k / 2.0 * sampleRate) / captureSize

        // Sum the energy in the low frequency band
        while (nextFrequency < LOW_FREQUENCY) {
            // Calculate the energy of the current frequency
            energySum =
                (energySum + sqrt((audioBytes[k] * audioBytes[k] + audioBytes[k + 1] * audioBytes[k + 1]).toDouble())).toInt()

            // Increment the frequency index
            k += 2
            nextFrequency = (k / 2.0 * sampleRate) / captureSize
        }

        // Calculate the average energy in the low frequency band
        var sampleAvgAudioEnergy = energySum / (k / 2.0)

        // Accumulate the low frequency band energy over time
        mRunningSoundAvg!![0] += sampleAvgAudioEnergy

        // Check for a beat in the low frequency band
        // A beat occurs when the average sound energy of a sample is greater than
        // the average sound energy of a one second part of a song
        // Also make sure the mCurrentAvgEnergy has been set, otherwise its -1 before its first pass
        if ((sampleAvgAudioEnergy > mCurrentAvgEnergyOneSec!![0]) && (mCurrentAvgEnergyOneSec!![0] > 0)) {
            if (DEBUG) Log.d(TAG, "Low frequency band beat detected")
            AnimationManager.playMusic("low")
        }

        energySum = 0

        // Sum the energy in the mid-low frequency band
        while (nextFrequency < MID_LOW_FREQUENCY) {
            // Calculate the energy of the current frequency
            energySum =
                (energySum + sqrt((audioBytes[k] * audioBytes[k] + audioBytes[k + 1] * audioBytes[k + 1]).toDouble())).toInt()

            // Increment the frequency index
            k += 2
            nextFrequency = (k / 2.0 * sampleRate) / captureSize
        }

        // Calculate the average energy in the mid-low frequency band
        sampleAvgAudioEnergy = energySum / (k / 2.0)

        // Accumulate the mid low frequency band energy over time
        mRunningSoundAvg!![1] += sampleAvgAudioEnergy

        // Check for a beat in the mid-low frequency band
        if ((sampleAvgAudioEnergy > mCurrentAvgEnergyOneSec!![1]) && (mCurrentAvgEnergyOneSec!![1] > 0)) {
            if (DEBUG) Log.d(TAG, "Mid-low frequency band beat detected")
            AnimationManager.playMusic("mid_low")
        }

        energySum = 0

        // Sum the energy in the mid frequency band
        while (nextFrequency < MID_FREQUENCY) {
            // Calculate the energy of the current frequency
            energySum =
                (energySum + sqrt((audioBytes[k] * audioBytes[k] + audioBytes[k + 1] * audioBytes[k + 1]).toDouble())).toInt()

            // Increment the frequency index
            k += 2
            nextFrequency = (k / 2.0 * sampleRate) / captureSize
        }

        // Calculate the average energy in the mid frequency band
        sampleAvgAudioEnergy = energySum / (k / 2.0)

        // Accumulate the mid frequency band energy over time
        mRunningSoundAvg!![2] += sampleAvgAudioEnergy

        // Check for a beat in the mid frequency band
        if ((sampleAvgAudioEnergy > mCurrentAvgEnergyOneSec!![2]) && (mCurrentAvgEnergyOneSec!![2] > 0)) {
            if (DEBUG) Log.d(TAG, "Mid frequency band beat detected")
            AnimationManager.playMusic("mid")
        }

        energySum = 0

        // Sum the energy in the mid-high frequency band
        while (nextFrequency < MID_HIGH_FREQUENCY) {
            // Calculate the energy of the current frequency
            energySum =
                (energySum + sqrt((audioBytes[k] * audioBytes[k] + audioBytes[k + 1] * audioBytes[k + 1]).toDouble())).toInt()

            // Increment the frequency index
            k += 2
            nextFrequency = (k / 2.0 * sampleRate) / captureSize
        }

        // Calculate the average energy in the mid-high frequency band
        sampleAvgAudioEnergy = energySum / (k / 2.0)

        // Accumulate the mid high-frequency band energy over time
        mRunningSoundAvg!![3] += sampleAvgAudioEnergy

        // Check for a beat in the mid-high frequency band
        if ((sampleAvgAudioEnergy > mCurrentAvgEnergyOneSec!![3]) && (mCurrentAvgEnergyOneSec!![3] > 0)) {
            if (DEBUG) Log.d(TAG, "Mid-high frequency band beat detected")
            AnimationManager.playMusic("mid_high")
        }

        // Second Byte: Only imaginary part of the last frequency (include in highs)
        energySum = abs(audioBytes[1].toInt())

        // Sum the energy in the high frequency band
        while (nextFrequency < HIGH_FREQUENCY) {
            // Calculate the energy of the current frequency
            energySum =
                (energySum + sqrt((audioBytes[k] * audioBytes[k] + audioBytes[k + 1] * audioBytes[k + 1]).toDouble())).toInt()

            // Increment the frequency index
            k += 2
            nextFrequency = (k / 2.0 * sampleRate) / captureSize
        }

        // Calculate the average energy in the high frequency band
        sampleAvgAudioEnergy = energySum / (k / 2.0)

        // Accumulate the high frequency band energy over time
        mRunningSoundAvg!![4] += sampleAvgAudioEnergy

        // Check for a beat in the high frequency band
        if ((sampleAvgAudioEnergy > mCurrentAvgEnergyOneSec!![4]) && (mCurrentAvgEnergyOneSec!![4] > 0)) {
            if (DEBUG) Log.d(TAG, "High frequency band beat detected")
            AnimationManager.playMusic("high")
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - mSystemTimeStartSec >= 1000) {
            mCurrentAvgEnergyOneSec!![0] = mRunningSoundAvg!![0] / mNumberOfSamplesInOneSec
            mCurrentAvgEnergyOneSec!![1] = mRunningSoundAvg!![1] / mNumberOfSamplesInOneSec
            mCurrentAvgEnergyOneSec!![2] = mRunningSoundAvg!![2] / mNumberOfSamplesInOneSec
            mCurrentAvgEnergyOneSec!![3] = mRunningSoundAvg!![3] / mNumberOfSamplesInOneSec
            mCurrentAvgEnergyOneSec!![4] = mRunningSoundAvg!![4] / mNumberOfSamplesInOneSec

            // Reset the running energy sum and sample count
            mRunningSoundAvg!![0] = 0.0
            mRunningSoundAvg!![1] = 0.0
            mRunningSoundAvg!![2] = 0.0
            mRunningSoundAvg!![3] = 0.0
            mRunningSoundAvg!![4] = 0.0
            mNumberOfSamplesInOneSec = 0

            // Update the start time for the next one-second interval
            mSystemTimeStartSec = currentTime
        }
        mNumberOfSamplesInOneSec++
    }

    companion object {
        private const val TAG = "GlyphMusicVisualizerService"
        private const val DEBUG = true

        // Define the max value for a frequency band
        private const val LOW_FREQUENCY = 200
        private const val MID_LOW_FREQUENCY = 500
        private const val MID_FREQUENCY = 1500
        private const val MID_HIGH_FREQUENCY = 5000
        private const val HIGH_FREQUENCY = 10000
    }
}
