
package com.eniola.smartstudyplanner

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.ImageView

/** SearchTasksFragment allows students to search their saved study tasks.
 * It retrieves tasks from Firestore, filters results by title, course, or description, and manages recent searches using SharedPreferences.*/
class SearchTasksFragment : Fragment() {

    // Search input, suggestions, and results interface components.
    private lateinit var edtSearchTasks: TextInputEditText
    private lateinit var layoutSearchSuggestions: LinearLayout
    private lateinit var layoutSearchResults: LinearLayout
    private lateinit var txtSearchSuggestionsTitle: TextView
    private lateinit var txtClearRecentSearches: TextView
    private lateinit var txtSearchResultsTitle: TextView

    // Stores the student's tasks retrieved from Firestore.
    private val allTasks = mutableListOf<Task>()

    // Stores previously submitted search queries.
    private val recentSearches = mutableListOf<String>()

    // Identifiers used to store recent searches in SharedPreferences.
    private val preferencesName =
        "smart_study_search_preferences"

    private val recentSearchesKey =
        "recent_searches"

    /** Initializes the task search screen and loads tasks and recent searches. */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(
            R.layout.fragment_search_tasks,
            container,
            false
        )

        // Connect the search interface components to their XML views.
        edtSearchTasks =
            view.findViewById(R.id.edtSearchTasks)

        layoutSearchSuggestions =
            view.findViewById(R.id.layoutSearchSuggestions)

        layoutSearchResults =
            view.findViewById(R.id.layoutSearchResults)

        txtSearchSuggestionsTitle =
            view.findViewById(R.id.txtSearchSuggestionsTitle)

        txtClearRecentSearches =
            view.findViewById(R.id.txtClearRecentSearches)

        txtSearchResultsTitle =
            view.findViewById(R.id.txtSearchResultsTitle)

        // Return to the previous screen when the back button is tapped.
        val imgBackSearch =
            view.findViewById<android.widget.ImageView>(
                R.id.imgBackSearch
            )

        imgBackSearch.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Remove saved search history when the clear button is tapped.
        txtClearRecentSearches.setOnClickListener {
            clearRecentSearches()
        }
        loadRecentSearches()
        setupSearch()
        loadTasks()

        // Search field is immediately ready for typing.
        edtSearchTasks.requestFocus()
        edtSearchTasks.postDelayed({

            if (!isAdded) return@postDelayed

            val inputMethodManager =
                requireContext().getSystemService(
                    Context.INPUT_METHOD_SERVICE
                ) as InputMethodManager

            inputMethodManager.showSoftInput(
                edtSearchTasks,
                InputMethodManager.SHOW_IMPLICIT
            )

        }, 200)

