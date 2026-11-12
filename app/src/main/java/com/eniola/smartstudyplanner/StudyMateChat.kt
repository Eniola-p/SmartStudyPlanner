
package com.eniola.smartstudyplanner

import com.google.firebase.Timestamp

/**Defines the data models used to manage StudyMate AI conversations.
  It stores chat information and individual messages, includingtheir identifiers, content, and timestamps.*/

// Stores information about one StudyMate conversation.
data class StudyMateChat(
    val id: String = "",
    val title: String = "New Chat",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

// Stores an individual message inside a StudyMate conversation.
data class StudyMateMessage(
    val id: String = "",
    val role: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null
)
