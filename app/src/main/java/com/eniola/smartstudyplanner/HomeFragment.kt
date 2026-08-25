package com.eniola.smartstudyplanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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


class HomeFragment : Fragment() {



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

        val txtCurrentDate =
            view.findViewById<TextView>(R.id.txtCurrentDate)

        //Take the user to your Tasks screen
        val txtViewAllTasks =
            view.findViewById<TextView>(R.id.txtViewAllTasks)

        txtViewAllTasks.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, TasksFragment())
                .addToBackStack(null)
                .commit()
        }

        val txtViewAllDeadlines =
            view.findViewById<TextView>(R.id.txtViewAllDeadlines)

        txtViewAllDeadlines.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, TasksFragment())
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

        return view
    }

    override fun onResume() {
        super.onResume()
        val currentView = view ?: return
        if (!isAdded) {
            return
        }
        loadTodayTasks(currentView)
        loadUpcomingDeadlines(currentView)
    }

    private fun loadUserGreeting(view: View) {

        val txtGreeting =
            view.findViewById<TextView>(R.id.txtGreeting)

        val user =
            FirebaseAuth.getInstance().currentUser

        if (user == null) {
            txtGreeting.text = "NO USER"
            return
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {
                    txtGreeting.text = "USER DOCUMENT NOT FOUND"
                    return@addOnSuccessListener
                }

                val userName =
                    document.getString("fullName")
                        ?: "NO FULL NAME"

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
            .addOnFailureListener { exception ->

                txtGreeting.text =
                    "ERROR: ${exception.message}"
            }
    }
    private fun loadTodayTasks(view: View) {

        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            return
        }

        val layoutTodayTasks =
            view.findViewById<LinearLayout>(
                R.id.layoutTodayTasks
            )

        val txtTodayTasksMessage =
            view.findViewById<TextView>(
                R.id.txtTodayTasksMessage
            )

        val firestore =
            FirebaseFirestore.getInstance()

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

        firestore
            .collection("users")
            .document(user.uid)
            .collection("tasks")
            .whereGreaterThanOrEqualTo(
                "deadline",
                com.google.firebase.Timestamp(
                    startOfToday.time
                )
            )
            .whereLessThan(
                "deadline",
                com.google.firebase.Timestamp(
                    startOfTomorrow.time
                )
            )
            .orderBy("deadline")
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded) {
                    return@addOnSuccessListener
                }
                layoutTodayTasks.removeAllViews()

                // Convert all today's documents into Task objects.
                val allTodayTasks =
                    documents.map {
                        it.toObject(Task::class.java)
                    }

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

                    return@addOnSuccessListener
                }

                // If every task for today is completed,
                // show only the congratulatory message.
                if (completedCount == totalCount) {

                    txtTodayTasksMessage.text =
                        getString(
                            R.string.all_tasks_completed_today
                        )

                    return@addOnSuccessListener
                }

                // Keep only uncompleted tasks for display.
                val incompleteTasks =
                    allTodayTasks
                        .filter {
                            !it.completed
                        }

                // Sort incomplete tasks by priority first,
                // then by deadline.
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
                // Display a maximum of three
                // uncompleted tasks.
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
            .addOnFailureListener {
                if (!isAdded) {
                    return@addOnFailureListener
                }
                layoutTodayTasks.removeAllViews()

                txtTodayTasksMessage.text =
                    getString(
                        R.string.unable_to_load_today_tasks
                    )
            }
    }
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
        checkTaskCompleted.isChecked = task.completed

        // Update Firestore when the checkbox is changed.
        checkTaskCompleted.setOnCheckedChangeListener { _, isChecked ->
            updateTaskCompletion(
                taskId = task.id,
                completed = isChecked
            )
        }
        container.addView(itemView)
    }

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
                // Reload today's tasks so the dashboard immediately reflects the new completion state.
                view?.let {
                    loadTodayTasks(it)
                }
            }
            .addOnFailureListener {
                // If Firestore fails, refresh the dashboard so the checkbox returns to its saved state.
                view?.let {
                    loadTodayTasks(it)
                }
            }
    }
    private fun loadUpcomingDeadlines(view: View) {

        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            return
        }

        val layoutUpcomingTasks =
            view.findViewById<LinearLayout>(
                R.id.layoutUpcomingTasks
            )

        val firestore =
            FirebaseFirestore.getInstance()

        // Start of tomorrow.
        val startOfTomorrow =
            Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

        firestore
            .collection("users")
            .document(user.uid)
            .collection("tasks")
            .whereGreaterThanOrEqualTo(
                "deadline",
                com.google.firebase.Timestamp(
                    startOfTomorrow.time
                )
            )
            .whereEqualTo(
                "completed",
                false
            )
            .get()
            .addOnSuccessListener { documents ->
                val context = context ?: return@addOnSuccessListener
                if (!isAdded || view == null) {
                    return@addOnSuccessListener
                }

                layoutUpcomingTasks.removeAllViews()

                if (documents.isEmpty) {

                    val emptyMessage =
                        TextView(context) .apply {
                            text = getString(
                                R.string.no_upcoming_deadlines
                            )

                            setTextColor(
                                ContextCompat.getColor(
                                    context,
                                    R.color.secondary_text
                                )
                            )
                            textSize = 14f
                        }

                    layoutUpcomingTasks.addView(
                        emptyMessage
                    )

                    return@addOnSuccessListener
                }

                // Sort by priority first,
                // then by earliest deadline.
                val sortedTasks =
                    documents
                        .map {
                            it.toObject(Task::class.java)
                        }
                        .sortedWith(
                            compareBy<Task> {

                                when (it.priority) {
                                    "High" -> 1
                                    "Medium" -> 2
                                    "Normal" -> 3
                                    "Low" -> 4
                                    else -> 5
                                }

                            }.thenBy {

                                it.deadline
                                    ?.toDate()
                                    ?.time
                                    ?: Long.MAX_VALUE
                            }
                        )

                // Show only the top three upcoming deadlines.
                sortedTasks
                    .take(3)
                    .forEach { task ->

                        addUpcomingDeadline(
                            layoutUpcomingTasks,
                            task
                        )
                    }
            }
            .addOnFailureListener {
                val context = context ?: return@addOnFailureListener
                if (!isAdded || view == null) {
                    return@addOnFailureListener
                }

                val currentView = view ?: return@addOnFailureListener

                val currentLayout =
                    currentView.findViewById<LinearLayout>(
                        R.id.layoutUpcomingTasks
                    )

                currentLayout.removeAllViews()

                val errorMessage =
                    TextView(context).apply {
                        text =
                            getString(
                                R.string.failed_to_load_deadlines
                            )

                        setTextColor(
                            ContextCompat.getColor(
                                currentView.context,
                                R.color.secondary_text
                            )
                        )

                        textSize = 14f
                    }

                currentLayout.addView(
                    errorMessage
                )
            }
    }
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
    }