package com.eniola.smartstudyplanner.model

import com.eniola.smartstudyplanner.enums.Priority
import java.time.LocalDate

/** Represents one cell in the monthly calendar.
    A cell may either contain a real date or be an empty placeholder.*/
data class CalendarDay(

    // Null means this is an empty placeholder cell.
    val day: Int?,

    // Full LocalDate represented by this cell.
    val date: LocalDate? = null,

    // Indicates today's date.
    val isToday: Boolean = false,

    // Indicates the currently selected date.
    val isSelected: Boolean = false,

    // Number of study tasks.
    val taskCount: Int = 0,

    // Number of deadlines.
    val deadlineCount: Int = 0,

    // Number of reminders.
    val reminderCount: Int = 0,

    // Highest priority task.
    val priorityLevel: Priority = Priority.NONE
)