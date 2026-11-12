
package com.eniola.smartstudyplanner.receivers

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.eniola.smartstudyplanner.MainActivity
import com.eniola.smartstudyplanner.R
import com.eniola.smartstudyplanner.utils.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

/**ReminderReceiver handles scheduled study task notifications.
   It sends task reminders, alerts students when tasks are due,
   and checks Firestore for incomplete tasks during the 10 PM check-in.*/
class ReminderReceiver : BroadcastReceiver() {

    //Receives scheduled alerts and displays notifications based on their type.
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        // Check whether notifications are enabled
        // in the app Settings.
        val preferences =
            context.getSharedPreferences(
                "study_planner_settings",
                Context.MODE_PRIVATE
            )

        val notificationsEnabled =
            preferences.getBoolean(
                "notifications_enabled",
                true
            )

        // Stop immediately if the student disabled notifications.
        if (!notificationsEnabled) {
            return
        }

        // Find out which type of notification triggered this receiver.
        val notificationType =
            intent.getStringExtra(
                "NOTIFICATION_TYPE"
            ) ?: "REMINDER"

        // The 10 PM check needs to query Firestore, so handle it separately.
        if (notificationType == "OVERDUE_CHECK") {

            checkForIncompleteTasks(context)

            return
        }

        // Get the task title for normal reminders.
        val taskTitle =
            intent.getStringExtra("TASK_TITLE")
                ?: "Study Task"

        // Open the app when the notification is tapped.
        val openAppIntent =
            Intent(
                context,
                MainActivity::class.java
            )

        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        // Notification title and message.
        val notificationTitle: String
        val notificationMessage: String

        if (notificationType == "TASK_TIME") {

            // Notification shown at the actual task time.
            notificationTitle =
                "Task Time"

            notificationMessage =
                "$taskTitle is due now."

        } else {

            // Normal pre-task reminder.
            notificationTitle =
                "Study Task Reminder"

            notificationMessage =
                taskTitle
        }

        // Build the notification.
        val notification =
            NotificationCompat.Builder(
                context,
                NotificationHelper.CHANNEL_ID
            )
                .setSmallIcon(
                    R.drawable.ic_notification
                )
                .setContentTitle(
                    notificationTitle
                )
                .setContentText(
                    notificationMessage
                )
                .setPriority(
                    NotificationCompat.PRIORITY_HIGH
                )
                .setAutoCancel(true)
                .setContentIntent(
                    pendingIntent
                )
                .build()

        // Android 13+ requires notification permission.
        if (
            android.os.Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            NotificationManagerCompat
                .from(context)
                .notify(
                    System.currentTimeMillis().toInt(),
                    notification
                )
        }
    }

    /**Checks Firestore for today's incomplete tasks and prepares a reminder message during the scheduled 10 PM check-in.*/
    private fun checkForIncompleteTasks(
        context: Context
    ) {

        val user =
            FirebaseAuth.getInstance().currentUser

        // If nobody is logged in, there is nobody whose tasks we can check.
        if (user == null) {
            return
        }

        val firestore =
            FirebaseFirestore.getInstance()

        //Get the beginning and end of today.
        val calendar =
            Calendar.getInstance()

        calendar.set(
            Calendar.HOUR_OF_DAY,
            0
        )

        calendar.set(
            Calendar.MINUTE,
            0
        )

        calendar.set(
            Calendar.SECOND,
            0
        )

        calendar.set(
            Calendar.MILLISECOND,
            0
        )

        val startOfDay =
            calendar.time

        calendar.add(
            Calendar.DAY_OF_MONTH,
            1
        )

        val startOfTomorrow =
            calendar.time

        /*Get today's tasks from Firestore.
          We use the deadline field because it contains the actual date and time of the task. */

        firestore
            .collection("users")
            .document(user.uid)
            .collection("tasks")
            .whereGreaterThanOrEqualTo(
                "deadline",
                com.google.firebase.Timestamp(
                    startOfDay
                )
            )
            .whereLessThan(
                "deadline",
                com.google.firebase.Timestamp(
                    startOfTomorrow
                )
            )
            .get()
            .addOnSuccessListener { documents ->

                // Count today's incomplete tasks.
                var incompleteTaskCount = 0

                for (document in documents) {

                    val completed =
                        document.getBoolean(
                            "completed"
                        ) ?: false

                    if (!completed) {
                        incompleteTaskCount++
                    }
                }

                // If every task was completed, don't send a notification.
                if (incompleteTaskCount == 0) {
                    return@addOnSuccessListener
                }

                // Create the notification message.
                val message: String
                if (incompleteTaskCount == 1) {

                    // One unfinished task.
                    val incompleteTask =
                        documents.documents.firstOrNull {
                            !(it.getBoolean(
                                "completed"
                            ) ?: false)
                        }

                    val taskTitle =
                        incompleteTask
                            ?.getString("title")
                            ?: "Your task"

                    message =
                        "Did you forget to tick your task? " +
                                "$taskTitle is still incomplete."

                } else {

                    // Multiple unfinished tasks.
                    message =
                        "You have $incompleteTaskCount " +
                                "incomplete tasks today. " +
                                "Did you forget to tick any as completed?"
                }

                // Show the 10 PM notification.
                showOverdueNotification(
                    context,
                    message
                )
            }
    }

    // Displays the incomplete-task check-in notification with the reminder message.
    private fun showOverdueNotification(
        context: Context,
        message: String
    ) {

        // Open the app when the notification is tapped.
        val openAppIntent =
            Intent(
                context,
                MainActivity::class.java
            )

        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        // Build the overdue notification.
        val notification =
            NotificationCompat.Builder(
                context,
                NotificationHelper.CHANNEL_ID
            )
                .setSmallIcon(
                    R.drawable.ic_notification
                )
                .setContentTitle(
                    "Study Check-in"
                )
                .setContentText(
                    message
                )
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(message)
                )
                .setPriority(
                    NotificationCompat.PRIORITY_HIGH
                )
                .setAutoCancel(true)
                .setContentIntent(
                    pendingIntent
                )
                .build()

        // Check notification permission.
        if (
            android.os.Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            NotificationManagerCompat
                .from(context)
                .notify(
                    999998,
                    notification
                )
        }
    }
}
