
package com.eniola.smartstudyplanner

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.eniola.smartstudyplanner.utils.ReminderUtils
import com.eniola.smartstudyplanner.utils.TaskPriorityUtils
import java.util.Calendar
import java.util.Locale
import com.eniola.smartstudyplanner.utils.ReminderScheduler

/**EditTaskActivity allows students to modify existing study tasks.
  It loads task details from Firestore, updates task information,recalculates priorities, and reschedules reminders after saving.*/
class EditTaskActivity : AppCompatActivity() {

    // Input fields for the task's title, course, and description.
    private lateinit var edtTaskTitle: TextInputEditText
    private lateinit var edtTaskCourse: TextInputEditText
    private lateinit var edtTaskDescription: TextInputEditText

    // Displays the selected task date and time.
    private lateinit var edtTaskDate: TextView
    private lateinit var edtTaskTime: TextView

    // Dropdown menus for importance, urgency, and reminder settings.
    private lateinit var spinnerImportance: Spinner
    private lateinit var spinnerUrgency: Spinner
    private lateinit var spinnerReminder: Spinner

    // Displays the calculated task priority.
    private lateinit var txtCalculatedPriority: TextView

    // Stores the task's selected and original scheduling details.
    private var selectedDate: Calendar? = null
    private var selectedTime: Calendar? = null
    private var originalCompleted = false
    private var originalDeadline: Calendar? = null
    private var taskId: String = ""
    private var originalReminderMinutes = 0

    // Provides access to the Firestore database.
    private val firestore =
        FirebaseFirestore.getInstance()

    // Initializes the task editing screen and loads the selected task.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_add_task)

        // Retrieve the ID of the task being edited.
        taskId =
            intent.getStringExtra("taskId") ?: ""

        if (taskId.isBlank()) {
            Toast.makeText(
                this,
                "Task not found",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        connectViews()
        setupSpinners()
        setupDateAndTimePickers()
        setupPriorityCalculation()
        setupSaveButton()
        loadTask()
    }

    // Connects the task editing fields to their XML layout components.
    private fun connectViews() {

        edtTaskTitle =
            findViewById(R.id.edtTaskTitle)

        edtTaskCourse =
            findViewById(R.id.edtTaskCourse)

        edtTaskDescription =
            findViewById(R.id.edtTaskDescription)

        edtTaskDate =
            findViewById(R.id.edtTaskDate)

        edtTaskTime =
            findViewById(R.id.edtTaskTime)

        spinnerImportance =
            findViewById(R.id.spinnerImportance)

        spinnerUrgency =
            findViewById(R.id.spinnerUrgency)

        spinnerReminder =
            findViewById(R.id.spinnerReminder)

        txtCalculatedPriority =
            findViewById(R.id.txtCalculatedPriority)
    }

    // Configures the dropdown menus for task importance, urgency, and reminders.
    private fun setupSpinners() {

        val importanceAdapter =
            ArrayAdapter.createFromResource(
                this,
                R.array.importance_options,
                R.layout.item_spinner
            )

        importanceAdapter.setDropDownViewResource(
            R.layout.item_spinner
        )

        spinnerImportance.adapter =
            importanceAdapter

        val urgencyAdapter =
            ArrayAdapter.createFromResource(
                this,
                R.array.urgency_options,
                R.layout.item_spinner
            )

        urgencyAdapter.setDropDownViewResource(
            R.layout.item_spinner
        )

        spinnerUrgency.adapter =
            urgencyAdapter

        val reminderAdapter =
            ArrayAdapter.createFromResource(
                this,
                R.array.reminder_options,
                R.layout.item_spinner
            )

        reminderAdapter.setDropDownViewResource(
            R.layout.item_spinner
        )

        spinnerReminder.adapter =
            reminderAdapter
    }

    // Enables date and time selection through their respective picker dialogs.
    private fun setupDateAndTimePickers() {

        val layoutTaskDate =
            findViewById<TextInputLayout>(
                R.id.layoutTaskDate
            )

        val layoutTaskTime =
            findViewById<TextInputLayout>(
                R.id.layoutTaskTime
            )

        edtTaskDate.setOnClickListener {
            showDatePicker()
        }

        layoutTaskDate.setEndIconOnClickListener {
            showDatePicker()
        }

        edtTaskTime.setOnClickListener {
            showTimePicker()
        }

        layoutTaskTime.setEndIconOnClickListener {
            showTimePicker()
        }
    }

    // Automatically updates task priority when importance or urgency changes.
    private fun setupPriorityCalculation() {

        // Calculates and displays the priority based on selected values.
        fun updatePriority() {

            val priority =
                TaskPriorityUtils.calculatePriority(
                    spinnerImportance.selectedItemPosition,
                    spinnerUrgency.selectedItemPosition
                )

            txtCalculatedPriority.text =
                getString(
                    R.string.priority_format,
                    priority.displayName
                )
        }

        // Recalculate priority when the importance selection changes.
        spinnerImportance.onItemSelectedListener =
            object :
                android.widget.AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    updatePriority()
                }

                override fun onNothingSelected(
                    parent: android.widget.AdapterView<*>?
                ) {}
            }

