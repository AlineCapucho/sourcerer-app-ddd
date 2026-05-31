package app.application.dto

/**
 * DTO para transportar informação de repositório entre camadas.
 */
data class RepoDTO(
    val rehash: String = "",
    val initialCommitRehash: String = "",
    val emails: List<String> = listOf(),
    val meta: RepoMetaDTO = RepoMetaDTO(),
    val processEntryId: Int = 0
)

data class RepoMetaDTO(
    val hosterId: String = "",
    val service: String = "",
    val name: String = "",
    val ownerName: String = "",
    val description: String = "",
    val htmlUrl: String = "",
    val cloneUrl: String = ""
)
