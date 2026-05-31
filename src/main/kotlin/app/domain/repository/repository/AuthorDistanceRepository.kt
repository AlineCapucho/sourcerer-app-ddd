package app.domain.repository.repository

import app.domain.repository.valueobject.AuthorDistance

/**
 * Interface de repositório para distâncias entre autores.
 * Define como as métricas de colaboração são persistidas.
 */
interface AuthorDistanceRepository {
    /**
     * Envia métricas de distância entre autores ao servidor.
     */
    fun saveAll(distances: List<AuthorDistance>)
}
