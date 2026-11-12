package com.eniola.smartstudyplanner.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.eniola.smartstudyplanner.R
import com.eniola.smartstudyplanner.model.CalendarDay
import java.time.format.TextStyle
import java.util.Locale

/**WeekCalendarAdapter displays the seven days of the weekly calendar.
   It shows day names, dates, task priority indicators, highlights
   the selected date, and handles date selection. */
class WeekCalendarAdapter(
    private val days: List<CalendarDay>,
    private val selectedDate: java.time.LocalDate?
) : RecyclerView.Adapter<WeekCalendarAdapter.WeekDayViewHolder>() {

    // Handles user clicks on a calendar date.
    var onDateClick: ((CalendarDay) -> Unit)? = null

    /** Holds the day name, date number, and task indicator views. */
    class WeekDayViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val dayName: TextView =
            itemView.findViewById(R.id.txtWeekDayName)

        val dayNumber: TextView =
            itemView.findViewById(R.id.txtWeekDayNumber)

        val taskIndicator: View =
            itemView.findViewById(R.id.viewWeekTaskIndicator)
    }

    /** Creates the layout for each day displayed in the weekly calendar. */
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): WeekDayViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.calendar_week_day_item,
                parent,
                false
            )

        return WeekDayViewHolder(view)
    }

    /** Displays each day's information, task indicators, and selection highlight. */
    override fun onBindViewHolder(
        holder: WeekDayViewHolder,
        position: Int
    ) {

        val calendarDay = days[position]
        val context = holder.itemView.context

        val date = calendarDay.date ?: return

        // Display the day name.
        holder.dayName.text =
            date.dayOfWeek.getDisplayName(
                TextStyle.SHORT,
                Locale.getDefault()
            ).uppercase()

        // Display the day number.
        holder.dayNumber.text =
            date.dayOfMonth.toString()

        // Show the indicator only when the date has tasks.
        if (calendarDay.taskCount > 0) {

            holder.taskIndicator.visibility = View.VISIBLE

            // Set indicator colour based on the highest task priority.
            holder.taskIndicator.backgroundTintList =
                ContextCompat.getColorStateList(
                    context,
                    calendarDay.priorityLevel.colorRes
                )

        } else {

            holder.taskIndicator.visibility = View.INVISIBLE
        }

        // Use a light blue background for the selected day.
        if (selectedDate == date) {

            holder.itemView.setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.calendar_selected
                )
            )

            holder.dayName.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.primary_dark
                )
            )

            holder.dayNumber.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.primary_dark
                )
            )

        } else {

            // Restore the default background and text colours.
            holder.itemView.setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.background
                )
            )

            holder.dayName.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.secondary_text
                )
            )

            holder.dayNumber.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.primary_text
                )
            )
        }

        // Make the day clickable.
        holder.itemView.setOnClickListener {
            onDateClick?.invoke(calendarDay)
        }
    }

    /** Returns the total number of days in the weekly calendar list. */
    override fun getItemCount(): Int {
        return days.size
    }
}
