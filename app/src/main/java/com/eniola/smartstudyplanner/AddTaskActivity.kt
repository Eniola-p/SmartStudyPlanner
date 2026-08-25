package com.eniola.smartstudyplanner

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputLayout
import com.eniola.smartstudyplanner.utils.TaskPriorityUtils
import java.util.Calendar
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import android.widget.Toast
import com.eniola.smartstudyplanner.utils.ReminderScheduler
import com.eniola.smartstudyplanner.utils.ReminderUtils
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import android.widget.ArrayAdapter

class AddTaskActivity : AppCompatActivity() {
    private val taskRepository = TaskRepository()

    // Stores the actual date selected by the user.
    private var selectedDate: Calendar? = null

    // Stores the actual time selected by the user.
    private var selectedTime: Calendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_add_task)

        // Request notification permission
        if (android.os.Build.VERSION.SDK_INT >= 33) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }

        // Connect views
        val spinnerImportance = findViewById<Spinner>(R.id.spinnerImportance)
        val spinnerUrgency = findViewById<Spinner>(R.id.spinnerUrgency)
        val spinnerReminder = findViewById<Spinner>(R.id.spinnerReminder)
        val txtCalculatedPriority = findViewById<TextView>(R.id.txtCalculatedPriority)
        val imgPriority = findViewById<ImageView>(R.id.imgPriority)
        val edtTaskDate = findViewById<TextView>(R.id.edtTaskDate)
        val edtTaskTime = findViewById<TextView>(R.id.edtTaskTime)
        val layoutTaskDate = findViewById<TextInputLayout>(R.id.layoutTaskDate)
        val layoutTaskTime = findViewById<TextInputLayout>(R.id.layoutTaskTime)
        val edtTaskTitle = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edtTaskTitle)
        val edtTaskCourse = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edtTaskCourse)
        val edtTaskDescription = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edtTaskDescription)

        // Date picker
        edtTaskDate.setOnClickListener {
            showDatePicker(edtTaskDate)
        }

        layoutTaskDate.setEndIconOnClickListener {
            showDatePicker(edtTaskDate)
        }

        // Time picker
        edtTaskTime.setOnClickListener {
            showTimePicker(edtTaskTime)
        }

        layoutTaskTime.setEndIconOnClickListener {
            showTimePicker(edtTaskTime)
        }

        val importanceAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.importance_options,
            R.layout.item_spinner
        )
        importanceAdapter.setDropDownViewResource(
            R.layout.item_spinner
        )
        spinnerImportance.adapter = importanceAdapter

        val urgencyAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.urgency_options,
            R.layout.item_spinner
        )
        urgencyAdapter.setDropDownViewResource(
            R.layout.item_spinner
        )
        spinnerUrgency.adapter = urgencyAdapter

        val reminderAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.reminder_options,
            R.layout.item_spinner
        )
        reminderAdapter.setDropDownViewResource(
            R.layout.item_spinner
        )
        spinnerReminder.adapter = reminderAdapter

        // Priority calculation
        fun updatePriority() {

            val priority = TaskPriorityUtils.calculatePriority(
                spinnerImportance.selectedItemPosition,
                spinnerUrgency.selectedItemPosition
            )

            // Update priority text.
            txtCalculatedPriority.text =
                getString(
                    R.string.priority_format,
                    priority.displayName
                )

            // Use the same circle icon for every priority.
            imgPriority.setImageResource(priority.iconRes)

            // Change the circle color.
            imgPriority.setColorFilter(
                ContextCompat.getColor(
                    this,
                    priority.colorRes
                )
            )
        }

        // Importance selection
        spinnerImportance.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    updatePriority()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                }
            }

        // Urgency selection
        spinnerUrgency.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    updatePriority()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                }
            }

        // Initial priority
        // Both Spinners start at position 0: "Select importance" and "Select urgency".
        // Therefore, the task starts with: No Priority
        updatePriority()
        // Reminder: No reminder is the first option, so position 0 is automatically selected.
        // I use the selected position when the task is saved.



        val btnSaveTask =
            findViewById<com.google.android.material.button.MaterialButton>(
                R.id.btnSaveTask
            )

        val alarmManager =
            getSystemService(ALARM_SERVICE) as android.app.AlarmManager

        btnSaveTask.setOnClickListener {
            // Prevent multiple taps while saving.
            btnSaveTask.isEnabled = false
            btnSaveTask.text = "Saving..."

            val taskTitle = edtTaskTitle.text.toString().trim()

            // Make sure the user entered a task title.
            if (taskTitle.isEmpty()) {
                edtTaskTitle.error = getString(R.string.task_title_required)

                btnSaveTask.isEnabled = true
                btnSaveTask.text = getString(R.string.save_task)

                return@setOnClickListener
            }
            // Make sure a date was selected.
            if (selectedDate == null) {
                Toast.makeText(
                    this,
                    "Please select a date",
                    Toast.LENGTH_SHORT
                ).show()
                btnSaveTask.isEnabled = true
                btnSaveTask.text = getString(R.string.save_task)
                return@setOnClickListener
            }

            // Make sure a time was selected.
            if (selectedTime == null) {
                Toast.makeText(
                    this,
                    "Please select a time",
                    Toast.LENGTH_SHORT
                ).show()

                btnSaveTask.isEnabled = true
                btnSaveTask.text = getString(R.string.save_task)

                return@setOnClickListener
            }

            // Combine the selected date and selected time.
            val taskDateTime =
                Calendar.getInstance().apply {

                    set(
                        Calendar.YEAR,
                        selectedDate!!.get(Calendar.YEAR)
                    )

                    set(
                        Calendar.MONTH,
                        selectedDate!!.get(Calendar.MONTH)
                    )

                    set(
                        Calendar.DAY_OF_MONTH,
                        selectedDate!!.get(Calendar.DAY_OF_MONTH)
                    )

                    set(
                        Calendar.HOUR_OF_DAY,
                        selectedTime!!.get(Calendar.HOUR_OF_DAY)
                    )

                    set(
                        Calendar.MINUTE,
                        selectedTime!!.get(Calendar.MINUTE)
                    )

                    set(Calendar.SECOND, 0)

                    set(Calendar.MILLISECOND, 0)
                }

            // Convert the reminder selection into minutes.
            val reminderMinutes =
                ReminderUtils.getReminderMinutes(
                    spinnerReminder.selectedItemPosition
                )

            if (
                reminderMinutes > 0 &&
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms()
            ) {
                val intent = Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    "package:$packageName".toUri()
                )
                startActivity(intent)
                Toast.makeText(
                    this,
                    "Please allow alarms and reminders for Smart Study Planner.",
                    Toast.LENGTH_LONG
                ).show()

                btnSaveTask.isEnabled = true
                btnSaveTask.text = getString(R.string.save_task)

                return@setOnClickListener
            }

            // Get the selected importance.
            val importance =
                spinnerImportance.selectedItem.toString()

