
package com.eniola.smartstudyplanner

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**Defines the student data models and repository used by StudyMate AI.
 * It retrieves study preferences and saved tasks from Firebase Firestore to provide information for personalized academic assistance. */

// Holds the information StudyMate needs from the student's profile.
data class StudyMateProfile(
    val availableStudyHours: String = "",
    val preferredStudyTime: String = ""
)

// Holds both the student's profile and task information.
data class StudyMateData(
    val profile: StudyMateProfile,
    val tasks: List<Task>
)

// Reads the student's information from Firebase.
object StudyMateRepository {

    // Get the currently signed-in student's profile and tasks.
    fun getStudyMateData(
        onSuccess: (StudyMateData) -> Unit,
        onFailure: (Exception) -> Unit
    ) {

        // Get the currently logged-in student.
        val user =
            FirebaseAuth.getInstance().currentUser

        // Stop if there is no signed-in user.
        if (user == null) {
            onFailure(
                Exception("No signed-in user.")
            )
            return
        }

        // Get the Firebase database.
        val firestore =
            FirebaseFirestore.getInstance()

        // Read the student's profile document.
        firestore
            .collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { profileDocument ->

                // Read available study hours.
                val availableStudyHours =
                    profileDocument.getString(
                        "availableStudyHours"
                    ) ?: ""

                // Read preferred study time.
                val preferredStudyTime =
                    profileDocument.getString(
                        "preferredStudyTime"
                    ) ?: ""

                // Create the profile information.
                val profile =
                    StudyMateProfile(
                        availableStudyHours =
                            availableStudyHours,
                        preferredStudyTime =
                            preferredStudyTime
                    )

                // Now get all tasks belonging
                // to the same student.
                firestore
                    .collection("users")
                    .document(user.uid)
                    .collection("tasks")
                    .get()
                    .addOnSuccessListener { documents ->

                        // Convert every Firestore task
                        // document into a Task object.
                        val tasks =
                            documents.mapNotNull { document ->
                                document.toObject(
                                    Task::class.java
                                )
                            }

                        // Combine the profile and tasks.
                        val studyMateData =
                            StudyMateData(
                                profile = profile,
                                tasks = tasks
                            )

                        // Return the complete data.
                        onSuccess(studyMateData)
                    }
                    .addOnFailureListener { exception ->

                        // Return the task-loading error.
                        onFailure(exception)
                    }
            }
            .addOnFailureListener { exception ->

                // Return the profile-loading error.
                onFailure(exception)
            }
    }
}
