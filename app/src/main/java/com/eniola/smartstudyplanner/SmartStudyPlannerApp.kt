
package com.eniola.smartstudyplanner

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.eniola.smartstudyplanner.utils.NotificationHelper
import com.eniola.smartstudyplanner.utils.ReminderScheduler
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/** SmartStudyPlannerApp initializes essential application services at startup.
 * It configures Firebase App Check, enforces Light Mode, creates the notification channel, and schedules daily incomplete-task reminders.
 */
class SmartStudyPlannerApp : Application() {

    /** Initializes application-wide services before any Activity is created. */
    override fun onCreate() {
        super.onCreate()

        // Enable Firebase App Check for local development.
        FirebaseApp.initializeApp(this)

        // Get the Firebase App Check instance.
        val firebaseAppCheck = FirebaseAppCheck.getInstance()

        // Configure the debug provider for Firebase App Check.
        firebaseAppCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )

        // Force the entire app to always use Light Mode.
        AppCompatDelegate.setDefaultNightMode(
            AppCompatDelegate.MODE_NIGHT_NO
        )

        // Create the notification channel when the application starts.
        NotificationHelper.createNotificationChannel(
            this
        )

        // Schedule the daily 10 PM unfinished-task check.
        ReminderScheduler.scheduleOverdueCheck(this)
    }
}
