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
package co.aospa.glyph.Manager

import android.app.AlarmManager
import co.aospa.glyph.Constants.Constants
import java.util.Date

object GlyphScheduleManager {
    private const val TAG = "GlyphScheduleManager"
    private const val DEBUG = true

    private const val PREF_SCHEDULE_ENABLED = "glyph_schedule_enabled"
    private const val PREF_SCHEDULE_START_HOUR = "glyph_schedule_start_hour"
    private const val PREF_SCHEDULE_START_MINUTE = "glyph_schedule_start_minute"
    private const val PREF_SCHEDULE_END_HOUR = "glyph_schedule_end_hour"
    private const val PREF_SCHEDULE_END_MINUTE = "glyph_schedule_end_minute"
    private const val PREF_SCHEDULE_ACTIVE = "glyph_schedule_currently_active"
    private const val PREF_SCHEDULE_DAYS = "glyph_schedule_days"

    private const val ACTION_SCHEDULE_START = "co.aospa.glyph.ACTION_SCHEDULE_START"
    private const val ACTION_SCHEDULE_END = "co.aospa.glyph.ACTION_SCHEDULE_END"

    private const val REQUEST_CODE_START = 1001
    private const val REQUEST_CODE_END = 1002

    val SUNDAY: Int = Calendar.SUNDAY
    val MONDAY: Int = Calendar.MONDAY
    val TUESDAY: Int = Calendar.TUESDAY
    val WEDNESDAY: Int = Calendar.WEDNESDAY
    val THURSDAY: Int = Calendar.THURSDAY
    val FRIDAY: Int = Calendar.FRIDAY
    val SATURDAY: Int = Calendar.SATURDAY

    fun isScheduleEnabled(context: Context?): Boolean {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(PREF_SCHEDULE_ENABLED, false)
    }

    fun setScheduleEnabled(context: Context, enabled: Boolean) {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putBoolean(PREF_SCHEDULE_ENABLED, enabled).apply()

        if (enabled) {
            setupScheduleAlarms(context)
            if (isWithinSchedulePeriod(context)) {
                applyScheduleStart(context)
            }
        } else {
            cancelScheduleAlarms(context)
            if (isScheduleCurrentlyActive(context)) {
                setScheduleActive(context, false)
                ServiceUtils.checkGlyphService()
                updateTorchTile(context)
            }
        }
    }

    fun getScheduleStartHour(context: Context?): Int {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getInt(PREF_SCHEDULE_START_HOUR, 22)
    }

    fun getScheduleStartMinute(context: Context?): Int {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getInt(PREF_SCHEDULE_START_MINUTE, 0)
    }

    fun getScheduleEndHour(context: Context?): Int {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getInt(PREF_SCHEDULE_END_HOUR, 7)
    }

    fun getScheduleEndMinute(context: Context?): Int {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getInt(PREF_SCHEDULE_END_MINUTE, 0)
    }

    fun setScheduleStartTime(context: Context, hour: Int, minute: Int) {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit()
            .putInt(PREF_SCHEDULE_START_HOUR, hour)
            .putInt(PREF_SCHEDULE_START_MINUTE, minute)
            .apply()

        if (isScheduleEnabled(context)) {
            setupScheduleAlarms(context)
        }
    }

    fun setScheduleEndTime(context: Context, hour: Int, minute: Int) {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit()
            .putInt(PREF_SCHEDULE_END_HOUR, hour)
            .putInt(PREF_SCHEDULE_END_MINUTE, minute)
            .apply()

        if (isScheduleEnabled(context)) {
            setupScheduleAlarms(context)
        }
    }

    fun getScheduleDays(context: Context?): MutableSet<String?> {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val defaultDays: MutableSet<String?> = HashSet<String?>()
        defaultDays.add(MONDAY.toString())
        defaultDays.add(TUESDAY.toString())
        defaultDays.add(WEDNESDAY.toString())
        defaultDays.add(THURSDAY.toString())
        defaultDays.add(FRIDAY.toString())
        defaultDays.add(SATURDAY.toString())
        defaultDays.add(SUNDAY.toString())
        return prefs.getStringSet(PREF_SCHEDULE_DAYS, defaultDays)
    }

