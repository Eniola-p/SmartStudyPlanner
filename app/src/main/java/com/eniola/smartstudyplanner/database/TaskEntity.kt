
package com.eniola.smartstudyplanner.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**Represents a study task stored locally in SQLite.*/
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String,

    val userId: String = "",
    val title: String = "",
    val course: String = "",
    val description: String = "",

    // Store deadline as milliseconds.
    val deadlineMillis: Long? = null,

    val date: String = "",
    val time: String = "",

    val importance: String = "",
    val urgency: String = "",
    val priority: String = "No Priority",
    val reminder: String = "No reminder",

    val completed: Boolean = false
)
