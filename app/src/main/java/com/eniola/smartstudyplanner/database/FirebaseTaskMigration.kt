
package com.eniola.smartstudyplanner.database

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.eniola.smartstudyplanner.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**Copies existing Firestore tasks to the local SQLite database. */
object FirebaseTaskMigration {

    private const val TAG = "SQLiteMigration"

    fun migrateTasks(
        context: Context,
        scope: LifecycleCoroutineScope
    ) {
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            Log.e(TAG, "Migration cancelled: No signed-in user")
            return
        }

        val userId = user.uid
        val appContext = context.applicationContext

        val preferences = appContext.getSharedPreferences(
            "sqlite_migration",
            Context.MODE_PRIVATE
        )

        val migrationKey = "tasks_imported_$userId"

        if (preferences.getBoolean(migrationKey, false)) {
            Log.d(TAG, "Tasks already imported for this user")
            return
        }

        // Retrieve the student's existing tasks from Firebase.
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("tasks")
            .get(Source.SERVER)
            .addOnSuccessListener { documents ->

                // Convert Firebase tasks into SQLite records.
                val localTasks = try {
                    documents.map { document ->

                        val task = document.toObject(Task::class.java)

                        TaskEntity(
                            id = document.id,
                            userId = userId,
                            title = task.title,
                            course = task.course,
                            description = task.description,
                            deadlineMillis = task.deadline
                                ?.toDate()?.time,
                            date = task.date,
                            time = task.time,
                            importance = task.importance,
                            urgency = task.urgency,
                            priority = task.priority,
                            reminder = task.reminder,
                            completed = task.completed
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Task conversion failed", e)
                    return@addOnSuccessListener
                }

                scope.launch(Dispatchers.IO) {
                    try {
                        val database =
                            AppDatabase.getDatabase(appContext)

                        val results =
                            database.taskDao().importTasks(localTasks)

                        val insertedCount =
                            results.count { it != -1L }

                        val totalCount =
                            database.taskDao().countTasks(userId)

                        // Mark complete only after the import succeeds.
                        preferences.edit()
                            .putBoolean(migrationKey, true)
                            .apply()

                        Log.d(
                            TAG,
                            "Import completed. Firebase: " +
                                    "${documents.size()}, " +
                                    "Newly inserted: $insertedCount, " +
                                    "Local total: $totalCount"
                        )

                    } catch (e: Exception) {
                        Log.e(TAG, "SQLite import failed", e)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Firebase task retrieval failed", exception)
            }
    }
}