        // Recalculate priority when the urgency selection changes.
        spinnerUrgency.onItemSelectedListener =
            object :
                android.widget.AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    updatePriority()
                }

                override fun onNothingSelected(
                    parent: android.widget.AdapterView<*>?
                ) {}
            }
    }

    // Validates and saves task changes, then updates the scheduled reminders.
    private fun setupSaveButton() {

        val btnSaveTask =
            findViewById<MaterialButton>(R.id.btnSaveTask)

        btnSaveTask.text = "Save Changes"

        btnSaveTask.setOnClickListener {

            val title =
                edtTaskTitle.text.toString().trim()

            // Ensure the task has a title.
            if (title.isBlank()) {

                edtTaskTitle.error =
                    "Task title is required"

                return@setOnClickListener
            }

            // Ensure the task has a selected date.
            if (selectedDate == null) {

                Toast.makeText(
                    this,
                    "Please select a date",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // Ensure the task has a selected time.
            if (selectedTime == null) {

                Toast.makeText(
                    this,
                    "Please select a time",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // Prevent repeated submissions while saving.
            btnSaveTask.isEnabled = false
            btnSaveTask.text = "Saving..."

            // Combine the selected date and time into one deadline.
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

            // Retrieve the updated importance and urgency levels.
            val importance =
                spinnerImportance.selectedItem.toString()

            val urgency =
                spinnerUrgency.selectedItem.toString()

            // Calculate the new task priority.
            val priority =
                TaskPriorityUtils.calculatePriority(
                    spinnerImportance.selectedItemPosition,
                    spinnerUrgency.selectedItemPosition
                )

            // Retrieve the selected reminder and its interval in minutes.
            val reminder =
                spinnerReminder.selectedItem.toString()

            val reminderMinutes =
                ReminderUtils.getReminderMinutes(
                    spinnerReminder.selectedItemPosition
                )

            // Create the updated task while preserving its ID and completion status.
            val updatedTask =
                Task(
                    id = taskId,

                    title = title,

                    course =
                        edtTaskCourse.text
                            .toString()
                            .trim(),

                    description =
                        edtTaskDescription.text
                            .toString()
                            .trim(),

                    deadline =
                        Timestamp(
                            taskDateTime.time
                        ),

                    date =
                        edtTaskDate.text
                            .toString(),

                    time =
                        edtTaskTime.text
                            .toString(),

                    importance = importance,

                    urgency = urgency,

                    priority =
                        priority.displayName,

                    reminder = reminder,

                    completed = originalCompleted
                )

            // Confirm that the student is authenticated before saving.
            val user =
                FirebaseAuth.getInstance().currentUser

            if (user == null) {

                Toast.makeText(
                    this,
                    "Please sign in again",
                    Toast.LENGTH_SHORT
                ).show()

                btnSaveTask.isEnabled = true
                btnSaveTask.text = "Save Changes"

                return@setOnClickListener
            }

            // Save the updated task to the student's Firestore collection.
            firestore
                .collection("users")
                .document(user.uid)
                .collection("tasks")
                .document(taskId)
                .set(updatedTask)
                .addOnSuccessListener {

                    // Cancel the old reminder first.
                    originalDeadline?.let { oldDeadline ->

                        ReminderScheduler.cancelReminder(
                            context = this,
                            taskDateTime = oldDeadline,
                            reminderMinutes = originalReminderMinutes
                        )
                    }

                    // Schedule the new reminder.
                    ReminderScheduler.scheduleReminder(
                        context = this,
                        taskTitle = title,
                        taskDateTime = taskDateTime,
                        reminderMinutes = reminderMinutes
                    )

                    Toast.makeText(
                        this,
                        "Task updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                }
                .addOnFailureListener { exception ->

                    // Restore the save button if the update fails.
                    btnSaveTask.isEnabled = true
                    btnSaveTask.text = "Save Changes"

                    Toast.makeText(
                        this,
                        "Failed to update task: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    // Loads the selected task from Firestore for editing.
    private fun loadTask() {

        val user =
            FirebaseAuth.getInstance().currentUser

        // Ensure the student is signed in.
        if (user == null) {
            Toast.makeText(
                this,
                "Please sign in again",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        firestore
            .collection("users")
            .document(user.uid)
            .collection("tasks")
            .document(taskId)
            .get()
            .addOnSuccessListener { document ->

                // Stop if the requested task no longer exists.
                if (!document.exists()) {

                    Toast.makeText(
                        this,
                        "Task no longer exists",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                    return@addOnSuccessListener
                }

                val task =
                    document.toObject(Task::class.java)

                if (task == null) {
                    finish()
                    return@addOnSuccessListener
                }

                // Populate the editing fields with the retrieved task.
                fillTaskForm(task)
            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Failed to load task",
                    Toast.LENGTH_SHORT
                ).show()

                finish()
            }
    }

    // Fills the editing form with existing task details and reminder settings.
    private fun fillTaskForm(task: Task) {
        originalCompleted = task.completed

        // Store the original deadline for cancelling previous reminders.
        originalDeadline =
            task.deadline?.toDate()?.let { date ->
                Calendar.getInstance().apply {
                    time = date
                }
            }

        // Convert the saved reminder option into its interval in minutes.
        originalReminderMinutes = when (task.reminder) {
            getString(R.string.five_minutes_before) -> 5
            getString(R.string.fifteen_minutes_before) -> 15
            getString(R.string.thirty_minutes_before) -> 30
            getString(R.string.one_hour_before) -> 60
            getString(R.string.one_day_before) -> 1440
            else -> 0
        }

        // Display the task's existing information.
        edtTaskTitle.setText(task.title)
        edtTaskCourse.setText(task.course)
        edtTaskDescription.setText(task.description)

        edtTaskDate.text = task.date
        edtTaskTime.text = task.time

        // Restore the task's previously selected date and time.
        selectedDate =
            task.deadline?.toDate()?.let { date ->

                Calendar.getInstance().apply {
                    time = date
                }
            }

        selectedTime =
            task.deadline?.toDate()?.let { date ->

                Calendar.getInstance().apply {
                    time = date
                }
            }

        // Restore the saved importance, urgency, and reminder selections.
        setSpinnerValue(
            spinnerImportance,
            task.importance
        )

        setSpinnerValue(
            spinnerUrgency,
            task.urgency
        )

        setSpinnerValue(
            spinnerReminder,
            task.reminder
        )
    }

    // Selects the matching saved value in a dropdown menu.
    private fun setSpinnerValue(
        spinner: Spinner,
        value: String
    ) {

        for (i in 0 until spinner.adapter.count) {

            if (
                spinner.adapter
                    .getItem(i)
                    ?.toString()
                    ?.equals(
                        value,
                        ignoreCase = true
                    ) == true
            ) {

                spinner.setSelection(i)
                return
            }
        }
    }

    // Opens the date picker and updates the task's selected date.
    private fun showDatePicker() {

        val calendar =
            selectedDate ?: Calendar.getInstance()

        val datePicker = DatePickerDialog(
            this,
            R.style.BlueDateTimePicker,
            { _, year, month, day ->

                edtTaskDate.text =
                    String.format(
                        Locale.getDefault(),
                        "%02d/%02d/%04d",
                        day,
                        month + 1,
                        year
                    )

                selectedDate =
                    Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, day)
                    }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Apply the application's primary colour to the date picker buttons.
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

    // Opens the time picker and updates the task's selected time.
    private fun showTimePicker() {

        val calendar =
            selectedTime ?: Calendar.getInstance()

        val timePicker = TimePickerDialog(
            this,
            R.style.BlueDateTimePicker,
            { _, hour, minute ->

                edtTaskTime.text =
                    String.format(
                        Locale.getDefault(),
                        "%02d:%02d",
                        hour,
                        minute
                    )

                selectedTime =
                    Calendar.getInstance().apply {

                        set(
                            Calendar.HOUR_OF_DAY,
                            hour
                        )

                        set(
                            Calendar.MINUTE,
                            minute
                        )

                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        )

        // Apply the application's primary colour to the time picker buttons.
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
