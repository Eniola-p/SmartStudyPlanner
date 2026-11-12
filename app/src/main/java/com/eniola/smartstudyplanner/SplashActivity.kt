
package com.eniola.smartstudyplanner

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

/** SplashActivity displays the application's startup screen.
 * It waits for two seconds, checks the user's Firebase login status, and redirects them to the Main screen or Login screen. */
class SplashActivity : AppCompatActivity() {

    /** Initializes the splash screen and handles navigation based on login status. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display for the splash screen.
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        val mainView =
            findViewById<android.view.View>(R.id.main)

        // Adjust the layout padding to accommodate system bars.
        ViewCompat.setOnApplyWindowInsetsListener(
            mainView
        ) { view, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        // Wait 2 seconds before deciding where to go.
        Handler(Looper.getMainLooper()).postDelayed({

            // Check whether a user is already signed in.
            val user =
                FirebaseAuth.getInstance().currentUser

            if (user != null) {

                // User is already logged in.
                startActivity(
                    Intent(
                        this,
                        MainActivity::class.java
                    )
                )

            } else {

                // No user is logged in.
                startActivity(
                    Intent(
                        this,
                        LoginActivity::class.java
                    )
                )
            }

            // Close the splash screen after navigating.
            finish()

        }, 2000)
    }
}
