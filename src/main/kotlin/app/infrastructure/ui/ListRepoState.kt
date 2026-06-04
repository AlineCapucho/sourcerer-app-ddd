package app.infrastructure.ui

import app.infrastructure.Logger

/**
 * List tracked repositories console UI state.
 */
class ListRepoState(private val context: ConsoleUi) : ConsoleState {
    override fun doAction() {
        val aggregate = context.userRepository.load()
        val repos = aggregate.getLocalRepos()
        if (repos.isNotEmpty()) {
            Logger.print("Tracked repositories:", indentLine = true)
            repos.forEach { Logger.print(it) }
        }
    }

    override fun next() {
        context.changeState(AddRepoState(context))
    }
}
