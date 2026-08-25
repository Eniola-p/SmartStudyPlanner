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

class SearchTasksFragment : Fragment() {

    private lateinit var edtSearchTasks: TextInputEditText
    private lateinit var layoutSearchSuggestions: LinearLayout
    private lateinit var layoutSearchResults: LinearLayout
    private lateinit var txtSearchSuggestionsTitle: TextView
    private lateinit var txtClearRecentSearches: TextView
    private lateinit var txtSearchResultsTitle: TextView
    private val allTasks = mutableListOf<Task>()

    private val recentSearches = mutableListOf<String>()

    private val preferencesName =
        "smart_study_search_preferences"

    private val recentSearchesKey =
        "recent_searches"

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


        val imgBackSearch =
            view.findViewById<android.widget.ImageView>(
                R.id.imgBackSearch
            )

        imgBackSearch.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

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

    private fun setupSearch() {

        edtSearchTasks.setOnFocusChangeListener { _, hasFocus ->

            if (hasFocus &&
                edtSearchTasks.text.isNullOrBlank()
            ) {
                showRecentSearches()
            }
        }

        edtSearchTasks.doAfterTextChanged { text ->

            val query =
                text?.toString()?.trim() ?: ""

            if (query.isBlank()) {

                showRecentSearches()

            } else {

                searchTasks(query)
            }
        }

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

                // Do not show recent searches automatically
                // if the user has not interacted with the field yet.
                if (edtSearchTasks.hasFocus() &&
                    edtSearchTasks.text.isNullOrBlank()
                ) {
                    showRecentSearches()
                }
            }
    }

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

    private fun saveRecentSearch(
        query: String
    ) {

        val cleanQuery =
            query.trim()

        if (cleanQuery.isBlank()) {
            return
        }

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