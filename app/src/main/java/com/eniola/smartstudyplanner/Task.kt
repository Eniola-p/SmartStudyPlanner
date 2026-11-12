
package com.eniola.smartstudyplanner

import com.google.firebase.Timestamp

/** Task defines the data model for a student's academic activity.
 * It stores task details, deadlines, priority information, reminder preferences, and completion status for use throughout the application.*/
data class Task(
    val id: String = "",
    val title: String = "",
    val course: String = "",
    val description: String = "",

    // Actual date and time of the task deadline.
    val deadline: Timestamp? = null,

    // Formatted values used for displaying the date and time.
    val date: String = "",
    val time: String = "",

    // Stores the task's importance, urgency, and calculated priority.
    val importance: String = "",
    val urgency: String = "",
    val priority: String = "No Priority",
    val reminder: String = "No reminder",

    // False until the user marks the task as completed.
    val completed: Boolean = false
)
