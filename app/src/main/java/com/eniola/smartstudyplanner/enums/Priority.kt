package com.eniola.smartstudyplanner.enums

import com.eniola.smartstudyplanner.R

/**Priority represents the calculated priority level of a study task based on importance and urgency.*/
enum class Priority(
    // Display name shown throughout the app
    val displayName: String,
    // Color resource used for priority indicators
    val colorRes: Int,
    // Icon resource used for the priority indicator
    val iconRes: Int

) {
    NONE(
        "No Priority",
        R.color.priority_none,
        R.drawable.ic_priority
    ),

    HIGH(
        "High",
        R.color.priority_red,
        R.drawable.ic_priority
    ),

    MEDIUM(
        "Medium",
        R.color.priority_orange,
        R.drawable.ic_priority
    ),

    NORMAL(
        "Normal",
        R.color.priority_yellow,
        R.drawable.ic_priority
    ),

    LOW(
        "Low",
        R.color.priority_green,
        R.drawable.ic_priority
    )
}