    fun setScheduleDays(context: Context, days: MutableSet<String?>?) {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putStringSet(PREF_SCHEDULE_DAYS, days).apply()

        if (isScheduleEnabled(context)) {
            setupScheduleAlarms(context)
        }
    }

    fun isScheduleActiveToday(context: Context?): Boolean {
        if (!isScheduleEnabled(context)) {
            return false
        }

        val now: Calendar = Calendar.getInstance()
        val currentDay: Int = now.get(Calendar.DAY_OF_WEEK)

        val enabledDays = getScheduleDays(context)
        return enabledDays.contains(currentDay.toString())
    }

    fun isScheduleCurrentlyActive(context: Context?): Boolean {
        if (!isScheduleEnabled(context)) {
            return false
        }

        if (!isScheduleActiveToday(context)) {
            return false
        }

        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(PREF_SCHEDULE_ACTIVE, false)
    }

    fun isWithinSchedulePeriod(context: Context?): Boolean {
        if (!isScheduleActiveToday(context)) {
            return false
        }

        val now: Calendar = Calendar.getInstance()
        val currentHour: Int = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute: Int = now.get(Calendar.MINUTE)

        val startHour = getScheduleStartHour(context)
        val startMinute = getScheduleStartMinute(context)
        val endHour = getScheduleEndHour(context)
        val endMinute = getScheduleEndMinute(context)

        val currentTotalMinutes = currentHour * 60 + currentMinute
        val startTotalMinutes = startHour * 60 + startMinute
        val endTotalMinutes = endHour * 60 + endMinute

        if (startTotalMinutes < endTotalMinutes) {
            return currentTotalMinutes >= startTotalMinutes && currentTotalMinutes < endTotalMinutes
        } else {
            return currentTotalMinutes >= startTotalMinutes || currentTotalMinutes < endTotalMinutes
        }
    }

    private fun setScheduleActive(context: Context?, active: Boolean) {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putBoolean(PREF_SCHEDULE_ACTIVE, active).apply()
    }

    /**
     * Setup schedule alarms
     */
    fun setupScheduleAlarms(context: Context) {
        val alarmManager: AlarmManager? =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null")
            return
        }

        cancelScheduleAlarms(context)

        val startIntent: Intent = Intent(context, ScheduleReceiver::class.java)
        startIntent.setAction(ACTION_SCHEDULE_START)

        val endIntent: Intent = Intent(context, ScheduleReceiver::class.java)
        endIntent.setAction(ACTION_SCHEDULE_END)

        val startPendingIntent: PendingIntent? = PendingIntent.getBroadcast(
            context, REQUEST_CODE_START, startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val endPendingIntent: PendingIntent? = PendingIntent.getBroadcast(
            context, REQUEST_CODE_END, endIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val startTriggerTime = calculateNextTriggerTime(
            getScheduleStartHour(context),
            getScheduleStartMinute(context)
        )

        val endTriggerTime = calculateNextTriggerTime(
            getScheduleEndHour(context),
            getScheduleEndMinute(context)
        )

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            startTriggerTime,
            AlarmManager.INTERVAL_DAY,
            startPendingIntent
        )

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            endTriggerTime,
            AlarmManager.INTERVAL_DAY,
            endPendingIntent
        )

        if (isWithinSchedulePeriod(context)) {
            applyScheduleStart(context)
        } else {
            applyScheduleEnd(context)
        }

