package app.infrastructure.ui

import app.infrastructure.Logger

/**
 * On application close console UI state.
 */
class CloseState : ConsoleState {
    override fun doAction() {
        Logger.print("You could use console commands to control repositories.",
            indentLine = true)
        Logger.print("For more info run application with flag --help.")
        Logger.print("Feel free to contact us on any problem by " +
            "support@sourcerer.io.")
    }

    override fun next() {
    }
}
