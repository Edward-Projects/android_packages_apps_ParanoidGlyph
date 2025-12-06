/*
 * Copyright (C) 2023-2024 Paranoid Android
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

class VolumeLevelService : Service() {
    private var thread: HandlerThread? = null
    private var mThreadHandler: Handler? = null
    private var mVolumeChangeReceiver: VolumeChangeReceiver? = null

    private var mContext: Context? = null

    private var audioManager: AudioManager? = null
    private val dismissVolume: Runnable = object : Runnable {
        override fun run() {
            AnimationManager.dismissVolume(mContext)
        }
    }

    public override fun onCreate() {
        if (DEBUG) Log.d(TAG, "Creating service")

        mContext = this

        // Add a handler thread
        thread = HandlerThread("VolumeLevelService")
        thread.start()
        val looper: Looper? = thread.getLooper()
        mThreadHandler = Handler(looper)

        audioManager = getSystemService(AudioManager::class.java) as AudioManager
        mVolumeChangeReceiver = VolumeChangeReceiver()
        registerReceiver(mVolumeChangeReceiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION"))
    }

    public override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "Starting service")
        return START_STICKY
    }

    public override fun onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying service")
        unregisterReceiver(mVolumeChangeReceiver)
        thread.quit()
        super.onDestroy()
    }

    public override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private inner class VolumeChangeReceiver : BroadcastReceiver() {
        public override fun onReceive(context: Context?, intent: Intent) {
            if ("android.media.VOLUME_CHANGED_ACTION" == intent.getAction()) {
                val streamType: Int =
                    intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                val currentVolume: Int =
                    intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", -1)
                val oldVolume: Int =
                    intent.getIntExtra("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE", -1)

                // Only check streams which are shown in the volume panel
                if ((streamType >= 0 && streamType <= AudioSystem.NUM_STREAMS) && currentVolume >= 0 && oldVolume >= 0) {
                    val maxVolume: Int = audioManager.getStreamMaxVolume(streamType)
                    val oldVolumePercent = (Math.round(100.0 / maxVolume * oldVolume)).toInt()
                    val currentVolumePercent =
                        (Math.round(100.0 / maxVolume * currentVolume)).toInt()

                    if (oldVolumePercent != currentVolumePercent) {
                        if (mThreadHandler.hasCallbacks(dismissVolume)) {
                            mThreadHandler.removeCallbacks(dismissVolume)
                        }
                        if (DEBUG) {
                            Log.d(
                                TAG, "Volume level changed for stream type " + streamType +
                                        ": oldVolumePercent: " + oldVolumePercent + ", currentVolumePercent: " + currentVolumePercent
                            )
                        }
                        mThreadHandler.post({
                            AnimationManager.playVolume(context, currentVolumePercent, false)
                        })
                        mThreadHandler.postDelayed(dismissVolume, 3000)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "GlyphVolumeLevelService"
        private const val DEBUG = true
    }
}
