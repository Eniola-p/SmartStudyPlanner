package com.eniola.smartstudyplanner

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enables edge-to-edge display for a modern full-screen layout.
        enableEdgeToEdge()

        setContentView(R.layout.activity_main)

        // Applies system bar padding to the root layout.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Connects the Bottom Navigation View.
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Displays the Home Fragment when the app opens.
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }

        // Handles Bottom Navigation item selection.
        bottomNavigation.setOnItemSelectedListener {

            when (it.itemId) {

                R.id.nav_home -> replaceFragment(HomeFragment())

                R.id.nav_tasks -> replaceFragment(TaskFragment())

                R.id.nav_calendar -> replaceFragment(CalendarFragment())

                R.id.nav_profile -> replaceFragment(ProfileFragment())

            }

            true
        }
    }

    // Replaces the current fragment inside the FragmentContainerView.
    private fun replaceFragment(fragment: Fragment) {

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()

    }
}