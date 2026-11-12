
package com.eniola.smartstudyplanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.eniola.smartstudyplanner.utils.TaskPriorityUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import com.google.firebase.Timestamp
import java.util.Date
import android.app.AlertDialog

/** TasksFragment displays and manages the student's academic tasks.
 * It provides task filtering, searching, completion tracking, daily progress monitoring, and rescheduling of missed tasks.*/
class TasksFragment : Fragment() {

    // Stores the selected task filters and search query.
    private var selectedFilter = "today"
    private var searchQuery = ""
    private var selectedCourse = "All"
    private var selectedPriority = "All"
    private var selectedStatus = "All"

    // Shares the user's task data with the other screens.
    private lateinit var taskViewModel: TaskViewModel

    /** Initializes the task screen, filters, and shared task data. */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(
            R.layout.fragment_tasks,
            container,
            false
        )

        // Gets the ViewModel shared with the Activity.
        taskViewModel =
            ViewModelProvider(requireActivity())[TaskViewModel::class.java]

        displayTasksDate(view)

        // Open the Upcoming tab when requested by another screen.
        if (
            arguments?.getBoolean(
                "openUpcoming",
                false
            ) == true
        ) {
            selectedFilter = "upcoming"
        }

        setupFilterButtons(view)
        setupSearch(view)

        val imgFilterTasks =
            view.findViewById<android.widget.ImageView>(
                R.id.imgFilterTasks
            )

        imgFilterTasks.setOnClickListener {
            openTaskFilter()
        }

        // Receive and apply selected course, priority, and status filters.
        parentFragmentManager.setFragmentResultListener(
            "taskFilterResult",
            viewLifecycleOwner
        ) { _, bundle ->

            selectedCourse =
                bundle.getString(
                    "course",
                    "All"
                )

            selectedPriority =
                bundle.getString(
                    "priority",
                    "All"
                )

            selectedStatus =
                bundle.getString(
                    "status",
                    "All"
                )

            displayTasks(view, taskViewModel.tasks.value ?: emptyList())
        }

        // Observe the shared task data from the ViewModel.
        taskViewModel.tasks.observe(viewLifecycleOwner) { tasks ->

            if (!isAdded || view != this.view) {
                return@observe
            }

            // Display the current task data using the existing filters and UI.
            displayTasks(view, tasks)
        }

        // Load tasks through the shared ViewModel.
        taskViewModel.loadTasks()

