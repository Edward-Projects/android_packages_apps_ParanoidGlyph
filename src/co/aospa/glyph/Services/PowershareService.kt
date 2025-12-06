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
import co.aospa.glyph.Utils.FileUtils

class PowershareService : Service() {
    private var mPowershareActiveObserver: PowershareActiveObserver? = null
    private var mContext: Context? = null

    public override fun onCreate() {
        if (DEBUG) Log.d(TAG, "Creating service")
        mPowershareActiveObserver = PowershareActiveObserver()
        mContext = this
    }

    public override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "Starting service")
        mFileObserver.startWatching()
        mPowershareActiveObserver!!.startWatching()
        return START_STICKY
    }

    public override fun onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying service")
        mFileObserver.stopWatching()
        mPowershareActiveObserver!!.stopWatching()
        super.onDestroy()
    }

    public override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun onPowershareEnabled() {
        if (DEBUG) Log.e(TAG, "onPowershareEnabled")
        mPowershareActiveObserver!!.continueWatching()
    }

    private fun onPowershareDisabled() {
        if (DEBUG) Log.e(TAG, "onPowershareDisabled")
        mPowershareActiveObserver!!.pauseWatching()
    }

    private val mFileObserver: FileObserver =
        object : FileObserver(POWERSHARE_ENABLED, FileObserver.MODIFY) {
            public override fun onEvent(event: Int, file: String?) {
                this.checkIfPowerShareIsEnabled()
            }

            public override fun startWatching() {
                if (DEBUG) Log.e(TAG, "FileObserver: startWatching")
                this.checkIfPowerShareIsEnabled()
                super.startWatching()
            }

            private fun checkIfPowerShareIsEnabled() {
                if (DEBUG) Log.e(
                    TAG, "FileObserver: checkIfPowerShareIsEnabled: " + FileUtils.readLineInt(
                        POWERSHARE_ENABLED
                    )
                )
                if (FileUtils.readLineInt(POWERSHARE_ENABLED) == 1) {
                    onPowershareEnabled()
                } else {
                    onPowershareDisabled()
                }
            }
        }

    private inner class PowershareActiveObserver : Thread() {
        private var lastState = false
        private var pause = true
        private var ended = false

        private val mPowershareActiveObserverLock = Any()

        fun startWatching() {
            if (DEBUG) Log.e(TAG, "PowershareActiveObserver: startWatching")
            if (super.isAlive()) return
            super.start()
        }

        fun continueWatching() {
            if (DEBUG) Log.e(TAG, "PowershareActiveObserver: continueWatching")
            if (!pause) return
            pause = false
            synchronized(mPowershareActiveObserverLock) {
                (mPowershareActiveObserverLock as Object).notify()
            }
        }

        fun pauseWatching() {
            if (DEBUG) Log.e(TAG, "PowershareActiveObserver: pauseWatching")
            if (pause) return
            lastState = false
            pause = true
        }

        fun stopWatching() {
            if (DEBUG) Log.e(TAG, "PowershareActiveObserver: stopWatching")
            if (pause) this.continueWatching()
            ended = true
        }

        fun updatePowershareState() {
            if (DEBUG) Log.d(
                TAG, "updatePowershareState: " + FileUtils.readLineInt(
                    POWERSHARE_ACTIVE
                )
            )
            if (FileUtils.readLineInt(POWERSHARE_ACTIVE) == 1) {
                if (lastState) return
                lastState = true
                AnimationManager.playCsv(mContext, "powershare", true)
            } else {
                lastState = false
            }
        }

        override fun run() {
            if (DEBUG) Log.e(TAG, "PowershareActiveObserver: run")
            while (!ended) {
                synchronized(mPowershareActiveObserverLock) {
                    if (pause) {
                        try {
                            if (DEBUG) Log.d(TAG, "mPowershareActiveObserverLock.wait()")
                            (mPowershareActiveObserverLock as Object).wait()
                        } catch (e: InterruptedException) {
                        }
                    }
                }
                updatePowershareState()
                try {
                    sleep(500)
                } catch (e: InterruptedException) {
                }
            }
        }
    }

    companion object {
        private const val TAG = "GlyphPowershareService"
        private const val DEBUG = true

        private val POWERSHARE_ACTIVE: String =
            ResourceUtils.getString("glyph_settings_paths_powershare_active_absolute")
        private val POWERSHARE_ENABLED: String =
            ResourceUtils.getString("glyph_settings_paths_powershare_enabled_absolute")
    }
}
