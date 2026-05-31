package app.domain.repository.valueobject

import app.domain.shared.valueobject.Email

/**
 * Objeto de valor que representa a distância/colaboração entre autores.
 * Imutável, sem identidade própria.
 */
data class AuthorDistance(
    val repoRehash: String,
    val email: Email,
    val score: Double
) {
    init {
        require(score >= 0.0) { "Score must be non-negative" }
    }
}
