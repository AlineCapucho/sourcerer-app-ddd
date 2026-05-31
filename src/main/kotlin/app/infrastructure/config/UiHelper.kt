package app.infrastructure.config

import app.infrastructure.Logger

/**
 * Helper for console UI interactions.
 */
object UiHelper {
    fun confirm(message: String, defaultIsYes: Boolean = false): Boolean {
        val options = if (defaultIsYes) "(Y/n)" else "(y/N)"
        Logger.print("$message $options")
        val answer = (readLine() ?: "").trim().toLowerCase()
        return if (answer.isEmpty()) {
            defaultIsYes
        } else {
            answer == "y" || answer == "yes"
        }
    }
}
