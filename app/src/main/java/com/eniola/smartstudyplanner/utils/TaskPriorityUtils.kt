package com.eniola.smartstudyplanner.utils

import com.eniola.smartstudyplanner.enums.Priority

object TaskPriorityUtils {

    fun calculatePriority(
        importancePosition: Int,
        urgencyPosition: Int
    ): Priority {

        // If either option has not been selected,
        // the task has no priority yet.
        if (importancePosition == 0 || urgencyPosition == 0) {
            return Priority.NONE
        }

        return when {

            // Very Important + Do Soon
            importancePosition == 1 && urgencyPosition == 1 ->
                Priority.HIGH

            // Very Important + Can Wait
            importancePosition == 1 && urgencyPosition == 2 ->
                Priority.MEDIUM

            // Very Important + No Rush
            importancePosition == 1 && urgencyPosition == 3 ->
                Priority.NORMAL

            // Important + Do Soon
            importancePosition == 2 && urgencyPosition == 1 ->
                Priority.MEDIUM

            // Important + Can Wait
            importancePosition == 2 && urgencyPosition == 2 ->
                Priority.NORMAL

            // Important + No Rush
            importancePosition == 2 && urgencyPosition == 3 ->
                Priority.LOW

            // Less Important + Do Soon
            importancePosition == 3 && urgencyPosition == 1 ->
                Priority.NORMAL

            // Less Important + Can Wait
            importancePosition == 3 && urgencyPosition == 2 ->
                Priority.LOW

            // Less Important + No Rush
            importancePosition == 3 && urgencyPosition == 3 ->
                Priority.LOW

            else ->
                Priority.NONE
        }
    }

    fun getPriorityFromName(name: String): Priority {
        return when (name) {
            "High" -> Priority.HIGH
            "Medium" -> Priority.MEDIUM
            "Normal" -> Priority.NORMAL
            "Low" -> Priority.LOW
            "No Priority" -> Priority.NONE
            else -> Priority.NONE
        }
    }
}