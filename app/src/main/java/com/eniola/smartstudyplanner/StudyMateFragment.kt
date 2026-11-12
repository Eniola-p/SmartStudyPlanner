
package com.eniola.smartstudyplanner

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Spanned
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.eniola.smartstudyplanner.ai.GeminiService
import com.eniola.smartstudyplanner.ai.StudyMatePrompt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** StudyMateFragment provides the student-facing AI study assistant.
 * It manages conversations, generates contextual responses, and lets students review and confirm AI-suggested schedule changes.*/
class StudyMateFragment : Fragment() {

    /** Stores a message used as context for the current AI conversation. */
    private data class ConversationMessage(
        val role: String,
        val text: String
    )

    private val conversationHistory =
        mutableListOf<ConversationMessage>()

    private var studyMateInput: EditText? = null

    // Uses MainActivity to keep the active chat while the app is alive.
    private var currentChatId: String?
        get() = (activity as? MainActivity)?.activeStudyMateChatId
        set(value) {
            (activity as? MainActivity)?.activeStudyMateChatId = value
        }

    // Prevents multiple chats from being created at the same time.
    private var creatingChat = false

    /** Inflates the StudyMate chat interface. */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_study_mate,
            container,
            false
        )
    }

    /** Configures chat controls, conversation history, and keyboard handling. */
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.showStudyMateMode()
        val header =
            view.findViewById<View>(R.id.studyMateHeader)

        val inputBar =
            view.findViewById<View>(R.id.studyMateInputBar)

        val questionInput =
            view.findViewById<EditText>(
                R.id.etStudyMateQuestion
            )

        studyMateInput = questionInput

        val sendButton =
            view.findViewById<View>(
                R.id.btnSendQuestion
            )

        // StudyMate chat history views.
        val studyMateMenu =
            view.findViewById<ImageButton>(
                R.id.btnStudyMateMenu
            )

        val studyMateDrawer =
            view.findViewById<LinearLayout>(
                R.id.studyMateDrawer
            )
        val studyMateDrawerOverlay =
            view.findViewById<View>(
                R.id.studyMateDrawerOverlay
            )

        // Closes the drawer when the chat area is tapped.
        view.findViewById<LinearLayout>(
            R.id.chatContainer
        ).setOnClickListener {

            studyMateDrawer.visibility =
                View.GONE
        }

        val closeStudyMateDrawer =
            view.findViewById<ImageButton>(
                R.id.btnCloseStudyMateDrawer
            )

        val newChatButton =
            view.findViewById<LinearLayout>(
                R.id.btnDrawerNewChat
            )

        val recentChatsContainer =
            view.findViewById<LinearLayout>(
                R.id.recentChatsContainer
            )

        // Loads the user's previous StudyMate conversations.
        loadRecentChats(
            recentChatsContainer,
            studyMateDrawer
        )

        // Restores the active conversation when returning to StudyMate.
        val activeChatId =
            (activity as? MainActivity)?.activeStudyMateChatId

        if (activeChatId != null) {

            // Hide the new-chat screen immediately.
            view.findViewById<View>(
                R.id.studyMateWelcome
            ).visibility = View.GONE

            view.findViewById<LinearLayout>(
                R.id.chatContainer
            ).visibility = View.VISIBLE

            loadSelectedChat(
                activeChatId,
                studyMateDrawer
            )
        }

        val welcomeContent =
            view.findViewById<View>(
                R.id.studyMateWelcome
            )

        val chatContainer =
            view.findViewById<LinearLayout>(
                R.id.chatContainer
            )

        // Starts a fresh StudyMate conversation.
        newChatButton.setOnClickListener {

            // Do not reset an unfinished chat creation.
            if (creatingChat) {
                Toast.makeText(
                    requireContext(),
                    "Please wait for the current message to finish.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            currentChatId = null
            conversationHistory.clear()

            chatContainer.removeAllViews()

            welcomeContent.visibility =
                View.VISIBLE

            chatContainer.visibility =
                View.GONE

            studyMateDrawer.visibility =
                View.GONE

            studyMateDrawerOverlay.visibility =
                View.GONE
        }

        // Opens the StudyMate chat history drawer.
        studyMateMenu.setOnClickListener {
            studyMateDrawer.visibility = View.VISIBLE
            studyMateDrawerOverlay.visibility = View.VISIBLE
        }

        // Closes the StudyMate chat history drawer.
        closeStudyMateDrawer.setOnClickListener {
            studyMateDrawer.visibility = View.GONE
            studyMateDrawerOverlay.visibility = View.GONE
        }

        // Closes the drawer when the outside area is tapped.
        studyMateDrawerOverlay.setOnClickListener {
            studyMateDrawer.visibility = View.GONE
            studyMateDrawerOverlay.visibility = View.GONE
        }

        // Keeps the chat area and latest message visible above the keyboard.
        ViewCompat.setOnApplyWindowInsetsListener(
            view
        ) { _, insets ->

            val imeBottom =
                insets.getInsets(
                    WindowInsetsCompat.Type.ime()
                ).bottom

            val systemBottom =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                ).bottom

            val keyboardHeight =
                (imeBottom - systemBottom)
                    .coerceAtLeast(0)

            // Moves the input bar above the keyboard.
            inputBar.translationY =
                -keyboardHeight.toFloat()

            // Gives the chat enough bottom space above the input bar.
            chatContainer.setPadding(
                chatContainer.paddingLeft,
                chatContainer.paddingTop,
                chatContainer.paddingRight,
                keyboardHeight
            )

            // Keeps the newest message visible.
            if (keyboardHeight > 0) {
                chatContainer.post {
                    chatContainer.parent?.let { parent ->
                        (parent as? android.widget.ScrollView)
                            ?.fullScroll(View.FOCUS_DOWN)
                    }
                }
            }

            insets
        }

        ViewCompat.requestApplyInsets(view)

        // Quick actions send predefined academic assistance requests.
        view.findViewById<View>(
            R.id.btnPlanStudy
        ).setOnClickListener {
            sendMessage(
                "Plan my study",
                header,
                welcomeContent,
                chatContainer,
                questionInput
            )
        }

        view.findViewById<View>(
            R.id.btnTodayStudy
        ).setOnClickListener {
            sendMessage(
                "What should I study today?",
                header,
                welcomeContent,
                chatContainer,
                questionInput
            )
        }

        view.findViewById<View>(
            R.id.btnMissedTasks
        ).setOnClickListener {
            sendMessage(
                "Help me with my missed tasks",
                header,
                welcomeContent,
                chatContainer,
                questionInput
            )
        }

        view.findViewById<View>(
            R.id.btnWorkload
        ).setOnClickListener {
            sendMessage(
                "Analyze my workload",
                header,
                welcomeContent,
                chatContainer,
                questionInput
            )
        }

        // Sends the user's message when the Send button is tapped.
        sendButton.setOnClickListener {
            val question = questionInput.text.toString().trim()

            // Ignore empty messages.
            if (question.isEmpty()) return@setOnClickListener

            // Display the message and start generating the AI response.
            sendMessage(
                message = question,
                header = header,
                welcomeContent = welcomeContent,
                chatContainer = chatContainer,
                questionInput = questionInput
            )

            // Clear the input field after the message is sent.
            questionInput.text.clear()
        }
    }

    /** Processes a student message and starts the AI response workflow. */
    private fun sendMessage(
        message: String,
        header: View,
        welcomeContent: View,
        chatContainer: LinearLayout,
        questionInput: EditText
    ) {
        header.visibility = View.VISIBLE
        welcomeContent.visibility = View.GONE
        chatContainer.visibility = View.VISIBLE

        questionInput.clearFocus()

        // Show the user's message immediately.
        addUserMessage(
            message = message,
            chatContainer = chatContainer,
            saveToFirestore = false
        )

        // Show the thinking indicator immediately.
        val loadingMessage =
            addStudyMateMessage(
                "Thinking...",
                chatContainer,
                false
            )

        // Retrieve the student's study data for a personalized AI prompt.
        StudyMateRepository.getStudyMateData(
            onSuccess = { studyMateData ->

                val history = buildConversationHistory()

                val prompt = StudyMatePrompt.buildPrompt(
                    message = message,
                    data = studyMateData,
                    conversationHistory = history
                )

                CoroutineScope(Dispatchers.IO).launch {

                    if (currentChatId == null) {

                        if (creatingChat) {
                            return@launch
                        }

                        creatingChat = true

                        // Create the chat immediately so the response does not
                        // have to wait for Gemini to generate the title.
                        withContext(Dispatchers.Main) {

                            StudyMateChatRepository.createChat(
                                title = "New Chat",

                                onSuccess = { chatId ->

                                    currentChatId = chatId
                                    creatingChat = false

                                    // Save the message that was already displayed.
                                    StudyMateChatRepository.saveMessage(
                                        chatId = chatId,
                                        role = "user",
                                        text = message,
                                        onFailure = { exception ->
                                            exception.printStackTrace()
                                        }
                                    )

                                    // Generate the chat title separately.
                                    CoroutineScope(Dispatchers.IO).launch {

                                        val chatTitle =
                                            try {
                                                GeminiService.generateChatTitle(message)
                                            } catch (e: Exception) {
                                                "New Chat"
                                            }

                                        withContext(Dispatchers.Main) {

                                            StudyMateChatRepository.updateChatTitle(
                                                chatId = chatId,
                                                title = chatTitle,
                                                onFailure = { exception ->
                                                    exception.printStackTrace()
                                                }
                                            )
                                        }
                                    }

                                    // Start the actual StudyMate response immediately.
                                    generateStudyMateResponse(
                                        message = message,
                                        prompt = prompt,
                                        chatContainer = chatContainer,
                                        loadingMessage = loadingMessage
                                    )
                                },

                                onFailure = { exception ->

                                    creatingChat = false

                                    // Remove the thinking indicator if chat creation fails.
                                    chatContainer.removeView(
                                        loadingMessage
                                    )

                                    Toast.makeText(
                                        requireContext(),
                                        "Could not start chat",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    exception.printStackTrace()
                                }
                            )
                        }

                    } else {

                        withContext(Dispatchers.Main) {

                            // Save the message that was already displayed.
                            StudyMateChatRepository.saveMessage(
                                chatId = currentChatId!!,
                                role = "user",
                                text = message,
                                onFailure = { exception ->
                                    exception.printStackTrace()
                                }
                            )

                            generateStudyMateResponse(
                                message = message,
                                prompt = prompt,
                                chatContainer = chatContainer,
                                loadingMessage = loadingMessage
                            )
                        }
                    }
                }
            },

            onFailure = { exception ->

                chatContainer.removeView(
                    loadingMessage
                )

                Toast.makeText(
                    requireContext(),
                    "I couldn't load your study information right now. Please try again.",
                    Toast.LENGTH_SHORT
                ).show()

                exception.printStackTrace()
            }
        )
    }

    /** Generates an AI response and displays or previews the result. */
    private fun generateStudyMateResponse(
        message: String,
        prompt: String,
        chatContainer: LinearLayout,
        loadingMessage: TextView
    ) {

        // Keep references before the Fragment can be detached.
        val mainActivity =
            activity as? MainActivity

        val chatId =
            currentChatId

        CoroutineScope(
            Dispatchers.IO
        ).launch {

            val response =
                try {
                    GeminiService.askGemini(prompt)
                } catch (e: Exception) {
                    "ERROR: ${e.message}"
                }

            // Separate the visible AI response from structured scheduling data.
            val visibleResponse =
                response
                    .substringBefore(
                        "---SCHEDULE_DATA---"
                    )
                    .trim()

            withContext(Dispatchers.Main) {

                if (response.startsWith("ERROR:")) {

                    if (isAdded && view != null) {

                        chatContainer.removeView(
                            loadingMessage
                        )

                        displayStudyMateResponse(
                            response,
                            chatContainer
                        )
                    }

                    return@withContext
                }

                // Save the AI response to the active conversation.
                chatId?.let { activeChatId ->

                    StudyMateChatRepository.saveMessage(
                        chatId = activeChatId,
                        role = "assistant",
                        text = visibleResponse,
                        onFailure = { exception ->
                            exception.printStackTrace()
                        }
                    )
                }

                // The Fragment is still open.
                if (isAdded && view != null) {

                    conversationHistory.add(
                        ConversationMessage(
                            role = "Student",
                            text = message
                        )
                    )

                    conversationHistory.add(
                        ConversationMessage(
                            role = "StudyMate",
                            text = visibleResponse
                        )
                    )

                    trimConversationHistory()

                    chatContainer.removeView(
                        loadingMessage
                    )

                    displayStudyMateResponse(
                        response,
                        chatContainer
                    )

                    return@withContext
                }

                // The user left StudyMate while Gemini was responding.
                mainActivity?.showStudyMateResponsePreview(
                    visibleResponse
                )
            }
        }
    }

    /** Formats previous messages into context for the next AI request. */
    private fun buildConversationHistory(): String {

        if (conversationHistory.isEmpty()) {
            return "No previous conversation."
        }

        return conversationHistory
            .joinToString("\n\n") { message ->

                "${message.role}: ${message.text}"
            }
    }

    /** Keeps only the 12 most recent messages in AI conversation context. */
    private fun trimConversationHistory() {

        val maximumMessages = 12

        if (conversationHistory.size > maximumMessages) {

            val messagesToRemove =
                conversationHistory.size -
                        maximumMessages

            repeat(messagesToRemove) {
                conversationHistory.removeAt(0)
            }
        }
    }


    /** Displays AI text and processes any proposed schedule changes. */
    private fun displayStudyMateResponse(
        rawResponse: String,
        chatContainer: LinearLayout
    ) {

        val scheduleMarker =
            "---SCHEDULE_DATA---"

        val endMarker =
            "---END_SCHEDULE---"

        val responseText =
            if (rawResponse.contains(scheduleMarker)) {
                rawResponse
                    .substringBefore(scheduleMarker)
                    .trim()
            } else {
                rawResponse.trim()
            }

        if (
            rawResponse.contains("SCHEDULE: YES") &&
            rawResponse.contains(scheduleMarker)
        ) {

            val scheduleBlock =
                rawResponse
                    .substringAfter(scheduleMarker)
                    .substringBefore(endMarker)

            val suggestion =
                ScheduleSuggestion(
                    taskId =
                        extractField(
                            scheduleBlock,
                            "TASK_ID"
                        ),
                    title =
                        extractField(
                            scheduleBlock,
                            "TITLE"
                        ),
                    course =
                        extractField(
                            scheduleBlock,
                            "COURSE"
                        ),
                    date =
                        extractField(
                            scheduleBlock,
                            "DATE"
                        ),
                    time =
                        extractField(
                            scheduleBlock,
                            "TIME"
                        ),
                    importance =
                        extractField(
                            scheduleBlock,
                            "IMPORTANCE"
                        ),
                    urgency =
                        extractField(
                            scheduleBlock,
                            "URGENCY"
                        ),
                    description =
                        extractField(
                            scheduleBlock,
                            "DESCRIPTION"
                        )
                )

            if (
                suggestion.title.isNotBlank() &&
                suggestion.date.isNotBlank() &&
                suggestion.time.isNotBlank() &&
                isValidScheduleDateTime(
                    suggestion.date,
                    suggestion.time
                )
            ) {

                checkScheduleConflict(
                    suggestion,
                    chatContainer,
                    responseText
                )

                return
            }
        }

        val cleanedResponseText =
            responseText
                .replace(
                    Regex(
                        """(?i)\(?\s*Task ID\s*:\s*[A-Za-z0-9_-]+\s*\)?"""
                    ),
                    ""
                )
                .replace(
                    Regex(
                        """(?i)\(?\s*Task ID\s*=\s*[A-Za-z0-9_-]+\s*\)?"""
                    ),
                    ""
                )
                .trim()

        addStudyMateMessage(
            cleanedResponseText,
            chatContainer
        )
    }

    /** Checks Firestore for an incomplete task at the proposed date and time. */
    private fun checkScheduleConflict(
        suggestion: ScheduleSuggestion,
        chatContainer: LinearLayout,
        aiResponse: String
    ) {

        val user =
            FirebaseAuth
                .getInstance()
                .currentUser

        if (user == null) {
            Toast.makeText(
                requireContext(),
                "Please sign in again.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val targetDate =
            formatDisplayDate(
                suggestion.date
            )

        val targetTime =
            formatDisplayTime(
                suggestion.time
            )

        FirebaseFirestore
            .getInstance()
            .collection("users")
            .document(user.uid)
            .collection("tasks")
            .whereEqualTo(
                "date",
                targetDate
            )
            .whereEqualTo(
                "time",
                targetTime
            )
            .get()
            .addOnSuccessListener { documents ->

                val conflictingTask =
                    documents.documents.firstOrNull { document ->

                        val isSameTask =
                            suggestion.taskId.isNotBlank() &&
                                    document.id == suggestion.taskId

                        val isCompleted =
                            document.getBoolean("completed") == true

                        !isSameTask && !isCompleted
                    }

                if (conflictingTask != null) {

                    val conflictingTitle =
                        conflictingTask
                            .getString("title")
                            ?: "Another task"

                    val conflictMessage =
                        "$targetTime is already occupied by " +
                                "$conflictingTitle.\n\n" +
                                "Please choose another time, or I can " +
                                "suggest an available time for you."

                    addStudyMateMessage(
                        conflictMessage,
                        chatContainer
                    )

                    return@addOnSuccessListener
                }

                val cleanedResponseText =
                    aiResponse
                        .replace(
                            Regex(
                                """(?i)\(?\s*Task ID\s*:\s*[A-Za-z0-9_-]+\s*\)?"""
                            ),
                            ""
                        )
                        .replace(
                            Regex(
                                """(?i)\(?\s*Task ID\s*=\s*[A-Za-z0-9_-]+\s*\)?"""
                            ),
                            ""
                        )
                        .trim()

                addStudyMateMessage(
                    cleanedResponseText,
                    chatContainer
                )

                addScheduleButton(
                    suggestion,
                    chatContainer
                )
            }
            .addOnFailureListener {

                addStudyMateMessage(
                    "I couldn't check your current schedule. " +
                            "Please try again.",
                    chatContainer
                )
            }
    }

    /** Extracts a named value from the AI-generated schedule data. */
    private fun extractField(
        text: String,
        fieldName: String
    ): String {

        val pattern =
            Regex(
                "(?m)^${Regex.escape(fieldName)}:\\s*(.*)$"
            )

        return pattern
            .find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?: ""
    }

    /** Adds a button for confirming an AI-suggested study task. */
    private fun addScheduleButton(
        suggestion: ScheduleSuggestion,
        chatContainer: LinearLayout
    ) {

        val scheduleButton =
            TextView(requireContext())

        scheduleButton.text =
            "＋  Add to my schedule"

        scheduleButton.textSize = 14f

        scheduleButton.setTextColor(
            resources.getColor(
                R.color.primary,
                null
            )
        )
        scheduleButton.setPadding(
            16,
            10,
            16,
            10
        )

        val params =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        params.gravity = Gravity.START

        params.setMargins(
            0,
            0,
            40,
            12
        )

        scheduleButton.layoutParams = params

        scheduleButton.setOnClickListener {
            showScheduleConfirmation(
                suggestion,
                scheduleButton
            )
        }

        chatContainer.addView(
            scheduleButton
        )
    }

    /** Requests student confirmation before saving a schedule suggestion. */
    private fun showScheduleConfirmation(
        suggestion: ScheduleSuggestion,
        scheduleButton: TextView
    ) {

        val dialog =
            AlertDialog.Builder(requireContext())
                .setTitle("Add to your schedule?")
                .setMessage(
                    "${suggestion.title}\n\n" +
                            "${suggestion.date} at ${suggestion.time}"
                )
                .setNegativeButton(
                    "Cancel",
                    null
                )
                .setPositiveButton(
                    "Add"
                ) { _, _ ->

                    saveSuggestionToFirestore(
                        suggestion,
                        scheduleButton
                    )
                }
                .create()

        dialog.show()

        // Make the dialog buttons use the app's primary colour.
        dialog.getButton(
            AlertDialog.BUTTON_POSITIVE
        ).setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.primary
            )
        )

        dialog.getButton(
            AlertDialog.BUTTON_NEGATIVE
        ).setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.primary
            )
        )
    }

    /** Updates a matching task or creates a new task in Firestore. */
    private fun saveSuggestionToFirestore(
        suggestion: ScheduleSuggestion,
        scheduleButton: TextView
    ) {

        val user =
            FirebaseAuth
                .getInstance()
                .currentUser

        if (user == null) {
            Toast.makeText(
                requireContext(),
                "Please sign in again.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val deadline =
            try {

                val formatter =
                    SimpleDateFormat(
                        "yyyy-MM-dd HH:mm",
                        Locale.getDefault()
                    )

                formatter.isLenient = false

                val date =
                    formatter.parse(
                        "${suggestion.date} ${suggestion.time}"
                    )

                Timestamp(
                    date ?: Date()
                )

            } catch (e: Exception) {

                Toast.makeText(
                    requireContext(),
                    "The suggested date or time is invalid.",
                    Toast.LENGTH_SHORT
                ).show()

                return
            }

        val tasksReference =
            FirebaseFirestore
                .getInstance()
                .collection("users")
                .document(user.uid)
                .collection("tasks")

        if (suggestion.taskId.isNotBlank()) {

            val taskReference =
                tasksReference
                    .document(suggestion.taskId)

            taskReference
                .get()
                .addOnSuccessListener { document ->

                    if (!document.exists()) {

                        createNewScheduleTask(
                            suggestion,
                            deadline,
                            tasksReference,
                            scheduleButton
                        )

                        return@addOnSuccessListener
                    }

                    val existingTitle =
                        document
                            .getString("title")
                            ?.trim()
                            ?: ""

                    val existingCourse =
                        document
                            .getString("course")
                            ?.trim()
                            ?: ""

                    val suggestedTitle =
                        suggestion.title.trim()

                    val suggestedCourse =
                        suggestion.course.trim()

                    val titleMatches =
                        existingTitle.equals(
                            suggestedTitle,
                            ignoreCase = true
                        )

                    val courseMatches =
                        suggestedCourse.isBlank() ||
                                existingCourse.equals(
                                    suggestedCourse,
                                    ignoreCase = true
                                )

                    if (!titleMatches || !courseMatches) {

                        createNewScheduleTask(
                            suggestion,
                            deadline,
                            tasksReference,
                            scheduleButton
                        )

                        return@addOnSuccessListener
                    }

                    taskReference
                        .update(
                            mapOf(
                                "deadline" to deadline,
                                "date" to formatDisplayDate(
                                    suggestion.date
                                ),
                                "time" to formatDisplayTime(
                                    suggestion.time
                                )
                            )
                        )
                        .addOnSuccessListener {

                            scheduleButton.isEnabled =
                                false

                            scheduleButton.text =
                                "✓  Added to my schedule"

                            Toast.makeText(
                                requireContext(),
                                "Schedule updated ✓",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .addOnFailureListener {

                            Toast.makeText(
                                requireContext(),
                                "Couldn't update this task. Please try again.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                .addOnFailureListener {

                    Toast.makeText(
                        requireContext(),
                        "Couldn't verify this task. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            return
        }

        createNewScheduleTask(
            suggestion,
            deadline,
            tasksReference,
            scheduleButton
        )
    }

    /** Creates a new study task from the AI-generated schedule suggestion. */
    private fun createNewScheduleTask(
        suggestion: ScheduleSuggestion,
        deadline: Timestamp,
        tasksReference: com.google.firebase.firestore.CollectionReference,
        scheduleButton: TextView
    ) {

        val taskReference =
            tasksReference.document()

        val task =
            Task(
                id = taskReference.id,
                title = suggestion.title,
                course = suggestion.course,
                description = suggestion.description,
                deadline = deadline,
                date = formatDisplayDate(
                    suggestion.date
                ),
                time = formatDisplayTime(
                    suggestion.time
                ),
                importance = suggestion.importance,
                urgency = suggestion.urgency,
                priority = calculatePriority(
                    suggestion.importance,
                    suggestion.urgency
                ),
                reminder = "No reminder",
                completed = false
            )

        taskReference
            .set(task)
            .addOnSuccessListener {

                scheduleButton.isEnabled =
                    false

                scheduleButton.text =
                    "✓  Added to my schedule"

                Toast.makeText(
                    requireContext(),
                    "Added to your schedule ✓",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {

                Toast.makeText(
                    requireContext(),
                    "Couldn't add this to your schedule. Please try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    /** Converts a schedule date into the application's display format. */
    private fun formatDisplayDate(
        date: String
    ): String {

        return try {

            val inputFormatter =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                )

            val outputFormatter =
                SimpleDateFormat(
                    "dd MMM yyyy",
                    Locale.getDefault()
                )

            val parsedDate =
                inputFormatter.parse(date)

            outputFormatter.format(
                parsedDate ?: Date()
            )

        } catch (e: Exception) {

            date
        }
    }

    /** Converts a 24-hour time into the application's 12-hour display format. */
    private fun formatDisplayTime(
        time: String
    ): String {

        return try {

            val inputFormatter =
                SimpleDateFormat(
                    "HH:mm",
                    Locale.getDefault()
                )

            val outputFormatter =
                SimpleDateFormat(
                    "hh:mm a",
                    Locale.getDefault()
                )

            val parsedTime =
                inputFormatter.parse(time)

            outputFormatter.format(
                parsedTime ?: Date()
            )

        } catch (e: Exception) {
            time
        }
    }

    /** Calculates task priority from the importance and urgency values. */
    private fun calculatePriority(
        importance: String,
        urgency: String
    ): String {

        val important =
            importance.contains(
                "high",
                ignoreCase = true
            )

        val urgent =
            urgency.contains(
                "high",
                ignoreCase = true
            )

        return when {

            important && urgent ->
                "High"

            important || urgent ->
                "Medium"

            else ->
                "Low"
        }
    }

    /** Validates a suggested date and time and ensures it is in the future. */
    private fun isValidScheduleDateTime(
        date: String,
        time: String
    ): Boolean {

        return try {

            val formatter =
                SimpleDateFormat(
                    "yyyy-MM-dd HH:mm",
                    Locale.getDefault()
                )

            formatter.isLenient = false

            val dateTime =
                "$date $time"

            val parsedDate =
                formatter.parse(dateTime)
                    ?: return false

            if (
                formatter.format(parsedDate)
                != dateTime
            ) {
                return false
            }

            parsedDate.after(Date())

        } catch (e: Exception) {

            false
        }
    }

    /** Displays a student message and optionally saves it to Firestore. */
    private fun addUserMessage(
        message: String,
        chatContainer: LinearLayout,
        saveToFirestore: Boolean = true
    ) {

        val messageView =
            TextView(requireContext())

        messageView.text =
            message

        messageView.textSize = 15f

        messageView.setTextColor(
            resources.getColor(
                R.color.button_text,
                null
            )
        )

        messageView.background =
            resources.getDrawable(
                R.drawable.bg_studymate_user,
                null
            )

        val params =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        params.gravity =
            Gravity.END

        params.setMargins(
            40,
            8,
            0,
            8
        )

        messageView.layoutParams =
            params

        messageView.setOnLongClickListener {

            showMessageMenu(
                messageView,
                message,
                true
            )

            true
        }

        chatContainer.addView(
            messageView
        )

        // Saves only newly sent messages.
        if (saveToFirestore) {
            currentChatId?.let { chatId ->

                StudyMateChatRepository.saveMessage(
                    chatId = chatId,
                    role = "user",
                    text = message,
                    onFailure = { exception ->
                        exception.printStackTrace()
                    }
                )
            }
        }
    }

    /** Displays a formatted AI message and optionally saves it. */
    private fun addStudyMateMessage(
        message: String,
        chatContainer: LinearLayout,
        saveToFirestore: Boolean = true
    ): TextView {

        val messageView =
            TextView(requireContext())

        messageView.text =
            formatStudyMateText(message)

        messageView.textSize =
            15f

        messageView.setTextColor(
            resources.getColor(
                R.color.primary_text,
                null
            )
        )

        messageView.background =
            resources.getDrawable(
                R.drawable.bg_studymate_ai,
                null
            )

        val params =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        params.gravity =
            Gravity.START

        params.setMargins(
            0,
            8,
            40,
            8
        )

        messageView.layoutParams =
            params

        messageView.setOnLongClickListener {

            showMessageMenu(
                messageView,
                messageView.text.toString(),
                false
            )

            true
        }

        chatContainer.addView(
            messageView
        )

        // Saves only newly generated AI messages.
        if (saveToFirestore) {
            currentChatId?.let { chatId ->

                StudyMateChatRepository.saveMessage(
                    chatId = chatId,
                    role = "assistant",
                    text = messageView.text.toString(),
                    onFailure = { exception ->
                        exception.printStackTrace()
                    }
                )
            }
        }

        return messageView
    }

    /** Shows the Edit and Copy actions available on a message. */
    private fun showMessageMenu(
        messageView: TextView,
        message: String,
        isUserMessage: Boolean
    ) {

        val menuContainer =
            LinearLayout(requireContext()).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    0,
                    4,
                    0,
                    4
                )

                background =
                    android.graphics.drawable.GradientDrawable().apply {

                        setColor(
                            android.graphics.Color.WHITE
                        )

                        cornerRadius =
                            16f
                    }
            }

        fun addMenuItem(
            text: String,
            action: () -> Unit
        ) {

            val item =
                TextView(requireContext()).apply {

                    this.text =
                        text

                    textSize =
                        15f

                    setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.primary
                        )
                    )

                    gravity =
                        Gravity.CENTER_VERTICAL

                    setPadding(
                        20,
                        0,
                        20,
                        0
                    )

                    layoutParams =
                        LinearLayout.LayoutParams(
                            160.dp(),
                            52.dp()
                        )

                    isClickable =
                        true

                    setOnClickListener {
                        action()
                    }
                }

            menuContainer.addView(item)
        }

        if (isUserMessage) {

            addMenuItem("Edit") {
                editUserMessage(message)
            }
        }

        addMenuItem("Copy") {
            copyMessage(message)
        }

        val popupWindow =
            android.widget.PopupWindow(
                menuContainer,
                160.dp(),
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
            )

        popupWindow.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(
                android.graphics.Color.WHITE
            )
        )

        popupWindow.elevation =
            8f

        popupWindow.isOutsideTouchable =
            true

        popupWindow.showAsDropDown(
            messageView,
            0,
            -messageView.height
        )
    }

    // Converts dp to pixels.
    private fun Int.dp(): Int =
        (this * resources.displayMetrics.density).toInt()

    /** Places a selected message back into the text input for editing. */
    private fun editUserMessage(
        message: String
    ) {

        val input =
            studyMateInput
                ?: return

        input.setText(
            message
        )

        input.setSelection(
            input.text.length
        )

        input.requestFocus()

        val keyboard =
            requireContext()
                .getSystemService(
                    Context.INPUT_METHOD_SERVICE
                ) as InputMethodManager

        keyboard.showSoftInput(
            input,
            InputMethodManager.SHOW_IMPLICIT
        )
    }

    /** Copies the selected message to the device clipboard. */
    private fun copyMessage(
        message: String
    ) {

        val clipboard =
            requireContext()
                .getSystemService(
                    Context.CLIPBOARD_SERVICE
                ) as ClipboardManager

        val clip =
            ClipData.newPlainText(
                "StudyMate message",
                message
            )

        clipboard.setPrimaryClip(
            clip
        )

        Toast.makeText(
            requireContext(),
            "Copied ✓",
            Toast.LENGTH_SHORT
        ).show()
    }

    /** Formats basic AI response markup for readable chat bubbles. */
    private fun formatStudyMateText(
        message: String
    ): Spanned {

        var html =
            TextUtils.htmlEncode(message)

        html =
            html.replace(
                Regex("""\*\*(.+?)\*\*""")
            ) {
                "<b>${it.groupValues[1]}</b>"
            }

        html =
            html.replace(
                Regex("""(?m)^\s*\*\s+""")
            ) {
                "• "
            }

        html =
            html.replace(
                Regex("""(?m)^\s*-\s+""")
            ) {
                "• "
            }

        html =
            html.replace(
                Regex("""(?m)^#+\s+"""),
                ""
            )

        html =
            html.replace(
                "*",
                ""
            )

        html =
            html.replace(
                "\n",
                "<br>"
            )

        return HtmlCompat.fromHtml(
            html,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }

    /** Loads recent conversations and provides selection and deletion controls. */
    private fun loadRecentChats(
        container: LinearLayout,
        drawer: LinearLayout
    ) {
        container.removeAllViews()

        // Keeps track of the chat currently showing Delete.
        var activeDeleteButton: TextView? = null

        StudyMateChatRepository.getChats(
            onSuccess = { chats ->

                // Stop if StudyMateFragment is no longer attached.
                val safeContext = context ?: return@getChats

                container.removeAllViews()

                // Keeps track of the currently selected chat row.
                var selectedChatRow: LinearLayout? = null

                chats.forEach { chat ->

                    val chatRow =
                        LinearLayout(safeContext).apply {
                            orientation =
                                LinearLayout.HORIZONTAL

                            gravity =
                                Gravity.CENTER_VERTICAL

                            setPadding(
                                16,
                                4,
                                8,
                                4
                            )

                            tag = chat.id
                            isClickable = true
                            isFocusable = true

                            setBackgroundColor(
                                ContextCompat.getColor(
                                    safeContext,
                                    android.R.color.transparent
                                )
                            )
                        }

                    val chatTitle =
                        TextView(safeContext).apply {
                            text =
                                chat.title

                            textSize =
                                14f

                            setTextColor(
                                ContextCompat.getColor(
                                    safeContext,
                                    R.color.primary_text
                                )
                            )

                            gravity =
                                Gravity.CENTER_VERTICAL

                            layoutParams =
                                LinearLayout.LayoutParams(
                                    0,
                                    52.dp(),
                                    1f
                                )
                        }

                    val deleteButton =
                        TextView(safeContext).apply {
                            text =
                                "Delete"

                            textSize =
                                14f

                            setTextColor(
                                ContextCompat.getColor(
                                    safeContext,
                                    R.color.primary
                                )
                            )

                            gravity =
                                Gravity.CENTER

                            setPadding(
                                12,
                                0,
                                8,
                                0
                            )

                            visibility =
                                View.GONE

                            layoutParams =
                                LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    52.dp()
                                )

                            setOnClickListener {

                                showDeleteChatDialog(
                                    chat.id,
                                    chat.title,
                                    container
                                )
                            }
                        }

                    chatRow.addView(
                        chatTitle
                    )

                    chatRow.addView(
                        deleteButton
                    )

                    chatRow.setOnClickListener {

                        // Removes highlight from the previous chat.
                        selectedChatRow?.let { previousRow ->

                            previousRow.setBackgroundColor(
                                ContextCompat.getColor(
                                    safeContext,
                                    android.R.color.transparent
                                )
                            )

                            val previousDeleteButton =
                                previousRow.getChildAt(1) as? TextView

                            previousDeleteButton?.setTextColor(
                                ContextCompat.getColor(
                                    safeContext,
                                    R.color.primary
                                )
                            )
                        }

                        // Highlights the selected chat.
                        chatRow.setBackgroundColor(
                            ContextCompat.getColor(
                                safeContext,
                                R.color.primary
                            )
                        )

                        // Makes Delete white on the highlighted chat.
                        deleteButton.setTextColor(
                            ContextCompat.getColor(
                                safeContext,
                                android.R.color.white
                            )
                        )

                        selectedChatRow =
                            chatRow

                        loadSelectedChat(
                            chat.id,
                            drawer
                        )
                    }

                    // Shows Delete only for the long-pressed chat.
                    chatRow.setOnLongClickListener {

                        // Hides the previously active Delete button.
                        activeDeleteButton?.visibility =
                            View.GONE

                        // Shows Delete for this chat.
                        deleteButton.visibility =
                            View.VISIBLE

                        // Makes this the active Delete button.
                        activeDeleteButton =
                            deleteButton

                        true
                    }

                    container.addView(
                        chatRow
                    )
                }
            },

            onFailure = { exception ->

                // Stop if StudyMateFragment is no longer attached.
                val safeContext = context ?: return@getChats

                Toast.makeText(
                    safeContext,
                    "Could not load chats",
                    Toast.LENGTH_SHORT
                ).show()

                exception.printStackTrace()
            }
        )
    }

    /** Confirms and deletes a selected chat conversation. */
    private fun showDeleteChatDialog(
        chatId: String,
        chatTitle: String,
        container: LinearLayout
    ) {
        val dialog =
            AlertDialog.Builder(requireContext())
                .setTitle("Delete conversation?")
                .setMessage(
                    "This will permanently delete \"$chatTitle\" and all its messages."
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", null)
                .create()

        dialog.setOnShowListener {

            val primaryColor =
                ContextCompat.getColor(
                    requireContext(),
                    R.color.primary
                )

            dialog.getButton(
                AlertDialog.BUTTON_NEGATIVE
            ).setTextColor(primaryColor)

            val deleteButton =
                dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
                )

            deleteButton.setTextColor(primaryColor)

            deleteButton.setOnClickListener {

                // Prevents repeated taps while deleting.
                deleteButton.isEnabled = false
                deleteButton.text = "Deleting..."

                StudyMateChatRepository.deleteChat(
                    chatId = chatId,

                    onSuccess = {

                        if (currentChatId == chatId) {
                            currentChatId = null
                            conversationHistory.clear()

                            view?.findViewById<LinearLayout>(
                                R.id.chatContainer
                            )?.removeAllViews()

                            view?.findViewById<View>(
                                R.id.chatContainer
                            )?.visibility = View.GONE

                            view?.findViewById<View>(
                                R.id.studyMateWelcome
                            )?.visibility = View.VISIBLE
                        }

                        // The realtime listener will rebuild the Recent list using the updated Firestore data.
                        dialog.dismiss()

                        Toast.makeText(
                            requireContext(),
                            "Conversation deleted",
                            Toast.LENGTH_SHORT
                        ).show()
                    },

                    onFailure = { exception ->

                        deleteButton.isEnabled = true
                        deleteButton.text = "Delete"

                        Toast.makeText(
                            requireContext(),
                            "Delete failed: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()

                        exception.printStackTrace()
                    }
                )
            }
        }

        dialog.show()
    }

    /** Restores a saved conversation and its messages from Firestore. */
    private fun loadSelectedChat(
        chatId: String,
        drawer: LinearLayout
    ) {

        val rootView =
            view ?: return

        val welcomeContent =
            rootView.findViewById<View>(
                R.id.studyMateWelcome
            )

        val chatContainer =
            rootView.findViewById<LinearLayout>(
                R.id.chatContainer
            )

        currentChatId =
            chatId

        StudyMateChatRepository.getMessages(
            chatId = chatId,
            onSuccess = { messages ->

                conversationHistory.clear()

                chatContainer.removeAllViews()

                welcomeContent.visibility =
                    View.GONE

                chatContainer.visibility =
                    View.VISIBLE

                messages.forEach { message ->

                    conversationHistory.add(
                        ConversationMessage(
                            role = message.role,
                            text = message.text
                        )
                    )

                    when (message.role) {

                        "user" -> {
                            addUserMessage(
                                message.text,
                                chatContainer,
                                false
                            )
                        }

                        "assistant" -> {
                            addStudyMateMessage(
                                message.text,
                                chatContainer,
                                false
                            )
                        }
                    }
                }

                drawer.visibility =
                    View.GONE
            },
            onFailure = { exception ->

                Toast.makeText(
                    requireContext(),
                    "Could not load conversation",
                    Toast.LENGTH_SHORT
                ).show()

                exception.printStackTrace()
            }
        )
    }

    /** Releases the input reference and restores the main navigation. */
    override fun onDestroyView() {
        studyMateInput = null
        super.onDestroyView()
        (activity as? MainActivity)
            ?.hideStudyMateMode()
    }

    /** Holds the structured details of an AI-proposed schedule entry. */
    private data class ScheduleSuggestion(
        val taskId: String = "",
        val title: String = "",
        val course: String = "",
        val date: String = "",
        val time: String = "",
        val importance: String = "",
        val urgency: String = "",
        val description: String = ""
    )
}
