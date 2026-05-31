package app.application.dto

/**
 * DTO para transportar informação de fatos entre camadas.
 */
data class FactDTO(
    val repoRehash: String = "",
    val code: Int = 0,
    val key: Int = 0,
    val value: String = "",
    val authorEmail: String = "",
    val value2: String = "",
    val value3: String = ""
)
