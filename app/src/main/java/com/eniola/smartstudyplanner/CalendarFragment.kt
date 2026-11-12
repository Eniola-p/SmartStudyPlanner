
package com.eniola.smartstudyplanner

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.eniola.smartstudyplanner.adapter.CalendarAdapter
import com.eniola.smartstudyplanner.adapter.DayTimelineAdapter
import com.eniola.smartstudyplanner.adapter.DayTimelineItem
import com.eniola.smartstudyplanner.adapter.WeekCalendarAdapter
import com.eniola.smartstudyplanner.databinding.FragmentCalendarBinding
import com.eniola.smartstudyplanner.enums.Priority
import com.eniola.smartstudyplanner.utils.CalendarUtils
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import android.graphics.Typeface
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.card.MaterialCardView

/** CalendarFragment displays study tasks in monthly, weekly, and daily views.
   It handles calendar navigation, date selection, task priority indicators,and displays scheduled tasks using data from the shared TaskViewModel.*/

class CalendarFragment : Fragment() {

    // Manages the calendar layout and its UI components.
    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    // Stores the month and year currently displayed.
    private var currentMonth = LocalDate.now().monthValue
    private var currentYear = LocalDate.now().year

    // Stores the currently selected calendar date.
    private var selectedDate: LocalDate = LocalDate.now()

    // Tracks the active calendar view.
    private var calendarView = "month"

    // Stores the user's tasks currently available to the calendar.
    private var allTasks: List<Task> = emptyList()

    // Shares the user's task data with other fragments.
    private lateinit var taskViewModel: TaskViewModel

