
package com.eniola.smartstudyplanner

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

/** LoginActivity handles student authentication using Firebase.
 * It validates login details, signs students into the application,provides password recovery, and navigates to registration.*/
class LoginActivity : AppCompatActivity() {

    // Firebase Authentication instance used for user login.
    private lateinit var auth: FirebaseAuth

    /** Initializes the login screen and handles login, registration, and password recovery. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Authentication.
        auth = FirebaseAuth.getInstance()

        // Connect XML views to Kotlin.
        val edtEmail = findViewById<TextInputEditText>(R.id.edtEmail)
        val edtPassword = findViewById<TextInputEditText>(R.id.edtPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val txtForgotPassword = findViewById<TextView>(R.id.txtForgotPassword)
        val txtSignUp = findViewById<TextView>(R.id.txtSignUp)

        // Open Sign Up screen.
        txtSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        // Login button.
        btnLogin.setOnClickListener {

            // Hide the keyboard immediately.
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE)
                    as InputMethodManager
            imm.hideSoftInputFromWindow(
                currentFocus?.windowToken,
                0
            )

            // Retrieve the student's email and password.
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            // Email validation.
            if (email.isEmpty()) {
                edtEmail.error = "Please enter your email address"
                edtEmail.requestFocus()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                edtEmail.error = "Enter a valid email address"
                edtEmail.requestFocus()
                return@setOnClickListener
            }

            // Password validation.
            if (password.isEmpty()) {
                edtPassword.error = "Please enter your password"
                edtPassword.requestFocus()
                return@setOnClickListener
            }

            // Show loading state on the button.
            btnLogin.text = "Logging in..."
            btnLogin.isEnabled = false

            // Authenticate the student using Firebase email and password.
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->

                    if (task.isSuccessful) {

                        // Open the main application after successful login.
                        Toast.makeText(
                            this,
                            "Login successful!",
                            Toast.LENGTH_SHORT
                        ).show()

                        startActivity(
                            Intent(this, MainActivity::class.java)
                        )
                        finish()

                    } else {

                        // Restore the button after a failed login.
                        btnLogin.text = "Login"
                        btnLogin.isEnabled = true

                        // Display an appropriate message based on the authentication error.
                        val message = when (task.exception) {

                            is FirebaseAuthInvalidUserException ->
                                "No account was found with this email address."

                            is FirebaseAuthInvalidCredentialsException ->
                                "Incorrect email or password."

                            else ->
                                "Unable to sign in. Please check your internet connection and try again."
                        }

                        Toast.makeText(
                            this,
                            message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        // Forgot Password.
        txtForgotPassword.setOnClickListener {
            val email = edtEmail.text.toString().trim()

            // Validate the email address before requesting a password reset.
            if (email.isEmpty()) {
                edtEmail.error = "Please enter your email address"
                edtEmail.requestFocus()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                edtEmail.error = "Enter a valid email address"
                edtEmail.requestFocus()
                return@setOnClickListener
            }

            // Send a password reset email using Firebase Authentication.
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->

                    if (task.isSuccessful) {

                        Toast.makeText(
                            this,
                            "Password reset email sent.\n\nPlease check your inbox or spam folder. Use the most recent reset email, as older reset links become invalid when a new one is requested.",
                            Toast.LENGTH_LONG
                        ).show()

                    } else {

                        // Display an error message if the reset email cannot be sent.
                        val message = when (task.exception) {

                            is FirebaseAuthInvalidUserException ->
                                "No account was found with this email address."

                            else ->
                                "Unable to send the reset email. Please try again."
                        }

                        Toast.makeText(
                            this,
                            message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }
}