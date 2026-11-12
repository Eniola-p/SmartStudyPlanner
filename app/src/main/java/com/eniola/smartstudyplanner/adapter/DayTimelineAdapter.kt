
package com.eniola.smartstudyplanner.adapter

import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.eniola.smartstudyplanner.R
import com.eniola.smartstudyplanner.Task
import java.text.SimpleDateFormat
import androidx.fragment.app.FragmentActivity
import java.util.Locale
import android.os.Bundle
import com.eniola.smartstudyplanner.TaskDetailsFragment

/**
 * DayTimelineAdapter displays daily scheduled tasks grouped by hour.
 * It creates task cards showing time, title, course, and priority,
 * colours timeline indicators, and opens task details when selected.
 */
class DayTimelineAdapter(
    private val timelineItems: List<DayTimelineItem>
) : RecyclerView.Adapter<DayTimelineAdapter.DayTimelineViewHolder>() {
    
    /** Holds the time label, timeline indicator, and task container for each hour. */
    class DayTimelineViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val timelineTime: TextView = itemView.findViewById(R.id.txtTimelineTime)
        val timelineDot: View = itemView.findViewById(R.id.viewTimelineDot)
        val taskContainer: LinearLayout = itemView.findViewById(R.id.timelineTaskContainer)
    }

    /** Creates the layout used to display each hourly timeline item. */
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DayTimelineViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(
                R.layout.calendar_day_timeline_item,
                parent,
                false
            )

        return DayTimelineViewHolder(view)
    }

    /** Displays tasks for each hour and applies their priority colours and click actions. */
    override fun onBindViewHolder(
        holder: DayTimelineViewHolder,
        position: Int
    ) {
        val item = timelineItems[position]
        val context = holder.itemView.context
        holder.timelineTime.text = item.timeLabel

        // Clear cards from recycled rows.
        holder.taskContainer.removeAllViews()

        if (item.tasks.isEmpty()) {
            holder.timelineDot.visibility = View.INVISIBLE
            return
        }

        holder.timelineDot.visibility = View.VISIBLE

        // Add every task belonging to this hour.
        item.tasks.forEach { task ->

            // Creates a clickable card for the current task.
            val taskCard = MaterialCardView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 8.dp(context)
                }

                radius = 12.dp(context).toFloat()
                cardElevation = 0f

                setCardBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        R.color.card_background
                    )
                )
                strokeWidth = 1.dp(context)
                strokeColor =
                    ContextCompat.getColor(
                        context,
                        R.color.divider
                    )
                isClickable = true
                isFocusable = true

                // Opens the selected task's details using its unique ID.
                setOnClickListener {
                    val detailsFragment =
                        TaskDetailsFragment().apply {
                            arguments = Bundle().apply {
                                putString("taskId", task.id)
                            }
                        }

                    (context as? FragmentActivity)
                        ?.supportFragmentManager
                        ?.beginTransaction()
                        ?.replace(
                            R.id.fragmentContainer,
                            detailsFragment
                        )
                        ?.addToBackStack(null)
                        ?.commit()
                }
            }

            // Arranges the task information vertically inside the card.
            val content = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(
                    12.dp(context),
                    10.dp(context),
                    12.dp(context),
                    10.dp(context)
                )
            }

            // Displays the task's scheduled time.
            val taskTime = TextView(context).apply {
                text = getTaskTime(task)

                setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.primary
                    )
                )

                textSize = 11f

                typeface =
                    Typeface.create(
                        "sans",
                        Typeface.BOLD
                    )
            }

            // Displays the task title.
            val taskTitle = TextView(context).apply {
                text = task.title

                setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.primary_text
                    )
                )

                textSize = 14f

                typeface =
                    Typeface.create(
                        "sans",
                        Typeface.BOLD
                    )

                gravity = Gravity.START
                setPadding(0, 3.dp(context), 0, 0)
            }

            // Displays the course associated with the task.
            val taskCourse = TextView(context).apply {
                text = task.course

                setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.secondary_text
                    )
                )

                textSize = 12f

                setPadding(0, 3.dp(context), 0, 0)
            }

            // Displays the task priority using its assigned colour.
            val taskPriority = TextView(context).apply {
                text = task.priority

                textSize = 11f

                setPadding(0, 5.dp(context), 0, 0)

                setTextColor(
                    ContextCompat.getColor(
                        context,
                        getPriorityColor(task.priority)
                    )
                )
            }

            // Adds all task information to the card and timeline.
            content.addView(taskTime)
            content.addView(taskTitle)
            content.addView(taskCourse)
            content.addView(taskPriority)

            taskCard.addView(content)

            holder.taskContainer.addView(taskCard)
        }

        // Use the highest priority task for the timeline dot.
        val highestPriorityTask =
            item.tasks.maxByOrNull {
                getPriorityValue(it.priority)
            }

        highestPriorityTask?.let { task ->
            holder.timelineDot.backgroundTintList =
                ContextCompat.getColorStateList(
                    context,
                    getPriorityColor(task.priority)
                )
        }
    }

    /** Formats the task deadline as time or returns its stored time. */
    private fun getTaskTime(task: Task): String {

        val deadline = task.deadline?.toDate()
            ?: return task.time

        return SimpleDateFormat(
            "h:mm a",
            Locale.getDefault()
        ).format(deadline)
    }

    /** Returns the colour resource assigned to each priority level. */
    private fun getPriorityColor(priority: String): Int {

        return when (priority) {

            "High" ->
                R.color.priority_red

            "Medium" ->
                R.color.priority_orange

            "Normal" ->
                R.color.priority_yellow

            "Low" ->
                R.color.priority_green

            else ->
                R.color.secondary_text
        }
    }

    /** Assigns numerical values to priorities for comparing task importance. */
    private fun getPriorityValue(priority: String): Int {

        return when (priority) {

            "High" -> 4
            "Medium" -> 3
            "Normal" -> 2
            "Low" -> 1
            else -> 0
        }
    }

    /** Returns the total number of hourly timeline items. */
    override fun getItemCount(): Int =
        timelineItems.size

    /** Converts dp measurements into pixels based on screen density. */
    private fun Int.dp(context: android.content.Context): Int =
        (this * context.resources.displayMetrics.density).toInt()
}

/**
 * Represents one hour on the daily timeline.
 * Multiple tasks can belong to the same hour.
 */
data class DayTimelineItem(
    val timeLabel: String,
    val tasks: List<Task> = emptyList()
)
