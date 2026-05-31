package app.domain.repository.valueobject

import app.domain.shared.valueobject.Email

/**
 * Objeto de valor que representa um fato/estatística sobre um desenvolvedor.
 * Imutável, sem identidade própria.
 */
data class Fact(
    val repoRehash: String,
    val code: Int,
    val key: Int,
    val value: String,
    val authorEmail: Email,
    val value2: String = "",
    val value3: String = ""
)
