package com.eniola.smartstudyplanner

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

/**Application class runs before any Activity is created. */
class SmartStudyPlannerApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Force the entire app to always use Light Mode.
        AppCompatDelegate.setDefaultNightMode(
            AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}