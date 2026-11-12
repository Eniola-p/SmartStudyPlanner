
package com.eniola.smartstudyplanner.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.delay

/**GeminiService connects StudyMate to the Gemini AI model through Firebase.
   It generates AI responses, retries temporary service failures,
   and creates short titles for StudyMate conversations.*/
object GeminiService {

    // Initializes the Gemini AI model through Firebase AI Logic.
    private val model =
        Firebase.ai(
            backend = GenerativeBackend.googleAI()
        ).generativeModel(
            modelName = "gemini-3.1-flash-lite"
        )

    /** Sends a prompt to Gemini and retries up to three times if temporary errors occur. */
    suspend fun askGemini(prompt: String): String {

        // Attempt to generate a response up to three times.
        repeat(3) { attempt ->
            try {

                // Generate the response.
                val response =
                    model.generateContent(prompt)

                // Return Gemini's response.
                val text =
                    response.text

                if (!text.isNullOrBlank()) {
                    return text
                }

                throw Exception("Gemini returned an empty response.")

            } catch (e: Exception) {

                // Retrieve the error message to identify the failure.
                val errorMessage =
                    e.message ?: ""

                // Check whether the error is caused by temporary service issues.
                val isTemporaryError =
                    errorMessage.contains("429") ||
                            errorMessage.contains("503") ||
                            errorMessage.contains("TooManyRequests") ||
                            errorMessage.contains("ServiceUnavailable") ||
                            errorMessage.contains("high demand")

                // Stop retrying for permanent errors or after the final attempt.
                if (!isTemporaryError || attempt == 2) {
                    throw e
                }

                // Wait progressively longer before retrying the request.
                delay(
                    when (attempt) {
                        0 -> 2000L
                        1 -> 4000L
                        else -> 6000L
                    }
                )
            }
        }

        // Return an error if no response is obtained.
        throw Exception("Unable to get a response from StudyMate.")
    }

    /** Generates a short, meaningful title from the first message of a StudyMate conversation. */
    suspend fun generateChatTitle(message: String): String {

        // Define the instructions Gemini must follow when generating the title.
        val prompt =
            """
        Create a short, meaningful title for this StudyMate conversation.

        User's first message:
        $message

        Rules:
        - 2 to 5 words only.
        - Describe the main topic of the conversation.
        - Do not copy the user's sentence exactly.
        - Do not use quotation marks.
        - Return only the title.
        """.trimIndent()

        // Generate and clean the title before returning it.
        return askGemini(prompt)
            .trim()
            .removeSurrounding("\"")
            .take(50)
    }
}
