
package com.eniola.smartstudyplanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.google.firebase.auth.FirebaseAuth
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.ImageView
import com.eniola.smartstudyplanner.utils.TaskPriorityUtils
import java.io.File
import com.bumptech.glide.Glide

/**HomeFragment displays the student's main dashboard.
 It shows personalized greetings, today's tasks, upcoming deadlines,task priorities, and weekly study progress using shared task data.*/
class HomeFragment : Fragment() {

    // Shares the user's task data with the other fragments.
    private lateinit var taskViewModel: TaskViewModel

    //Initializes the Home dashboard, navigation, and task data observer.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(
            R.layout.fragment_home,
            container,
            false
        )

        // Gets the ViewModel shared with the Activity.
        taskViewModel =
            ViewModelProvider(requireActivity())[TaskViewModel::class.java]

        val txtCurrentDate =
            view.findViewById<TextView>(R.id.txtCurrentDate)

        // Take the user to the Tasks screen.
        val txtViewAllTasks =
            view.findViewById<TextView>(R.id.txtViewAllTasks)

        txtViewAllTasks.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, TasksFragment())
                .addToBackStack(null)
                .commit()
        }

        // Opens the upcoming deadlines section of the Tasks screen.
        val txtViewAllDeadlines =
            view.findViewById<TextView>(R.id.txtViewAllDeadlines)

        txtViewAllDeadlines.setOnClickListener {

            val fragment =
                TasksFragment().apply {

                    arguments =
                        Bundle().apply {
                            putBoolean(
                                "openUpcoming",
                                true
                            )
                        }
                }

            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    fragment
                )
                .addToBackStack(null)
                .commit()
        }

        val calendar = Calendar.getInstance()

        // Format today's date.
        val dateFormat =
            SimpleDateFormat(
                "EEEE, dd MMMM yyyy",
                Locale.getDefault()
            )

        // Display today's date.
        txtCurrentDate.text =
            dateFormat.format(calendar.time)

        loadUserGreeting(view)
        loadHomeProfilePicture(view)

        // Connect the Home profile picture.
        val imgHomeProfile =
            view.findViewById<ImageView>(
                R.id.imgHomeProfile
            )

        // Open the Profile screen when tapped.
        imgHomeProfile.setOnClickListener {

            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    ProfileFragment()
                )
                .addToBackStack(null)
                .commit()
        }

        // Opens the detailed study progress summary.
        val txtProgressSummary =
            view.findViewById<TextView>(
                R.id.txtProgressSummary
            )

        txtProgressSummary.setOnClickListener {

            val fragment =
                ProgressSummaryFragment()

            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    fragment
                )
                .addToBackStack(null)
                .commit()
        }

        // Opens the student's study statistics screen.
        val txtViewStatistics =
            view.findViewById<TextView>(
                R.id.txtViewStatistics
            )

        txtViewStatistics.setOnClickListener {

            val fragment =
                StatisticsFragment()

            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    fragment
                )
                .addToBackStack(null)
                .commit()
        }

        // Observe the shared task data from the ViewModel.
        taskViewModel.tasks.observe(viewLifecycleOwner) { tasks ->

            if (!isAdded || view != this.view) {
                return@observe
            }

            // Update the Home sections using the shared task list.
            updateTodayTasks(view, tasks)
            updateUpcomingDeadlines(view, tasks)
            updateStudyProgress(view, tasks)
        }

        // Load tasks through the shared ViewModel.
        taskViewModel.loadTasks()

        return view
    }

    // Refreshes the profile picture whenever the student returns to Home.
    override fun onResume() {
        super.onResume()

        val currentView = view ?: return

        if (!isAdded) {
            return
        }

        // Refresh the profile picture when returning to Home.
        loadHomeProfilePicture(currentView)
    }

    // Displays a personalized greeting based on the student's name and time of day.
    private fun loadUserGreeting(view: View) {

        val txtGreeting =
            view.findViewById<TextView>(R.id.txtGreeting)

        val user =
            FirebaseAuth.getInstance().currentUser

        if (user == null) {
            txtGreeting.text = "Welcome!"
            return
        }

        // Gets the name already available from the signed-in user.
        val userName =
            user.displayName?.takeIf { it.isNotBlank() }
                ?: "Student"

        // Gets the current hour for the greeting.
        val hour =
            Calendar.getInstance()
                .get(Calendar.HOUR_OF_DAY)

        val greeting = when (hour) {
            in 6..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..22 -> "Good Evening"
            else -> "Good Night"
        }

        txtGreeting.text =
            "$greeting, $userName!"
    }

    // Loads the saved local profile picture or displays the default profile icon.
    private fun loadHomeProfilePicture(view: View) {

        val imgHomeProfile =
            view.findViewById<ImageView>(
                R.id.imgHomeProfile
            )

        val file =
            File(
                requireContext().filesDir,
                "profile_picture.jpg"
            )

        if (file.exists()) {

            Glide.with(this)
                .load(file)
                .skipMemoryCache(true)
                .diskCacheStrategy(
                    com.bumptech.glide.load.engine.DiskCacheStrategy.NONE
                )
                .into(imgHomeProfile)

        } else {

            imgHomeProfile.setImageResource(
                R.drawable.ic_profile
            )
        }
    }

    // Filters today's tasks and displays completion, missed-task, and remaining-task information.
    private fun updateTodayTasks(
        view: View,
        tasks: List<Task>
    ) {

        val layoutTodayTasks =
            view.findViewById<LinearLayout>(
                R.id.layoutTodayTasks
            )

        val txtTodayTasksMessage =
            view.findViewById<TextView>(
                R.id.txtTodayTasksMessage
            )

        // Start of today.
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Start of tomorrow.
        val startOfTomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Keep only today's tasks.
        val allTodayTasks =
            tasks.filter { task ->

                val deadline =
                    task.deadline?.toDate()?.time
                        ?: return@filter false

                deadline >= startOfToday.timeInMillis &&
                        deadline < startOfTomorrow.timeInMillis
            }

        layoutTodayTasks.removeAllViews()

        val totalCount =
            allTodayTasks.size

        // Find completed tasks.
        val completedCount =
            allTodayTasks.count {
                it.completed
            }

        // Find missed tasks that are still incomplete.
        val missedCount =
            allTodayTasks.count { task ->

                val deadlineTime =
                    task.deadline?.toDate()?.time
                        ?: 0L

                !task.completed &&
                        deadlineTime < System.currentTimeMillis()
            }

        // If there are no tasks at all today.
        if (totalCount == 0) {

            txtTodayTasksMessage.text =
                getString(
                    R.string.no_study_tasks_today
                )

            return
        }

        // If every task for today is completed, show only the congratulatory message.
        if (completedCount == totalCount) {

            txtTodayTasksMessage.text =
                getString(
                    R.string.all_tasks_completed_today
                )

            return
        }

        // Keep only uncompleted tasks for display.
        val incompleteTasks =
            allTodayTasks.filter {
                !it.completed
            }

        // Sort incomplete tasks by priority first, then by deadline.
        val sortedTasks =
            incompleteTasks.sortedWith(
                compareBy<Task> {

                    when (it.priority) {
                        "High" -> 1
                        "Medium" -> 2
                        "Normal" -> 3
                        "Low" -> 4
                        else -> 5
                    }

                }.thenBy {

                    it.deadline?.toDate()?.time
                        ?: Long.MAX_VALUE
                }
            )

        // Display a maximum of three uncompleted tasks.
        sortedTasks
            .take(3)
            .forEach { task ->

                addTodayTask(
                    layoutTodayTasks,
                    task
                )
            }

        // Display the appropriate message.
        val remainingCount =
            incompleteTasks.size

        txtTodayTasksMessage.text =
            when {

                missedCount > 0 ->
                    getString(
                        R.string.missed_tasks_today,
                        missedCount
                    )

                completedCount > 0 ->
                    getString(
                        R.string.tasks_completed_today,
                        completedCount,
                        totalCount
                    )

                else ->
                    getString(
                        R.string.tasks_remaining_today,
                        remainingCount
                    )
            }
    }

    // Creates and displays a task row with its details, priority, and completion checkbox.
    private fun addTodayTask(
        container: LinearLayout,
        task: Task
    ) {
        val context = context ?: return
        val itemView =
            LayoutInflater.from(context)
                .inflate(
                    R.layout.home_item_today_task,
                    container,
                    false
                )

        val checkTaskCompleted =
            itemView.findViewById<android.widget.CheckBox>(
                R.id.checkTaskCompleted
            )

        val txtTodayTaskTitle =
            itemView.findViewById<TextView>(
                R.id.txtTodayTaskTitle
            )

        val txtTodayTaskDetails =
            itemView.findViewById<TextView>(
                R.id.txtTodayTaskDetails
            )

        val txtTodayTaskPriority =
            itemView.findViewById<TextView>(
                R.id.txtTodayTaskPriority
            )

        val imgTodayTaskPriority =
            itemView.findViewById<ImageView>(
                R.id.imgTodayTaskPriority
            )

        // Display the task information.
        txtTodayTaskTitle.text = task.title

        txtTodayTaskDetails.text =
            context.getString(
                R.string.today_task_details,
                task.course,
                task.time
            )

        val priority =
            TaskPriorityUtils.getPriorityFromName(
                task.priority
            )

        txtTodayTaskPriority.text =
            context.getString(
                R.string.priority_format,
                task.priority
            )

        imgTodayTaskPriority.setImageResource(
            priority.iconRes
        )

        imgTodayTaskPriority.setColorFilter(
            ContextCompat.getColor(
                requireContext(),
                priority.colorRes
            )
        )

        // Show the current completion state.
        checkTaskCompleted.isChecked =
            task.completed

        // Update Firestore when the checkbox is changed.
        checkTaskCompleted.setOnCheckedChangeListener { _, isChecked ->

            updateTaskCompletion(
                taskId = task.id,
                completed = isChecked
            )
        }

        container.addView(itemView)
    }

    // Updates a task's completion status in Firestore and refreshes the shared task list.
    private fun updateTaskCompletion(
        taskId: String,
        completed: Boolean
    ) {

        val user =
            FirebaseAuth.getInstance().currentUser

        if (user == null) {
            return
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .collection("tasks")
            .document(taskId)
            .update("completed", completed)
            .addOnSuccessListener {

                // Refresh the shared task data after the update.
                taskViewModel.loadTasks()
            }
            .addOnFailureListener {

                // Refresh the shared task data if the update fails.
                taskViewModel.loadTasks()
            }
    }

    /** Displays up to three upcoming incomplete tasks sorted by deadline and priority. */
    private fun updateUpcomingDeadlines(
        view: View,
        tasks: List<Task>
    ) {

        val layoutUpcomingTasks =
            view.findViewById<LinearLayout>(
                R.id.layoutUpcomingTasks
            )

        val startOfTomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Keep only incomplete tasks from tomorrow onward.
        val sortedTasks =
            tasks
                .filter { task ->

                    val deadline =
                        task.deadline?.toDate()?.time
                            ?: return@filter false

                    !task.completed &&
                            deadline >= startOfTomorrow.timeInMillis
                }
                .sortedWith(
                    compareBy<Task> {

                        it.deadline?.toDate()?.time
                            ?: Long.MAX_VALUE

                    }.thenBy {

                        when (it.priority) {
                            "High" -> 1
                            "Medium" -> 2
                            "Normal" -> 3
                            "Low" -> 4
                            else -> 5
                        }
                    }
                )

        layoutUpcomingTasks.removeAllViews()

        if (sortedTasks.isEmpty()) {

            val emptyMessage =
                TextView(requireContext()).apply {

                    text =
                        getString(
                            R.string.no_upcoming_deadlines
                        )

                    setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.secondary_text
                        )
                    )

                    textSize = 14f
                }

            layoutUpcomingTasks.addView(
                emptyMessage
            )

            return
        }

        // Display a maximum of three upcoming deadlines.
        sortedTasks
            .take(3)
            .forEach { task ->

                addUpcomingDeadline(
                    layoutUpcomingTasks,
                    task
                )
            }
    }

    /** Creates a deadline row showing the task's title, course, date, time, and priority. */
    private fun addUpcomingDeadline(
        container: LinearLayout,
        task: Task
    ) {

        val context = context ?: return

        val itemView =
            LayoutInflater.from(context)
                .inflate(
                    R.layout.item_upcoming_deadline,
                    container,
                    false
                )

        val txtTitle =
            itemView.findViewById<TextView>(
                R.id.txtDeadlineTaskTitle
            )

        val txtCourse =
            itemView.findViewById<TextView>(
                R.id.txtDeadlineCourse
            )

        val txtDateTime =
            itemView.findViewById<TextView>(
                R.id.txtDeadlineDateTime
            )

        val txtPriority =
            itemView.findViewById<TextView>(
                R.id.txtDeadlinePriority
            )

        val imgPriority =
            itemView.findViewById<ImageView>(
                R.id.imgDeadlinePriority
            )

        txtTitle.text = task.title

        txtCourse.text = task.course

        txtDateTime.text =
            getString(
                R.string.deadline_date_time,
                task.date,
                task.time
            )

        val priority =
            TaskPriorityUtils.getPriorityFromName(
                task.priority
            )

        // Display priority label.
        txtPriority.text =
            getString(
                R.string.priority_format,
                task.priority
            )

        // Match the priority label to the priority colour.
        txtPriority.setTextColor(
            ContextCompat.getColor(
                context,
                priority.colorRes
            )
        )

        // Display priority icon.
        imgPriority.setImageResource(
            priority.iconRes
        )

        imgPriority.setColorFilter(
            ContextCompat.getColor(
                context,
                priority.colorRes
            )
        )

        container.addView(itemView)
    }

    /** Calculates and displays weekly task completion progress and study statistics. */
    private fun updateStudyProgress(
        view: View,
        tasks: List<Task>
    ) {

        val txtWeeklyGoal =
            view.findViewById<TextView>(
                R.id.txtWeeklyGoal
            )

        val txtProgressPercent =
            view.findViewById<TextView>(
                R.id.txtProgressPercent
            )

        val progressStudy =
            view.findViewById<android.widget.ProgressBar>(
                R.id.progressStudy
            )

        val txtProgressSummary =
            view.findViewById<TextView>(
                R.id.txtProgressSummary
            )

        val calendar =
            Calendar.getInstance()

        // Set the current week's starting date to Monday.
        calendar.set(
            Calendar.DAY_OF_WEEK,
            Calendar.MONDAY
        )

        calendar.set(
            Calendar.HOUR_OF_DAY,
            0
        )

        calendar.set(
            Calendar.MINUTE,
            0
        )

        calendar.set(
            Calendar.SECOND,
            0
        )

        calendar.set(
            Calendar.MILLISECOND,
            0
        )

        val startOfWeek =
            calendar.timeInMillis

        calendar.add(
            Calendar.DAY_OF_MONTH,
            7
        )

        val startOfNextWeek =
            calendar.timeInMillis

        // Keep tasks planned for the current week.
        val weeklyTasks =
            tasks.filter { task ->

                val deadline =
                    task.deadline?.toDate()?.time
                        ?: return@filter false

                deadline >= startOfWeek &&
                        deadline < startOfNextWeek
            }

        val totalTasks =
            weeklyTasks.size

        if (totalTasks == 0) {

            txtWeeklyGoal.text =
                getString(
                    R.string.no_tasks_planned_this_week
                )

            txtProgressPercent.text =
                "0%"

            progressStudy.progress =
                0

            txtProgressSummary.text =
                getString(
                    R.string.progress_summary
                )

            return
        }

        // Count completed, missed, and pending tasks for the week.
        val completedTasks =
            weeklyTasks.count {
                it.completed
            }

        val missedTasks =
            weeklyTasks.count { task ->

                !task.completed &&
                        task.deadline
                            ?.toDate()
                            ?.time
                            ?.let {
                                it < System.currentTimeMillis()
                            } == true
            }

        val pendingTasks =
            totalTasks -
                    completedTasks -
                    missedTasks

        // Calculate the percentage of weekly tasks completed.
        val progress =
            (completedTasks * 100) /
                    totalTasks

        txtWeeklyGoal.text =
            getString(
                R.string.weekly_goal_tasks,
                totalTasks
            )

        txtProgressPercent.text =
            "$progress%"

        progressStudy.progress =
            progress

        txtProgressSummary.text =
            getString(
                R.string.progress_summary_tasks,
                completedTasks,
                pendingTasks,
                missedTasks
            )
    }
}