        if (DEBUG) {
            Log.d(TAG, "Start alarm set for: " + Date(startTriggerTime))
            Log.d(TAG, "End alarm set for: " + Date(endTriggerTime))
        }
    }

    fun cancelScheduleAlarms(context: Context) {
        val alarmManager: AlarmManager? =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
        if (alarmManager == null) return

        val startIntent: Intent = Intent(context, ScheduleReceiver::class.java)
        startIntent.setAction(ACTION_SCHEDULE_START)

        val endIntent: Intent = Intent(context, ScheduleReceiver::class.java)
        endIntent.setAction(ACTION_SCHEDULE_END)

        val startPendingIntent: PendingIntent? = PendingIntent.getBroadcast(
            context, REQUEST_CODE_START, startIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        val endPendingIntent: PendingIntent? = PendingIntent.getBroadcast(
            context, REQUEST_CODE_END, endIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (startPendingIntent != null) {
            alarmManager.cancel(startPendingIntent)
            startPendingIntent.cancel()
        }

        if (endPendingIntent != null) {
            alarmManager.cancel(endPendingIntent)
            endPendingIntent.cancel()
        }
    }

    private fun calculateNextTriggerTime(hour: Int, minute: Int): Long {
        val calendar: Calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return calendar.getTimeInMillis()
    }

    fun applyScheduleStart(context: Context) {
        setScheduleActive(context, true)
        ServiceUtils.checkGlyphService()
        updateTorchTile(context)
    }

    fun applyScheduleEnd(context: Context) {
        setScheduleActive(context, false)
        ServiceUtils.checkGlyphService()
        updateTorchTile(context)
    }

    private fun updateTorchTile(context: Context) {
        try {
            val intent: Intent = Intent("co.aospa.glyph.UPDATE_TORCH_TILE")
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update torch tile", e)
        }
    }

    /**
     * Format time based on system 12/24 hour preference
     */
    fun formatTime(context: Context?, hour: Int, minute: Int): String? {
        val calendar: Calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)


        // Use system preference for 12/24 hour format
        val timeFormat: DateFormat = DateFormat.getTimeFormat(context)
        return timeFormat.format(calendar.getTime())
    }

    /**
     * Format time with explicit 24-hour format (for backwards compatibility)
     */
    fun formatTime24Hour(hour: Int, minute: Int): String {
        return String.format("%02d:%02d", hour, minute)
    }

    fun getScheduleDaysFormatted(context: Context?): String {
        val days = getScheduleDays(context)

        if (days.size == 7) {
            return "Every day"
        }

        if (days.size == 0) {
            return "No days selected"
        }

        val weekdays: MutableSet<String?> = HashSet<String?>()
        weekdays.add(MONDAY.toString())
        weekdays.add(TUESDAY.toString())
        weekdays.add(WEDNESDAY.toString())
        weekdays.add(THURSDAY.toString())
        weekdays.add(FRIDAY.toString())

        if (days == weekdays) {
            return "Weekdays (Mon-Fri)"
        }

        val weekends: MutableSet<String?> = HashSet<String?>()
        weekends.add(SATURDAY.toString())
        weekends.add(SUNDAY.toString())

        if (days == weekends) {
            return "Weekends (Sat-Sun)"
        }

        val result = StringBuilder()
        val dayNames = arrayOf<String?>("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val dayOrder = intArrayOf(SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY)

        for (i in dayOrder.indices) {
            if (days.contains(dayOrder[i].toString())) {
                if (result.length > 0) {
                    result.append(", ")
                }
                result.append(dayNames[i])
            }
        }

        return result.toString()
    }

    fun getScheduleTimeRange(context: Context?): String {
        val startHour = getScheduleStartHour(context)
        val startMinute = getScheduleStartMinute(context)
        val endHour = getScheduleEndHour(context)
        val endMinute = getScheduleEndMinute(context)
        return formatTime(context, startHour, startMinute) + " - " + formatTime(
            context,
            endHour,
            endMinute
        )
    }

    fun getScheduleSummary(context: Context?): String {
        if (!isScheduleEnabled(context)) {
            return "Schedule disabled"
        }

        val days = getScheduleDaysFormatted(context)
        val time = getScheduleTimeRange(context)

        return days + " • " + time
    }

    class ScheduleReceiver : BroadcastReceiver() {
        public override fun onReceive(context: Context, intent: Intent?) {
            if (intent == null || intent.getAction() == null) return

            Constants.CONTEXT = context.getApplicationContext()
            if (!isScheduleEnabled(context)) {
                return
            }
            if (!isScheduleActiveToday(context)) {
                return
            }
            val action: String? = intent.getAction()
            if (ACTION_SCHEDULE_START == action) {
                applyScheduleStart(context)
            } else if (ACTION_SCHEDULE_END == action) {
                applyScheduleEnd(context)
            }
        }
    }
}