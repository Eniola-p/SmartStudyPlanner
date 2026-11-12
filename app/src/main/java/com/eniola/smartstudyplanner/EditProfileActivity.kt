
package com.eniola.smartstudyplanner

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

/** EditProfileActivity allows students to view and update their profile.
 * It manages personal information, academic details, preferred study time, and available study hours using Firebase Authentication and Firestore.*/
class EditProfileActivity : AppCompatActivity() {

    // Input fields for the student's personal and academic information.
    private lateinit var edtProfileName: TextInputEditText
    private lateinit var edtProfileEmail: TextInputEditText
    private lateinit var edtProfileDepartment: TextInputEditText
    private lateinit var edtProfileLevel: TextInputEditText
    private lateinit var spinnerPreferredStudyTime: Spinner
    private lateinit var edtAvailableStudyHours: TextInputEditText
    private lateinit var btnSaveProfile: MaterialButton

    // Provides access to the Firestore database.
    private val firestore =
        FirebaseFirestore.getInstance()

    // Manages the currently authenticated student.
    private val auth =
        FirebaseAuth.getInstance()

    /** Initializes the profile editing screen and loads existing student information. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_edit_profile)

        // Connect XML views to Kotlin.
        edtProfileName =
            findViewById(R.id.edtProfileName)

        edtProfileEmail =
            findViewById(R.id.edtProfileEmail)

        edtProfileDepartment =
            findViewById(R.id.edtProfileDepartment)

        edtProfileLevel =
            findViewById(R.id.edtProfileLevel)

        spinnerPreferredStudyTime =
            findViewById(R.id.spinnerPreferredStudyTime)

        edtAvailableStudyHours =
            findViewById(R.id.edtAvailableStudyHours)

        btnSaveProfile =
            findViewById(R.id.btnSaveProfile)

        setupStudyTimeSpinner()

        loadProfile()

        // Save changes when the student taps the button.
        btnSaveProfile.setOnClickListener {
            saveProfile()
        }
    }

    /** Configures the dropdown menu for selecting a preferred study time. */
    private fun setupStudyTimeSpinner() {

        // Create the preferred study time dropdown.
        val adapter =
            ArrayAdapter.createFromResource(
                this,
                R.array.study_time_options,
                R.layout.item_spinner
            )

        adapter.setDropDownViewResource(
            R.layout.item_spinner
        )

        spinnerPreferredStudyTime.adapter =
            adapter
    }

    /** Retrieves the student's saved profile information from Firestore. */
    private fun loadProfile() {

        val user = auth.currentUser

        // Make sure a student is signed in.
        if (user == null) {

            Toast.makeText(
                this,
                "Please sign in again",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        // Retrieve the authenticated student's profile document.
        firestore
            .collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {
                    return@addOnSuccessListener
                }

                // Load the existing profile information.
                edtProfileName.setText(
                    document.getString("fullName") ?: ""
                )

                edtProfileEmail.setText(
                    document.getString("email")
                        ?: user.email
                        ?: ""
                )

                edtProfileDepartment.setText(
                    document.getString("department") ?: ""
                )

                edtProfileLevel.setText(
                    document.getString("level") ?: ""
                )

                edtAvailableStudyHours.setText(
                    document.getString("availableStudyHours")
                        ?: ""
                )

                // Select the saved preferred study time.
                val savedStudyTime =
                    document.getString("preferredStudyTime")

                if (!savedStudyTime.isNullOrBlank()) {

                    setSpinnerValue(
                        spinnerPreferredStudyTime,
                        savedStudyTime
                    )
                }
            }
            .addOnFailureListener {

                // Display an error if the profile cannot be retrieved.
                Toast.makeText(
                    this,
                    "Failed to load profile",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    /** Validates and saves updated profile details to Firestore and Firebase Authentication. */
    private fun saveProfile() {

        val user = auth.currentUser

        // Make sure the student is still signed in.
        if (user == null) {

            Toast.makeText(
                this,
                "Please sign in again",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // Retrieve the updated information entered by the student.
        val fullName =
            edtProfileName.text
                .toString()
                .trim()

        val department =
            edtProfileDepartment.text
                .toString()
                .trim()

        val level =
            edtProfileLevel.text
                .toString()
                .trim()

        val availableHours =
            edtAvailableStudyHours.text
                .toString()
                .trim()

        val preferredStudyTime =
            spinnerPreferredStudyTime
                .selectedItem
                .toString()

        // Validate the student's name.
        if (fullName.isBlank()) {

            edtProfileName.error =
                "Name is required"

            edtProfileName.requestFocus()

            return
        }

        // Validate the department.
        if (department.isBlank()) {

            edtProfileDepartment.error =
                "Department is required"

            edtProfileDepartment.requestFocus()

            return
        }

        // Validate the level.
        if (level.isBlank()) {

            edtProfileLevel.error =
                "Level is required"

            edtProfileLevel.requestFocus()

            return
        }

        // Validate available study hours.
        if (availableHours.isBlank()) {

            edtAvailableStudyHours.error =
                "Available study hours is required"

            edtAvailableStudyHours.requestFocus()

            return
        }

        // Disable the save button while the profile is being updated.
        btnSaveProfile.isEnabled = false
        btnSaveProfile.text = "Saving..."

        // Update only the student's profile fields.
        val updates =
            hashMapOf<String, Any>(
                "fullName" to fullName,
                "department" to department,
                "level" to level,
                "preferredStudyTime" to preferredStudyTime,
                "availableStudyHours" to availableHours
            )

        // Save the updated profile fields to Firestore.
        firestore
            .collection("users")
            .document(user.uid)
            .update(updates)
            .addOnSuccessListener {

                // Keep Firebase Authentication's display name
                // synchronized with the Firestore profile.
                val profileUpdates =
                    UserProfileChangeRequest
                        .Builder()
                        .setDisplayName(fullName)
                        .build()

                user.updateProfile(profileUpdates)

                Toast.makeText(
                    this,
                    "Profile updated successfully",
                    Toast.LENGTH_SHORT
                ).show()

                // Return to the Profile page.
                finish()
            }
            .addOnFailureListener { exception ->

                // Restore the save button if the update fails.
                btnSaveProfile.isEnabled = true
                btnSaveProfile.text = "Save Changes"

                Toast.makeText(
                    this,
                    "Failed to update profile: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    /** Selects the previously saved study time option in the dropdown menu. */
    // Select the saved value in the study-time Spinner.
    private fun setSpinnerValue(
        spinner: Spinner,
        value: String
    ) {

        for (i in 0 until spinner.adapter.count) {

            if (
                spinner.adapter
                    .getItem(i)
                    ?.toString()
                    ?.equals(
                        value,
                        ignoreCase = true
                    ) == true
            ) {

                spinner.setSelection(i)

                return
            }
        }
    }
}
