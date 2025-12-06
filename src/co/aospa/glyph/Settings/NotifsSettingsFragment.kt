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
package co.aospa.glyph.Settings

import android.content.pm.ApplicationInfo
import co.aospa.glyph.Constants.Constants
import co.aospa.glyph.Manager.SettingsManager

class NotifsSettingsFragment : SettingsBasePreferenceFragment(), OnPreferenceChangeListener,
    OnCheckedChangeListener {
    private var mScreen: PreferenceScreen? = null

    private var mSwitchBar: MainSwitchPreference? = null
    private var mCategory: PreferenceCategory? = null

    private val mEssentialApps: MutableList<String?> = ArrayList<String?>()
    private val mEssentialAppsNames: MutableList<String?> = ArrayList<String?>()

    private var mPackageManager: PackageManager? = null

    private var mListPreference: ListPreference? = null
    private var mMultiSelectListPreference: MultiSelectListPreference? = null

    private var mGlyphAnimationPreference: GlyphAnimationPreference? = null

    private val mHandler: Handler = Handler()

    public override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.glyph_notifs_settings)

        mScreen = this.getPreferenceScreen()
        getActivity().setTitle(R.string.glyph_settings_notifs_toggle_title)

        mSwitchBar = findPreference(Constants.GLYPH_NOTIFS_SUB_ENABLE) as MainSwitchPreference
        mSwitchBar.addOnSwitchChangeListener(this)
        mSwitchBar.setChecked(SettingsManager.isGlyphNotifsEnabled())

        mCategory = findPreference(Constants.GLYPH_NOTIFS_SUB_CATEGORY) as PreferenceCategory

        mListPreference = findPreference(Constants.GLYPH_NOTIFS_SUB_ANIMATIONS) as ListPreference
        mListPreference.setOnPreferenceChangeListener(this)
        mListPreference.setEntries(ResourceUtils.getNotificationAnimations())
        mListPreference.setEntryValues(ResourceUtils.getNotificationAnimations())
        if (!ArrayUtils.contains(
                ResourceUtils.getNotificationAnimations(),
                mListPreference.getValue()
            )
        ) {
            mListPreference.setValue(ResourceUtils.getString("glyph_settings_notifs_animations_default"))
        }

        mGlyphAnimationPreference =
            findPreference(Constants.GLYPH_NOTIFS_SUB_PREVIEW) as GlyphAnimationPreference

        mPackageManager = getActivity().getPackageManager()
        val mApps: MutableList<ApplicationInfo> =
            mPackageManager.getInstalledApplications(PackageManager.GET_GIDS)
        Collections.sort<ApplicationInfo?>(mApps, DisplayNameComparator(mPackageManager))
        for (app in mApps) {
            if (mPackageManager.getLaunchIntentForPackage(app.packageName) != null && !ArrayUtils.contains(
                    Constants.APPS_TO_IGNORE, app.packageName
                )
            ) { // apps with launcher intent
                val mSwitchPreference: SwitchPreferenceCompat =
                    SwitchPreferenceCompat(mScreen.getContext())
                mSwitchPreference.setKey(app.packageName)
                mSwitchPreference.setTitle(
                    " " + app.loadLabel(mPackageManager).toString()
                ) // add this space since the layout looks off otherwise
                mSwitchPreference.setIcon(app.loadIcon(mPackageManager))
                mSwitchPreference.setDefaultValue(true)
                mSwitchPreference.setOnPreferenceChangeListener(this)
                mCategory.addPreference(mSwitchPreference)

                mEssentialApps.add(app.packageName)
                mEssentialAppsNames.add(app.loadLabel(mPackageManager).toString())
            }
        }

        mMultiSelectListPreference =
            findPreference(Constants.GLYPH_NOTIFS_SUB_ESSENTIAL) as MultiSelectListPreference
        mMultiSelectListPreference.setOnPreferenceChangeListener(this)
        mMultiSelectListPreference.setEntries(
            mEssentialAppsNames.< T > toArray < T ? > (arrayOfNulls<CharSequence>(
                0
            ))
        )
        mMultiSelectListPreference.setEntryValues(
            mEssentialApps.< T > toArray < T ? > (arrayOfNulls<CharSequence>(
                0
            ))
        )
    }

    public override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mGlyphAnimationPreference.updateAnimation(
            SettingsManager.isGlyphNotifsEnabled(),
            SettingsManager.getGlyphNotifsAnimation(), 1500
        )
    }

    public override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val preferenceKey: String = preference.getKey()

        if (preferenceKey == Constants.GLYPH_NOTIFS_SUB_ANIMATIONS) {
            mGlyphAnimationPreference.updateAnimation(
                SettingsManager.isGlyphNotifsEnabled(),
                newValue.toString(), 1500
            )
        }

        if (preferenceKey == Constants.GLYPH_NOTIFS_SUB_ESSENTIAL) {
            //if (DEBUG) Log.d(TAG, "onPreferenceChange: " + newValue.toString());
        }

        //mHandler.post(() -> ServiceUtils.checkGlyphService());
        return true
    }

    public override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        SettingsManager.setGlyphNotifsEnabled(isChecked)
        ServiceUtils.checkGlyphService()
        mGlyphAnimationPreference.updateAnimation(
            isChecked,
            SettingsManager.getGlyphNotifsAnimation(), 1500
        )
    }
}
