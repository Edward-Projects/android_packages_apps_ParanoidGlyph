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
package co.aospa.glyph.Services

import android.app.Service
import co.aospa.glyph.Manager.SettingsManager

class CallReceiverService : Service() {
    private var mAudioManager: AudioManager? = null
    private var mGlyphSyncPlayer: GlyphSyncPlayer? = null

    private var thread: HandlerThread? = null
    private var mThreadHandler: Handler? = null

    private var isComposerPatternPlaying = false
    private var composerPatternStartTime: Long = 0

    private val playCall: Runnable = object : Runnable {
        override fun run() {
            playRingtoneWithGlyphSync()
        }
    }

    public override fun onCreate() {
        if (DEBUG) Log.d(TAG, "Creating service")

        thread = HandlerThread("CallReceiverService")
        thread.start()
        val looper: Looper? = thread.getLooper()
        mThreadHandler = Handler(looper)

        mAudioManager = getSystemService(AudioManager::class.java)
        mAudioManager.addOnModeChangedListener(
            { cmd -> mThreadHandler.post(cmd) },
            mAudioManagerOnModeChangedListener
        )
        mAudioManagerOnModeChangedListener.onModeChanged(mAudioManager.getMode())

        // Initialize Glyph Sync Player
        mGlyphSyncPlayer = GlyphSyncPlayer(this)
        mGlyphSyncPlayer.setOnCompletionListener(OnCompletionListener {
            if (DEBUG) Log.d(TAG, "Ringtone playback completed")
        })

        val callReceiver: IntentFilter = IntentFilter()
        callReceiver.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        registerReceiver(mCallReceiver, callReceiver)
    }

