package app.infrastructure.ui

import app.BuildConfig
import app.application.usecase.HashingException
import app.domain.repository.port.ApiResult
import app.domain.repository.port.ServerApiPort
import app.domain.repository.valueobject.ProcessEntry
import app.domain.user.valueobject.LocalRepo
import app.infrastructure.Logger
import java.util.*
import kotlin.concurrent.fixedRateTimer

/**
 * Update repositories console UI state.
 * Orquestra o hashing de todos os repositórios rastreados.
 */
class UpdateRepoState(private val context: ConsoleUi) : ConsoleState {
    override fun doAction() {
        Logger.info { "Hashing started" }

        val aggregate = context.userRepository.load()
        val localRepos = aggregate.getLocalRepos()
        val userEmails = aggregate.user.emailAddresses()

        // Assign process IDs from server
        val reposWithProcess = assignProcess(localRepos, context.apiPort)

        val heartbeatTimer = runHeartbeatTimer(reposWithProcess, context.apiPort)
        for (repo in reposWithProcess) {
            try {
                Logger.print("Hashing ${repo.path} repository...", indentLine = true)
                val useCase = context.hashRepositoryUseCaseFactory()
                useCase.execute(repo, userEmails)
                Logger.print("Hashing ${repo.path} completed.")
            } catch (e: HashingException) {
                e.errors.forEach { error ->
                    Logger.error(error, "Error while hashing")
                }
            } catch (e: Throwable) {
                Logger.error(e, "Error while hashing")
            }
        }
        heartbeatTimer.cancel()

        Logger.print("The repositories have been hashed.")
        Logger.print("Take a look at the updates in your profile at " +
            BuildConfig.PROFILE_URL + (aggregate.credentials?.username ?: ""),
            indentLine = true)
        Logger.info(Logger.Events.HASHING_SUCCESS) { "Hashing success" }
    }

    override fun next() {
        context.changeState(CloseState())
    }

    /**
     * Notifica o servidor sobre o início do processamento e obtém IDs.
     */
    private fun assignProcess(
        localRepos: List<LocalRepo>,
        apiPort: ServerApiPort
    ): List<LocalRepo> {
        val result = apiPort.postProcessCreate(requestNumEntries = localRepos.size)
        if (result is ApiResult.Success) {
            val processData = result.data
            if (processData.entries.isNotEmpty()) {
                return localRepos.mapIndexed { index, repo ->
                    if (index < processData.entries.size) {
                        LocalRepo(
                            path = repo.path,
                            hashAllContributors = repo.hashAllContributors,
                            remoteOrigin = repo.remoteOrigin,
                            meta = repo.meta,
                            processEntryId = processData.entries[index].id
                        )
                    } else repo
                }
            }
        }
        return localRepos
    }

    private fun runHeartbeatTimer(
        localRepos: List<LocalRepo>,
        apiPort: ServerApiPort
    ): Timer {
        val entries = localRepos
            .filter { it.processEntryId != null }
            .map { ProcessEntry(id = it.processEntryId!!, status = 0) }
        return fixedRateTimer(
            name = "heartbeat",
            daemon = true,
            initialDelay = BuildConfig.HEARTBEAT_RATE,
            period = BuildConfig.HEARTBEAT_RATE,
            action = {
                try {
                    if (entries.isNotEmpty()) {
                        apiPort.postProcess(entries).onErrorThrow()
                    }
                } catch (e: Throwable) {
                    Logger.error(e)
                }
            }
        )
    }
}
