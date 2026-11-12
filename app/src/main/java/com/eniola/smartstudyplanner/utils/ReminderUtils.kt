
package com.eniola.smartstudyplanner.utils

/** ReminderUtils converts the student's selected reminder option into the number of minutes before a scheduled task.*/
object ReminderUtils {

    /** Returns the reminder interval in minutes based on the selected position. */
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
