package com.eniola.smartstudyplanner.utils

object ReminderUtils {

    fun getReminderMinutes(reminderPosition: Int): Int {

        return when (reminderPosition) {

            0 -> 0       // No reminder
            1 -> 5       // 5 minutes before
            2 -> 15      // 15 minutes before
            3 -> 30      // 30 minutes before
            4 -> 60      // 1 hour before
            5 -> 1440    // 1 day before

            else -> 0
        }
    }
}