        return view
    }

    /** Handles live task searching, recent search suggestions, and search submissions. */
    private fun setupSearch() {

        // Display recent searches when the empty search field gains focus.
        edtSearchTasks.setOnFocusChangeListener { _, hasFocus ->

            if (hasFocus &&
                edtSearchTasks.text.isNullOrBlank()
            ) {
                showRecentSearches()
            }
        }

        // Filter tasks automatically whenever the search text changes.
        edtSearchTasks.doAfterTextChanged { text ->

            val query =
                text?.toString()?.trim() ?: ""

            if (query.isBlank()) {

                showRecentSearches()

            } else {
                searchTasks(query)
            }
        }

        // Save the search query when the Search or Enter key is pressed.
        edtSearchTasks.setOnEditorActionListener { _, actionId, event ->

            val isSearchAction =
                actionId ==
                        android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH

            val isEnterKey =
                event?.keyCode ==
                        android.view.KeyEvent.KEYCODE_ENTER &&
                        event.action ==
                        android.view.KeyEvent.ACTION_DOWN

            if (isSearchAction || isEnterKey) {

                val query =
                    edtSearchTasks.text
                        ?.toString()
                        ?.trim()
                        ?: ""

                if (query.isNotBlank()) {
                    saveRecentSearch(query)
                    searchTasks(query)
                }
                true
            } else {
                false
            }
        }
    }

    /** Retrieves the authenticated student's tasks from Firestore for searching. */
    private fun loadTasks() {

        val user =
            FirebaseAuth.getInstance().currentUser
                ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .collection("tasks")
            .get()
            .addOnSuccessListener { documents ->

                if (!isAdded) return@addOnSuccessListener

                allTasks.clear()

                allTasks.addAll(
                    documents.map {
                        it.toObject(Task::class.java)
                    }
                )

                // Do not show recent searches automatically if the user has not interacted with the field yet.
                if (edtSearchTasks.hasFocus() &&
                    edtSearchTasks.text.isNullOrBlank()
                ) {
                    showRecentSearches()
                }
            }
    }

    /** Filters tasks by title, course, or description and displays matching results. */
    private fun searchTasks(query: String) {

        val results =
            allTasks.filter { task ->

                task.title.contains(
                    query,
                    ignoreCase = true
                ) ||
                        task.course.contains(
                            query,
                            ignoreCase = true
                        ) ||
                        task.description.contains(
                            query,
                            ignoreCase = true
                        )
            }

        // Display search results.
        layoutSearchSuggestions.removeAllViews()
        txtSearchSuggestionsTitle.visibility =
            View.GONE
        txtSearchResultsTitle.visibility =
            View.VISIBLE
        txtSearchResultsTitle.text =
            if (results.isEmpty()) {
                "No results"
            } else {
                "Search results"
            }

        layoutSearchResults.removeAllViews()

        if (results.isEmpty()) {

            showNoResults(query)

        } else {

            results.forEach { task ->

                addSearchResult(task)
            }
        }
    }

    /** Displays the student's saved recent search queries as suggestions. */
    private fun showRecentSearches() {

        layoutSearchResults.removeAllViews()

        txtSearchResultsTitle.visibility =
            View.GONE

        layoutSearchSuggestions.removeAllViews()

        if (recentSearches.isEmpty()) {

            txtSearchSuggestionsTitle.visibility =
                View.GONE

            return
        }

        txtSearchSuggestionsTitle.visibility =
            View.VISIBLE

        txtSearchSuggestionsTitle.text =
            "Recent searches"

        recentSearches.forEach { search ->

            addRecentSearch(search)
        }
    }

    /** Creates a recent search row with options to reuse or delete the query. */
    private fun addRecentSearch(search: String) {

        val row =
            LayoutInflater.from(requireContext())
                .inflate(
                    R.layout.item_recent_search,
                    layoutSearchSuggestions,
                    false
                )

        val txtRecentSearch =
            row.findViewById<TextView>(
                R.id.txtRecentSearch
            )

        val imgDeleteRecentSearch =
            row.findViewById<ImageView>(
                R.id.imgDeleteRecentSearch
            )

        // Remove the selected query from recent searches.
        imgDeleteRecentSearch.setOnClickListener {
            removeRecentSearch(search)
        }

        txtRecentSearch.text = search

        // Tap the search row
        row.setOnClickListener {

            edtSearchTasks.setText(search)

            edtSearchTasks.setSelection(
                edtSearchTasks.text?.length ?: 0
            )
        }

        layoutSearchSuggestions.addView(row)
    }

    /** Loads previously saved search queries from SharedPreferences. */
    private fun loadRecentSearches() {

        val preferences =
            requireContext().getSharedPreferences(
                preferencesName,
                Context.MODE_PRIVATE
            )

        val saved =
            preferences.getStringSet(
                recentSearchesKey,
                emptySet()
            )

        recentSearches.clear()

        if (saved != null) {
            recentSearches.addAll(saved)
        }
    }

    /** Saves a search query and keeps a maximum of five recent searches. */
    private fun saveRecentSearch(
        query: String
    ) {

        val cleanQuery =
            query.trim()

        if (cleanQuery.isBlank()) {
            return
        }

        // Remove duplicate queries before adding the latest search.
        recentSearches.removeAll {
            it.equals(
                cleanQuery,
                ignoreCase = true
            )
        }

        recentSearches.add(
            0,
            cleanQuery
        )

        if (recentSearches.size > 5) {

            recentSearches.removeAt(
                recentSearches.lastIndex
            )
        }

        // Store the updated recent searches on the student's device.
        val preferences =
            requireContext().getSharedPreferences(
                preferencesName,
                Context.MODE_PRIVATE
            )

        preferences.edit()
            .putStringSet(
                recentSearchesKey,
                recentSearches.toSet()
            )
            .apply()
    }

    /** Deletes a selected recent search and refreshes the suggestions list. */
    private fun removeRecentSearch(
        search: String
    ) {

        recentSearches.removeAll {
            it.equals(
                search,
                ignoreCase = true
            )
        }

        val preferences =
            requireContext().getSharedPreferences(
                preferencesName,
                Context.MODE_PRIVATE
            )

        preferences.edit()
            .putStringSet(
                recentSearchesKey,
                recentSearches.toSet()
            )
            .apply()

        showRecentSearches()
    }

    /** Creates a search result row showing the task's title, course, and deadline. */
    private fun addSearchResult(
        task: Task
    ) {

        val resultView =
            LayoutInflater.from(requireContext())
                .inflate(
                    R.layout.item_task,
                    layoutSearchResults,
                    false
                )

        val txtTaskTitle =
            resultView.findViewById<TextView>(
                R.id.txtTaskTitle
            )

        val txtTaskCourse =
            resultView.findViewById<TextView>(
                R.id.txtTaskCourse
            )

        val txtTaskDateTime =
            resultView.findViewById<TextView>(
                R.id.txtTaskDateTime
            )

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

        layoutSearchResults.addView(
            resultView
        )
    }

    /** Displays a message when no tasks match the entered search query. */
    private fun showNoResults(
        query: String
    ) {

        val message =
            TextView(requireContext()).apply {
                text = "No tasks found for \"$query\""
                textSize = 14f
                setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.secondary_text
                    )
                )

                setPadding(0, 16, 0, 16
                )
            }

        layoutSearchResults.addView(
            message
        )
    }

    /** Clears all saved search history and updates the recent searches display. */
    private fun clearRecentSearches() {
        val preferences =
            requireContext().getSharedPreferences(
                preferencesName,
                Context.MODE_PRIVATE
            )
        preferences.edit()
            .remove(recentSearchesKey)
            .apply()
        recentSearches.clear()
        showRecentSearches()
    }
}