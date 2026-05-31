package app.domain.repository.valueobject

/**
 * Objeto de valor que representa um intervalo de linhas alteradas.
 * [start] (inclusive) e [end] (exclusive) especificam o range de linhas.
 * Imutável, sem identidade própria, com autovalidação.
 */
data class DiffRange(val start: Int, val end: Int) {

    init {
        require(start >= 0) { "Start must be non-negative" }
        require(end >= start) { "End must be greater than or equal to start" }
    }

    fun lineCount(): Int = end - start
}
