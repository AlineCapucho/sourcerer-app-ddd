package app.domain.repository.valueobject

/**
 * Objeto de valor que representa estatísticas de tecnologia em um commit.
 * Imutável, sem identidade própria.
 */
data class CommitStats(
    val numLinesAdded: Int = 0,
    val numLinesDeleted: Int = 0,
    val type: Int = 0,
    val tech: String = ""
) {
    companion object {
        const val TYPE_LANGUAGE = 1
        const val TYPE_LIBRARY = 2
        const val TYPE_KEYWORD = 3
        const val TYPE_SYNTAX = 4
    }

    init {
        require(numLinesAdded >= 0) { "numLinesAdded must be non-negative" }
        require(numLinesDeleted >= 0) { "numLinesDeleted must be non-negative" }
    }
}
