package com.example.spelltracker.data

import android.content.Context

/**
 * Simple SharedPreferences wrapper for consent, report interval, and last report time.
 */
class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("spell_tracker_prefs", Context.MODE_PRIVATE)

    var consentGiven: Boolean
        get() = sp.getBoolean(KEY_CONSENT, false)
        set(value) = sp.edit().putBoolean(KEY_CONSENT, value).apply()

    var reportIntervalHours: Int
        get() = sp.getInt(KEY_INTERVAL_HOURS, 8) // default: report every 8 hours
        set(value) = sp.edit().putInt(KEY_INTERVAL_HOURS, value).apply()

    var lastReportTimestamp: Long
        get() = sp.getLong(KEY_LAST_REPORT, System.currentTimeMillis())
        set(value) = sp.edit().putLong(KEY_LAST_REPORT, value).apply()

    companion object {
        private const val KEY_CONSENT = "consent_given"
        private const val KEY_INTERVAL_HOURS = "report_interval_hours"
        private const val KEY_LAST_REPORT = "last_report_timestamp"
    }
}
