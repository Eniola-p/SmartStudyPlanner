package com.eniola.smartstudyplanner

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

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

        val userData = hashMapOf(
            "fullName" to fullName,
            "email" to email
        )

        firestore
            .collection("users")
            .document(user.uid)
            .set(userData)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }
}