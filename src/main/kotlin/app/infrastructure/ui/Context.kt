package app.infrastructure.ui

/**
 * Context interface for state machine pattern.
 */
interface Context {
    fun changeState(state: ConsoleState)
}
