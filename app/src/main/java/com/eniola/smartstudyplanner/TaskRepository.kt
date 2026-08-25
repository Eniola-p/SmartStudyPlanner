package com.eniola.smartstudyplanner

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TaskRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

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