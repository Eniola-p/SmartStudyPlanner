
package com.eniola.smartstudyplanner

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Intent
import java.util.Calendar

/** SettingsActivity manages the student's application settings.
 * It handles notification preferences, password changes, app reset, account logout, and displays information about the application.*/
class SettingsActivity : AppCompatActivity() {

    // Stores the student's Settings choices.
    private val preferences by lazy {
        getSharedPreferences(
            "study_planner_settings",
            MODE_PRIVATE
        )
    }

    /** Initializes the Settings screen and handles the available settings options. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Display the Settings screen.
        setContentView(R.layout.activity_settings)

        // Connect the back button.
        val btnSettingsBack =
            findViewById<ImageButton>(
                R.id.btnSettingsBack
            )

        // Return to the Profile screen.
        btnSettingsBack.setOnClickListener {
            finish()
        }

        // Connect the Enable Notifications switch.
        val switchNotifications =
            findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(
                R.id.switchNotifications
            )

        // Load the saved notification setting.
        switchNotifications.isChecked =
            preferences.getBoolean(
                "notifications_enabled",
                true
            )

        // Save the notification setting.
        switchNotifications.setOnCheckedChangeListener {
                _, isChecked ->

            preferences
                .edit()
                .putBoolean(
                    "notifications_enabled",
                    isChecked
                )
                .apply()
        }

        // Connect Change Password.
        val txtChangePassword =
            findViewById<TextView>(
                R.id.txtChangePassword
            )

        // Open the Change Password dialog.
        txtChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        // Connect Reset App.
        val txtResetApp =
            findViewById<TextView>(
                R.id.txtResetApp
            )

// Show the Reset App confirmation dialog.
        txtResetApp.setOnClickListener {
            showResetAppDialog()
        }

        // Connect Log Out.
        val txtLogOut =
            findViewById<TextView>(
                R.id.txtLogout
            )

// Show the LogOut confirmation dialog.
        txtLogOut.setOnClickListener {
            showLogoutDialog()
        }

        // Connect About Smart Study Planner.
        val txtAbout =
            findViewById<TextView>(
                R.id.txtAboutApp
            )

// Open the About dialog.
        txtAbout.setOnClickListener {
            showAboutDialog()
        }
    }

    /** Displays a dialog for validating and changing the student's Firebase password. */
    private fun showChangePasswordDialog() {

        // Load our custom password layout.
        val view =
            LayoutInflater.from(this)
                .inflate(
                    R.layout.dialog_change_password,
                    null
                )

        // Connect the password fields.
        val edtNewPassword =
            view.findViewById<TextInputEditText>(
                R.id.edtNewPassword
            )

        val edtConfirmPassword =
            view.findViewById<TextInputEditText>(
                R.id.edtConfirmPassword
            )

        // Create the password dialog.
        val dialog =
            AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(view)
                .setNegativeButton(
                    "Cancel",
                    null
                )
                .setPositiveButton(
                    "Confirm Change",
                    null
                )
                .create()

        // Handle the Confirm Change button.
        dialog.setOnShowListener {

            // Make CANCEL button blue.
            dialog.getButton(
                AlertDialog.BUTTON_NEGATIVE
            ).setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.primary
                )
            )

