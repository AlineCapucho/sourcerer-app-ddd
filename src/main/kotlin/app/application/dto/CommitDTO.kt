package app.application.dto

/**
 * DTO para transportar informação de commit entre camadas.
 */
data class CommitDTO(
    val rehash: String = "",
    val repoRehash: String = "",
    val treeRehash: String = "",
    val authorName: String = "",
    val authorEmail: String = "",
    val dateTimestamp: Long = 0,
    val isQommit: Boolean = false,
    val numLinesAdded: Int = 0,
    val numLinesDeleted: Int = 0,
    val stats: List<CommitStatsDTO> = listOf()
)

data class CommitStatsDTO(
    val numLinesAdded: Int = 0,
    val numLinesDeleted: Int = 0,
    val type: Int = 0,
    val tech: String = ""
)
