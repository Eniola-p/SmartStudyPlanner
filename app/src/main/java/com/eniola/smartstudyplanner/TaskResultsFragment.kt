package com.eniola.smartstudyplanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TaskResultsFragment : Fragment() {

    private var selectedCourse = "All"
    private var selectedPriority = "All"
    private var selectedStatus = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get selected filters.
        arguments?.let {
            selectedCourse = it.getString("course", "All")
            selectedPriority = it.getString("priority", "All")
            selectedStatus = it.getString("status", "All")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return inflater.inflate(
            R.layout.fragment_task_results,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )

        val imgBackResults =
            view.findViewById<View>(
                R.id.imgBackResults
            )

        imgBackResults.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        loadFilteredTasks(view)
    }

    private fun loadFilteredTasks(view: View) {

        val user =
            FirebaseAuth.getInstance().currentUser
                ?: return

        val layoutTaskResults =
            view.findViewById<LinearLayout>(
                R.id.layoutTaskResults
            )

        val txtResultsSummary =
            view.findViewById<TextView>(
                R.id.txtResultsSummary
            )

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .collection("tasks")
            .get()
            .addOnSuccessListener { documents ->

                layoutTaskResults.removeAllViews()

                val tasks =
                    documents.map {
                        it.toObject(Task::class.java)
                    }

                val filteredTasks =
                    tasks.filter { task ->

                        val courseMatches =
                            selectedCourse == "All" || task.course.equals(
                                        selectedCourse,
                                        ignoreCase = true
                                    )

                        val priorityMatches =
                            selectedPriority == "All" || task.priority.equals(
                                        selectedPriority,
                                        ignoreCase = true
                                    )

                        val statusMatches =
                            when (selectedStatus) {
                                "All" -> true
                                "Pending" -> !task.completed && !isPastDeadline(task)
                                "Completed" -> task.completed
                                "Missed" -> !task.completed && isPastDeadline(task)

                                else -> true
                            }
                        courseMatches && priorityMatches && statusMatches
                    }
                txtResultsSummary.text = "${filteredTasks.size} task(s) found"

                if (filteredTasks.isEmpty()) {
                    showNoResults(
                        layoutTaskResults
                    )

                    return@addOnSuccessListener
                }

                filteredTasks
                    .sortedBy {
                        it.deadline?.toDate()?.time
                            ?: Long.MAX_VALUE
                    }
                    .forEach { task ->

                        addTask(
                            layoutTaskResults,
                            task
                        )
                    }
            }
    }

    private fun isPastDeadline(
        task: Task
    ): Boolean {

        val deadline =
            task.deadline?.toDate()?.time
                ?: return false

        return deadline <
                System.currentTimeMillis()
    }

    private fun addTask(
        container: LinearLayout,
        task: Task
    ) {

        val itemView =
            LayoutInflater.from(
                requireContext()
            ).inflate(
                R.layout.item_task,
                container,
                false
            )

        itemView.findViewById<TextView>(
            R.id.txtTaskTitle
        ).text = task.title

        itemView.findViewById<TextView>(
            R.id.txtTaskCourse
        ).text = task.course

        itemView.findViewById<TextView>(
            R.id.txtTaskDateTime
        ).text =
            "${task.date} • ${task.time}"

        itemView.findViewById<TextView>(
            R.id.txtTaskPriority
        ).text =
            task.priority

        container.addView(
            itemView
        )
    }

    private fun showNoResults(
        container: LinearLayout
    ) {

        val message =
            TextView(requireContext()).apply {

                text = "No tasks found"

                textSize = 16f

                setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.secondary_text
                    )
                )

                setPadding(
                    0,
                    40,
                    0,
                    0
                )
            }

        container.addView(
            message
        )
    }
}