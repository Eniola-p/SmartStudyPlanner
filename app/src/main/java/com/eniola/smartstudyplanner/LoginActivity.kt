package com.eniola.smartstudyplanner

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import android.view.View
import com.google.android.material.progressindicator.CircularProgressIndicator

class LoginActivity : AppCompatActivity() {
    // Firebase Authentication instance used for user login.
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Authentication.
        auth = FirebaseAuth.getInstance()

        // Connect XML views to Kotlin.
        val edtEmail = findViewById<TextInputEditText>(R.id.edtEmail)
        val edtPassword = findViewById<TextInputEditText>(R.id.edtPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val progressLogin = findViewById<CircularProgressIndicator>(R.id.progressLogin)
        val txtForgotPassword = findViewById<TextView>(R.id.txtForgotPassword)
        val txtSignUp = findViewById<TextView>(R.id.txtSignUp)

        // Open Sign Up screen.
        txtSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        // Login button.
        btnLogin.setOnClickListener {

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

            progressLogin.visibility = View.VISIBLE
            btnLogin.isEnabled = false

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->

                    progressLogin.visibility = View.GONE
                    btnLogin.isEnabled = true

                    if (task.isSuccessful) {

                        Toast.makeText(
                            this,
                            "Login successful!",
                            Toast.LENGTH_SHORT
                        ).show()

                        startActivity(Intent(this, MainActivity::class.java))
                        finish()

                    } else {

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

            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->

                    if (task.isSuccessful) {

                        Toast.makeText(
                            this,
                            "Password reset email sent.\n\nPlease check your inbox or spam folder. Use the most recent reset email," +
                                    " as older reset links become invalid when a new one is requested.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {

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