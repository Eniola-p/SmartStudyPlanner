package com.eniola.smartstudyplanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.core.content.ContextCompat

class TaskDetailsFragment : Fragment() {

    private lateinit var txtDetailsTaskTitle: TextView
    private lateinit var txtDetailsTaskCourse: TextView
    private lateinit var txtDetailsTaskDate: TextView
    private lateinit var txtDetailsTaskTime: TextView
    private lateinit var txtDetailsTaskPriority: TextView
    private lateinit var txtDetailsTaskStatus: TextView
    private lateinit var txtDetailsTaskDescription: TextView

    private var taskId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        taskId = arguments?.getString("taskId") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(
            R.layout.fragment_task_details,
            container,
            false
        )

        txtDetailsTaskTitle =
            view.findViewById(R.id.txtDetailsTaskTitle)

        txtDetailsTaskCourse =
            view.findViewById(R.id.txtDetailsTaskCourse)

        txtDetailsTaskDate =
            view.findViewById(R.id.txtDetailsTaskDate)

        txtDetailsTaskTime =
            view.findViewById(R.id.txtDetailsTaskTime)

        txtDetailsTaskPriority =
            view.findViewById(R.id.txtDetailsTaskPriority)

        txtDetailsTaskStatus =
            view.findViewById(R.id.txtDetailsTaskStatus)

        txtDetailsTaskDescription =
            view.findViewById(R.id.txtDetailsTaskDescription)

        val imgBackTaskDetails =
            view.findViewById<ImageView>(
                R.id.imgBackTaskDetails
            )

        val imgEditTask =
            view.findViewById<ImageView>(
                R.id.imgEditTask
            )

        val imgDeleteTask =
            view.findViewById<ImageView>(
                R.id.imgDeleteTask
            )

        // Back
        imgBackTaskDetails.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Edit
        imgEditTask.setOnClickListener {

            val intent =
                android.content.Intent(
                    requireContext(),
                    EditTaskActivity::class.java
                )
            intent.putExtra(
                "taskId",
                taskId
            )
            startActivity(intent)
        }

        // Delete
        imgDeleteTask.setOnClickListener {
            showDeleteConfirmation()
        }
        loadTask()

        return view
    }

    override fun onResume() {
        super.onResume()

        if (taskId.isNotBlank()) {
            loadTask()
        }
    }

    private fun loadTask() {

        if (taskId.isBlank()) {
            Toast.makeText(
                requireContext(),
                "Task not found",
                Toast.LENGTH_SHORT
            ).show()

            parentFragmentManager.popBackStack()
            return
        }

        val user =
            FirebaseAuth.getInstance().currentUser

        if (user == null) {
            Toast.makeText(
                requireContext(),
                "Please sign in again",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .collection("tasks")
            .document(taskId)
            .get()
            .addOnSuccessListener { document ->

                if (!isAdded) return@addOnSuccessListener

                if (!document.exists()) {

                    Toast.makeText(
                        requireContext(),
                        "Task no longer exists",
                        Toast.LENGTH_SHORT
                    ).show()

                    parentFragmentManager.popBackStack()

                    return@addOnSuccessListener
                }

                val task =
                    document.toObject(Task::class.java)

                if (task == null) {
                    return@addOnSuccessListener
                }

                displayTask(task)
            }
            .addOnFailureListener {

                if (!isAdded) return@addOnFailureListener

                Toast.makeText(
                    requireContext(),
                    "Failed to load task",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun displayTask(task: Task) {

        txtDetailsTaskTitle.text =
            task.title

        txtDetailsTaskCourse.text =
            if (task.course.isBlank()) {
                "No course"
            } else {
                task.course
            }

        txtDetailsTaskDate.text =
            task.date

        txtDetailsTaskTime.text =
            task.time

        txtDetailsTaskPriority.text =
            task.priority

        txtDetailsTaskStatus.text =
            if (task.completed) {
                "Completed"
            } else {
                "Pending"
            }

        txtDetailsTaskDescription.text =
            if (task.description.isBlank()) {
                "No description"
            } else {
                task.description
            }
    }

    // Show confirmation before deleting.
    private fun showDeleteConfirmation() {

        val dialog = androidx.appcompat.app.AlertDialog.Builder(
            requireContext()
        )
            .setTitle("Delete task?")
            .setMessage(
                "Are you sure you want to delete this task? " +
                        "This action cannot be undone."
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                deleteTask()
            }
            .create()

        // Make dialog buttons blue.
        dialog.setOnShowListener {

            val primaryColor =
                ContextCompat.getColor(
                    requireContext(),
                    R.color.primary
                )

            dialog.getButton(
                androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE
            ).setTextColor(primaryColor)

            dialog.getButton(
                androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE
            ).setTextColor(primaryColor)
        }

        dialog.show()
    }

    // Delete the selected task.
    private fun deleteTask() {

        // Make sure a task ID exists.
        if (taskId.isBlank()) {
            Toast.makeText(
                requireContext(),
                "Task not found",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Make sure the user is signed in.
        val user =
            FirebaseAuth.getInstance().currentUser

        if (user == null) {
            Toast.makeText(
                requireContext(),
                "Please sign in again",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Find the task in Firestore.
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .collection("tasks")
            .document(taskId)
            .get()
            .addOnSuccessListener { document ->

                // Check that the task still exists.
                if (!document.exists()) {

                    Toast.makeText(
                        requireContext(),
                        "Task no longer exists",
                        Toast.LENGTH_SHORT
                    ).show()

                    parentFragmentManager.popBackStack()
                    return@addOnSuccessListener
                }

                val task =
                    document.toObject(Task::class.java)

                // Cancel the existing reminder.
                if (task != null && task.deadline != null) {

                    val reminderMinutes =
                        when (task.reminder) {

                            getString(R.string.five_minutes_before) -> 5
                            getString(R.string.fifteen_minutes_before) -> 15
                            getString(R.string.thirty_minutes_before) -> 30
                            getString(R.string.one_hour_before) -> 60
                            getString(R.string.one_day_before) -> 1440

                            else -> 0
                        }

                    if (reminderMinutes > 0) {

                        // Convert deadline to Calendar.
                        val deadline =
                            java.util.Calendar.getInstance().apply {
                                time = task.deadline.toDate()
                            }

                        // Remove the scheduled alarm.
                        com.eniola.smartstudyplanner.utils.ReminderScheduler
                            .cancelReminder(
                                context = requireContext(),
                                taskDateTime = deadline,
                                reminderMinutes = reminderMinutes
                            )
                    }
                }

                // Delete the task from Firestore.
                document.reference
                    .delete()
                    .addOnSuccessListener {

                        if (!isAdded) return@addOnSuccessListener

                        // Tell the user the deletion worked.
                        Toast.makeText(
                            requireContext(),
                            "Task deleted successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Return to the previous screen.
                        parentFragmentManager.popBackStack()
                    }
                    .addOnFailureListener { exception ->

                        if (!isAdded) return@addOnFailureListener

                        Toast.makeText(
                            requireContext(),
                            "Failed to delete task: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { exception ->

                if (!isAdded) return@addOnFailureListener

                Toast.makeText(
                    requireContext(),
                    "Failed to find task: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}