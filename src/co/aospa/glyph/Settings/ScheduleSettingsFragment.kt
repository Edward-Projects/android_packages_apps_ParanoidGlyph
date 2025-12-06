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
package co.aospa.glyph.Settings

import android.app.TimePickerDialog

class ScheduleSettingsFragment : SettingsBasePreferenceFragment(),
    CompoundButton.OnCheckedChangeListener, Preference.OnPreferenceChangeListener {
    private var mScheduleSwitch: MainSwitchPreference? = null
    private var mDaysPreference: MultiSelectListPreference? = null
    private var mStartTimePreference: Preference? = null
    private var mEndTimePreference: Preference? = null
    private var mStatusPreference: Preference? = null

    public override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.glyph_schedule_settings)

        getActivity().setTitle("Glyph Schedule")

        mScheduleSwitch = findPreference("glyph_schedule_enable")
        mDaysPreference = findPreference("glyph_schedule_days")
        mStartTimePreference = findPreference("glyph_schedule_start_time")
        mEndTimePreference = findPreference("glyph_schedule_end_time")
        mStatusPreference = findPreference("glyph_schedule_status")

        if (mScheduleSwitch != null) {
            mScheduleSwitch.setChecked(GlyphScheduleManager.isScheduleEnabled(requireContext()))
            mScheduleSwitch.addOnSwitchChangeListener(this)
        }

        if (mDaysPreference != null) {
            mDaysPreference.setOnPreferenceChangeListener(this)
        }

        if (mStartTimePreference != null) {
            mStartTimePreference.setOnPreferenceClickListener({ preference ->
                showTimePickerDialog(true)
                true
            })
        }

        if (mEndTimePreference != null) {
            mEndTimePreference.setOnPreferenceClickListener({ preference ->
                showTimePickerDialog(false)
                true
            })
        }

        updatePreferences()
    }

    public override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        GlyphScheduleManager.setScheduleEnabled(requireContext(), isChecked)
        updatePreferences()
    }

    public override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        if (preference === mDaysPreference) {
            val selectedDays = newValue as MutableSet<String?>?
            GlyphScheduleManager.setScheduleDays(requireContext(), selectedDays)
            updatePreferences()
            return true
        }
        return false
    }

    private fun showTimePickerDialog(isStartTime: Boolean) {
        val hour: Int
        val minute: Int

        if (isStartTime) {
            hour = GlyphScheduleManager.getScheduleStartHour(requireContext())
            minute = GlyphScheduleManager.getScheduleStartMinute(requireContext())
        } else {
            hour = GlyphScheduleManager.getScheduleEndHour(requireContext())
            minute = GlyphScheduleManager.getScheduleEndMinute(requireContext())
        }

        val is24HourFormat: Boolean = DateFormat.is24HourFormat(requireContext())

        val dialog: TimePickerDialog = TimePickerDialog(
            requireContext(),
            { view, selectedHour, selectedMinute ->
                if (isStartTime) {
                    GlyphScheduleManager.setScheduleStartTime(
                        requireContext(), selectedHour, selectedMinute
                    )
                } else {
                    GlyphScheduleManager.setScheduleEndTime(
                        requireContext(), selectedHour, selectedMinute
                    )
                }
                updatePreferences()
            },
            hour,
            minute,
            is24HourFormat
        )

        dialog.setTitle(if (isStartTime) "Select Start Time" else "Select End Time")
        dialog.show()
    }

    private fun updatePreferences() {
        if (mDaysPreference != null) {
            val selectedDays: MutableSet<String?>? =
                GlyphScheduleManager.getScheduleDays(requireContext())
            mDaysPreference.setValues(selectedDays)
            mDaysPreference.setSummary(GlyphScheduleManager.getScheduleDaysFormatted(requireContext()))
        }

        if (mStartTimePreference != null) {
            val hour: Int = GlyphScheduleManager.getScheduleStartHour(requireContext())
            val minute: Int = GlyphScheduleManager.getScheduleStartMinute(requireContext())
            mStartTimePreference.setSummary(
                GlyphScheduleManager.formatTime(requireContext(), hour, minute)
            )
        }

        if (mEndTimePreference != null) {
            val hour: Int = GlyphScheduleManager.getScheduleEndHour(requireContext())
            val minute: Int = GlyphScheduleManager.getScheduleEndMinute(requireContext())
            mEndTimePreference.setSummary(
                GlyphScheduleManager.formatTime(requireContext(), hour, minute)
            )
        }

        if (mStatusPreference != null) {
            val scheduleEnabled: Boolean = GlyphScheduleManager.isScheduleEnabled(requireContext())
            val scheduleActiveToday: Boolean =
                GlyphScheduleManager.isScheduleActiveToday(requireContext())
            val scheduleActive: Boolean =
                GlyphScheduleManager.isScheduleCurrentlyActive(requireContext())

            val status: String?
            if (!scheduleEnabled) {
                status = "Schedule disabled"
            } else if (!scheduleActiveToday) {
                status = "⏸ Not active today"
            } else if (scheduleActive) {
                status = "⏸ Schedule active - Glyph disabled (Torch still works)"
            } else {
                status = "✓ Schedule inactive - Glyph enabled"
            }

            mStatusPreference.setSummary(status)
        }
    }

    public override fun onResume() {
        super.onResume()
        updatePreferences()
    }
}