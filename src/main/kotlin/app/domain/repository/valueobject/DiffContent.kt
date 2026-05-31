package app.domain.repository.valueobject

/**
 * Objeto de valor que representa o conteúdo de um diff (lado old ou new).
 * Imutável, sem identidade própria.
 */
data class DiffContent(
    val content: List<String> = listOf(),
    val ranges: List<DiffRange> = listOf(),
    val imports: List<String> = listOf()
) {
    fun getAllDiffs(): List<String> {
        return ranges.fold(mutableListOf()) { total, range ->
            total.addAll(content.subList(range.start, range.end))
            total
        }
    }
}
