
package com.eniola.smartstudyplanner

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**TaskRepository manages the storage of student tasks in Firebase Firestore.
 * It saves new academic tasks under the authenticated student's accountand assigns each task a unique Firestore document ID.*/
class TaskRepository {

    // Firebase instances used for database access and authentication.
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /** Saves a new task to Firestore and reports whether the operation succeeds. */
    fun saveTask(
        task: Task,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {

        val user = auth.currentUser

        // Make sure a user is logged in.
        if (user == null) {
            onError(
                Exception("No user is currently signed in.")
            )
            return
        }

        // Create a new document ID automatically.
        val taskDocument =
            firestore
                .collection("users")
                .document(user.uid)
                .collection("tasks")
                .document()

        // Give the task the generated Firestore ID.
        val taskWithId =
            task.copy(
                id = taskDocument.id
            )

        // Save the task and return the result through the appropriate callback.
        taskDocument
            .set(taskWithId)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }
}
