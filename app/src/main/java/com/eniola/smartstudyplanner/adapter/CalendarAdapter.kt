package com.eniola.smartstudyplanner.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.eniola.smartstudyplanner.R
import com.eniola.smartstudyplanner.model.CalendarDay

/**
 * CalendarAdapter displays the monthly calendar.
 * It handles:
 * - Today's highlight
 * - Selected date highlight (coming next)
 * - Empty calendar cells
 * - Priority indicators
 */
class CalendarAdapter(
    private val days: List<CalendarDay>
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    /**Called whenever a real calendar day is tapped.*/
    var onDateClick: ((CalendarDay) -> Unit)? = null

     class CalendarViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val txtDay: TextView =
            itemView.findViewById(R.id.txtDay)

        val priorityIndicator: View =
            itemView.findViewById(R.id.viewPriorityIndicator)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CalendarViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.calendar_day_item,
                parent,
                false
            )
        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: CalendarViewHolder,
        position: Int
    ) {

        val calendarDay = days[position]

        // Display the day number.
        holder.txtDay.text =
            calendarDay.day?.toString() ?: ""

        // Reset recycled view.
        holder.txtDay.background = null

        holder.txtDay.setTextColor(
            ContextCompat.getColor(
                holder.itemView.context,
                R.color.primary_text
            )
        )

        // Hide priority indicator for empty cells.
        holder.priorityIndicator.visibility =
            if (calendarDay.day == null)
                View.INVISIBLE
            else
                View.VISIBLE

        // Highlight today's date.
        if (calendarDay.isToday) {
            holder.txtDay.setBackgroundResource(
                R.drawable.bg_today
            )
            holder.txtDay.setTextColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    android.R.color.white
                )
            )
        }

        // Empty cells should not be clickable.
        if (calendarDay.day == null) {
            holder.itemView.isClickable = false
            holder.itemView.isFocusable = false
        }
        else {
            holder.itemView.isClickable = true
            holder.itemView.isFocusable = true
            holder.itemView.setOnClickListener {
                onDateClick?.invoke(calendarDay)
            }
        }
    }
    override fun getItemCount(): Int {
        return days.size
    }
}