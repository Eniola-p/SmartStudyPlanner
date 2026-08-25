package com.eniola.smartstudyplanner.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.eniola.smartstudyplanner.receivers.ReminderReceiver
import java.util.Calendar

object ReminderScheduler {

    fun scheduleReminder(
        context: Context,
        taskTitle: String,
        taskDateTime: Calendar,
        reminderMinutes: Int
    ) {

        if (reminderMinutes <= 0) {
            return
        }

        val reminderTime =
            taskDateTime.clone() as Calendar

        reminderTime.add(
            Calendar.MINUTE,
            -reminderMinutes
        )

        if (
            reminderTime.timeInMillis <=
            System.currentTimeMillis()
        ) {
            return
        }

        val intent =
            Intent(
                context,
                ReminderReceiver::class.java
            ).apply {

                putExtra(
                    "TASK_TITLE",
                    taskTitle
                )
            }

        val requestCode =
            taskDateTime.timeInMillis.hashCode()

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val alarmManager =
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

        if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            return
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminderTime.timeInMillis,
            pendingIntent
        )
    }

    fun cancelReminder(
        context: Context,
        taskDateTime: Calendar,
        reminderMinutes: Int
    ) {

        if (reminderMinutes <= 0) {
            return
        }

        val reminderTime =
            taskDateTime.clone() as Calendar

        reminderTime.add(
            Calendar.MINUTE,
            -reminderMinutes
        )

        val intent =
            Intent(
                context,
                ReminderReceiver::class.java
            )

        val requestCode =
            taskDateTime.timeInMillis.hashCode()

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val alarmManager =
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

        alarmManager.cancel(
            pendingIntent
        )

        pendingIntent.cancel()
    }
}
