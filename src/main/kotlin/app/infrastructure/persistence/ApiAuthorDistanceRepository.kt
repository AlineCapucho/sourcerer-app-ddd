package app.infrastructure.persistence

import app.domain.repository.port.ServerApiPort
import app.domain.repository.repository.AuthorDistanceRepository
import app.domain.repository.valueobject.AuthorDistance

/**
 * Implementação de AuthorDistanceRepository via API do servidor.
 */
class ApiAuthorDistanceRepository(
    private val apiPort: ServerApiPort
) : AuthorDistanceRepository {

    override fun saveAll(distances: List<AuthorDistance>) {
        if (distances.isNotEmpty()) {
            apiPort.postAuthorDistances(distances).onErrorThrow()
        }
    }
}
