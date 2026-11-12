
package com.eniola.smartstudyplanner

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.eniola.smartstudyplanner.utils.NotificationHelper
import kotlin.math.abs
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast

/** MainActivity manages the application's main interface and navigation.
 * It handles bottom navigation, task creation, StudyMate AI access, notification setup, and internet connectivity checks.*/
class MainActivity : AppCompatActivity() {

    // Bottom navigation items.
    private lateinit var navHome: LinearLayout
    private lateinit var navCalendar: LinearLayout
    private lateinit var navTasks: LinearLayout
    private lateinit var navProfile: LinearLayout

    // Keeps the active StudyMate conversation while the app process is alive.
    var activeStudyMateChatId: String? = null

    // StudyMate response bubble.
    private lateinit var studyMateResponseBubble: TextView

    /** Initializes the main interface, navigation, notifications, and StudyMate controls. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create the notification channel for study task reminders.
        NotificationHelper.createNotificationChannel(this)

        // Enable edge-to-edge display and adjust padding for system bars.
        enableEdgeToEdge()

        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
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

        // Connect the bottom navigation buttons to their layout views.
        navHome = findViewById(R.id.navHome)
        navCalendar = findViewById(R.id.navCalendar)
        navTasks = findViewById(R.id.navTasks)
        navProfile = findViewById(R.id.navProfile)

        studyMateResponseBubble =
            findViewById(R.id.studyMateResponseBubble)

        // Display Home and check internet connectivity when first launched.
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
            updateBottomNavigation(navHome)
            checkInternetConnection()
        }

        // Open the Home screen.
        navHome.setOnClickListener {
            replaceFragment(HomeFragment())
            updateBottomNavigation(navHome)
        }

        // Open the Calendar screen.
        navCalendar.setOnClickListener {
            replaceFragment(CalendarFragment())
            updateBottomNavigation(navCalendar)
        }

        // Open the Tasks screen.
        navTasks.setOnClickListener {
            replaceFragment(TasksFragment())
            updateBottomNavigation(navTasks)
        }

        // Open the Profile screen.
        navProfile.setOnClickListener {
            replaceFragment(ProfileFragment())
            updateBottomNavigation(navProfile)
        }

        // Add Task button.
        val fabAddTask =
            findViewById<FloatingActionButton>(R.id.fabAddTask)

        fabAddTask.post {
            fabAddTask.bringToFront()
        }

        fabAddTask.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    AddTaskActivity::class.java
                )
            )
        }

        // Configure the floating StudyMate AI assistant button.
        setupStudyMateButton()

        // Open StudyMate when the response preview bubble is tapped.
        studyMateResponseBubble.setOnClickListener {
            studyMateResponseBubble.visibility = View.GONE
            openStudyMate()
        }
    }

    /** Hides the main navigation and task button while StudyMate is open. */
    fun showStudyMateMode() {

        findViewById<View>(R.id.customBottomNav).visibility =
            View.GONE

        findViewById<View>(R.id.fabAddTask).visibility =
            View.GONE

        findViewById<View>(R.id.txtAddTask).visibility =
            View.GONE

        findViewById<View>(R.id.btnStudyMate).visibility =
            View.GONE

        studyMateResponseBubble.visibility =
            View.GONE
    }

    /** Restores the main navigation and task button when leaving StudyMate. */
    fun hideStudyMateMode() {

        findViewById<View>(R.id.customBottomNav).visibility =
            View.VISIBLE

        findViewById<View>(R.id.fabAddTask).visibility =
            View.VISIBLE

        findViewById<View>(R.id.txtAddTask).visibility =
            View.VISIBLE

        findViewById<View>(R.id.btnStudyMate).visibility =
            View.VISIBLE
    }

    /** Displays a short animated preview of a completed StudyMate AI response. */
    fun showStudyMateResponsePreview(response: String) {
        val preview = response
            .replace("\n", " ")
            .trim()

        if (preview.isBlank()) return

        // Limit the response preview to 90 characters.
        studyMateResponseBubble.text =
            if (preview.length > 90) {
                preview.take(90).trimEnd() + "..."
            } else {
                preview
            }

        studyMateResponseBubble.visibility = View.VISIBLE
        studyMateResponseBubble.bringToFront()

        // Prepare the preview bubble for its appearance animation.
        studyMateResponseBubble.alpha = 0f
        studyMateResponseBubble.scaleX = 0.85f
        studyMateResponseBubble.scaleY = 0.85f

        studyMateResponseBubble.post {
            positionStudyMateResponseBubble()

            studyMateResponseBubble.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(220)
                .start()
        }
    }

