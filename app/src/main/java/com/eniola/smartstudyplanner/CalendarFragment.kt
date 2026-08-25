package com.eniola.smartstudyplanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.eniola.smartstudyplanner.adapter.CalendarAdapter
import com.eniola.smartstudyplanner.databinding.FragmentCalendarBinding
import com.eniola.smartstudyplanner.utils.CalendarUtils
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/** CalendarFragment displays the monthly calendar.*/
class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    // Keeps track of the month currently being displayed
    private var currentMonth = LocalDate.now().monthValue

    // Keeps track of the year currently being displayed
    private var currentYear = LocalDate.now().year

    // Stores the currently selected date.
    private var selectedDate: LocalDate? = LocalDate.now()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        setupCalendar()
        setupMonthNavigation()
        return binding.root
    }

    /**Displays the current month's calendar.*/
    private fun setupCalendar() {
        val calendarDays = CalendarUtils.generateMonth(
            currentYear,
            currentMonth
        )

        binding.recyclerCalendar.layoutManager =
            GridLayoutManager(requireContext(), 7)

        val adapter = CalendarAdapter(calendarDays)
        binding.recyclerCalendar.adapter = adapter
        adapter.onDateClick = { calendarDay ->
            selectedDate = calendarDay.date
            updateSelectedDateCard()
        }

        updateMonthTitle()
        updateSelectedDateCard()
    }
    /** Updates the selected date card.*/
    private fun updateSelectedDateCard() {

        if (selectedDate == null) {
            binding.includeSelectedDate.txtSelectedDate.text =
                getString(R.string.no_date_selected)
            binding.includeSelectedDate.txtSelectedDateTasks.text =
                getString(R.string.no_tasks_for_selected_date)
            return
        }

        binding.includeSelectedDate.txtSelectedDate.text =
            selectedDate.toString()

        binding.includeSelectedDate.txtSelectedDateTasks.text =
            getString(R.string.no_tasks_for_selected_date)

    }

    /** Updates the month title shown in the calendar header.*/
    private fun updateMonthTitle() {

        val monthName = Month.of(currentMonth)
            .getDisplayName(TextStyle.FULL, Locale.getDefault())

        binding.includeCalendarHeader.txtCurrentMonth.text =
            "$monthName $currentYear"

    }
    /** Handles previous and next month navigation.*/
    private fun setupMonthNavigation() {
        binding.includeCalendarHeader.btnPreviousMonth.setOnClickListener {
            currentMonth--
            if (currentMonth < 1) {
                currentMonth = 12
                currentYear--
            }

            setupCalendar()
        }
        binding.includeCalendarHeader.btnNextMonth.setOnClickListener {
            currentMonth++
            if (currentMonth > 12) {
                currentMonth = 1
                currentYear++
            }
            setupCalendar()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}