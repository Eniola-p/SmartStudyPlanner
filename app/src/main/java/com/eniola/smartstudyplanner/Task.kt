package com.eniola.smartstudyplanner

import com.google.firebase.Timestamp

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

    val importance: String = "",
    val urgency: String = "",
    val priority: String = "No Priority",
    val reminder: String = "No reminder",

    // False until the user marks the task as completed.
    val completed: Boolean = false
)