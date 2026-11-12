
package com.eniola.smartstudyplanner

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**StudyMateChatRepository manages the storage and retrieval of AI conversations.
 * It uses Firebase Firestore to create, load, update, and delete chats and their messages for the currently authenticated student.*/
object StudyMateChatRepository {

    // Provides access to the Firestore database.
    private val firestore =
        FirebaseFirestore.getInstance()

    // Provides access to the currently authenticated student.
    private val auth =
        FirebaseAuth.getInstance()

    // Keeps only one Recent chats listener active.
    private var chatsListener: ListenerRegistration? = null

    // Returns the current user's StudyMate chats collection.
    private fun chatsCollection() =
        firestore
            .collection("users")
            .document(auth.currentUser!!.uid)
            .collection("studyMateChats")

    // Creates a new StudyMate conversation.
    fun createChat(
        title: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {

        val now =
            Timestamp.now()

        // Prepare the conversation title and creation timestamps.
        val chat =
            hashMapOf(
                "title" to title,
                "createdAt" to now,
                "updatedAt" to now
            )

        // Save the conversation and return its generated document ID.
        chatsCollection()
            .add(chat)
            .addOnSuccessListener { document ->
                onSuccess(
                    document.id
                )
            }
            .addOnFailureListener { exception ->
                onFailure(
                    exception
                )
            }
    }

    // Loads all StudyMate conversations in real time for the current user.
    fun getChats(
        onSuccess: (List<StudyMateChat>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // Removes any previous listener before creating a new one.
        chatsListener?.remove()

        // Listen for chat updates and sort conversations by recent activity.
        chatsListener =
            chatsCollection()
                .orderBy(
                    "updatedAt",
                    com.google.firebase.firestore.Query.Direction.DESCENDING
                )
                .addSnapshotListener { documents, exception ->

                    if (exception != null) {
                        onFailure(exception)
                        return@addSnapshotListener
                    }

                    if (documents == null) {
                        onSuccess(emptyList())
                        return@addSnapshotListener
                    }

                    // Convert Firestore documents into StudyMateChat objects.
                    val chats =
                        documents.map { document ->
                            StudyMateChat(
                                id = document.id,
                                title =
                                    document.getString("title")
                                        ?: "New Chat",
                                createdAt =
                                    document.getTimestamp("createdAt"),
                                updatedAt =
                                    document.getTimestamp("updatedAt")
                            )
                        }

                    onSuccess(chats)
                }
    }

    // Loads all messages from one StudyMate conversation.
    fun getMessages(
        chatId: String,
        onSuccess: (List<StudyMateMessage>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {

        // Retrieve messages in chronological order.
        chatsCollection()
            .document(chatId)
            .collection("messages")
            .orderBy(
                "timestamp",
                com.google.firebase.firestore.Query.Direction.ASCENDING
            )
            .get()
            .addOnSuccessListener { documents ->

                // Convert saved message records into StudyMateMessage objects.
                val messages =
                    documents.map { document ->
                        StudyMateMessage(
                            id = document.id,
                            role =
                                document.getString("role")
                                    ?: "",
                            text =
                                document.getString("text")
                                    ?: "",
                            timestamp =
                                document.getTimestamp("timestamp")
                        )
                    }

                onSuccess(
                    messages
                )
            }
            .addOnFailureListener { exception ->
                onFailure(
                    exception
                )
            }
    }

    // Saves one message inside a StudyMate conversation.
    fun saveMessage(
        chatId: String,
        role: String,
        text: String,
        onFailure: (Exception) -> Unit
    ) {

        val now =
            Timestamp.now()

        // Prepare the message content, sender role, and timestamp.
        val message =
            hashMapOf(
                "role" to role,
                "text" to text,
                "timestamp" to now
            )

        // Store the message in the selected conversation.
        chatsCollection()
            .document(chatId)
            .collection("messages")
            .add(message)
            .addOnFailureListener { exception ->
                onFailure(
                    exception
                )
            }

        // Update the conversation's most recent activity timestamp.
        chatsCollection()
            .document(chatId)
            .update(
                "updatedAt",
                now
            )
            .addOnFailureListener { exception ->
                onFailure(
                    exception
                )
            }
    }

    // Updates the title of a StudyMate conversation.
    fun updateChatTitle(
        chatId: String,
        title: String,
        onFailure: (Exception) -> Unit
    ) {
        chatsCollection()
            .document(chatId)
            .update("title", title)
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    // Deletes a StudyMate conversation and all its messages.
    fun deleteChat(
        chatId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val chatReference =
            chatsCollection()
                .document(chatId)

        // Retrieve the conversation's messages before deleting them.
        chatReference
            .collection("messages")
            .get()
            .addOnSuccessListener { messages ->

                val batch =
                    firestore.batch()

                // Delete every message in the conversation.
                messages.documents.forEach { message ->
                    batch.delete(message.reference)
                }

                // Delete the conversation itself.
                batch.delete(chatReference)

                // Commit the deletions together and report the result.
                batch.commit()
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener { exception ->
                        onFailure(exception)
                    }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}