// Get the selected urgency.
            val urgency =
                spinnerUrgency.selectedItem.toString()

// Get the calculated priority.
            val priority =
                TaskPriorityUtils.calculatePriority(
                    spinnerImportance.selectedItemPosition,
                    spinnerUrgency.selectedItemPosition
                )

// Get the selected reminder.
            val reminder =
                spinnerReminder.selectedItem.toString()

// Create the task object.
            val task = Task(
                title = taskTitle,
                course = edtTaskCourse.text.toString().trim(),
                description = edtTaskDescription.text.toString().trim(),

                deadline = com.google.firebase.Timestamp(taskDateTime.time),

                date = edtTaskDate.text.toString(),
                time = edtTaskTime.text.toString(),

                importance = importance,
                urgency = urgency,
                priority = priority.displayName,
                reminder = reminder,

                completed = false
            )

// Save the task to Firestore.
            taskRepository.saveTask(
                task = task,
                onSuccess = {
                    // Schedule the reminder after the task is saved.
                    ReminderScheduler.scheduleReminder(
                        context = this,
                        taskTitle = taskTitle,
                        taskDateTime = taskDateTime,
                        reminderMinutes = reminderMinutes
                    )
                    Toast.makeText(
                        this,
                        "Task saved successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                },

                onError = { exception ->

                    btnSaveTask.isEnabled = true
                    btnSaveTask.text = getString(R.string.save_task)

                    Toast.makeText(
                        this,
                        "Failed to save task: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    // DATE PICKER
    private fun showDatePicker(dateTextView: TextView) {
        val calendar = Calendar.getInstance()

        val year =
            calendar.get(Calendar.YEAR)

        val month =
            calendar.get(Calendar.MONTH)

        val day =
            calendar.get(Calendar.DAY_OF_MONTH)


        val datePicker = DatePickerDialog(
            this,
            R.style.BlueDateTimePicker,
            { _, selectedYear, selectedMonth, selectedDay ->

                // Format date for display.
                val formattedDate = String.format(
                    Locale.getDefault(),
                    "%02d/%02d/%04d",
                    selectedDay,
                    selectedMonth + 1,
                    selectedYear
                )

                // Store the actual selected date.
                this.selectedDate =
                    Calendar.getInstance().apply {

                        set(
                            Calendar.YEAR,
                            selectedYear
                        )

                        set(
                            Calendar.MONTH,
                            selectedMonth
                        )

                        set(
                            Calendar.DAY_OF_MONTH,
                            selectedDay
                        )
                    }

                // Display the selected date.
                dateTextView.text = formattedDate
            },

            year,
            month,
            day
        )
        datePicker.setOnShowListener {

            datePicker.getButton(
                DatePickerDialog.BUTTON_POSITIVE
            ).setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.primary
                )
            )

            datePicker.getButton(
                DatePickerDialog.BUTTON_NEGATIVE
            ).setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.primary
                )
            )
        }

        datePicker.show()

    }
    // TIME PICKER
    private fun showTimePicker(timeTextView: TextView) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val timePicker = TimePickerDialog(
            this,
            R.style.BlueDateTimePicker,
            { _, selectedHour, selectedMinute ->

                // Format time for display.
                val formattedTime = String.format(
                    Locale.getDefault(),
                    "%02d:%02d",
                    selectedHour,
                    selectedMinute
                )

                // Store the actual selected time.
                this.selectedTime =
                    Calendar.getInstance().apply {
                        set(
                            Calendar.HOUR_OF_DAY,
                            selectedHour
                        )
                        set(
                            Calendar.MINUTE,
                            selectedMinute
                        )

                        set(
                            Calendar.SECOND,
                            0
                        )

                        set(
                            Calendar.MILLISECOND,
                            0
                        )
                    }
                // Display the selected time.
                timeTextView.text = formattedTime
            },

            hour,
            minute,
            false
        )

        timePicker.setOnShowListener {

            timePicker.getButton(
                TimePickerDialog.BUTTON_POSITIVE
            ).setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.primary
                )
            )

            timePicker.getButton(
                TimePickerDialog.BUTTON_NEGATIVE
            ).setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.primary
                )
            )
        }

        timePicker.show()
    }
}