    /** Positions the response preview bubble above the StudyMate button. */
    private fun positionStudyMateResponseBubble() {
        val btnStudyMate = findViewById<View>(R.id.btnStudyMate)

        studyMateResponseBubble.post {
            if (
                studyMateResponseBubble.visibility != View.VISIBLE ||
                studyMateResponseBubble.width == 0 ||
                studyMateResponseBubble.height == 0 ||
                btnStudyMate.width == 0 ||
                btnStudyMate.height == 0
            ) {
                return@post
            }

            studyMateResponseBubble.x =
                btnStudyMate.x +
                        btnStudyMate.width -
                        studyMateResponseBubble.width

            studyMateResponseBubble.y =
                btnStudyMate.y -
                        studyMateResponseBubble.height -
                        8f

            studyMateResponseBubble.bringToFront()
        }
    }

    /** Checks for a validated internet connection and alerts the user if offline. */
    private fun checkInternetConnection() {

        val connectivityManager =
            getSystemService(
                ConnectivityManager::class.java
            )

        val network =
            connectivityManager.activeNetwork

        val capabilities =
            connectivityManager.getNetworkCapabilities(network)

        val isConnected =
            capabilities?.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
            ) == true

        if (!isConnected) {
            Toast.makeText(
                this,
                "You are not connected to the internet.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /** Enables dragging the StudyMate button and tapping it to open the AI assistant. */
    private fun setupStudyMateButton() {

        val btnStudyMate =
            findViewById<View>(R.id.btnStudyMate)

        // Track the button's touch position and dragging state.
        var downX = 0f
        var downY = 0f
        var startX = 0f
        var startY = 0f
        var isDragging = false

        btnStudyMate.setOnTouchListener { view, event ->

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    downX = event.rawX
                    downY = event.rawY

                    startX = view.x
                    startY = view.y

                    isDragging = false

                    true
                }

                MotionEvent.ACTION_MOVE -> {

                    val moveX =
                        event.rawX - downX

                    val moveY =
                        event.rawY - downY

                    // Detect when the movement exceeds the dragging threshold.
                    if (abs(moveX) > 8 || abs(moveY) > 8) {
                        isDragging = true
                    }

                    if (isDragging) {

                        view.x =
                            startX + moveX

                        view.y =
                            startY + moveY

                        // Keep the response bubble positioned near the moving button.
                        if (
                            studyMateResponseBubble.visibility ==
                            View.VISIBLE
                        ) {
                            positionStudyMateResponseBubble()
                        }
                    }

                    true
                }

                MotionEvent.ACTION_UP -> {

                    // Open StudyMate only when the gesture is a tap.
                    if (!isDragging) {
                        studyMateResponseBubble.visibility =
                            View.GONE

                        openStudyMate()
                    }

                    true
                }

                else -> false
            }
        }
    }

    /** Opens the StudyMate AI assistant inside the main fragment container. */
    private fun openStudyMate() {

        studyMateResponseBubble.visibility =
            View.GONE

        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainer,
                StudyMateFragment()
            )
            .addToBackStack(null)
            .commit()
    }

    /** Highlights the selected bottom navigation item and resets the others. */
    private fun updateBottomNavigation(
        selectedView: View
    ) {

        val inactiveColor =
            ContextCompat.getColor(
                this,
                R.color.bottom_nav_color
            )

        val activeColor =
            ContextCompat.getColor(
                this,
                R.color.bottom_nav_active
            )

        val navigationItems =
            listOf(
                navHome,
                navCalendar,
                navTasks,
                navProfile
            )

        navigationItems.forEach { item ->

            val color =
                if (item == selectedView) {
                    activeColor
                } else {
                    inactiveColor
                }

            val icon =
                item.getChildAt(0) as ImageView

            val label =
                item.getChildAt(1) as TextView

            icon.setColorFilter(color)
            label.setTextColor(color)
        }
    }

    /** Replaces the currently displayed fragment with the selected screen. */
    private fun replaceFragment(
        fragment: Fragment
    ) {
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainer,
                fragment
            )
            .commit()
    }
}