        return view
    }

    /** Displays the current date at the top of the Tasks screen. */
    private fun displayTasksDate(view: View) {

        val txtTasksDate =
            view.findViewById<TextView>(
                R.id.txtTasksDate
            )

        val dateFormat =
            SimpleDateFormat(
                "EEEE, MMMM dd",
                Locale.getDefault()
            )

        txtTasksDate.text =
            dateFormat.format(
                Calendar.getInstance().time
            )
    }

    /** Configures the Today, Upcoming, Completed, and Missed task filters. */
    private fun setupFilterButtons(view: View) {

        val btnToday =
            view.findViewById<MaterialButton>(
                R.id.btnFilterToday
            )

        val btnUpcoming =
            view.findViewById<MaterialButton>(
                R.id.btnFilterUpcoming
            )

        val btnCompleted =
            view.findViewById<MaterialButton>(
                R.id.btnFilterCompleted
            )

        val btnMissed =
            view.findViewById<MaterialButton>(
                R.id.btnFilterMissed
            )

        // Display today's tasks.
        btnToday.setOnClickListener {
            selectedFilter = "today"
            updateSelectedFilter(view)
            displayTasks(view, taskViewModel.tasks.value ?: emptyList())
        }

        // Display upcoming tasks.
        btnUpcoming.setOnClickListener {
            selectedFilter = "upcoming"
            updateSelectedFilter(view)
            displayTasks(view, taskViewModel.tasks.value ?: emptyList())
        }

        // Display completed tasks.
        btnCompleted.setOnClickListener {
            selectedFilter = "completed"
            updateSelectedFilter(view)
            displayTasks(view, taskViewModel.tasks.value ?: emptyList())
        }

        // Display missed tasks.
        btnMissed.setOnClickListener {
            selectedFilter = "missed"
            updateSelectedFilter(view)
            displayTasks(view, taskViewModel.tasks.value ?: emptyList())
        }

        updateSelectedFilter(view)
    }

    /** Opens the task search screen when the search icon is selected. */
    private fun setupSearch(view: View) {
        val imgSearchTasks =
            view.findViewById<android.widget.ImageView>(
                R.id.imgSearchTasks
            )

        imgSearchTasks.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    SearchTasksFragment()
                )
                .addToBackStack(null)
                .commit()
        }
    }

    // Open the task filter screen.
    private fun openTaskFilter() {

        parentFragmentManager.beginTransaction()
            .replace(
                R.id.fragmentContainer,
                TaskFilterFragment()
            )
            .addToBackStack(null)
            .commit()
    }

    /** Highlights the currently selected task filter button. */
    private fun updateSelectedFilter(view: View) {

        val buttons = listOf(
            view.findViewById<MaterialButton>(
                R.id.btnFilterToday
            ),
            view.findViewById<MaterialButton>(
                R.id.btnFilterUpcoming
            ),
            view.findViewById<MaterialButton>(
                R.id.btnFilterCompleted
            ),
            view.findViewById<MaterialButton>(
                R.id.btnFilterMissed
            )
        )

        val names = listOf(
            "today",
            "upcoming",
            "completed",
            "missed"
        )

        // Apply active and inactive colors to the filter buttons.
        for (i in buttons.indices) {

            if (names[i] == selectedFilter) {

                buttons[i].setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.primary_dark
                    )
                )

                buttons[i].setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        android.R.color.white
                    )
                )

            } else {

                buttons[i].setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        android.R.color.transparent
                    )
                )

                buttons[i].setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.primary_dark
                    )
                )
            }
        }
    }

    /** Filters, sorts, and displays tasks based on the student's selections. */
    private fun displayTasks(
        view: View,
        tasks: List<Task>
    ) {

        val layoutAllTasks =
            view.findViewById<LinearLayout>(
                R.id.layoutAllTasks
            )

        layoutAllTasks.removeAllViews()

        updateTodayProgress(
            view,
            tasks
        )

        // Select tasks belonging to the active task category.
        val filteredTasks =
            when (selectedFilter) {

                "today" ->
                    getTodayTasks(tasks)

                "upcoming" ->
                    getUpcomingTasks(tasks)

                "completed" ->
                    tasks.filter {
                        it.completed
                    }

                "missed" ->
                    tasks.filter {
                        !it.completed &&
                                isPastDeadline(it)
                    }

                else ->
                    emptyList()
            }

        // Apply the Course, Priority, and Status filters.
        val filteredByOptions =
            filteredTasks.filter { task ->

                val courseMatches =
                    selectedCourse == "All" ||
                            task.course.equals(
                                selectedCourse,
                                ignoreCase = true
                            )

                val priorityMatches =
                    selectedPriority == "All" ||
                            task.priority.equals(
                                selectedPriority,
                                ignoreCase = true
                            )

                val statusMatches =
                    when (selectedStatus) {

                        "All" -> true

                        "Pending" ->
                            !task.completed &&
                                    !isPastDeadline(task)

                        "Completed" ->
                            task.completed

                        "Missed" ->
                            !task.completed &&
                                    isPastDeadline(task)

                        else -> true
                    }

                courseMatches &&
                        priorityMatches &&
                        statusMatches
            }

        // Match the search query against task titles, courses, and descriptions.
        val searchedTasks =
            if (searchQuery.isBlank()) {

                filteredByOptions

            } else {

                filteredByOptions.filter { task ->

                    task.title.contains(
                        searchQuery,
                        ignoreCase = true
                    ) ||
                            task.course.contains(
                                searchQuery,
                                ignoreCase = true
                            ) ||
                            task.description.contains(
                                searchQuery,
                                ignoreCase = true
                            )
                }
            }

        val sortedTasks =
            sortTasksByPriorityAndDate(
                searchedTasks
            )

        if (sortedTasks.isEmpty()) {

            showEmptyMessage(
                layoutAllTasks
            )

            return
        }

        // Add each matching task to the task list.
        sortedTasks.forEach { task ->

            addTask(
                layoutAllTasks,
                task
            )
        }
    }

    /** Calculates and displays today's task completion percentage. */
    private fun updateTodayProgress(
        view: View,
        tasks: List<Task>
    ) {

        val todayTasks =
            getTodayTasks(tasks)

        val completedTasks =
            todayTasks.count {
                it.completed
            }

        val totalTasks =
            todayTasks.size

        val percentage =
            if (totalTasks == 0) {
                0
            } else {
                (completedTasks * 100) / totalTasks
            }

        val txtTaskProgress =
            view.findViewById<TextView>(
                R.id.txtTaskProgress
            )

        val txtTaskProgressPercentage =
            view.findViewById<TextView>(
                R.id.txtTaskProgressPercentage
            )

        val progressTodayTasks =
            view.findViewById<ProgressBar>(
                R.id.progressTodayTasks
            )

        // Update the progress text and progress bar.
        txtTaskProgress.text =
            view.context.getString(
                R.string.tasks_progress_format,
                completedTasks,
                totalTasks
            )

        txtTaskProgressPercentage.text =
            view.context.getString(
                R.string.percentage_format,
                percentage
            )

        progressTodayTasks.progress =
            percentage
    }

    /** Returns tasks whose deadlines fall within the current day. */
    private fun getTodayTasks(
        tasks: List<Task>
    ): List<Task> {

        // Determine the beginning of today and tomorrow.
        val startOfToday =
            Calendar.getInstance().apply {

                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

        val startOfTomorrow =
            Calendar.getInstance().apply {

                add(Calendar.DAY_OF_MONTH, 1)

                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

        val start =
            startOfToday.timeInMillis

        val end =
            startOfTomorrow.timeInMillis

        return tasks.filter { task ->

            val deadline =
                task.deadline?.toDate()?.time
                    ?: 0L

            deadline >= start &&
                    deadline < end
        }
    }

    /** Returns incomplete tasks scheduled from tomorrow onward. */
    private fun getUpcomingTasks(
        tasks: List<Task>
    ): List<Task> {

        val startOfTomorrow =
            Calendar.getInstance().apply {

                add(Calendar.DAY_OF_MONTH, 1)

                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

        val start =
            startOfTomorrow.timeInMillis

        return tasks.filter { task ->

            val deadline =
                task.deadline?.toDate()?.time
                    ?: 0L

            !task.completed &&
                    deadline >= start
        }
    }

    /** Checks whether a task has passed its scheduled deadline. */
    private fun isPastDeadline(
        task: Task
    ): Boolean {

        val deadline =
            task.deadline?.toDate()?.time
                ?: return false

        return deadline <
                System.currentTimeMillis()
    }

    /** Sorts tasks by priority level and then by their deadlines. */
    private fun sortTasksByPriorityAndDate(
        tasks: List<Task>
    ): List<Task> {

        return tasks.sortedWith(

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
    }

    /** Displays an appropriate message when the selected task list is empty. */
    private fun showEmptyMessage(
        container: LinearLayout
    ) {

        val emptyMessage =
            TextView(context ?: return).apply {

                text =
                    when (selectedFilter) {

                        "today" ->
                            getString(
                                R.string.no_study_tasks_today
                            )

                        "upcoming" ->
                            getString(
                                R.string.no_upcoming_tasks
                            )

                        "completed" ->
                            getString(
                                R.string.no_completed_tasks
                            )

                        "missed" ->
                            getString(
                                R.string.no_missed_tasks
                            )

                        else ->
                            getString(
                                R.string.no_tasks
                            )
                    }

                setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.secondary_text
                    )
                )

                textSize = 14f
            }

        container.addView(
            emptyMessage
        )
    }

    /** Creates a task row with details, completion controls, and rescheduling options. */
    private fun addTask(
        container: LinearLayout,
        task: Task
    ) {
        if (!isAdded) return

        val itemView =
            LayoutInflater.from(requireContext())
                .inflate(
                    R.layout.item_task,
                    container,
                    false
                )

        val checkTaskCompleted =
            itemView.findViewById<CheckBox>(
                R.id.checkTaskCompleted
            )

        val txtTaskTitle =
            itemView.findViewById<TextView>(
                R.id.txtTaskTitle
            )

        val txtTaskCourse =
            itemView.findViewById<TextView>(
                R.id.txtTaskCourse
            )

        val txtTaskDateTime =
            itemView.findViewById<TextView>(
                R.id.txtTaskDateTime
            )

        val txtTaskDueStatus =
            itemView.findViewById<TextView>(
                R.id.txtTaskDueStatus
            )

        val btnRescheduleTask =
            itemView.findViewById<MaterialButton>(
                R.id.btnRescheduleTask
            )

        val txtTaskPriority =
            itemView.findViewById<TextView>(
                R.id.txtTaskPriority
            )

        // Display the task's title, course, and deadline.
        txtTaskTitle.text =
            task.title

        txtTaskCourse.text =
            task.course

        txtTaskDateTime.text =
            getString(
                R.string.deadline_date_time,
                task.date,
                task.time
            )

        val deadlineTime =
            task.deadline?.toDate()?.time

        if (deadlineTime != null) {

            val currentTime =
                System.currentTimeMillis()

            if (
                !task.completed &&
                deadlineTime < currentTime
            ) {

                // This task has been missed.
                txtTaskDueStatus.text =
                    getString(
                        R.string.missed_task
                    )

                txtTaskDueStatus.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.error
                    )
                )

                txtTaskDueStatus.visibility =
                    View.VISIBLE

                btnRescheduleTask.visibility =
                    View.VISIBLE

                btnRescheduleTask.setOnClickListener {

                    showRescheduleDialog(
                        task
                    )
                }

            } else if (!task.completed) {

                // Show the remaining time for incomplete upcoming tasks.
                txtTaskDueStatus.text =
                    getDueStatus(
                        deadlineTime
                    )

                txtTaskDueStatus.visibility =
                    View.VISIBLE

                btnRescheduleTask.visibility =
                    View.GONE
            }
        }

        // Format the task priority using its corresponding color.
        val priority =
            TaskPriorityUtils.getPriorityFromName(
                task.priority
            )

        txtTaskPriority.text =
            getString(
                R.string.priority_format,
                priority.displayName
            )

        txtTaskPriority.setTextColor(
            ContextCompat.getColor(
                itemView.context,
                priority.colorRes
            )
        )

        // Allow students to mark tasks as completed or incomplete.
        checkTaskCompleted.isChecked =
            task.completed

        checkTaskCompleted.setOnCheckedChangeListener {
                _, isChecked ->

            updateTaskCompletion(
                task.id,
                isChecked
            )
        }

        // Make the whole task item clickable
        itemView.setOnClickListener {

            val bundle = Bundle().apply {
                putString("taskId", task.id)
            }

            val fragment =
                TaskDetailsFragment().apply {
                    arguments = bundle
                }

            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    fragment
                )
                .addToBackStack(null)
                .commit()
        }

        container.addView(
            itemView
        )
    }

    /** Updates the task completion status in Firestore and refreshes task data. */
    private fun updateTaskCompletion(
        taskId: String,
        completed: Boolean
    ) {

        val user =
            FirebaseAuth.getInstance().currentUser
                ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .collection("tasks")
            .document(taskId)
            .update(
                "completed",
                completed
            )
            .addOnSuccessListener {

                view?.let {
                    // Refresh the shared task data after the update.
                    taskViewModel.loadTasks()
                }
            }
    }

    /** Calculates a readable countdown describing when a task is due. */
    private fun getDueStatus(
        deadlineTime: Long
    ): String {

        val difference =
            deadlineTime - System.currentTimeMillis()

        val minute =
            60 * 1000L

        val hour =
            60 * minute

        val day =
            24 * hour

        val week =
            7 * day

        val month =
            30 * day

        // Select a suitable time description based on the remaining duration.
        return when {

            difference <= day -> {

                val hours =
                    (difference / hour).toInt()

                if (hours <= 0) {

                    getString(
                        R.string.due_today
                    )

                } else {

                    getString(
                        R.string.due_in_hours,
                        hours
                    )
                }
            }

            difference <= 2 * day -> {

                getString(
                    R.string.due_tomorrow
                )
            }

            difference < week -> {

                val days =
                    (difference / day).toInt()

                getString(
                    R.string.due_in_days,
                    days
                )
            }

            difference < 2 * week -> {

                getString(
                    R.string.due_in_week
                )
            }

            difference < month -> {

                val weeks =
                    (difference / week).toInt()

                getString(
                    R.string.due_in_weeks,
                    weeks
                )
            }

            difference < 2 * month -> {

                getString(
                    R.string.due_in_month
                )
            }

            else -> {

                val months =
                    (difference / month).toInt()

                getString(
                    R.string.due_in_months,
                    months
                )
            }
        }
    }

    /** Opens confirmation, date, and time dialogs for rescheduling a task. */
    private fun showRescheduleDialog(task: Task) {
        val context = context ?: return

        AlertDialog.Builder(context)
            .setTitle("Reschedule Task?")
            .setMessage(
                "Do you want to choose a new date and time for this task?"
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Continue") { _, _ ->
                val calendar = Calendar.getInstance()

                // Use the existing task deadline as the starting date and time.
                task.deadline?.toDate()?.let {
                    calendar.time = it
                }

                // First choose a new date.
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->

                        calendar.set(
                            Calendar.YEAR,
                            year
                        )

                        calendar.set(
                            Calendar.MONTH,
                            month
                        )

                        calendar.set(
                            Calendar.DAY_OF_MONTH,
                            dayOfMonth
                        )

                        // Then choose a new time.
                        TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->

                                calendar.set(
                                    Calendar.HOUR_OF_DAY,
                                    hourOfDay
                                )

                                calendar.set(
                                    Calendar.MINUTE,
                                    minute
                                )

                                calendar.set(
                                    Calendar.SECOND,
                                    0
                                )

                                calendar.set(
                                    Calendar.MILLISECOND,
                                    0
                                )

                                saveRescheduledTask(
                                    task,
                                    calendar
                                )
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            false
                        ).show()
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
            .show()
    }

    /** Saves the task's updated deadline and resets its completion status. */
    private fun saveRescheduledTask(
        task: Task,
        calendar: Calendar
    ) {

        val user =
            FirebaseAuth.getInstance().currentUser
                ?: return

        val newDeadline =
            Timestamp(
                Date(calendar.timeInMillis)
            )

        // Format the newly selected date and time for display.
        val dateFormat =
            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            )

        val timeFormat =
            SimpleDateFormat(
                "hh:mm a",
                Locale.getDefault()
            )

        val newDate =
            dateFormat.format(calendar.time)

        val newTime =
            timeFormat.format(calendar.time)

        // Update the selected task in Firestore.
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .collection("tasks")
            .document(task.id)
            .update(
                mapOf(
                    "deadline" to newDeadline,
                    "date" to newDate,
                    "time" to newTime,
                    "completed" to false
                )
            )
            .addOnSuccessListener {

                view?.let {
                    // Refresh the shared task data after the update.
                    taskViewModel.loadTasks()
                }
            }
    }
}
