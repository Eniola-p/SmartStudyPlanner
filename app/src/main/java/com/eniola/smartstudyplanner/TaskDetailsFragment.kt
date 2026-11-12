
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

/** TaskDetailsFragment displays detailed information about a selected task.
 * It retrieves task data from Firestore and allows students to edit or delete tasks while managing their associated reminders.*/
class TaskDetailsFragment : Fragment() {

    // Views used to display the selected task's information.
    private lateinit var txtDetailsTaskTitle: TextView
    private lateinit var txtDetailsTaskCourse: TextView
    private lateinit var txtDetailsTaskDate: TextView
    private lateinit var txtDetailsTaskTime: TextView
    private lateinit var txtDetailsTaskPriority: TextView
    private lateinit var txtDetailsTaskStatus: TextView
    private lateinit var txtDetailsTaskDescription: TextView

    // Identifies the task whose details are being displayed.
    private var taskId: String = ""

    /** Retrieves the selected task ID from the Fragment arguments. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        taskId = arguments?.getString("taskId") ?: ""
    }

    /** Initializes the task details screen and handles navigation and task actions. */
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

        // Connect the task information views to their XML components.
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

        // Connect the back, edit, and delete action buttons.
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

        // Retrieve and display the selected task.
        loadTask()

        return view
    }

    /** Refreshes the task details when the student returns to this screen. */
    override fun onResume() {
        super.onResume()

        if (taskId.isNotBlank()) {
            loadTask()
        }
    }

    /** Retrieves the selected task from the authenticated student's Firestore records. */
    private fun loadTask() {

        // Ensure a valid task ID was provided.
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

        // Find the task using the student's user ID and selected task ID.
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

                // Convert the Firestore document into a Task object.
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

    /** Displays the task's title, course, deadline, priority, status, and description. */
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