    public override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "Starting service")
        return START_STICKY
    }

    public override fun onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying service")
        this.unregisterReceiver(mCallReceiver)
        mAudioManager.removeOnModeChangedListener(mAudioManagerOnModeChangedListener)
        disableCallAnimation()

        if (mGlyphSyncPlayer != null) {
            mGlyphSyncPlayer.stop()
            mGlyphSyncPlayer = null
        }

        thread.quit()
        super.onDestroy()
    }

    public override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun enableCallAnimation() {
        if (DEBUG) Log.d(TAG, "enableCallAnimation")
        mThreadHandler.post(playCall)
    }

    private fun disableCallAnimation() {
        if (DEBUG) Log.d(TAG, "disableCallAnimation")
        if (mThreadHandler.hasCallbacks(playCall)) mThreadHandler.removeCallbacks(playCall)

        if (isComposerPatternPlaying) {
            isComposerPatternPlaying = false
            mThreadHandler.removeCallbacksAndMessages(null)
            if (DEBUG) Log.d(TAG, "Stopped composer pattern playback")
        }

        AnimationManager.stopCall()
    }

    private fun playRingtoneWithGlyphSync() {
        if (!SettingsManager.isGlyphComposerEnabled()) {
            if (DEBUG) Log.d(TAG, "Glyph Composer disabled, using standard animation")
            AnimationManager.playCall(SettingsManager.getGlyphCallAnimation())
            return
        }

        val ringtoneUri: Uri? =
            RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)

        if (ringtoneUri == null) {
            if (DEBUG) Log.w(TAG, "No ringtone URI, falling back to standard animation")
            AnimationManager.playCall(SettingsManager.getGlyphCallAnimation())
            return
        }

        if (DEBUG) Log.d(TAG, "Ringtone URI: " + ringtoneUri)

        val audioPath = getRealPathFromUri(ringtoneUri)

        if (audioPath != null) {
            val glyphPatternPath: String? = GlyphComposerParser.getGlyphPatternPath(audioPath)

            if (glyphPatternPath != null) {
                val pattern: GlyphPattern? = GlyphComposerParser.parseFromFile(glyphPatternPath)

                if (pattern != null && GlyphComposerParser.isValid(pattern)) {
                    if (DEBUG) Log.d(TAG, "Playing ringtone with Glyph Composer sync")
                    playGlyphPatternOnly(pattern)
                    return
                }
            }
        }

        if (SettingsManager.useComposerFallback()) {
            if (DEBUG) Log.d(TAG, "No Glyph pattern found, using fallback animation")
            AnimationManager.playCall(SettingsManager.getGlyphCallAnimation())
        } else {
            if (DEBUG) Log.d(TAG, "No Glyph pattern found, fallback disabled, no animation")
        }
    }

    private fun playGlyphPatternOnly(pattern: GlyphPattern?) {
        if (pattern == null || pattern.getFrames() == null) {
            return
        }

        isComposerPatternPlaying = true
        composerPatternStartTime = System.currentTimeMillis()

        Handler(Looper.getMainLooper()).post({
            scheduleGlyphFrames(pattern, 0, composerPatternStartTime)
        })
    }

    private fun scheduleGlyphFrames(pattern: GlyphPattern, frameIndex: Int, startTime: Long) {
        if (!isComposerPatternPlaying) {
            if (DEBUG) Log.d(TAG, "Composer pattern stopped by user action")
            return
        }

        if (frameIndex >= pattern.getFrames().size) {
            if (DEBUG) Log.d(TAG, "All Glyph frames completed")
            isComposerPatternPlaying = false
            return
        }

        val frame: GlyphFrame = pattern.getFrames().get(frameIndex)
        val currentTime = System.currentTimeMillis() - startTime
        var delay: Long = frame.getTimestamp() - currentTime

        if (delay < 0) delay = 0

        mThreadHandler.postDelayed({
            if (!isComposerPatternPlaying) return@postDelayed
            activateGlyphFrame(frame)
            scheduleGlyphFrames(pattern, frameIndex + 1, startTime)
        }, delay)
    }

    private fun activateGlyphFrame(frame: GlyphFrame?) {
        if (frame == null || frame.getZones() == null) {
            return
        }

        if (!AnimationManager.canPlayGlyphComposer()) {
            if (DEBUG) Log.d(TAG, "Cannot play frame, other animation active")
            return
        }

        val brightness = scaleBrightness(frame.getBrightness())
        AnimationManager.playGlyphFrame(this, frame.getZones(), brightness, frame.getDuration())
    }

    private fun scaleBrightness(patternBrightness: Int): Int {
        val userBrightness = SettingsManager.getGlyphBrightness()
        return (patternBrightness * userBrightness) / 100
    }

    private fun getRealPathFromUri(uri: Uri?): String? {
        if (uri == null) return null

        if ("file" == uri.getScheme()) {
            return uri.getPath()
        }

        if ("content" == uri.getScheme()) {
            var cursor: android.database.Cursor? = null
            try {
                val projection = arrayOf<String?>(android.provider.MediaStore.Audio.Media.DATA)
                cursor = getContentResolver().query(uri, projection, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex: Int =
                        cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA)
                    return cursor.getString(columnIndex)
                }
            } catch (e: Exception) {
                if (DEBUG) Log.e(TAG, "Error getting path from URI", e)
            } finally {
                if (cursor != null) {
                    cursor.close()
                }
            }
        }

        return null
    }

    private val mCallReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        public override fun onReceive(context: Context?, intent: Intent) {
            if (intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
                val state: String = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                    if (DEBUG) Log.d(TAG, "EXTRA_STATE_RINGING")
                    enableCallAnimation()
                }
                if ((state == TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                    if (DEBUG) Log.d(TAG, "EXTRA_STATE_OFFHOOK")
                    disableCallAnimation()
                }
                if (state == TelephonyManager.EXTRA_STATE_IDLE) {
                    if (DEBUG) Log.d(TAG, "EXTRA_STATE_IDLE")
                    disableCallAnimation()
                }
            }
        }
    }

    private val mAudioManagerOnModeChangedListener: AudioManager.OnModeChangedListener =
        object : OnModeChangedListener() {
            public override fun onModeChanged(mode: Int) {
                if (mode != AudioManager.MODE_RINGTONE) {
                    if (DEBUG) Log.d(TAG, "mAudioManagerOnModeChangedListener: " + mode)
                    disableCallAnimation()
                }
            }
        }

    companion object {
        private const val TAG = "GlyphCallReceiverService"
        private const val DEBUG = true
    }
}