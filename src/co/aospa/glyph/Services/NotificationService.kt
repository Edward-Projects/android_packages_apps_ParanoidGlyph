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

import co.aospa.glyph.Constants.Constants
import co.aospa.glyph.Manager.SettingsManager

class NotificationService : NotificationListenerService(),
    SharedPreferences.OnSharedPreferenceChangeListener {
    private var mContext: Context? = null

    private var mNotificationManager: NotificationManager? = null

    private var mContentResolver: ContentResolver? = null
    private var mSettingObserver: SettingObserver? = null

    private var mSharedPreferences: SharedPreferences? = null

    public override fun onCreate() {
        if (DEBUG) Log.d(TAG, "Creating service")

        mContext = this
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mContentResolver = getContentResolver()
        mSettingObserver = NotificationService.SettingObserver()
        mSettingObserver!!.register(mContentResolver)
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this)
        super.onCreate()
    }

    public override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "Starting service")
        onNotificationUpdated()
        return super.onStartCommand(intent, flags, startId)
    }

    public override fun onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying service")
        AnimationManager.stopEssential()
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        mSettingObserver!!.unregister(mContentResolver)
        super.onDestroy()
    }

    public override fun onBind(intent: Intent?): IBinder {
        return super.onBind(intent)
    }

    public override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (Constants.CONTEXT == null) return
        if (DEBUG) Log.d(TAG, "onNotificationPosted")
        if (!SettingsManager.isGlyphNotifsEnabled()) return
        val packageName: String? = sbn.getPackageName()
        val packageChannelID: String? = sbn.getNotification().getChannelId()
        var packageImportance = -1
        var packageCanBypassDnd = false
        val interruptionFilter: Int = mNotificationManager.getCurrentInterruptionFilter()
        try {
            val packageContext: Context = createPackageContext(packageName, 0)
            val packageNotificationManager: NotificationManager =
                packageContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val packageChannel: NotificationChannel? =
                packageNotificationManager.getNotificationChannel(packageChannelID)
            if (packageChannel != null) {
                packageImportance = packageChannel.getImportance()
                packageCanBypassDnd = packageChannel.canBypassDnd()
            }
        } catch (e: NameNotFoundException) {
        }
        if (DEBUG) Log.d(
            TAG,
            "onNotificationPosted: package:" + packageName + " | channel id: " + packageChannelID + " | importance: " + packageImportance + " | can bypass dnd: " + packageCanBypassDnd
        )
        if (SettingsManager.isGlyphNotifsAppEnabled(packageName)
            && !sbn.isOngoing() && !ArrayUtils.contains(
                Constants.APPS_TO_IGNORE,
                packageName
            ) && !ArrayUtils.contains(
                Constants.NOTIFS_TO_IGNORE, packageName + ":" + packageChannelID
            ) && (packageImportance >= NotificationManager.IMPORTANCE_DEFAULT || packageImportance == -1)
            && (interruptionFilter <= NotificationManager.INTERRUPTION_FILTER_ALL || packageCanBypassDnd)
        ) {
            AnimationManager.playCsv(mContext, SettingsManager.getGlyphNotifsAnimation())
        }
        if (SettingsManager.isGlyphNotifsAppEssential(packageName)
            && !sbn.isOngoing() && !ArrayUtils.contains(
                Constants.APPS_TO_IGNORE,
                packageName
            ) && !ArrayUtils.contains(
                Constants.NOTIFS_TO_IGNORE, packageName + ":" + packageChannelID
            ) && (packageImportance >= NotificationManager.IMPORTANCE_DEFAULT || packageImportance == -1)
            && (interruptionFilter <= NotificationManager.INTERRUPTION_FILTER_ALL || packageCanBypassDnd)
            && mNotificationManager.isNotificationPolicyAccessGranted()
        ) {
            AnimationManager.playEssential()
        }
    }

    public override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (DEBUG) Log.d(
            TAG,
            "onNotificationRemoved: package:" + sbn.getPackageName() + " | channel id: " + sbn.getNotification()
                .getChannelId()
        )
        onNotificationUpdated()
    }

    public override fun onSharedPreferenceChanged(preference: SharedPreferences?, key: String) {
        if (key == "glyph_settings_notifs_sub_essential") {
            if (DEBUG) Log.d(TAG, "onSharedPreferenceChanged: glyph_settings_notifs_sub_essential")
            onNotificationUpdated()
        }
    }

    private fun onNotificationUpdated() {
        if (DEBUG) Log.d(TAG, "onNotificationUpdated")
        var playEssential = false
        if (SettingsManager.isGlyphNotifsEnabled()) {
            if (!mNotificationManager.isNotificationPolicyAccessGranted()) return
            val activeNotifications: Array<StatusBarNotification> = getActiveNotifications()
            for (sbn in activeNotifications) {
                val packageName: String? = sbn.getPackageName()
                val packageChannelID: String? = sbn.getNotification().getChannelId()
                var packageImportance = -1
                var packageCanBypassDnd = false
                val interruptionFilter: Int = mNotificationManager.getCurrentInterruptionFilter()
                try {
                    val packageContext: Context = createPackageContext(packageName, 0)
                    val packageNotificationManager: NotificationManager =
                        packageContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val packageChannel: NotificationChannel? =
                        packageNotificationManager.getNotificationChannel(packageChannelID)
                    if (packageChannel != null) {
                        packageImportance = packageChannel.getImportance()
                        packageCanBypassDnd = packageChannel.canBypassDnd()
                    }
                } catch (e: NameNotFoundException) {
                }
                if (DEBUG) Log.d(
                    TAG,
                    "onNotificationUpdated: package:" + packageName + " | channel id: " + packageChannelID + " | importance: " + packageImportance + " | can bypass dnd: " + packageCanBypassDnd
                )
                if (SettingsManager.isGlyphNotifsAppEssential(packageName)
                    && !sbn.isOngoing() && !ArrayUtils.contains(
                        Constants.APPS_TO_IGNORE,
                        packageName
                    ) && !ArrayUtils.contains(
                        Constants.NOTIFS_TO_IGNORE, packageName + ":" + packageChannelID
                    ) && (packageImportance >= NotificationManager.IMPORTANCE_DEFAULT || packageImportance == -1)
                    && (interruptionFilter <= NotificationManager.INTERRUPTION_FILTER_ALL || packageCanBypassDnd)
                ) {
                    if (DEBUG) Log.d(
                        TAG,
                        "onNotificationUpdated: found essential notification | package:" + packageName
                    )
                    playEssential = true
                }
            }
        }
        if (playEssential) {
            AnimationManager.playEssential()
        } else {
            AnimationManager.stopEssential()
        }
    }

    private inner class SettingObserver : ContentObserver(Handler()) {
        fun register(cr: ContentResolver) {
            cr.registerContentObserver(
                Settings.Secure.getUriFor(
                    Constants.GLYPH_ENABLE
                ), false, this
            )
            cr.registerContentObserver(
                Settings.Secure.getUriFor(
                    Constants.GLYPH_NOTIFS_ENABLE
                ), false, this
            )
        }

        fun unregister(cr: ContentResolver) {
            cr.unregisterContentObserver(this)
        }

        public override fun onChange(selfChange: Boolean) {
            if (DEBUG) Log.d(TAG, "SettingObserver: onChange")
            onNotificationUpdated()
            super.onChange(selfChange)
        }
    }

    companion object {
        private const val TAG = "GlyphNotification"
        private const val DEBUG = true
    }
}
