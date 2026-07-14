package com.example.spelltracker

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.switchmaterial.SwitchMaterial
import com.example.spelltracker.data.Prefs
import com.example.spelltracker.report.ReportWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = Prefs(this)
        requestNotificationPermissionIfNeeded()

        val consentSwitch = findViewById<SwitchMaterial>(R.id.consentSwitch)
        val enableKeyboardButton = findViewById<Button>(R.id.enableKeyboardButton)
        val chooseKeyboardButton = findViewById<Button>(R.id.chooseKeyboardButton)
        val intervalEditText = findViewById<EditText>(R.id.intervalEditText)
        val saveIntervalButton = findViewById<Button>(R.id.saveIntervalButton)
        val viewReportButton = findViewById<Button>(R.id.viewReportButton)
        val practiceListButton = findViewById<Button>(R.id.practiceListButton)

        consentSwitch.isChecked = prefs.consentGiven
        intervalEditText.setText(prefs.reportIntervalHours.toString())

        consentSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showConsentDialog(consentSwitch)
            } else {
                prefs.consentGiven = false
            }
        }

        enableKeyboardButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        chooseKeyboardButton.setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }

        saveIntervalButton.setOnClickListener {
            val hours = intervalEditText.text.toString().toIntOrNull()
            if (hours != null && hours > 0) {
                prefs.reportIntervalHours = hours
                scheduleReportWork(hours)
            }
        }

        viewReportButton.setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java))
        }

        practiceListButton.setOnClickListener {
            startActivity(Intent(this, PracticeListActivity::class.java))
        }

        // Make sure a report job is scheduled based on whatever interval is saved
        if (prefs.consentGiven) {
            scheduleReportWork(prefs.reportIntervalHours)
        }
    }

    private fun showConsentDialog(consentSwitch: SwitchMaterial) {
        AlertDialog.Builder(this)
            .setTitle("Enable spelling tracking?")
            .setMessage(
                "When you turn this on and switch to the Spell Tracker keyboard, " +
                "the app will check each word you type for spelling mistakes using " +
                "the device's built-in spell checker. Only words flagged as misspelled " +
                "are saved (the word itself and the time), so they can appear in your " +
                "report and practice list. No other text you type is stored or sent anywhere."
            )
            .setPositiveButton("I agree") { _, _ ->
                prefs.consentGiven = true
                scheduleReportWork(prefs.reportIntervalHours)
            }
            .setNegativeButton("Cancel") { _, _ ->
                consentSwitch.isChecked = false
            }
            .setCancelable(false)
            .show()
    }

    private fun scheduleReportWork(hours: Int) {
        val minutes = (hours * 60).coerceAtLeast(15) // WorkManager minimum periodic interval is 15 minutes
        val request = PeriodicWorkRequestBuilder<ReportWorker>(minutes.toLong(), TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ReportWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }
    }
}
