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
package co.aospa.glyph.Composer

import android.content.Context
import co.aospa.glyph.Manager.SettingsManager

class GlyphSyncPlayer(context: Context) {
    private val mContext: Context
    private var mMediaPlayer: MediaPlayer? = null
    private var mPattern: GlyphPattern? = null
    private val mSyncHandler: Handler?
    private var mCurrentFrameIndex = 0
    var isPlaying: Boolean = false
        private set
    private var mStartTime: Long = 0

    private var mCompletionListener: OnCompletionListener? = null

    interface OnCompletionListener {
        fun onCompletion()
    }

    init {
        mContext = context
        mSyncHandler = Handler(Looper.getMainLooper())
    }

    fun play(audioUri: Uri?, pattern: GlyphPattern?): Boolean {
        if (audioUri == null || pattern == null) {
            if (DEBUG) Log.e(TAG, "Invalid audio URI or pattern")
            return false
        }

        if (!GlyphComposerParser.isValid(pattern)) {
            if (DEBUG) Log.e(TAG, "Invalid pattern data")
            return false
        }

        stop()

        mPattern = pattern
        mCurrentFrameIndex = 0

        try {
            mMediaPlayer = MediaPlayer()
            mMediaPlayer.setAudioAttributes(
                Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )

            mMediaPlayer.setDataSource(mContext, audioUri)
            mMediaPlayer.setOnPreparedListener({ mp ->
                if (DEBUG) Log.d(TAG, "MediaPlayer prepared, starting playback")
                mp.start()
                this.isPlaying = true
                mStartTime = System.currentTimeMillis()
                startGlyphSync()
            })

            mMediaPlayer.setOnCompletionListener({ mp ->
                if (DEBUG) Log.d(TAG, "Playback completed")
                stop()
                if (mCompletionListener != null) {
                    mCompletionListener!!.onCompletion()
                }
            })

            mMediaPlayer.setOnErrorListener({ mp, what, extra ->
                if (DEBUG) Log.e(TAG, "MediaPlayer error: " + what + ", " + extra)
                stop()
                true
            })

            mMediaPlayer.prepareAsync()
            return true
        } catch (e: IOException) {
            if (DEBUG) Log.e(TAG, "Error setting up MediaPlayer", e)
            return false
        }
    }

    fun playWithoutSync(audioUri: Uri?): Boolean {
        if (audioUri == null) {
            if (DEBUG) Log.e(TAG, "Invalid audio URI")
            return false
        }

        stop()

        try {
            mMediaPlayer = MediaPlayer()
            mMediaPlayer.setAudioAttributes(
                Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )

            mMediaPlayer.setDataSource(mContext, audioUri)
            mMediaPlayer.setOnPreparedListener({ mp ->
                if (DEBUG) Log.d(TAG, "MediaPlayer prepared (no sync)")
                mp.start()
                this.isPlaying = true
            })

            mMediaPlayer.setOnCompletionListener({ mp ->
                if (DEBUG) Log.d(TAG, "Playback completed (no sync)")
                stop()
                if (mCompletionListener != null) {
                    mCompletionListener!!.onCompletion()
                }
            })

            mMediaPlayer.prepareAsync()
            return true
        } catch (e: IOException) {
            if (DEBUG) Log.e(TAG, "Error setting up MediaPlayer", e)
            return false
        }
    }

    fun stop() {
        this.isPlaying = false

        if (mSyncHandler != null) {
            mSyncHandler.removeCallbacksAndMessages(null)
        }

        if (mMediaPlayer != null) {
            try {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop()
                }
                mMediaPlayer.release()
            } catch (e: IllegalStateException) {
                if (DEBUG) Log.e(TAG, "Error stopping MediaPlayer", e)
            }
            mMediaPlayer = null
        }

        AnimationManager.stopAll()
        mPattern = null
        mCurrentFrameIndex = 0
    }

    private fun startGlyphSync() {
        if (mPattern == null || mPattern!!.getFrames() == null) {
            if (DEBUG) Log.e(TAG, "No pattern to sync")
            return
        }

        if (DEBUG) Log.d(TAG, "Starting Glyph sync with " + mPattern!!.getFrames().size + " frames")
        scheduleNextFrame()
    }

    private fun scheduleNextFrame() {
        if (!this.isPlaying || mPattern == null) {
            return
        }

        val frames: MutableList<GlyphFrame> = mPattern!!.getFrames()
        if (mCurrentFrameIndex >= frames.size) {
            if (DEBUG) Log.d(TAG, "All frames completed")
            return
        }

        val frame: GlyphFrame = frames.get(mCurrentFrameIndex)
        val currentTime = System.currentTimeMillis() - mStartTime
        var delay: Long = frame.getTimestamp() - currentTime

        if (delay < 0) delay = 0

        if (DEBUG) Log.d(
            TAG,
            "Scheduling frame " + mCurrentFrameIndex + " at " + frame.getTimestamp() + "ms (delay: " + delay + "ms)"
        )

        mSyncHandler.postDelayed({
            if (this.isPlaying) {
                activateGlyphFrame(frame)
                mCurrentFrameIndex++
                scheduleNextFrame()
            }
        }, delay)
    }

    private fun activateGlyphFrame(frame: GlyphFrame?) {
        if (frame == null || frame.getZones() == null) {
            return
        }

        if (DEBUG) Log.d(
            TAG, "Activating frame: zones=" + frame.getZones().size +
                    ", brightness=" + frame.getBrightness() +
                    ", duration=" + frame.getDuration() + "ms"
        )

        val brightness = scaleBrightness(frame.getBrightness())

        for (zone in frame.getZones()) {
            AnimationManager.singleLedBlink(mContext, zone, brightness, frame.getDuration())
        }
    }

    private fun scaleBrightness(patternBrightness: Int): Int {
        val userBrightness = SettingsManager.glyphBrightness
        return (patternBrightness * userBrightness) / 100
    }

    fun setOnCompletionListener(listener: OnCompletionListener?) {
        mCompletionListener = listener
    }

    companion object {
        private const val TAG = "GlyphSyncPlayer"
        private const val DEBUG = true
    }
}