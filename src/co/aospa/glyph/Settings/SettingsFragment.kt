/*
 * Copyright (C) 2015 The CyanogenMod Project
 *               2017-2019 The LineageOS Project
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
package co.aospa.glyph.Settings

import co.aospa.glyph.Constants.Constants
import co.aospa.glyph.Manager.SettingsManager

class SettingsFragment : SettingsBasePreferenceFragment(), OnPreferenceChangeListener,
    OnCheckedChangeListener {
    private var mSwitchBar: MainSwitchPreference? = null

    private var mFlipPreference: SwitchPreferenceCompat? = null
    private var mAutoBrightnessPreference: SwitchPreferenceCompat? = null
    private var mBrightnessPreference: SeekBarPreference? = null
    private var mNotifsPreference: PrimarySwitchPreference? = null
    private var mCallPreference: PrimarySwitchPreference? = null
    private var mChargingCategoryPreference: PreferenceCategory? = null
    private var mChargingLevelPreference: SwitchPreferenceCompat? = null
    private var mChargingPowersharePreference: SwitchPreferenceCompat? = null
    private var mVolumeLevelPreference: SwitchPreferenceCompat? = null
    private var mShakeTorchPreference: SwitchPreferenceCompat? = null
    private var mShakeSensitivityPreference: SeekBarPreference? = null
    private var mMusicVisualizerPreference: SwitchPreferenceCompat? = null
    private var mFlipRingerModePreference: ListPreference? = null
    private var mComposerEnablePreference: SwitchPreferenceCompat? = null
    private var mComposerFallbackPreference: SwitchPreferenceCompat? = null

    private var mContentResolver: ContentResolver? = null
    private var mSettingObserver: SettingObserver? = null
    private var mSchedulePreference: Preference? = null

    private val mHandler: Handler = Handler()

    public override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.glyph_settings)

        mContentResolver = getActivity().getContentResolver()
        mSettingObserver = SettingsFragment.SettingObserver()
        mSettingObserver!!.register(mContentResolver)

        val glyphEnabled = SettingsManager.isGlyphEnabled()

        mSwitchBar = findPreference(Constants.GLYPH_ENABLE) as MainSwitchPreference
        mSwitchBar.addOnSwitchChangeListener(this)
        mSwitchBar.setChecked(glyphEnabled)

        mFlipPreference = findPreference(Constants.GLYPH_FLIP_ENABLE) as SwitchPreferenceCompat
        mFlipPreference.setEnabled(glyphEnabled)
        mFlipPreference.setOnPreferenceChangeListener(this)

        mAutoBrightnessPreference =
            findPreference(Constants.GLYPH_AUTO_BRIGHTNESS_ENABLE) as SwitchPreferenceCompat
        mAutoBrightnessPreference.setEnabled(glyphEnabled)
        mAutoBrightnessPreference.setOnPreferenceChangeListener(this)
        mAutoBrightnessPreference.setChecked(SettingsManager.isGlyphAutoBrightnessEnabled())
        if (ResourceUtils.getString("glyph_light_sensor").isBlank()) {
            getPreferenceScreen().removePreference(mAutoBrightnessPreference)
        }

        mBrightnessPreference = findPreference(Constants.GLYPH_BRIGHTNESS) as SeekBarPreference
        if (mAutoBrightnessPreference.isChecked()) {
            mBrightnessPreference.setEnabled(false)
        } else {
            mBrightnessPreference.setEnabled(glyphEnabled)
        }
        mBrightnessPreference.setMin(1)
        mBrightnessPreference.setMax(Constants.getBrightnessLevels().size)
        mBrightnessPreference.setValue(SettingsManager.getGlyphBrightnessSetting())
        mBrightnessPreference.setUpdatesContinuously(true)
        mBrightnessPreference.setOnPreferenceChangeListener(this)

        mNotifsPreference = findPreference(Constants.GLYPH_NOTIFS_ENABLE) as PrimarySwitchPreference
        mNotifsPreference.setChecked(SettingsManager.isGlyphNotifsEnabled())
        mNotifsPreference.setEnabled(glyphEnabled)
        mNotifsPreference.setSwitchEnabled(glyphEnabled)
        mNotifsPreference.setOnPreferenceChangeListener(this)

        mCallPreference = findPreference(Constants.GLYPH_CALL_ENABLE) as PrimarySwitchPreference
        mCallPreference.setChecked(SettingsManager.isGlyphCallEnabled())
        mCallPreference.setEnabled(glyphEnabled)
        mCallPreference.setSwitchEnabled(glyphEnabled)
        mCallPreference.setOnPreferenceChangeListener(this)

        mChargingCategoryPreference =
            findPreference(Constants.GLYPH_CHARGING_CATEGORY) as PreferenceCategory
        mChargingCategoryPreference.setVisible(glyphChargingMeterAvailable || glyphPowershareAvailable)

        mChargingLevelPreference =
            findPreference(Constants.GLYPH_CHARGING_LEVEL_ENABLE) as SwitchPreferenceCompat
        mChargingLevelPreference.setDefaultValue(glyphChargingMeterAvailable)
        mChargingLevelPreference.setEnabled(glyphEnabled)
        mChargingLevelPreference.setVisible(glyphChargingMeterAvailable)
        mChargingLevelPreference.setOnPreferenceChangeListener(this)

        mChargingPowersharePreference =
            findPreference(Constants.GLYPH_CHARGING_POWERSHARE_ENABLE) as SwitchPreferenceCompat
        mChargingPowersharePreference.setDefaultValue(glyphPowershareAvailable)
        mChargingPowersharePreference.setEnabled(glyphEnabled)
        mChargingPowersharePreference.setVisible(glyphPowershareAvailable)
        mChargingPowersharePreference.setOnPreferenceChangeListener(this)

        mVolumeLevelPreference =
            findPreference(Constants.GLYPH_VOLUME_LEVEL_ENABLE) as SwitchPreferenceCompat
        mVolumeLevelPreference.setEnabled(glyphEnabled)
        mVolumeLevelPreference.setOnPreferenceChangeListener(this)

        mShakeTorchPreference =
            findPreference(Constants.GLYPH_SHAKE_TORCH_ENABLE) as SwitchPreferenceCompat
        mShakeTorchPreference.setEnabled(glyphEnabled)
        mShakeTorchPreference.setOnPreferenceChangeListener(this)

        mShakeSensitivityPreference =
            findPreference(Constants.GLYPH_SHAKE_SENSITIVITY) as SeekBarPreference
        mShakeSensitivityPreference.setEnabled(glyphEnabled && mShakeTorchPreference.isChecked())
        mShakeSensitivityPreference.setUpdatesContinuously(false)
        mShakeSensitivityPreference.setOnPreferenceChangeListener(this)

        mMusicVisualizerPreference =
            findPreference(Constants.GLYPH_MUSIC_VISUALIZER_ENABLE) as SwitchPreferenceCompat
        mMusicVisualizerPreference.setEnabled(glyphEnabled)
        mMusicVisualizerPreference.setOnPreferenceChangeListener(this)

        mFlipRingerModePreference =
            findPreference(Constants.GLYPH_FLIP_RINGER_MODE) as ListPreference
        mFlipRingerModePreference.setEnabled(glyphEnabled && mFlipPreference.isChecked())
        mFlipRingerModePreference.setOnPreferenceChangeListener(this)

        mComposerEnablePreference =
            findPreference(Constants.GLYPH_COMPOSER_ENABLE) as SwitchPreferenceCompat
        mComposerEnablePreference.setEnabled(glyphEnabled)
        mComposerEnablePreference.setOnPreferenceChangeListener(this)

        mComposerFallbackPreference =
            findPreference(Constants.GLYPH_COMPOSER_FALLBACK) as SwitchPreferenceCompat?
        mComposerFallbackPreference.setEnabled(glyphEnabled && mComposerEnablePreference.isChecked())
        mComposerFallbackPreference.setOnPreferenceChangeListener(this)

        mSchedulePreference = findPreference(Constants.GLYPH_SCHEDULE) as Preference?
        updateScheduleSummary()

        mHandler.post({ ServiceUtils.checkGlyphService() })
    }

    public override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        val preferenceKey: String = preference.getKey()

        if (preferenceKey == Constants.GLYPH_CALL_ENABLE) {
            SettingsManager.setGlyphCallEnabled(!mCallPreference.isChecked())
        }

        if (preferenceKey == Constants.GLYPH_NOTIFS_ENABLE) {
            SettingsManager.setGlyphNotifsEnabled(!mNotifsPreference.isChecked())
        }

        if (preferenceKey == Constants.GLYPH_AUTO_BRIGHTNESS_ENABLE) {
            mBrightnessPreference.setEnabled(mAutoBrightnessPreference.isChecked())
        }

        if (preferenceKey == Constants.GLYPH_SHAKE_TORCH_ENABLE) {
            val enabled = newValue as Boolean
            mShakeSensitivityPreference.setEnabled(enabled)
            mHandler.postDelayed({
                if (enabled) {
                    ShakeManager.startShakeService(getContext())
                } else {
                    ShakeManager.stopShakeService(getContext())
                }
            }, 100)
        }

        if (preferenceKey == Constants.GLYPH_SHAKE_SENSITIVITY) {
            if (mShakeTorchPreference.isChecked()) {
                mHandler.postDelayed({
                    ShakeManager.restartShakeService(getContext())
                }, 100)
            }
        }

        if (preferenceKey == Constants.GLYPH_FLIP_RINGER_MODE) {
            val mode = (newValue as String?)!!.toInt()
            Settings.Secure.putInt(
                mContentResolver,
                Constants.GLYPH_FLIP_RINGER_MODE, mode
            )
        }

        if (preferenceKey == Constants.GLYPH_FLIP_ENABLE) {
            val flipEnabled = newValue as Boolean
            mFlipRingerModePreference.setEnabled(flipEnabled && SettingsManager.isGlyphEnabled())
        }

        if (preferenceKey == Constants.GLYPH_COMPOSER_ENABLE) {
            val enabled = newValue as Boolean
            SettingsManager.setGlyphComposerEnabled(enabled)
            if (mComposerFallbackPreference != null) {
                mComposerFallbackPreference.setEnabled(enabled)
            }
        }

        mHandler.post({ ServiceUtils.checkGlyphService() })

        return true
    }

    public override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        SettingsManager.enableGlyph(isChecked)

        mSwitchBar.setChecked(isChecked)

        mFlipPreference.setEnabled(isChecked)
        mAutoBrightnessPreference.setEnabled(isChecked)
        mBrightnessPreference.setEnabled(isChecked && !mAutoBrightnessPreference.isChecked())
        mNotifsPreference.setEnabled(isChecked)
        mNotifsPreference.setSwitchEnabled(isChecked)
        mCallPreference.setEnabled(isChecked)
        mCallPreference.setSwitchEnabled(isChecked)
        mChargingLevelPreference.setEnabled(isChecked)
        mChargingPowersharePreference.setEnabled(isChecked)
        mVolumeLevelPreference.setEnabled(isChecked)
        mShakeTorchPreference.setEnabled(isChecked)
        mShakeSensitivityPreference.setEnabled(isChecked && mShakeTorchPreference.isChecked())
        mMusicVisualizerPreference.setEnabled(isChecked)
        mFlipRingerModePreference.setEnabled(isChecked && mFlipPreference.isChecked())
        mComposerEnablePreference.setEnabled(isChecked)
        mComposerFallbackPreference.setEnabled(isChecked && mComposerEnablePreference.isChecked())

        mHandler.post({
            ServiceUtils.checkGlyphService()
            updateTorchTile()
        })
    }

    private fun updateTorchTile() {
        try {
            val intent: Intent = Intent("co.aospa.glyph.UPDATE_TORCH_TILE")
            requireContext().sendBroadcast(intent)
        } catch (e: Exception) {
        }
    }

    public override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (Constants.GLYPH_NOTIFS_ENABLE == preference.getKey()) {
            if (!ServiceUtils.isNotificationServiceEnabled) {
                Builder(requireContext())
                    .setTitle(R.string.glyph_settings_notifs_permission_dialog_title)
                    .setMessage(R.string.glyph_settings_notifs_permission_dialog_message)
                    .setPositiveButton(android.R.string.ok, { dialog, which ->
                        val intent: Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        requireContext().startActivity(intent)
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
                return true
            }
        }

        if ("glyph_settings_composer_apply" == preference.getKey()) {
            val intent: Intent = Intent(getActivity(), GlyphPatternSelectorActivity::class.java)
            startActivity(intent)
            return true
        }

        if ("glyph_settings_composer_preview" == preference.getKey()) {
            val intent: Intent = Intent(getActivity(), GlyphPatternPreviewActivity::class.java)
            startActivity(intent)
            return true
        }

        if ("glyph_settings_composer_create" == preference.getKey()) {
            val intent: Intent = Intent(getActivity(), GlyphPatternCreatorActivity::class.java)
            startActivity(intent)
            return true
        }

        return super.onPreferenceTreeClick(preference)
    }

    public override fun onDestroy() {
        mSettingObserver!!.unregister(mContentResolver)
        super.onDestroy()
    }

    public override fun onResume() {
        super.onResume()
        updateScheduleSummary()
    }

    private fun updateScheduleSummary() {
        if (mSchedulePreference != null) {
            val summary: String = GlyphScheduleManager.getScheduleSummary(requireContext())
            mSchedulePreference.setSummary(summary)
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
                    Constants.GLYPH_CALL_ENABLE
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

        public override fun onChange(selfChange: Boolean, uri: Uri) {
            super.onChange(selfChange, uri)
            if (uri.equals(Settings.Secure.getUriFor(Constants.GLYPH_ENABLE))) {
                mSwitchBar.setChecked(SettingsManager.isGlyphEnabled())
            }
            if (uri.equals(Settings.Secure.getUriFor(Constants.GLYPH_CALL_ENABLE))) {
                mCallPreference.setChecked(SettingsManager.isGlyphCallEnabled())
            }
            if (uri.equals(Settings.Secure.getUriFor(Constants.GLYPH_NOTIFS_ENABLE))) {
                mNotifsPreference.setChecked(SettingsManager.isGlyphNotifsEnabled())
            }
        }
    }

    companion object {
        private val CHARGING_LEVELS: Int =
            ResourceUtils.getInteger("glyph_settings_battery_levels_num")
        private val glyphChargingMeterAvailable = CHARGING_LEVELS > 0

        private val POWERSHARE_ACTIVE: String =
            ResourceUtils.getString("glyph_settings_paths_powershare_active_absolute")
        private val POWERSHARE_ENABLED: String =
            ResourceUtils.getString("glyph_settings_paths_powershare_enabled_absolute")
        private val glyphPowershareAvailable =
            !POWERSHARE_ACTIVE.isBlank() && !POWERSHARE_ENABLED.isBlank()
    }
}
