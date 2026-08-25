package com.eniola.smartstudyplanner

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.eniola.smartstudyplanner.utils.NotificationHelper

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createNotificationChannel(this)

        enableEdgeToEdge()

        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->

            val systemBars = insets.getInsets(
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

        val navHome = findViewById<android.view.View>(R.id.navHome)
        val navCalendar = findViewById<android.view.View>(R.id.navCalendar)
        val navTasks = findViewById<android.view.View>(R.id.navTasks)
        val navProfile = findViewById<android.view.View>(R.id.navProfile)

        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }

        navHome.setOnClickListener {
            replaceFragment(HomeFragment())
        }

        navCalendar.setOnClickListener {
            replaceFragment(CalendarFragment())
        }

        navTasks.setOnClickListener {
            replaceFragment(TasksFragment())
        }

        navProfile.setOnClickListener {
            replaceFragment(ProfileFragment())
        }

        val fabAddTask =
            findViewById<FloatingActionButton>(R.id.fabAddTask)

        fabAddTask.post {
            fabAddTask.bringToFront()
        }

        fabAddTask.setOnClickListener {
            startActivity(Intent(this, AddTaskActivity::class.java))
        }
    }

    private fun replaceFragment(fragment: Fragment) {

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}