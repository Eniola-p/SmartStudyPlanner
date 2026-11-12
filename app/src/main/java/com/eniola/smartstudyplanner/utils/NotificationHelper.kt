
package com.eniola.smartstudyplanner.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/** NotificationHelper creates and manages the notification channel (STUDY TASK REMINDER)
 * used to deliver study task reminders and alerts.*/
object NotificationHelper {

    // Unique identifier for the study task notification channel.
    const val CHANNEL_ID = "study_task_reminders"

    // Creates a high-priority notification channel for study task reminders.
    fun createNotificationChannel(context: Context) {

        // Configure the notification channel's name and importance.
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Study Task Reminders",
            NotificationManager.IMPORTANCE_HIGH
        )

        // Set the description displayed in notification settings.
        channel.description =
            "Notifications for upcoming study tasks"

        // Get the Android system's notification manager.
        val notificationManager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        // Register the notification channel with the Android system.
        notificationManager.createNotificationChannel(channel)
    }
}