    // Initializes the calendar interface and observes changes to task data.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentCalendarBinding.inflate(
                inflater,
                container,
                false
            )
        // Gets the ViewModel shared with the Activity.
        taskViewModel =
            ViewModelProvider(requireActivity())[TaskViewModel::class.java]
        setupViewSelector()
        setupNavigation()

        // Observe the shared task data from the ViewModel.
        taskViewModel.tasks.observe(viewLifecycleOwner) { tasks ->

            allTasks = tasks

            // Refresh the calendar whenever the task data changes.
            when (calendarView) {
                "month" -> showMonthView()
                "week" -> showWeekView()
                "day" -> showDayView()
            }
        }

        // Load tasks through the shared ViewModel.
        taskViewModel.loadTasks()
        return binding.root
    }

    // Handles switching between Day, Week and Month views.
    private fun setupViewSelector() {

        binding.calendarViewToggle.check(
            R.id.btnCalendarMonth
        )
        updateViewSelectorColors()

        binding.btnCalendarDay.setOnClickListener {
            calendarView = "day"
            updateViewSelectorColors()
            showDayView()
        }

        binding.btnCalendarWeek.setOnClickListener {
            calendarView = "week"
            updateViewSelectorColors()
            showWeekView()
        }

        binding.btnCalendarMonth.setOnClickListener {
            calendarView = "month"
            updateViewSelectorColors()
            showMonthView()
        }
    }

    // Updates the selected view button color.
    private fun updateViewSelectorColors() {

        val buttons = listOf(
            binding.btnCalendarDay,
            binding.btnCalendarWeek,
            binding.btnCalendarMonth
        )

        buttons.forEach { button ->

            if (button.isChecked) {

                button.backgroundTintList =
                    ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.primary
                        )
                    )

                button.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.button_text
                    )
                )

            } else {

                button.backgroundTintList =
                    ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.background
                        )
                    )

                button.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.primary
                    )
                )
            }
        }
    }

    // Sets up previous and next navigation.
    private fun setupNavigation() {

        binding.btnCalendarPrevious.setOnClickListener {

            when (calendarView) {

                "month" -> {

                    currentMonth--

                    if (currentMonth < 1) {
                        currentMonth = 12
                        currentYear--
                    }

                    setupCalendar()
                }

                "week" -> {

                    selectedDate =
                        selectedDate.minusWeeks(1)

                    setupWeekView()
                }

                "day" -> {

                    selectedDate =
                        selectedDate.minusDays(1)

                    setupDayView()
                }
            }
        }

        binding.btnCalendarNext.setOnClickListener {

            when (calendarView) {

                "month" -> {

                    currentMonth++

                    if (currentMonth > 12) {
                        currentMonth = 1
                        currentYear++
                    }

                    setupCalendar()
                }

                "week" -> {

                    selectedDate =
                        selectedDate.plusWeeks(1)

                    setupWeekView()
                }

                "day" -> {

                    selectedDate =
                        selectedDate.plusDays(1)

                    setupDayView()
                }
            }
        }
    }

    // Shows the monthly calendar.
    private fun showMonthView() {

        binding.recyclerCalendar.visibility =
            View.VISIBLE

        binding.recyclerDayTasks.visibility =
            View.GONE

        binding.txtNoDayTasks.visibility =
            View.GONE

        binding.includeSelectedDate.root.visibility =
            View.VISIBLE

        setupCalendar()
    }

    // Shows the weekly calendar.
    private fun showWeekView() {

        binding.recyclerCalendar.visibility =
            View.VISIBLE

        binding.recyclerDayTasks.visibility =
            View.GONE

        binding.txtNoDayTasks.visibility =
            View.GONE

        binding.includeSelectedDate.root.visibility =
            View.VISIBLE

        setupWeekView()
    }

    // Shows the daily timeline.
    private fun showDayView() {

        binding.recyclerCalendar.visibility =
            View.GONE

        binding.recyclerDayTasks.visibility =
            View.VISIBLE

        binding.includeSelectedDate.root.visibility =
            View.GONE

        binding.txtNoDayTasks.visibility =
            View.GONE

        setupDayView()
    }

    // Sets up the monthly calendar using the shared task data.
    private fun setupCalendar() {

        val calendarDays =
            CalendarUtils.generateMonth(
                currentYear,
                currentMonth
            ).map { calendarDay ->

                val date =
                    calendarDay.date

                if (date != null) {

                    val tasksForDate =
                        getTasksForDate(date)

                    calendarDay.copy(
                        isSelected =
                            date == selectedDate,

                        taskCount =
                            tasksForDate.size,

                        priorityLevel =
                            getHighestPriority(
                                tasksForDate
                            )
                    )

                } else {

                    calendarDay
                }
            }

        binding.recyclerCalendar.layoutManager =
            GridLayoutManager(
                requireContext(),
                7
            )

        val adapter =
            CalendarAdapter(calendarDays)

        binding.recyclerCalendar.adapter =
            adapter

        adapter.onDateClick = { calendarDay ->

            calendarDay.date?.let { date ->

                selectedDate = date

                currentMonth =
                    date.monthValue

                currentYear =
                    date.year

                // Rebuild the calendar so the newly selected date is highlighted.
                setupCalendar()
            }
        }

        updateMonthTitle()
        updateSelectedDateCard()
    }

    // Sets up the weekly calendar using the shared task data.
    private fun setupWeekView() {

        val weekDays =
            CalendarUtils.generateWeek(
                selectedDate
            ).map { calendarDay ->

                val date =
                    calendarDay.date

                if (date != null) {

                    val tasksForDate =
                        getTasksForDate(date)

                    calendarDay.copy(
                        isSelected =
                            date == selectedDate,

                        taskCount =
                            tasksForDate.size,

                        priorityLevel =
                            getHighestPriority(
                                tasksForDate
                            )
                    )

                } else {

                    calendarDay
                }
            }

        binding.recyclerCalendar.layoutManager =
            GridLayoutManager(
                requireContext(),
                7
            )

        val adapter =
            WeekCalendarAdapter(
                weekDays,
                selectedDate
            )

        binding.recyclerCalendar.adapter =
            adapter

        adapter.onDateClick = { calendarDay ->

            calendarDay.date?.let { date ->

                selectedDate = date

                // Rebuild the week and update the selected date card.
                setupWeekView()
            }
        }

        updateWeekTitle()
        updateSelectedDateCard()
    }

    // Sets up the daily timeline using the shared task data.
    private fun setupDayView() {

        updateDayTitle()
        setupDayTimeline()
    }

    // Updates the shared navigation title for Month view.
    private fun updateMonthTitle() {

        val monthName =
            Month.of(currentMonth)
                .getDisplayName(
                    TextStyle.FULL,
                    Locale.getDefault()
                )

        binding.txtCalendarPeriod.text =
            "$monthName $currentYear"
    }

    // Updates the shared navigation title for Week view.
    private fun updateWeekTitle() {

        val weekDays =
            CalendarUtils.generateWeek(
                selectedDate
            )

        val firstDay =
            weekDays.first().date

        val lastDay =
            weekDays.last().date

        if (firstDay != null && lastDay != null) {

            val firstMonth =
                firstDay.month
                    .getDisplayName(
                        TextStyle.SHORT,
                        Locale.getDefault()
                    )

            val lastMonth =
                lastDay.month
                    .getDisplayName(
                        TextStyle.SHORT,
                        Locale.getDefault()
                    )

            binding.txtCalendarPeriod.text =
                if (firstDay.year == lastDay.year) {

                    if (firstDay.month == lastDay.month) {

                        "$firstMonth ${firstDay.dayOfMonth} – " +
                                "${lastDay.dayOfMonth}, ${firstDay.year}"

                    } else {

                        "$firstMonth ${firstDay.dayOfMonth} – " +
                                "$lastMonth ${lastDay.dayOfMonth}, " +
                                firstDay.year
                    }

                } else {

                    "$firstMonth ${firstDay.dayOfMonth}, " +
                            "${firstDay.year} – " +
                            "$lastMonth ${lastDay.dayOfMonth}, " +
                            lastDay.year
                }
        }
    }

    // Updates the shared navigation title for Day view.
    private fun updateDayTitle() {

        binding.txtCalendarPeriod.text =
            selectedDate.format(
                DateTimeFormatter.ofPattern(
                    "EEEE, MMMM d, yyyy",
                    Locale.getDefault()
                )
            )
    }

    // Displays the actual tasks for the selected date.
    private fun updateSelectedDateCard() {

        binding.includeSelectedDate.txtSelectedDate.text =
            selectedDate.format(
                DateTimeFormatter.ofPattern(
                    "EEEE, MMMM d, yyyy",
                    Locale.getDefault()
                )
            )

        val selectedTasks =
            getTasksForDate(selectedDate)
                .sortedBy {
                    it.deadline?.toDate()?.time
                        ?: Long.MAX_VALUE
                }

        val taskContainer =
            binding.includeSelectedDate.layoutSelectedDateTasks

        // Clear previous task rows.
        taskContainer.removeAllViews()

        if (selectedTasks.isEmpty()) {

            val emptyMessage =
                TextView(requireContext()).apply {

                    text =
                        getString(
                            R.string.no_tasks_for_selected_date
                        )

                    setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.secondary_text
                        )
                    )

                    textSize = 14f
                }

            taskContainer.addView(emptyMessage)

            return
        }

        // Creates a clickable card for each task scheduled on the selected date.
        selectedTasks.forEach { task ->

            val taskCard =
                MaterialCardView(requireContext()).apply {

                    layoutParams =
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = 8.dp(requireContext())
                        }

                    radius = 10.dp(requireContext()).toFloat()
                    cardElevation = 0f

                    setCardBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.card_background
                        )
                    )

                    strokeWidth = 1.dp(requireContext())

                    strokeColor =
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.divider
                        )

                    isClickable = true
                    isFocusable = true

                    // Opens the selected task's details when its card is tapped.
                    setOnClickListener {

                        val detailsFragment =
                            TaskDetailsFragment().apply {

                                arguments =
                                    Bundle().apply {
                                        putString(
                                            "taskId",
                                            task.id
                                        )
                                    }
                            }

                        parentFragmentManager
                            .beginTransaction()
                            .replace(
                                R.id.fragmentContainer,
                                detailsFragment
                            )
                            .addToBackStack(null)
                            .commit()
                    }
                }

            // Arranges the task's time, title, and course vertically.
            val content =
                LinearLayout(requireContext()).apply {

                    orientation =
                        LinearLayout.VERTICAL

                    setPadding(
                        12.dp(requireContext()),
                        10.dp(requireContext()),
                        12.dp(requireContext()),
                        10.dp(requireContext())
                    )
                }

            // Formats the task deadline into a readable time.
            val time =
                task.deadline
                    ?.toDate()
                    ?.let {
                        DateTimeFormatter
                            .ofPattern(
                                "h:mm a",
                                Locale.getDefault()
                            )
                            .format(
                                it.toInstant()
                                    .atZone(
                                        ZoneId.systemDefault()
                                    )
                            )
                    }
                    ?: task.time

            // Displays the scheduled task time.
            val taskTime =
                TextView(requireContext()).apply {

                    text = time

                    setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.primary
                        )
                    )

                    textSize = 12f
                }

            // Displays the task title.
            val taskTitle =
                TextView(requireContext()).apply {

                    text = task.title

                    setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.primary_text
                        )
                    )

                    textSize = 14f

                    typeface =
                        Typeface.create(
                            "sans",
                            Typeface.BOLD
                        )

                    setPadding(
                        0,
                        4.dp(requireContext()),
                        0,
                        0
                    )
                }

            // Displays the course name or a default label if none was provided.
            val taskCourse =
                TextView(requireContext()).apply {

                    text =
                        if (task.course.isBlank()) {
                            "No course"
                        } else {
                            task.course
                        }

                    setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.secondary_text
                        )
                    )

                    textSize = 12f

                    setPadding(
                        0,
                        3.dp(requireContext()),
                        0,
                        0
                    )
                }

            // Adds the task information to the selected date card.
            content.addView(taskTime)
            content.addView(taskTitle)
            content.addView(taskCourse)

            taskCard.addView(content)

            taskContainer.addView(taskCard)
        }
    }

    // Creates the daily timeline from the selected day's tasks.
    private fun setupDayTimeline() {

        val dayTasks =
            getTasksForDate(selectedDate)
                .filter {
                    it.deadline != null
                }
                .sortedBy {
                    it.deadline?.toDate()?.time
                        ?: Long.MAX_VALUE
                }

        val timelineItems =
            mutableListOf<DayTimelineItem>()

        // Create the 24-hour timeline.
        for (hour in 0..23) {

            val tasksForHour =
                dayTasks.filter { task ->

                    val date =
                        task.deadline
                            ?.toDate()
                            ?.toInstant()
                            ?.atZone(
                                ZoneId.systemDefault()
                            )

                    date?.hour == hour
                }

            // Converts each hour into a 12-hour AM/PM label.
            val timeLabel =
                when {

                    hour == 0 ->
                        "12 AM"

                    hour < 12 ->
                        "$hour AM"

                    hour == 12 ->
                        "12 PM"

                    else ->
                        "${hour - 12} PM"
                }

            timelineItems.add(
                DayTimelineItem(
                    timeLabel = timeLabel,
                    tasks = tasksForHour
                )
            )
        }

        // Displays the hourly tasks using DayTimelineAdapter.
        binding.recyclerDayTasks.layoutManager =
            LinearLayoutManager(
                requireContext()
            )

        binding.recyclerDayTasks.adapter =
            DayTimelineAdapter(
                timelineItems
            )
    }

    // Returns all tasks belonging to a specific calendar date.
    private fun getTasksForDate(
        date: LocalDate
    ): List<Task> {

        return allTasks.filter { task ->

            val deadline =
                task.deadline
                    ?.toDate()
                    ?.toInstant()
                    ?.atZone(
                        ZoneId.systemDefault()
                    )

            deadline?.toLocalDate() == date
        }
    }

    // Returns the highest priority among tasks for a date.
    private fun getHighestPriority(
        tasks: List<Task>
    ): Priority {

        if (tasks.isEmpty()) {
            return Priority.NONE
        }

        return tasks.maxByOrNull {
            getPriorityValue(it.priority)
        }?.let { task ->

            when (task.priority.trim().lowercase()) {

                "high" ->
                    Priority.HIGH

                "medium" ->
                    Priority.MEDIUM

                "normal" ->
                    Priority.NORMAL

                "low" ->
                    Priority.LOW

                else ->
                    Priority.NONE
            }

        } ?: Priority.NONE
    }

    // Converts task priority into a sortable value.
    private fun getPriorityValue(
        priority: String
    ): Int {

        return when (priority.trim().lowercase()) {

            "high" -> 4
            "medium" -> 3
            "normal" -> 2
            "low" -> 1

            else -> 0
        }
    }

    // Converts density-independent pixels into screen pixels.
    private fun Int.dp(context: android.content.Context): Int =
        (this * context.resources.displayMetrics.density).toInt()

    //Clears the view binding when the Fragment's view is destroyed.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
