package app.domain.repository.valueobject

/**
 * Objeto de valor que representa o status de processamento de um repositório.
 * Imutável, sem identidade própria.
 */
data class ProcessEntry(
    val id: Int,
    val status: Int,
    val errorCode: Int = 0
) {
    init {
        require(id >= 0) { "ProcessEntry id must be non-negative" }
    }
}
