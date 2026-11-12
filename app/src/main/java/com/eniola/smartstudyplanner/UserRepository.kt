
package com.eniola.smartstudyplanner

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/** UserRepository manages the storage of student profile information.
 * It uses Firebase Authentication to identify the signed-in student and saves their name and email address in Firebase Firestore. */
class UserRepository {

    // Firebase instances for database access and user authentication.
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /** Saves the authenticated student's profile and reports the operation result. */
    fun saveUserProfile(
        fullName: String,
        email: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {

        val user = auth.currentUser

        // Make sure a user was created and is signed in.
        if (user == null) {
            onError(
                Exception("No user is currently signed in.")
            )
            return
        }

        // Prepare the student's name and email for storage.
        val userData = hashMapOf(
            "fullName" to fullName,
            "email" to email
        )

        // Save the profile under the authenticated student's unique ID.
        firestore
            .collection("users")
            .document(user.uid)
            .set(userData)
            .addOnSuccessListener {

                // Notify the caller when the profile is saved successfully.
                onSuccess()
            }
            .addOnFailureListener { exception ->

                // Return any error encountered while saving the profile.
                onError(exception)
            }
    }
}