package app.infrastructure.persistence

import app.domain.repository.entity.Commit
import app.domain.repository.port.ServerApiPort
import app.domain.repository.repository.CommitRepository
import app.infrastructure.Logger

/**
 * Implementação de CommitRepository via API do servidor.
 */
class ApiCommitRepository(
    private val apiPort: ServerApiPort
) : CommitRepository {

    override fun saveAll(commits: List<Commit>) {
        if (commits.isNotEmpty()) {
            apiPort.postCommits(commits).onErrorThrow()
            Logger.info { "Sent ${commits.size} added commits to server" }
        }
    }

    override fun deleteAll(commits: List<Commit>) {
        if (commits.isNotEmpty()) {
            apiPort.deleteCommits(commits).onErrorThrow()
            Logger.info { "Sent ${commits.size} deleted commits to server" }
        }
    }
}
