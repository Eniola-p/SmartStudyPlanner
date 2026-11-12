
package com.eniola.smartstudyplanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/** TaskFilterFragment allows students to filter their academic tasks.
 * It provides course, priority, and status filters, loads availablecourses from Firestore, and passes selections to the results screen.*/
class TaskFilterFragment : Fragment() {

    /** Inflates the task filtering interface. */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return inflater.inflate(
            R.layout.fragment_task_filter,
            container,
            false
        )
    }

    /** Initializes the filter dropdowns and handles reset and apply actions. */
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )

        // Connect the filter dropdowns and action buttons.
        val dropdownCourse =
            view.findViewById<MaterialAutoCompleteTextView>(
                R.id.dropdownFilterCourse
            )

        val dropdownPriority =
            view.findViewById<MaterialAutoCompleteTextView>(
                R.id.dropdownFilterPriority
            )

        val dropdownStatus =
            view.findViewById<MaterialAutoCompleteTextView>(
                R.id.dropdownFilterStatus
            )

        val btnReset =
            view.findViewById<MaterialButton>(
                R.id.btnResetFilters
            )

        val btnApply =
            view.findViewById<MaterialButton>(
                R.id.btnApplyFilters
            )

        val imgBack =
            view.findViewById<View>(
                R.id.imgBackFilter
            )

        // Load courses dynamically from Firestore.
        loadCourses(dropdownCourse)

        // Priority options.
        val priorities = arrayOf(
            "All",
            "High",
            "Medium",
            "Low"
        )

        dropdownPriority.setAdapter(
            ArrayAdapter(
                requireContext(),
                R.layout.item_filter_dropdown,
                priorities
            )
        )

        // Status options.
        val statuses = arrayOf(
            "All",
            "Pending",
            "Completed",
            "Missed"
        )

        dropdownStatus.setAdapter(
            ArrayAdapter(
                requireContext(),
                R.layout.item_filter_dropdown,
                statuses
            )
        )

        // Start with all filters selected.
        dropdownCourse.setText("All", false)
        dropdownPriority.setText("All", false)
        dropdownStatus.setText("All", false)

        // Back button.
        imgBack.setOnClickListener { parentFragmentManager.popBackStack() }

        // Reset filters.
        btnReset.setOnClickListener {
            dropdownCourse.setText("All", false)
            dropdownPriority.setText("All", false)
            dropdownStatus.setText("All", false)
        }

        // Apply selected filters.
        btnApply.setOnClickListener {

            // Ensure all filter fields contain a selected option.
            if (
                dropdownCourse.text.isNullOrBlank() ||
                dropdownPriority.text.isNullOrBlank() ||
                dropdownStatus.text.isNullOrBlank()
            ) {

                Toast.makeText(
                    requireContext(),
                    "Please select an option for all filters.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // Package the selected filter values for the results screen.
            val bundle = Bundle().apply {
                putString(
                    "course",
                    dropdownCourse.text.toString()
                )

                putString(
                    "priority",
                    dropdownPriority.text.toString()
                )

                putString(
                    "status",
                    dropdownStatus.text.toString()
                )
            }

            // Open filtered task results.
            val resultsFragment =
                TaskResultsFragment().apply {
                    arguments = bundle
                }

            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    resultsFragment
                )
                .addToBackStack(null)
                .commit()
        }
    }

    /** Retrieves unique course names from the student's saved Firestore tasks. */
    private fun loadCourses(
        dropdownCourse: MaterialAutoCompleteTextView
    ) {

        val user =
            FirebaseAuth.getInstance().currentUser
                ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .collection("tasks")
            .get()
            .addOnSuccessListener { documents ->

                if (!isAdded) {
                    return@addOnSuccessListener
                }

                val courses =
                    mutableListOf("All")

                // Collect course names without adding duplicates or empty values.
                documents.forEach { document ->

                    val task =
                        document.toObject(Task::class.java)

                    val course =
                        task.course.trim()

                    if (
                        course.isNotEmpty() &&
                        !courses.contains(course)
                    ) {
                        courses.add(course)
                    }
                }

                // Display unique courses.
                dropdownCourse.setAdapter(
                    ArrayAdapter(
                        requireContext(),
                        R.layout.item_filter_dropdown,
                        courses
                    )
                )

                dropdownCourse.setText(
                    "All",
                    false
                )
            }
    }

    // Adapter that prevents the dropdown from filtering itself.
    private class NonFilteringAdapter(
        context: android.content.Context,
        items: Array<String>
    ) : ArrayAdapter<String>(
        context,
        R.layout.item_filter_dropdown
    ), Filterable {

        // Stores the complete list of dropdown options.
        private val allItems =
            items.toList()

        /** Returns a custom filter that retains every dropdown option. */
        override fun getFilter(): Filter {

            return object : Filter() {

                /** Returns all options regardless of the entered search text. */
                override fun performFiltering(
                    constraint: CharSequence?
                ): FilterResults {

                    return FilterResults().apply {
                        values = allItems
                        count = allItems.size
                    }
                }

                /** Refreshes the dropdown after the filtering operation. */
                override fun publishResults(
                    constraint: CharSequence?,
                    results: FilterResults?
                ) {

                    notifyDataSetChanged()
                }
            }
        }
    }
}
