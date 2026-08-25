package com.eniola.smartstudyplanner.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationHelper {

    const val CHANNEL_ID = "study_task_reminders"

    fun createNotificationChannel(context: Context) {

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Study Task Reminders",
            NotificationManager.IMPORTANCE_HIGH
        )

        channel.description =
            "Notifications for upcoming study tasks"

        val notificationManager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        notificationManager.createNotificationChannel(channel)
    }
}