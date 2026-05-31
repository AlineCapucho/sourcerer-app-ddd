package app.domain.repository.repository

import app.domain.repository.entity.Author

/**
 * Interface de repositório para a entidade Author.
 * Define como os autores são persistidos.
 */
interface AuthorRepository {
    /**
     * Envia autores descobertos ao servidor (para convites).
     */
    fun saveAll(authors: List<Author>)
}
