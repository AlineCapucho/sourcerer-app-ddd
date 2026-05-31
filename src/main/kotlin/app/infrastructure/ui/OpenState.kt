package app.infrastructure.ui

import app.infrastructure.Logger

/**
 * On application open console UI state.
 */
class OpenState(private val context: ConsoleUi) : ConsoleState {
    override fun doAction() {
        val aggregate = context.userRepository.load()
        if (!aggregate.hasValidCredentials()) {
            Logger.print("Sourcerer hashes your git repositories into " +
                "intelligent engineering profiles.")
            Logger.print("If you don't have an account, please, sign up at " +
                "https://sourcerer.io/join")
        } else {
            Logger.print("Sourcerer. Use flag --help to list available commands.")
        }
    }

    override fun next() {
        context.changeState(AuthState(context))
    }
}
