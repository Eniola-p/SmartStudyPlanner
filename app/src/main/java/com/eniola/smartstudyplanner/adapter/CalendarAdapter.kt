package com.eniola.smartstudyplanner.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.eniola.smartstudyplanner.R
import com.eniola.smartstudyplanner.model.CalendarDay

/**CalendarAdapter displays the days in the monthly calendar grid. It handles:
  - Today's highlight
  - Priority indicators for dates with tasks
  - Priority indicator colOrs
  - White date cell backgrounds
  - Empty calendar cells
  - Date selection */
class CalendarAdapter(
    private val days: List<CalendarDay>
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    /** Called whenever a real calendar day is tapped. */
    var onDateClick: ((CalendarDay) -> Unit)? = null

    class CalendarViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val txtDay: TextView = itemView.findViewById(R.id.txtDay)
        val priorityIndicator: View = itemView.findViewById(R.id.viewPriorityIndicator)
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
        val context = holder.itemView.context
        holder.txtDay.text = calendarDay.day?.toString() ?: ""

        // Default appearance
        holder.txtDay.background = null
        holder.itemView.setBackgroundColor(
            ContextCompat.getColor(context, R.color.background)
        )
        holder.txtDay.setTextColor(
            ContextCompat.getColor(context, R.color.primary_text)
        )

        // Priority indicator
        if (calendarDay.day == null || calendarDay.taskCount == 0) {
            holder.priorityIndicator.visibility = View.INVISIBLE
        } else {
            holder.priorityIndicator.visibility = View.VISIBLE
            holder.priorityIndicator.backgroundTintList =
                ContextCompat.getColorStateList(
                    context,
                    calendarDay.priorityLevel.colorRes
                )
        }

        // Highlight today's date with an outline
        if (calendarDay.isToday) {
            holder.txtDay.setBackgroundResource(
                R.drawable.bg_today_outline
            )
        }

    // Highlight today's date with an outline
        if (calendarDay.isToday) {
            holder.txtDay.setBackgroundResource(
                R.drawable.bg_today_outline
            )
        }

    // Highlight the selected date
        if (calendarDay.isSelected) {
            holder.itemView.setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.calendar_selected
                )
            )
            holder.txtDay.background = null
            holder.txtDay.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.primary_dark
                )
            )
        }

        // Empty calendar cells cannot be clicked
        if (calendarDay.day == null) {
            holder.itemView.isClickable = false
            holder.itemView.isFocusable = false
            holder.itemView.setOnClickListener(null)
        } else {
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