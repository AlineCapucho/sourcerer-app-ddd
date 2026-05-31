package app.infrastructure.persistence

import app.domain.repository.entity.Author
import app.domain.repository.port.ServerApiPort
import app.domain.repository.repository.AuthorRepository

/**
 * Implementação de AuthorRepository via API do servidor.
 */
class ApiAuthorRepository(
    private val apiPort: ServerApiPort
) : AuthorRepository {

    override fun saveAll(authors: List<Author>) {
        if (authors.isNotEmpty()) {
            apiPort.postAuthors(authors).onErrorThrow()
        }
    }
}
