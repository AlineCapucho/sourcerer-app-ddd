package app.domain.repository.valueobject

/**
 * Objeto de valor com metadados do repositório.
 * Imutável, sem identidade própria.
 */
data class RepoMeta(
    val hosterId: String = "",
    val service: String = "",
    val name: String = "",
    val ownerName: String = "",
    val description: String = "",
    val htmlUrl: String = "",
    val cloneUrl: String = ""
)
