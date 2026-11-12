
package com.eniola.smartstudyplanner.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.eniola.smartstudyplanner.receivers.ReminderReceiver
import java.util.Calendar

/** ReminderScheduler manages scheduled notifications for study tasks.
 * It schedules task-time alerts, pre-task reminders, and daily 10 PM for
 * incomplete-task checks, and handles cancellation of reminder alarms.*/
object ReminderScheduler {

    /**Schedules notifications at the actual task time and at the student's selected reminder interval before the task.*/
    fun scheduleReminder(
        context: Context,
        taskTitle: String,
        taskDateTime: Calendar,
        reminderMinutes: Int
    ) {

        // Get the Android system service responsible for scheduling alarms.
        val alarmManager =
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

        // Android 12+ requires exact alarm permission.
        if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            return
        }

        // Actual task-time notification.
        val taskTimeIntent =
            Intent(
                context,
                ReminderReceiver::class.java
            ).apply {
                putExtra("TASK_TITLE", taskTitle)
                putExtra("NOTIFICATION_TYPE", "TASK_TIME")
            }

        // Generate a request code based on the task's scheduled time.
        val taskTimeRequestCode =
            (taskDateTime.timeInMillis + 1).hashCode()

        // Create the PendingIntent that triggers the task-time notification.
        val taskTimePendingIntent =
            PendingIntent.getBroadcast(
                context,
                taskTimeRequestCode,
                taskTimeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        // Schedule the task-time alert only if the task is in the future.
        if (
            taskDateTime.timeInMillis >
            System.currentTimeMillis()
        ) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                taskDateTime.timeInMillis,
                taskTimePendingIntent
            )
        }

        // Check whether pre-task reminders are enabled.
        val preferences =
            context.getSharedPreferences(
                "study_planner_settings",
                Context.MODE_PRIVATE
            )

        val remindersEnabled =
            preferences.getBoolean(
                "reminders_enabled",
                true
            )

        if (!remindersEnabled) {
            return
        }

        // No reminder selected.
        if (reminderMinutes <= 0) {
            return
        }

        // Calculate reminder time.
        val reminderTime =
            taskDateTime.clone() as Calendar

        reminderTime.add(
            Calendar.MINUTE,
            -reminderMinutes
        )

        // Skip reminders whose scheduled time has already passed.
        if (
            reminderTime.timeInMillis <=
            System.currentTimeMillis()
        ) {
            return
        }

        // Pre-task reminder.
        val reminderIntent =
            Intent(
                context,
                ReminderReceiver::class.java
            ).apply {
                putExtra("TASK_TITLE", taskTitle)
                putExtra("NOTIFICATION_TYPE", "REMINDER")
            }

        // Generate the request code for the selected pre-task reminder.
        val reminderRequestCode =
            taskDateTime.timeInMillis.hashCode()

        // Create the PendingIntent for the pre-task reminder.
        val reminderPendingIntent =
            PendingIntent.getBroadcast(
                context,
                reminderRequestCode,
                reminderIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        // Schedule the reminder to trigger even when the device is idle.
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminderTime.timeInMillis,
            reminderPendingIntent
        )
    }

    /** Schedules a daily 10 PM check for unfinished study tasks. */
    fun scheduleOverdueCheck(context: Context) {

        val alarmManager =
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

        // Set the check time to 10 PM or the following day if 10 PM has passed.
        val checkTime =
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 22)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                if (
                    timeInMillis <=
                    System.currentTimeMillis()
                ) {
                    add(
                        Calendar.DAY_OF_MONTH,
                        1
                    )
                }
            }

        // Create the broadcast intent for checking incomplete tasks.
        val intent =
            Intent(
                context,
                ReminderReceiver::class.java
            ).apply {
                putExtra(
                    "NOTIFICATION_TYPE",
                    "OVERDUE_CHECK"
                )
            }

        // Create the PendingIntent for the daily overdue check.
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                999999,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        // Repeat the overdue task check at daily intervals.
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            checkTime.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    /** Cancels the task-time alert and selected pre-task reminder for a task. */
    fun cancelReminder(
        context: Context,
        taskDateTime: Calendar,
        reminderMinutes: Int
    ) {

        val alarmManager =
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

        // Cancel task-time notification.
        val taskTimeIntent =
            Intent(
                context,
                ReminderReceiver::class.java
            )

        val taskTimeRequestCode =
            (taskDateTime.timeInMillis + 1).hashCode()

        val taskTimePendingIntent =
            PendingIntent.getBroadcast(
                context,
                taskTimeRequestCode,
                taskTimeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        alarmManager.cancel(
            taskTimePendingIntent
        )

        taskTimePendingIntent.cancel()

        // Cancel selected pre-task reminder.
        if (reminderMinutes <= 0) {
            return
        }

        val reminderIntent =
            Intent(
                context,
                ReminderReceiver::class.java
            )

        val reminderRequestCode =
            taskDateTime.timeInMillis.hashCode()

        val reminderPendingIntent =
            PendingIntent.getBroadcast(
                context,
                reminderRequestCode,
                reminderIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        alarmManager.cancel(
            reminderPendingIntent
        )

        reminderPendingIntent.cancel()
    }

    /**
     * Cancels the daily 10 PM overdue-check alarm.
     * Individual task reminder alarms are not cancelled by this method.
     */
    // Cancels all scheduled notification alarms.
    fun cancelAllReminders(context: Context) {

        val alarmManager =
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

        // Cancel the daily 10 PM overdue check.
        val overdueIntent =
            Intent(
                context,
                ReminderReceiver::class.java
            ).apply {
                putExtra(
                    "NOTIFICATION_TYPE",
                    "OVERDUE_CHECK"
                )
            }

        val overduePendingIntent =
            PendingIntent.getBroadcast(
                context,
                999999,
                overdueIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        alarmManager.cancel(
            overduePendingIntent
        )

        overduePendingIntent.cancel()
    }
}
