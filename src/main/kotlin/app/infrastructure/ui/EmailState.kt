package app.infrastructure.ui

import app.domain.user.valueobject.UserEmail
import app.infrastructure.Logger
import org.eclipse.jgit.api.Git
import java.io.File

/**
 * Email management console UI state.
 * Handles adding/confirming user emails for profile building.
 */
class EmailState(private val context: ConsoleUi) : ConsoleState {

    override fun doAction() {
        val aggregate = context.userRepository.load()
        val user = aggregate.user
        val emails = user.emails

        if (emails.isNotEmpty()) {
            Logger.print("List of your emails:", indentLine = true)
            emails.forEach { email -> println(email) }
        } else {
            Logger.print("Add at least one email to build your profile.",
                indentLine = true)
        }

        val knownEmails = emails.map { it.address }
        val newEmails = hashSetOf<String>()
        val configEmails = hashSetOf<String>()

        // Add emails from git configs of tracked repos.
        val reposEmails = hashMapOf<String, HashSet<String>>()
        for (repo in aggregate.getLocalRepos()) {
            var git: Git? = null
            try {
                git = Git.open(File(repo.path))
                var email = git.repository.config
                    .getString("user", null, "email") ?: ""
                email = email.toLowerCase()
                if (email.isNotEmpty() && !knownEmails.contains(email)) {
                    configEmails.add(email)
                }
                // Fetch emails from repo for "no-email" warning.
                val revWalk = org.eclipse.jgit.revwalk.RevWalk(git.repository)
                val head = git.repository.resolve("HEAD")
                if (head != null) {
                    revWalk.markStart(revWalk.parseCommit(head))
                    val repoEmailSet = hashSetOf<String>()
                    var commit = revWalk.next()
                    while (commit != null) {
                        repoEmailSet.add(commit.authorIdent.emailAddress.toLowerCase())
                        commit = revWalk.next()
                    }
                    reposEmails[repo.path] = repoEmailSet
                    revWalk.dispose()
                }
            } catch (e: Exception) {
                Logger.error(e, "Error while parsing repo")
            } finally {
                git?.repository?.close()
                git?.close()
            }
        }

        if (configEmails.isNotEmpty()) {
            Logger.print("Your git config contains untracked emails:")
            configEmails.forEach { email -> println(email) }
            if (confirm("Do you want to add this emails to your account?",
                    defaultIsYes = true)) {
                newEmails.addAll(configEmails)
            }
        }

        // Show warning if no commits from user in some repos.
        val reposUserMissing = mutableListOf<String>()
        for (repo in aggregate.getLocalRepos()) {
            val presentedEmails = reposEmails[repo.path]
            val updatedEmails = knownEmails + newEmails
            if (presentedEmails != null) {
                var userMissing = true
                for (email in presentedEmails) {
                    if (updatedEmails.contains(email)) {
                        userMissing = false
                        break
                    }
                }
                if (userMissing) {
                    reposUserMissing.add(repo.path)
                }
            }
        }
        if (reposUserMissing.isNotEmpty()) {
            if (reposUserMissing.size == 1) {
                Logger.print("${reposUserMissing.first()} repo does not " +
                    "contains commits from emails you've specified")
            } else {
                Logger.print("Following repos do not contain commits from " +
                    "emails you've specified:")
                reposUserMissing.forEach { Logger.print(it) }
            }
        }

        // Ask user to enter additional emails.
        if (confirm("Do you want to specify additional emails " +
                "that you use in repositories?", defaultIsYes = false)) {
            while (true) {
                Logger.print("Type a email, or hit Enter to continue.")
                val email = (readLine() ?: "").toLowerCase()
                if (email.isBlank()) break
                if (!knownEmails.contains(email)) newEmails.add(email)
            }
        }

        if (newEmails.isNotEmpty()) {
            val newUserEmails = newEmails.map {
                UserEmail(address = it)
            }
            user.registerEmails(newUserEmails)
            context.userRepository.save(aggregate)

            // Send new emails to server.
            context.apiPort.postUser(
                emails = newUserEmails,
                repos = listOf()
            )
        }

        // Warn user about need of confirmation.
        val unverified = emails.filter { !it.verified }
        if (unverified.isNotEmpty() || newEmails.isNotEmpty()) {
            Logger.print("Confirm your emails to show all statistics in profile.")
        }
    }

    override fun next() {
        context.changeState(UpdateRepoState(context))
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
