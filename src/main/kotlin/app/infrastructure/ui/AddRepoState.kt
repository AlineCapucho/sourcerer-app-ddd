package app.infrastructure.ui

import app.domain.user.valueobject.LocalRepo
import app.infrastructure.Logger
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Add repository dialog console UI state.
 */
class AddRepoState(private val context: ConsoleUi) : ConsoleState {

    override fun doAction() {
        val aggregate = context.userRepository.load()
        if (aggregate.getLocalRepos().isNotEmpty()) return

        while (true) {
            Logger.print("")
            Logger.print("Type one or more paths to repository. You can specify " +
                "multiple repository paths separated by space on the same line.")
            Logger.print("If you finished specifying repositories, just hit 'Enter' to continue.")
            val pathsString = readLine() ?: ""

            if (pathsString.isEmpty()) {
                val currentAggregate = context.userRepository.load()
                if (currentAggregate.getLocalRepos().isEmpty()) {
                    Logger.print("Add at least one valid repository.")
                } else {
                    Logger.print("Finished processing git repositories")
                    break
                }
            } else {
                val paths: List<String> = pathsString.split(' ')
                paths.forEach { pathStr ->
                    val path = toAbsolutePath(pathStr)
                    if (context.gitValidator.isValidRepo(path.toString())) {
                        processPath(path)
                    } else {
                        Files.walk(path)
                            .filter { p -> context.gitValidator.isValidGitRepo(p.toString()) }
                            .forEach { p -> processPath(p.parent) }
                    }
                }
            }
        }

        Logger.info(Logger.Events.CONFIG_SETUP) { "Config setup" }
    }

    private fun processPath(path: Path) {
        if (context.gitValidator.isValidRepo(path.toString())) {
            Logger.print("Added git repository at $path.")
            val hashAll = confirm("Do you want to hash commits of all contributors?",
                defaultIsYes = true)
            val localRepo = LocalRepo(
                path = path.toString(),
                hashAllContributors = hashAll
            )
            val aggregate = context.userRepository.load()
            aggregate.addLocalRepo(localRepo)
            context.userRepository.save(aggregate)
            Logger.print("Successfully processed $path")
        } else {
            Logger.warn { "No valid git repository found at specified path $path" }
            Logger.print("Make sure that master branch with at least one commit exists.")
        }
    }

    override fun next() {
        context.changeState(EmailState(context))
    }

    private fun toAbsolutePath(pathStr: String): Path {
        val substitutePath = if (pathStr.startsWith("~/")) {
            System.getProperty("user.home") + pathStr.substring(1)
        } else pathStr
        return Paths.get(substitutePath).toAbsolutePath().normalize()
    }

    private fun confirm(message: String, defaultIsYes: Boolean): Boolean {
        val yes = if (defaultIsYes) "Y" else "y"
        val no = if (!defaultIsYes) "N" else "n"
        Logger.print("$message [$yes/$no]")
        val oppositeDefaultValue = if (defaultIsYes) no else yes
        return if ((readLine() ?: "").toLowerCase() == oppositeDefaultValue) {
            !defaultIsYes
        } else defaultIsYes
    }
}
