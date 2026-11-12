
package com.eniola.smartstudyplanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import androidx.core.content.ContextCompat

/** StatisticsFragment displays the student's study task statistics.
 * It calculates completion rates for selected weekly or monthly periodsand shows a four-week progress trend using a line chart.*/
class StatisticsFragment : Fragment() {

    /** Initializes the statistics screen, period selector, and progress chart. */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(
            R.layout.fragment_statistics,
            container,
            false
        )

        // Set up the statistics period selector.
        setupPeriodSpinner(view)

        // Load statistics for the default period.
        loadStatistics(view, 0)

        // Load the weekly progress trend.
        loadWeeklyProgressTrend(view)

        return view
    }

    /** Configures the period dropdown and reloads statistics when the selection changes. */
    private fun setupPeriodSpinner(view: View) {

        val spinner =
            view.findViewById<Spinner>(
                R.id.spinnerStatisticsPeriod
            )

        // Load the available statistics periods.
        val adapter =
            ArrayAdapter.createFromResource(
                requireContext(),
                R.array.statistics_periods,
                android.R.layout.simple_spinner_item
            )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinner.adapter = adapter

        // Reload statistics whenever the selected period changes.
        spinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    selectedView: View?,
                    position: Int,
                    id: Long
                ) {

                    loadStatistics(
                        view,
                        position
                    )
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                    // Keep the current statistics.
                }
            }
    }

    /** Retrieves tasks and calculates completion statistics for the selected period. */
    private fun loadStatistics(
        view: View,
        period: Int
    ) {

        val user =
            FirebaseAuth.getInstance().currentUser
                ?: return

        // Connect the statistics display components.
        val txtPercent =
            view.findViewById<TextView>(
                R.id.txtStatisticsPercent
            )

        val txtTotal =
            view.findViewById<TextView>(
                R.id.txtStatisticsTotal
            )

        val txtCompleted =
            view.findViewById<TextView>(
                R.id.txtStatisticsCompleted
            )

        val txtPending =
            view.findViewById<TextView>(
                R.id.txtStatisticsPending
            )

        val txtMissed =
            view.findViewById<TextView>(
                R.id.txtStatisticsMissed
            )

        // Determine the beginning and end of the selected period.
        val calendar =
            Calendar.getInstance()

        val startTime: Long
        val endTime: Long

        when (period) {

            0 -> {
                // Current week: Monday 12:00 AM to next Monday 12:00 AM.
                calendar.set(
                    Calendar.DAY_OF_WEEK,
                    Calendar.MONDAY
                )

                calendar.set(
                    Calendar.HOUR_OF_DAY,
                    0
                )

                calendar.set(
                    Calendar.MINUTE,
                    0
                )

                calendar.set(
                    Calendar.SECOND,
                    0
                )

                calendar.set(
                    Calendar.MILLISECOND,
                    0
                )

                startTime =
                    calendar.timeInMillis

                calendar.add(
                    Calendar.DAY_OF_MONTH,
                    7
                )

                endTime =
                    calendar.timeInMillis
            }

            1 -> {
                // Previous week: the Monday before the current week
                // through the start of the current week.
                calendar.set(
                    Calendar.DAY_OF_WEEK,
                    Calendar.MONDAY
                )

                calendar.set(
                    Calendar.HOUR_OF_DAY,
                    0
                )

                calendar.set(
                    Calendar.MINUTE,
                    0
                )

                calendar.set(
                    Calendar.SECOND,
                    0
                )

                calendar.set(
                    Calendar.MILLISECOND,
                    0
                )

                calendar.add(
                    Calendar.DAY_OF_MONTH,
                    -7
                )

                startTime =
                    calendar.timeInMillis

                calendar.add(
                    Calendar.DAY_OF_MONTH,
                    7
                )

                endTime =
                    calendar.timeInMillis
            }

            2 -> {
                // Current month: first day of this month
                // through the first day of next month.
                calendar.set(
                    Calendar.DAY_OF_MONTH,
                    1
                )

                calendar.set(
                    Calendar.HOUR_OF_DAY,
                    0
                )

                calendar.set(
                    Calendar.MINUTE,
                    0
                )

                calendar.set(
                    Calendar.SECOND,
                    0
                )

                calendar.set(
                    Calendar.MILLISECOND,
                    0
                )

                startTime =
                    calendar.timeInMillis

                calendar.add(
                    Calendar.MONTH,
                    1
                )

                endTime =
                    calendar.timeInMillis
            }

            else -> {
                // Previous month: first day of the previous month
                // through the first day of the current month.
                calendar.set(
                    Calendar.DAY_OF_MONTH,
                    1
                )

                calendar.set(
                    Calendar.HOUR_OF_DAY,
                    0
                )

                calendar.set(
                    Calendar.MINUTE,
                    0
                )

                calendar.set(
                    Calendar.SECOND,
                    0
                )

                calendar.set(
                    Calendar.MILLISECOND,
                    0
                )

                calendar.add(
                    Calendar.MONTH,
                    -1
                )

                startTime =
                    calendar.timeInMillis

                calendar.add(
                    Calendar.MONTH,
                    1
                )

                endTime =
                    calendar.timeInMillis
            }
        }

        // Retrieve the student's task records from Firestore.
        FirebaseFirestore
            .getInstance()
            .collection("users")
            .document(user.uid)
            .collection("tasks")
            .get()
            .addOnSuccessListener { documents ->

                if (!isAdded) {
                    return@addOnSuccessListener
                }

                // Keep only tasks whose deadlines fall within
                // the selected statistics period.
                val selectedTasks =
                    documents
                        .map {
                            it.toObject(Task::class.java)
                        }
                        .filter { task ->

                            val deadline =
                                task.deadline
                                    ?.toDate()
                                    ?.time
                                    ?: return@filter false

                            deadline >= startTime &&
                                    deadline < endTime
                        }

                val total =
                    selectedTasks.size

                // Count completed tasks.
                val completed =
                    selectedTasks.count {
                        it.completed
                    }

                // Count incomplete tasks whose deadlines have passed.
                val missed =
                    selectedTasks.count { task ->

                        !task.completed &&
                                task.deadline
                                    ?.toDate()
                                    ?.time
                                    ?.let {
                                        it < System.currentTimeMillis()
                                    } == true
                    }

                // Remaining incomplete tasks are still pending.
                val pending =
                    total -
                            completed -
                            missed

                // Calculate the completion percentage.
                val percentage =
                    if (total > 0) {
                        (completed * 100) / total
                    } else {
                        0
                    }

                // Display the calculated progress and task statistics.
                txtPercent.text =
                    "$percentage%"

                txtTotal.text =
                    "Total Tasks: $total"

                txtCompleted.text =
                    "Completed: $completed"

                txtPending.text =
                    "Pending: $pending"

                txtMissed.text =
                    "Missed: $missed"
            }
    }

    /** Generates a line chart showing task completion rates over four weeks. */
    private fun loadWeeklyProgressTrend(view: View) {

        val user =
            FirebaseAuth.getInstance().currentUser
                ?: return

        val chart =
            view.findViewById<com.github.mikephil.charting.charts.LineChart>(
                R.id.chartProgressTrend
            )

        val calendar =
            Calendar.getInstance()

        // Start of the current week: Monday at 12:00 AM.
        calendar.set(
            Calendar.DAY_OF_WEEK,
            Calendar.MONDAY
        )

        calendar.set(
            Calendar.HOUR_OF_DAY,
            0
        )

        calendar.set(
            Calendar.MINUTE,
            0
        )

        calendar.set(
            Calendar.SECOND,
            0
        )

        calendar.set(
            Calendar.MILLISECOND,
            0
        )

        // Move back three weeks so the graph contains four weeks.
        calendar.add(
            Calendar.DAY_OF_MONTH,
            -21
        )

        val firstWeekStart =
            calendar.timeInMillis

        // Retrieve the student's tasks to calculate weekly completion rates.
        FirebaseFirestore
            .getInstance()
            .collection("users")
            .document(user.uid)
            .collection("tasks")
            .get()
            .addOnSuccessListener { documents ->

                if (!isAdded || view != this.view) {
                    return@addOnSuccessListener
                }

                val tasks =
                    documents.map {
                        it.toObject(Task::class.java)
                    }

                val entries =
                    ArrayList<Entry>()

                val weekLabels =
                    ArrayList<String>()

                // Calculate completion rate for each of the four weeks.
                for (weekIndex in 0 until 4) {

                    val weekCalendar =
                        Calendar.getInstance().apply {
                            timeInMillis =
                                firstWeekStart

                            add(
                                Calendar.DAY_OF_MONTH,
                                weekIndex * 7
                            )
                        }

                    val weekStart =
                        weekCalendar.timeInMillis

                    weekCalendar.add(
                        Calendar.DAY_OF_MONTH,
                        7
                    )

                    val weekEnd =
                        weekCalendar.timeInMillis

                    // Filter tasks scheduled within the current chart week.
                    val weeklyTasks =
                        tasks.filter { task ->

                            val deadline =
                                task.deadline
                                    ?.toDate()
                                    ?.time
                                    ?: return@filter false

                            deadline >= weekStart &&
                                    deadline < weekEnd
                        }

                    val totalTasks =
                        weeklyTasks.size

                    val completedTasks =
                        weeklyTasks.count {
                            it.completed
                        }

                    // Calculate the percentage of completed tasks for the week.
                    val percentage =
                        if (totalTasks > 0) {
                            (completedTasks * 100f) /
                                    totalTasks
                        } else {
                            0f
                        }

                    // Add the week's completion percentage to the chart data.
                    entries.add(
                        Entry(
                            weekIndex.toFloat(),
                            percentage
                        )
                    )

                    weekLabels.add(
                        "Week ${weekIndex + 1}"
                    )
                }

                // Configure the chart's line appearance and data labels.
                val dataSet =
                    LineDataSet(
                        entries,
                        "Completion Rate"
                    ).apply {

                        lineWidth = 3f
                        circleRadius = 5f
                        setDrawValues(true)
                        valueTextSize = 12f
                        color =
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.primary
                            )

                        setCircleColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.primary
                            )
                        )
                    }

                // Apply the completion data and weekly labels to the chart.
                chart.data =
                    LineData(dataSet)

                chart.xAxis.valueFormatter =
                    IndexAxisValueFormatter(
                        weekLabels
                    )

                chart.xAxis.granularity = 1f
                chart.xAxis.setDrawGridLines(false)

                // Set the completion percentage scale from 0 to 100.
                chart.axisLeft.axisMinimum = 0f
                chart.axisLeft.axisMaximum = 100f
                chart.axisLeft.granularity = 20f

                chart.axisRight.isEnabled = false

                chart.description.isEnabled = false

                chart.legend.isEnabled = true

                // Refresh the chart to display the calculated progress trend.
                chart.invalidate()
            }
    }
}