package com.example.spelltracker.report

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.spelltracker.data.AppDatabase
import com.example.spelltracker.data.Prefs

/**
 * Runs periodically (interval set by the user) and posts a notification
 * summarizing spelling mistakes made since the last report.
 */
class ReportWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = Prefs(applicationContext)
        if (!prefs.consentGiven) return Result.success()

        val db = AppDatabase.getDatabase(applicationContext)
        val since = prefs.lastReportTimestamp
        val mistakes = db.mistakeDao().getSince(since)

        if (mistakes.isNotEmpty()) {
            val wordList = mistakes.take(10).joinToString("\n") { "\u2022 ${it.word}" }
            val more = if (mistakes.size > 10) "\n...and ${mistakes.size - 10} more" else ""

            showNotification(
                title = "You made ${mistakes.size} spelling mistake(s)",
                body = wordList + more
            )
        }

        prefs.lastReportTimestamp = System.currentTimeMillis()
        return Result.success()
    }

    private fun showNotification(title: String, body: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Spelling Reports",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val WORK_NAME = "spell_tracker_report_work"
        private const val CHANNEL_ID = "spell_tracker_reports"
        private const val NOTIFICATION_ID = 1001
    }
}
