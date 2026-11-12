
package com.eniola.smartstudyplanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

/** ProgressSummaryFragment displays the student's weekly study progress.
  It retrieves tasks from Firestore, calculates completion percentage,and shows the total, completed, pending, and missed tasks.*/
class ProgressSummaryFragment : Fragment() {

    /** Creates the progress summary screen and loads the student's weekly progress. */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(
            R.layout.fragment_progress_summary,
            container,
            false
        )

        loadWeeklyProgress(view)

        return view
    }

    /** Retrieves weekly tasks from Firestore and calculates their completion statistics. */
    private fun loadWeeklyProgress(view: View) {

        // Get the currently authenticated student and Firestore instance.
        val user =
            FirebaseAuth.getInstance().currentUser
                ?: return

        val firestore =
            FirebaseFirestore.getInstance()

        // Connect the progress summary views to their XML components.
        val txtProgressPercent =
            view.findViewById<TextView>(
                R.id.txtSummaryProgressPercent
            )

        val txtCompletion =
            view.findViewById<TextView>(
                R.id.txtSummaryCompletion
            )

        val txtTotal =
            view.findViewById<TextView>(
                R.id.txtSummaryTotal
            )

        val txtCompleted =
            view.findViewById<TextView>(
                R.id.txtSummaryCompleted
            )

        val txtPending =
            view.findViewById<TextView>(
                R.id.txtSummaryPending
            )

        val txtMissed =
            view.findViewById<TextView>(
                R.id.txtSummaryMissed
            )

        val calendar =
            Calendar.getInstance()

        // Set the start of the current week to Monday at 12:00 AM.
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

        // Set the end of the current week to the next Monday at 12:00 AM.
        calendar.add(
            Calendar.DAY_OF_MONTH,
            7
        )

        val startOfNextWeek =
            calendar.timeInMillis

        // Retrieve the student's task records from Firestore.
        firestore
            .collection("users")
            .document(user.uid)
            .collection("tasks")
            .get()
            .addOnSuccessListener { documents ->

                if (!isAdded) {
                    return@addOnSuccessListener
                }

                // Only include tasks whose deadlines fall within the current Monday-to-Sunday week.
                val weeklyTasks =
                    documents
                        .map {
                            it.toObject(Task::class.java)
                        }
                        .filter { task ->

                            val deadline =
                                task.deadline
                                    ?.toDate()
                                    ?.time
                                    ?: return@filter false

                            deadline >= startOfWeek &&
                                    deadline < startOfNextWeek
                        }

                val total =
                    weeklyTasks.size

                // Count tasks completed during the current week.
                val completed =
                    weeklyTasks.count {
                        it.completed
                    }

                // Count incomplete tasks whose deadlines have passed.
                val missed =
                    weeklyTasks.count { task ->

                        !task.completed &&
                                task.deadline
                                    ?.toDate()
                                    ?.time
                                    ?.let {
                                        it < System.currentTimeMillis()
                                    } == true
                    }

                // Pending tasks are incomplete tasks whose deadlines have not yet passed.
                val pending =
                    total -
                            completed -
                            missed

                // Calculate completion percentage for the current week.
                val percentage =
                    if (total > 0) {
                        (completed * 100) / total
                    } else {
                        0
                    }

                // Display the calculated progress and task statistics.
                txtProgressPercent.text =
                    "$percentage%"

                txtCompletion.text =
                    "$completed of $total tasks completed"

                txtTotal.text =
                    "Total Tasks: $total"

                txtCompleted.text =
                    "Completed: $completed"

                txtPending.text =
                    "Pending: $pending"

                txtMissed.text =
                    "Missed: $missed"
            }
    }
}
