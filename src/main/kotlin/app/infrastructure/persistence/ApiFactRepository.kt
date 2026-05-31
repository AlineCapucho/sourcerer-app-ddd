package app.infrastructure.persistence

import app.domain.repository.port.ServerApiPort
import app.domain.repository.repository.FactRepository
import app.domain.repository.valueobject.Fact
import app.infrastructure.Logger

/**
 * Implementação de FactRepository via API do servidor.
 */
class ApiFactRepository(
    private val apiPort: ServerApiPort
) : FactRepository {

    override fun saveAll(facts: List<Fact>) {
        if (facts.isNotEmpty()) {
            apiPort.postFacts(facts).onErrorThrow()
            Logger.info { "Sent ${facts.size} facts to server" }
        }
    }
}
