package com.eniola.smartstudyplanner

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import android.content.Intent
import android.widget.TextView
class SignUpActivity : AppCompatActivity() {

    // Firebase Authentication instance used to create and manage user accounts.
    private lateinit var auth: FirebaseAuth
    private val userRepository = UserRepository()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        val txtLogin = findViewById<TextView>(R.id.txtLogin)

        txtLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Initialize the Firebase Authentication instance.
        auth = FirebaseAuth.getInstance()

        // Connect XML views to Kotlin.
        val btnSignup = findViewById<MaterialButton>(R.id.btnSignup)
        val edtFullName = findViewById<TextInputEditText>(R.id.edtFullName)
        val edtEmail = findViewById<TextInputEditText>(R.id.edtEmail)
        val edtPassword = findViewById<TextInputEditText>(R.id.edtPassword)
        val edtConfirmPassword = findViewById<TextInputEditText>(R.id.edtConfirmPassword)

        // Validate user input and create a Firebase account when Sign Up is clicked.
        btnSignup.setOnClickListener {

            // Read user input.
            val fullName = edtFullName.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString()
            val confirmPassword = edtConfirmPassword.text.toString()

            // FULL NAME VALIDATION
            if (fullName.isEmpty()) {
                edtFullName.error = "Please enter your full name"
                edtFullName.requestFocus()
                return@setOnClickListener
            }

            if (!fullName.matches(Regex("^[A-Za-z]{2,}(\\s+[A-Za-z]{2,})+$"))) {
                edtFullName.error = "Enter a valid first and last name"
                edtFullName.requestFocus()
                return@setOnClickListener
            }

            // EMAIL VALIDATION
            if (email.isEmpty()) {
                edtEmail.error = "Please enter your email address"
                edtEmail.requestFocus()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                edtEmail.error = "Enter a valid email address"
                edtEmail.requestFocus()
                return@setOnClickListener
            }

            // PASSWORD VALIDATION
            if (password.isEmpty()) {
                edtPassword.error = "Please enter your password"
                edtPassword.requestFocus()
                return@setOnClickListener
            }

            if (!password.matches(Regex("^(?=.*[A-Z])(?=.*\\d)(?=.*[@#\$%^&+=!]).{6,12}$"))) {
                edtPassword.error =
                    "Password must be 6-12 characters with a capital letter, number and special character"
                edtPassword.requestFocus()
                return@setOnClickListener
            }

            // CONFIRM PASSWORD VALIDATION
            if (confirmPassword.isEmpty()) {
                edtConfirmPassword.error = "Please confirm your password"
                edtConfirmPassword.requestFocus()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                edtConfirmPassword.error = "Passwords do not match"
                edtConfirmPassword.requestFocus()
                return@setOnClickListener
            }

            // Create a Firebase account and open the Login screen if registration succeeds.
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->

                    if (task.isSuccessful) {
                        val firebaseUser = auth.currentUser

                        val profileUpdates =
                            com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                .setDisplayName(fullName)
                                .build()

                        firebaseUser?.updateProfile(profileUpdates)

                        userRepository.saveUserProfile(
                            fullName = fullName,
                            email = email,

                            onSuccess = {

                                Toast.makeText(
                                    this,
                                    "Account created successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                startActivity(
                                    Intent(
                                        this,
                                        LoginActivity::class.java
                                    )
                                )

                                finish()
                            },

                            onError = { exception ->

                                Toast.makeText(
                                    this,
                                    "Account created, but profile could not be saved: ${exception.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    } else {

                        Toast.makeText(
                            this,
                            task.exception?.localizedMessage
                                ?: "Registration failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }
}