            // Make CONFIRM CHANGE button blue.
            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.primary
                )
            )

            // Confirm Change button functionality.
            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val newPassword =
                    edtNewPassword.text
                        .toString()
                        .trim()

                val confirmPassword =
                    edtConfirmPassword.text
                        .toString()
                        .trim()

                // Check that the new password is entered.
                if (newPassword.isEmpty()) {
                    edtNewPassword.error =
                        "Enter a new password"
                    return@setOnClickListener
                }

                // Check that the confirmation is entered.
                if (confirmPassword.isEmpty()) {
                    edtConfirmPassword.error =
                        "Confirm your new password"
                    return@setOnClickListener
                }

                // Check that both passwords match.
                if (newPassword != confirmPassword) {
                    edtConfirmPassword.error =
                        "Passwords do not match"
                    return@setOnClickListener
                }

                // Firebase requires at least 6 characters.
                if (newPassword.length < 6) {
                    edtNewPassword.error =
                        "Password must be at least 6 characters"
                    return@setOnClickListener
                }

                // Get the currently signed-in student.
                val user =
                    FirebaseAuth
                        .getInstance()
                        .currentUser

                if (user == null) {
                    Toast.makeText(
                        this,
                        "No user is currently signed in.",
                        Toast.LENGTH_LONG
                    ).show()

                    dialog.dismiss()
                    return@setOnClickListener
                }

                // Update the Firebase password.
                user.updatePassword(newPassword)
                    .addOnSuccessListener {

                        Toast.makeText(
                            this,
                            "Password changed successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        dialog.dismiss()
                    }

                    .addOnFailureListener { exception ->

                        Toast.makeText(
                            this,
                            "Failed to change password: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        }

        // Display the dialog.
        dialog.show()
    }

    /** Displays a confirmation dialog before resetting the student's app data. */
    private fun showResetAppDialog() {

        AlertDialog.Builder(this)
            .setTitle("Reset App?")
            .setMessage(
                "This will delete all your saved tasks " +
                        "and reset your app settings. " +
                        "Your account will not be deleted. " +
                        "This action cannot be undone."
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Reset App"
            ) { _, _ ->

                resetApp()
            }
            .create()
            .apply {

                setOnShowListener {

                    // Cancel button — blue.
                    getButton(
                        AlertDialog.BUTTON_NEGATIVE
                    ).setTextColor(
                        ContextCompat.getColor(
                            this@SettingsActivity,
                            R.color.primary
                        )
                    )

                    // Reset button — red
                    getButton(
                        AlertDialog.BUTTON_POSITIVE
                    ).setTextColor(
                        ContextCompat.getColor(
                            this@SettingsActivity,
                            R.color.error
                        )
                    )
                }

                show()
            }
    }

    /**Resets the student's app data.This deletes all saved tasks and resets notification/reminder settings. The student's Firebase account remains intact.*/
    private fun resetApp() {

        val user =
            FirebaseAuth
                .getInstance()
                .currentUser

        // Make sure a student is logged in.
        if (user == null) {

            Toast.makeText(
                this,
                "No user is currently signed in.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val firestore =
            FirebaseFirestore.getInstance()

        // Get all saved tasks before deleting them.

        firestore
            .collection("users")
            .document(user.uid)
            .collection("tasks")
            .get()
            .addOnSuccessListener { documents ->

                // Cancel every task's scheduled alarm.
                for (document in documents) {

                    val deadline =
                        document.getTimestamp("deadline")

                    val reminderText =
                        document.getString("reminder")

                    if (deadline != null) {

                        val taskDateTime =
                            Calendar.getInstance().apply {
                                time = deadline.toDate()
                            }

                        /*Convert the saved reminder text back into minutes.*/
                        val reminderMinutes =
                            when (reminderText) {

                                "No reminder" -> 0
                                "5 minutes before" -> 5
                                "15 minutes before" -> 15
                                "30 minutes before" -> 30
                                "1 hour before" -> 60
                                "1 day before" -> 1440

                                else -> 0
                            }

                        // Get the alarm manager.
                        val alarmManager =
                            getSystemService(
                                ALARM_SERVICE
                            ) as android.app.AlarmManager

                        // Cancel the task-time notification.
                        val taskTimeIntent =
                            Intent(
                                this,
                                com.eniola.smartstudyplanner.receivers.ReminderReceiver::class.java
                            )

                        val taskTimeRequestCode =
                            (taskDateTime.timeInMillis + 1).hashCode()

                        val taskTimePendingIntent =
                            android.app.PendingIntent.getBroadcast(
                                this,
                                taskTimeRequestCode,
                                taskTimeIntent,
                                android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                                        android.app.PendingIntent.FLAG_IMMUTABLE
                            )

                        alarmManager.cancel(taskTimePendingIntent)
                        taskTimePendingIntent.cancel()

                        // Cancel the advance reminder.
                        if (reminderMinutes > 0) {

                            val reminderIntent =
                                Intent(
                                    this,
                                    com.eniola.smartstudyplanner.receivers.ReminderReceiver::class.java
                                )

                            val reminderRequestCode =
                                taskDateTime.timeInMillis.hashCode()

                            val reminderPendingIntent =
                                android.app.PendingIntent.getBroadcast(
                                    this,
                                    reminderRequestCode,
                                    reminderIntent,
                                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                                            android.app.PendingIntent.FLAG_IMMUTABLE
                                )

                            alarmManager.cancel(reminderPendingIntent)
                            reminderPendingIntent.cancel()
                        }
                    }
                }

                // Cancel the daily 9 PM overdue check.
                val alarmManager =
                    getSystemService(
                        ALARM_SERVICE
                    ) as android.app.AlarmManager

                val overdueIntent =
                    Intent(
                        this,
                        com.eniola.smartstudyplanner.receivers.ReminderReceiver::class.java
                    ).apply {

                        putExtra(
                            "NOTIFICATION_TYPE",
                            "OVERDUE_CHECK"
                        )
                    }

                val overduePendingIntent =
                    android.app.PendingIntent.getBroadcast(
                        this,
                        999999,
                        overdueIntent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                                android.app.PendingIntent.FLAG_IMMUTABLE
                    )

                alarmManager.cancel(
                    overduePendingIntent
                )

                overduePendingIntent.cancel()

                // Delete all tasks from Firestore.
                val batch =
                    firestore.batch()

                for (document in documents) {
                    batch.delete(document.reference)
                }

                batch.commit()
                    .addOnSuccessListener {

                        // Reset all saved Settings. Notifications and reminders will return to their default ON state.
                        preferences
                            .edit()
                            .clear()
                            .apply()

                        Toast.makeText(
                            this,
                            "App reset successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Return to Profile.
                        finish()
                    }
                    .addOnFailureListener { exception ->

                        Toast.makeText(
                            this,
                            "Failed to reset app: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Failed to reset app: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    /** Displays a confirmation dialog and signs the student out of the application. */

    private fun showLogoutDialog() {

        AlertDialog.Builder(this)
            .setTitle("Log Out?")
            .setMessage(
                "Are you sure you want to log out?"
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Log Out"
            ) { _, _ ->

                // Sign the student out of Firebase.
                FirebaseAuth
                    .getInstance()
                    .signOut()

                // Open the Login screen.
                val intent =
                    Intent(
                        this,
                        LoginActivity::class.java
                    )

                // Prevent the student from returning
                // to Settings with the Back button.
                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK

                startActivity(intent)

                finish()
            }
            .create()
            .apply {

                setOnShowListener {

                    // Cancel button — blue.
                    getButton(
                        AlertDialog.BUTTON_NEGATIVE
                    ).setTextColor(
                        ContextCompat.getColor(
                            this@SettingsActivity,
                            R.color.primary
                        )
                    )

                    // Log Out button — blue.
                    getButton(
                        AlertDialog.BUTTON_POSITIVE
                    ).setTextColor(
                        ContextCompat.getColor(
                            this@SettingsActivity,
                            R.color.primary
                        )
                    )
                }

                show()
            }
    }

    /** Displays information about the app.*/
    private fun showAboutDialog() {

        AlertDialog.Builder(this)
            .setTitle("Smart Study Planner")
            .setMessage(
                "Plan Smart. Study Better.\n\n" +
                        "A smart academic planning application " +
                        "designed to help students organize " +
                        "their courses, assignments, examinations, " +
                        "and study sessions.\n\n" +
                        "Version 1.0\n\n" +
                        "Developed as a Final Year Project."
            )
            .setPositiveButton(
                "Close",
                null
            )
            .create()
            .apply {

                setOnShowListener {

                    // Make Close button blue.
                    getButton(
                        AlertDialog.BUTTON_POSITIVE
                    ).setTextColor(
                        ContextCompat.getColor(
                            this@SettingsActivity,
                            R.color.primary
                        )
                    )
                }

                show()
            }
    }
}