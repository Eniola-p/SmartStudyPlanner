
package com.eniola.smartstudyplanner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**TaskViewModel manages task data shared across the application's screens.
 * It retrieves tasks from Firestore and holds them in LiveData, allowing UI components to observe updates without repeated loading. */

//This is where tasks and information are stored locally to prevent lagging
class TaskViewModel : ViewModel() {

    // Firebase instances used to access the student's saved tasks.
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Stores the task list internally and exposes it as read-only LiveData.
    private val _tasks = MutableLiveData<List<Task>>(emptyList())
    val tasks: LiveData<List<Task>> = _tasks

    /** Retrieves the signed-in student's tasks and updates the shared task list. */
    fun loadTasks() {
        val user = auth.currentUser ?: return

        // Retrieve tasks belonging to the authenticated student.
        firestore
            .collection("users")
            .document(user.uid)
            .collection("tasks")
            .get()
            .addOnSuccessListener { documents ->

                // Convert Firestore documents into Task objects and notify observers.
                _tasks.value = documents.map { document ->
                    document.toObject(Task::class.java)
                }
            }
    }
}