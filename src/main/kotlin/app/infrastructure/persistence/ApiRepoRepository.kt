package app.infrastructure.persistence

import app.domain.repository.entity.Repo
import app.domain.repository.port.ServerApiPort
import app.domain.repository.repository.RepoRepository
import app.domain.repository.valueobject.ProcessEntry
import app.infrastructure.Logger

/**
 * Implementação de RepoRepository via API do servidor.
 */
class ApiRepoRepository(
    private val apiPort: ServerApiPort
) : RepoRepository {

    override fun postAndRetrieve(repo: Repo): Repo {
        val repoData = apiPort.postRepo(repo).getOrThrow()
        Logger.info { "Received repo from server with ${repoData.commits.size} commits" }
        return repo
    }

    override fun updateProcessStatus(processEntry: ProcessEntry) {
        apiPort.postProcess(listOf(processEntry)).onErrorThrow()
    }
}
